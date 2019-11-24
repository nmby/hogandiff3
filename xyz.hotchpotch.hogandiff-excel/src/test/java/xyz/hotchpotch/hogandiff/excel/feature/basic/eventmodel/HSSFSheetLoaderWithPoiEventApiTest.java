package xyz.hotchpotch.hogandiff.excel.feature.basic.eventmodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.CellReplica.CellContentType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.feature.basic.BasicFactory;

class HSSFSheetLoaderWithPoiEventApiTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    private static Path test3_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
        test3_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test3.xls").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertTrue(
                HSSFSheetLoaderWithPoiEventApi.of(true) instanceof HSSFSheetLoaderWithPoiEventApi);
        assertTrue(
                HSSFSheetLoaderWithPoiEventApi.of(false) instanceof HSSFSheetLoaderWithPoiEventApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(true);
        
        // 対照群
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xls, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsx, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsm, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsb, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test2_xlsx, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外1() {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(true);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(Path.of("X:\\dummy\\dummy.xls"), "A1_ワークシート"));
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "X9_ダミー"));
        
        // サポート対象外のシート種類
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "A2_グラフ"));
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳かダイアログシートとワークシートを見分けられない。
        //assertThrows(
        //        ExcelHandlingException.class,
        //        () -> testee.loadCells(test1_xls, "A3_ダイアログ"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "A4_マクロ"));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外2() {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(false);
        
        // FIXME: [No.4 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
        
        // of(false) で生成されても、目的のシートに数式が含まれない場合は例外をスローしない。
        assertDoesNotThrow(
                () -> testee.loadCells(test3_xls, "B_数式なし"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(true);
        CellContentType<String> type = BasicFactory.normalStringContent;
        
        assertEquals(
                Set.of(
                        CellReplica.of(0, 0, type, "これはワークシートです。"),
                        CellReplica.of(2, 1, type, "X"),
                        CellReplica.of(3, 1, type, "Y"),
                        CellReplica.of(4, 1, type, "Z"),
                        CellReplica.of(2, 2, type, "90"),
                        CellReplica.of(3, 2, type, "20"),
                        CellReplica.of(4, 2, type, "60")),
                testee.loadCells(test1_xls, "A1_ワークシート"));
    }
    
    //@Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(true);
        CellContentType<String> type = BasicFactory.normalStringContent;
        
        List<CellReplica> actual = new ArrayList<>(
                testee.loadCells(test3_xls, "A_バリエーション"));
        actual.sort((c1, c2) -> {
            if (c1.id().row() != c2.id().row()) {
                return c1.id().row() < c2.id().row() ? -1 : 1;
            } else if (c1.id().column() != c2.id().column()) {
                return c1.id().column() < c2.id().column() ? -1 : 1;
            } else {
                throw new AssertionError();
            }
        });
        
        assertEquals(56, actual.size());
        
        assertEquals(
                List.of(
                        CellReplica.of(1, 2, type, "数値：整数"),
                        CellReplica.of(1, 3, type, "1234567890"),
                        CellReplica.of(2, 2, type, "数値：小数"),
                        CellReplica.of(2, 3, type, "3.141592"),
                        CellReplica.of(3, 2, type, "文字列"),
                        CellReplica.of(3, 3, type, "abcあいう123"),
                        CellReplica.of(4, 2, type, "真偽値：真"),
                        CellReplica.of(4, 3, type, "true"),
                        CellReplica.of(5, 2, type, "真偽値：偽"),
                        CellReplica.of(5, 3, type, "false")),
                actual.subList(0, 10));
        
        assertEquals(
                List.of(
                        CellReplica.of(6, 2, type, "エラー：ゼロ除算"),
                        CellReplica.of(6, 3, type, "#DIV/0!"),
                        CellReplica.of(7, 2, type, "エラー：該当なし"),
                        CellReplica.of(7, 3, type, "#N/A"),
                        CellReplica.of(8, 2, type, "エラー：名前不正"),
                        CellReplica.of(8, 3, type, "#NAME?"),
                        CellReplica.of(9, 2, type, "エラー：ヌル"),
                        CellReplica.of(9, 3, type, "#NULL!"),
                        CellReplica.of(10, 2, type, "エラー：数値不正"),
                        CellReplica.of(10, 3, type, "#NUM!"),
                        CellReplica.of(11, 2, type, "エラー：参照不正"),
                        CellReplica.of(11, 3, type, "#REF!"),
                        CellReplica.of(12, 2, type, "エラー：値不正"),
                        CellReplica.of(12, 3, type, "#VALUE!")),
                actual.subList(10, 24));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(13, 2, type, "日付"),
                        //CellReplica.of(13, 3, type, "2019/7/28"),
                        CellReplica.of(13, 3, type, "43674"),
                        CellReplica.of(14, 2, type, "時刻"),
                        //CellReplica.of(14, 3, type, "13:47"),
                        CellReplica.of(14, 3, type, "0.574305555555556")),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        CellReplica.of(16, 2, type, "数式（数値：整数）"),
                        CellReplica.of(16, 3, type, "31400"),
                        CellReplica.of(17, 2, type, "数式（数値：小数）"),
                        CellReplica.of(17, 3, type, "3.33333333333333"),
                        CellReplica.of(18, 2, type, "数式（文字列）"),
                        CellReplica.of(18, 3, type, "TRUEだよ"),
                        CellReplica.of(19, 2, type, "数式（真偽値：真）"),
                        CellReplica.of(19, 3, type, "true"),
                        CellReplica.of(20, 2, type, "数式（真偽値：偽）"),
                        CellReplica.of(20, 3, type, "false")),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        CellReplica.of(21, 2, type, "数式（エラー：ゼロ除算）"),
                        CellReplica.of(21, 3, type, "#DIV/0!"),
                        CellReplica.of(22, 2, type, "数式（エラー：該当なし）"),
                        CellReplica.of(22, 3, type, "#N/A"),
                        CellReplica.of(23, 2, type, "数式（エラー：名前不正）"),
                        CellReplica.of(23, 3, type, "#NAME?"),
                        CellReplica.of(24, 2, type, "数式（エラー：ヌル）"),
                        CellReplica.of(24, 3, type, "#NULL!"),
                        CellReplica.of(25, 2, type, "数式（エラー：数値不正）"),
                        CellReplica.of(25, 3, type, "#NUM!"),
                        CellReplica.of(26, 2, type, "数式（エラー：参照不正）"),
                        CellReplica.of(26, 3, type, "#REF!"),
                        CellReplica.of(27, 2, type, "数式（エラー：値不正）"),
                        CellReplica.of(27, 3, type, "#VALUE!")),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(28, 2, type, "数式（日付）"),
                        //CellReplica.of(28, 3, type, "2019/7/28"),
                        CellReplica.of(28, 3, type, "43674"),
                        CellReplica.of(29, 2, type, "数式（時刻）"),
                        //CellReplica.of(29, 3, type, "12:47")),
                        CellReplica.of(29, 3, type, "0.532638888888889")),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系3_バリエーション_数式抽出() throws ExcelHandlingException {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(false);
        
        // FIXME: [No.4 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
    }
}
