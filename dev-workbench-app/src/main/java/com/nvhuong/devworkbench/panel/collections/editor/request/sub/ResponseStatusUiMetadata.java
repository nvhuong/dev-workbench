package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.common.constants.ModernColors;

import java.awt.Color;

final class ResponseStatusUiMetadata {

    private ResponseStatusUiMetadata() {
    }

    static Color statusColor(int statusCode) {
        if (statusCode == 101) {
            return ModernColors.getSuccess();
        }
        if (statusCode >= 200 && statusCode < 300) {
            return ModernColors.getSuccess();
        }
        if (statusCode >= 400 && statusCode < 500) {
            return ModernColors.getWarning();
        }
        if (statusCode >= 500) {
            return ModernColors.getError();
        }
        if (statusCode >= 300) {
            return ModernColors.getPrimary();
        }
        return ModernColors.getError();
    }
}
