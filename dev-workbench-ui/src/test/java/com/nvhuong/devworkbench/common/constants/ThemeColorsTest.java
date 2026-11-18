package com.nvhuong.devworkbench.common.constants;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.remember;
import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class ThemeColorsTest {
    private static final String TEST_COLOR = "DevWorkbench.test.color";
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(TEST_COLOR);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void shouldReadSemanticColorFromUiDefaults() {
        Color uiDefault = new Color(12, 34, 56);
        Color fallback = new Color(90, 91, 92);

        UIManager.put(TEST_COLOR, uiDefault);

        assertEquals(ThemeColors.color(TEST_COLOR, fallback), uiDefault);
    }

    @Test
    public void shouldFallbackWhenSemanticColorIsMissing() {
        Color fallback = new Color(90, 91, 92);

        assertEquals(ThemeColors.color(TEST_COLOR, fallback), fallback);
    }

    @Test
    public void shouldRequireNotificationSurfaceTokens() {
        assertEquals(ThemeColors.REQUIRED_KEYS.contains("DevWorkbench.notification.background"), true);
        assertEquals(ThemeColors.REQUIRED_KEYS.contains("DevWorkbench.notification.border"), true);
        assertEquals(ThemeColors.REQUIRED_KEYS.contains("DevWorkbench.notification.divider"), true);
        assertEquals(ThemeColors.REQUIRED_KEYS.contains("DevWorkbench.notification.bodyForeground"), true);
    }
}
