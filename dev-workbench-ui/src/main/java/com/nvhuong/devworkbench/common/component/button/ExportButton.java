package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút xuất phổ quát,Với các biểu tượng và phong cách thống nhất.
 */
public class ExportButton extends JButton {
    public ExportButton() {
        setIcon(IconUtil.createThemed("icons/export.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_EXPORT));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
