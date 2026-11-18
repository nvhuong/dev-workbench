package com.nvhuong.devworkbench.common.component.combobox;

import com.nvhuong.devworkbench.model.Workspace;

import javax.swing.*;
import java.awt.*;

/**
 * Trình kết xuất cho menu thả xuống của không gian làm việc
 */
public class WorkspaceItemRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Workspace workspace) {
            label.setText(workspace.getName());
            // Nếu tên quá dài, hãy sử dụng tooltipHiển thị tên đầy đủ
            if (workspace.getName().length() > 15) {
                label.setToolTipText(workspace.getName());
            }
        }
        return label;
    }
}