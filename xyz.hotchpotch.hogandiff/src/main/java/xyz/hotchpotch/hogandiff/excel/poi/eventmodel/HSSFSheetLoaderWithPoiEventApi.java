package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.NumberToTextConverter;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;

/**
 * Apache POI イベントモデル API を利用して、
 * .xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link SheetLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class HSSFSheetLoaderWithPoiEventApi implements SheetLoader {
    
    // [static members] ********************************************************
    
    /**
     * 内部処理のステップを表す列挙型です。<br>
     *
     * @author nmby
     */
    private static enum ProcessingStep {
        
        // [static members] ----------------------------------------------------
        
        /** BOUNDSHEET レコードの中から、目的のシートが何番目に定義されているかを探します。 */
        SEARCHING_SHEET_DEFINITION,
        
        /** Excelブック共通の SST レコードを読み取ります。 */
        READING_SST_DATA,
        
        /** 目的のシートが定義される BOF レコードを探します。 */
        SEARCHING_SHEET_BODY,
        
        /** 目的のシートがワークシートなのかダイアログシートなのかを確認します。 */
        CHECK_WORKSHEET_OR_DIALOGSHEET,
        
        /** 目的のシートのセル内容物とセルコメントを読み取ります。 */
        READING_CELL_CONTENTS_AND_COMMENTS,
        
        /** 処理完了。 */
        COMPLETED;
        
        // [instance members] --------------------------------------------------
    }
    
    private static class Listener1 implements HSSFListener {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final String sheetName;
        private final boolean extractContents;
        private final boolean extractComments;
        private final boolean extractCachedValue;
        private final Map<String, CellReplica> cells = new HashMap<>();
        private final Map<Integer, String> comments = new HashMap<>();
        
        private ProcessingStep step = ProcessingStep.SEARCHING_SHEET_DEFINITION;
        private int sheetIdx = 0;
        private int currIdx = 0;
        private List<String> sst;
        private FormulaRecord prevFormulaRec;
        private CommonObjectDataSubRecord prevFtCmoRec;
        
        private Listener1(
                String sheetName,
                boolean extractContents,
                boolean extractComments,
                boolean extractCachedValue) {
            
            assert sheetName != null;
            assert extractContents || extractComments;
            
            this.sheetName = sheetName;
            this.extractContents = extractContents;
            this.extractComments = extractComments;
            this.extractCachedValue = extractCachedValue;
        }
        
        /**
         * .xls 形式のExcelブックからセルデータを抽出します。<br>
         * 
         * @param record レコード
         * @throws NoSuchElementException
         *      指定された名前のシートが見つからない場合
         * @throws UnsupportedOperationException
         *      指定された名前のシートがワークシートではなかった場合
         * @throws UnsupportedOperationException
         *      数式セルからキャッシュされた計算値ではなく数式文字列を抽出しようとした場合
         */
        @Override
        public void processRecord(Record record) {
            switch (step) {
            case SEARCHING_SHEET_DEFINITION:
                searchingSheetDefinition(record);
                break;
            
            case READING_SST_DATA:
                readingSstData(record);
                break;
            
            case SEARCHING_SHEET_BODY:
                searchingSheetBody(record);
                break;
            
            case CHECK_WORKSHEET_OR_DIALOGSHEET:
                checkWorksheetOrDialogsheet(record);
                break;
            
            case READING_CELL_CONTENTS_AND_COMMENTS:
                readingCellContentsAndComments(record);
                break;
            
            case COMPLETED:
                // nop
                break;
            
            default:
                throw new AssertionError(step);
            }
        }
        
        /**
         * BOUNDSHEET レコードの中から、目的のシートが何番目に定義されているかを探します。<br>
         * 
         * @param record レコード
         * @throws NoSuchElementException 指定された名前のシートが見つからない場合
         */
        private void searchingSheetDefinition(Record record) {
            if (record.getSid() == BoundSheetRecord.sid) {
                BoundSheetRecord bsRec = (BoundSheetRecord) record;
                if (sheetName.equals(bsRec.getSheetname())) {
                    step = ProcessingStep.READING_SST_DATA;
                } else {
                    sheetIdx++;
                }
                
            } else if (record.getSid() == EOFRecord.sid) {
                throw new NoSuchElementException(
                        "指定された名前のシートが見つかりません：" + sheetName);
            }
        }
        
        /**
         * Excelブック共通の SST レコードを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingSstData(Record record) {
            if (record.getSid() == SSTRecord.sid) {
                SSTRecord sstRec = (SSTRecord) record;
                sst = IntStream.range(0, sstRec.getNumUniqueStrings())
                        .mapToObj(sstRec::getString)
                        .map(UnicodeString::getString)
                        .toList();
                step = ProcessingStep.SEARCHING_SHEET_BODY;
                
            } else if (record.getSid() == EOFRecord.sid) {
                throw new AssertionError("no sst record");
            }
        }
        
        /**
         * 目的のシートが定義される BOF レコードを探します。<br>
         * 
         * @param record レコード
         * @throws UnsupportedOperationException
         *      指定された名前のシートがグラフシートもしくはマクロシートだった場合
         */
        private void searchingSheetBody(Record record) {
            if (record.getSid() == BOFRecord.sid) {
                BOFRecord bofRec = (BOFRecord) record;
                
                switch (bofRec.getType()) {
                case BOFRecord.TYPE_WORKSHEET:
                    if (currIdx == sheetIdx) {
                        step = ProcessingStep.CHECK_WORKSHEET_OR_DIALOGSHEET;
                    } else {
                        currIdx++;
                    }
                    break;
                
                case BOFRecord.TYPE_CHART:
                case BOFRecord.TYPE_EXCEL_4_MACRO:
                    if (currIdx == sheetIdx) {
                        throw new UnsupportedOperationException(
                                "このシート形式はサポートされません：type==" + bofRec.getType());
                    } else {
                        currIdx++;
                        break;
                    }
                    
                case BOFRecord.TYPE_WORKBOOK:
                case BOFRecord.TYPE_WORKSPACE_FILE:
                case BOFRecord.TYPE_VB_MODULE:
                    // nop
                    break;
                
                default:
                    throw new AssertionError("unknown BOF type: " + bofRec.getType());
                }
            }
        }
        
        /**
         * 目的のシートがワークシートなのかダイアログシートなのかを確認します。<br>
         * 
         * @param record レコード
         * @throws UnsupportedOperationException
         *      指定された名前のシートがダイアログシートだった場合
         */
        private void checkWorksheetOrDialogsheet(Record record) {
            if (record.getSid() == WSBoolRecord.sid) {
                WSBoolRecord wsbRec = (WSBoolRecord) record;
                
                if (wsbRec.getDialog()) {
                    // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートも何故か getDialog() == false が返されるっぽい。
                    throw new UnsupportedOperationException(
                            "ダイアログシートはサポートされません。");
                }
                step = ProcessingStep.READING_CELL_CONTENTS_AND_COMMENTS;
                
            } else if (record.getSid() == EOFRecord.sid) {
                throw new AssertionError("no WSBool record");
            }
        }
        
        /**
         * 目的のシートのセル内容物とセルコメントを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingCellContentsAndComments(Record record) {
            if (extractContents) {
                if (record instanceof CellRecord) {
                    if (prevFormulaRec != null) {
                        throw new AssertionError("no following string record");
                    }
                    
                    String value = null;
                    
                    switch (record.getSid()) {
                    case LabelSSTRecord.sid:
                        LabelSSTRecord lRec = (LabelSSTRecord) record;
                        value = sst.get(lRec.getSSTIndex());
                        break;
                    
                    case NumberRecord.sid:
                        NumberRecord nRec = (NumberRecord) record;
                        value = NumberToTextConverter.toText(nRec.getValue());
                        break;
                    
                    case RKRecord.sid:
                        RKRecord rkRec = (RKRecord) record;
                        value = NumberToTextConverter.toText(rkRec.getRKNumber());
                        break;
                    
                    case BoolErrRecord.sid:
                        BoolErrRecord beRec = (BoolErrRecord) record;
                        if (beRec.isBoolean()) {
                            value = Boolean.toString(beRec.getBooleanValue());
                        } else {
                            value = ErrorEval.getText(beRec.getErrorValue());
                        }
                        break;
                    
                    case FormulaRecord.sid:
                        value = getValueFromFormulaRecord((FormulaRecord) record);
                        break;
                    
                    default:
                        throw new AssertionError(record.getSid());
                    }
                    
                    if (value != null && !"".equals(value)) {
                        CellRecord cellRec = (CellRecord) record;
                        cells.put(
                                CellReplica.idxToAddress(
                                        cellRec.getRow(),
                                        cellRec.getColumn()),
                                CellReplica.of(
                                        cellRec.getRow(),
                                        cellRec.getColumn(),
                                        value,
                                        null));
                    }
                    
                } else if (record.getSid() == StringRecord.sid) {
                    if (prevFormulaRec == null) {
                        throw new AssertionError("unexpected string record");
                    }
                    
                    StringRecord sRec = (StringRecord) record;
                    String value = sRec.getString();
                    if (value != null && !"".equals(value)) {
                        cells.put(
                                CellReplica.idxToAddress(
                                        prevFormulaRec.getRow(),
                                        prevFormulaRec.getColumn()),
                                CellReplica.of(
                                        prevFormulaRec.getRow(),
                                        prevFormulaRec.getColumn(),
                                        sRec.getString(),
                                        null));
                    }
                    prevFormulaRec = null;
                }
            }
            
            if (extractComments) {
                switch (record.getSid()) {
                case ObjRecord.sid:
                    ObjRecord objRec = (ObjRecord) record;
                    Optional<CommonObjectDataSubRecord> ftCmo = objRec.getSubRecords().stream()
                            .filter(sub -> sub instanceof CommonObjectDataSubRecord)
                            .map(sub -> (CommonObjectDataSubRecord) sub)
                            .filter(sub -> sub.getObjectType() == CommonObjectDataSubRecord.OBJECT_TYPE_COMMENT)
                            .findAny();
                    ftCmo.ifPresent(ftCmoRec -> {
                        if (prevFtCmoRec != null) {
                            throw new AssertionError("no following txo record");
                        }
                        prevFtCmoRec = ftCmoRec;
                    });
                    break;
                
                case TextObjectRecord.sid:
                    if (prevFtCmoRec == null) {
                        // throw new AssertionError("no preceding ftCmo record");
                        // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートの場合もこのパスに流れ込んできてしまう。
                        break;
                    }
                    TextObjectRecord txoRec = (TextObjectRecord) record;
                    comments.put(prevFtCmoRec.getObjectId(), txoRec.getStr().getString());
                    prevFtCmoRec = null;
                    break;
                
                case NoteRecord.sid:
                    NoteRecord noteRec = (NoteRecord) record;
                    String address = CellReplica.idxToAddress(noteRec.getRow(), noteRec.getColumn());
                    String comment = comments.remove(noteRec.getShapeId());
                    
                    if (cells.containsKey(address)) {
                        CellReplica original = cells.get(address);
                        cells.put(address, CellReplica.of(
                                original.row(),
                                original.column(),
                                original.content(),
                                comment));
                    } else {
                        cells.put(address, CellReplica.of(address, "", comment));
                    }
                    break;
                }
            }
            
            if (record.getSid() == EOFRecord.sid) {
                step = ProcessingStep.COMPLETED;
            }
        }
        
        /**
         * FORMULA レコードからセル格納値を抽出します。<br>
         * 
         * @param fRec レコード
         * @return セル格納値
         * @throws UnsupportedOperationException
         *      キャッシュされた計算値ではなく数式文字列を抽出しようとした場合
         */
        private String getValueFromFormulaRecord(FormulaRecord fRec) {
            if (extractCachedValue) {
                if (fRec.hasCachedResultString()) {
                    prevFormulaRec = fRec;
                    return null;
                }
                
                @SuppressWarnings("deprecation")
                // FIXME: [No.91 内部実装改善] これが deprecated なら、どうすりゃいいのさ...
                CellType type = CellType.forInt(fRec.getCachedResultType());
                
                switch (type) {
                case NUMERIC:
                    return NumberToTextConverter.toText(fRec.getValue());
                
                case BOOLEAN:
                    return Boolean.toString(fRec.getCachedBooleanValue());
                
                case ERROR:
                    return ErrorEval.getText(fRec.getCachedErrorValue());
                
                case BLANK:
                    // nop: 空のセルは抽出しない。
                    return null;
                
                case _NONE:
                    throw new AssertionError("_NONE");
                
                case FORMULA:
                    // キャッシュされた値のタイプが FORMULA というのは無いはず
                    throw new AssertionError("FORMULA");
                
                case STRING:
                    // 利用者からのレポートによると、このパスに入る場合があるらしい。
                    // 返すべき適切な fRec のメンバが見当たらないため、nullを返しておく。
                    // FIXME: [No.4 数式サポート改善].xlsファイル形式を理解したうえでちゃんとやる
                    return null;
                
                default:
                    throw new AssertionError("unknown cell type: " + type);
                }
                
            } else {
                // FIXME: [No.4 数式サポート改善] 数式文字列もサポートできるようにする
                throw new UnsupportedOperationException(
                        "数式文字列の抽出はサポートされません。");
            }
        }
    }
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param extractContents セル内容物を抽出する場合は {@code true}
     * @param extractComments セルコメントを抽出する場合は {@code true}
     * @param extractCachedValue
     *              数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *              数式文字列を抽出する場合は {@code false}
     * @return 新しいローダー
     */
    public static SheetLoader of(
            boolean extractContents,
            boolean extractComments,
            boolean extractCachedValue) {
        
        return new HSSFSheetLoaderWithPoiEventApi(extractContents, extractComments, extractCachedValue);
    }
    
    // [instance members] ******************************************************
    
    private final boolean extractContents;
    private final boolean extractComments;
    private final boolean extractCachedValue;
    
    private HSSFSheetLoaderWithPoiEventApi(
            boolean extractContents,
            boolean extractComments,
            boolean extractCachedValue) {
        
        this.extractContents = extractContents;
        this.extractComments = extractComments;
        this.extractCachedValue = extractCachedValue;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookPath}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellReplica> loadCells(Path bookPath, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        Objects.requireNonNull(sheetName, "sheetName");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        if (!extractContents && !extractComments) {
            return Set.of();
        }
        
        try (FileInputStream fin = new FileInputStream(bookPath.toFile());
                POIFSFileSystem poifs = new POIFSFileSystem(fin)) {
            
            HSSFRequest req = new HSSFRequest();
            Listener1 listener1 = new Listener1(
                    sheetName, extractContents, extractComments, extractCachedValue);
            req.addListenerForAllRecords(listener1);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.abortableProcessWorkbookEvents(req, poifs);
            return Set.copyOf(listener1.cells.values());
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
