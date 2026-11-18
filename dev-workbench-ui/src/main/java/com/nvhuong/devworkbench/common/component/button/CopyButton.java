package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút sao chép
 * Dùng để sao chép nội dung văn bản vào clipboard
 */
public class CopyButton extends JButton {
    public CopyButton() {
        super();
        setIcon(IconUtil.createThemed("icons/copy.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_COPY));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
