package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.common.component.table.AbstractTablePanel;
import com.nvhuong.devworkbench.common.component.table.DevWorkbenchSmartValueCellEditor;
import com.nvhuong.devworkbench.common.component.table.HttpHeaderKeyDevWorkbenchTextFieldCellEditor;
import com.nvhuong.devworkbench.common.component.table.DevWorkbenchTextFieldCellRenderer;
import com.nvhuong.devworkbench.common.component.table.HttpHeaderValueDevWorkbenchTextFieldCellEditor;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.request.defaults.HttpRequestDefaults;
import com.nvhuong.devworkbench.util.HttpHeaderConstants;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.*;

@Slf4j
public class DevWorkbenchRequestHeadersTablePanel extends AbstractTablePanel<Map<String, Object>> {

    // Default header keys for consistency
    private static final String USER_AGENT = HttpRequestDefaults.USER_AGENT;
    private static final String ACCEPT = HttpRequestDefaults.ACCEPT;
    private static final String ACCEPT_ENCODING = HttpRequestDefaults.ACCEPT_ENCODING;
    private static final String CONNECTION = HttpRequestDefaults.CONNECTION;
    private static final Set<String> DEFAULT_HEADER_KEYS = new HashSet<>();

    static {
        DEFAULT_HEADER_KEYS.add(USER_AGENT);
        DEFAULT_HEADER_KEYS.add(ACCEPT);
        DEFAULT_HEADER_KEYS.add(ACCEPT_ENCODING);
        DEFAULT_HEADER_KEYS.add(CONNECTION);
    }

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DESCRIPTION = 3;
    private static final int COL_DELETE = 4;

    // Parent panel reference for handling default headers
    private DevWorkbenchRequestHttpHeadersPanel parentPanel;

    public DevWorkbenchRequestHeadersTablePanel() {
        super(new String[]{
                "",
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                ""
        });
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();
        setupDefaultHeaderDetection();
    }

