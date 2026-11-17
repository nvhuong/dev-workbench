package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EasyTextFieldCellRenderer extends EasyTextField implements TableCellRenderer {

    // Màu ngựa vằn nhạt cho các hàng xen kẽ (sậm hơn một chút so với nền bảng/nông)
    private static final float STRIPE_ALPHA = 0.04f;

    public EasyTextFieldCellRenderer() {
        super(1);
        setBorder(TableUIConstants.createLabelBorder());
        setOpaque(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(TableUIConstants.createLabelBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // value Có thể null，Chuyển đổi thành chuỗi trống một cách thống nhất
        String text = (value == null) ? "" : value.toString();

        // Tự động tính toán số lượng ký tự tối đa có thể được hiển thị
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        int maxDisplayLength = calculateMaxDisplayLength(text, columnWidth);

        // Đặt văn bản hiển thị (cắt ngắn nếu quá dài)
        boolean truncated = false;
        if (maxDisplayLength > 0 && text.length() > maxDisplayLength) {
            setText(text.substring(0, maxDisplayLength) + "...");
            truncated = true;
        } else {
            setText(text);
        }

        // Tooltip：Hiển thị văn bản đầy đủ khi cắt bớt để dễ xem
        setToolTipText(truncated ? text : null);

        int hoveredRow = getHoveredRow(table);
        setBackground(TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row));
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

        return this;
    }

    /**
     * Từ JTable Duyệt qua hệ thống phân cấp vùng chứa chính và tìm AbstractTablePanel và quay trở lại hoveredRow。
     * Sử dụng truyền tải vòng lặp thay vì độ sâu cố định để thích ứng với các cấu trúc lồng nhau của vùng chứa khác nhau.
     */
    private static int getHoveredRow(JTable table) {
        java.awt.Container parent = table.getParent();
        while (parent != null) {
            if (parent instanceof AbstractTablePanel<?> panel) {
                return panel.hoveredRow;
            }
            parent = parent.getParent();
        }
        return -1;
    }

    /**
     * Trộn hai màu
     *
     * @param base   Màu cơ bản
     * @param blend  Trộn màu
     * @param alpha  Tỷ lệ trộn 0.0–1.0
     */
    private static Color blendColor(Color base, Color blend, float alpha) {
        int r = Math.min(255, Math.max(0, Math.round(base.getRed()   * (1 - alpha) + blend.getRed()   * alpha)));
        int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * (1 - alpha) + blend.getGreen() * alpha)));
        int b = Math.min(255, Math.max(0, Math.round(base.getBlue()  * (1 - alpha) + blend.getBlue()  * alpha)));
        return new Color(r, g, b);
    }

    static Color stripeBackground(Color base) {
        return blendColor(base, ModernColors.getTextPrimary(), STRIPE_ALPHA);
    }

    /**
     * Tự động tính toán số lượng ký tự có thể hiển thị dựa trên chiều rộng cột và phông chữ
     *
     * @param text        Văn bản cần hiển thị
     * @param columnWidth Độ rộng cột (pixel)
     * @return Số lượng ký tự tối đa có thể được hiển thị
     */
    private int calculateMaxDisplayLength(String text, int columnWidth) {
        if (text == null || text.isEmpty() || columnWidth <= 0) {
            return 0;
        }

        // Lấy phông chữ hiện tại
        Font font = getFont();
        if (font == null) {
            return text.length(); // Nếu không thể lấy được phông chữ thì việc cắt bớt phông chữ sẽ không được thực hiện
        }

        // Lấy FontMetrics
        FontMetrics fm = getFontMetrics(font);
        if (fm == null) {
            return text.length();
        }

        // Không gian dành riêng: lề trái và phải + "..." chiều rộng
        int ellipsisWidth = fm.stringWidth("...");
        int availableWidth = columnWidth - TableUIConstants.PADDING_LEFT - TableUIConstants.PADDING_RIGHT - ellipsisWidth;

        if (availableWidth <= 0) {
            return 0;
        }

        // Tính chiều rộng thực tế của văn bản
        int textWidth = fm.stringWidth(text);

        // Nếu chiều rộng văn bản nhỏ hơn chiều rộng có sẵn thì không cần cắt bớt
        if (textWidth <= availableWidth) {
            return text.length();
        }

        // Tìm kiếm nhị phân cho vị trí cắt ngắn tốt nhất
        int left = 0;
        int right = text.length();
        int bestLength = 0;

        while (left <= right) {
            int mid = (left + right) / 2;
            String substring = text.substring(0, mid);
            int substringWidth = fm.stringWidth(substring);

            if (substringWidth <= availableWidth) {
                bestLength = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return Math.max(0, bestLength);
    }
}
