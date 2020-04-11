package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Set;

/**
 * Excelシートから図形データを抽出するローダーを表します。<br>
 * これは、{@link #loadShapes(Path, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface ShapeLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelシートに含まれる図形のセットを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param sheetName シート名
     * @return 指定されたExcelシートに含まれる図形のセット
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    Set<ShapeReplica> loadShapes(Path bookPath, String sheetName)
            throws ExcelHandlingException;
}
