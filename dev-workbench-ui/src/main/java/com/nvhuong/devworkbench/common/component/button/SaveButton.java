package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút lưu phổ quát với biểu tượng và phong cách thống nhất.
 */
public class SaveButton extends JButton {
    public SaveButton() {
        setIcon(IconUtil.createThemed("icons/save.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_SAVE));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
