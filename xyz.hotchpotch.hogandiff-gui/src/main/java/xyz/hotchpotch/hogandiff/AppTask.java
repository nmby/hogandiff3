package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @param <T> 利用するファクトリの型
 * @author nmby
 */
public class AppTask<T> extends Task<Void> {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final int PROGRESS_MAX = 100;
    
    private static String sheetNamePair(Pair<String> pair) {
        return String.format("  - %s vs %s\n",
                pair.isPresentA() ? "[" + pair.a() + "]" : "(なし)",
                pair.isPresentB() ? "[" + pair.b() + "]" : "(なし)");
    }
    
    /**
     * 新しいタスクを生成して返します。<br>
     * 
     * @param <T> ファクトリの型
     * @param settings 設定
     * @param factory ファクトリ
     * @return 新しいタスク
     * 
     */
    public static <T> Task<Void> of(
            Settings settings,
            Factory<T> factory) {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(factory, "factory");
        
        return new AppTask<>(settings, factory);
    }
    
    // [instance members] ******************************************************
    
    private final Settings settings;
    private final Factory<T> factory;
    private final AppMenu menu;
    private final StringBuilder str = new StringBuilder();
    
    private AppTask(
            Settings settings,
            Factory<T> factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
        this.menu = settings.get(AppSettingKeys.CURR_MENU);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 2. 比較するシートの組み合わせの決定
        List<Pair<String>> pairs = pairingSheets(2, 5);
        
        // 3. シート同士の比較
        BResult<T> results = compareSheets(pairs, 5, 75);
        
        // 4. 比較結果の表示（テキスト）
        if (settings.get(AppSettingKeys.SHOW_RESULT_TEXT)) {
            showResultText(workDir, results, 75, 80);
        }
        
        // 5. 比較結果の表示（Excelブック）
        if (settings.get(AppSettingKeys.SHOW_PAINTED_SHEETS)) {
            showPaintedSheets(workDir, results, 80, 98);
        }
        
        // 6. 処理終了のアナウンス
        announceEnd();
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(int progressBefore, int progressAfter) {
        updateProgress(progressBefore, PROGRESS_MAX);
        
        Path bookPath1 = settings.get(AppSettingKeys.CURR_BOOK_PATH1);
        Path bookPath2 = settings.get(AppSettingKeys.CURR_BOOK_PATH2);
        
        if (menu == AppMenu.COMPARE_BOOKS) {
            str.append(String.format(
                    "ブック同士の比較を開始します。\n[A] %s\n[B] %s\n\n",
                    bookPath1, bookPath2));
            
        } else {
            String sheetName1 = settings.get(AppSettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(AppSettingKeys.CURR_SHEET_NAME2);
            
            if (bookPath1.equals(bookPath2)) {
                str.append(String.format(
                        "シート同士の比較を開始します。\n%s\n[A] %s\n[B] %s\n\n",
                        bookPath1, sheetName1, sheetName2));
            } else {
                str.append(String.format(
                        "シート同士の比較を開始します。\n[A] %s - %s\n[B] %s - %s\n\n",
                        bookPath1, sheetName1, bookPath2, sheetName2));
            }
        }
        updateMessage(str.toString());
        
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 1. 作業用ディレクトリの作成
    private Path createWorkDir(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        Path workDir = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            workDir = settings.get(AppSettingKeys.WORK_DIR_BASE)
                    .resolve(settings.get(AppSettingKeys.CURR_TIMESTAMP));
            str.append(String.format(
                    "作業用フォルダを作成しています...\n  - %s\n\n", workDir));
            updateMessage(str.toString());
            
            workDir = Files.createDirectories(workDir);
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return workDir;
            
        } catch (Exception e) {
            str.append(String.format("作業用フォルダの作成に失敗しました。\n\n"));
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "作業用フォルダの作成に失敗しました。\n" + workDir, e);
        }
    }
    
    // 2. 比較するシートの組み合わせの決定
    private List<Pair<String>> pairingSheets(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            List<Pair<String>> pairs;
            if (menu == AppMenu.COMPARE_BOOKS) {
                str.append("比較するシートの組み合わせを決定しています...\n");
                updateMessage(str.toString());
                
                pairs = menu.getSheetNamePairs(settings, factory);
                pairs.forEach(p -> str.append(sheetNamePair(p)));
                str.append("\n");
                
                updateMessage(str.toString());
                
            } else {
                pairs = menu.getSheetNamePairs(settings, factory);
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return pairs;
            
        } catch (Exception e) {
            str.append("シートの組み合わせ決定に失敗しました。\n\n");
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "シートの組み合わせ決定に失敗しました。", e);
        }
    }
    
