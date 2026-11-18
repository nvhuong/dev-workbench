package com.nvhuong.devworkbench.panel.workspace.components;

import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.setting.SettingsInputStyle;
import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Chuyển đổi sang GitHộp thoại không gian làm việc
 */
public class ConvertToGitDialog extends JDialog {

    @Getter
    private String branchName;
    @Getter
    private boolean confirmed = false;

    private JTextField branchField;
    private JButton confirmButton;
    private final transient Workspace workspace;

    public ConvertToGitDialog(Window parent, Workspace workspace) {
        super(parent, I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT),
                ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        initComponents();
        initDialog();
    }

    private void initComponents() {
        branchField = new JTextField("master", 20);
        branchField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        SettingsInputStyle.apply(branchField);

        // Đã thêm hỗ trợ xác nhận khóa nhập
        branchField.addActionListener(e -> onConfirm());
    }

    private void initDialog() {
        setupLayout();
        setupKeyBindings();
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);

        // Đặt tiêu điểm vào hộp nhập và chọn tất cả văn bản để người dùng có thể sửa đổi nhanh chóng
        SwingUtilities.invokeLater(() -> {
            branchField.requestFocusInWindow();
            branchField.selectAll();
        });
    }

    /**
     * Đặt ràng buộc phím tắt
     */
    private void setupKeyBindings() {
        // ESCphím để hủy bỏ
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Đặt nút mặc định (Nhập phím để xác nhận)
        getRootPane().setDefaultButton(confirmButton);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Khu vực thông tin hàng đầu
        JPanel topPanel = createTopPanel();
        contentPanel.add(topPanel, BorderLayout.NORTH);

        // Khu vực đầu vào ở giữa
        JPanel centerPanel = createCenterPanel();
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Khu vực nút dưới cùng
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Tạo bảng thông tin hàng đầu
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 4, 0));

        // biểu tượng ở bên trái
        JLabel iconLabel = new JLabel(com.nvhuong.devworkbench.util.IconUtil.create(
                "icons/git.svg", 24, 24));
        panel.add(iconLabel, BorderLayout.WEST);

        // Thông tin bên phải
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 3));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        infoPanel.add(titleLabel);

        JLabel workspaceLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ": " + workspace.getName());
        workspaceLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        infoPanel.add(workspaceLabel);

        // Thêm thông tin kịp thời
        JLabel tipLabel = new JLabel("<html><i>" + I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_SUCCESS_TIP) + "</i></html>");
        tipLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        infoPanel.add(tipLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tạo một bảng điều khiển đầu vào ở giữa
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogTopSeparator(panel, 10, 0, 0, 0);

        // Nhãn tên chi nhánh
        JLabel branchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_BRANCH_PROMPT) + ":");
        branchLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        panel.add(branchLabel, BorderLayout.NORTH);

        // Hộp nhập chi nhánh
        branchField.setPreferredSize(new Dimension(320, branchField.getPreferredSize().height));
        panel.add(branchField, BorderLayout.CENTER);


        return panel;
    }

    /**
     * Tạo một bảng điều khiển nút dưới cùng
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        confirmButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);

        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());

        panel.add(cancelButton);
        panel.add(confirmButton);

        return panel;
    }

    private void onConfirm() {
        // Xác minh tên chi nhánh
        branchName = branchField.getText().trim();

        // Kiểm tra xem nó có trống không
        if (branchName.isEmpty()) {
            showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
            branchField.requestFocusInWindow();
            return;
        }

        // nghiên cứuGitĐịnh dạng tên chi nhánh (quy tắc cơ bản)
        if (!isValidGitBranchName(branchName)) {
            showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_FORMAT));
            branchField.requestFocusInWindow();
            branchField.selectAll();
            return;
        }

        confirmed = true;
        dispose();
    }


    /**
     * Xác minhGitTên chi nhánh có hợp pháp không?
     * dựa trênGitĐơn giản hóa việc xác minh các quy tắc đặt tên chi nhánh
     */
    private boolean isValidGitBranchName(String name) {
        // Không thể chứa dấu cách
        if (name.contains(" ")) {
            return false;
        }

        // không thể . hoặc / Bắt đầu
        if (name.startsWith(".") || name.startsWith("/")) {
            return false;
        }

        // không thể / Sự kết thúc
        if (name.endsWith("/")) {
            return false;
        }

        // Không thể chứa dấu gạch chéo liên tiếp //
        if (name.contains("//")) {
            return false;
        }

        // Không thể chứa các ký tự đặc biệt
        String[] invalidChars = {"~", "^", ":", "?", "*", "[", "@{", ".."};
        for (String invalidChar : invalidChars) {
            if (name.contains(invalidChar)) {
                return false;
            }
        }

        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }
}
