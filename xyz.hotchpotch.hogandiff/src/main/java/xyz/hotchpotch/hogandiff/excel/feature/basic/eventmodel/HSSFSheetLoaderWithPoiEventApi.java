package xyz.hotchpotch.hogandiff.excel.feature.basic.eventmodel;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
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
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;

/**
 * Apache POI イベントモデル API を利用して、
 * .xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link SheetLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class HSSFSheetLoaderWithPoiEventApi implements SheetLoader<String> {
    
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
        
        /** 目的のシートのセルデータを読み取ります。 */
        READING_SHEET_DATA,
        
        /** 処理完了。 */
        COMPLETED;
        
        // [instance members] --------------------------------------------------
    }
    
    private static class Listener1 implements HSSFListener {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final String sheetName;
        private final boolean extractCachedValue;
        private final Set<CellReplica> cells = new HashSet<>();
        
        private ProcessingStep step = ProcessingStep.SEARCHING_SHEET_DEFINITION;
        private int sheetIdx = 0;
        private int currIdx = 0;
        private List<String> sst;
        private FormulaRecord prevFormulaRec;
        
        private Listener1(String sheetName, boolean extractCachedValue) {
            assert sheetName != null;
            
            this.sheetName = sheetName;
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
            
            case READING_SHEET_DATA:
                readingSheetData(record);
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
                        .collect(Collectors.toList());
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
                step = ProcessingStep.READING_SHEET_DATA;
                
            } else if (record.getSid() == EOFRecord.sid) {
                throw new AssertionError("no WSBool record");
            }
        }
        
        /**
         * 目的のシートのセルデータを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingSheetData(Record record) {
            if (record instanceof CellRecord) {
                if (prevFormulaRec != null) {
                    throw new AssertionError("no following string record");
                }
                
                CellRecord cellRec = (CellRecord) record;
                String value = null;
                
                switch (record.getSid()) {
                case LabelSSTRecord.sid:
                    LabelSSTRecord lRec = (LabelSSTRecord) cellRec;
                    value = sst.get(lRec.getSSTIndex());
                    break;
                
                case NumberRecord.sid:
                    NumberRecord nRec = (NumberRecord) cellRec;
                    value = NumberToTextConverter.toText(nRec.getValue());
                    break;
                
                case RKRecord.sid:
                    RKRecord rkRec = (RKRecord) cellRec;
                    value = NumberToTextConverter.toText(rkRec.getRKNumber());
                    break;
                
                case BoolErrRecord.sid:
                    BoolErrRecord beRec = (BoolErrRecord) cellRec;
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
                    cells.add(CellReplica.of(
                            cellRec.getRow(),
                            cellRec.getColumn(),
                            value));
                }
                
            } else if (record.getSid() == StringRecord.sid) {
                if (prevFormulaRec == null) {
                    throw new AssertionError("unexpected string record");
                }
                
                StringRecord sRec = (StringRecord) record;
                String value = sRec.getString();
                if (value != null && !"".equals(value)) {
                    cells.add(CellReplica.of(
                            prevFormulaRec.getRow(),
                            prevFormulaRec.getColumn(),
                            sRec.getString()));
                }
                prevFormulaRec = null;
                
            } else if (record.getSid() == EOFRecord.sid) {
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
                    // STRING の場合は後続の STRING レコードがあるはず
                    throw new AssertionError("STRING");
                
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
     * @param extractCachedValue
     *              数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *              数式文字列を抽出する場合は {@code false}
     * @return 新しいローダー
     */
    public static SheetLoader<String> of(boolean extractCachedValue) {
        return new HSSFSheetLoaderWithPoiEventApi(extractCachedValue);
    }
    
    // [instance members] ******************************************************
    
    private final boolean extractCachedValue;
    
    private HSSFSheetLoaderWithPoiEventApi(boolean extractCachedValue) {
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
        
        try (FileInputStream fin = new FileInputStream(bookPath.toFile());
                POIFSFileSystem poifs = new POIFSFileSystem(fin)) {
            
            HSSFRequest req = new HSSFRequest();
            Listener1 listener1 = new Listener1(sheetName, extractCachedValue);
            req.addListenerForAllRecords(listener1);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.abortableProcessWorkbookEvents(req, poifs);
            return listener1.cells;
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
