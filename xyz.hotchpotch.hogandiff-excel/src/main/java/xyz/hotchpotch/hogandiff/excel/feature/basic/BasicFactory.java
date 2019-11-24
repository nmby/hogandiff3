package xyz.hotchpotch.hogandiff.excel.feature.basic;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.CellReplica.CellContentType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
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
 * セルの内容を文字列として比較するためのファクトリです。<br>
 *
 * @author nmby
 */
public class BasicFactory implements Factory {
    
    // [static members] ********************************************************
    
    public static final CellContentType<String> normalStringContent = new CellContentType<>() {
        
        @Override
        public String tag() {
            // TODO 後で見直す
            return "";
        }
    };
    
    /**
     * 新しいファクトリを返します。<br>
     * 
     * @return 新しいファクトリ
     */
    public static Factory of() {
        return new BasicFactory();
    }
    
    // [instance members] ******************************************************
    
    private BasicFactory() {
    }
    
    @Override
    public Set<CellContentType<?>> targetContentTypes() {
        return Set.of(normalStringContent);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} が不明な形式のファイルの場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポートされないブック形式の場合
     */
    @Override
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
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} が不明な形式のファイルの場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポートされないブック形式の場合
     */
    @Override
    public SheetLoader sheetLoader(Settings settings, Path bookPath)
            throws ExcelHandlingException {
        
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
                            cell.getColumnIndex(),
                            normalStringContent,
                            data)
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
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    @Override
    public SComparator comparator(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean considerRowGaps = settings.get(SettingKeys.CONSIDER_ROW_GAPS);
        boolean considerColumnGaps = settings.get(SettingKeys.CONSIDER_COLUMN_GAPS);
        
        return SComparatorImpl.of(
                considerRowGaps,
                considerColumnGaps,
                normalStringContent,
                (cell1, cell2) -> {
                    String value1 = Optional
                            .ofNullable(cell1)
                            .map(c -> c.getContent(normalStringContent))
                            .orElse("");
                    String value2 = Optional
                            .ofNullable(cell2)
                            .map(c -> c.getContent(normalStringContent))
                            .orElse("");
                    return !Objects.equals(value1, value2);
                });
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポートされないブック形式の場合
     */
    @Override
    public BookPainter painter(Settings settings, Path bookPath)
            throws ExcelHandlingException {
        
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
