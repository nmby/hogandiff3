package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.ShapeLoader;
import xyz.hotchpotch.hogandiff.excel.ShapeReplica;

class HSSFShapeLoaderWithPoiEventApiTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test101_xls;
    private static Path test101_xlsb;
    private static Path test101_xlsm;
    private static Path test101_xlsx;
    private static Path test2_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI());
        test101_xls = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test101.xls").toURI());
        test101_xlsb = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test101.xlsb").toURI());
        test101_xlsm = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test101.xlsm").toURI());
        test101_xlsx = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test101.xlsx").toURI());
        test2_xls = Path.of(HSSFShapeLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() throws ExcelHandlingException {
        // ■正常系
        assertTrue(
                HSSFShapeLoaderWithPoiEventApi.of() instanceof HSSFShapeLoaderWithPoiEventApi);
    }
    
    @Test
    void testLoadShapes_例外系_非チェック例外() throws ExcelHandlingException {
        ShapeLoader testee = HSSFShapeLoaderWithPoiEventApi.of();
        
        // 対照
        assertDoesNotThrow(
                () -> testee.loadShapes(test101_xls, "Sheet1"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(null, "Sheet1"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(test101_xls, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadShapes(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadShapes(test101_xlsx, "Sheet1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadShapes(test101_xlsm, "Sheet1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadShapes(test101_xlsb, "Sheet1"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() throws ExcelHandlingException {
        ShapeLoader testee = HSSFShapeLoaderWithPoiEventApi.of();
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test101_xls, "ダミー"));
        
        // サポート対象外のシート形式
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xls, "A2_グラフ"));
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳かダイアログシートとワークシートを見分けられない。
        //assertThrows(
        //        ExcelHandlingException.class,
        //        () -> testee.loadShapes(test1_xls, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadShapes(test1_xls, "A4_マクロ"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        ShapeLoader testee = HSSFShapeLoaderWithPoiEventApi.of();
        
        assertEquals(
                Set.of(
                        //ShapeReplica.of(2, ""),
                        ShapeReplica.of(3, "あいう"),
                        //ShapeReplica.of(4, ""),
                        ShapeReplica.of(7, "あいうえお\nかきく"),
                        ShapeReplica.of(10, "グループ化1"),
                        ShapeReplica.of(11, "グループ化2")),
                testee.loadShapes(test101_xls, "Sheet1"));
    }
}
