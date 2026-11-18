package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút khởi động phổ quát với biểu tượng và phong cách thống nhất.
 */
public class StartButton extends JButton {
    public StartButton() {
        setIcon(IconUtil.createThemed("icons/start.svg", 20, 20));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_START));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
