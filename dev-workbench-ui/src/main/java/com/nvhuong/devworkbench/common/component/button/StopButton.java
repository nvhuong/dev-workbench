package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút dừng phổ quát với biểu tượng và phong cách thống nhất.
 */
public class StopButton extends JButton {
    public StopButton() {
        setIcon(IconUtil.createThemed("icons/stop.svg", 20, 20));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_STOP));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
