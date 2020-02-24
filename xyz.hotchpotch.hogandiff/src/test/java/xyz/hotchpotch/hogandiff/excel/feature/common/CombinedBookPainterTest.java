package xyz.hotchpotch.hogandiff.excel.feature.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;

class CombinedBookPainterTest {
    
    // [static members] ********************************************************
    
    private static final BookPainter successPainter = new BookPainter() {
        @Override
        public <T> void paintAndSave(Path srcBookPath, Path dstBookPath, Map<String, Piece<T>> diffs)
                throws ExcelHandlingException {
            // nop
        }
    };
    
    private static final BookPainter failPainter = new BookPainter() {
        @Override
        public <T> void paintAndSave(Path srcBookPath, Path dstBookPath, Map<String, Piece<T>> diffs)
                throws ExcelHandlingException {
            
            throw new ExcelHandlingException();
        }
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedBookPainter.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedBookPainter.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedBookPainter.of(List.of(
                        () -> successPainter)) instanceof CombinedBookPainter);
        assertTrue(
                CombinedBookPainter.of(List.of(
                        () -> successPainter,
                        () -> failPainter)) instanceof CombinedBookPainter);
    }
    
    @Test
    void testPaintAndSave_パラメータチェック() {
        BookPainter testee = CombinedBookPainter.of(List.of(() -> successPainter));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(null, Path.of("dummy2.xlsx"), Map.of()));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(Path.of("dummy1.xlsx"), null, Map.of()));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(Path.of("dummy1.xlsx"), Path.of("dummy2.xlsx"), null));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(null, null, null));
        
        assertDoesNotThrow(
                () -> testee.paintAndSave(Path.of("dummy1.xlsx"), Path.of("dummy2.xlsx"), Map.of()));
        
        // サポート対象外の拡張子
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.paintAndSave(Path.of("dummy1.ppt"), Path.of("dummy2.ppt"), Map.of()));
        
        // 同一パス
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.paintAndSave(Path.of("dummy0.xlsx"), Path.of("dummy0.xlsx"), Map.of()));
        
        // 異なる拡張子
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.paintAndSave(Path.of("dummy1.xlsx"), Path.of("dummy2.xlsm"), Map.of()));
    }
    
    @Test
    void testPaintAndSave_失敗系() {
        BookPainter testeeF = CombinedBookPainter.of(List.of(() -> failPainter));
        BookPainter testeeFFF = CombinedBookPainter.of(List.of(
                () -> failPainter, () -> failPainter, () -> failPainter));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.paintAndSave(Path.of("dummy1.xls"), Path.of("dummy2.xls"), Map.of()));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.paintAndSave(Path.of("dummy1.xls"), Path.of("dummy2.xls"), Map.of()));
    }
    
    @Test
    void testPaintAndSave_成功系() {
        BookPainter testeeS = CombinedBookPainter.of(List.of(() -> successPainter));
        BookPainter testeeFFSF = CombinedBookPainter.of(List.of(
                () -> failPainter, () -> failPainter, () -> successPainter, () -> failPainter));
        
        // 成功１つ
        assertDoesNotThrow(
                () -> testeeS.paintAndSave(Path.of("dummy1.xls"), Path.of("dummy2.xls"), Map.of()));
        
        // いくつかの失敗ののちに成功
        assertDoesNotThrow(
                () -> testeeFFSF.paintAndSave(Path.of("dummy1.xls"), Path.of("dummy2.xls"), Map.of()));
    }
}
