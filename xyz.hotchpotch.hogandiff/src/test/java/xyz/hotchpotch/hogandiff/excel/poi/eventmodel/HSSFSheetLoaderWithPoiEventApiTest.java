package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

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
    private static Path test5_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
        test3_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test3.xls").toURI());
        test5_xls = Path.of(HSSFSheetLoaderWithPoiEventApiTest.class.getResource("Test5.xls").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertTrue(
                HSSFSheetLoaderWithPoiEventApi.of(true, true, true) instanceof HSSFSheetLoaderWithPoiEventApi);
        assertTrue(
                HSSFSheetLoaderWithPoiEventApi.of(false, false, false) instanceof HSSFSheetLoaderWithPoiEventApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, true);
        
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
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, true);
        
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
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, false);
        
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
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, true);
        
        assertEquals(
                Set.of(
                        CellReplica.of(0, 0, "これはワークシートです。", null),
                        CellReplica.of(2, 1, "X", null),
                        CellReplica.of(3, 1, "Y", null),
                        CellReplica.of(4, 1, "Z", null),
                        CellReplica.of(2, 2, "90", null),
                        CellReplica.of(3, 2, "20", null),
                        CellReplica.of(4, 2, "60", null)),
                testee.loadCells(test1_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, true);
        
        List<CellReplica> actual = new ArrayList<>(
                testee.loadCells(test3_xls, "A_バリエーション"));
        actual.sort((c1, c2) -> {
            if (c1.row() != c2.row()) {
                return c1.row() < c2.row() ? -1 : 1;
            } else if (c1.column() != c2.column()) {
                return c1.column() < c2.column() ? -1 : 1;
            } else {
                throw new AssertionError();
            }
        });
        
        assertEquals(56, actual.size());
        
        assertEquals(
                List.of(
                        CellReplica.of(1, 2, "数値：整数", null),
                        CellReplica.of(1, 3, "1234567890", null),
                        CellReplica.of(2, 2, "数値：小数", null),
                        CellReplica.of(2, 3, "3.141592", null),
                        CellReplica.of(3, 2, "文字列", null),
                        CellReplica.of(3, 3, "abcあいう123", null),
                        CellReplica.of(4, 2, "真偽値：真", null),
                        CellReplica.of(4, 3, "true", null),
                        CellReplica.of(5, 2, "真偽値：偽", null),
                        CellReplica.of(5, 3, "false", null)),
                actual.subList(0, 10));
        
        assertEquals(
                List.of(
                        CellReplica.of(6, 2, "エラー：ゼロ除算", null),
                        CellReplica.of(6, 3, "#DIV/0!", null),
                        CellReplica.of(7, 2, "エラー：該当なし", null),
                        CellReplica.of(7, 3, "#N/A", null),
                        CellReplica.of(8, 2, "エラー：名前不正", null),
                        CellReplica.of(8, 3, "#NAME?", null),
                        CellReplica.of(9, 2, "エラー：ヌル", null),
                        CellReplica.of(9, 3, "#NULL!", null),
                        CellReplica.of(10, 2, "エラー：数値不正", null),
                        CellReplica.of(10, 3, "#NUM!", null),
                        CellReplica.of(11, 2, "エラー：参照不正", null),
                        CellReplica.of(11, 3, "#REF!", null),
                        CellReplica.of(12, 2, "エラー：値不正", null),
                        CellReplica.of(12, 3, "#VALUE!", null)),
                actual.subList(10, 24));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(13, 2, "日付", null),
                        //CellReplica.of(13, 3, "2019/7/28", null),
                        CellReplica.of(13, 3, "43674", null),
                        CellReplica.of(14, 2, "時刻", null),
                        //CellReplica.of(14, 3, "13:47", null),
                        CellReplica.of(14, 3, "0.574305555555556", null)),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        CellReplica.of(16, 2, "数式（数値：整数）", null),
                        CellReplica.of(16, 3, "31400", null),
                        CellReplica.of(17, 2, "数式（数値：小数）", null),
                        CellReplica.of(17, 3, "3.33333333333333", null),
                        CellReplica.of(18, 2, "数式（文字列）", null),
                        CellReplica.of(18, 3, "TRUEだよ", null),
                        CellReplica.of(19, 2, "数式（真偽値：真）", null),
                        CellReplica.of(19, 3, "true", null),
                        CellReplica.of(20, 2, "数式（真偽値：偽）", null),
                        CellReplica.of(20, 3, "false", null)),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        CellReplica.of(21, 2, "数式（エラー：ゼロ除算）", null),
                        CellReplica.of(21, 3, "#DIV/0!", null),
                        CellReplica.of(22, 2, "数式（エラー：該当なし）", null),
                        CellReplica.of(22, 3, "#N/A", null),
                        CellReplica.of(23, 2, "数式（エラー：名前不正）", null),
                        CellReplica.of(23, 3, "#NAME?", null),
                        CellReplica.of(24, 2, "数式（エラー：ヌル）", null),
                        CellReplica.of(24, 3, "#NULL!", null),
                        CellReplica.of(25, 2, "数式（エラー：数値不正）", null),
                        CellReplica.of(25, 3, "#NUM!", null),
                        CellReplica.of(26, 2, "数式（エラー：参照不正）", null),
                        CellReplica.of(26, 3, "#REF!", null),
                        CellReplica.of(27, 2, "数式（エラー：値不正）", null),
                        CellReplica.of(27, 3, "#VALUE!", null)),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplica.of(28, 2, "数式（日付）", null),
                        //CellReplica.of(28, 3, "2019/7/28", null),
                        CellReplica.of(28, 3, "43674", null),
                        CellReplica.of(29, 2, "数式（時刻）", null),
                        //CellReplica.of(29, 3, "12:47", null)),
                        CellReplica.of(29, 3, "0.532638888888889", null)),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系3_バリエーション_数式抽出() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, false);
        
        // FIXME: [No.4 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連a() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, true, true);
        
        assertEquals(
                Set.of(
                        CellReplica.of(1, 1, "", "Author:\nComment\nComment"),
                        CellReplica.of(4, 1, "", "Authorなし"),
                        CellReplica.of(7, 1, "", "非表示"),
                        CellReplica.of(10, 1, "", "書式設定"),
                        CellReplica.of(13, 1, "セル値あり", "コメント"),
                        CellReplica.of(16, 1, "空コメント", ""),
                        CellReplica.of(19, 1, "セル値のみ", null)),
                testee.loadCells(test5_xls, "コメント"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連b() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(true, false, true);
        
        assertEquals(
                Set.of(
                        CellReplica.of(13, 1, "セル値あり", null),
                        CellReplica.of(16, 1, "空コメント", null),
                        CellReplica.of(19, 1, "セル値のみ", null)),
                testee.loadCells(test5_xls, "コメント"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連c() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(false, true, true);
        
        assertEquals(
                Set.of(
                        CellReplica.of(1, 1, "", "Author:\nComment\nComment"),
                        CellReplica.of(4, 1, "", "Authorなし"),
                        CellReplica.of(7, 1, "", "非表示"),
                        CellReplica.of(10, 1, "", "書式設定"),
                        CellReplica.of(13, 1, "", "コメント"),
                        CellReplica.of(16, 1, "", "")),
                testee.loadCells(test5_xls, "コメント"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連d() throws ExcelHandlingException {
        SheetLoader testee = HSSFSheetLoaderWithPoiEventApi.of(false, false, true);
        
        assertEquals(
                Set.of(),
                testee.loadCells(test5_xls, "コメント"));
    }
}
