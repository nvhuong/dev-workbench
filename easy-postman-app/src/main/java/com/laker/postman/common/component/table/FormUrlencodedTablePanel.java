package com.laker.postman.common.component.table;

import com.laker.postman.request.model.HttpFormUrlencoded;


import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Form-Urlencoded Thành phần bảng điều khiển
 * Được thiết kế đặc biệt để xử lý application/x-www-form-urlencoded Loại dữ liệu nội dung yêu cầu
 * Hỗ trợ Enable、Key、Value và Delete cấu trúc cột
 */
@Slf4j
public class FormUrlencodedTablePanel extends AbstractTablePanel<HttpFormUrlencoded> {

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DESCRIPTION = 3;
    private static final int COL_DELETE = 4;

    /**
     * Trình xây dựng, tạo cái mặc định Form-Urlencoded Bảng điều khiển
     */
    public FormUrlencodedTablePanel() {
        this(true, true);
    }

    /**
     * Trình xây dựng, hỗ trợ cấu hình tùy chỉnh
     *
     * @param popupMenuEnabled     Có bật menu chuột phải hay không
     * @param autoAppendRowEnabled Có bật tự động điền dòng trống hay không
     */
    public FormUrlencodedTablePanel(boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
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
        if (popupMenuEnabled) {
            setupTableListeners();
        } else {
            addDeleteButtonListener();
        }
        if (autoAppendRowEnabled) {
            addAutoAppendRowFeature();
        }

        // Add initial empty row (same as ParamsTablePanel)
        addRow();
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
        return column == COL_KEY || column == COL_VALUE || column == COL_DESCRIPTION;
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
    protected void initializeTableUI() {
        // Gọi hàm chung của lớp chaUIcấu hình
        super.initializeTableUI();

        // Đặt chiều rộng cột
        setEnabledColumnWidth(40);
        setDeleteColumnWidth();

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    private void setupCellRenderersAndEditors() {
        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasySmartValueCellEditor());
        setColumnEditor(COL_DESCRIPTION, new EasySmartValueCellEditor());
        setColumnRenderer(COL_KEY, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_DESCRIPTION, new EasyTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    /**
     * Từ tableModel Đọc trực tiếp mà không dừng chỉnh sửa ô.
     * Được sử dụng cho tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng (ví dụ: Tab điều hướng).
     */
    public List<HttpFormUrlencoded> getFormDataListFromModel() {
        List<HttpFormUrlencoded> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);
            String description = getStringValue(i, COL_DESCRIPTION);
            if (!key.isEmpty()) {
                dataList.add(new HttpFormUrlencoded(enabled, key, value, description));
            }
        }
        return dataList;
    }

    /**
     * Get form-urlencoded data list with enabled state (new format)
     */
    public List<HttpFormUrlencoded> getFormDataList() {
        // Stop cell editing to ensure any in-progress edits are committed to the table model
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<HttpFormUrlencoded> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);
            String description = getStringValue(i, COL_DESCRIPTION);

            // Only add non-empty params
            if (!key.isEmpty()) {
                dataList.add(new HttpFormUrlencoded(enabled, key, value, description));
            }
        }
        return dataList;
    }

    /**
     * Set form-urlencoded data list with enabled state (new format)
     */
    public void setFormDataList(List<HttpFormUrlencoded> dataList) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (dataList != null) {
                for (HttpFormUrlencoded param : dataList) {
                    tableModel.addRow(new Object[]{param.isEnabled(), param.getKey(), param.getValue(), param.getDescription(), ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(createEmptyRow());
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
