package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.ShapeLoader;
import xyz.hotchpotch.hogandiff.excel.ShapeReplica;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;

/**
 * Apache POI イベントモデル API を利用して、
 * .xls 形式のExcelブックのワークシートから
 * 図形データを抽出する {@link ShapeLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class HSSFShapeLoaderWithPoiEventApi implements ShapeLoader {
    
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
        private final Set<ShapeReplica> shapes = new HashSet<>();
        
        private ProcessingStep step = ProcessingStep.SEARCHING_SHEET_DEFINITION;
        private int sheetIdx = 0;
        private int currIdx = 0;
        private CommonObjectDataSubRecord prevFtCmoRec;
        
        private Listener1(String sheetName) {
            assert sheetName != null;
            
            this.sheetName = sheetName;
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
            switch (record.getSid()) {
            case BoundSheetRecord.sid:
                BoundSheetRecord bsRec = (BoundSheetRecord) record;
                if (sheetName.equals(bsRec.getSheetname())) {
                    step = ProcessingStep.SEARCHING_SHEET_BODY;
                } else {
                    sheetIdx++;
                }
                break;
            
            case EOFRecord.sid:
                throw new NoSuchElementException(
                        "指定された名前のシートが見つかりません：" + sheetName);
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
            switch (record.getSid()) {
            case WSBoolRecord.sid:
                WSBoolRecord wsbRec = (WSBoolRecord) record;
                
                if (wsbRec.getDialog()) {
                    // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートも何故か getDialog() == false が返されるっぽい。
                    throw new UnsupportedOperationException(
                            "ダイアログシートはサポートされません。");
                }
                step = ProcessingStep.READING_CELL_CONTENTS_AND_COMMENTS;
                break;
            
            case EOFRecord.sid:
                throw new AssertionError("no WSBool record");
            }
        }
        
        /**
         * 目的のシートのセル内容物とセルコメントを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingCellContentsAndComments(Record record) {
            switch (record.getSid()) {
            case ObjRecord.sid:
                ObjRecord objRec = (ObjRecord) record;
                prevFtCmoRec = objRec.getSubRecords().stream()
                        .filter(sub -> sub instanceof CommonObjectDataSubRecord)
                        .map(sub -> (CommonObjectDataSubRecord) sub)
                        .filter(sub -> sub.getObjectType() != CommonObjectDataSubRecord.OBJECT_TYPE_COMMENT)
                        .findAny()
                        .orElse(null);
                break;
            
            case TextObjectRecord.sid:
                if (prevFtCmoRec != null) {
                    TextObjectRecord txoRec = (TextObjectRecord) record;
                    shapes.add(ShapeReplica.of(prevFtCmoRec.getObjectId(), txoRec.getStr().getString()));
                    prevFtCmoRec = null;
                    
                } else {
                    // throw new AssertionError("no preceding ftCmo record");
                    // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートの場合もこのパスに流れ込んできてしまう。
                }
                break;
            
            case EOFRecord.sid:
                step = ProcessingStep.COMPLETED;
                break;
            }
        }
    }
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @return 新しいローダー
     */
    public static ShapeLoader of() {
        return new HSSFShapeLoaderWithPoiEventApi();
    }
    
    // [instance members] ******************************************************
    
    private HSSFShapeLoaderWithPoiEventApi() {
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
    public Set<ShapeReplica> loadShapes(Path bookPath, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        Objects.requireNonNull(sheetName, "sheetName");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        try (FileInputStream fin = new FileInputStream(bookPath.toFile());
                POIFSFileSystem poifs = new POIFSFileSystem(fin)) {
            
            HSSFRequest req = new HSSFRequest();
            Listener1 listener1 = new Listener1(sheetName);
            req.addListenerForAllRecords(listener1);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.abortableProcessWorkbookEvents(req, poifs);
            return Set.copyOf(listener1.shapes);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
