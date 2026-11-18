package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút trước
 * Được sử dụng cho kết quả khớp trước đó trong điều hướng kết quả tìm kiếm
 */
public class PreviousButton extends JButton {
    public PreviousButton() {
        setIcon(IconUtil.createThemed("icons/arrow-up.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText("Previous");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
