package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút tìm kiếm phổ quát với biểu tượng và phong cách thống nhất.
 */
public class SearchButton extends JButton {
    public SearchButton() {
        setIcon(IconUtil.createThemed("icons/search.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_SEARCH));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
