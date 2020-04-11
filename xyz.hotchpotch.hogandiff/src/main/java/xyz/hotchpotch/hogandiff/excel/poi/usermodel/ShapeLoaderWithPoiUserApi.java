package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.ShapeLoader;
import xyz.hotchpotch.hogandiff.excel.ShapeReplica;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートから
 * 図形データを抽出する {@link ShapeLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS, BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class ShapeLoaderWithPoiUserApi implements ShapeLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private ShapeLoaderWithPoiUserApi() {
    }
    
    @Override
    public Set<ShapeReplica> loadShapes(Path bookPath, String sheetName)
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
            
            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                return Set.of();
            }
            
            return null;
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