    // 3. シート同士の比較
    private BResult<T> compareSheets(
            List<Pair<String>> pairs,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            Path bookPath1 = settings.get(AppSettingKeys.CURR_BOOK_PATH1);
            Path bookPath2 = settings.get(AppSettingKeys.CURR_BOOK_PATH2);
            SheetLoader<T> loader1 = factory.sheetLoader(settings, bookPath1);
            SheetLoader<T> loader2 = bookPath1.equals(bookPath2)
                    ? loader1
                    : factory.sheetLoader(settings, bookPath2);
            SComparator<T> comparator = factory.comparator(settings);
            
            Map<Pair<String>, SResult<T>> results = new HashMap<>();
            List<Pair<String>> pairedPairs = pairs.stream()
                    .filter(Pair::isPaired)
                    .collect(Collectors.toList());
            
            int total = progressAfter - progressBefore;
            int i = 0;
            for (Pair<String> pair : pairedPairs) {
                i++;
                
                str.append(String.format(
                        "シートを比較しています(%d/%d)...\n%s",
                        i, pairedPairs.size(), sheetNamePair(pair)));
                updateMessage(str.toString());
                
                Set<CellReplica<T>> cells1 = loader1.loadCells(bookPath1, pair.a());
                Set<CellReplica<T>> cells2 = loader2.loadCells(bookPath2, pair.b());
                SResult<T> result = comparator.compare(cells1, cells2);
                results.put(pair, result);
                
                str.append(result.getSummary().indent(8)).append(BR);
                updateMessage(str.toString());
                updateProgress(progressBefore + total * i / pairedPairs.size(), PROGRESS_MAX);
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return BResult.of(bookPath1, bookPath2, pairs, results);
            
        } catch (Exception e) {
            str.append(String.format("シートの比較に失敗しました。\n\n"));
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException("シートの比較に失敗しました。", e);
        }
    }
    
    // 4. 比較結果の表示（テキスト）
    private void showResultText(
            Path workDir,
            BResult<T> results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        Path textPath = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            textPath = workDir.resolve("result.txt");
            
            str.append(String.format(
                    "比較結果テキストを保存して表示しています...\n  - %s\n\n", textPath));
            updateMessage(str.toString());
            
            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(results.toString());
            }
            Desktop.getDesktop().open(textPath.toFile());
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(String.format("比較結果テキストの保存と表示に失敗しました。\n\n"));
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "比較結果テキストの保存と表示に失敗しました。\n" + textPath, e);
        }
    }
    
    // 5. 比較結果の表示（Excelブック）
    private void showPaintedSheets(
            Path workDir,
            BResult<T> results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        boolean isSameBook = Objects.equals(
                settings.get(AppSettingKeys.CURR_BOOK_PATH1),
                settings.get(AppSettingKeys.CURR_BOOK_PATH2));
        
        if (isSameBook) {
            showPaintedSheets1(workDir, results, progressBefore, progressAfter);
        } else {
            showPaintedSheets2(workDir, results, progressBefore, progressAfter);
        }
    }
    
    private void showPaintedSheets1(
            Path workDir,
            BResult<T> results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            int progressTotal = progressAfter - progressBefore;
            
            str.append("Excelブックに比較結果の色を付けて保存しています...\n");
            updateMessage(str.toString());
            Path src = settings.get(AppSettingKeys.CURR_BOOK_PATH1);
            Path dst = workDir.resolve(src.getFileName());
            BookPainter painter = factory.painter(settings, dst);
            str.append(String.format("  - %s\n\n", dst));
            updateMessage(str.toString());
            
            Map<String, SResult.Piece<T>> result = new HashMap<>(results.getPiece(Side.A));
            result.putAll(results.getPiece(Side.B));
            painter.paintAndSave(src, dst, result);
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
            str.append("比較結果のExcelブックを表示しています...\n\n");
            updateMessage(str.toString());
            Desktop.getDesktop().open(dst.toFile());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(String.format("Excelブックへの着色、保存、表示に失敗しました。\n\n"));
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "Excelブックへの着色、保存、表示に失敗しました。", e);
        }
    }
    
    private void showPaintedSheets2(
            Path workDir,
            BResult<T> results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            int progressTotal = progressAfter - progressBefore;
            
            str.append("Excelブックに比較結果の色を付けて保存しています(1/2)...\n");
            updateMessage(str.toString());
            Path src1 = settings.get(AppSettingKeys.CURR_BOOK_PATH1);
            Path dst1 = workDir.resolve("【A】" + src1.getFileName());
            BookPainter painter1 = factory.painter(settings, dst1);
            str.append(String.format("  - %s\n\n", dst1));
            updateMessage(str.toString());
            painter1.paintAndSave(src1, dst1, results.getPiece(Side.A));
            updateProgress(progressBefore + progressTotal * 2 / 5, PROGRESS_MAX);
            
            str.append("Excelブックに比較結果の色を付けて保存しています(2/2)...\n");
            updateMessage(str.toString());
            Path src2 = settings.get(AppSettingKeys.CURR_BOOK_PATH2);
            Path dst2 = workDir.resolve("【B】" + src2.getFileName());
            BookPainter painter2 = factory.painter(settings, dst2);
            str.append(String.format("  - %s\n\n", dst2));
            updateMessage(str.toString());
            painter2.paintAndSave(src2, dst2, results.getPiece(Side.B));
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
            str.append("比較結果のExcelブックを表示しています...\n\n");
            updateMessage(str.toString());
            Desktop.getDesktop().open(dst1.toFile());
            Desktop.getDesktop().open(dst2.toFile());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(String.format("Excelブックへの着色、保存、表示に失敗しました。\n\n"));
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "Excelブックへの着色、保存、表示に失敗しました。", e);
        }
    }
    
    // 6. 処理終了のアナウンス
    private void announceEnd() {
        str.append("処理が完了しました。");
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
