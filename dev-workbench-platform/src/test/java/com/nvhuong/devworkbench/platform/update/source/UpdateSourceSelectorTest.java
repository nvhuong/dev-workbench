package com.nvhuong.devworkbench.platform.update.source;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class UpdateSourceSelectorTest {
    @Test
    public void shouldAlwaysSelectGithub() {
        UpdateSourceSelector selector = new UpdateSourceSelector(() -> "github");
        assertEquals(selector.selectBestSource().getName(), "GitHub");
    }

    @Test
    public void shouldHaveNoFallbackProvider() {
        UpdateSourceSelector selector = new UpdateSourceSelector();
        assertNull(selector.getFallbackSource("GitHub"));
        assertNull(selector.getFallbackSource("unknown"));
    }
}
