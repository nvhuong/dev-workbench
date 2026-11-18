package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút nén
 * Được sử dụng để nén mã hoặc nội dung văn bản
 */
public class CompressButton extends JButton {
    public CompressButton() {
        setIcon(IconUtil.createThemed("icons/compress.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_COMPRESS));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
