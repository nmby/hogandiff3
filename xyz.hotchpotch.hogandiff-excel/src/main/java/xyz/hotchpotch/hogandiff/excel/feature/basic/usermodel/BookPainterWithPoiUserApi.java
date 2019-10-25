package xyz.hotchpotch.hogandiff.excel.feature.basic.usermodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.PoiUtil;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートに着色を行う
 * {@link BookPainter} の実装です。<br>
 * 
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM, BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class BookPainterWithPoiUserApi implements BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param redundantColor 余剰個所に着ける色のインデックス値
     * @param diffColor 差分個所に着ける色のインデックス値
     * @return 新たなペインター
     */
    public static BookPainter of(
            short redundantColor,
            short diffColor) {
        
        return new BookPainterWithPoiUserApi(
                redundantColor,
                diffColor);
    }
    
    // [instance members] ******************************************************
    
    private final short redundantColor;
    private final short diffColor;
    
    private BookPainterWithPoiUserApi(
            short redundantColor,
            short diffColor) {
        
        this.redundantColor = redundantColor;
        this.diffColor = diffColor;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code srcBookPath}, {@code dstBookPath}, {@code diffs}
     *              のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} がサポート対象外の形式もしくは不明な形式の場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} と {@code dstBookPath} が同じパスの場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} と {@code dstBookPath} の形式が異なる場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public <T> void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            Map<String, Piece<T>> diffs)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(srcBookPath, "srcBookPath");
        Objects.requireNonNull(dstBookPath, "dstBookPath");
        Objects.requireNonNull(diffs, "diffs");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(srcBookPath));
        if (srcBookPath.equals(dstBookPath)) {
            throw new IllegalArgumentException(String.format(
                    "異なるパスを指定する必要があります：%s -> %s", srcBookPath, dstBookPath));
        }
        if (BookType.of(srcBookPath) != BookType.of(dstBookPath)) {
            throw new IllegalArgumentException(String.format(
                    "拡張子が異なります：%s -> %s", srcBookPath, dstBookPath));
        }
        
        // 1. 目的のブックをコピーする。
        try {
            Files.copy(srcBookPath, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);
            
        } catch (Exception e) {
            try {
                Files.deleteIfExists(dstBookPath);
            } catch (IOException e1) {
                e.addSuppressed(e1);
            }
            throw new ExcelHandlingException(String.format(
                    "Excelファイルのコピーに失敗しました：%s -> %s",
                    srcBookPath, dstBookPath), e);
        }
        
        // 2. コピーしたファイルをExcelブックとしてロードする。
        try (InputStream is = Files.newInputStream(dstBookPath);
                Workbook book = WorkbookFactory.create(is)) {
            
            // 3. まず、全ての色をクリアする。
            PoiUtil.clearAllColors(book);
            
            // 4. 差分個所に色を付ける。
            diffs.forEach((sheetName, piece) -> {
                Sheet sheet = book.getSheet(sheetName);
                PoiUtil.paintRows(sheet, piece.redundantRows(), redundantColor);
                PoiUtil.paintColumns(sheet, piece.redundantColumns(), redundantColor);
                
                Set<CellAddress> addresses = piece.diffCells().stream()
                        .map(c -> new CellAddress(c.row(), c.column()))
                        .collect(Collectors.toSet());
                PoiUtil.paintCells(sheet, addresses, diffColor);
            });
            
            // 5. Excelブックを上書き保存する。
            try (OutputStream os = Files.newOutputStream(dstBookPath)) {
                book.write(os);
            }
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "Excelブックの着色と保存に失敗しました：" + dstBookPath, e);
        }
    }
}
