package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

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