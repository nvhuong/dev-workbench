package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút tiếp theo
 * Được sử dụng cho kết quả phù hợp tiếp theo trong điều hướng kết quả tìm kiếm
 */
public class NextButton extends JButton {
    public NextButton() {
        setIcon(IconUtil.createThemed("icons/arrow-down.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText("Next");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
