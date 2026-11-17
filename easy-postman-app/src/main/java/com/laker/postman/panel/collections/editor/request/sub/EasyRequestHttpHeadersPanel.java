package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.request.defaults.GeneratedRequestHeaderPolicy;
import com.laker.postman.util.*;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Tính bắt chước caoPostmancủaHeadersbảng điều khiển
 * 1. Bảng hai cột, cột đầu tiênKey，Cột thứ hai làValue
 * 2. Tiêu đề yêu cầu mặc định User-Agent: EasyPostman/Số phiên bản Accept: *, Accept-Encoding: gzip, deflate, br, Connection: keep-alive
 * 3. Có ở góc trên bên tráiHeadersThẻ vàeyenút biểu tượng và(4)Nhãn, bấm vào để chuyển đổi hiển thị/trốn Tiêu đề yêu cầu mặc định
 * 4. Ở giữa là cái bàn
 */
public class EasyRequestHttpHeadersPanel extends JPanel {
    private EasyRequestHeadersTablePanel tablePanel;
    private final GeneratedRequestHeaderPolicy generatedHeaderPolicy;

    // UI components
    private final Icon eyeOpenIcon;
    private final Icon eyeCloseIcon;
    private JButton eyeButton;
    private JLabel countLabel;
    private EditButton bulkEditButton;

    // Table filtering
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private transient DefaultHeaderRowFilter defaultHeaderFilter;
    private boolean showDefaultHeaders = false;

