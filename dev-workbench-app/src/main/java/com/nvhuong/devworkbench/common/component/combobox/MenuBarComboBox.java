package com.nvhuong.devworkbench.common.component.combobox;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.DevWorkbenchComboBox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Lớp cơ sở hộp thả xuống thanh menu trên cùng: chiều rộng thích ứng theo các mục đã chọn, không có viền và nền được tích hợp với thanh menu.
 */
abstract class MenuBarComboBox<E> extends DevWorkbenchComboBox<E> {
    private static final int MIN_WIDTH = 64;
    private static final int MAX_WIDTH = 200;
    private static final int HEIGHT = 28;

    protected MenuBarComboBox() {
        // FlatLaf Chiều rộng tối thiểu mặc định (ComboBox.minimumWidth=72）Lớn hơn yêu cầu đối với thanh menu, hãy hạ thấp nó một cách rõ ràng
        putClientProperty(FlatClientProperties.MINIMUM_WIDTH, MIN_WIDTH);
        setDynamicWidthRange(MIN_WIDTH, MAX_WIDTH);
        setFixedHeight(HEIGHT);
        setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Áp dụng lại màu nền của thanh menu sau khi chuyển đổi chủ đề
        Color background = UIManager.getColor("MenuBar.background");
        if (background != null) {
            String hex = String.format("#%02x%02x%02x", background.getRed(), background.getGreen(), background.getBlue());
            putClientProperty("FlatLaf.style", "buttonBackground: " + hex
                    + "; buttonEditableBackground: " + hex
                    + "; buttonFocusedBackground: " + hex
                    + "; buttonSeparatorColor: null; buttonDisabledSeparatorColor: null");
            setBackground(background);
        }
    }
}
