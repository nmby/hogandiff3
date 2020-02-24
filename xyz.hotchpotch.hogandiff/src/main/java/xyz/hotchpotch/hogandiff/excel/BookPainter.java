package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Map;

import xyz.hotchpotch.hogandiff.excel.SResult.Piece;

/**
 * Excelブックの差分個所に色を付けて新しいファイルとして保存するペインターを表します。<br>
 * これは、{@link #paintAndSave(Path, Path, Map)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
// FIXME: [No.92 ドキュメント改善] 要お勉強
// 副作用があっても「関数型インタフェース」と言って良いのかしら？？
// Consumer も同じだから、良いのかな？？
@FunctionalInterface
public interface BookPainter {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 元のExcelブックの差分個所に色を付けたものを
     * 指定されたパスに保存します。<br>
     * 
     * @param <T> セルデータの型
     * @param srcBookPath 元のExcelブックのパス
     * @param dstBookPath 保存先パス（ファイル名を含む）
     * @param diffs シート名とその差分個所のマップ
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    <T> void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            Map<String, Piece<T>> diffs)
            throws ExcelHandlingException;
}
