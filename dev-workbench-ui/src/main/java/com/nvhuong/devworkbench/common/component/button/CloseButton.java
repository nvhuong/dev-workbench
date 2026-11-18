package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút đóng
 * Được sử dụng để đóng hộp thoại hoặc bảng điều khiển
 */
public class CloseButton extends JButton {
    public CloseButton() {
        super();
        setIcon(IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_CLOSE));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
