package xyz.hotchpotch.hogandiff.excel.sax;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.ShapeLoader;
import xyz.hotchpotch.hogandiff.excel.ShapeReplica;

class XSSFShapeLoaderWithSaxTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xlsm;
    private static Path test101_xls;
    private static Path test101_xlsb;
    private static Path test101_xlsm;
    private static Path test101_xlsx;
    private static Path test2_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xlsm = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test1.xlsm").toURI());
        test101_xls = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test101.xls").toURI());
        test101_xlsb = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test101.xlsb").toURI());
        test101_xlsm = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test101.xlsm").toURI());
        test101_xlsx = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test101.xlsx").toURI());
        test2_xlsx = Path.of(XSSFShapeLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() throws ExcelHandlingException {
        // ■非チェック例外
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> XSSFShapeLoaderWithSax.of(null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFShapeLoaderWithSax.of(test101_xls));
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFShapeLoaderWithSax.of(test101_xlsb));
        
        // ■チェック例外
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> XSSFShapeLoaderWithSax.of(Path.of("dummy\\dummy.xlsx")));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> XSSFShapeLoaderWithSax.of(test2_xlsx));
        
        // ■正常系
        assertTrue(
                XSSFShapeLoaderWithSax.of(test101_xlsx) instanceof XSSFShapeLoaderWithSax);
        assertTrue(
                XSSFShapeLoaderWithSax.of(test101_xlsm) instanceof XSSFShapeLoaderWithSax);
    }
    
    @Test
    void testLoadShapes_例外系_非チェック例外() throws ExcelHandlingException {
        ShapeLoader testee = XSSFShapeLoaderWithSax.of(test101_xlsx);
        
        // 対照
        assertDoesNotThrow(
                () -> testee.loadShapes(test101_xlsx, "Sheet1"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(null, "Sheet1"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(test101_xlsx, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(null, null));
        
        // 構成時と異なるブック
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadShapes(test101_xlsm, "Sheet1"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() throws ExcelHandlingException {
        ShapeLoader testee = XSSFShapeLoaderWithSax.of(test1_xlsm);
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xlsm, "X9_ダミー"));
        
        // サポート対象外のシート形式
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xlsm, "A2_グラフ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xlsm, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xlsm, "A4_マクロ"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        ShapeLoader testee = XSSFShapeLoaderWithSax.of(test101_xlsx);
        
        assertEquals(
                Set.of(
                        //ShapeReplica.of(2, ""),
                        ShapeReplica.of(3, "あいう"),
                        //ShapeReplica.of(4, ""),
                        // FIXME: [No.8 図形関連] SmartArtのテキストを取れるようにする
                        //ShapeReplica.of(7, ""),
                        //ShapeReplica.of(8, ""),
                        ShapeReplica.of(10, "あいうえお\nかきく"),
                        ShapeReplica.of(13, "グループ化1"),
                        ShapeReplica.of(14, "グループ化2")),
                testee.loadShapes(test101_xlsx, "Sheet1"));
    }
}
