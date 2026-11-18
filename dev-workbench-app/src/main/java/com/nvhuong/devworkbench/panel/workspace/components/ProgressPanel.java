package com.nvhuong.devworkbench.panel.workspace.components;

import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Thành phần công khai của bảng hiển thị tiến trình
 * Được sử dụng để hiển thị tiến trình hoạt động và thông tin trạng thái
 */
public class ProgressPanel extends JPanel {

    @Getter
    private JProgressBar progressBar;
    @Getter
    private JLabel statusLabel;

    public ProgressPanel(String title) {
        initComponents();
        setupLayout(title);
    }

    private void initComponents() {
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_READY));

        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_FILL_CONFIG));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
    }

    private void setupLayout(String title) {
        setLayout(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyDialogTopSeparator(this, 8, 16, 8, 16);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        add(titleLabel, BorderLayout.NORTH);

        // nhãn trạng thái
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 4));
        contentPanel.setOpaque(false);
        contentPanel.add(statusPanel, BorderLayout.NORTH);
        contentPanel.add(progressBar, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Đặt văn bản tiến trình
     */
    public void setProgressText(String text) {
        progressBar.setString(text);
    }

    /**
     * Đặt lại trạng thái bảng tiến trình
     */
    public void reset() {
        progressBar.setValue(0);
        progressBar.setString(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_READY));
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_FILL_CONFIG));
    }
}
