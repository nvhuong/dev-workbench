package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút chỉnh sửa
 */
public class EditButton extends JButton {
    public EditButton() {
        this(IconUtil.SIZE_SMALL);
    }

    public EditButton(int iconSize) {
        setIcon(IconUtil.createThemed("icons/edit.svg", iconSize, iconSize));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_EDIT));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
