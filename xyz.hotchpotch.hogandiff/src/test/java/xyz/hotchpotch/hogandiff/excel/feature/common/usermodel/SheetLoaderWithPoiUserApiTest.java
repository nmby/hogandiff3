package xyz.hotchpotch.hogandiff.excel.feature.common.usermodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.feature.common.CellReplicaImpl;
import xyz.hotchpotch.hogandiff.excel.util.PoiUtil;

class SheetLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static final Function<Cell, CellReplica<String>> converter = cell -> CellReplicaImpl.of(
            cell.getRowIndex(),
            cell.getColumnIndex(),
            PoiUtil.getCellContentAsString(cell, false));
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertThrows(
                NullPointerException.class,
                () -> SheetLoaderWithPoiUserApi.of(null));
        
        assertTrue(
                SheetLoaderWithPoiUserApi.of(converter) instanceof SheetLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        SheetLoader<String> testee = SheetLoaderWithPoiUserApi.of(converter);
        
        // 対照群
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsx, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsm, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A1_ワークシート"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xlsx, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsb, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() {
        SheetLoader<String> testee = SheetLoaderWithPoiUserApi.of(converter);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(Path.of("X:\\dummy\\dummy.xlsx"), "A1_ワークシート"));
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsx, "X9_ダミー"));
        
        // サポート対象外のシート種類
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A2_グラフ"));
        assertThrows(
                // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからダイアログシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A3_ダイアログ"));
        assertThrows(
                // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからマクロシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A4_マクロ"));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xlsx, "A1_ワークシート"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        SheetLoader<String> testee = SheetLoaderWithPoiUserApi.of(converter);
        
        assertEquals(
                Set.of(
                        CellReplicaImpl.of(0, 0, "これはワークシートです。"),
                        CellReplicaImpl.of(2, 1, "X"),
                        CellReplicaImpl.of(3, 1, "Y"),
                        CellReplicaImpl.of(4, 1, "Z"),
                        CellReplicaImpl.of(2, 2, "90"),
                        CellReplicaImpl.of(3, 2, "20"),
                        CellReplicaImpl.of(4, 2, "60")),
                testee.loadCells(test1_xls, "A1_ワークシート"));
        
        assertEquals(
                Set.of(
                        CellReplicaImpl.of(0, 0, "これはワークシートです。"),
                        CellReplicaImpl.of(2, 1, "X"),
                        CellReplicaImpl.of(3, 1, "Y"),
                        CellReplicaImpl.of(4, 1, "Z"),
                        CellReplicaImpl.of(2, 2, "90"),
                        CellReplicaImpl.of(3, 2, "20"),
                        CellReplicaImpl.of(4, 2, "60")),
                testee.loadCells(test1_xlsx, "A1_ワークシート"));
        
        assertEquals(
                Set.of(
                        CellReplicaImpl.of(0, 0, "これはワークシートです。"),
                        CellReplicaImpl.of(2, 1, "X"),
                        CellReplicaImpl.of(3, 1, "Y"),
                        CellReplicaImpl.of(4, 1, "Z"),
                        CellReplicaImpl.of(2, 2, "90"),
                        CellReplicaImpl.of(3, 2, "20"),
                        CellReplicaImpl.of(4, 2, "60")),
                testee.loadCells(test1_xlsm, "A1_ワークシート"));
    }
}
