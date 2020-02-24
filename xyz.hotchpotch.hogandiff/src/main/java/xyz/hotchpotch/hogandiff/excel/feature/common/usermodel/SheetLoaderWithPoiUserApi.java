package xyz.hotchpotch.hogandiff.excel.feature.common.usermodel;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.PoiUtil;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link SheetLoader} の実装です。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS, BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class SheetLoaderWithPoiUserApi<T> implements SheetLoader<T> {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param <T> セルデータの型
     * @param converter セル変換関数
     * @return 新しいローダー
     * @throws NullPointerException {@code converter} が {@code null} の場合
     */
    public static <T> SheetLoader<T> of(
            Function<Cell, CellReplica<T>> converter) {
        
        Objects.requireNonNull(converter, "converter");
        
        return new SheetLoaderWithPoiUserApi<>(converter);
    }
    
    // [instance members] ******************************************************
    
    private final Function<Cell, CellReplica<T>> converter;
    
    private SheetLoaderWithPoiUserApi(
            Function<Cell, CellReplica<T>> converter) {
        
        assert converter != null;
        
        this.converter = converter;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookPath}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellReplica<T>> loadCells(Path bookPath, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        Objects.requireNonNull(sheetName, "sheetName");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        try (Workbook wb = WorkbookFactory.create(bookPath.toFile())) {
            
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException(String.format(
                        "シートが存在しません：%s - %s", bookPath, sheetName));
            }
            
            Set<SheetType> possibleTypes = PoiUtil.possibleTypes(sheet);
            // 同じく、後続の catch でさらに ExcelHandlingException にラップする。
            CommonUtil.ifNotSupportedSheetTypeThenThrow(getClass(), possibleTypes);
            
            return StreamSupport.stream(sheet.spliterator(), true)
                    .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                    .map(converter::apply)
                    .filter(cell -> cell != null)
                    .collect(Collectors.toSet());
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
