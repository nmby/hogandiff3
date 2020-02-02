package xyz.hotchpotch.hogandiff.excel.feature.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;

class CombinedBookLoaderTest {
    
    // [static members] ********************************************************
    
    private static final BookLoader successLoader = bookPath -> List.of("success");
    
    private static final BookLoader failLoader = bookPath -> {
        throw new RuntimeException("fail");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedBookLoader.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedBookLoader.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedBookLoader.of(List.of(
                        () -> successLoader)) instanceof CombinedBookLoader);
        assertTrue(
                CombinedBookLoader.of(List.of(
                        () -> successLoader,
                        () -> failLoader)) instanceof CombinedBookLoader);
    }
    
    @Test
    void testLoadSheetNames_パラメータチェック() {
        BookLoader testee = CombinedBookLoader.of(List.of(() -> successLoader));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null));
    }
    
    @Test
    void testLoadSheetNames_失敗系() {
        BookLoader testeeF = CombinedBookLoader.of(List.of(() -> failLoader));
        BookLoader testeeFFF = CombinedBookLoader.of(List.of(
                () -> failLoader, () -> failLoader, () -> failLoader));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.loadSheetNames(Path.of("dummy.xlsx")));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.loadSheetNames(Path.of("dummy.xlsx")));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        BookLoader testeeS = CombinedBookLoader.of(List.of(() -> successLoader));
        BookLoader testeeFFSF = CombinedBookLoader.of(List.of(
                () -> failLoader,
                () -> failLoader,
                () -> successLoader,
                () -> failLoader));
        
        // 成功１つ
        assertEquals(
                List.of("success"),
                testeeS.loadSheetNames(Path.of("dummy.xlsx")));
        
        // いくつかの失敗ののちに成功
        assertEquals(
                List.of("success"),
                testeeFFSF.loadSheetNames(Path.of("dummy.xlsx")));
    }
}
