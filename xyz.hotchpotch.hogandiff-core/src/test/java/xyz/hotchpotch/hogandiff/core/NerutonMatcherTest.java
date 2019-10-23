package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.Pair;

class NerutonMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final ToIntFunction<String> gapEvaluator = String::length;
    private static final ToIntBiFunction<String, String> diffEvaluator = StringDiffUtil::levenshteinDistance;
    
    private static final List<String> list0_1 = List.of();
    private static final List<String> list0_2 = new ArrayList<>();
    private static final List<String> listABC_1 = List.of("A", "B", "C");
    private static final List<String> listABC_2 = List.of("A", "B", "C");
    private static final List<String> listXXX_1 = List.of("X", "X", "X");
    private static final List<String> listXXX_2 = List.of("X", "X", "X");
    private static final List<String> listBCA_1 = List.of("B", "C", "A");
    private static final List<String> listXXBAX_1 = List.of("X", "X", "B", "A", "X");
    private static final List<String> listBB_1 = List.of("BB");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testConstructor() {
        assertThrows(
                AssertionError.class,
                () -> new NerutonMatcher<>(null, diffEvaluator));
        assertThrows(
                AssertionError.class,
                () -> new NerutonMatcher<>(gapEvaluator, null));
        assertThrows(
                AssertionError.class,
                () -> new NerutonMatcher<>(null, null));
        
        assertDoesNotThrow(
                () -> new NerutonMatcher<>(gapEvaluator, diffEvaluator));
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        NerutonMatcher<String> testee = new NerutonMatcher<>(gapEvaluator, diffEvaluator);
        
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, list0_1));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(list0_1, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, null));
        
        assertDoesNotThrow(
                () -> testee.makePairs(list0_1, list0_1));
    }
    
    @Test
    void testMakePairs2_マッチングロジック_同じ内容() {
        NerutonMatcher<String> testee = new NerutonMatcher<>(gapEvaluator, diffEvaluator);
        
        // 同一インスタンス
        assertEquals(
                List.of(),
                testee.makePairs(list0_1, list0_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC_1, listABC_1));
        
        // 別インスタンス同一内容
        assertEquals(
                List.of(),
                testee.makePairs(list0_1, list0_2));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC_1, listABC_2));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listXXX_1, listXXX_2));
    }
    
    @Test
    void testMakePairs3_マッチングロジック_異なる内容() {
        NerutonMatcher<String> testee = new NerutonMatcher<>(gapEvaluator, diffEvaluator);
        
        // 一方が長さゼロ
        assertEquals(
                List.of(
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(null, 1),
                        Pair.ofNullable(null, 2)),
                testee.makePairs(list0_1, listABC_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(1, null),
                        Pair.ofNullable(2, null)),
                testee.makePairs(listABC_1, list0_1));
        
        // 同じ長さでギャップ無し
        assertEquals(
                List.of(
                        Pair.ofNullable(1, 0),
                        Pair.ofNullable(2, 1),
                        Pair.ofNullable(0, 2)),
                testee.makePairs(listABC_1, listBCA_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 1),
                        Pair.ofNullable(1, 2),
                        Pair.ofNullable(2, 0)),
                testee.makePairs(listBCA_1, listABC_1));
        
        // ギャップあり
        assertEquals(
                List.of(
                        Pair.ofNullable(1, 2),
                        Pair.ofNullable(0, 3),
                        Pair.ofNullable(2, null),
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(null, 1),
                        Pair.ofNullable(null, 4)),
                testee.makePairs(listABC_1, listXXBAX_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(2, 1),
                        Pair.ofNullable(3, 0),
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(1, null),
                        Pair.ofNullable(4, null),
                        Pair.ofNullable(null, 2)),
                testee.makePairs(listXXBAX_1, listABC_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(1, 0),
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(2, null)),
                testee.makePairs(listABC_1, listBB_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 1),
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(null, 2)),
                testee.makePairs(listBB_1, listABC_1));
    }
}
