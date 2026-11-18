package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.remember;
import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class SwitchButtonTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.BORDER_MEDIUM, ThemeColors.TEXT_INVERSE);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void switchChromeShouldUseThemeTokens() {
        Color trackOff = new Color(12, 34, 56);
        Color thumb = new Color(230, 235, 240);
        UIManager.put(ThemeColors.BORDER_MEDIUM, trackOff);
        UIManager.put(ThemeColors.TEXT_INVERSE, thumb);

        assertEquals(SwitchButton.trackOffColor(), trackOff);
        assertEquals(SwitchButton.thumbColor(), thumb);
    }
}
