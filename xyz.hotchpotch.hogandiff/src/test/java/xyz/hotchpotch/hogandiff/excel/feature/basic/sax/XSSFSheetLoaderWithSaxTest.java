package xyz.hotchpotch.hogandiff.excel.feature.basic.sax;

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
import xyz.hotchpotch.hogandiff.excel.feature.common.CellReplicaImpl;

class XSSFSheetLoaderWithSaxTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xlsm;
    private static Path test3_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test1.xlsx").toURI());
        test2_xlsm = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xlsm").toURI());
        test3_xlsx = Path.of(XSSFSheetLoaderWithSaxTest.class.getResource("Test3.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() throws ExcelHandlingException {
        // ■非チェック例外
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> XSSFSheetLoaderWithSax.of(true, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFSheetLoaderWithSax.of(true, test1_xls));
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFSheetLoaderWithSax.of(true, test1_xlsb));
        
        // ■チェック例外
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> XSSFSheetLoaderWithSax.of(true, Path.of("dummy\\dummy.xlsx")));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> XSSFSheetLoaderWithSax.of(true, test2_xlsm));
        
        // ■正常系
        assertTrue(
                XSSFSheetLoaderWithSax.of(true, test1_xlsx) instanceof XSSFSheetLoaderWithSax);
        assertTrue(
                XSSFSheetLoaderWithSax.of(false, test1_xlsm) instanceof XSSFSheetLoaderWithSax);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() throws ExcelHandlingException {
        SheetLoader<String> testee = XSSFSheetLoaderWithSax.of(true, test1_xlsm);
        
        // 対照
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsm, "A1_ワークシート"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xlsm, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        // 構成時と異なるブック
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test3_xlsx, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() throws ExcelHandlingException {
        SheetLoader<String> testee = XSSFSheetLoaderWithSax.of(true, test1_xlsm);
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "X9_ダミー"));
        
        // サポート対象外のシート形式
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A2_グラフ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A4_マクロ"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        SheetLoader<String> testee = XSSFSheetLoaderWithSax.of(true, test1_xlsx);
        
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
    }
    
    @Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        SheetLoader<String> testee = XSSFSheetLoaderWithSax.of(true, test3_xlsx);
        
        List<CellReplica<? extends String>> actual = new ArrayList<>(
                testee.loadCells(test3_xlsx, "A_バリエーション"));
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
                        CellReplicaImpl.of(1, 2, "数値：整数"),
                        CellReplicaImpl.of(1, 3, "1234567890"),
                        CellReplicaImpl.of(2, 2, "数値：小数"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        //CellReplicaImpl.of(2, 3, "3.141592"),
                        CellReplicaImpl.of(2, 3, "3.1415920000000002"),
                        CellReplicaImpl.of(3, 2, "文字列"),
                        CellReplicaImpl.of(3, 3, "abcあいう123"),
                        CellReplicaImpl.of(4, 2, "真偽値：真"),
                        CellReplicaImpl.of(4, 3, "true"),
                        CellReplicaImpl.of(5, 2, "真偽値：偽"),
                        CellReplicaImpl.of(5, 3, "false")),
                actual.subList(0, 10));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(6, 2, "エラー：ゼロ除算"),
                        CellReplicaImpl.of(6, 3, "#DIV/0!"),
                        CellReplicaImpl.of(7, 2, "エラー：該当なし"),
                        CellReplicaImpl.of(7, 3, "#N/A"),
                        CellReplicaImpl.of(8, 2, "エラー：名前不正"),
                        CellReplicaImpl.of(8, 3, "#NAME?"),
                        CellReplicaImpl.of(9, 2, "エラー：ヌル"),
                        CellReplicaImpl.of(9, 3, "#NULL!"),
                        CellReplicaImpl.of(10, 2, "エラー：数値不正"),
                        CellReplicaImpl.of(10, 3, "#NUM!"),
                        CellReplicaImpl.of(11, 2, "エラー：参照不正"),
                        CellReplicaImpl.of(11, 3, "#REF!"),
                        CellReplicaImpl.of(12, 2, "エラー：値不正"),
                        CellReplicaImpl.of(12, 3, "#VALUE!")),
                actual.subList(10, 24));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplicaImpl.of(13, 2, "日付"),
                        //CellReplicaImpl.of(13, 3, "2019/7/28"),
                        CellReplicaImpl.of(13, 3, "43674"),
                        CellReplicaImpl.of(14, 2, "時刻"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        //CellReplicaImpl.of(14, 3, "13:47"),
                        CellReplicaImpl.of(14, 3, "0.57430555555555551")),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(16, 2, "数式（数値：整数）"),
                        CellReplicaImpl.of(16, 3, "31400"),
                        CellReplicaImpl.of(17, 2, "数式（数値：小数）"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        CellReplicaImpl.of(17, 3, "3.3333333333333335"),
                        CellReplicaImpl.of(18, 2, "数式（文字列）"),
                        CellReplicaImpl.of(18, 3, "TRUEだよ"),
                        CellReplicaImpl.of(19, 2, "数式（真偽値：真）"),
                        CellReplicaImpl.of(19, 3, "true"),
                        CellReplicaImpl.of(20, 2, "数式（真偽値：偽）"),
                        CellReplicaImpl.of(20, 3, "false")),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(21, 2, "数式（エラー：ゼロ除算）"),
                        CellReplicaImpl.of(21, 3, "#DIV/0!"),
                        CellReplicaImpl.of(22, 2, "数式（エラー：該当なし）"),
                        CellReplicaImpl.of(22, 3, "#N/A"),
                        CellReplicaImpl.of(23, 2, "数式（エラー：名前不正）"),
                        CellReplicaImpl.of(23, 3, "#NAME?"),
                        CellReplicaImpl.of(24, 2, "数式（エラー：ヌル）"),
                        CellReplicaImpl.of(24, 3, "#NULL!"),
                        CellReplicaImpl.of(25, 2, "数式（エラー：数値不正）"),
                        CellReplicaImpl.of(25, 3, "#NUM!"),
                        CellReplicaImpl.of(26, 2, "数式（エラー：参照不正）"),
                        CellReplicaImpl.of(26, 3, "#REF!"),
                        CellReplicaImpl.of(27, 2, "数式（エラー：値不正）"),
                        CellReplicaImpl.of(27, 3, "#VALUE!")),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplicaImpl.of(28, 2, "数式（日付）"),
                        //CellReplicaImpl.of(28, 3, "2019/7/28"),
                        CellReplicaImpl.of(28, 3, "43674"),
                        CellReplicaImpl.of(29, 2, "数式（時刻）"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        //CellReplicaImpl.of(29, 3, "12:47")),
                        CellReplicaImpl.of(29, 3, "0.53263888888888888")),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系3_数式抽出() throws ExcelHandlingException {
        SheetLoader<String> testee = XSSFSheetLoaderWithSax.of(false, test3_xlsx);
        
        List<CellReplica<? extends String>> actual = new ArrayList<>(
                testee.loadCells(test3_xlsx, "A_バリエーション"));
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
                        CellReplicaImpl.of(1, 2, "数値：整数"),
                        CellReplicaImpl.of(1, 3, "1234567890"),
                        CellReplicaImpl.of(2, 2, "数値：小数"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        //CellReplicaImpl.of(2, 3, "3.141592"),
                        CellReplicaImpl.of(2, 3, "3.1415920000000002"),
                        CellReplicaImpl.of(3, 2, "文字列"),
                        CellReplicaImpl.of(3, 3, "abcあいう123"),
                        CellReplicaImpl.of(4, 2, "真偽値：真"),
                        CellReplicaImpl.of(4, 3, "true"),
                        CellReplicaImpl.of(5, 2, "真偽値：偽"),
                        CellReplicaImpl.of(5, 3, "false")),
                actual.subList(0, 10));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(6, 2, "エラー：ゼロ除算"),
                        CellReplicaImpl.of(6, 3, "#DIV/0!"),
                        CellReplicaImpl.of(7, 2, "エラー：該当なし"),
                        CellReplicaImpl.of(7, 3, "#N/A"),
                        CellReplicaImpl.of(8, 2, "エラー：名前不正"),
                        CellReplicaImpl.of(8, 3, "#NAME?"),
                        CellReplicaImpl.of(9, 2, "エラー：ヌル"),
                        CellReplicaImpl.of(9, 3, "#NULL!"),
                        CellReplicaImpl.of(10, 2, "エラー：数値不正"),
                        CellReplicaImpl.of(10, 3, "#NUM!"),
                        CellReplicaImpl.of(11, 2, "エラー：参照不正"),
                        CellReplicaImpl.of(11, 3, "#REF!"),
                        CellReplicaImpl.of(12, 2, "エラー：値不正"),
                        CellReplicaImpl.of(12, 3, "#VALUE!")),
                actual.subList(10, 24));
        
        assertEquals(
                List.of(
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        CellReplicaImpl.of(13, 2, "日付"),
                        //CellReplicaImpl.of(13, 3, "2019/7/28"),
                        CellReplicaImpl.of(13, 3, "43674"),
                        CellReplicaImpl.of(14, 2, "時刻"),
                        // FIXME: [No.6 小数の扱い改善] 小数精度は仕方ないのかな？
                        //CellReplicaImpl.of(14, 3, "13:47"),
                        CellReplicaImpl.of(14, 3, "0.57430555555555551")),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(16, 2, "数式（数値：整数）"),
                        CellReplicaImpl.of(16, 3, " ROUND(D3 * 100, 0) * 100"),
                        CellReplicaImpl.of(17, 2, "数式（数値：小数）"),
                        CellReplicaImpl.of(17, 3, " 10 / 3"),
                        CellReplicaImpl.of(18, 2, "数式（文字列）"),
                        CellReplicaImpl.of(18, 3, " D5 & \"だよ\""),
                        CellReplicaImpl.of(19, 2, "数式（真偽値：真）"),
                        CellReplicaImpl.of(19, 3, "(1=1)"),
                        CellReplicaImpl.of(20, 2, "数式（真偽値：偽）"),
                        CellReplicaImpl.of(20, 3, " (\"あ\" = \"い\")")),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(21, 2, "数式（エラー：ゼロ除算）"),
                        CellReplicaImpl.of(21, 3, " D3 / (D2 - 1234567890)"),
                        CellReplicaImpl.of(22, 2, "数式（エラー：該当なし）"),
                        CellReplicaImpl.of(22, 3, " VLOOKUP(\"dummy\", C17:D22, 2)"),
                        CellReplicaImpl.of(23, 2, "数式（エラー：名前不正）"),
                        CellReplicaImpl.of(23, 3, " dummy()"),
                        CellReplicaImpl.of(24, 2, "数式（エラー：ヌル）"),
                        CellReplicaImpl.of(24, 3, " MAX(D2:D3 D17:D18)"),
                        CellReplicaImpl.of(25, 2, "数式（エラー：数値不正）"),
                        CellReplicaImpl.of(25, 3, " DATE(-1, -1, -1)"),
                        CellReplicaImpl.of(26, 2, "数式（エラー：参照不正）"),
                        CellReplicaImpl.of(26, 3, " INDIRECT(\"dummy\") + 100"),
                        CellReplicaImpl.of(27, 2, "数式（エラー：値不正）"),
                        CellReplicaImpl.of(27, 3, " \"abc\" + 123")),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        CellReplicaImpl.of(28, 2, "数式（日付）"),
                        CellReplicaImpl.of(28, 3, " DATE(2019, 7, 28)"),
                        CellReplicaImpl.of(29, 2, "数式（時刻）"),
                        CellReplicaImpl.of(29, 3, " D15 - \"1:00\"")),
                actual.subList(52, 56));
    }
}
