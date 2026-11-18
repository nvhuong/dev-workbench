package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class ToolboxTheme {
    Color selectedNavItemBackground() {
        return ModernColors.getSelectionBackgroundColor();
    }

    Color selectedNavItemForeground() {
        return ModernColors.getTextPrimary();
    }
}
