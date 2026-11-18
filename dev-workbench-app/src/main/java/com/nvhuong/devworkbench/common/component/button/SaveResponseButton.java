package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút lưu phản hồi
 * để tiết kiệmHTTPTrả lời một biến hoặc tập tin
 */
public class SaveResponseButton extends JButton {
    public SaveResponseButton() {
        setIcon(IconUtil.createThemed("icons/save.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText("Save Response");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
