package xyz.hotchpotch.hogandiff;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import xyz.hotchpotch.hogandiff.excel.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * このアプリケーションの設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class AppSettingKeys {
    
    // [static members] ********************************************************
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Path> WORK_DIR_BASE = Key.defineAs(
            "application.system.workDirBase",
            () -> Path.of(
                    System.getProperty("java.io.tmpdir"),
                    AppSettingKeys.class.getPackage().getName()),
            Path::toString,
            Path::of);
    
    /** 今回の実行を識別するためのタイムスタンプタグ */
    public static final Key<String> CURR_TIMESTAMP = Key.defineAs(
            "application.current.timestamp",
            () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")),
            Function.identity(),
            Function.identity());
    
    /** 今回の実行における比較メニュー */
    public static final Key<AppMenu> CURR_MENU = Key.defineAs(
            "application.current.menu",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            AppMenu::toString,
            AppMenu::valueOf);
    
    /** 今回の実行における比較対象Excelブック1のパス */
    public static final Key<Path> CURR_BOOK_PATH1 = Key.defineAs(
            "application.current.bookPath1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of);
    
    /** 今回の実行における比較対象Excelブック2のパス */
    public static final Key<Path> CURR_BOOK_PATH2 = Key.defineAs(
            "application.current.bookPath2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of);
    
    /** 今回の実行における比較対象Excelシート1の名前 */
    public static final Key<String> CURR_SHEET_NAME1 = Key.defineAs(
            "application.current.sheetName1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity());
    
    /** 今回の実行における比較対象Excelシート2の名前 */
    public static final Key<String> CURR_SHEET_NAME2 = Key.defineAs(
            "application.current.sheetName2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity());
    
    /** レポートオプション：差分個所に色を付けたシートを表示するか */
    public static final Key<Boolean> SHOW_PAINTED_SHEETS = Key.defineAs(
            "application.report.showPaintedSheets",
            () -> true,
            String::valueOf,
            Boolean::valueOf);
    
    /** レポートオプション：比較結果が記載されたテキストを表示するか */
    public static final Key<Boolean> SHOW_RESULT_TEXT = Key.defineAs(
            "application.report.showResultText",
            () -> true,
            String::valueOf,
            Boolean::valueOf);
    
    /** 実行オプション：比較完了時にこのアプリを終了するか */
    public static final Key<Boolean> EXIT_WHEN_FINISHED = Key.defineAs(
            "application.execution.exitWhenFinished",
            () -> false,
            String::valueOf,
            Boolean::valueOf);
    
    // Collectors#toSet は実態として immutable set を返してくれるはずだが
    // 保証されないということなので、一応 Set#copyOf でラップしておく。
    private static final Set<Key<?>> keys = Set.copyOf(
            Stream.of(SettingKeys.class.getFields())
                    .filter(f -> f.getType() == Key.class && Modifier.isPublic(f.getModifiers()))
                    .map(f -> {
                        try {
                            return (Key<?>) f.get(null);
                        } catch (IllegalAccessException e) {
                            throw new AssertionError(e);
                        }
                    })
                    .collect(Collectors.toSet()));
    
    /**
     * 全ての定義済み設定項目を含むセットを返します。<br>
     * 
     * @return 全ての定義済み設定項目を含むセット
     */
    public static Set<Key<?>> keySet() {
        return keys;
    }
    
    // [instance members] ******************************************************
    
    private AppSettingKeys() {
    }
}
