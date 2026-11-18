package com.nvhuong.devworkbench.common.component.tab;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class IndicatorTabTheme {
    Color indicator() {
        return ModernColors.getSuccessDark();
    }

    Color indicatorBorder() {
        return ModernColors.withAlpha(ModernColors.getSuccessDark(), 100);
    }
}
