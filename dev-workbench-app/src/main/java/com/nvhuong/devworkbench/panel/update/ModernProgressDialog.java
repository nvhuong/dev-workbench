package com.nvhuong.devworkbench.panel.update;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Hộp thoại tiến trình tải xuống hiện đại - Hiển thị tiến trình đơn giản và trực quan
 */
@Slf4j
public class ModernProgressDialog {

    private static final long KB = 1024L;
    private static final long MB = 1024 * KB;
    private static final long GB = 1024 * MB;

    private final JDialog dialog;
    private final JProgressBar progressBar;
    private final JLabel percentLabel;
    private final JLabel statusLabel;
    private final JLabel sizeLabel;
    private final JLabel speedLabel;
    private final JButton cancelButton;

    @Setter
    private Runnable onCancelListener;

    public ModernProgressDialog(JFrame parent) {
        dialog = new JDialog(parent, I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING), true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setResizable(false);
        // Bấm vào cửa sổ × Tương đương với điểm"Hủy tải xuống"，Thay vào đó DO_NOTHING
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                triggerCancel();
            }
        });

        percentLabel  = new JLabel("0%", SwingConstants.CENTER);
        statusLabel   = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING), SwingConstants.CENTER);
        progressBar   = new JProgressBar(0, 100);
        sizeLabel     = new JLabel("-- / -- MB");
        speedLabel    = new JLabel("-- KB/s");
        cancelButton  = createCancelButton();

        dialog.setContentPane(createContentPanel());
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    }

    private JPanel createContentPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.setBorder(new EmptyBorder(32, 40, 28, 40));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);

        // biểu tượng
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/download.svg", 48, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconLabel);
        contentPanel.add(Box.createVerticalStrut(16));

        // Văn bản trạng thái
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 4));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Tỷ lệ phần trăm
        percentLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 20));
        percentLabel.setForeground(ModernColors.getPrimary());
        percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(percentLabel);
        contentPanel.add(Box.createVerticalStrut(12));

        // thanh tiến trình (theo dõi FlatLaf chủ đề, không có màu nền cố định)
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(progressBar);
        contentPanel.add(Box.createVerticalStrut(20));

        // Chi tiết
        JPanel detailsPanel = createDetailsPanel();
        detailsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detailsPanel);
        contentPanel.add(Box.createVerticalStrut(24));

        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(cancelButton);

        mainPanel.add(contentPanel);
        return mainPanel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 20, 8));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setPreferredSize(new Dimension(400, 60));
        panel.setMaximumSize(new Dimension(400, 60));

        JLabel sizeTitle  = makeHintLabel(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADED));
        JLabel speedTitle = makeHintLabel(I18nUtil.getMessage(MessageKeys.UPDATE_SPEED));

        sizeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        speedLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(sizeTitle);
        panel.add(sizeLabel);
        panel.add(speedTitle);
        panel.add(speedLabel);
        return panel;
    }

    private JLabel makeHintLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextHint());
        return label;
    }

    private JButton createCancelButton() {
        JButton button = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL_DOWNLOAD), false);
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 1));
        button.setBorder(new EmptyBorder(8, 24, 8, 24));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> triggerCancel());
        return button;
    }

    /**
     * triggerHủy(× nút và nút hủy có cùng logic)
     * Trước tiên hãy ẩn hộp thoại đi rồi thông báo cho người tải xuống hủy để tránh trường hợp hộp thoại bị kẹt
     */
    public void triggerCancel() {
        // Để hộp thoại biến mất trước, đừng đợi luồng tải xuống phản hồi
        hideNow();
        if (onCancelListener != null) {
            onCancelListener.run();
        }
    }

    public void updateProgress(int percentage, long downloaded, long total, double speed) {
        SwingUtilities.invokeLater(() -> {
            boolean totalKnown = total > 0;
            progressBar.setIndeterminate(!totalKnown);
            if (totalKnown) {
                progressBar.setValue(percentage);
                percentLabel.setText(percentage + "%");
                sizeLabel.setText(formatBytes(downloaded) + " / " + formatBytes(total));
            } else {
                percentLabel.setText("--");
                sizeLabel.setText(formatBytes(downloaded));
            }
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING));
            speedLabel.setText(formatSpeed(speed));
        });
    }

    private String formatBytes(long bytes) {
        if (bytes >= GB) return String.format("%.2f GB", (double) bytes / GB);
        if (bytes >= MB) return String.format("%.1f MB", (double) bytes / MB);
        if (bytes >= KB) return String.format("%.1f KB", (double) bytes / KB);
        return bytes + " B";
    }

    private String formatSpeed(double speed) {
        if (speed >= MB) return String.format("%.2f MB/s", speed / MB);
        if (speed >= KB) return String.format("%.1f KB/s", speed / KB);
        return String.format("%.0f B/s", speed);
    }

    /**
     * Hiển thị hộp thoại.
     * Lưu ý: Phải có mặt EDT gọi(modal dialog của setVisible(true) sẽ chặn EDT）。
     * Người gọi nên vượt qua SwingUtilities.invokeLater / invokeAndWait Hãy chắc chắn rằng bạn có mặt ở đó EDT Khi thực hiện,
     * Và trong show() Vượt qua trước setOnCancelListener Đăng ký gọi lại hủy và bắt đầu tác vụ tải xuống.
     */
    public void show() {
        // Phải ở đó EDT Đã gọi, không còn là gói nội bộ nữa invokeLater（Ngược lại modal setVisible xếp hàng sau khi gọi lại tải xuống)
        if (!SwingUtilities.isEventDispatchThread()) {
            log.warn("ModernProgressDialog.show() called off EDT, using invokeAndWait");
            try {
                SwingUtilities.invokeAndWait(this::showOnEdt);
            } catch (Exception ex) {
                log.error("Failed to show progress dialog", ex);
            }
        } else {
            showOnEdt();
        }
    }

    private void showOnEdt() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        percentLabel.setText("0%");
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING));
        sizeLabel.setText("-- / -- MB");
        speedLabel.setText("-- KB/s");
        dialog.setVisible(true); // modal — Chặn ở đây cho đến khi hide() được gọi
    }

    /**
     * Ẩn và hủy hộp thoại (luồng an toàn, có thể được gọi từ bất kỳ luồng nào)
     */
    public void hide() {
        hideNow();
    }

    private void hideNow() {
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.setVisible(false);
            dialog.dispose();
        } else {
            SwingUtilities.invokeLater(() -> {
                dialog.setVisible(false);
                dialog.dispose();
            });
        }
    }
}
