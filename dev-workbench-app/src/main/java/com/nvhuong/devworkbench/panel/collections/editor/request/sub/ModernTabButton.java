package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.util.FontsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * hiện đạiTabThành phần nút
 * Hỗ trợ:
 * 1. Chỉ báo đánh dấu phía dưới
 * 2. Hiệu ứng di chuột
 * 3. Hoạt hình mượt mà
 * 4. Chủ đề thích ứng (sử dụngTabbedPanemàu chủ đề)
 */
public class ModernTabButton extends JButton {

    // Định nghĩa hằng số
    private static final int INDICATOR_HEIGHT = 3;
    private static final int MIN_HEIGHT = 34;
    private static final int HOVER_ANIMATION_DELAY = 10;
    private static final float HOVER_ALPHA_STEP = 0.1f;

    private final int tabIndex;
    private int selectedTabIndex = -1;

    // Hiệu ứng di chuột
    private float hoverAlpha = 0.0f;
    private Timer hoverAnimationTimer;

    public ModernTabButton(String text, int tabIndex) {
        super(text);
        this.tabIndex = tabIndex;
        initializeStyle();
        setupHoverEffect();
    }

    /**
     * Khởi tạo kiểu nút
     */
    private void initializeStyle() {
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setForeground(ModernTabButtonTheme.foreground(true));

        // Đặt phần đệm
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Đặt con trỏ
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Đặt hiệu ứng di chuột
     */
    private void setupHoverEffect() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    startHoverAnimation(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startHoverAnimation(false);
            }
        });
    }

    /**
     * Bắt đầu hoạt ảnh di chuột
     *
     * @param fadeIn trueĐể mờ dần trong,falsemờ đi
     */
    private void startHoverAnimation(boolean fadeIn) {
        if (hoverAnimationTimer != null && hoverAnimationTimer.isRunning()) {
            hoverAnimationTimer.stop();
        }

        hoverAnimationTimer = new Timer(HOVER_ANIMATION_DELAY, e -> {
            if (fadeIn) {
                hoverAlpha = Math.min(1.0f, hoverAlpha + HOVER_ALPHA_STEP);
                if (hoverAlpha >= 1.0f) {
                    hoverAnimationTimer.stop();
                }
            } else {
                hoverAlpha = Math.max(0.0f, hoverAlpha - HOVER_ALPHA_STEP);
                if (hoverAlpha <= 0.0f) {
                    hoverAnimationTimer.stop();
                }
            }
            repaint();
        });
        hoverAnimationTimer.start();
    }

    /**
     * Đã chọn cập nhậttabchỉ số
     */
    public void updateSelectedIndex(int selectedIndex) {
        this.selectedTabIndex = selectedIndex;
        repaint();
    }

    /**
     * Xác định xem nó có được chọn hay không (matching JToggleButton giao diện)
     */
    @Override
    public boolean isSelected() {
        return selectedTabIndex == tabIndex;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Vẽ nền di chuột
        if (hoverAlpha > 0 && !isSelected()) {
            Color hoverBg = ModernTabButtonTheme.hoverBackground(hoverAlpha);
            g2d.setColor(hoverBg);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight() - INDICATOR_HEIGHT, 4, 4);
        }

        // Vẽ nền đã chọn
        if (isSelected()) {
            Color selectedBg = ModernTabButtonTheme.selectedBackground();
            g2d.setColor(selectedBg);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight() - INDICATOR_HEIGHT, 4, 4);
        }

        g2d.dispose();

        // Vẽ văn bản
        super.paintComponent(g);

        // Vẽ chỉ báo phía dưới (sử dụng TabbedPanemàu chủ đề)
        if (isSelected()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color indicatorColor = ModernTabButtonTheme.indicator(isEnabled());
            g2.setColor(indicatorColor);
            g2.fillRoundRect(4, getHeight() - INDICATOR_HEIGHT,
                    getWidth() - 8, INDICATOR_HEIGHT, 2, 2);
            g2.dispose();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            hoverAlpha = 0.0f;
            if (hoverAnimationTimer != null) {
                hoverAnimationTimer.stop();
            }
        }
        setForeground(ModernTabButtonTheme.foreground(enabled));
    }

    /**
     * Dọn dẹp tài nguyên và ngăn chặn rò rỉ bộ nhớ
     * Phương thức này sẽ được gọi khi nút được lấy ra khỏi vùng chứa
     */
    public void cleanup() {
        if (hoverAnimationTimer != null && hoverAnimationTimer.isRunning()) {
            hoverAnimationTimer.stop();
        }
        hoverAnimationTimer = null;
    }

    @Override
    public void removeNotify() {
        // Tự động dọn sạch tài nguyên khi thành phần bị xóa khỏi vùng chứa
        cleanup();
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // Hãy chắc chắn rằng có đủ chiều cao
        size.height = Math.max(size.height, MIN_HEIGHT);
        return size;
    }
}
