package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Nút đoạn mã
 * Được sử dụng để mở hộp thoại đoạn mã
 */
public class SnippetButton extends JButton {
    public SnippetButton() {
        setIcon(IconUtil.createThemed("icons/code.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(I18nUtil.getMessage(MessageKeys.SCRIPT_BUTTON_SNIPPETS));
        setText(I18nUtil.getMessage(MessageKeys.SCRIPT_BUTTON_SNIPPETS));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}