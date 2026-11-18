package com.nvhuong.devworkbench.common.component;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Thành phần chỉ báo bước
 * Hỗ trợ tự động điều chỉnh các chủ đề tối và sáng
 */
public class StepIndicator extends JPanel {
    private final String[] steps;
    private int currentStep = 0;

    public StepIndicator() {
        this.steps = getStepsForOperation();

        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        setOpaque(false);

        initSteps();
    }

    /**
     * Lấy màu nền của vòng tròn đang hoạt động (điều chỉnh theo chủ đề)
     */
    private Color getActiveCircleBackground() {
        return ModernColors.getPrimary();
    }

    /**
     * Lấy màu nền của vòng tròn ở trạng thái không hoạt động (điều chỉnh chủ đề)
     */
    private Color getInactiveCircleBackground() {
        return ModernColors.getBorderLightColor();
    }

    /**
     * Lấy màu đường viền vòng tròn (điều chỉnh theo chủ đề)
     */
    private Color getCircleBorderColor() {
        return ModernColors.getBorderMediumColor();
    }

    /**
     * Lấy màu văn bản hiện hoạt (điều chỉnh chủ đề)
     */
    private Color getActiveTextColor() {
        return ModernColors.getTextPrimary();
    }

    /**
     * Lấy màu văn bản không hoạt động (điều chỉnh chủ đề)
     */
    private Color getInactiveTextColor() {
        return ModernColors.getTextHint();
    }

    /**
     * Nhận màu mũi tên (thích ứng chủ đề)
     */
    private Color getArrowColor() {
        return ModernColors.getTextSecondary();
    }

    private String[] getStepsForOperation() {
        return new String[]{I18nUtil.getMessage(MessageKeys.STEP_CHECK_STATUS),
                I18nUtil.getMessage(MessageKeys.STEP_CONFIRM_CHANGE),
                I18nUtil.getMessage(MessageKeys.STEP_EXECUTE_OPERATION)};
    }

    private void initSteps() {
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) {
                add(createArrow());
            }
            add(createStepCircle(i + 1, steps[i], i == currentStep));
        }
    }

    private JPanel createStepCircle(int number, String text, boolean active) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);

        JLabel circle = new StepBadge(String.valueOf(number));
        circle.setPreferredSize(new Dimension(22, 22));
        circle.setBackground(active ? getActiveCircleBackground() : getInactiveCircleBackground());
        circle.setForeground(active ? ModernColors.getTextInverse() : getInactiveTextColor());
        circle.setFont(FontsUtil.getDefaultFont(Font.BOLD));

        // văn bản - Sử dụng màu sắc phù hợp với chủ đề
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(FontsUtil.getDefaultFontWithOffset(active ? Font.BOLD : Font.PLAIN, -3));
        label.setForeground(active ? getActiveTextColor() : getInactiveTextColor());

        panel.add(circle, BorderLayout.CENTER);
        panel.add(label, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createArrow() {
        JLabel arrow = new JLabel("›");
        arrow.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 2)); // Lớn hơn kích thước phông chữ tiêu chuẩn2Không
        arrow.setForeground(getArrowColor()); // Sử dụng màu mũi tên phù hợp với chủ đề
        return arrow;
    }

    public void setCurrentStep(int step) {
        this.currentStep = step;
        removeAll();
        initSteps();
        revalidate();
        repaint();
    }

    private final class StepBadge extends JLabel {
        private StepBadge(String text) {
            super(text, SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;

            g2.setColor(getBackground());
            g2.fillRoundRect(x, y, size - 1, size - 1, size, size);
            g2.setColor(getCircleBorderColor());
            g2.drawRoundRect(x, y, size - 1, size - 1, size, size);
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
