package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

class MatcherTest {
    
    // [static members] ********************************************************
    
    private static final ToIntFunction<String> gapEvaluator = String::length;
    private static final ToIntBiFunction<String, String> diffEvaluator = StringDiffUtil::levenshteinDistance;
    
    // [instance members] ******************************************************
    
    @Test
    void testSimpleMatcherOf() {
        assertDoesNotThrow(
                () -> Matcher.simpleMatcherOf());
        
        assertTrue(
                Matcher.simpleMatcherOf() instanceof SimpleMatcher);
    }
    
    @Test
    void testMinimumEditDistanceMatcherOf() {
        assertThrows(
                NullPointerException.class,
                () -> Matcher.minimumEditDistanceMatcherOf(null, diffEvaluator));
        assertThrows(
                NullPointerException.class,
                () -> Matcher.minimumEditDistanceMatcherOf(gapEvaluator, null));
        assertThrows(
                NullPointerException.class,
                () -> Matcher.minimumEditDistanceMatcherOf(null, null));
        assertDoesNotThrow(
                () -> Matcher.minimumEditDistanceMatcherOf(gapEvaluator, diffEvaluator));
        
        assertTrue(
                Matcher.minimumEditDistanceMatcherOf(gapEvaluator,
                        diffEvaluator) instanceof MinimumEditDistanceMatcher);
    }
    
    @Test
    void testNerutonMatcherOf() {
        assertThrows(
                NullPointerException.class,
                () -> Matcher.nerutonMatcherOf(null, diffEvaluator));
        assertThrows(
                NullPointerException.class,
                () -> Matcher.nerutonMatcherOf(gapEvaluator, null));
        assertThrows(
                NullPointerException.class,
                () -> Matcher.nerutonMatcherOf(null, null));
        assertDoesNotThrow(
                () -> Matcher.nerutonMatcherOf(gapEvaluator, diffEvaluator));
        
        assertTrue(
                Matcher.nerutonMatcherOf(gapEvaluator, diffEvaluator) instanceof NerutonMatcher);
    }
}