    public EasyRequestHttpHeadersPanel(GeneratedRequestHeaderPolicy generatedHeaderPolicy) {
        this.generatedHeaderPolicy = Objects.requireNonNull(generatedHeaderPolicy, "generatedHeaderPolicy");
        // Khởi tạo biểu tượng (thêm bộ lọc màu cho phù hợp với chủ đề)
        eyeOpenIcon = IconUtil.createThemed("icons/eye-open.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);
        eyeCloseIcon = IconUtil.createThemed("icons/eye-close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);

        initializeComponents();
        setupLayout();
        initializeTableWithDefaults();
        setupFiltering();
    }


    /**
     * Tạo một nút mắt (được sử dụng để chuyển đổi hiển thị các tiêu đề yêu cầu mặc định/trốn)
     *
     * @return Nút mắt được cấu hình
     */
    private JButton createEyeButton() {
        JButton button = new JButton(eyeOpenIcon);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> toggleDefaultHeadersVisibility());
        return button;
    }

    /**
     * Tạo nhãn đếm (hiển thị số lượng tiêu đề yêu cầu mặc định ẩn)
     *
     * @return Thẻ đếm được cấu hình
     */
    private JLabel createCountLabel() {
        JLabel label = new JLabel();
        // Đặt văn bản ban đầu (số hiển thị ở trạng thái ẩn)
        int count = generatedHeaderPolicy.generatedHeaderCount();
        String countText = "(" + count + ")";
        String countHtml = createSuccessCountHtml(countText);
        label.setText("<html>" + countHtml + "</html>");

        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleDefaultHeadersVisibility();
            }
        });
        return label;
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        ToolWindowSurfaceStyle.applyCard(headerPanel);
        JLabel label = new JLabel("Headers");

        // Create eye button and count label with local factory methods
        eyeButton = createEyeButton();
        countLabel = createCountLabel();

        // Create Bulk Edit button
        bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());

        headerPanel.add(label);
        headerPanel.add(eyeButton);
        headerPanel.add(countLabel);
        headerPanel.add(Box.createHorizontalStrut(5)); // Thêm khoảng cách
        headerPanel.add(bulkEditButton);

        // Create table panel and set parent reference
        tablePanel = new EasyRequestHeadersTablePanel();
        tablePanel.setParentPanel(this);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupLayout() {
        // tablePanel Đã bao gồm nội bộ JScrollPane，Chỉ cần thêm nó trực tiếp
        add(tablePanel, BorderLayout.CENTER);
    }

    private void initializeTableWithDefaults() {
        // Thêm tiêu đề yêu cầu mặc định vào bảng
        for (HttpHeader header : generatedHeaderPolicy.generatedHeaders()) {
            tablePanel.addRow(header.getKey(), header.getValue());
        }
    }

    private void setupFiltering() {
        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Initialize row sorter and filter
        rowSorter = new TableRowSorter<>(model);
        defaultHeaderFilter = new DefaultHeaderRowFilter();

        // Disable sorting for all columns
        for (int i = 0; i < model.getColumnCount(); i++) {
            rowSorter.setSortable(i, false);
        }

        table.setRowSorter(rowSorter);

        // Apply initial filter (hide default headers by default)
        applyCurrentFilter();
    }

    private void updateCountLabel() {
        if (!showDefaultHeaders) {
            int hiddenCount = generatedHeaderPolicy.generatedHeaderCount();
            String countText = "(" + hiddenCount + ")";
            String countHtml = createSuccessCountHtml(countText);
            countLabel.setText("<html>" + countHtml + "</html>");
            countLabel.setVisible(true);
        } else {
            countLabel.setText("");
            countLabel.setVisible(false);
        }
    }

    private void toggleDefaultHeadersVisibility() {
        showDefaultHeaders = !showDefaultHeaders;

        // Update UI components
        eyeButton.setIcon(showDefaultHeaders ? eyeCloseIcon : eyeOpenIcon);
        updateCountLabel();

        // Apply filter
        applyCurrentFilter();
    }

    private String createSuccessCountHtml(String countText) {
        return "<span style='color:" + ModernColors.toHtmlColor(ModernColors.getSuccess())
                + ";font-weight:bold;'>" + countText + "</span>";
    }

    private void applyCurrentFilter() {
        if (rowSorter != null && defaultHeaderFilter != null) {
            defaultHeaderFilter.setShowDefaultHeaders(showDefaultHeaders);
            rowSorter.setRowFilter(defaultHeaderFilter);
        }
    }

    /**
     * Custom row filter for managing default headers visibility
     */
    private class DefaultHeaderRowFilter extends RowFilter<DefaultTableModel, Integer> {
        private boolean showDefaultHeaders = false;

        public void setShowDefaultHeaders(boolean showDefaultHeaders) {
            this.showDefaultHeaders = showDefaultHeaders;
        }

        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            try {
                Object keyObj = entry.getValue(1);
                if (keyObj == null) {
                    return true; // Show empty rows
                }

                String key = keyObj.toString().trim();
                if (key.isEmpty()) {
                    return true; // Show empty key rows
                }

                boolean isDefaultHeader = generatedHeaderPolicy.isGeneratedHeaderKey(key);
                return showDefaultHeaders || !isDefaultHeader;
            } catch (Exception e) {
                // In case of any errors, show the row
                return true;
            }
        }
    }

    // Public API methods

    public void addTableModelListener(TableModelListener l) {
        if (tablePanel != null) {
            tablePanel.addTableModelListener(l);
        }
    }

    /** Gửi ô hiện đang được chỉnh sửa (được sử dụng để đảm bảo dữ liệu đầu vào mới nhất được gửi trước khi lưu theo cách thủ công). */
    public void stopCellEditing() {
        if (tablePanel != null) {
            tablePanel.stopCellEditing();
        }
    }

    /**
     * Get all headers as a list (including enabled state) for persistence
     */
    public List<HttpHeader> getHeadersList() {
        List<HttpHeader> headersList = new ArrayList<>();

        if (tablePanel == null) {
            return headersList;
        }

        // Get all rows from the model (not view) to include both visible and hidden headers
        List<Map<String, Object>> allRows = tablePanel.getRows();

        for (Map<String, Object> row : allRows) {
            Object enabledObj = row.get("Enabled");
            Object keyObj = row.get("Key");
            Object valueObj = row.get("Value");
            Object descriptionObj = row.get("Description");

            boolean enabled = !(enabledObj instanceof Boolean b) || b;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();
            String description = descriptionObj == null ? "" : descriptionObj.toString().trim();

            // Only add non-empty headers
            if (!key.isEmpty()) {
                headersList.add(new HttpHeader(enabled, key, value, description));
            }
        }

        return headersList;
    }

    /**
     * Từ tableModel Đọc trực tiếp headers，Không ngừng chỉnh sửa ô hiện tại.
     * để lưu tự động / tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng (ví dụ: Tab điều hướng).
     */
    public List<HttpHeader> getHeadersListFromModel() {
        List<HttpHeader> headersList = new ArrayList<>();
        if (tablePanel == null) return headersList;

        List<Map<String, Object>> allRows = tablePanel.getRowsFromModel();
        for (Map<String, Object> row : allRows) {
            Object enabledObj = row.get("Enabled");
            Object keyObj    = row.get("Key");
            Object valueObj  = row.get("Value");
            Object descriptionObj = row.get("Description");

            boolean enabled = !(enabledObj instanceof Boolean b) || b;
            String key   = keyObj   == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();
            String description = descriptionObj == null ? "" : descriptionObj.toString().trim();

            if (!key.isEmpty()) {
                headersList.add(new HttpHeader(enabled, key, value, description));
            }
        }
        return headersList;
    }

    /**
     * Set headers from list (including enabled state) for loading from persistence
     */
    public void setHeadersList(List<HttpHeader> headersList) {
        if (headersList == null || headersList.isEmpty()) {
            // Xóa và khôi phục tiêu đề yêu cầu mặc định
            tablePanel.clear();
            initializeTableWithDefaults();
            applyCurrentFilter();
            return;
        }

        // Build sorted list with default headers first
        List<HttpHeader> sortedList = buildSortedHeadersList(headersList);

        // Set rows in table
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HttpHeader header : sortedList) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Enabled", header.isEnabled());
            row.put("Key", header.getKey());
            row.put("Value", header.getValue());
            row.put("Description", header.getDescription());
            rows.add(row);
        }

        tablePanel.setRows(rows);

        // Reapply filter after setting new data
        applyCurrentFilter();
    }

    /**
     * Build sorted headers list with default headers first
     */
    private List<HttpHeader> buildSortedHeadersList(List<HttpHeader> inputList) {
        return generatedHeaderPolicy.applyDefaults(inputList);
    }

    /**
     * Check if header is a default header (case-insensitive)
     */
    private boolean isDefaultHeader(String key) {
        return generatedHeaderPolicy.isGeneratedHeaderKey(key);
    }

    /**
     * Show default headers if they are currently hidden
     */
    public void ensureDefaultHeadersVisible() {
        if (!showDefaultHeaders) {
            toggleDefaultHeadersVisibility();
        }
    }

    /**
     * Tập trung vào những gì được chỉ định header dòng và bắt đầu chỉnh sửa Value cột
     * Được sử dụng để tùy chỉnh từ header Hàng nhảy về mặc định header Được rồi
     *
     * @param key header của key Tên (không phân biệt chữ hoa chữ thường)
     */
    public void focusOnHeader(String key) {
        if (tablePanel == null || key == null || key.trim().isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Tìm các hàng phù hợp trong mô hình
        int modelRow = findHeaderRowInModel(model, key);
        if (modelRow < 0) {
            return;
        }

        // Chuyển đổi để xem chỉ mục hàng (có tính đến bộ lọc)
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow < 0) {
            return;
        }

        // Chọn và cuộn đến hàng
        table.setRowSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 2, true));

        // Chắc chắn là trì hoãn việc bắt đầu chỉnh sửaUIĐã cập nhật
        startEditingValueCell(table, viewRow);
    }

    /**
     * Tìm bài tập trong mô hình dạng bảng key ĐƯỢC RỒI
     */
    private int findHeaderRowInModel(DefaultTableModel model, String key) {
        for (int row = 0; row < model.getRowCount(); row++) {
            Object keyObj = model.getValueAt(row, 1); // Column 1 is Key
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Bắt đầu Value Chỉnh sửa tế bào
     */
    private void startEditingValueCell(JTable table, int viewRow) {
        SwingUtilities.invokeLater(() -> {
            table.editCellAt(viewRow, 2); // Column 2 is Value
            Component editor = table.getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
            }
        });
    }

    /**
     * Remove a header by key name
     */
    public void removeHeader(String key) {
        if (tablePanel == null || key == null) {
            return;
        }

        key = key.trim();
        if (key.isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Find and remove rows with matching key (case-insensitive)
        // Note: Column index 1 is now the Key column (0 is Enabled checkbox)
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Object keyObj = model.getValueAt(i, 1);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                model.removeRow(i);
            }
        }

        // Reapply filter after removing data
        applyCurrentFilter();
    }

    /**
     * Set or update a header value. If the header exists, update its value; otherwise, add a new header
     */
    public void setOrUpdateHeader(String key, String value) {
        if (tablePanel == null || key == null) {
            return;
        }

        key = key.trim();
        value = value == null ? "" : value.trim();

        if (key.isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // First, try to find existing header (case-insensitive)
        // Note: Column index 1 is now the Key column (0 is Enabled checkbox)
        boolean found = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object keyObj = model.getValueAt(i, 1);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                // Update existing header value
                model.setValueAt(value, i, 2); // Column 2 is Value
                model.setValueAt(true, i, 0); // Ensure it's enabled
                found = true;
                break;
            }
        }

        // If not found, add new header
        if (!found) {
            // Check if this is a default header that should be added in the correct position
            if (generatedHeaderPolicy.isGeneratedHeaderKey(key)) {
                addDefaultHeaderInOrder(key, value, model);
            } else {
                // Add as regular header using direct model manipulation to avoid triggering auto-append
                addNonDefaultHeader(key, value, model);
            }
        }

        // Reapply filter after updating data
        applyCurrentFilter();
    }

    public void setEditable(boolean editable) {
        if (tablePanel != null) {
            tablePanel.setEditable(editable);
        }
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable);
            bulkEditButton.setEnabled(editable);
        }
    }

    /**
     * Add a default header in the correct position among other default headers
     */
    private void addDefaultHeaderInOrder(String key, String value, DefaultTableModel model) {
        int insertPosition = findInsertPositionForDefaultHeader(key, model);
        if (insertPosition >= 0) {
            model.insertRow(insertPosition, new Object[]{true, key, value, "", ""});
        } else {
            model.addRow(new Object[]{true, key, value, "", ""});
        }
    }

    /** Trở về mặc định header Chỉ mục của hàng cần được chèn.-1 Biểu thị việc thêm vào cuối */
    private int findInsertPositionForDefaultHeader(String key, DefaultTableModel model) {
        int keyIndex = generatedHeaderOrder(key);
        if (keyIndex < 0) return -1;

        for (int row = 0; row < model.getRowCount(); row++) {
            Object rowKeyObj = model.getValueAt(row, 1);
            if (rowKeyObj == null) continue;
            int rowKeyIndex = generatedHeaderOrder(rowKeyObj.toString().trim());
            if (rowKeyIndex > keyIndex || rowKeyIndex == -1) {
                return row;
            }
        }
        return -1;
    }

    /** Trở về mặc định header Chỉ số tuần tự của , không có kết quả trả về -1 */
    private int generatedHeaderOrder(String target) {
        List<HttpHeader> generatedHeaders = generatedHeaderPolicy.generatedHeaders();
        for (int i = 0; i < generatedHeaders.size(); i++) {
            if (generatedHeaders.get(i).getKey().equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Add a non-default header intelligently to avoid creating extra blank rows
     */
    private void addNonDefaultHeader(String key, String value, DefaultTableModel model) {
        // Check if the last row is empty
        int rowCount = model.getRowCount();
        if (rowCount > 0) {
            int lastRow = rowCount - 1;
            Object lastKey = model.getValueAt(lastRow, 1); // Column 1 is Key
            Object lastValue = model.getValueAt(lastRow, 2); // Column 2 is Value
            Object lastDescription = model.getValueAt(lastRow, 3); // Column 3 is Description

            boolean lastRowIsEmpty = (lastKey == null || lastKey.toString().trim().isEmpty()) &&
                    (lastValue == null || lastValue.toString().trim().isEmpty()) &&
                    (lastDescription == null || lastDescription.toString().trim().isEmpty());

            if (lastRowIsEmpty) {
                // Replace the empty row with the new header
                model.setValueAt(true, lastRow, 0); // Enabled
                model.setValueAt(key, lastRow, 1); // Key
                model.setValueAt(value, lastRow, 2); // Value
                return;
            }
        }

        // If no empty row at the end, add new row directly
        model.addRow(new Object[]{true, key, value, "", ""});
    }

    /**
     * Hiển thị hộp thoại chỉnh sửa hàng loạt
     * Được hỗ trợ bởi "Key: Value" Định dạng dán hàng loạt và chỉnh sửa tiêu đề yêu cầu
     */
    private void showBulkEditDialog() {
        // 1. Chuyển đổi dữ liệu bảng hiện tại sang định dạng văn bản (Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<HttpHeader> currentHeaders = getHeadersList();
        for (HttpHeader header : currentHeaders) {
            if (!header.getKey().isEmpty()) {
                text.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
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
        hintPanel.add(Box.createVerticalStrut(6)); // Thêm vào3Khoảng cách pixel
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
        Window window = SwingUtilities.getWindowAncestor(tablePanel);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.BULK_EDIT_HEADERS), Dialog.ModalityType.APPLICATION_MODAL);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Đặt thuộc tính hộp thoại
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(tablePanel);

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
     * Các dòng trống và  # // Dòng bình luận được bỏ qua.
     * đã tồn tại rồi header của enabled Trạng thái sẽ được giữ nguyên.
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // Nếu văn bản trống, hãy xóa tất cả các tiêu đề yêu cầu không mặc định
            clearNonDefaultHeaders();
            return;
        }

        // Xây dựng hiện tại header của enabled Ảnh chụp trạng thái để sử dụng lại sau này
        Map<String, Boolean> enabledSnapshot = new java.util.LinkedHashMap<>();
        Map<String, String> descriptionSnapshot = new java.util.LinkedHashMap<>();
        for (HttpHeader existing : getHeadersList()) {
            String normalizedKey = existing.getKey().trim().toLowerCase(java.util.Locale.ROOT);
            enabledSnapshot.put(normalizedKey, existing.isEnabled());
            descriptionSnapshot.put(normalizedKey, existing.getDescription());
        }

        List<HttpHeader> headers = new ArrayList<>();
        // Hỗ trợ \r\n（Windows Dán) và \n
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue; // Các dòng trống và nhận xét sẽ bị bỏ qua
            }

            String key;
            String value;

            // Cái đầu tiên được ưu tiên ':' chia (HTTP Header định dạng chuẩn), tiếp theo là định dạng đầu tiên '=' sự phân đoạn
            int colonIdx = line.indexOf(':');
            int equalsIdx = line.indexOf('=');

            if (colonIdx > 0) {
                key   = line.substring(0, colonIdx).trim();
                value = line.substring(colonIdx + 1).trim();
            } else if (equalsIdx > 0) {
                key   = line.substring(0, equalsIdx).trim();
                value = line.substring(equalsIdx + 1).trim();
            } else {
                // Chỉ key，Không có dấu phân cách → Như key="" xử lý
                key   = line;
                value = "";
            }

            if (!key.isEmpty()) {
                // Giữ bản gốc enabled trạng thái
                String normalizedKey = key.toLowerCase(java.util.Locale.ROOT);
                boolean enabled = enabledSnapshot.getOrDefault(normalizedKey, true);
                String description = descriptionSnapshot.getOrDefault(normalizedKey, "");
                headers.add(new HttpHeader(enabled, key, value, description));
            }
        }

        // Cập nhật dữ liệu bảng
        setHeadersList(headers);
    }

    /**
     * Xóa tất cả các tiêu đề yêu cầu không mặc định
     */
    private void clearNonDefaultHeaders() {
        setHeadersList(generatedHeaderPolicy.generatedHeaders());
    }
}
