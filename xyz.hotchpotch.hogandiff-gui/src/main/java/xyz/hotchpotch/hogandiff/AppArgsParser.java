package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * アプリケーション実行時引数を解析してアプリケーション設定に変換するパーサーです。<br>
 *
 * @author nmby
 */
public class AppArgsParser {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /** このアプリケーションのコマンドライン起動時の使い方 */
    public static final String USAGE = ""
            + "方眼Diff.exe bookPath1 bookPath2 <OPTIONS>" + BR
            + BR
            + "<OPTIONS>" + BR
            + "    --consider-row-gaps=[true|false]    : default value is true" + BR
            + "    --consider-column-gaps=[true|false] : default value is false" + BR
            + "    --compare-on-formulas=[true|false]  : default value is false" + BR
            + "    --show-painted-sheets=[true|false]  : default value is true" + BR
            + "    --show-result-text=[true|false]     : default value is true" + BR
            + "    --exit-when-finished=[true|false]   : default value is false" + BR
            + BR;
    
    private static final Map<String, Key<Boolean>> OPTIONS = Map.of(
            "--consider-row-gaps", SettingKeys.CONSIDER_ROW_GAPS,
            "--consider-column-gaps", SettingKeys.CONSIDER_COLUMN_GAPS,
            "--compare-on-formulas", SettingKeys.COMPARE_ON_FORMULA_STRING,
            "--show-painted-sheets", AppSettingKeys.SHOW_PAINTED_SHEETS,
            "--show-result-text", AppSettingKeys.SHOW_RESULT_TEXT,
            "--exit-when-finished", AppSettingKeys.EXIT_WHEN_FINISHED);
    
    /**
     * {@link #parseArgs(List)} と同じ。<br>
     * 詳しくは {@link #parseArgs(List)} の説明を参照してください。<br>
     * 
     * @param args アプリケーション実行時引数
     * @return アプリケーション設定。解析できない場合は空の {@link Optional}
     * @throws NullPointerException {@code args} が {@code null} の場合
     */
    public static Optional<Settings> parseArgs(String[] args) {
        Objects.requireNonNull(args, "args");
        return parseArgs(List.of(args));
    }
    
    /**
     * アプリケーション実行時引数を解析してアプリケーション設定に変換します。<br>
     * アプリケーション実行時引数の一部でも解析できない部分がある場合は、
     * 空の {@link Optional} を返します。<br>
     * 
     * @param args アプリケーション実行時引数
     * @return アプリケーション設定。解析できない場合は空の {@link Optional}
     * @throws NullPointerException {@code args} が {@code null} の場合
     */
    public static Optional<Settings> parseArgs(List<String> args) {
        Objects.requireNonNull(args, "args");
        if (args.size() < 2) {
            return Optional.empty();
        }
        
        try {
            // 比較メニューと比較対象Excelブックパスのパース
            Settings.Builder builder = Settings.builder()
                    .set(AppSettingKeys.CURR_MENU, AppMenu.COMPARE_BOOKS)
                    .set(AppSettingKeys.CURR_BOOK_PATH1, Path.of(args.get(0)))
                    .set(AppSettingKeys.CURR_BOOK_PATH2, Path.of(args.get(1)));
            
            // オプションのパース
            Deque<String> remainingParams = new ArrayDeque<>(args.subList(2, args.size()));
            Map<String, Key<Boolean>> remainingOptions = new HashMap<>(OPTIONS);
            while (!remainingParams.isEmpty() && !remainingOptions.isEmpty()) {
                String[] keyValue = remainingParams.removeFirst().split("=", 2);
                if (!remainingOptions.containsKey(keyValue[0])) {
                    return Optional.empty();
                }
                if (!"true".equals(keyValue[1]) && !"false".equals(keyValue[1])) {
                    return Optional.empty();
                }
                builder.set(remainingOptions.remove(keyValue[0]), Boolean.valueOf(keyValue[1]));
            }
            
            // 解析不能なパラメータが残っている場合はエラー（解析失敗）とする。
            if (!remainingParams.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(builder.build());
            
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
    
    // [instance members] ******************************************************
    
    private AppArgsParser() {
    }
}
