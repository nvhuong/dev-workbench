package com.laker.postman.panel.topmenu.help;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.platform.update.changelog.ChangelogService;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

/**
 * Hộp thoại nhật ký cập nhật phiên bản
 *
 * <p>Phiên bản được tái cấu trúc: Sử dụng kiến trúc nguồn cập nhật thống nhất để loại bỏ trùng lặp mã</p>
 */
@Slf4j
public class ChangelogDialog extends JDialog {

    private final JTextArea contentArea;
    private final JButton refreshButton;
    private final JLabel statusLabel;
    private final ChangelogService changelogService;

    public ChangelogDialog(Frame parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.CHANGELOG_TITLE), true);

        this.changelogService = new ChangelogService(SettingManager::getUpdateSourcePreference);

        setLayout(new BorderLayout(10, 10));
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        ToolWindowSurfaceStyle.applyDialogSurface((JPanel) getContentPane());
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // Bảng trên cùng: Thông tin phiên bản hiện tại
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Bảng ở giữa: Cập nhật nội dung nhật ký
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(contentArea);

        JScrollPane scrollPane = new JScrollPane(contentArea);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        add(scrollPane, BorderLayout.CENTER);

        // Bảng dưới cùng: Nút
        JPanel bottomPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogFooter(bottomPanel);

        // nhãn trạng thái
        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        // Bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(buttonPanel);

        refreshButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_REFRESH), false);
        refreshButton.addActionListener(e -> loadChangelog());

        JButton githubButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITHUB), false);
        githubButton.setToolTipText(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITHUB_TOOLTIP));
        githubButton.addActionListener(e -> openGitHubWeb());

        JButton giteeButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITEE), false);
        giteeButton.setToolTipText(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITEE_TOOLTIP));
        giteeButton.addActionListener(e -> openGiteeWeb());

        JButton closeButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_CLOSE), true);
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(refreshButton);
        buttonPanel.add(githubButton);
        buttonPanel.add(giteeButton);
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Đặt thuộc tính hộp thoại
        setSize(750, 650);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Tải nhật ký cập nhật không đồng bộ
        loadChangelog();
    }

    /**
     * Tạo bảng trên cùng
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        String currentVersion = SystemUtil.getCurrentVersion();
        JLabel versionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CHANGELOG_CURRENT_VERSION, currentVersion));
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));

        panel.add(versionLabel, BorderLayout.WEST);

        return panel;
    }

    /**
     * Tải nhật ký thay đổi
     */
    private void loadChangelog() {
        contentArea.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));
        refreshButton.setEnabled(false);
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return changelogService.getChangelog();
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (result != null) {
                        contentArea.setText(result);
                        contentArea.setCaretPosition(0); // Cuộn lên trên cùng
                        statusLabel.setText(" ");
                    } else {
                        String errorMsg = I18nUtil.getMessage(MessageKeys.CHANGELOG_LOAD_FAILED,
                                I18nUtil.getMessage(MessageKeys.ERROR_NETWORK_TIMEOUT));
                        contentArea.setText(errorMsg);
                        statusLabel.setText(errorMsg);
                    }
                } catch (Exception e) {
                    log.error("Failed to load changelog", e);
                    String errorMsg = I18nUtil.getMessage(MessageKeys.CHANGELOG_LOAD_FAILED, e.getMessage());
                    contentArea.setText(errorMsg);
                    statusLabel.setText(errorMsg);
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * Mở GitHub Phiên bản web
     */
    private void openGitHubWeb() {
        UpdateSource github = changelogService.getSourceSelector().selectBestSource();
        // Nếu hiện tại không có GitHub，Lấy GitHub Ví dụ
        if (!"GitHub".equals(github.getName())) {
            github = changelogService.getSourceSelector().getFallbackSource(github);
        }
        openUrl(github.getWebUrl());
    }

    /**
     * Mở Gitee Phiên bản web
     */
    private void openGiteeWeb() {
        UpdateSource source = changelogService.getSourceSelector().selectBestSource();
        // Nếu hiện tại không có Gitee，Lấy Gitee Ví dụ
        if (!"Gitee".equals(source.getName())) {
            source = changelogService.getSourceSelector().getFallbackSource(source);
        }
        openUrl(source.getWebUrl());
    }

    /**
     * Mở URL
     */
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            log.error("Failed to open URL: {}", url, e);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, e.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Hiển thị hộp thoại nhật ký cập nhật
     */
    public static void showDialog(Frame parent) {
        SwingUtilities.invokeLater(() -> {
            ChangelogDialog dialog = new ChangelogDialog(parent);
            dialog.setVisible(true);
        });
    }
}
