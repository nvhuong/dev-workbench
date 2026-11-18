package com.nvhuong.devworkbench.common.component.placeholder;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class PlaceholderTheme {
    Color skeletonBlock() {
        return ModernColors.getBorderLightColor();
    }

    Color skeletonSoftBlock() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color skeletonAccent() {
        return ModernColors.primaryWithAlpha(64);
    }

    Color skeletonAccentLine() {
        return ModernColors.primaryWithAlpha(96);
    }
}
