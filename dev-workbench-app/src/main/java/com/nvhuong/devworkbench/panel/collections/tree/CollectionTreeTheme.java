package com.nvhuong.devworkbench.panel.collections.tree;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
public class CollectionTreeTheme {
    private static final int HOVER_OVERLAY_ALPHA = 24;

    public Color hoverOverlayColor() {
        return ModernColors.withAlpha(ModernColors.getTextPrimary(), HOVER_OVERLAY_ALPHA);
    }

}
