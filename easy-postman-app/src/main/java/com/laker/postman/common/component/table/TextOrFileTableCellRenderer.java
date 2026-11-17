package com.laker.postman.common.component.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Trình kết xuất ô kết hợp văn bản hoặc tập tin
 * Phiên bản nâng cao: Hỗ trợ cấu hình kiểu động và hiển thị văn bản tùy chỉnh
 */
public class TextOrFileTableCellRenderer implements TableCellRenderer {

    private final FileCellRenderer fileRenderer = new FileCellRenderer();
    private final EasyTextFieldCellRenderer textRenderer;

    // Chỉ mục cột loại có thể định cấu hình, mặc định là 2（Form-Datatrong bảngTypechỉ mục cột)
    private final int typeColumnIndex = 2;

    /**
     * Tạo trình kết xuất thành phần văn bản hoặc tệp mặc định
     */
    public TextOrFileTableCellRenderer() {
        textRenderer = new EasyTextFieldCellRenderer();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Lấy loại ô từ cột loại
        Object type = table.getValueAt(row, typeColumnIndex);
        boolean isFileType = TableUIConstants.FILE_TYPE.equalsIgnoreCase(String.valueOf(type));
        Component c;

        if (isFileType) {
            c = fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            c = textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;

        String cellValue = value == null ? null : value.toString();
        boolean isEmpty = cellValue == null || cellValue.trim().isEmpty();
        boolean isHovered = row == hoverRow;

        c.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, isFileType && isEmpty, table, row));

        // Thêm phần đệm vào ô văn bản
        if (!isFileType) {
            if (c instanceof JLabel) {
                ((JLabel) c).setBorder(TableUIConstants.createLabelBorder());
            }
        }

        return c;
    }
}
