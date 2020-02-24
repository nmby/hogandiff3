package xyz.hotchpotch.hogandiff.excel;

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
     * 比較結果のレポートにおいて、余剰個所に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> REDUNDANT_COLOR = Key.defineAs(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf);
    
    /**
     * 比較結果のレポートにおいて、差分個所に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> DIFF_COLOR = Key.defineAs(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf);
    
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
