package xyz.hotchpotch.hogandiff.excel.feature.common;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のローダーで順に処理を行う {@link SheetLoader} の実装です。<br>
 *
 * @param <T> セルデータの型
 * @author nmby
 */
@BookHandler
@SheetHandler
public class CombinedSheetLoader<T> implements SheetLoader<T> {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param <T> セルデータの型
     * @param suppliers このローダーを構成するローダーたちのサプライヤ
     * @return 新しいローダー
     * @throws NullPointerException {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static <T> SheetLoader<T> of(List<UnsafeSupplier<SheetLoader<T>>> suppliers) {
        Objects.requireNonNull(suppliers, "suppliers");
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new CombinedSheetLoader<>(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<SheetLoader<T>>> suppliers;
    
    private CombinedSheetLoader(List<UnsafeSupplier<SheetLoader<T>>> suppliers) {
        assert suppliers != null;
        
        this.suppliers = List.copyOf(suppliers);
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、構成時に指定されたローダーを使って処理を行います。<br>
     * 一つ目のローダーで処理を行い、正常に終了したらその結果を返します。
     * 失敗したら二つ目のローダーで処理を行い、正常に終了したらその結果を返します。
     * 以下同様に処理を行い、
     * 全てのローダーで処理が失敗したら例外をスローします。<br>
     * 
     * @throws NullPointerException
     *              {@code bookPath}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookpath} がサポート対象外の形式もしくは不明な形式の場合
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
        
        ExcelHandlingException failed = new ExcelHandlingException(String.format(
                "処理に失敗しました：%s - %s", bookPath, sheetName));
        
        Iterator<UnsafeSupplier<SheetLoader<T>>> itr = suppliers.iterator();
        while (itr.hasNext()) {
            try {
                SheetLoader<T> loader = itr.next().get();
                return loader.loadCells(bookPath, sheetName);
            } catch (Exception e) {
                e.printStackTrace();
                failed.addSuppressed(e);
            }
        }
        throw failed;
    }
}