    /**
     * Set parent panel reference
     */
    public void setParentPanel(DevWorkbenchRequestHttpHeadersPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
     * Đặt mặc định header phát hiện
     * Khi người dùng nhập vào key với mặc định header Khi giống nhau: .
     * 1. Người ta phát hiện ra rằng một tên mặc định có cùng tên đã tồn tại header
     * 2. Tự động mở rộng theo mặc định headers（nếu nó bị ẩn)
     * 3. Xóa đầu vào hiện tại
     * 4. Tập trung vào mặc định hiện có header Được rồi
     */
    private void setupDefaultHeaderDetection() {
        tableModel.addTableModelListener(e -> {
            // Chỉ xử lý Key Sự kiện cập nhật cột
            if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) {
                return;
            }

            int column = e.getColumn();
            if (column != COL_KEY) {
                return;
            }

            int row = e.getFirstRow();
            if (row < 0 || row >= tableModel.getRowCount()) {
                return;
            }

            // Trì hoãn xử lý để tránh kích hoạt trong quá trình chỉnh sửa
            SwingUtilities.invokeLater(() -> handleDefaultHeaderInput(row));
        });
    }

    /**
     * Xử lý mặc định header Nhập
     * Khi người dùng đang tùy chỉnh header Mặc định đầu vào dòng header tên, nó sẽ tự động nhảy về mặc định tương ứng header Được rồi
     */
    private void handleDefaultHeaderInput(int row) {
        if (parentPanel == null) {
            return;
        }

        try {
            Object keyObj = tableModel.getValueAt(row, COL_KEY);
            if (keyObj == null) {
                return;
            }

            String inputKey = keyObj.toString().trim();
            if (inputKey.isEmpty()) {
                return;
            }

            // Kiểm tra xem nó có phải là mặc định không header key
            boolean isDefaultKey = DEFAULT_HEADER_KEYS.stream()
                    .anyMatch(defaultKey -> defaultKey.equalsIgnoreCase(inputKey));

            if (!isDefaultKey) {
                return;
            }

            // Tìm xem giá trị mặc định có cùng tên đã tồn tại chưa header（loại trừ hàng hiện tại)
            int existingRow = findExistingHeaderRow(inputKey, row);
            if (existingRow < 0) {
                // Không tìm thấy cái nào hiện có, cho biết người dùng có thể muốn thay đổi mặc định header giá trị
                return;
            }

            // Một mặc định hiện có đã được tìm thấy header
            // 1. Xóa dòng hiện tại
            tableModel.setValueAt("", row, COL_KEY);
            tableModel.setValueAt("", row, COL_VALUE);
            tableModel.setValueAt("", row, COL_DESCRIPTION);

            // 2. Hãy chắc chắn để mặc định headers dễ thấy
            parentPanel.ensureDefaultHeadersVisible();

            // 3. Trì hoãn lấy nét về mặc định header Được rồi
            focusOnExistingHeader(inputKey);

        } catch (Exception ex) {
            log.warn("Error handling default header input", ex);
        }
    }

    /**
     * Tập trung vào những gì đã tồn tại header（Với sự chậm trễ để chờ đợiUICập nhật hoàn tất)
     * sử dụng javax.swing.Timer thay thế Thread.sleep，Tránh chặn EDT
     */
    private void focusOnExistingHeader(String headerKey) {
        javax.swing.Timer timer = new javax.swing.Timer(120, e -> parentPanel.focusOnHeader(headerKey));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Tìm những cái hiện có header hàng (loại trừ các hàng được chỉ định)
     */
    private int findExistingHeaderRow(String key, int excludeRow) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (i == excludeRow) {
                continue;
            }

            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                return i;
            }
        }
        return -1;
    }

    // ========== Thực hiện các phương pháp trừu tượng ==========

    @Override
    protected int getEnabledColumnIndex() {
        return COL_ENABLED;
    }

    @Override
    protected int getDeleteColumnIndex() {
        return COL_DELETE;
    }

    @Override
    protected int getFirstEditableColumnIndex() {
        return COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_DESCRIPTION;
    }

    @Override
    protected int getEnterTargetColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        if (column != COL_KEY && column != COL_VALUE && column != COL_DESCRIPTION) {
            return false;
        }
        // Mặc định Header của Key Các cột không thể chỉnh sửa được (không tham gia Tab/Enter điều hướng)
        if (column == COL_KEY) {
            // row là chỉ mục hàng xem (Tab Điều hướng được truyền vào là editingRow，tức là xem hàng).
            // tableModel.getValueAt Yêu cầu chỉ mục hàng mô hình - phải được chuyển đổi.
            int modelRow = row;
            if (table.getRowSorter() != null) {
                try {
                    modelRow = table.getRowSorter().convertRowIndexToModel(row);
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
            }
            if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return false;
            Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
            return keyObj == null || !DEFAULT_HEADER_KEYS.contains(keyObj.toString().trim());
        }
        return true;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        String description = getStringValue(row, COL_DESCRIPTION);
        return !key.isEmpty() || !value.isEmpty() || !description.isEmpty();
    }

    // ========== Phương pháp khởi tạo ==========

    @Override
    protected boolean isCellEditable(int row, int column) {
        if (!editable) {
            return false;
        }

        // Checkbox column is always editable
        if (column == COL_ENABLED) {
            return true;
        }

        // Delete column is not editable (uses custom renderer)
        if (column == COL_DELETE) {
            return false;
        }

        // Check if this is a default header
        Object keyObj = tableModel.getValueAt(row, COL_KEY);
        if (keyObj != null) {
            String key = keyObj.toString().trim();
            if (DEFAULT_HEADER_KEYS.contains(key)) {
                // For default headers: Key column not editable, Value column editable
                return column == COL_VALUE;
            }
        }

        // For non-default headers: Key and Value columns editable
        return column == COL_KEY || column == COL_VALUE || column == COL_DESCRIPTION;
    }

    @Override
    protected boolean isDeletableRow(int modelRow) {
        // Mặc định header Các hàng không được phép xóa
        Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
        String keyStr = keyObj == null ? "" : keyObj.toString().trim();
        return !DEFAULT_HEADER_KEYS.contains(keyStr);
    }

    @Override
    protected JPopupMenu createContextPopupMenu(int viewRow) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        // Convert view row to model row if needed
        int modelRow = viewRow;
        if (viewRow >= 0 && table.getRowSorter() != null) {
            modelRow = table.getRowSorter().convertRowIndexToModel(viewRow);
        }

        // Check if the selected row contains a default header
        boolean isDefaultHeaderRow = false;
        if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
            Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
            if (keyObj != null) {
                String key = keyObj.toString().trim();
                isDefaultHeaderRow = DEFAULT_HEADER_KEYS.contains(key);
            }
        }

        if (isDefaultHeaderRow) {
            // For default header rows, show a disabled menu item
            JMenuItem defaultHeaderItem = new JMenuItem("Default Header (Cannot Delete)");
            defaultHeaderItem.setEnabled(false);
            menu.add(defaultHeaderItem);
        } else {
            // For normal rows, show add/remove options
            JMenuItem addItem = new JMenuItem("Add");
            addItem.addActionListener(e -> addRowAndScroll());
            menu.add(addItem);

            JMenuItem removeItem = new JMenuItem("Remove");
            removeItem.addActionListener(e -> deleteSelectedRow());
            menu.add(removeItem);
        }

        return menu;
    }

    @Override
    protected void initializeTableUI() {
        // Gọi hàm chung của lớp chaUIcấu hình
        super.initializeTableUI();

        // Đặt chiều rộng cột
        setEnabledColumnWidth(40);
        setDeleteColumnWidth();

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    /**
     * Thiết lập trình kết xuất và trình chỉnh sửa ô
     * - Key Cột: Sử dụng trình chỉnh sửa tự động hoàn thành thông minh cung cấp thông tin chung HTTP Header Gợi ý tên
     * - Value Cột: Sử dụng trình soạn thảo nhận biết ngữ cảnh, theo  Key Cung cấp các đề xuất giá trị tương ứng
     */
    private void setupCellRenderersAndEditors() {
        // Key Cột: Trình chỉnh sửa hỗ trợ tự động hoàn thành (hiển thị common HTTP Header tên)
        setColumnEditor(COL_KEY, new HttpHeaderKeyDevWorkbenchTextFieldCellEditor(HttpHeaderConstants.COMMON_HEADERS));

        // Value Cột: Smart Editor (theo  Key Đưa ra gợi ý một cách linh hoạt)
        setColumnEditor(COL_VALUE, new HttpHeaderValueDevWorkbenchTextFieldCellEditor(COL_KEY));

        // Thiết lập trình kết xuất để hỗ trợ các tính năng như đánh dấu biến
        setColumnRenderer(COL_KEY, new DevWorkbenchTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new DevWorkbenchTextFieldCellRenderer());
        setColumnEditor(COL_DESCRIPTION, new DevWorkbenchSmartValueCellEditor());
        setColumnRenderer(COL_DESCRIPTION, new DevWorkbenchTextFieldCellRenderer());

        // Xóa cột bằng trình kết xuất nút tùy chỉnh
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    /**
     * Get all rows as a list of maps (model data, not view data)
     */
    public List<Map<String, Object>> getRows() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            // Store enabled state
            row.put("Enabled", getBooleanValue(i, COL_ENABLED));
            // Store Key and Value
            row.put("Key", getStringValue(i, COL_KEY));
            row.put("Value", getStringValue(i, COL_VALUE));
            row.put("Description", getStringValue(i, COL_DESCRIPTION));
            rows.add(row);
        }

        return rows;
    }

    /**
     * Từ tableModel Đọc dữ liệu hàng trực tiếp mà không dừng chỉnh sửa ô hiện tại.
     * để lưu tự động / tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng.
     */
    public List<Map<String, Object>> getRowsFromModel() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Enabled", getBooleanValue(i, COL_ENABLED));
            row.put("Key", getStringValue(i, COL_KEY));
            row.put("Value", getStringValue(i, COL_VALUE));
            row.put("Description", getStringValue(i, COL_DESCRIPTION));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Set all rows from a list of maps
     */
    public void setRows(List<Map<String, Object>> rows) {
        suppressAutoAppendRow = true;
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Add new rows
            if (rows != null && !rows.isEmpty()) {
                for (Map<String, Object> row : rows) {
                    Object enabled = row.get("Enabled");
                    if (enabled == null) {
                        enabled = true; // Default to enabled
                    }
                    Object key = row.get("Key");
                    Object value = row.get("Value");
                    Object description = row.get("Description");

                    tableModel.addRow(new Object[]{enabled, key, value, description, ""});
                }
            }

            // Ensure there's always an empty row at the end
            ensureEmptyLastRow();
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
