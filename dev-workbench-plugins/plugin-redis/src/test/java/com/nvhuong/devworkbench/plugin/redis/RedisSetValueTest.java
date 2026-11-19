package com.nvhuong.devworkbench.plugin.redis;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class RedisSetValueTest {

    /** SET used to strip the value, silently altering stored data (leading/trailing spaces, final newline). */
    @Test
    public void panelSetValueIsStoredVerbatim() {
        assertEquals(RedisSetValues.forPanel("  hello  \n", List.of()), "  hello  \n");
        assertEquals(RedisSetValues.forPanel("{\"a\": 1}\n", List.of("ignored")), "{\"a\": 1}\n");
    }

    @Test
    public void panelFallsBackToArgsOnlyWhenValueEditorIsEmpty() {
        assertEquals(RedisSetValues.forPanel("", List.of("a", "b")), "a b");
        assertEquals(RedisSetValues.forPanel(null, List.of("x")), "x");
        expectThrows(IllegalArgumentException.class, () -> RedisSetValues.forPanel("   ", List.of()));
    }

    @Test
    public void scriptValueIsStoredVerbatimIncludingEmptyString() {
        assertEquals(RedisSetValues.forScript(" padded ", List.of()), " padded ");
        assertEquals(RedisSetValues.forScript("", List.of()), "");
        assertEquals(RedisSetValues.forScript(42, List.of()), "42");
        assertEquals(RedisSetValues.forScript(null, List.of("from", "args")), "from args");
        expectThrows(IllegalArgumentException.class, () -> RedisSetValues.forScript(null, List.of()));
    }
}
