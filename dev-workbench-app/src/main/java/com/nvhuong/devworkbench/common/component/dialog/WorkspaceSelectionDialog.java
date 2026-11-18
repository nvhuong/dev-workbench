package com.nvhuong.devworkbench.common.component.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.setting.SettingsInputStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.model.GitRepoSource;
import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.model.WorkspaceType;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Hộp thoại lựa chọn không gian làm việc - Các thành phần có thể tái sử dụng
 * Được sử dụng để chọn không gian làm việc mục tiêu trong nhiều tình huống (chẳng hạn như chuyển môi trường, chuyển bộ sưu tập, v.v.)
 */
public class WorkspaceSelectionDialog extends JDialog {
    private final JList<Workspace> workspaceList;
    private final DefaultListModel<Workspace> listModel;
    private Workspace selectedWorkspace;
    private final JTextField searchField;

    public WorkspaceSelectionDialog(String title, List<Workspace> workspaces) {
        super(UiSingletonFactory.getInstance(MainFrame.class), title, true);
        setSize(500, 400);
        setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        setLayout(new BorderLayout(10, 10));
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        ToolWindowSurfaceStyle.applyDialogSurface((JComponent) getContentPane());

        // Bảng điều khiển chính
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Hộp tìm kiếm hàng đầu
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GENERAL_SEARCH) + ":");
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(0, 30));
        SettingsInputStyle.apply(searchField);
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Tạo danh sách không gian làm việc
        listModel = new DefaultListModel<>();
        for (Workspace workspace : workspaces) {
            listModel.addElement(workspace);
        }

        workspaceList = new JList<>(listModel);
        workspaceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workspaceList.setFixedCellHeight(40);
        workspaceList.setCellRenderer(new WorkspaceListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(workspaceList);
        ToolWindowSurfaceStyle.applyDialogListScrollPane(scrollPane, workspaceList);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Văn bản mô tả
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_HINT));
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -2));
        hintLabel.setForeground(ModernColors.getTextSecondary());
        hintLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        mainPanel.add(hintLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);

        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);

        okButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));

        okButton.addActionListener(e -> onOkClicked());
        cancelButton.addActionListener(e -> onCancelClicked());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Đăng ký một người nghe sự kiện
        registerListeners(workspaces);

        // Cái đầu tiên được chọn tự động
        if (!listModel.isEmpty()) {
            workspaceList.setSelectedIndex(0);
        }

        // Đặt nút mặc định
        getRootPane().setDefaultButton(okButton);
    }

    private void registerListeners(List<Workspace> allWorkspaces) {
        // Hộp tìm kiếm nghe
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            private void filterWorkspaces() {
                String filter = searchField.getText().trim().toLowerCase();
                listModel.clear();
                for (Workspace workspace : allWorkspaces) {
                    if (filter.isEmpty() ||
                            workspace.getName().toLowerCase().contains(filter) ||
                            (workspace.getDescription() != null && workspace.getDescription().toLowerCase().contains(filter))) {
                        listModel.addElement(workspace);
                    }
                }
                // Kết quả tìm kiếm đầu tiên được chọn tự động
                if (!listModel.isEmpty()) {
                    workspaceList.setSelectedIndex(0);
                }
            }
        });

        // Nhấp đúp chuột để xác nhận
        workspaceList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = workspaceList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        workspaceList.setSelectedIndex(index);
                        onOkClicked();
                    }
                }
            }
        });

        // Enterchìa khóa để xác nhận
        workspaceList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onOkClicked();
                }
            }
        });

        // ESCphím để hủy bỏ
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancelClicked();
            }
        });
    }

    private void onOkClicked() {
        Workspace selected = workspaceList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_REQUIRED),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedWorkspace = selected;
        dispose();
    }

    private void onCancelClicked() {
        selectedWorkspace = null;
        dispose();
    }

    /**
     * Hiển thị hộp thoại và quay lại không gian làm việc đã chọn
     *
     * @return Không gian làm việc đã chọn, được trả về nếu bị hủynull
     */
    public Workspace showDialog() {
        setVisible(true);
        return selectedWorkspace;
    }

    /**
     * Trình kết xuất ô danh sách không gian làm việc
     */
    private static class WorkspaceListCellRenderer extends DefaultListCellRenderer {
        private static final String HTML_START = "<html>";
        private static final String HTML_END = "</html>";

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Workspace workspace) {
                FlatSVGIcon icon = workspace.getType() == WorkspaceType.LOCAL
                        ? IconUtil.createThemed("icons/local.svg", 20, 20)
                        : createGitIcon(workspace);
                setIcon(icon);

                // Xây dựng văn bản hiển thị: tên + loại + Mô tả
                StringBuilder text = new StringBuilder();
                text.append(HTML_START);
                text.append("<b>").append(workspace.getName()).append("</b>");
                text.append("<br>");
                text.append("<small style='color: ")
                        .append(ModernColors.toHtmlColor(ModernColors.getTextHint()))
                        .append(";'>");

                // Hiển thị loại không gian làm việc
                text.append(workspace.getType() == WorkspaceType.LOCAL
                        ? I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL)
                        : I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));

                // Nếu có mô tả, hãy thêm nó vào màn hình
                if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
                    text.append(" - ").append(workspace.getDescription());
                }

                text.append("</small>");
                text.append(HTML_END);

                setText(text.toString());
                setBorder(new EmptyBorder(5, 10, 5, 10));
            }

            return this;
        }

        private FlatSVGIcon createGitIcon(Workspace workspace) {
            if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().trim().isEmpty()) {
                return IconUtil.create("icons/git-warning.svg", 20, 20);
            }
            if (workspace.getGitRepoSource() == GitRepoSource.CLONED) {
                return IconUtil.create("icons/git-remote.svg", 20, 20);
            }
            return IconUtil.create("icons/git.svg", 20, 20);
        }
    }
}
