package com.nvhuong.devworkbench.panel.topmenu.setting;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.setting.SettingsHintLabel;
import com.nvhuong.devworkbench.common.component.setting.SettingsSectionPanel;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.model.ClientCertificate;
import com.nvhuong.devworkbench.panel.topmenu.plugin.PluginManagerDialog;
import com.nvhuong.devworkbench.plugin.api.service.ClientCertificatePluginService;
import com.nvhuong.devworkbench.plugin.host.ClientCertificatePluginAccess;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.FileChooserUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;

/**
 * Bảng cài đặt chứng chỉ ứng dụng khách được hiện đại hóa
 * kế thừa ModernSettingsPanel Nhận được hiện đại hóa thống nhấtUIphong cách
 */
public class ClientCertificateSettingsPanelModern extends ModernSettingsPanel {
    private static final int SECTION_SPACING = 12;

    private JTable certificateTable;
    private CertificateTableModel tableModel;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton helpBtn;
    private JButton openPluginManagerBtn;
    private final Window parentWindow;
    private boolean pluginInstalled;

    public ClientCertificateSettingsPanelModern(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    @Override
    protected void buildContent(JPanel contentPanel) {
        pluginInstalled = ClientCertificatePluginAccess.isServiceAvailable();

        // Khu vực mô tả
        JPanel descSection = createDescriptionSection();
        contentPanel.add(descSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        if (!pluginInstalled) {
            contentPanel.add(createPluginInstallSection());
            contentPanel.add(createVerticalSpace(SECTION_SPACING));
            return;
        }

        // Certificate form area
        JPanel tableSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.CERT_LIST_TITLE),
                ""
        );

        // Thanh nút hành động
        JPanel actionBar = createActionBar();
        tableSection.add(actionBar);
        tableSection.add(Box.createVerticalStrut(8));

        // cái bàn
        JScrollPane scrollPane = createTablePanel();
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableSection.add(scrollPane);

        contentPanel.add(tableSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));


