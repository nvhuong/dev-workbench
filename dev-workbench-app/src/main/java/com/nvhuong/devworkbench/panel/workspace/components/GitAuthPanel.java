package com.nvhuong.devworkbench.panel.workspace.components;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.component.DevWorkbenchPasswordField;
import com.nvhuong.devworkbench.common.component.setting.SettingsInputStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.model.GitAuthType;
import com.nvhuong.devworkbench.util.FileChooserUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * GitCác thành phần công khai của bảng cấu hình xác thực
 */
public class GitAuthPanel extends JPanel {

    private static final int FORM_LABEL_WIDTH = 128;
    private static final int FORM_CONTROL_HEIGHT = 32;
    private static final int AUTH_TYPE_WIDTH = 260;
    private static final int CREDENTIAL_FIELD_WIDTH = 420;
    private static final int SSH_KEY_FIELD_WIDTH = 378;
    private static final String PASSWORD_FIELD_STYLE = "arc: 8; margin: 7, 12, 7, 12";
    private static final Dimension BROWSE_BUTTON_SIZE = new Dimension(34, 32);

    @Getter
    private JComboBox<GitAuthType> authTypeCombo;
    @Getter
    private JTextField passwordUsernameField;
    @Getter
    private DevWorkbenchPasswordField passwordField;
    @Getter
    private JTextField tokenUsernameField;
    @Getter
    private DevWorkbenchPasswordField tokenField;
    @Getter
    private JTextField sshKeyPathField;
    @Getter
    private DevWorkbenchPasswordField sshPassphraseField;

    private JPanel authDetailsPanel;

    // SSH Các thành phần liên quan đến chứng nhận
    private JButton sshKeyBrowseButton;

