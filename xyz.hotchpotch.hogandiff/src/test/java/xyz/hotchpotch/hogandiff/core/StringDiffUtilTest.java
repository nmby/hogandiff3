package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringDiffUtilTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testLevenshteinDistance1_パラメータチェック() {
        assertThrows(
                NullPointerException.class,
                () -> StringDiffUtil.levenshteinDistance(null, ""));
        assertThrows(
                NullPointerException.class,
                () -> StringDiffUtil.levenshteinDistance("", null));
        assertThrows(
                NullPointerException.class,
                () -> StringDiffUtil.levenshteinDistance(null, null));
        
        assertDoesNotThrow(
                () -> StringDiffUtil.levenshteinDistance("", ""));
    }
    
    @Test
    void testLevenshteinDistance2_同一内容() {
        // テストのためにやってる。ふつうは new String() なんてやっちゃダメよ！
        String str1 = new String("abcde");
        String str2 = new String("abcde");
        assert str1 != str2;
        
        // 同一インスタンスの場合
        assertEquals(0, StringDiffUtil.levenshteinDistance("", ""));
        assertEquals(0, StringDiffUtil.levenshteinDistance(str1, str1));
        
        // 別インスタンス・同一内容の場合
        assertEquals(0, StringDiffUtil.levenshteinDistance(str1, str2));
    }
    
    @Test
    void testLevenshteinDistance3_異なる内容() {
        // 特殊ケース
        assertEquals(5, StringDiffUtil.levenshteinDistance("", "abcde"));
        assertEquals(5, StringDiffUtil.levenshteinDistance("abcde", ""));
        
        // 一般ケース
        assertEquals(2, StringDiffUtil.levenshteinDistance("abcde", "abc"));
        assertEquals(2, StringDiffUtil.levenshteinDistance("abc", "abcde"));
        assertEquals(2, StringDiffUtil.levenshteinDistance("abcde", "cde"));
        assertEquals(2, StringDiffUtil.levenshteinDistance("cde", "abcde"));
        assertEquals(4, StringDiffUtil.levenshteinDistance("abcd", "bxde"));
        assertEquals(10, StringDiffUtil.levenshteinDistance("abcde", "vwxyz"));
    }
}
