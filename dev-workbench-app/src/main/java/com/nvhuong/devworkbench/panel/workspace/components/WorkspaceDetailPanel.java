package com.nvhuong.devworkbench.panel.workspace.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.model.GitOperation;
import com.nvhuong.devworkbench.model.GitRepoSource;
import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.model.WorkspaceType;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class WorkspaceDetailPanel extends JPanel {

    public record GitActions(
            Runnable commitAction,
            Runnable pullAction,
            Runnable pushAction,
            Runnable remoteConfigAction,
            Runnable historyAction,
            Runnable branchManagementAction,
            Runnable diffAction
    ) {
        private boolean hasAnyAction() {
            return commitAction != null
                    || pullAction != null
                    || pushAction != null
                    || remoteConfigAction != null
                    || historyAction != null
                    || branchManagementAction != null
                    || diffAction != null;
        }
    }

    public WorkspaceDetailPanel(Workspace workspace) {
        this(workspace, (GitActions) null);
    }

    public WorkspaceDetailPanel(Workspace workspace, Runnable branchManagementAction) {
        this(workspace, branchManagementAction, null);
    }

    public WorkspaceDetailPanel(Workspace workspace, Runnable branchManagementAction, Runnable diffAction) {
        this(workspace, new GitActions(null, null, null, null, null, branchManagementAction, diffAction));
    }

    public WorkspaceDetailPanel(Workspace workspace, GitActions gitActions) {
        setLayout(new GridBagLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));

        JPanel infoGrid = createDetailGrid();
        JLabel nameLabel = new JLabel(workspace.getName());
        nameLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        int row = 0;
        addRow(infoGrid, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ":", nameLabel);
        addRow(infoGrid, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE) + ":",
                workspace.getType() == WorkspaceType.LOCAL
                        ? I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL)
                        : I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));
        addRow(infoGrid, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH) + ":", new PathFieldPanel(workspace.getPath()));
        addRow(infoGrid, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DESCRIPTION) + ":", workspace.getDescription());
        addRow(infoGrid, row, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_CREATED_TIME) + ":",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(workspace.getCreatedAt())));

        int sectionRow = 0;
        addSection(sectionRow++, createSection(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_BASIC_INFO), infoGrid),
                new Insets(0, 0, 0, 0));

        if (workspace.getType() == WorkspaceType.GIT) {
            addSection(sectionRow++, createSection(
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_GIT_INFO),
                    createGitInfoPanel(workspace),
                    createGitActionPanel(gitActions)
            ), new Insets(14, 0, 0, 0));
        }
        addVerticalFiller(sectionRow);
    }

    private void addSection(int row, JPanel section, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = insets;
        add(section, constraints);
    }

    private void addVerticalFiller(int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), constraints);
    }

    private static JPanel createSection(String title, JComponent body) {
        return createSection(title, body, null);
    }

    private static JPanel createSection(String title, JComponent body, JComponent headerAction) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        header.add(titleLabel, BorderLayout.WEST);
        if (headerAction != null) {
            header.add(headerAction, BorderLayout.EAST);
        }
        section.add(header, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    private static JPanel createDetailGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        return grid;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Thành phần đường dẫn tương tác: văn bản đường dẫn + Nút sao chép + Nút mở trong trình quản lý tập tin
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Thành phần tương tác đường dẫn: văn bản đường dẫn + Nút sao chép + Các nút Mở trong trình quản lý file được bố trí sát nhau từ trái sang phải.
     */
    private static class PathFieldPanel extends JPanel {

        PathFieldPanel(String path) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);

            JLabel pathLabel = new JLabel(path);
            pathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            pathLabel.setToolTipText(path);
            pathLabel.setPreferredSize(new Dimension(
                    Math.min(pathLabel.getPreferredSize().width, 400),
                    pathLabel.getPreferredSize().height));
            pathLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            add(pathLabel);

            add(Box.createHorizontalStrut(8));
            add(makeCopyBtn(path));
            add(makeOpenBtn(path));
        }

        private JButton makeCopyBtn(String path) {
            JButton btn = makeIconBtn(
                    IconUtil.createThemed("icons/copy.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL),
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_COPY_TOOLTIP));
            btn.addActionListener(e -> {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(path), null);
                // Tạm thời đổi sang màu xanh check biểu tượng như phản hồi thành công
                btn.setIcon(IconUtil.createColored("icons/check.svg",
                        IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL, ModernColors.getSuccess()));
                Timer t = new Timer(1500, ev ->
                        btn.setIcon(IconUtil.createThemed("icons/copy.svg",
                                IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)));
                t.setRepeats(false);
                t.start();
            });
            return btn;
        }

        private JButton makeOpenBtn(String path) {
            JButton btn = makeIconBtn(
                    IconUtil.createThemed("icons/file.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL),
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_OPEN_TOOLTIP));
            btn.addActionListener(e -> {
                try {
                    File dir = new File(path);
                    if (!dir.exists()) {
                        JOptionPane.showMessageDialog(btn, path,
                                I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_NOT_EXIST),
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    Desktop.getDesktop().open(dir);
                } catch (Exception ex) {
                    log.warn("Failed to open directory: {}", path, ex);
                    JOptionPane.showMessageDialog(btn, ex.getMessage(),
                            I18nUtil.getMessage(MessageKeys.ERROR),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return btn;
        }

        /**
         * Nút biểu tượng không viền,hover/press theo FlatLaf Màu nền chủ đề
         */
        private static JButton makeIconBtn(Icon icon, String tooltip) {
            JButton btn = new JButton(icon);
            btn.setToolTipText(tooltip);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            return btn;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Git Bảng thông tin
    // ──────────────────────────────────────────────────────────────────────────

    private JPanel createGitInfoPanel(Workspace workspace) {
        JPanel panel = createDetailGrid();
        int row = 0;

        addRow(panel, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REPO_SOURCE) + ":",
                workspace.getGitRepoSource() == GitRepoSource.CLONED
                        ? I18nUtil.getMessage(MessageKeys.WORKSPACE_CLONE_FROM_REMOTE)
                        : I18nUtil.getMessage(MessageKeys.WORKSPACE_INIT_LOCAL));

        addRow(panel, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_REPO) + ":",
                workspace.getGitRemoteUrl());

        addRow(panel, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_LOCAL_BRANCH) + ":",
                workspace.getCurrentBranch());

        addRow(panel, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_BRANCH) + ":",
                workspace.getRemoteBranch());

        String shortCommitId = workspace.getLastCommitId() == null ? "" :
                workspace.getLastCommitId().length() > 8
                        ? workspace.getLastCommitId().substring(0, 8)
                        : workspace.getLastCommitId();
        addRow(panel, row, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_LAST_COMMIT) + ":",
                shortCommitId);

        return panel;
    }

    private JComponent createGitActionPanel(GitActions gitActions) {
        if (gitActions == null || !gitActions.hasAnyAction()) {
            return null;
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panel.setOpaque(false);
        addGitOperationButton(panel, GitOperation.COMMIT, gitActions.commitAction());
        addGitOperationButton(panel, GitOperation.PULL, gitActions.pullAction());
        addGitOperationButton(panel, GitOperation.PUSH, gitActions.pushAction());
        addHeaderActionButton(panel, I18nUtil.getMessage(MessageKeys.WORKSPACE_REMOTE_CONFIG_TITLE),
                "icons/git-remote-config.svg", gitActions.remoteConfigAction());
        addHeaderActionButton(panel, I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_DIFF),
                "icons/detail.svg", gitActions.diffAction());
        addHeaderActionButton(panel, I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_BRANCHES),
                "icons/git.svg", gitActions.branchManagementAction());
        addHeaderActionButton(panel, I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_HISTORY),
                "icons/history.svg", gitActions.historyAction());
        return panel;
    }

    private void addGitOperationButton(JPanel panel, GitOperation operation, Runnable action) {
        if (action == null) {
            return;
        }
        panel.add(createHeaderIconButton(
                operation.getDisplayName(),
                GitOperationPresentation.getIconName(operation),
                action
        ));
    }

    private void addHeaderActionButton(JPanel panel, String text, String iconPath, Runnable action) {
        if (action == null) {
            return;
        }
        panel.add(createHeaderIconButton(text, iconPath, action));
    }

    private JButton createHeaderIconButton(String tooltip, String iconPath, Runnable action) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, 16, 16));
        button.setPreferredSize(new Dimension(30, 30));
        button.setMinimumSize(new Dimension(30, 30));
        button.setMaximumSize(new Dimension(30, 30));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * Đối với GridBagLayout Bảng điều khiển nối thêm một hàng label + value
     */
    private static void addRow(JPanel panel, int row, String label, String value) {
        JLabel v = new JLabel(value == null ? "" : value);
        v.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        addRow(panel, row, label, v);
    }

    private static void addRow(JPanel panel, int row, String label, JComponent value) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0;
        labelConstraints.fill = GridBagConstraints.NONE;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 0, 4, 18);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(ModernColors.getTextPrimary());
        panel.add(labelComponent, labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1.0;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.insets = new Insets(4, 0, 4, 0);
        panel.add(value, valueConstraints);
    }
}
