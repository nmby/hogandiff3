package xyz.hotchpotch.hogandiff.excel.feature.common;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のペインターで順に処理を行う {@link BookPainter} の実装です。<br>
 * 
 * @author nmby
 */
@BookHandler
@SheetHandler
public class CombinedBookPainter implements BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param suppliers このペインターを構成するペインターたちのサプライヤ
     * @return 新しいペインター
     * @throws NullPointerException {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static BookPainter of(List<UnsafeSupplier<BookPainter>> suppliers) {
        Objects.requireNonNull(suppliers);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new CombinedBookPainter(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<BookPainter>> suppliers;
    
    private CombinedBookPainter(List<UnsafeSupplier<BookPainter>> suppliers) {
        assert suppliers != null;
        
        this.suppliers = List.copyOf(suppliers);
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、構成時に指定されたペインターを使って処理を行います。<br>
     * 一つ目のペインターで処理を行い、正常に終了したらそのまま終了します。
     * 失敗したら二つ目のペインターで処理を行い、正常に終了したらそのまま終了します。
     * 以下同様に処理を行い、
     * 全てのペインターで処理が失敗したら例外をスローします。<br>
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
     * @throws ExcelHandlingException
     *              処理に失敗した場合
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
        
        ExcelHandlingException failed = new ExcelHandlingException(String.format(
                "処理に失敗しました：%s -> %s", srcBookPath, dstBookPath));
        
        Iterator<UnsafeSupplier<BookPainter>> itr = suppliers.iterator();
        while (itr.hasNext()) {
            try {
                BookPainter painter = itr.next().get();
                painter.paintAndSave(srcBookPath, dstBookPath, diffs);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                failed.addSuppressed(e);
            }
        }
        throw failed;
    }
}
