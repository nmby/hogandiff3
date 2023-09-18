package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較タスクの基底クラスです。<br>
 * 
 * @author nmby
 */
/*package*/ abstract class AppTaskBase extends Task<Void> {
    
    // [static members] ********************************************************
    
    protected static final String BR = System.lineSeparator();
    protected static final int PROGRESS_MAX = 100;
    
    // [instance members] ******************************************************
    
    /** 今回の実行における各種設定を保持する設定セット */
    protected final Settings settings;
    
    /** 各種インスタンスのファクトリ */
    protected final Factory factory;
    
    /** ユーザー向け表示文字列を保持する {@link StringBuilder} */
    protected final StringBuilder str = new StringBuilder();
    
    /** このアプリケーションのリソースバンドル */
    protected final ResourceBundle rb = AppMain.appResource.get();
    
    /*package*/ AppTaskBase(
            Settings settings,
            Factory factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
    }
    
    /**
     * このタスクの比較対象Excelブックが同じ同一ブックかを返します。<br>
     * 
     * @return 同一ブックの場合は {@code true}
     * @throws IllegalStateException 今回の実行メニューが {@link AppMenu.COMPARE_DIRS} の場合
     */
    protected boolean isSameBook() {
        AppMenu menu = settings.get(SettingKeys.CURR_MENU);
        if (menu == AppMenu.COMPARE_DIRS) {
            throw new IllegalStateException("not suitable for COMPARE_DIRS");
        }
        
        return Objects.equals(
                settings.get(SettingKeys.CURR_BOOK_INFO1).bookPath(),
                settings.get(SettingKeys.CURR_BOOK_INFO2).bookPath());
    }
    
    /**
     * 指定された2つのExcelブックに含まれるシート名をロードし、
     * 設定内容に基づいてシート名をペアリングして返します。<br>
     * 
     * @param bookInfo1 Excelブック情報1
     * @param bookInfo2 Excelブック情報2
     * @return シート名のペアのリスト
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    protected List<Pair<String>> getSheetNamePairs(
            BookInfo bookInfo1,
            BookInfo bookInfo2)
            throws ExcelHandlingException {
        
        assert bookInfo1 != null;
        assert bookInfo2 != null;
        assert !Objects.equals(bookInfo1.bookPath(), bookInfo2.bookPath());
        
        BookLoader bookLoader1 = factory.bookLoader(bookInfo1);
        BookLoader bookLoader2 = factory.bookLoader(bookInfo2);
        List<String> sheetNames1 = bookLoader1.loadSheetNames(bookInfo1);
        List<String> sheetNames2 = bookLoader2.loadSheetNames(bookInfo2);
        
        Matcher<String> matcher = factory.sheetNameMatcher(settings);
        List<IntPair> pairs = matcher.makePairs(sheetNames1, sheetNames2);
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? sheetNames1.get(p.a()) : null,
                        p.hasB() ? sheetNames2.get(p.b()) : null))
                .toList();
    }
    
    /**
     * 今回の実行のための作業要ディレクトリを作成してそのパスを返します。<br>
     * 
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @return 作業用ディレクトリのパス
     * @throws ApplicationException 処理に失敗した場合
     */
    protected Path createWorkDir(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        assert 0 <= progressBefore;
        assert progressBefore <= progressAfter;
        assert progressAfter <= PROGRESS_MAX;
        
        Path workDir = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            workDir = settings.getOrDefault(SettingKeys.WORK_DIR_BASE)
                    .resolve(settings.getOrDefault(SettingKeys.CURR_TIMESTAMP));
            
            str.append("%s%n    - %s%n%n".formatted(rb.getString("AppTaskBase.010"), workDir));
            updateMessage(str.toString());
            
            workDir = Files.createDirectories(workDir);
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return workDir;
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.020")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTaskBase.020"), workDir),
                    e);
        }
    }
    
    /**
     * 比較結果文字列をテキストファイルに保存するとともに、
     * 設定に応じてアプリケーション（メモ帳）を立ち上げて表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param resultText 比較結果文字列
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void saveAndShowResultText(
            Path workDir,
            String resultText,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        assert workDir != null;
        assert resultText != null;
        assert 0 <= progressBefore;
        assert progressBefore <= progressAfter;
        assert progressAfter <= PROGRESS_MAX;
        
        Path textPath = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            textPath = workDir.resolve("result.txt");
            
            str.append("%s%n    - %s%n%n".formatted(rb.getString("AppTaskBase.030"), textPath));
            updateMessage(str.toString());
            
            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(resultText);
            }
            if (settings.getOrDefault(SettingKeys.SHOW_RESULT_TEXT)) {
                str.append(rb.getString("AppTaskBase.040")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(textPath.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.050")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTaskBase.050"), textPath),
                    e);
        }
    }
    
    /**
     * Excelブックの各シートに比較結果の色を付けて保存し、
     * 設定に応じてExcelを立ち上げて表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param bResult Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void paintSaveAndShowBook(
            Path workDir,
            BResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        assert workDir != null;
        assert bResult != null;
        assert 0 <= progressBefore;
        assert progressBefore <= progressAfter;
        assert progressAfter <= PROGRESS_MAX;
        
        if (isSameBook()) {
            paintSaveAndShowBook1(workDir, bResult, progressBefore, progressAfter);
        } else {
            paintSaveAndShowBook2(workDir, bResult, progressBefore, progressAfter);
        }
    }
    
    private void paintSaveAndShowBook1(
            Path workDir,
            BResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        BookInfo dst = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            BookInfo src = settings.get(SettingKeys.CURR_BOOK_INFO1);
            dst = BookInfo.of(
                    workDir.resolve(src.bookPath().getFileName()),
                    src.getReadPassword());
            
            str.append("    - %s%n%n".formatted(dst));
            updateMessage(str.toString());
            
            BookPainter painter = factory.painter(settings, dst);
            Map<String, Optional<SResult.Piece>> result = new HashMap<>(bResult.getPiece(Side.A));
            result.putAll(bResult.getPiece(Side.B));
            painter.paintAndSave(src, dst, result);
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.070")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.070"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.090")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.090"), e);
        }
    }
    
    private void paintSaveAndShowBook2(
            Path workDir,
            BResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        BookInfo dst1 = null;
        BookInfo dst2 = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            BookInfo src1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            dst1 = BookInfo.of(
                    workDir.resolve("【A】" + src1.bookPath().getFileName()),
                    src1.getReadPassword());
            
            str.append("    - %s%n".formatted(dst1));
            updateMessage(str.toString());
            
            BookPainter painter1 = factory.painter(settings, dst1);
            painter1.paintAndSave(src1, dst1, bResult.getPiece(Side.A));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 2 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.100")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.100"), e);
        }
        
        try {
            BookInfo src2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            dst2 = BookInfo.of(
                    workDir.resolve("【B】" + src2.bookPath().getFileName()),
                    src2.getReadPassword());
            
            str.append("    - %s%n%n".formatted(dst2));
            updateMessage(str.toString());
            
            BookPainter painter2 = factory.painter(settings, dst2);
            painter2.paintAndSave(src2, dst2, bResult.getPiece(Side.B));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.110")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.110"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst1.bookPath().toFile());
                Desktop.getDesktop().open(dst2.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.090")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.090"), e);
        }
    }
    
    /**
     * 処理修了をアナウンスする。<br>
     */
    protected void announceEnd() {
        str.append(rb.getString("AppTaskBase.120"));
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
