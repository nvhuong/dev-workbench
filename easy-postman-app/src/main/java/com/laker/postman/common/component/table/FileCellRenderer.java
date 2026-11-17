package com.laker.postman.common.component.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * Trình kết xuất ô loại tệp
 * Phiên bản nâng cao: Hỗ trợ nhắc nhở trạng thái tệp và cắt ngắn đường dẫn
 */
public class FileCellRenderer implements TableCellRenderer {

    // Độ dài tối đa của đường dẫn tệp được hiển thị. Nếu vượt quá chiều dài, nó sẽ bị cắt bớt
    private static final int MAX_PATH_DISPLAY_LENGTH = 35;
    private final JLabel label = new JLabel();

    public FileCellRenderer() {
        label.setOpaque(true);
        label.setBorder(TableUIConstants.createLabelBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String val = value == null ? "" : value.toString();
        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
        boolean isEmpty = val.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(val);
        boolean isHovered = row == hoverRow;

        label.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, isEmpty, table, row));
        label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

        if (isEmpty) {
            label.setText(TableUIConstants.SELECT_FILE_TEXT);
            if (!isSelected) {
                label.setForeground(TableUIConstants.getFileEmptyTextColor());
            }
            label.setToolTipText(null);
            return label;
        }

        label.setText(formatFilePath(val));
        label.setToolTipText(fileTooltip(val));
        return label;
    }

    /**
     * Định dạng đường dẫn file và cắt bớt phần giữa nếu quá dài
     */
    private String formatFilePath(String path) {
        if (path == null || path.length() <= MAX_PATH_DISPLAY_LENGTH) {
            return path;
        }

        File file = new File(path);
        String fileName = file.getName();
        String parentPath = file.getParent();
        if (parentPath == null || parentPath.isBlank()) {
            return path;
        }

        // Nếu tên tệp đã rất dài thì chỉ một phần tên tệp được hiển thị
        if (fileName.length() > MAX_PATH_DISPLAY_LENGTH) {
            return fileName.substring(0, 10) + "..." +
                    fileName.substring(fileName.length() - 20);
        }

        // Nếu không, tên tệp đầy đủ sẽ được giữ lại nhưng đường dẫn gốc bị cắt bớt
        int remainingLength = MAX_PATH_DISPLAY_LENGTH - fileName.length() - 5; // 5vì"..."và độ dài dấu phân cách
        if (remainingLength < 10) remainingLength = 10;

        return parentPath.substring(0, Math.min(remainingLength, parentPath.length()))
                + "..." + File.separator + fileName;
    }

    private String fileTooltip(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            return path + " (file not found)";
        }
        if (!file.canRead()) {
            return path + " (file is not readable)";
        }
        return path;
    }
}
