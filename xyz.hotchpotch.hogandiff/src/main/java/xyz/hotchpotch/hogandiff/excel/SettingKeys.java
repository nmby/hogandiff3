package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.IndexedColors;

import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * Excelシートの比較における設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class SettingKeys {
    
    // [static members] ********************************************************
    
    /**
     * Excelシート同士の比較において、
     * 行の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_ROW_GAPS = Key.defineAs(
            "compare.considerRowGaps",
            () -> true,
            String::valueOf,
            Boolean::valueOf);
    
    /**
     * Excelシート同士の比較において、
     * 列の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_COLUMN_GAPS = Key.defineAs(
            "compare.considerColumnGaps",
            () -> false,
            String::valueOf,
            Boolean::valueOf);
    
    /**
     * Excelセルの比較において、セル内容を比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_CONTENTS = Key.defineAs(
            "compare.compareCellContents",
            () -> true,
            String::valueOf,
            Boolean::valueOf);
    
    /**
     * Excelセルの比較において、セルコメントを比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_COMMENTS = Key.defineAs(
            "compare.compareCellComments",
            () -> true,
            String::valueOf,
            Boolean::valueOf);
    
    /**
     * Excelセル内容の比較において、セルの内容が数式の場合に
     * 数式文字列を比較する（{@code true}）か
     * Excelファイルにキャッシュされている計算結果の値を比較する（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> COMPARE_ON_FORMULA_STRING = Key.defineAs(
            "compare.compareOnFormulaString",
            () -> false,
            String::valueOf,
            Boolean::valueOf);
    
    /**
     * 比較結果のレポートにおいて、余剰行・余剰列に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> REDUNDANT_COLOR = Key.defineAs(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf);
    
    /**
     * 比較結果のレポートにおいて、差分セルに着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> DIFF_COLOR = Key.defineAs(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf);
    
    /**
     * 比較結果のレポートにおいて、余剰セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_COMMENT_COLOR = Key.defineAs(
            "report.redundantCommentColor",
            () -> new Color(255, 128, 128),
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode);
    
    /**
     * 比較結果のレポートにおいて、差分セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_COMMENT_COLOR = Key.defineAs(
            "report.diffCommentColor",
            () -> Color.YELLOW,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode);
    
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
    
    private SettingKeys() {
    }
}
