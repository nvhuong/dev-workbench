package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * CSV Nút dữ liệu với biểu tượng và phong cách thống nhất.
 */
public class CSVButton extends JButton {
    public CSVButton() {
        setIcon(IconUtil.createThemed("icons/csv.svg", 20, 20));
        setToolTipText("CSV Data");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
