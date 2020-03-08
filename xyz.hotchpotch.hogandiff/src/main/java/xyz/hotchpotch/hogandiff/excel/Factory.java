package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.excel.feature.basic.eventmodel.HSSFSheetLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.feature.basic.sax.XSSFSheetLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.XSSFBookPainterWithStax;
import xyz.hotchpotch.hogandiff.excel.feature.basic.usermodel.BookPainterWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.feature.common.CombinedBookLoader;
import xyz.hotchpotch.hogandiff.excel.feature.common.CombinedBookPainter;
import xyz.hotchpotch.hogandiff.excel.feature.common.CombinedSheetLoader;
import xyz.hotchpotch.hogandiff.excel.feature.common.SComparatorImpl;
import xyz.hotchpotch.hogandiff.excel.feature.common.eventmodel.HSSFBookLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.feature.common.sax.XSSFBookLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.feature.common.usermodel.BookLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.feature.common.usermodel.SheetLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.util.PoiUtil;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理に必要な一連の機能を提供するファクトリです。<br>
 *
 * @author nmby
 */
public class Factory {
    
    // [static members] ********************************************************
    
    /**
     * 新しいファクトリを返します。<br>
     * 
     * @return 新しいファクトリ
     */
    public static Factory of() {
        return new Factory();
    }
    
    // [instance members] ******************************************************
    
    private Factory() {
    }
    
    /**
     * Excelブックからシート名の一覧を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} が不明な形式のファイルの場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポートされないブック形式の場合
     */
    public BookLoader bookLoader(Path bookPath) throws ExcelHandlingException {
        Objects.requireNonNull(bookPath, "bookPath");
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        BookType bookType = BookType.of(bookPath);
        switch (bookType) {
        case XLS:
            return CombinedBookLoader.of(List.of(
                    () -> HSSFBookLoaderWithPoiEventApi.of(targetSheetTypes),
                    () -> BookLoaderWithPoiUserApi.of(targetSheetTypes)));
        
        case XLSX:
        case XLSM:
            return CombinedBookLoader.of(List.of(
                    () -> XSSFBookLoaderWithSax.of(targetSheetTypes),
                    () -> BookLoaderWithPoiUserApi.of(targetSheetTypes)));
        
        case XLSB:
            // FIXME: [No.2 .xlsbのサポート]
            throw new UnsupportedOperationException(".xlsb 形式はサポート対象外です。");
        
        default:
            throw new AssertionError("unknown book type: " + bookType);
        }
    }
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excelブックのパス
     * @return Excelシートからセルデータを抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} が不明な形式のファイルの場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポートされないブック形式の場合
     */
    public SheetLoader sheetLoader(Settings settings, Path bookPath) throws ExcelHandlingException {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookPath, "bookPath");
        
        // 設計メモ：
        // Settings を扱うのは Factory の層までとし、これ以下の各機能へは
        // Settings 丸ごとではなく、必要な個別のパラメータを渡すこととする。
        
        boolean useCachedValue = !settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING);
        Function<Cell, CellReplica> converter = cell -> {
            String data = PoiUtil.getCellContentAsString(cell, useCachedValue);
            return data != null && !"".equals(data)
                    ? CellReplica.of(
                            cell.getRowIndex(),
                            cell.getColumnIndex(), data)
                    : null;
        };
        
        BookType bookType = BookType.of(bookPath);
        switch (bookType) {
        case XLS:
            return CombinedSheetLoader.of(List.of(
                    () -> HSSFSheetLoaderWithPoiEventApi.of(useCachedValue),
                    () -> SheetLoaderWithPoiUserApi.of(converter)));
        
        case XLSX:
        case XLSM:
            return CombinedSheetLoader.of(List.of(
                    () -> XSSFSheetLoaderWithSax.of(useCachedValue, bookPath),
                    () -> SheetLoaderWithPoiUserApi.of(converter)));
        
        case XLSB:
            // FIXME: [No.2 .xlsbのサポート]
            throw new UnsupportedOperationException(".xlsb 形式はサポート対象外です。");
        
        default:
            throw new AssertionError("unknown book type: " + bookType);
        }
    }
    
    /**
     * 2つのExcelシートから抽出したセルセット同士を比較するコンパレータを返します。<br>
     * 
     * @param settings 設定
     * @return セルセット同士を比較するコンパレータ
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public SComparator comparator(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean considerRowGaps = settings.get(SettingKeys.CONSIDER_ROW_GAPS);
        boolean considerColumnGaps = settings.get(SettingKeys.CONSIDER_COLUMN_GAPS);
        
        return SComparatorImpl.of(considerRowGaps, considerColumnGaps);
    }
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excelブックのパス
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public BookPainter painter(Settings settings, Path bookPath) throws ExcelHandlingException {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookPath, "bookPath");
        
        short redundantColor = settings.get(SettingKeys.REDUNDANT_COLOR);
        short diffColor = settings.get(SettingKeys.DIFF_COLOR);
        
        BookType bookType = BookType.of(bookPath);
        switch (bookType) {
        case XLS:
            return CombinedBookPainter.of(List.of(
                    // FIXME: [No.3 着色関連] 形式特化型ペインターも実装して追加する
                    () -> BookPainterWithPoiUserApi.of(redundantColor, diffColor)));
        
        case XLSX:
        case XLSM:
            return CombinedBookPainter.of(List.of(
                    () -> XSSFBookPainterWithStax.of(redundantColor, diffColor),
                    () -> BookPainterWithPoiUserApi.of(redundantColor, diffColor)));
        
        case XLSB:
            // FIXME: [No.2 .xlsbのサポート]
            throw new UnsupportedOperationException(".xlsb 形式はサポート対象外です。");
        
        default:
            throw new AssertionError("unknown book type: " + bookType);
        }
    }
}
