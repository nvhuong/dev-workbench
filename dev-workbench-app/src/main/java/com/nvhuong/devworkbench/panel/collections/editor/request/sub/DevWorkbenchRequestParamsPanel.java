package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.request.model.HttpParam;


import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.EditButton;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Params Trình bao bọc bảng điều khiển cung cấp chức năng chỉnh sửa hàng loạt
 * Tương tự như DevWorkbenchRequestHttpHeadersPanel thiết kế
 */
public class DevWorkbenchRequestParamsPanel extends JPanel {
    private ParamsTablePanel tablePanel;
    private EditButton bulkEditButton;
    private JPanel headerPanel;
    private final String titleKey;
    private final String bulkEditTitleKey;
    private final boolean generatedFromUrl;

    public DevWorkbenchRequestParamsPanel() {
        this(MessageKeys.REQUEST_PARAMS_TITLE, MessageKeys.BULK_EDIT_PARAMS, false);
    }

    private DevWorkbenchRequestParamsPanel(String titleKey, String bulkEditTitleKey, boolean generatedFromUrl) {
        this.titleKey = titleKey;
        this.bulkEditTitleKey = bulkEditTitleKey;
        this.generatedFromUrl = generatedFromUrl;
        initializeComponents();
        setupLayout();
    }

    public static DevWorkbenchRequestParamsPanel queryParamsPanel() {
        return new DevWorkbenchRequestParamsPanel(MessageKeys.REQUEST_QUERY_PARAMS_TITLE, MessageKeys.BULK_EDIT_PARAMS, false);
    }

    public static DevWorkbenchRequestParamsPanel pathVariablesPanel() {
        return new DevWorkbenchRequestParamsPanel(MessageKeys.REQUEST_PATH_VARIABLES_TITLE, MessageKeys.BULK_EDIT_PATH_VARIABLES, true);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create header panel
        headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        ToolWindowSurfaceStyle.applyCard(headerPanel);
        JLabel label = new JLabel(I18nUtil.getMessage(titleKey));

        // Create Bulk Edit button
        bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());
        bulkEditButton.setVisible(!generatedFromUrl);

        headerPanel.add(label);
        headerPanel.add(Box.createHorizontalStrut(5)); // Thêm khoảng cách
        headerPanel.add(bulkEditButton);

        // Create table panel
        tablePanel = generatedFromUrl ? ParamsTablePanel.pathVariablesPanel() : new ParamsTablePanel();

        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupLayout() {
        // Params Hình thức bao gồm một lớp bên ngoài Params tab Chịu trách nhiệm cuộn theo cách thống nhất để tránh việc cuộn bảng lồng nhau khỏi việc thu giữ các sự kiện bánh xe.
        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Hiển thị hộp thoại chỉnh sửa hàng loạt
     * Được hỗ trợ bởi "Key: Value" Định dạng dán hàng loạt và chỉnh sửa các tham số yêu cầu
     */
    private void showBulkEditDialog() {
        // 1. Chuyển đổi dữ liệu bảng hiện tại sang định dạng văn bản (Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<HttpParam> currentParams = tablePanel.getParamsList();
        for (HttpParam param : currentParams) {
            if (!param.getKey().isEmpty()) {
                text.append(param.getKey()).append(": ").append(param.getValue()).append("\n");
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
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(bulkEditTitleKey), Dialog.ModalityType.APPLICATION_MODAL);
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
     * Các thông số đã tồn tại enabled trạng thái và description sẽ được giữ lại.
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // Nếu văn bản trống, tất cả các tham số sẽ bị xóa
            tablePanel.setParamsList(new ArrayList<>());
            return;
        }

        Map<String, Deque<HttpParam>> existingParamsByKey = new LinkedHashMap<>();
        for (HttpParam existing : tablePanel.getParamsList()) {
            String key = existing.getKey().trim();
            if (!key.isEmpty()) {
                existingParamsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>()).add(existing);
            }
        }

        List<HttpParam> params = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

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
                    HttpParam existing = pollExistingParam(existingParamsByKey, key);
                    boolean enabled = existing == null || existing.isEnabled();
                    String description = existing == null ? "" : existing.getDescription();
                    params.add(new HttpParam(enabled, key, value, description));
                }
            } else if (line.contains(":") || line.contains("=")) {
                // Nếu có dấu phân cách nhưng phân tích cú pháp không thành công thì có thể giá trị đó trống
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    HttpParam existing = pollExistingParam(existingParamsByKey, key);
                    boolean enabled = existing == null || existing.isEnabled();
                    String description = existing == null ? "" : existing.getDescription();
                    params.add(new HttpParam(enabled, key, "", description));
                }
            }
        }

        // Cập nhật dữ liệu bảng
        tablePanel.setParamsList(params);
    }

    private HttpParam pollExistingParam(Map<String, Deque<HttpParam>> existingParamsByKey, String key) {
        Deque<HttpParam> params = existingParamsByKey.get(key);
        return params == null ? null : params.pollFirst();
    }

    // ========== Phương thức proxy, được sử dụng bên ngoài ==========

    public void addTableModelListener(TableModelListener listener) {
        tablePanel.addTableModelListener(listener);
    }

    public List<HttpParam> getParamsList() {
        return tablePanel.getParamsList();
    }

    /**
     * Từ tableModel Đọc trực tiếp mà không dừng chỉnh sửa ô.
     * Được sử dụng cho tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng (ví dụ: Tab điều hướng).
     */
    public List<HttpParam> getParamsListFromModel() {
        return tablePanel.getParamsListFromModel();
    }

    public void setParamsList(List<HttpParam> paramsList) {
        tablePanel.setParamsList(paramsList);
    }

    public void stopCellEditing() {
        tablePanel.stopCellEditing();
    }

    public void clear() {
        tablePanel.clear();
    }

    public void setEditable(boolean editable) {
        tablePanel.setEditable(editable);
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable && !generatedFromUrl);
            bulkEditButton.setEnabled(editable && !generatedFromUrl);
        }
    }

    int getPreferredSectionHeight() {
        Insets insets = getInsets();
        int headerHeight = headerPanel == null ? 0 : headerPanel.getPreferredSize().height;
        return insets.top + headerHeight + tablePanel.getPreferredTableHeight() + insets.bottom;
    }
}