    public GitAuthPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initComponents() {
        authTypeCombo = new JComboBox<>(GitAuthType.values());
        authTypeCombo.setRenderer(new GitAuthTypeRenderer());
        SettingsInputStyle.apply(authTypeCombo);

        // Điều chỉnh độ rộng trường để phù hợp hơn với hộp thoại gốc
        passwordUsernameField = new JTextField(20);
        passwordField = new DevWorkbenchPasswordField(20);
        tokenUsernameField = new JTextField(20);
        tokenField = new DevWorkbenchPasswordField(20);
        sshKeyPathField = new JTextField(20);
        sshPassphraseField = new DevWorkbenchPasswordField(20);

        sshKeyBrowseButton = new JButton(IconUtil.createThemed("icons/file.svg", 16, 16));
        sshKeyBrowseButton.setToolTipText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_SELECT_KEY));
        sshKeyBrowseButton.setFocusPainted(false);
        sshKeyBrowseButton.setPreferredSize(BROWSE_BUTTON_SIZE);
        sshKeyBrowseButton.setMinimumSize(BROWSE_BUTTON_SIZE);
        sshKeyBrowseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Đặt phông chữ mặc định (DevWorkbenchPasswordField Phông chữ mặc định được đặt tự động)
        Font defaultFont = FontsUtil.getDefaultFont(Font.PLAIN);
        passwordUsernameField.setFont(defaultFont);
        tokenUsernameField.setFont(defaultFont);
        sshKeyPathField.setFont(defaultFont);
        styleEditableField(passwordUsernameField);
        styleEditableField(tokenUsernameField);
        styleEditableField(sshKeyPathField);
        stylePasswordField(passwordField);
        stylePasswordField(tokenField);
        stylePasswordField(sshPassphraseField);
    }

    private void setupLayout() {
        setLayout(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[" + FORM_LABEL_WIDTH + "!,right]12[grow,fill]",
                "[" + FORM_CONTROL_HEIGHT + "!]8[]"
        ));
        setOpaque(false);

        JLabel authTypeLabel = createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TYPE) + ":");
        add(authTypeLabel, "cell 0 0, aligny center");
        add(authTypeCombo, "cell 1 0, w " + AUTH_TYPE_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");

        // Bảng chi tiết chứng nhận
        authDetailsPanel = new JPanel(new CardLayout()) {
            @Override
            public Dimension getPreferredSize() {
                for (Component component : getComponents()) {
                    if (component.isVisible()) {
                        return component.getPreferredSize();
                    }
                }
                return super.getPreferredSize();
            }
        };
        authDetailsPanel.setOpaque(false);
        authDetailsPanel.add(createNoAuthPanel(), GitAuthType.NONE.name());
        authDetailsPanel.add(createPasswordAuthPanel(), GitAuthType.PASSWORD.name());
        authDetailsPanel.add(createTokenAuthPanel(), GitAuthType.TOKEN.name());
        authDetailsPanel.add(createSshAuthPanel(), GitAuthType.SSH_KEY.name());

        add(authDetailsPanel, "cell 0 1 2 1, growx");
    }

    private void setupEventHandlers() {
        authTypeCombo.addActionListener(e -> updateAuthDetailsPanel());

        // SSH Sự kiện chọn tệp khóa riêng
        sshKeyBrowseButton.addActionListener(e -> selectSshKeyFile());

        updateAuthDetailsPanel(); // trạng thái ban đầu
    }

    /**
     * ChọnSSHTệp khóa riêng
     */
    private void selectSshKeyFile() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "workspace.git.sshKey",
                I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_SELECT_KEY));

        // Đặt thư mục mặc định vào thư mục chính của người dùng .ssh thư mục
        String userHome = System.getProperty("user.home");
        File sshDir = new File(userHome, ".ssh");
        if (sshDir.exists() && sshDir.isDirectory()) {
            fileChooser.setCurrentDirectory(sshDir);
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == SystemFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            sshKeyPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private JPanel createNoAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/info.svg", 14, 14));
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_NONE));
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
        label.setForeground(ModernColors.getTextSecondary());
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPasswordAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), "cell 0 0, aligny center");
        panel.add(passwordUsernameField, "cell 1 0, w " + CREDENTIAL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PASSWORD) + ":"), "cell 0 1, aligny center");
        panel.add(passwordField, "cell 1 1, w " + CREDENTIAL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        return panel;
    }

    private JPanel createTokenAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), "cell 0 0, aligny center");
        panel.add(tokenUsernameField, "cell 1 0, w " + CREDENTIAL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_TOKEN) + ":"), "cell 0 1, aligny center");
        panel.add(tokenField, "cell 1 1, w " + CREDENTIAL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        return panel;
    }

    private JPanel createSshAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_KEY_PATH) + ":"), "cell 0 0, aligny center");
        panel.add(sshKeyPathField, "cell 1 0, w " + SSH_KEY_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        panel.add(sshKeyBrowseButton, "cell 2 0, w 34!, h 32!");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_PASSPHRASE) + ":"), "cell 0 1, aligny center");
        panel.add(sshPassphraseField, "cell 1 1 2 1, w " + CREDENTIAL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        return panel;
    }

    private JPanel createCredentialFormPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[" + FORM_LABEL_WIDTH + "!,right]12[grow,fill]8[pref!]",
                "[" + FORM_CONTROL_HEIGHT + "!]8[" + FORM_CONTROL_HEIGHT + "!]"
        ));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextPrimary());
        return label;
    }

    private void styleEditableField(JTextField field) {
        SettingsInputStyle.apply(field);
    }

    private void stylePasswordField(DevWorkbenchPasswordField field) {
        field.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        field.setBackground(ModernColors.getInputBackgroundColor());
        field.setForeground(ModernColors.getTextPrimary());
        field.setCustomStyle(PASSWORD_FIELD_STYLE);
    }

    private void updateAuthDetailsPanel() {
        GitAuthType selectedAuth = (GitAuthType) authTypeCombo.getSelectedItem();
        if (selectedAuth != null) {
            CardLayout layout = (CardLayout) authDetailsPanel.getLayout();
            layout.show(authDetailsPanel, selectedAuth.name());
            syncAuthDetailsSize();
        }
        authDetailsPanel.revalidate();
        authDetailsPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.pack();
            }
        });
    }

    private void syncAuthDetailsSize() {
        Component selectedComponent = getVisibleAuthDetailsComponent();
        if (selectedComponent == null) {
            return;
        }
        Dimension preferredSize = selectedComponent.getPreferredSize();
        Dimension fixedHeightSize = new Dimension(preferredSize.width, preferredSize.height);
        authDetailsPanel.setPreferredSize(fixedHeightSize);
        authDetailsPanel.setMinimumSize(new Dimension(0, preferredSize.height));
        authDetailsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
    }

    private Component getVisibleAuthDetailsComponent() {
        for (Component component : authDetailsPanel.getComponents()) {
            if (component.isVisible()) {
                return component;
            }
        }
        return null;
    }

    /**
     * Đặt trạng thái kích hoạt thành phần
     */
    public void setComponentsEnabled(boolean enabled) {
        authTypeCombo.setEnabled(enabled);
        passwordUsernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        tokenUsernameField.setEnabled(enabled);
        tokenField.setEnabled(enabled);
        sshKeyPathField.setEnabled(enabled);
        sshPassphraseField.setEnabled(enabled);
        sshKeyBrowseButton.setEnabled(enabled);
    }

    /**
     * Xác minh thông tin xác thực
     */
    public void validateAuth() throws IllegalArgumentException {
        GitAuthType selectedAuthType = (GitAuthType) authTypeCombo.getSelectedItem();
        if (selectedAuthType == GitAuthType.PASSWORD) {
            String usernameText = passwordUsernameField.getText().trim();
            String passwordText = new String(passwordField.getPassword()).trim();
            if (usernameText.isEmpty() || passwordText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        } else if (selectedAuthType == GitAuthType.TOKEN) {
            String usernameText = tokenUsernameField.getText().trim();
            String tokenText = tokenField.getPasswordText().trim();
            if (usernameText.isEmpty() || tokenText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        } else if (selectedAuthType == GitAuthType.SSH_KEY) {
            String sshKeyPathText = sshKeyPathField.getText().trim();
            if (sshKeyPathText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        }
    }

    /**
     * Lấy tên người dùng (theo loại xác thực)
     */
    public String getUsername() {
        GitAuthType authType = (GitAuthType) authTypeCombo.getSelectedItem();
        if (authType == GitAuthType.PASSWORD) {
            return passwordUsernameField.getText().trim();
        } else if (authType == GitAuthType.TOKEN) {
            return tokenUsernameField.getText().trim();
        }
        return "";
    }

    /**
     * Nhận mật khẩu
     */
    public String getPassword() {
        return passwordField.getPasswordText();
    }

    /**
     * LấyToken
     */
    public String getToken() {
        return tokenField.getPasswordText();
    }

    /**
     * LấySSHĐường dẫn khóa riêng
     */
    public String getSshKeyPath() {
        return sshKeyPathField.getText().trim();
    }

    /**
     * LấySSHMật khẩu khóa riêng
     */
    public String getSshPassphrase() {
        return sshPassphraseField.getPasswordText();
    }

    /**
     * GitTrình kết xuất loại được chứng nhận
     */
    private static class GitAuthTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof GitAuthType authType) {
                switch (authType) {
                    case NONE:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_NONE));
                        break;
                    case PASSWORD:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_PASSWORD));
                        break;
                    case TOKEN:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TOKEN));
                        break;
                    case SSH_KEY:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_SSH));
                        break;
                }
            }

            return this;
        }
    }
}