        // Tải dữ liệu chứng chỉ
        loadCertificates();
    }

    @Override
    protected void registerListeners() {
        // The certificate settings panel is not required Save/Apply/Cancel nút
        // Trực tiếp ẩn thanh nút của lớp cha
        if (saveBtn != null) saveBtn.setVisible(false);
        if (applyBtn != null) applyBtn.setVisible(false);
        if (cancelBtn != null) {
            cancelBtn.setText(I18nUtil.getMessage(MessageKeys.CERT_CLOSE));
            cancelBtn.addActionListener(e -> {
                if (parentWindow != null) {
                    parentWindow.dispose();
                }
            });
        }

        if (!pluginInstalled) {
            if (openPluginManagerBtn != null) {
                openPluginManagerBtn.addActionListener(e -> openPluginManager());
            }
            return;
        }

        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> deleteCertificate());
        helpBtn.addActionListener(e -> showHelp());

        certificateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        updateButtonStates();
    }

    /**
     * Tạo một khu vực mô tả
     */
    private JPanel createDescriptionSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBackground(getBackgroundColor());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        titleLabel.setForeground(getTextPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        SettingsHintLabel descLabel = new SettingsHintLabel(
                I18nUtil.getMessage(MessageKeys.CERT_DESCRIPTION),
                SettingsSectionPanel.DEFAULT_DESCRIPTION_WIDTH
        );
        descLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        panel.add(titleLabel);
        panel.add(descLabel);

        return panel;
    }

    private JPanel createPluginInstallSection() {
        JPanel section = createModernSection(
                I18nUtil.getMessage(MessageKeys.CERT_PLUGIN_REQUIRED_TITLE),
                I18nUtil.getMessage(MessageKeys.CERT_PLUGIN_REQUIRED_DESCRIPTION)
        );

        openPluginManagerBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.CERT_PLUGIN_OPEN_MANAGER), true);
        openPluginManagerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        openPluginManagerBtn.setMaximumSize(new Dimension(220, 32));
        section.add(openPluginManagerBtn);

        return section;
    }

    /**
     * Tạo một thanh nút hành động
     */
    private JPanel createActionBar() {
        JPanel actionBar = new JPanel();
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.X_AXIS));
        actionBar.setOpaque(false);
        actionBar.setBackground(getBackgroundColor());
        actionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        addBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_ADD), true);
        editBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_EDIT), false);
        deleteBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_DELETE), false);
        helpBtn = new JButton(IconUtil.createThemed("icons/help.svg", 16, 16));
        helpBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
        // Giảm kích thước nút
        Dimension btnSize = new Dimension(80, 28);
        addBtn.setPreferredSize(btnSize);
        editBtn.setPreferredSize(btnSize);
        deleteBtn.setPreferredSize(btnSize);

        actionBar.add(addBtn);
        actionBar.add(Box.createHorizontalStrut(4));
        actionBar.add(editBtn);
        actionBar.add(Box.createHorizontalStrut(4));
        actionBar.add(deleteBtn);
        actionBar.add(Box.createHorizontalStrut(4));
        actionBar.add(helpBtn);
        actionBar.add(Box.createHorizontalGlue());

        return actionBar;
    }

    /**
     * Tạo một bảng bảng
     */
    private JScrollPane createTablePanel() {
        tableModel = new CertificateTableModel();
        certificateTable = new JTable(tableModel);
        certificateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        certificateTable.setRowHeight(28);
        certificateTable.setShowGrid(true);
        certificateTable.getTableHeader().setReorderingAllowed(false);
        certificateTable.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        certificateTable.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ToolWindowSurfaceStyle.applyTableCard(certificateTable);

        // Sử dụng chế độ thay đổi kích thước cột tiếp theo để điền vào bảng vào khoảng trống có sẵn
        certificateTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Đặt chiều rộng cột - Một số cột đầu tiên được cố định và các cột sau sẽ tự động được điền
        // Enabled cột - đã sửa
        certificateTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        certificateTable.getColumnModel().getColumn(0).setMinWidth(65);
        certificateTable.getColumnModel().getColumn(0).setMaxWidth(80);

        // Name cột - đã sửa
        certificateTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        certificateTable.getColumnModel().getColumn(1).setMinWidth(70);
        certificateTable.getColumnModel().getColumn(1).setMaxWidth(120);

        // Host cột - Có thể thay đổi, không gian còn lại sẽ được tự động lấp đầy
        certificateTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        certificateTable.getColumnModel().getColumn(2).setMinWidth(100);

        // Port cột - đã sửa
        certificateTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        certificateTable.getColumnModel().getColumn(3).setMinWidth(50);
        certificateTable.getColumnModel().getColumn(3).setMaxWidth(70);

        // Type cột - đã sửa
        certificateTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        certificateTable.getColumnModel().getColumn(4).setMinWidth(60);
        certificateTable.getColumnModel().getColumn(4).setMaxWidth(85);

        // Căn giữa các cột nhất định (nhưng không phải cột 0cột và để nó sử dụng mặc địnhBooleantrình kết xuất hộp kiểm)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // KHÔNG. 0Cột(Enabled）Sử dụng cái mặc địnhBooleanTrình kết xuất hộp kiểm, không có trình kết xuất tùy chỉnh nào được đặt
        certificateTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        certificateTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Thêm trình nghe chỉnh sửa nhấp đúp chuột
        certificateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = certificateTable.rowAtPoint(e.getPoint());
                    int col = certificateTable.columnAtPoint(e.getPoint());
                    // Nếu cột bấm đúp chưa được bật (phần 0cột), hộp thoại chỉnh sửa sẽ mở ra
                    if (row >= 0 && col != 0) {
                        showEditDialog();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(certificateTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, certificateTable);
        scrollPane.setPreferredSize(new Dimension(450, 280));
        scrollPane.setMaximumSize(new Dimension(600, 350));
        scrollPane.setMinimumSize(new Dimension(350, 200));
        // Bảng tự động lấp đầy khoảng trống và không yêu cầu thanh cuộn ngang
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    private void updateButtonStates() {
        boolean hasSelection = certificateTable.getSelectedRow() >= 0;
        editBtn.setEnabled(hasSelection);
        deleteBtn.setEnabled(hasSelection);
    }

    private void loadCertificates() {
        tableModel.loadCertificates();
    }

    private ClientCertificatePluginService getCertificateService() {
        return ClientCertificatePluginAccess.requireService();
    }

    private void openPluginManager() {
        Window window = parentWindow != null ? parentWindow : SwingUtilities.getWindowAncestor(this);
        PluginManagerDialog.showDialog(window);
    }

    private void showAddDialog() {
        ClientCertificate cert = new ClientCertificate();
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), cert, true);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            getCertificateService().addCertificate(cert);
            loadCertificates();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_ADD_SUCCESS));
        }
    }

    private void showEditDialog() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) return;

        ClientCertificate cert = tableModel.getCertificateAt(selectedRow);
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), cert, false);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            getCertificateService().updateCertificate(cert);
            loadCertificates();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_EDIT_SUCCESS));
        }
    }

    private void deleteCertificate() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) return;

        ClientCertificate cert = tableModel.getCertificateAt(selectedRow);
        String displayName = cert.getName() != null && !cert.getName().isEmpty()
                ? cert.getName()
                : cert.getHost();
        String message = MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.CERT_DELETE_CONFIRM),
                displayName);

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.CERT_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            getCertificateService().deleteCertificate(cert.getId());
            loadCertificates();
            updateButtonStates();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_DELETE_SUCCESS));
        }
    }

    private void showHelp() {
        String content = I18nUtil.getMessage(MessageKeys.CERT_HELP_CONTENT);
        JTextArea textArea = new JTextArea(content, 18, 50);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ToolWindowSurfaceStyle.applyTextComponentCard(textArea);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);
        scrollPane.setPreferredSize(new Dimension(600, 350));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                I18nUtil.getMessage(MessageKeys.CERT_HELP_TITLE),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Certificate form model
     */
    private class CertificateTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.CERT_ENABLED),
                I18nUtil.getMessage(MessageKeys.CERT_NAME),
                I18nUtil.getMessage(MessageKeys.CERT_HOST),
                I18nUtil.getMessage(MessageKeys.CERT_PORT),
                I18nUtil.getMessage(MessageKeys.CERT_CERT_TYPE)
        };
        private List<ClientCertificate> certificates = new java.util.ArrayList<>();

        public void loadCertificates() {
            certificates = ClientCertificatePluginAccess.requireService().getAllCertificates();
            fireTableDataChanged();
        }

        public ClientCertificate getCertificateAt(int row) {
            return certificates.get(row);
        }

        @Override
        public int getRowCount() {
            return certificates.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ClientCertificate cert = certificates.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> cert.isEnabled();
                case 1 -> cert.getName() != null ? cert.getName() : "";
                case 2 -> cert.getHost();
                case 3 -> cert.getPort() == 0 ?
                        I18nUtil.getMessage(MessageKeys.CERT_PORT_ALL) :
                        String.valueOf(cert.getPort());
                case 4 -> cert.getCertType();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0; // Chỉ được phép chỉnh sửa Enabled cột
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                ClientCertificate cert = certificates.get(rowIndex);
                cert.setEnabled((Boolean) aValue);
                ClientCertificatePluginAccess.requireService().updateCertificate(cert);
                fireTableCellUpdated(rowIndex, columnIndex);
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_STATUS_UPDATED));
            }
        }
    }

    /**
     * Certificate editing dialog box
     */
    private static class CertificateEditDialog extends JDialog {
        private final ClientCertificate certificate;
        @Getter
        private boolean confirmed = false;

        private JTextField nameField;
        private JTextField hostField;
        private JTextField portField;
        private JComboBox<String> certTypeCombo;
        private JTextField certPathField;
        private JTextField keyPathField;
        private JPasswordField passwordField;
        private JCheckBox enabledCheckBox;

        public CertificateEditDialog(Window owner, ClientCertificate cert, boolean isNew) {
            super(owner, isNew ? I18nUtil.getMessage(MessageKeys.CERT_ADD) :
                            I18nUtil.getMessage(MessageKeys.CERT_EDIT),
                    Dialog.ModalityType.APPLICATION_MODAL);
            this.certificate = cert;
            initUI();
            loadData();
            pack();
            setLocationRelativeTo(owner);
        }

        private void initUI() {
            ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
            setLayout(new BorderLayout(10, 10));
            ToolWindowSurfaceStyle.applyDialogSurface((JPanel) getContentPane());
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

            // Bảng biểu mẫu
            add(createFormPanel(), BorderLayout.CENTER);

            // Bảng nút
            add(createDialogButtonPanel(), BorderLayout.SOUTH);

            setMinimumSize(new Dimension(600, 450));
        }

        private JPanel createFormPanel() {
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;

            // Tên (tùy chọn)
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_NAME) + ":",
                    nameField = createTextField(30, I18nUtil.getMessage(MessageKeys.CERT_NAME_PLACEHOLDER)),
                    false);

            // Tên máy chủ (bắt buộc)
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_HOST) + ":",
                    hostField = createTextField(30, I18nUtil.getMessage(MessageKeys.CERT_HOST_PLACEHOLDER)),
                    true);

            // cổng
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_PORT) + ":",
                    portField = createTextField(10, I18nUtil.getMessage(MessageKeys.CERT_PORT_PLACEHOLDER)),
                    false);

            // Certificate type
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_CERT_TYPE) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            certTypeCombo = new JComboBox<>(new String[]{
                    I18nUtil.getMessage(MessageKeys.CERT_TYPE_PFX),
                    I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM)
            });
            certTypeCombo.addActionListener(e -> updateFieldVisibility());
            formPanel.add(certTypeCombo, gbc);
            row++;

            // Certificate file (required)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel certPathLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_CERT_PATH) + ":");
            certPathLabel.setForeground(ModernColors.getError());
            formPanel.add(certPathLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            certPathField = createTextField(28, I18nUtil.getMessage(MessageKeys.CERT_CERT_PATH_PLACEHOLDER));
            formPanel.add(certPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton certPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            certPathBtn.addActionListener(e -> selectFile(certPathField));
            formPanel.add(certPathBtn, gbc);
            row++;

            // Tệp khóa riêng (PEMBắt buộc khi định dạng)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel keyPathLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_KEY_PATH) + ":");
            formPanel.add(keyPathLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            keyPathField = createTextField(28, I18nUtil.getMessage(MessageKeys.CERT_KEY_PATH_PLACEHOLDER));
            formPanel.add(keyPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton keyPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            keyPathBtn.addActionListener(e -> selectFile(keyPathField));
            formPanel.add(keyPathBtn, gbc);
            row++;

            // mật khẩu
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_PASSWORD) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            passwordField = new JPasswordField(30);
            formPanel.add(passwordField, gbc);
            row++;

            // Kích hoạt
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_ENABLED) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            enabledCheckBox = new JCheckBox();
            formPanel.add(enabledCheckBox, gbc);

            return formPanel;
        }

        private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                                String label, JTextField field, boolean required) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel jLabel = new JLabel(label);
            if (required) {
                jLabel.setForeground(ModernColors.getError());
            }
            panel.add(jLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            panel.add(field, gbc);
        }

        private JTextField createTextField(int columns, String placeholder) {
            JTextField field = new JTextField();
            field.setColumns(columns);
            if (placeholder != null && !placeholder.isEmpty()) {
                field.setToolTipText(placeholder);
            }
            return field;
        }

        private JPanel createDialogButtonPanel() {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

            JButton saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SAVE));
            JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_CANCEL));

            saveBtn.addActionListener(e -> save());
            cancelBtn.addActionListener(e -> dispose());

            buttonPanel.add(saveBtn);
            buttonPanel.add(cancelBtn);

            return buttonPanel;
        }

        private void updateFieldVisibility() {
            boolean isPEM = I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            keyPathField.setEnabled(isPEM);
            if (!isPEM) {
                keyPathField.setText("");
            }
        }

        private void loadData() {
            nameField.setText(certificate.getName() != null ? certificate.getName() : "");
            hostField.setText(certificate.getHost() != null ? certificate.getHost() : "");
            portField.setText(certificate.getPort() > 0 ? String.valueOf(certificate.getPort()) : "0");

            if (ClientCertificate.CERT_TYPE_PEM.equals(certificate.getCertType())) {
                certTypeCombo.setSelectedItem(I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM));
            } else {
                certTypeCombo.setSelectedItem(I18nUtil.getMessage(MessageKeys.CERT_TYPE_PFX));
            }

            certPathField.setText(certificate.getCertPath() != null ? certificate.getCertPath() : "");
            keyPathField.setText(certificate.getKeyPath() != null ? certificate.getKeyPath() : "");
            passwordField.setText(certificate.getCertPassword() != null ? certificate.getCertPassword() : "");
            enabledCheckBox.setSelected(certificate.isEnabled());

            updateFieldVisibility();
        }

        private void selectFile(JTextField targetField) {
            SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                    "settings.clientCertificate.file",
                    I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));

            // Đặt thư mục ban đầu thành đường dẫn tệp của hộp văn bản hiện tại (nếu có)
            String currentPath = targetField.getText();
            if (currentPath != null && !currentPath.trim().isEmpty()) {
                File currentFile = new File(currentPath);
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    fileChooser.setCurrentDirectory(currentFile.getParentFile());
                }
            }

            if (fileChooser.showOpenDialog(this) == SystemFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                targetField.setText(file.getAbsolutePath());
            }
        }

        private void save() {
            // Xác minh tên máy chủ
            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_HOST_REQUIRED));
                hostField.requestFocus();
                return;
            }

            // Xác minh tệp chứng chỉ
            String certPath = certPathField.getText().trim();
            if (certPath.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_CERT_REQUIRED));
                certPathField.requestFocus();
                return;
            }

            // Xác minh PEM Định dạng này yêu cầu một tệp khóa riêng
            boolean isPEM = I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            String keyPath = keyPathField.getText().trim();
            if (isPEM && keyPath.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_KEY_REQUIRED));
                keyPathField.requestFocus();
                return;
            }

            // Xác minh rằng tập tin tồn tại
            if (!new File(certPath).exists()) {
                showError(MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), certPath));
                certPathField.requestFocus();
                return;
            }

            if (isPEM && !new File(keyPath).exists()) {
                showError(MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), keyPath));
                keyPathField.requestFocus();
                return;
            }

            // Lưu dữ liệu
            certificate.setName(nameField.getText().trim());
            certificate.setHost(host);

            try {
                int port = Integer.parseInt(portField.getText().trim());
                certificate.setPort(port);
            } catch (NumberFormatException e) {
                certificate.setPort(0);
            }

            certificate.setCertType(isPEM ? ClientCertificate.CERT_TYPE_PEM : ClientCertificate.CERT_TYPE_PFX);
            certificate.setCertPath(certPath);
            certificate.setKeyPath(isPEM ? keyPath : null);
            certificate.setCertPassword(new String(passwordField.getPassword()));
            certificate.setEnabled(enabledCheckBox.isSelected());

            confirmed = true;
            dispose();
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message,
                    I18nUtil.getMessage(MessageKeys.CERT_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }

    }
}
