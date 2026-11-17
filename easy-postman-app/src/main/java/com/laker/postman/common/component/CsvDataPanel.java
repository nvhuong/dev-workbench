package com.laker.postman.common.component;

import com.laker.postman.common.component.notification.NotificationCenter;

import cn.hutool.core.text.CharSequenceUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.button.CSVButton;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.component.button.HelpButton;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * CSV Bảng quản lý dữ liệu - độc lậpCSVCác thành phần chức năng
 */
@Slf4j
public class CsvDataPanel extends JPanel {

    static final String CSV_USAGE_BUTTON_PROPERTY = "CsvDataPanel.usageButton";
    private static final int CSV_TABLE_MIN_COLUMN_WIDTH = 140;
    private static final int CSV_OVERVIEW_DIALOG_WIDTH = 560;
    private static final int CSV_OVERVIEW_DIALOG_HEIGHT = 390;
    private static final int CSV_MANUAL_DIALOG_WIDTH = 620;
    private static final int CSV_MANUAL_DIALOG_HEIGHT = 260;
    private static final int CSV_MANAGE_DIALOG_WIDTH = 820;
    private static final int CSV_MANAGE_DIALOG_HEIGHT = 560;
    private static final int CSV_MANAGE_DIALOG_MIN_WIDTH = 760;
    private static final int CSV_MANAGE_DIALOG_MIN_HEIGHT = 500;
    private static final int CSV_TOOLBAR_BUTTON_HEIGHT = 30;
    private static final int CSV_FOOTER_BUTTON_HEIGHT = 34;

    public static final class CsvState {
        private final String sourceName;
        private final List<String> headers;
        private final List<Map<String, String>> rows;

        public CsvState(String sourceName, List<String> headers, List<Map<String, String>> rows) {
            this.sourceName = sourceName;
            this.headers = copyHeaders(headers);
            this.rows = copyRows(rows);
        }

        public String getSourceName() {
            return sourceName;
        }

        public List<String> getHeaders() {
            return copyHeaders(headers);
        }

        public List<Map<String, String>> getRows() {
            return copyRows(rows);
        }
    }

    private File csvFile;
    private String csvSourceName;
    private List<Map<String, String>> csvData;
    private List<String> csvHeaders; // cứuCSVThứ tự các tiêu đề cột
    private JPanel csvStatusPanel;  // CSV Bảng hiển thị trạng thái
    private JLabel csvStatusLabel;  // CSV nhãn trạng thái
    private String contextHelpText; // Mô tả kịch bản chèn người gọi
    private Runnable changeListener;


    public CsvDataPanel() {
        initUI();
    }

    static Dimension csvOverviewDialogSize() {
        return new Dimension(CSV_OVERVIEW_DIALOG_WIDTH, CSV_OVERVIEW_DIALOG_HEIGHT);
    }

    static Dimension csvManualDialogSize() {
        return new Dimension(CSV_MANUAL_DIALOG_WIDTH, CSV_MANUAL_DIALOG_HEIGHT);
    }

    static Dimension csvManualDialogDisplaySize(Dimension packedSize) {
        Dimension compactSize = csvManualDialogSize();
        if (packedSize == null) {
            return compactSize;
        }
        return new Dimension(
                Math.max(compactSize.width, packedSize.width),
                Math.max(compactSize.height, packedSize.height)
        );
    }

    static Dimension csvManageDialogSize() {
        return new Dimension(CSV_MANAGE_DIALOG_WIDTH, CSV_MANAGE_DIALOG_HEIGHT);
    }

    static Dimension csvManageDialogMinimumSize() {
        return new Dimension(CSV_MANAGE_DIALOG_MIN_WIDTH, CSV_MANAGE_DIALOG_MIN_HEIGHT);
    }

    static JPanel createManageHeaderPanel(JComponent infoComponent, JPanel toolPanel) {
        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogHeader(headerPanel, 10, 16, 10, 16);
        headerPanel.add(infoComponent, BorderLayout.NORTH);
        headerPanel.add(toolPanel, BorderLayout.SOUTH);
        return headerPanel;
    }

