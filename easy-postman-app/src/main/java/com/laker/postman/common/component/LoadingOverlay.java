package com.laker.postman.common.component;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Lightweight response loading overlay with an IDE-style indeterminate spinner.
 */
public class LoadingOverlay extends JComponent {
    private static final int SEGMENT_COUNT = 12;
    private static final int INNER_RADIUS = 7;
    private static final int OUTER_RADIUS = 14;
    private static final float SPINNER_STROKE = 2.2f;
    private static final int ANIMATION_DELAY_MS = 80;

    private final Timer animationTimer;
    private int activeSegment;
    private boolean isVisible = false;

    public LoadingOverlay() {
        setOpaque(false);
        setVisible(false);
        setFocusable(false);
        setEnabled(false);

        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> {
            activeSegment = (activeSegment + 1) % SEGMENT_COUNT;
            repaint();
        });
    }

    /**
     * Hiển thị mặt nạ tải
     */
    public void showLoading() {
        this.isVisible = true;
        setVisible(true);
        activeSegment = 0;
        animationTimer.start();
        repaint();
    }

    /**
     * Hiển thị mặt nạ tải. Trạng thái thực hiện bao gồm spinner và hủy biểu thức nút, không còn vẽ bản sao ở giữa.
     */
    public void showLoading(String message) {
        showLoading();
    }

    /**
     * Ẩn mặt nạ tải
     */
    public void hideLoading() {
        this.isVisible = false;
        animationTimer.stop();
        setVisible(false);
    }

    /**
     * Kiểm tra xem nó có hiển thị không
     */
    public boolean isLoading() {
        return isVisible;
    }

    /**
     * Giao diện cũ vẫn tương thích với người gọi và mặt nạ hiện tại không hiển thị bản sao.
     */
    public void setMessage(String message) {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Kích hoạt tính năng khử răng cưa
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Vẽ mặt nạ nền bán trong suốt - Chuyển thể chủ đề
            Color overlayColor = LoadingOverlayTheme.overlay();
            g2d.setColor(overlayColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            drawSpinner(g2d, centerX, centerY);
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        // loading overlay Nó chỉ chịu trách nhiệm về các tín hiệu thị giác và không được chặn trước các lần truy cập chuột từ các thành phần cơ bản.
        // Bằng cách này, người dùng vẫn có thể chuyển đổi ngay cả khi yêu cầu đang được thực thi response tab Hoặc xem nội dung cũ hơn.
        return false;
    }

    private void drawSpinner(Graphics2D g2d, int centerX, int centerY) {
        Stroke oldStroke = g2d.getStroke();
        try {
            g2d.setStroke(new BasicStroke(SPINNER_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                int fadeIndex = Math.floorMod(activeSegment - i, SEGMENT_COUNT);
                g2d.setColor(LoadingOverlayTheme.spinnerSegment(fadeIndex, SEGMENT_COUNT));

                double angle = Math.toRadians((360.0 / SEGMENT_COUNT) * i - 90.0);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                g2d.draw(new Line2D.Double(
                        centerX + INNER_RADIUS * cos,
                        centerY + INNER_RADIUS * sin,
                        centerX + OUTER_RADIUS * cos,
                        centerY + OUTER_RADIUS * sin
                ));
            }
        } finally {
            g2d.setStroke(oldStroke);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent != null) {
            return parent.getSize();
        }
        return super.getPreferredSize();
    }
}
