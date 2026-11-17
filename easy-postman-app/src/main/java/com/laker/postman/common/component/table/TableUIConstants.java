package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * cái bànUILớp liên tục
 * Quản lý tập trung các biểu mẫu liên quanUICác hằng số tạo điều kiện thuận lợi cho việc sửa đổi và bảo trì thống nhất
 * Hỗ trợ thích ứng chủ đề sáng và tối
 */
public class TableUIConstants {
    // Văn bản không đổi
    public static final String SELECT_FILE_TEXT = "Select File";
    public static final String FILE_TYPE = "File";

    // Kích thước biểu tượng
    public static final int ICON_SIZE = 14;

    // Lề
    public static final int PADDING_LEFT = 8;
    public static final int PADDING_RIGHT = 8;
    public static final int PADDING_TOP = 2;
    public static final int PADDING_BOTTOM = 2;

    /**
     * Lấy màu đường viền - Chuyển thể chủ đề
     */
    public static Color getBorderColor() {
        return ModernColors.getTableGridColor();
    }

    /**
     * Lấy màu di chuột - Chuyển thể chủ đề
     */
    public static Color getHoverColor() {
        return ModernColors.getHoverBackgroundColor();
    }

    /**
     * Lấy màu văn bản của nút tập tin - Chuyển thể chủ đề
     */
    public static Color getFileButtonTextColor() {
        // Màu sắc chủ đề vẫn nhất quán ở cả hai chế độ
        return ModernColors.getPrimary();
    }

    /**
     * Lấy màu văn bản đã chọn của tập tin - Chuyển thể chủ đề
     */
    public static Color getFileSelectedTextColor() {
        return ModernColors.getPrimaryLight();
    }

    /**
     * Lấy màu văn bản trạng thái trống của tập tin - Chuyển thể chủ đề
     */
    public static Color getFileEmptyTextColor() {
        return ModernColors.getTextHint();
    }

    /**
     * Tạo đường viền nút tiêu chuẩn
     */
    public static Border createButtonBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getBorderColor()),
                BorderFactory.createEmptyBorder(
                        PADDING_TOP, PADDING_LEFT, PADDING_BOTTOM, PADDING_RIGHT));
    }

    /**
     * Tạo đường viền nhãn tiêu chuẩn
     */
    public static Border createLabelBorder() {
        return BorderFactory.createEmptyBorder(
                PADDING_TOP, PADDING_LEFT, PADDING_BOTTOM, PADDING_RIGHT);
    }

    /**
     * Tạo khung trạng thái chỉnh sửa biểu mẫu.
     * Thay vào đó, được sử dụng khi trả về trực tiếp thành phần soạn thảo FlatTextField Mặc định focus/underline Tác dụng.
     */
    public static Border createCellEditorBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getPrimaryLight()),
                BorderFactory.createEmptyBorder(0, PADDING_LEFT - 1, 0, PADDING_RIGHT - 1)
        );
    }

    /**
     * Tạo phần đệm cho thành phần đầu vào bên trong vùng chứa.
     */
    public static Border createCellEditorInnerBorder() {
        return BorderFactory.createEmptyBorder(0, PADDING_LEFT, 0, PADDING_RIGHT);
    }

    /**
     * Tạo form-data Key/Type Đường viền tiêu đề nhóm.
     * Tiêu đề là điểm nhấn của cấu trúc bảng và không nên nằm rải rác trong bảng điều khiển thực tế legacy Swing border sự kết hợp.
     */
    public static Border createFormDataGroupedHeaderBorder(Color gridColor,
                                                          boolean rightBoundary,
                                                          int leftPadding,
                                                          int rightPadding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, rightBoundary ? 1 : 0, gridColor),
                BorderFactory.createEmptyBorder(0, leftPadding, 0, rightPadding)
        );
    }

    /**
     * Lấy màu nền chưa được chọn của hàng được chỉ định, giữ cho mẫu ngựa vằn nhất quán giữa trạng thái chỉnh sửa và trạng thái hiển thị.
     */
    public static Color getRowBackground(JTable table, int row) {
        Color base = table.getBackground();
        return row % 2 == 1 ? EasyTextFieldCellRenderer.stripeBackground(base) : base;
    }

    /**
     * Lấy màu nền của ô được chỉ định, bao gồm các trạng thái đã chọn, di chuột, null và ngựa vằn.
     */
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty,
                                          JTable table, int row) {
        if (isSelected) {
            return table.getSelectionBackground();
        } else if (isHovered) {
            return getHoverColor();
        } else if (isEmpty) {
            return ModernColors.getEmptyCellBackground();
        } else if (row >= 0) {
            return getRowBackground(table, row);
        } else {
            return table.getBackground();
        }
    }

    /**
     * Trình chỉnh sửa hoạt động trực tiếp như  cell editor Được sử dụng khi trở về.
     */
    public static void styleTextCellEditor(JTextComponent editor, JTable table, int row) {
        styleTextCellEditor(editor, table, row, createCellEditorBorder());
    }

    /**
     * Được sử dụng khi trình soạn thảo được đặt bên trong một khung chứa.
     */
    public static void styleContainedTextCellEditor(JTextComponent editor, JTable table, int row) {
        styleTextCellEditor(editor, table, row, createCellEditorInnerBorder());
    }

    /**
     * Loại thùng chứa cell editor Ví dụ, sử dụng multiline thông minh editor và tài liệu editor。
     */
    public static void styleCellEditorContainer(JComponent container, JTable table, int row) {
        Color background = getRowBackground(table, row);
        container.setOpaque(true);
        container.setBackground(background);
        container.setBorder(createCellEditorBorder());
    }

    /**
     * JScrollPane Mặc nó vào cell editor Trong nội bộ, cần phải đồng bộ hóa viewport nền, nếu không chủ đề tối sẽ hiển thị.
     */
    public static void styleEditorScrollPane(JScrollPane scrollPane, JTable table, int row) {
        Color background = getRowBackground(table, row);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(background);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        if (scrollPane.getViewport() != null) {
            scrollPane.getViewport().setOpaque(true);
            scrollPane.getViewport().setBackground(background);
        }
    }

    private static void styleTextCellEditor(JTextComponent editor, JTable table, int row, Border border) {
        Color background = getRowBackground(table, row);
        editor.setOpaque(true);
        editor.setBackground(background);
        editor.setForeground(table.getForeground());
        editor.setCaretColor(table.getForeground());
        editor.setBorder(border);
        editor.putClientProperty("JComponent.outline", null);
        editor.putClientProperty("FlatLaf.style", null);
    }

    /**
     * Lấy màu nền của ô
     */
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty,
                                          JTable table) {
        return getCellBackground(isSelected, isHovered, isEmpty, table, -1);
    }
}