    static JPanel createUsageHelpPanel(String summaryText, Runnable helpAction) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 16, 0, 16));

        JPanel summaryPanel = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(summaryPanel);

        JTextArea summaryArea = new JTextArea(summaryText);
        summaryArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        summaryArea.setForeground(ModernColors.getTextSecondary());
        summaryArea.setEditable(false);
        summaryArea.setFocusable(false);
        summaryArea.setOpaque(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(summaryArea);
        summaryArea.setForeground(ModernColors.getTextSecondary());
        summaryPanel.add(summaryArea, BorderLayout.CENTER);

        JButton helpButton = new HelpButton();
        helpButton.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS));
        helpButton.getAccessibleContext().setAccessibleName(I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS));
        helpButton.addActionListener(e -> {
            if (helpAction != null) {
                helpAction.run();
            }
        });
        summaryPanel.add(helpButton, BorderLayout.EAST);
        panel.add(summaryPanel, BorderLayout.NORTH);

        panel.putClientProperty(CSV_USAGE_BUTTON_PROPERTY, helpButton);
        return panel;
    }

    static List<String> parseManualHeaders(String headersText) {
        List<String> headers = new ArrayList<>();
        if (CharSequenceUtil.isBlank(headersText)) {
            return headers;
        }
        for (String header : headersText.split(",")) {
            String trimmedHeader = header.trim();
            if (CharSequenceUtil.isNotBlank(trimmedHeader)) {
                headers.add(trimmedHeader);
            }
        }
        return headers;
    }

    static String findDuplicateHeader(List<String> headers) {
        Set<String> seen = new HashSet<>();
        for (String header : headers) {
            String normalized = header.toLowerCase(Locale.ROOT);
            if (!seen.add(normalized)) {
                return header;
            }
        }
        return null;
    }

    private static void updateManualHeaderPreview(JTextField headersField, JLabel previewLabel) {
        List<String> headers = parseManualHeaders(headersField.getText());
        if (headers.isEmpty()) {
            previewLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_EXAMPLE));
            previewLabel.setForeground(ModernColors.getTextHint());
            return;
        }

        String duplicateHeader = findDuplicateHeader(headers);
        if (duplicateHeader != null) {
            previewLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DUPLICATE_HEADER, duplicateHeader));
            previewLabel.setForeground(ModernColors.getError());
            return;
        }

        previewLabel.setText(I18nUtil.getMessage(
                MessageKeys.CSV_CREATE_MANUAL_PREVIEW,
                headers.size(),
                String.join(", ", headers)
        ));
        previewLabel.setForeground(ModernColors.getTextHint());
    }


    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // TạoCSVbảng trạng thái vàCSVnút
        JPanel csvPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        csvPanel.setOpaque(false);

        // CSVnút
        JButton csvBtn = createCsvMenuButton();
        csvPanel.add(csvBtn);

        // CSVBảng hiển thị trạng thái
        csvStatusPanel = createCsvStatusPanel();
        csvPanel.add(csvStatusPanel);

        add(csvPanel, BorderLayout.CENTER);
    }

    /**
     * Tạo CSV Bảng hiển thị trạng thái
     */
    private JPanel createCsvStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        statusPanel.setOpaque(false);
        statusPanel.setVisible(false); // Ẩn ban đầu

        // Văn bản trạng thái
        csvStatusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
        csvStatusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        csvStatusLabel.setForeground(ModernColors.getTextSecondary()); // Sử dụng màu văn bản phụ
        csvStatusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Đặt con trỏ tay
        csvStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)); // Thêm văn bản nhắc nhở

        // Thêm trình xử lý sự kiện nhấp chuột và nhấp vào văn bản để mở hộp thoại quản lý dữ liệu
        csvStatusLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            private Color originalColor;

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty()) {
                    showCsvDataManageDialog();
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty()) {
                    // Thay đổi màu khi di chuột qua (sử dụng màu sáng hơn)
                    originalColor = csvStatusLabel.getForeground();
                    csvStatusLabel.setForeground(ModernColors.getPrimary());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty() && originalColor != null) {
                    // Khôi phục màu gốc khi chuột rời đi
                    csvStatusLabel.setForeground(originalColor);
                }
            }
        });

        // CSV Nút xóa
        CloseButton csvClearBtn = new CloseButton();
        csvClearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_TOOLTIP));
        csvClearBtn.addActionListener(e -> clearCsvData());

        statusPanel.add(csvStatusLabel);
        statusPanel.add(csvClearBtn);

        return statusPanel;
    }

    /**
     * Tạo một menu thả xuống CSV nút
     */
    private JButton createCsvMenuButton() {
        JButton csvBtn = new CSVButton();

        JPopupMenu csvMenu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(csvMenu);

        JMenuItem loadCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_IMPORT_FILE),
                IconUtil.createThemed("icons/import.svg", 16, 16));
        loadCsvItem.addActionListener(e -> showEnhancedCsvManagementDialog());
        csvMenu.add(loadCsvItem);

        JMenuItem createManualItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL),
                IconUtil.createThemed("icons/plus.svg", 16, 16));
        createManualItem.addActionListener(e -> showManualCreateDialog());
        csvMenu.add(createManualItem);

        JMenuItem manageCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA),
                IconUtil.createThemed("icons/code.svg", 16, 16));
        manageCsvItem.addActionListener(e -> showCsvDataManageDialog());
        manageCsvItem.setEnabled(false); // Bị tắt theo mặc định, được bật khi có dữ liệu
        manageCsvItem.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));
        csvMenu.add(manageCsvItem);

        csvMenu.addSeparator();

        JMenuItem clearCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_CLEAR_DATA),
                IconUtil.createThemed("icons/clear.svg", 16, 16));
        clearCsvItem.addActionListener(e -> clearCsvData());
        clearCsvItem.setEnabled(false); // Bị tắt theo mặc định, được bật khi có dữ liệu
        clearCsvItem.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));
        csvMenu.add(clearCsvItem);

        csvBtn.addActionListener(e -> {
            // Cập nhật trạng thái mục menu
            boolean hasCsvData = csvData != null && !csvData.isEmpty();
            manageCsvItem.setEnabled(hasCsvData);
            clearCsvItem.setEnabled(hasCsvData);
            manageCsvItem.setToolTipText(hasCsvData
                    ? I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)
                    : I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));
            clearCsvItem.setToolTipText(hasCsvData
                    ? I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_TOOLTIP)
                    : I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));

            // Trình đơn hiển thị
            csvMenu.show(csvBtn, 0, csvBtn.getHeight());
        });

        return csvBtn;
    }

    /**
     * Thiết lập mô tả dành riêng cho cảnh gọi và chỉ hiển thị mô tả đó trong bảng mô tả.
     */
    public void setContextHelpText(String contextHelpText) {
        this.contextHelpText = contextHelpText;
        if (csvStatusLabel != null) {
            csvStatusLabel.setToolTipText(buildStatusTooltip());
        }
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public CsvState exportState() {
        if (!hasData()) {
            return null;
        }
        return new CsvState(getCurrentSourceName(), csvHeaders, csvData);
    }

    public void restoreState(CsvState state) {
        applyState(state, false);
    }

    /**
     * Thông thoáng CSV dữ liệu
     */
    public void clearCsvData() {
        csvFile = null;
        csvSourceName = null;
        csvData = null;
        csvHeaders = null;
        updateCsvStatus();
        notifyChangeListener();
        NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.CSV_DATA_CLEARED));
    }

    /**
     * Mở trình chọn tệp để nhập CSV dữ liệu.
     */
    public boolean importCsvFile() {
        boolean imported = selectCsvFile();
        if (imported) {
            updateCsvStatus();
        }
        return imported;
    }

    /**
     * đổi mới CSV hiển thị trạng thái
     */
    private void updateCsvStatus() {
        if (csvData == null || csvData.isEmpty()) {
            csvStatusPanel.setVisible(false);
        } else {
            csvStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
            csvStatusLabel.setForeground(ModernColors.getTextSecondary()); // Sử dụng màu văn bản phụ
            csvStatusLabel.setToolTipText(buildStatusTooltip());
            csvStatusPanel.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private String buildStatusTooltip() {
        if (CharSequenceUtil.isBlank(contextHelpText)) {
            return I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA);
        }
        return "<html>" + I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)
                + "<br><br>"
                + contextHelpText.replace("\n", "<br>")
                + "</html>";
    }

    private String buildHelpText(String baseText) {
        if (CharSequenceUtil.isBlank(contextHelpText)) {
            return baseText;
        }
        return baseText + "\n\n" + contextHelpText;
    }

    /**
     * Tạo thủ công CSV Hộp thoại dữ liệu
     */
    public void showManualCreateDialog() {
        JDialog dialog = new JDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DIALOG_TITLE), true);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);

        JPanel rootPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(rootPanel);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        dialog.add(rootPanel, BorderLayout.CENTER);

        // Bảng hướng dẫn hàng đầu
        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyDialogSurface(topPanel);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DIALOG_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html><body style='width:520px'>"
                + I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DESCRIPTION)
                + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        descLabel.setForeground(ModernColors.getTextSecondary());
        topPanel.add(descLabel, BorderLayout.CENTER);

        rootPanel.add(topPanel, BorderLayout.NORTH);

        // Bảng nội dung ở giữa
        JPanel contentPanel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);

        // Đầu vào tiêu đề cột
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0;
        JLabel headersLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_COLUMN_HEADERS));
        headersLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        contentPanel.add(headersLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField headersField = new JTextField();
        headersField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        headersField.setText("username,password,email");
        headersField.setColumns(24);
        ToolWindowSurfaceStyle.applyTextComponentInput(headersField);
        contentPanel.add(headersField, gbc);

        // Lời nhắc giữ chỗ
        gbc.gridx = 1;
        gbc.gridy = 1;
        JLabel placeholderLabel = new JLabel();
        placeholderLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        placeholderLabel.setForeground(ModernColors.getTextHint());
        contentPanel.add(placeholderLabel, gbc);
        updateManualHeaderPreview(headersField, placeholderLabel);
        headersField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateManualHeaderPreview(headersField, placeholderLabel);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateManualHeaderPreview(headersField, placeholderLabel);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateManualHeaderPreview(headersField, placeholderLabel);
            }
        });

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        // Bảng nút dưới cùng
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(bottomPanel);

        JButton createBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        createBtn.addActionListener(e -> {
            try {
                // Được tạo theo mặc định1dữ liệu hàng
                int rowCount = 1;

                List<String> headers = parseManualHeaders(headersField.getText());
                if (headers.isEmpty()) {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_HEADERS_REQUIRED));
                    return;
                }
                String duplicateHeader = findDuplicateHeader(headers);
                if (duplicateHeader != null) {
                    NotificationCenter.showError(I18nUtil.getMessage(
                            MessageKeys.CSV_CREATE_MANUAL_DUPLICATE_HEADER, duplicateHeader));
                    return;
                }

                // Xác thực phạm vi số cột (được tính tự động từ tiêu đề cột)
                int columnCount = headers.size();
                if (columnCount > 50) {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_TOO_MANY_COLUMNS, columnCount));
                    return;
                }

                // Tạo dữ liệu trống
                List<Map<String, String>> newData = new ArrayList<>();
                for (int i = 0; i < rowCount; i++) {
                    Map<String, String> row = new java.util.LinkedHashMap<>();
                    for (String header : headers) {
                        row.put(header, "");
                    }
                    newData.add(row);
                }

                // Đặt dữ liệu
                csvData = newData;
                csvHeaders = headers;
                csvFile = null; // Xóa tham chiếu tệp và đánh dấu là được tạo thủ công
                csvSourceName = null;
                updateCsvStatus();
                notifyChangeListener();

                dialog.dispose();

                // Tự động mở hộp thoại quản lý dữ liệu
                showCsvDataManageDialog();

            } catch (Exception ex) {
                log.error("Tạo CSV Dữ liệu không thành công", ex);
                JOptionPane.showMessageDialog(dialog,
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        bottomPanel.add(createBtn);
        bottomPanel.add(cancelBtn);
        rootPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(createBtn);
        dialog.pack();
        dialog.setSize(csvManualDialogDisplaySize(dialog.getSize()));
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        SwingUtilities.invokeLater(headersField::requestFocusInWindow);

        dialog.setVisible(true);
    }

    /**
     * Tăng cường CSV Hộp thoại quản lý tập tin
     * Bố cục: Đã sửa tiêu đề → Description text is scrollable → Tình trạng hiện tại+Đã sửa các nút hành động → Đã sửa nút đóng
     */
    private void showEnhancedCsvManagementDialog() {
        JDialog dialog = new JDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.CSV_DIALOG_MANAGEMENT_TITLE), true);
        dialog.setSize(csvOverviewDialogSize());
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        ToolWindowSurfaceStyle.applyDialogSurface((JComponent) dialog.getContentPane());

        JPanel topPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(topPanel);
        topPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_DATA_DRIVEN_TEST));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html><body style='width:500px'>"
                + I18nUtil.getMessage(MessageKeys.CSV_DIALOG_SUMMARY)
                + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        descLabel.setForeground(ModernColors.getTextSecondary());
        topPanel.add(descLabel, BorderLayout.CENTER);

        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel fixedPanel = new JPanel();
        ToolWindowSurfaceStyle.applyDialogSurface(fixedPanel);
        fixedPanel.setLayout(new BoxLayout(fixedPanel, BoxLayout.Y_AXIS));
        fixedPanel.setBorder(BorderFactory.createEmptyBorder(4, 18, 8, 18));

        JPanel statusSection = createDialogSection(I18nUtil.getMessage(MessageKeys.CSV_CURRENT_STATUS));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusPanel.setOpaque(false);

        JLabel currentStatusLabel = new JLabel();
        if (csvData == null || csvData.isEmpty()) {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextHint());
        } else {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextSecondary());
        }
        statusPanel.add(currentStatusLabel);
        statusSection.add(statusPanel, BorderLayout.CENTER);
        statusSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        fixedPanel.add(statusSection);
        fixedPanel.add(Box.createVerticalStrut(8));

        JPanel actionSection = createDialogSection(I18nUtil.getMessage(MessageKeys.CSV_OPERATIONS));
        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        actionPanel.setOpaque(false);
        actionSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton selectFileBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_SELECT_FILE), false, "icons/file.svg", 16);

        JButton createManualBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL), false, "icons/plus.svg", 16);

        JButton manageDataBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_MANAGE_DATA), false, "icons/code.svg", 16);
        manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());
        manageDataBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));

        JButton clearBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_DATA), false, "icons/clear.svg", 16);
        clearBtn.setEnabled(csvData != null && !csvData.isEmpty());
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));

        selectFileBtn.addActionListener(e -> {
            if (selectCsvFile()) {
                currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
                currentStatusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
                currentStatusLabel.setForeground(ModernColors.getTextSecondary());
                updateCsvStatus();
                manageDataBtn.setEnabled(true);
                clearBtn.setEnabled(true);
                manageDataBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA));
                clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_TOOLTIP));
            }
        });
        createManualBtn.addActionListener(e -> {
            dialog.dispose();
            showManualCreateDialog();
        });
        manageDataBtn.addActionListener(e -> showCsvDataManageDialog());
        clearBtn.addActionListener(e -> {
            clearCsvData();
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextHint());
            manageDataBtn.setEnabled(false);
            clearBtn.setEnabled(false);
            manageDataBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));
            clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_ACTION_REQUIRES_DATA));
        });

        actionPanel.add(selectFileBtn);
        actionPanel.add(createManualBtn);
        actionPanel.add(manageDataBtn);
        actionPanel.add(clearBtn);
        actionPanel.setPreferredSize(new Dimension(0, 78));

        actionSection.add(actionPanel, BorderLayout.CENTER);
        actionSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        fixedPanel.add(actionSection);

        fixedPanel.add(Box.createVerticalStrut(8));
        JPanel usagePanel = createUsageHelpPanel(
                I18nUtil.getMessage(MessageKeys.CSV_USAGE_SUMMARY),
                () -> showUsageInstructionsDialog(dialog,
                        buildHelpText(I18nUtil.getMessage(MessageKeys.CSV_DIALOG_DESCRIPTION)))
        );
        usagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixedPanel.add(usagePanel);
        dialog.add(fixedPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(bottomPanel);
        JButton closeBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE), false);
        closeBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(closeBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createDialogSection(String title) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyDialogSurface(section);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        section.add(titleLabel, BorderLayout.NORTH);
        return section;
    }

    private static void showUsageInstructionsDialog(Window owner, String helpText) {
        JDialog dialog = new JDialog(owner,
                I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea helpArea = new JTextArea(helpText);
        helpArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        helpArea.setEditable(false);
        helpArea.setOpaque(false);
        helpArea.setFocusable(false);
        helpArea.setRequestFocusEnabled(false);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(helpArea);
        helpArea.setForeground(ModernColors.getTextSecondary());
        helpArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(helpArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(footerPanel);
        JButton closeButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE), false);
        closeButton.addActionListener(e -> dialog.dispose());
        footerPanel.add(closeButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(footerPanel, BorderLayout.SOUTH);
        dialog.setSize(560, 400);
        dialog.setMinimumSize(new Dimension(480, 320));
        dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.setVisible(true);
    }

    /**
     * CSV Hộp thoại quản lý dữ liệu - Tích hợp chức năng xem trước và chỉnh sửa
     */
    public void showCsvDataManageDialog() {
        if (csvData == null || csvData.isEmpty()) {
            JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.CSV_NO_MANAGEABLE_DATA),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog manageDialog = new JDialog((Frame) null, I18nUtil.getMessage(MessageKeys.CSV_DATA_MANAGEMENT), true);
        manageDialog.setSize(csvManageDialogSize());
        manageDialog.setMinimumSize(csvManageDialogMinimumSize());
        manageDialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        manageDialog.setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(manageDialog);
        ToolWindowSurfaceStyle.applyDialogSurface((JComponent) manageDialog.getContentPane());

        JLabel infoLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_DATA_SOURCE_INFO,
                getCurrentSourceDisplayName(),
                csvData.size()));
        infoLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // Tạo các bảng có thể chỉnh sửa
        List<String> headers;
        try {
            if (csvHeaders != null && !csvHeaders.isEmpty()) {
                // Thứ tự tiêu đề cột đã lưu được ưu tiên
                headers = new ArrayList<>(csvHeaders);
            } else if (csvFile != null) {
                headers = CsvDataUtil.getCsvHeaders(csvFile);
                csvHeaders = headers; // Lưu thứ tự tiêu đề cột
            } else {
                // Để lấy tên cột từ dữ liệu hiện có, hãy sử dụng LinkedHashMapGiữ trật tự
                if (csvData.isEmpty()) {
                    headers = new ArrayList<>();
                } else {
                    // Nếu dữ liệu được sử dụngLinkedHashMap，keySet()Thứ tự chèn sẽ được duy trì
                    headers = new ArrayList<>(csvData.get(0).keySet());
                    csvHeaders = headers; // Lưu thứ tự tiêu đề cột
                }
            }
        } catch (Exception e) {
            log.error("Lấy CSV Tiêu đề cột không thành công", e);
            headers = csvHeaders != null ? new ArrayList<>(csvHeaders) :
                    (csvData.isEmpty() ? new ArrayList<>() : new ArrayList<>(csvData.get(0).keySet()));
        }

        // Tạo dữ liệu bảng, đảm bảo có ít nhất 5hàng để chỉnh sửa
        Object[][] tableData = new Object[csvData.size()][headers.size()];
        for (int i = 0; i < csvData.size(); i++) {
            Map<String, String> row = csvData.get(i);
            for (int j = 0; j < headers.size(); j++) {
                tableData[i][j] = row.get(headers.get(j));
            }
        }

        // Tạo các mô hình bảng có thể chỉnh sửa
        DefaultTableModel editTableModel = new DefaultTableModel(tableData, headers.toArray()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Tất cả các ô đều có thể chỉnh sửa được
            }
        };

        JTable csvTable = new JTable(editTableModel);
        configureCsvTable(csvTable);

        // Tạo một bảng cuộn theo kiểu
        JScrollPane scrollPane = new JScrollPane(csvTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, csvTable);
        installResponsiveTableLayout(csvTable, scrollPane);

        // Tạo bảng chứa bảng và áp dụng màu nền
        JPanel tablePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(tablePanel);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 8, 16));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        manageDialog.add(tablePanel, BorderLayout.CENTER);

        // Bảng dưới cùng
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(bottomPanel);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder());

        // Thanh công cụ
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(toolPanel);

        JButton bulkEditBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT), false, "icons/edit.svg", 16);
        styleToolbarButton(bulkEditBtn);
        List<String> finalHeaders = headers;
        bulkEditBtn.addActionListener(e -> showBulkEditDialog(manageDialog, editTableModel, csvTable, finalHeaders, () -> csvTable.repaint()));

        JButton addRowBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_ROW), false, "icons/plus.svg", 16);
        styleToolbarButton(addRowBtn);
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_ROW), false, "icons/clear.svg", 16);
        styleToolbarButton(deleteRowBtn);
        deleteRowBtn.addActionListener(e -> {
            // Dừng chỉnh sửa ô trước để tránh các sự cố do xóa hàng trong khi chỉnh sửa
            if (csvTable.isEditing()) {
                csvTable.getCellEditor().stopCellEditing();
            }

            int[] selectedRows = csvTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SELECT_ROWS_TO_DELETE),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE_ROWS, selectedRows.length),
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE), JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    editTableModel.removeRow(selectedRows[i]);
                }
            }
        });

        JButton addColumnBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_COLUMN), false, "icons/plus.svg", 16);
        styleToolbarButton(addColumnBtn);
        addColumnBtn.addActionListener(e -> {
            TextInputDialog.showRequiredName(
                    manageDialog,
                    I18nUtil.getMessage(MessageKeys.CSV_ADD_COLUMN),
                    "",
                    I18nUtil.getMessage(MessageKeys.CSV_COLUMN_NAME_EMPTY)
            ).ifPresent(columnName -> {
                editTableModel.addColumn(columnName);
                csvTable.repaint();
                refreshResponsiveTableLayout(csvTable, scrollPane);
            });
        });

        JButton deleteColumnBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_COLUMN), false, "icons/clear.svg", 16);
        styleToolbarButton(deleteColumnBtn);
        deleteColumnBtn.addActionListener(e -> {
            // Dừng chỉnh sửa ô trước để tránh các sự cố do xóa cột trong khi chỉnh sửa
            if (csvTable.isEditing()) {
                csvTable.getCellEditor().stopCellEditing();
            }

            int[] selectedColumns = csvTable.getSelectedColumns();
            if (selectedColumns.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SELECT_COLUMNS_TO_DELETE),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Kiểm tra xem bạn có muốn xóa tất cả các cột không
            if (selectedColumns.length >= editTableModel.getColumnCount()) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_CANNOT_DELETE_ALL_COLUMNS),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Hiển thị tên cột cần xóa
            StringBuilder columnNames = new StringBuilder();
            for (int i = 0; i < selectedColumns.length; i++) {
                if (i > 0) columnNames.append(", ");
                columnNames.append(editTableModel.getColumnName(selectedColumns[i]));
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE_COLUMNS, columnNames.toString()),
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE), JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Xóa từ sau ra trước để tránh vấn đề thay đổi chỉ mục
                for (int i = selectedColumns.length - 1; i >= 0; i--) {
                    int columnIndex = selectedColumns[i];

                    // Xóa dữ liệu cột
                    for (int row = 0; row < editTableModel.getRowCount(); row++) {
                        // Di chuyển dữ liệu cột sau
                        for (int col = columnIndex; col < editTableModel.getColumnCount() - 1; col++) {
                            editTableModel.setValueAt(editTableModel.getValueAt(row, col + 1), row, col);
                        }
                    }

                    // Xóa cột
                    editTableModel.setColumnCount(editTableModel.getColumnCount() - 1);

                    // Cập nhật mã định danh cột
                    java.util.Vector<String> columnIdentifiers = new java.util.Vector<>();
                    for (int j = 0; j < editTableModel.getColumnCount(); j++) {
                        if (j < columnIndex) {
                            columnIdentifiers.add(editTableModel.getColumnName(j));
                        } else {
                            columnIdentifiers.add(editTableModel.getColumnName(j + 1));
                        }
                    }
                    editTableModel.setColumnIdentifiers(columnIdentifiers);
                }

                csvTable.repaint();
                refreshResponsiveTableLayout(csvTable, scrollPane);
            }
        });

        toolPanel.add(bulkEditBtn);
        toolPanel.add(addRowBtn);
        toolPanel.add(deleteRowBtn);
        toolPanel.add(addColumnBtn);
        toolPanel.add(deleteColumnBtn);
        manageDialog.add(createManageHeaderPanel(infoLabel, toolPanel), BorderLayout.NORTH);

        JPanel helpPanel = createUsageHelpPanel(
                I18nUtil.getMessage(MessageKeys.CSV_USAGE_SUMMARY),
                () -> showUsageInstructionsDialog(manageDialog,
                        buildHelpText(I18nUtil.getMessage(MessageKeys.CSV_USAGE_TEXT)))
        );
        bottomPanel.add(helpPanel, BorderLayout.CENTER);

        // Bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);

        JButton saveBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_SAVE), true, "icons/save.svg", 16);
        styleFooterButton(saveBtn);
        saveBtn.addActionListener(e -> {
            try {
                // Trước tiên hãy dừng chỉnh sửa ô để đảm bảo rằng nội dung đang chỉnh sửa được lưu
                if (csvTable.isEditing()) {
                    csvTable.getCellEditor().stopCellEditing();
                }

                // Chuyển đổi dữ liệu dạng bảng sang  CSV Định dạng dữ liệu
                List<Map<String, String>> newCsvData = new ArrayList<>();

                // Lấy tên cột hiện tại
                List<String> currentHeaders = new ArrayList<>();
                for (int i = 0; i < editTableModel.getColumnCount(); i++) {
                    currentHeaders.add(editTableModel.getColumnName(i));
                }

                // Chuyển đổi từng hàng dữ liệu
                for (int i = 0; i < editTableModel.getRowCount(); i++) {
                    Map<String, String> rowData = new LinkedHashMap<>();
                    boolean hasData = false;

                    for (int j = 0; j < currentHeaders.size(); j++) {
                        Object value = editTableModel.getValueAt(i, j);
                        String strValue = value != null ? value.toString().trim() : "";
                        rowData.put(currentHeaders.get(j), strValue);
                        if (!strValue.isEmpty()) {
                            hasData = true;
                        }
                    }

                    if (hasData) {
                        newCsvData.add(rowData);
                    }
                }

                if (newCsvData.isEmpty()) {
                    JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA_ROWS),
                            I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // đổi mới CSV Thứ tự tiêu đề cột và dữ liệu
                csvData = newCsvData;
                csvHeaders = currentHeaders; // Lưu thứ tự tiêu đề cột
                csvFile = null; // Xóa tham chiếu tệp gốc để tránh những hiểu lầm tiếp theo rằng tệp vẫn cần được đọc lại
                if (CharSequenceUtil.isBlank(csvSourceName)) {
                    csvSourceName = null;
                }

                updateCsvStatus();
                notifyChangeListener();

                NotificationCenter.showSuccess(
                        I18nUtil.getMessage(MessageKeys.CSV_DATA_SAVED, newCsvData.size(), currentHeaders.size()));

                manageDialog.dispose();

            } catch (Exception ex) {
                log.error("cứu CSV Dữ liệu không thành công", ex);
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SAVE_FAILED, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        styleFooterButton(cancelBtn);
        cancelBtn.addActionListener(e -> manageDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        manageDialog.add(bottomPanel, BorderLayout.SOUTH);
        manageDialog.setVisible(true);
    }

    /**
     * Hiển thị hộp thoại chỉnh sửa hàng loạt
     * Được hỗ trợ bởi CSV Định dạng dán và chỉnh sửa dữ liệu hàng loạt
     */
    private void showBulkEditDialog(Window parentWindow, DefaultTableModel tableModel, JTable csvTable, List<String> headers, Runnable applyEmptyCellRenderer) {
        // 1. Chuyển đổi dữ liệu bảng hiện tại sang định dạng văn bản (cách nhau bằng dấu phẩy)
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean hasData = false;
            StringBuilder rowText = new StringBuilder();
            for (int j = 0; j < headers.size(); j++) {
                Object value = tableModel.getValueAt(i, j);
                String strValue = value != null ? value.toString() : "";
                if (!strValue.trim().isEmpty()) {
                    hasData = true;
                }
                if (j > 0) {
                    rowText.append(",");
                }
                // Nếu giá trị chứa dấu phẩy, dòng mới hoặc dấu ngoặc kép, hãy bọc giá trị đó trong dấu ngoặc kép và thoát khỏi dấu ngoặc kép
                if (strValue.contains(",") || strValue.contains("\n") || strValue.contains("\"")) {
                    rowText.append("\"").append(strValue.replace("\"", "\"\"")).append("\"");
                } else {
                    rowText.append(strValue);
                }
            }
            if (hasData) {
                text.append(rowText).append("\n");
            }
        }

        // 2. Tạo vùng soạn thảo văn bản
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        ToolWindowSurfaceStyle.applyTextComponentInput(textArea);
        textArea.setCaretColor(ModernColors.getPrimary());

        // Đặt con trỏ ở cuối văn bản
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        ToolWindowSurfaceStyle.applyDialogInputBorder(scrollPane, false);
        scrollPane.setPreferredSize(new Dimension(700, 450));

        // 3. Tạo nhãn nhắc nhở - Sử dụng quốc tế hóa, sắp xếp theo chiều dọc
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // Văn bản nhắc nhở chính
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // Hỗ trợ mô tả định dạng
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6));
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 4. Lắp ráp bảng nội dung
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // 5. Tạo một bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        // 6. Tạo một hộp thoại
        JDialog dialog = new JDialog(parentWindow, I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT), Dialog.ModalityType.APPLICATION_MODAL);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(750, 550);
        dialog.setMinimumSize(new Dimension(600, 400));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(parentWindow); // Liên quan đến cửa sổ cha mẹ (CSVHộp thoại Quản lý dữ liệu) được căn giữa

        // 7. Xử lý sự kiện nút
        okButton.addActionListener(e -> {
            parseBulkCsvText(textArea.getText(), tableModel, headers, csvTable, applyEmptyCellRenderer);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. Hỗ trợ ESC phím để đóng hộp thoại
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. Đặt nút mặc định
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. Hiển thị hộp thoại
        dialog.setVisible(true);
    }

    /**
     * Phân tích cú pháp chỉnh sửa hàng loạt CSV Nội dung văn bản
     * Hỗ trợ tiêu chuẩn CSV định dạng (cách nhau bằng dấu phẩy)
     * Các dòng trống sẽ bị bỏ qua
     */
    private void parseBulkCsvText(String text, DefaultTableModel tableModel, List<String> headers, JTable csvTable, Runnable applyEmptyCellRenderer) {
        if (text == null || text.trim().isEmpty()) {
            // Nếu văn bản trống, hãy xóa tất cả các hàng dữ liệu
            tableModel.setRowCount(0);
            return;
        }

        // Dừng chỉnh sửa ô trước
        if (csvTable.isEditing()) {
            csvTable.getCellEditor().stopCellEditing();
        }

        // Xóa dữ liệu hiện có
        tableModel.setRowCount(0);

        for (List<String> values : CsvDataUtil.parseCsvText(buildBulkEditCsvText(headers, text)).rows()) {
            if (values.isEmpty()) {
                continue;
            }

            // Tạo hàng mới
            Object[] rowData = new Object[headers.size()];
            for (int i = 0; i < headers.size() && i < values.size(); i++) {
                rowData[i] = values.get(i);
            }

            // Đệm các cột còn lại bằng các chuỗi trống
            for (int i = values.size(); i < headers.size(); i++) {
                rowData[i] = "";
            }

            tableModel.addRow(rowData);
        }

        // Áp dụng lại trình kết xuất ô trống
        if (applyEmptyCellRenderer != null) {
            applyEmptyCellRenderer.run();
        }
    }

    private String buildBulkEditCsvText(List<String> headers, String text) {
        StringBuilder csvText = new StringBuilder();
        csvText.append(buildCsvHeaderLine(headers)).append('\n');
        csvText.append(text);
        return csvText.toString();
    }

    private String buildCsvHeaderLine(List<String> headers) {
        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) {
                headerLine.append(',');
            }
            headerLine.append(escapeCsvValue(headers.get(i)));
        }
        return headerLine.toString();
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void installResponsiveTableLayout(JTable table, JScrollPane scrollPane) {
        refreshResponsiveTableLayout(table, scrollPane);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> refreshResponsiveTableLayout(table, scrollPane));
            }
        });
    }

    private void refreshResponsiveTableLayout(JTable table, JScrollPane scrollPane) {
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            return;
        }

        int viewportWidth = scrollPane.getViewport().getExtentSize().width;
        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getViewport().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getPreferredSize().width;
        }

        if (viewportWidth <= 0) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(CSV_TABLE_MIN_COLUMN_WIDTH);
            }
            return;
        }

        int usableViewportWidth = Math.max(viewportWidth - 16, 0);
        int minTotalWidth = CSV_TABLE_MIN_COLUMN_WIDTH * columnCount;

        if (usableViewportWidth >= minTotalWidth) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            int calculatedWidth = usableViewportWidth / columnCount;
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(calculatedWidth);
            }
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(CSV_TABLE_MIN_COLUMN_WIDTH);
            }
        }

        table.revalidate();
        table.repaint();
    }

    private void styleToolbarButton(JButton button) {
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(0, CSV_TOOLBAR_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, CSV_TOOLBAR_BUTTON_HEIGHT));
    }

    private void styleFooterButton(JButton button) {
        Dimension preferred = button.getPreferredSize();
        int width = Math.max(112, preferred.width + 6);
        button.setPreferredSize(new Dimension(width, CSV_FOOTER_BUTTON_HEIGHT));
    }

    public static void configureCsvTableAppearance(JTable table) {
        Color tableBackground = ModernColors.getTableBackgroundColor();
        Color selectionBackground = ModernColors.getTableSelectionBackgroundColor();

        table.setRowHeight(30);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        table.setForeground(ModernColors.getTextPrimary());
        table.setBackground(tableBackground);
        table.setSelectionBackground(selectionBackground);
        table.setSelectionForeground(ModernColors.getTableSelectionForegroundColor());
        table.setGridColor(ModernColors.getTableGridColor());
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowMargin(1);
        table.setOpaque(true);
        table.setFillsViewportHeight(true);
        table.putClientProperty("csv.hoveredRow", -1);

        JTableHeader header = table.getTableHeader();
        header.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(ModernColors.getTableHeaderBackgroundColor());
        header.setForeground(ModernColors.getTextPrimary());
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));

        table.setDefaultRenderer(Object.class, new CsvTableCellRenderer());
    }

    private void configureCsvTable(JTable table) {
        configureCsvTableAppearance(table);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);

        table.setDefaultEditor(Object.class, createCsvCellEditor());
        setupCsvTableKeyboardNavigation(table);

        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Object oldValue = table.getClientProperty("csv.hoveredRow");
                int oldRow = oldValue instanceof Integer ? (Integer) oldValue : -1;
                if (row != oldRow) {
                    table.putClientProperty("csv.hoveredRow", row);
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col < 0 || !table.isCellEditable(row, col)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    if (table.editCellAt(row, col)) {
                        selectAllEditorText(table.getEditorComponent());
                    }
                });
            }

            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("csv.hoveredRow", -1);
                table.repaint();
            }
        });
    }

    private void setupCsvTableKeyboardNavigation(JTable table) {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("TAB"), "csv.nextCell");
        inputMap.put(KeyStroke.getKeyStroke("shift TAB"), "csv.prevCell");
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "csv.downCell");
        inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "csv.upCell");

        actionMap.put("csv.nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 0, 1);
            }
        });
        actionMap.put("csv.prevCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 0, -1);
            }
        });
        actionMap.put("csv.downCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 1, 0);
            }
        });
        actionMap.put("csv.upCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, -1, 0);
            }
        });
    }

    private void navigateCsvTable(JTable table, int rowDelta, int columnDelta) {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (row < 0 || column < 0) {
            row = 0;
            column = 0;
        }

        if (table.isEditing()) {
            int editingRow = table.getEditingRow();
            int editingColumn = table.getEditingColumn();
            if (!table.getCellEditor().stopCellEditing()) {
                return;
            }
            if (editingRow >= 0) {
                row = editingRow;
            }
            if (editingColumn >= 0) {
                column = editingColumn;
            }
        }

        int targetRow = row + rowDelta;
        int targetColumn = column + columnDelta;

        if (columnDelta != 0) {
            if (targetColumn >= table.getColumnCount()) {
                targetColumn = 0;
                targetRow = Math.min(row + 1, table.getRowCount() - 1);
            } else if (targetColumn < 0) {
                targetColumn = table.getColumnCount() - 1;
                targetRow = Math.max(row - 1, 0);
            }
        }

        targetRow = Math.max(0, Math.min(targetRow, table.getRowCount() - 1));
        targetColumn = Math.max(0, Math.min(targetColumn, table.getColumnCount() - 1));

        table.changeSelection(targetRow, targetColumn, false, false);
        final int nextRow = targetRow;
        final int nextColumn = targetColumn;
        SwingUtilities.invokeLater(() -> {
            if (table.editCellAt(nextRow, nextColumn)) {
                selectAllEditorText(table.getEditorComponent());
            } else {
                table.requestFocusInWindow();
            }
        });
    }

    private DefaultCellEditor createCsvCellEditor() {
        JTextField editorField = new JTextField();
        editorField.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        editorField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        editorField.setBackground(ModernColors.getInputBackgroundColor());
        editorField.setForeground(ModernColors.getTextPrimary());
        editorField.setCaretColor(ModernColors.getPrimary());
        editorField.setSelectionColor(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.getPrimary(), 0.12f));
        editorField.setSelectedTextColor(ModernColors.getTextPrimary());

        return new DefaultCellEditor(editorField) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (component instanceof JTextField textField) {
                    textField.setText(value == null ? "" : String.valueOf(value));
                    textField.setBackground(ModernColors.getInputBackgroundColor());
                    textField.setForeground(ModernColors.getTextPrimary());
                    textField.setSelectionColor(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.getPrimary(), 0.12f));
                    textField.setSelectedTextColor(ModernColors.getTextPrimary());
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.getPrimary(), 0.35f)),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                    SwingUtilities.invokeLater(textField::selectAll);
                }
                return component;
            }
        };
    }

    private void selectAllEditorText(Component editorComponent) {
        if (editorComponent instanceof JTextField textField) {
            textField.requestFocusInWindow();
            textField.selectAll();
        }
    }

    private static Color getStripeBackground(Color base) {
        Color alternate = UIManager.getColor("Table.alternateRowColor");
        if (alternate != null) {
            return alternate;
        }
        return ModernColors.getHoverBackgroundColor();
    }

    private static Color blendColor(Color base, Color blend, float alpha) {
        int r = Math.min(255, Math.max(0, Math.round(base.getRed() * (1 - alpha) + blend.getRed() * alpha)));
        int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * (1 - alpha) + blend.getGreen() * alpha)));
        int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * (1 - alpha) + blend.getBlue() * alpha)));
        return new Color(r, g, b);
    }

    private static final class CsvTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setFont(FontsUtil.getDefaultFont(Font.PLAIN));

            String text = value == null ? "" : String.valueOf(value);
            boolean isEmpty = text.trim().isEmpty();
            setText(text);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                Object hoveredValue = table.getClientProperty("csv.hoveredRow");
                int hoveredRow = hoveredValue instanceof Integer ? (Integer) hoveredValue : -1;
                Color base = row % 2 == 0 ? table.getBackground() : getStripeBackground(table.getBackground());
                if (row == hoveredRow) {
                    base = blendColor(base, table.getSelectionBackground(), 0.32f);
                }
                setBackground(isEmpty ? blendColor(base, ModernColors.getEmptyCellBackground(), 0.55f) : base);
                setForeground(ModernColors.getTextPrimary());
            }
            return this;
        }
    }

    /**
     * Chọn CSV tập tin
     */
    private boolean selectCsvFile() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "csv.import",
                I18nUtil.getMessage(MessageKeys.CSV_SELECT_FILE));
        fileChooser.setFileFilter(FileChooserUtil.extensionFilter(I18nUtil.getMessage(MessageKeys.CSV_FILE_FILTER), "csv"));

        File initialDirectory = CsvFileChooserDirectoryMemory.resolveInitialDirectory(
                csvFile,
                SettingManager.getCsvLastImportDirectory()
        );
        if (initialDirectory != null) {
            fileChooser.setCurrentDirectory(initialDirectory);
        }

        int result = fileChooser.showOpenDialog(UiSingletonFactory.getInstance(MainFrame.class));
        if (result == SystemFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Xác minh tài liệu
            String validationMsg = CsvDataUtil.validateCsvFile(selectedFile);
            if (CharSequenceUtil.isNotBlank(validationMsg)) {
                JOptionPane.showMessageDialog(this, validationMsg,
                        I18nUtil.getMessage(MessageKeys.CSV_FILE_VALIDATION_FAILED), JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {
                List<Map<String, String>> newCsvData = CsvDataUtil.readCsvData(selectedFile);
                if (newCsvData.isEmpty()) {
                    JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA),
                            I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                csvFile = selectedFile;
                csvSourceName = selectedFile.getName();
                csvData = newCsvData;
                csvHeaders = CsvDataUtil.getCsvHeaders(selectedFile); // Nhận tiêu đề cột
                File directoryToRemember = CsvFileChooserDirectoryMemory.resolveDirectoryToRemember(selectedFile);
                if (directoryToRemember != null) {
                    SettingManager.setCsvLastImportDirectory(directoryToRemember.getAbsolutePath());
                }
                notifyChangeListener();
                return true;

            } catch (Exception e) {
                log.error("trọng tải CSV Tệp không thành công", e);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.CSV_LOAD_FAILED, e.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    /**
     * Nhận xem hiện tại có mộtCSVdữ liệu
     */
    public boolean hasData() {
        return csvData != null && !csvData.isEmpty();
    }

    /**
     * LấyCSVSố lượng hàng dữ liệu
     */
    public int getRowCount() {
        return csvData != null ? csvData.size() : 0;
    }

    /**
     * Lấy dữ liệu của hàng được chỉ định
     */
    public Map<String, String> getRowData(int index) {
        if (csvData != null && index >= 0 && index < csvData.size()) {
            return csvData.get(index);
        }
        return Collections.emptyMap();
    }

    private void applyState(CsvState state, boolean notifyChange) {
        if (state == null || state.getRows().isEmpty()) {
            csvFile = null;
            csvSourceName = null;
            csvData = null;
            csvHeaders = null;
        } else {
            csvFile = null;
            csvSourceName = CharSequenceUtil.isBlank(state.getSourceName()) ? null : state.getSourceName();
            csvHeaders = copyHeaders(state.getHeaders());
            csvData = copyRows(state.getRows());
        }
        updateCsvStatus();
        if (notifyChange) {
            notifyChangeListener();
        }
    }

    private String getCurrentSourceName() {
        if (csvFile != null) {
            return csvFile.getName();
        }
        return csvSourceName;
    }

    private String getCurrentSourceDisplayName() {
        String sourceName = getCurrentSourceName();
        return CharSequenceUtil.isNotBlank(sourceName)
                ? sourceName
                : I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED);
    }

    private void notifyChangeListener() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private static List<String> copyHeaders(List<String> headers) {
        if (headers == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(headers);
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        List<Map<String, String>> copiedRows = new ArrayList<>();
        if (rows == null) {
            return copiedRows;
        }
        for (Map<String, String> row : rows) {
            copiedRows.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copiedRows;
    }
}
