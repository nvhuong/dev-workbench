package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.table.EasyVariableTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Variable;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Variables Trình bao bọc bảng điều khiển cung cấp chức năng chỉnh sửa hàng loạt
 */
public class EasyVariablesPanel extends JPanel {
    private EasyVariableTablePanel tablePanel;

    public EasyVariablesPanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        ToolWindowSurfaceStyle.applyCard(headerPanel);
        JLabel label = new JLabel("Variables");

        // Create Bulk Edit button
        EditButton bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());

        headerPanel.add(label);
        headerPanel.add(Box.createHorizontalStrut(5)); // Thêm khoảng cách
        headerPanel.add(bulkEditButton);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupLayout() {
        // Create table panel
        tablePanel = new EasyVariableTablePanel();
        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Hiển thị hộp thoại chỉnh sửa hàng loạt
     * Được hỗ trợ bởi "Key: Value" Định dạng dán hàng loạt và chỉnh sửa các biến
     */
    private void showBulkEditDialog() {
        // 1. Chuyển đổi dữ liệu bảng hiện tại sang định dạng văn bản (Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<Variable> currentVariables = tablePanel.getVariableList();
        for (Variable variable : currentVariables) {
            if (!variable.getKey().isEmpty()) {
                text.append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }

        // 2. Tạo vùng soạn thảo văn bản
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        ToolWindowSurfaceStyle.applyTextComponentInput(textArea);
        textArea.setCaretColor(ModernColors.getPrimary());

        // Đặt con trỏ ở cuối văn bản
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        ToolWindowSurfaceStyle.applyDialogInputBorder(scrollPane, false);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 3. Tạo nhãn nhắc nhở - Sử dụng quốc tế hóa, sắp xếp theo chiều dọc
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // Văn bản nhắc nhở chính
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // Hỗ trợ mô tả định dạng
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6)); // Thêm khoảng cách
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

        // 6. Tạo hộp thoại tùy chỉnh
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.BULK_EDIT_VARIABLES), Dialog.ModalityType.APPLICATION_MODAL);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Đặt thuộc tính hộp thoại
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        // 7. Xử lý sự kiện nút
        okButton.addActionListener(e -> {
            parseBulkText(textArea.getText());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. Hỗ trợ ESC phím để đóng hộp thoại
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. Đặt nút mặc định
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. Hiển thị hộp thoại
        dialog.setVisible(true);
    }

    /**
     * Phân tích nội dung văn bản chỉnh sửa hàng loạt
     * Các định dạng được hỗ trợ: .
     * - Key: Value
     * - Key:Value
     * - Key = Value
     * - Key=Value
     * Các dòng trống và nhận xét sẽ bị bỏ qua
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // Nếu văn bản trống, hãy xóa tất cả các biến
            tablePanel.setVariableList(new ArrayList<>());
            return;
        }

        List<Variable> variables = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue; // Các dòng trống và nhận xét sẽ bị bỏ qua
            }

            // Hỗ trợ ":" và "=" Hai dấu phân cách
            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            }

            if (parts != null && parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, value));
                }
            } else if (line.contains(":") || line.contains("=")) {
                // Nếu có dấu phân cách nhưng phân tích cú pháp không thành công thì có thể giá trị đó trống
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, ""));
                }
            }
        }

        // Cập nhật dữ liệu bảng
        tablePanel.setVariableList(variables);
    }

    // ========== Phương thức proxy, được sử dụng bên ngoài ==========

    public void addTableModelListener(TableModelListener listener) {
        tablePanel.addTableModelListener(listener);
    }

    public List<Variable> getVariableList() {
        return tablePanel.getVariableList();
    }

    /**
     * Từ model Đọc trực tiếp mà không dừng chỉnh sửa ô.
     * để lưu tự động / tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng.
     */
    public List<Variable> getVariableListFromModel() {
        return tablePanel.getVariableListFromModel();
    }

    public void setVariableList(List<Variable> variableList) {
        tablePanel.setVariableList(variableList);
    }

    public void stopCellEditing() {
        tablePanel.stopCellEditing();
    }

    public void clear() {
        tablePanel.clear();
    }
}
