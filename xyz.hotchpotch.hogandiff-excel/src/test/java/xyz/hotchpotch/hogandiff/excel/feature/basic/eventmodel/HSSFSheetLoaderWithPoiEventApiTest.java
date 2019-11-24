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
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;

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
        
        assertEquals(
                Set.of(
                        CellReplica.of(0, 0, "これはワークシートです。"),
                        CellReplica.of(2, 1, "X"),
                        CellReplica.of(3, 1, "Y"),
                        CellReplica.of(4, 1, "Z"),
                        CellReplica.of(2, 2, "90"),
                        CellReplica.of(3, 2, "20"),
                        CellReplica.of(4, 2, "60")),
                testee.loadCells(test1_xls, "A1_ワークシート"));
    }
    
    //@Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        SheetLoader<String> testee = HSSFSheetLoaderWithPoiEventApi.of(true);
        
        List<CellReplica<? extends String>> actual = new ArrayList<>(
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
                        CellReplica.of(1, 2, "数値：整数"),
                        CellReplica.of(1, 3, "1234567890"),
                        CellReplica.of(2, 2, "数値：小数"),
                        CellReplica.of(2, 3, "3.141592"),
                        CellReplica.of(3, 2, "文字列"),
                        CellReplica.of(3, 3, "abcあいう123"),
                        CellReplica.of(4, 2, "真偽値：真"),
                        CellReplica.of(4, 3, "true"),
                        CellReplica.of(5, 2, "真偽値：偽"),
                        CellReplica.of(5, 3, "false")),
                actual.subList(0, 10));
        
        assertEquals(
                List.of(
                        CellReplica.of(6, 2, "エラー：ゼロ除算"),
                        CellReplica.of(6, 3, "#DIV/0!"),
                        CellReplica.of(7, 2, "エラー：該当なし"),
                        CellReplica.of(7, 3, "#N/A"),
                        CellReplica.of(8, 2, "エラー：名前不正"),
                        CellReplica.of(8, 3, "#NAME?"),
                        CellReplica.of(9, 2, "エラー：ヌル"),
                        CellReplica.of(9, 3, "#NULL!"),
                        CellReplica.of(10, 2, "エラー：数値不正"),
                        CellReplica.of(10, 3, "#NUM!"),
                        CellReplica.of(11, 2, "エラー：参照不正"),
                        CellReplica.of(11, 3, "#REF!"),
                        CellReplica.of(12, 2, "エラー：値不正"),
                        CellReplica.of(12, 3, "#VALUE!")),
                actual.subList(10, 24));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(13, 2, "日付"),
                        //CellReplica.of(13, 3, "2019/7/28"),
                        CellReplica.of(13, 3, "43674"),
                        CellReplica.of(14, 2, "時刻"),
                        //CellReplica.of(14, 3, "13:47"),
                        CellReplica.of(14, 3, "0.574305555555556")),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        CellReplica.of(16, 2, "数式（数値：整数）"),
                        CellReplica.of(16, 3, "31400"),
                        CellReplica.of(17, 2, "数式（数値：小数）"),
                        CellReplica.of(17, 3, "3.33333333333333"),
                        CellReplica.of(18, 2, "数式（文字列）"),
                        CellReplica.of(18, 3, "TRUEだよ"),
                        CellReplica.of(19, 2, "数式（真偽値：真）"),
                        CellReplica.of(19, 3, "true"),
                        CellReplica.of(20, 2, "数式（真偽値：偽）"),
                        CellReplica.of(20, 3, "false")),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        CellReplica.of(21, 2, "数式（エラー：ゼロ除算）"),
                        CellReplica.of(21, 3, "#DIV/0!"),
                        CellReplica.of(22, 2, "数式（エラー：該当なし）"),
                        CellReplica.of(22, 3, "#N/A"),
                        CellReplica.of(23, 2, "数式（エラー：名前不正）"),
                        CellReplica.of(23, 3, "#NAME?"),
                        CellReplica.of(24, 2, "数式（エラー：ヌル）"),
                        CellReplica.of(24, 3, "#NULL!"),
                        CellReplica.of(25, 2, "数式（エラー：数値不正）"),
                        CellReplica.of(25, 3, "#NUM!"),
                        CellReplica.of(26, 2, "数式（エラー：参照不正）"),
                        CellReplica.of(26, 3, "#REF!"),
                        CellReplica.of(27, 2, "数式（エラー：値不正）"),
                        CellReplica.of(27, 3, "#VALUE!")),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(28, 2, "数式（日付）"),
                        //CellReplica.of(28, 3, "2019/7/28"),
                        CellReplica.of(28, 3, "43674"),
                        CellReplica.of(29, 2, "数式（時刻）"),
                        //CellReplica.of(29, 3, "12:47")),
                        CellReplica.of(29, 3, "0.532638888888889")),
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
