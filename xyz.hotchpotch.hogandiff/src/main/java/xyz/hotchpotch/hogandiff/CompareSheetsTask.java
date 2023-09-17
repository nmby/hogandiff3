package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * Excelシート同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ class CompareSheetsTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CompareSheetsTask(Settings settings, Factory factory) {
        super(settings, factory);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 2. シート同士の比較
        BResult bResult = compareSheets(5, 75);
        
        // 3. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, bResult.toString(), 75, 80);
        
        // 4. 比較結果の表示（Excelブック）
        saveAndShowPaintedSheets(workDir, bResult, 80, 98);
        
        // 5. 処理終了のアナウンス
        announceEnd();
        
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(int progressBefore, int progressAfter) {
        updateProgress(progressBefore, PROGRESS_MAX);
        
        BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
        BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
        String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
        String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
        
        str.append(rb.getString("AppTask.020")).append(BR);
        str.append(isSameBook()
                ? "%s%n[A] %s%n[B] %s%n%n".formatted(bookInfo1, sheetName1, sheetName2)
                : "[A] %s - %s%n[B] %s - %s%n%n".formatted(bookInfo1, sheetName1, bookInfo2, sheetName2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 2. シート同士の比較
    private BResult compareSheets(
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTask.070")).append(BR);
            updateMessage(str.toString());
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            SheetLoader loader1 = factory.sheetLoader(settings, bookInfo1);
            SheetLoader loader2 = isSameBook()
                    ? loader1
                    : factory.sheetLoader(settings, bookInfo2);
            
            Pair<String> pair = Pair.of(
                    settings.get(SettingKeys.CURR_SHEET_NAME1),
                    settings.get(SettingKeys.CURR_SHEET_NAME2));
            
            str.append(BResult.formatSheetNamesPair(0, pair));
            updateMessage(str.toString());
            
            Set<CellData> cells1 = loader1.loadCells(bookInfo1, pair.a());
            Set<CellData> cells2 = loader2.loadCells(bookInfo2, pair.b());
            
            SComparator comparator = factory.comparator(settings);
            SResult result = comparator.compare(cells1, cells2);
            
            str.append("  -  ").append(result.getDiffSummary()).append(BR).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return BResult.of(
                    bookInfo1.bookPath(),
                    bookInfo2.bookPath(),
                    List.of(pair),
                    Map.of(pair, Optional.of(result)));
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.080")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.080"), e);
        }
    }
}
