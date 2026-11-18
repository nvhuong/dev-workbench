package com.nvhuong.devworkbench.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FileChooserUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Trình chỉnh sửa ô chọn tập tin
 * Phiên bản nâng cao: hỗ trợ xem trước tệp, lọc loại tệp và bộ nhớ đường dẫn gần đây
 */
public class FileCellEditor extends DefaultCellEditor {
    private static final String BUTTON_BASE_BACKGROUND_PROPERTY = "Dev Workbench.fileCellEditor.buttonBaseBackground";

    private final JPanel editorPanel;
    private final JButton browseButton;
    private final JTextField pathField;
    private String filePath;
    private final Component parentComponent;

    // Ghi nhớ các thư mục đã mở gần đây
    private File lastDirectory = null;

    // Bộ lọc loại tệp tùy chọn
    private transient SystemFileChooser.FileNameExtensionFilter fileFilter = null;

    public FileCellEditor(Component parentComponent) {
        super(new JCheckBox());
        this.parentComponent = parentComponent;

        // Tạo một bảng điều khiển và sử dụng nóBorderLayout
        editorPanel = new JPanel(new BorderLayout(5, 0));
        final Color tableBackground = ModernColors.getCardBackgroundColor();
        editorPanel.setBackground(tableBackground);

        // Tạo một trường văn bản để hiển thị đường dẫn
        pathField = new JTextField();
        pathField.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Tạo nút duyệt
        browseButton = new JButton();
        // Tạo biểu tượng và đặt bộ lọc màu phù hợp với chủ đề
        FlatSVGIcon fileIcon = new FlatSVGIcon("icons/file.svg", TableUIConstants.ICON_SIZE, TableUIConstants.ICON_SIZE);
        fileIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary()));
        browseButton.setIcon(fileIcon);
        browseButton.setText("Browse");
        browseButton.setMargin(new Insets(2, 8, 2, 8));
        browseButton.setFocusPainted(false);
        browseButton.setBackground(tableBackground);
        browseButton.putClientProperty(BUTTON_BASE_BACKGROUND_PROPERTY, tableBackground);
        browseButton.setForeground(TableUIConstants.getFileButtonTextColor());
        browseButton.setBorder(TableUIConstants.createButtonBorder());
        browseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Thêm hiệu ứng di chuột vào nút duyệt
        browseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                browseButton.setBackground(TableUIConstants.getHoverColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                browseButton.setBackground(getButtonBaseBackground());
            }
        });

        // Đặt sự kiện bấm nút
        browseButton.addActionListener(e -> openFileChooser());

        // Bạn cũng có thể chọn tệp bằng cách bấm đúp vào hộp văn bản
        pathField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openFileChooser();
                }
            }
        });

        // Thêm các thành phần vào bảng điều khiển
        editorPanel.add(pathField, BorderLayout.CENTER);
        editorPanel.add(browseButton, BorderLayout.EAST);
    }

    /**
     * Đặt bộ lọc loại tệp
     *
     * @param description Lọc mô tả
     * @param extensions  Phần mở rộng tệp, chẳng hạn như  "json", "txt"
     */
    public void setFileFilter(String description, String... extensions) {
        if (extensions != null && extensions.length > 0) {
            this.fileFilter = FileChooserUtil.extensionFilter(description, extensions);
        } else {
            this.fileFilter = null;
        }
    }

    /**
     * Mở hộp thoại chọn tập tin
     */
    private void openFileChooser() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "table.fileCellEditor.open",
                TableUIConstants.SELECT_FILE_TEXT);

        // Áp dụng bộ lọc tập tin
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }

        // Sử dụng thư mục gần đây nhất
        if (lastDirectory != null && lastDirectory.exists()) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }

        // Nếu đường dẫn tệp đã tồn tại, hãy chọn trước tệp
        if (filePath != null && !filePath.isEmpty() && !TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            File currentFile = new File(filePath);
            if (currentFile.exists()) {
                fileChooser.setSelectedFile(currentFile);
            }
        }

        // Hiển thị bộ chọn tập tin
        int result = fileChooser.showOpenDialog(parentComponent);
        if (result == SystemFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePath = selectedFile.getAbsolutePath();
            pathField.setText(filePath);
            lastDirectory = selectedFile.getParentFile();

            // Tự động gửi chỉnh sửa
            SwingUtilities.invokeLater(this::stopCellEditing);
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        filePath = value == null ? "" : value.toString();

        TableUIConstants.styleCellEditorContainer(editorPanel, table, row);
        TableUIConstants.styleContainedTextCellEditor(pathField, table, row);
        browseButton.putClientProperty(BUTTON_BASE_BACKGROUND_PROPERTY, TableUIConstants.getRowBackground(table, row));
        browseButton.setBackground(getButtonBaseBackground());

        if (filePath.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            pathField.setText("");
            pathField.setToolTipText(null);
        } else {
            pathField.setText(filePath);
            pathField.setToolTipText(filePath);
            pathField.setForeground(table.getForeground());
        }

        return editorPanel;
    }

    private Color getButtonBaseBackground() {
        Object background = browseButton.getClientProperty(BUTTON_BASE_BACKGROUND_PROPERTY);
        return background instanceof Color color ? color : ModernColors.getTableBackgroundColor();
    }

    @Override
    public Object getCellEditorValue() {
        String value = pathField.getText();
        return value.isEmpty() ? "" : value;
    }

    /**
     * Lấy đối tượng tập tin đã chọn
     */
    public File getSelectedFile() {
        if (filePath == null || filePath.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            return null;
        }
        return new File(filePath);
    }
}
