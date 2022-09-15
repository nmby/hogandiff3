package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.common.CombinedBookLoader;
import xyz.hotchpotch.hogandiff.excel.common.CombinedBookPainter;
import xyz.hotchpotch.hogandiff.excel.common.CombinedSheetLoader;
import xyz.hotchpotch.hogandiff.excel.common.SComparatorImpl;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFBookLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFSheetLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookPainterWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.PoiUtil;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.SheetLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFBookLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFSheetLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.stax.XSSFBookPainterWithStax;
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
     * @param bookInfo Excelブックの情報
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code bookInfo} が {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookInfo} がサポート対象外の形式の場合
     */
    public BookLoader bookLoader(BookInfo bookInfo) throws ExcelHandlingException {
        Objects.requireNonNull(bookInfo, "bookInfo");
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        switch (bookInfo.bookType()) {
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
            throw new AssertionError("unknown book type: " + bookInfo.bookType());
        }
    }
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookInfo Excelブックの情報
     * @return Excelシートからセルデータを抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookInfo} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookInfo} がサポート対象外の形式の場合
     */
    public SheetLoader sheetLoader(Settings settings, BookInfo bookInfo) throws ExcelHandlingException {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookInfo, "bookInfo");
        
        // 設計メモ：
        // Settings を扱うのは Factory の層までとし、これ以下の各機能へは
        // Settings 丸ごとではなく、必要な個別のパラメータを渡すこととする。
        
        boolean useCachedValue = !settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING);
        boolean saveMemory = settings.get(SettingKeys.SAVE_MEMORY);
        boolean speedFirst = settings.get(SettingKeys.SPEED_FIRST);
        
        Function<Cell, CellData> converter = cell -> {
            String content = PoiUtil.getCellContentAsString(cell, useCachedValue);
            return "".equals(content)
                    ? null
                    : CellData.of(
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            content,
                            saveMemory);
        };
        
        switch (bookInfo.bookType()) {
        case XLS:
            return CombinedSheetLoader.of(List.of(
                    () -> HSSFSheetLoaderWithPoiEventApi.of(
                            useCachedValue,
                            saveMemory),
                    () -> SheetLoaderWithPoiUserApi.of(
                            saveMemory,
                            converter)));
        
        case XLSX:
        case XLSM:
            return speedFirst
                    ? CombinedSheetLoader.of(List.of(
                            () -> XSSFSheetLoaderWithSax.of(
                                    useCachedValue,
                                    saveMemory,
                                    bookInfo),
                            () -> SheetLoaderWithPoiUserApi.of(
                                    saveMemory,
                                    converter)))
                    : SheetLoaderWithPoiUserApi.of(
                            saveMemory,
                            converter);
        
        case XLSB:
            // FIXME: [No.2 .xlsbのサポート]
            throw new UnsupportedOperationException(".xlsb 形式はサポート対象外です。");
        
        default:
            throw new AssertionError("unknown book type: " + bookInfo.bookType());
        }
    }
    
    /**
     * 2つのExcelブックに含まれるシート名の対応付けを行うマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return シート名の対応付けを行うマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public Matcher<String> sheetNameMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        return settings.get(SettingKeys.MATCH_NAMES_STRICTLY)
                ? Matcher.identityMatcher()
                : Matcher.nerutonMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance);
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
        boolean saveMemory = settings.get(SettingKeys.SAVE_MEMORY);
        
        return SComparatorImpl.of(
                considerRowGaps,
                considerColumnGaps,
                saveMemory);
    }
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param settings 設定
     * @param bookInfo Excelブックの情報
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookInfo} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookInfo} がサポート対象外の形式の場合
     */
    public BookPainter painter(Settings settings, BookInfo bookInfo) throws ExcelHandlingException {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookInfo, "bookInfo");
        
        short redundantColor = settings.get(SettingKeys.REDUNDANT_COLOR);
        short diffColor = settings.get(SettingKeys.DIFF_COLOR);
        Color redundantCommentColor = settings.get(SettingKeys.REDUNDANT_COMMENT_COLOR);
        Color diffCommentColor = settings.get(SettingKeys.DIFF_COMMENT_COLOR);
        // もうなんか滅茶苦茶や・・・
        String redundantCommentHex = "#" + SettingKeys.REDUNDANT_COMMENT_COLOR.encoder().apply(redundantCommentColor);
        String diffCommentHex = "#" + SettingKeys.DIFF_COMMENT_COLOR.encoder().apply(diffCommentColor);
        Color redundantSheetColor = settings.get(SettingKeys.REDUNDANT_SHEET_COLOR);
        Color diffSheetColor = settings.get(SettingKeys.DIFF_SHEET_COLOR);
        Color sameSheetColor = settings.get(SettingKeys.SAME_SHEET_COLOR);
        
        switch (bookInfo.bookType()) {
        case XLS:
            return CombinedBookPainter.of(List.of(
                    // FIXME: [No.3 着色関連] 形式特化型ペインターも実装して追加する
                    () -> BookPainterWithPoiUserApi.of(
                            redundantColor,
                            diffColor,
                            redundantCommentColor,
                            diffCommentColor,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor)));
        
        case XLSX:
        case XLSM:
            return CombinedBookPainter.of(List.of(
                    () -> XSSFBookPainterWithStax.of(
                            redundantColor,
                            diffColor,
                            redundantCommentHex,
                            diffCommentHex,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor),
                    () -> BookPainterWithPoiUserApi.of(
                            redundantColor,
                            diffColor,
                            redundantCommentColor,
                            diffCommentColor,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor)));
        
        case XLSB:
            // FIXME: [No.2 .xlsbのサポート]
            throw new UnsupportedOperationException(".xlsb 形式はサポート対象外です。");
        
        default:
            throw new AssertionError("unknown book type: " + bookInfo.bookType());
        }
    }
}
