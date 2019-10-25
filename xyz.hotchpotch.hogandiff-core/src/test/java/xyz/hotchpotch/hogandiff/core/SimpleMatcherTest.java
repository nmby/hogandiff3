package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.Pair;

class SimpleMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final List<String> list0 = List.of();
    private static final List<String> listABC = List.of("A", "B", "C");
    private static final List<String> listXYZ = List.of("X", "Y", "Z");
    private static final List<String> listVWXYZ = List.of("V", "W", "X", "Y", "Z");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testConstructor() {
        assertDoesNotThrow(
                () -> new SimpleMatcher<>());
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        SimpleMatcher<String> testee = new SimpleMatcher<>();
        
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, list0));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(list0, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, null));
        
        assertDoesNotThrow(
                () -> testee.makePairs(list0, list0));
    }
    
    @Test
    void testMakePairs2_マッチングロジック() {
        SimpleMatcher<String> testee = new SimpleMatcher<>();
        
        // 長さが同じ場合
        assertEquals(
                List.of(),
                testee.makePairs(list0, list0));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC, listABC));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC, listXYZ));
        
        // 長さが異なる場合
        assertEquals(
                List.of(
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(null, 1),
                        Pair.ofNullable(null, 2)),
                testee.makePairs(list0, listABC));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(1, null),
                        Pair.ofNullable(2, null)),
                testee.makePairs(listABC, list0));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2),
                        Pair.ofNullable(null, 3),
                        Pair.ofNullable(null, 4)),
                testee.makePairs(listXYZ, listVWXYZ));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2),
                        Pair.ofNullable(3, null),
                        Pair.ofNullable(4, null)),
                testee.makePairs(listVWXYZ, listXYZ));
    }
}
