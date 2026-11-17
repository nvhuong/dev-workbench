package com.laker.postman.common.component.tab;

import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * Với chỉ sốTabThành phần tiêu đề
 * Đã có hỗ trợTabChỉ báo trạng thái (dấu chấm màu xanh lá cây) xuất hiện sau tiêu đề để xác định điều nàyTabCó nội dung hay không
 * Tương tự Postman của hiệu ứng nhãn
 */
@Slf4j
public class IndicatorTabComponent extends JPanel {
    private static final int INDICATOR_DIAMETER = 6; // Đường kính chỉ báo
    private static final int INDICATOR_SPACING = 6; // Khoảng cách giữa chỉ báo và văn bản
    private static final int LABEL_HORIZONTAL_PADDING = 6; // Label Đệm trái và phải
    private static final int LABEL_VERTICAL_PADDING = 4; // Label Đệm trên và dưới
    private static final int INDICATOR_STROKE_MARGIN = 1; // drawOval Đúng/Biên giới phía dưới đã bị chiếm đóng

    private final String title;
    private boolean showIndicator = false; // Có hiển thị chỉ báo hay không
    private final Font font;

    /**
     * Tạo một cái có chỉ báoTabthành phần
     *
     * @param title Tabtiêu đề
     */
    public IndicatorTabComponent(String title) {
        this.title = title;
        this.font = FontsUtil.getDefaultFont(Font.PLAIN);
        setOpaque(false);
        // Đặt kích thước ban đầu
        updatePreferredSize();
    }

    /**
     * Đặt xem có hiển thị chỉ báo hay không
     *
     * @param show true Một chấm màu xanh lá cây được hiển thị.false Ẩn dấu chấm màu xanh lá cây
     */
    public void setShowIndicator(boolean show) {
        if (this.showIndicator != show) {
            this.showIndicator = show;
            // Tính toán lại kích thước và tự động điều chỉnh độ rộng theo trạng thái hiển thị chấm xanh
            updatePreferredSize();
            repaint();
        }
    }

    /**
     * Cập nhật kích thước ưa thích của thành phần
     * Chiều rộng được tính toán linh hoạt dựa trên việc chỉ báo có được hiển thị hay không
     */
    private void updatePreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(title);
        int width = textWidth + LABEL_HORIZONTAL_PADDING * 2;

        // Chỉ tăng chiều rộng của chỉ báo khi nó được hiển thị
        if (showIndicator) {
            width += INDICATOR_DIAMETER + INDICATOR_STROKE_MARGIN + INDICATOR_SPACING;
        }

        int height = fm.getHeight() + LABEL_VERTICAL_PADDING * 2;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Kích hoạt tính năng khử răng cưa
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Đặt phông chữ và màu sắc
            g2.setFont(font);
            g2.setColor(getForeground());

            // Tính kích thước văn bản
            FontMetrics fm = g2.getFontMetrics();
            int textHeight = fm.getAscent();

            // Tổng chiều cao của thành phần
            int totalHeight = getHeight();

            // Văn bản được vẽ bắt đầu từ bên trái (cộngpadding）
            int textX = LABEL_HORIZONTAL_PADDING;

            // Văn bản được căn giữa theo chiều dọc
            int textY = (totalHeight - fm.getHeight()) / 2 + textHeight;

            // Vẽ văn bản
            g2.drawString(title, textX, textY);

            // Hiển thị chỉ báo nếu được yêu cầu
            if (showIndicator) {
                int textWidth = fm.stringWidth(title);
                // Chỉ báo nằm ở bên phải của văn bản
                int indicatorX = textX + textWidth + INDICATOR_SPACING;
                int indicatorY = (totalHeight - INDICATOR_DIAMETER) / 2;

                // Vẽ các chấm màu xanh lá cây
                g2.setColor(IndicatorTabTheme.indicator());
                g2.fillOval(indicatorX, indicatorY, INDICATOR_DIAMETER, INDICATOR_DIAMETER);

                // Thêm một đường viền tinh tế để làm cho nó dễ nhìn hơn
                g2.setColor(IndicatorTabTheme.indicatorBorder());
                g2.drawOval(indicatorX, indicatorY, INDICATOR_DIAMETER, INDICATOR_DIAMETER);
            }
        } finally {
            g2.dispose();
        }
    }

}
