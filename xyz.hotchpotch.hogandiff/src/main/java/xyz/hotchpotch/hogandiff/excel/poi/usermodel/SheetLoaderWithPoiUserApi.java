package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link SheetLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS, BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class SheetLoaderWithPoiUserApi implements SheetLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @param converter セル変換関数
     * @return 新しいローダー
     * @throws NullPointerException {@code extractContents} が {@code true}
     *                               かつ {@code converter} が {@code null} の場合
     * @throw IllegalArgumentException {@code extractContents} が {@code false}
     *                               かつ {@code converter} が {@code null} 以外の場合
     */
    public static SheetLoader of(
            boolean saveMemory,
            Function<Cell, CellData> converter) {
        
        Objects.requireNonNull(converter, "converter");
        
        return new SheetLoaderWithPoiUserApi(
                saveMemory,
                converter);
    }
    
    // [instance members] ******************************************************
    
    private final boolean saveMemory;
    private final Function<Cell, CellData> converter;
    
    private SheetLoaderWithPoiUserApi(
            boolean saveMemory,
            Function<Cell, CellData> converter) {
        
        assert converter != null;
        
        this.saveMemory = saveMemory;
        this.converter = converter;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookInfo}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookInfo} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellData> loadCells(BookInfo bookInfo, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookInfo, "bookInfo");
        Objects.requireNonNull(sheetName, "sheetName");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), bookInfo.bookType());
        
        try (Workbook wb = WorkbookFactory.create(bookInfo.bookPath().toFile())) {
            
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException(
                        "シートが存在しません：%s - %s".formatted(bookInfo.bookPath(), sheetName));
            }
            
            Set<SheetType> possibleTypes = PoiUtil.possibleTypes(sheet);
            // 同じく、後続の catch でさらに ExcelHandlingException にラップする。
            CommonUtil.ifNotSupportedSheetTypeThenThrow(getClass(), possibleTypes);
            
            Set<CellData> cells = StreamSupport.stream(sheet.spliterator(), true)
                    .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                    .map(converter::apply)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            
            Map<String, CellData> cellsMap = cells.parallelStream()
                    .collect(Collectors.toMap(
                            CellData::address,
                            Function.identity()));
            
            sheet.getCellComments().forEach((addr, comm) -> {
                String address = addr.formatAsString();
                // xlsx/xlsm 形式の場合、空コメントから null が返されるため、空文字列に標準化する。
                String comment = Optional.ofNullable(comm.getString().getString()).orElse("");
                
                if (cellsMap.containsKey(address)) {
                    CellData original = cellsMap.get(address);
                    cells.remove(original);
                    cells.add(original.withComment(comment));
                } else {
                    cells.add(CellData.of(address, "", saveMemory).withComment(comment));
                }
            });
            
            return cells;
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "処理に失敗しました：%s - %s".formatted(bookInfo.bookPath(), sheetName),
                    e);
        }
    }
}
