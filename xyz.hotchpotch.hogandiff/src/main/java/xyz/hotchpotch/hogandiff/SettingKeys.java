package xyz.hotchpotch.hogandiff;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.IndexedColors;

import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * このアプリケーションの設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class SettingKeys {
    
    // [static members] ********************************************************
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Path> WORK_DIR_BASE = Key.defineAs(
            "application.system.workDirBase",
            () -> Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "xyz.hotchpotch.hogandiff"),
            Path::toString,
            Path::of,
            false);
    
    /** 今回の実行を識別するためのタイムスタンプタグ */
    public static final Key<String> CURR_TIMESTAMP = Key.defineAs(
            "application.current.timestamp",
            () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")),
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較メニュー */
    public static final Key<AppMenu> CURR_MENU = Key.defineAs(
            "application.current.menu",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            AppMenu::toString,
            AppMenu::valueOf,
            false);
    
    /** 今回の実行における比較対象Excelブック1のパス */
    public static final Key<Path> CURR_BOOK_PATH1 = Key.defineAs(
            "application.current.bookPath1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of,
            false);
    
    /** 今回の実行における比較対象Excelブック2のパス */
    public static final Key<Path> CURR_BOOK_PATH2 = Key.defineAs(
            "application.current.bookPath2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of,
            false);
    
    /** 今回の実行における比較対象Excelシート1の名前 */
    public static final Key<String> CURR_SHEET_NAME1 = Key.defineAs(
            "application.current.sheetName1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較対象Excelシート2の名前 */
    public static final Key<String> CURR_SHEET_NAME2 = Key.defineAs(
            "application.current.sheetName2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity(),
            false);
    
    /**
     * Excelシート同士の比較において、
     * 行の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_ROW_GAPS = Key.defineAs(
            "compare.considerRowGaps",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelシート同士の比較において、
     * 列の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_COLUMN_GAPS = Key.defineAs(
            "compare.considerColumnGaps",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセルの比較において、セル内容を比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_CONTENTS = Key.defineAs(
            "compare.compareCellContents",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセルの比較において、セルコメントを比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_COMMENTS = Key.defineAs(
            "compare.compareCellComments",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセル内容の比較において、セルの内容が数式の場合に
     * 数式文字列を比較する（{@code true}）か
     * Excelファイルにキャッシュされている計算結果の値を比較する（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> COMPARE_ON_FORMULA_STRING = Key.defineAs(
            "compare.compareOnFormulaString",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * 比較結果のレポートにおいて、余剰行・余剰列に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> REDUNDANT_COLOR = Key.defineAs(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルに着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> DIFF_COLOR = Key.defineAs(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_COMMENT_COLOR = Key.defineAs(
            "report.redundantCommentColor",
            () -> new Color(255, 128, 128),
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_COMMENT_COLOR = Key.defineAs(
            "report.diffCommentColor",
            () -> Color.YELLOW,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** レポートオプション：差分個所に色を付けたシートを表示するか */
    public static final Key<Boolean> SHOW_PAINTED_SHEETS = Key.defineAs(
            "application.report.showPaintedSheets",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** レポートオプション：比較結果が記載されたテキストを表示するか */
    public static final Key<Boolean> SHOW_RESULT_TEXT = Key.defineAs(
            "application.report.showResultText",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：比較完了時にこのアプリを終了するか */
    public static final Key<Boolean> EXIT_WHEN_FINISHED = Key.defineAs(
            "application.execution.exitWhenFinished",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
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
    
    public static Set<Key<?>> storableKeys() {
        return keys.stream().filter(Key::storable).collect(Collectors.toSet());
    }
    
    // [instance members] ******************************************************
    
    private SettingKeys() {
    }
}
