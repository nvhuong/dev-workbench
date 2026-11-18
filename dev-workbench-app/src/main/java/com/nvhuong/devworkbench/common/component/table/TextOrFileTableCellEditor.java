package com.nvhuong.devworkbench.common.component.table;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import java.awt.*;
import java.util.WeakHashMap;

/**
 * Trình soạn thảo ô kết hợp văn bản hoặc tập tin
 * Phiên bản nâng cao: hỗ trợ lọc loại tệp, cấu hình động và bộ nhớ đệm của trình soạn thảo
 */
public class TextOrFileTableCellEditor extends DefaultCellEditor {
    // Sử dụng bộ nhớ đệm để tránh việc tạo đối tượng thường xuyên
    private static final WeakHashMap<Component, FileCellEditor> FILE_EDITOR_CACHE = new WeakHashMap<>();

    // Chỉ mục cột loại có thể định cấu hình, mặc định là 2（Form-Datatrong bảngTypechỉ mục cột)
    private static final int typeColumnIndex = 2;

    private final DevWorkbenchSmartValueCellEditor textEditor;
    private FileCellEditor fileEditor;
    private String currentType;

    // Cấu hình bộ lọc loại tệp
    private String fileFilterDescription;
    private String[] fileExtensions;

    /**
     * Tạo trình soạn thảo kết hợp văn bản hoặc tập tin mặc định
     */
    public TextOrFileTableCellEditor() {
        super(new JTextField());
        textEditor = new DevWorkbenchSmartValueCellEditor(); // Cho phép chỉnh sửa nhiều dòng thông minh
    }

    /**
     * Đặt bộ lọc loại tệp
     *
     * @param description Lọc mô tả
     * @param extensions  Phần mở rộng tệp, chẳng hạn như  "json", "txt"
     */
    public void setFileFilter(String description, String... extensions) {
        this.fileFilterDescription = description;
        this.fileExtensions = extensions;

        // Nếu trình chỉnh sửa tệp đã được tạo, bộ lọc sẽ được áp dụng ngay lập tức
        if (fileEditor != null) {
            fileEditor.setFileFilter(description, extensions);
        }
    }

    /**
     * Nhận trình chỉnh sửa tệp được lưu trong bộ nhớ đệm hoặc tạo một trình chỉnh sửa mới
     */
    private FileCellEditor getOrCreateFileEditor(Component parent) {
        FileCellEditor editor = FILE_EDITOR_CACHE.get(parent);
        if (editor == null) {
            editor = new FileCellEditor(parent);

            // Nếu bộ lọc tệp được định cấu hình, bộ lọc đó sẽ được áp dụng cho các trình chỉnh sửa mới được tạo
            if (fileFilterDescription != null && fileExtensions != null) {
                editor.setFileFilter(fileFilterDescription, fileExtensions);
            }

            FILE_EDITOR_CACHE.put(parent, editor);
        }
        return editor;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // Lấy loại ô từ cột loại
        Object type = table.getValueAt(row, typeColumnIndex);
        currentType = type == null ? "" : type.toString();

        // Chọn trình soạn thảo thích hợp dựa trên loại
        if (TableUIConstants.FILE_TYPE.equals(currentType)) {
            fileEditor = getOrCreateFileEditor(table);
            return fileEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else {
            fileEditor = null;
            return textEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    @Override
    public Object getCellEditorValue() {
        if (fileEditor != null && TableUIConstants.FILE_TYPE.equals(currentType)) {
            Object fileValue = fileEditor.getCellEditorValue();
            if (fileValue != null && !fileValue.toString().isEmpty()) {
                return fileValue;
            }
        }
        return textEditor.getCellEditorValue();
    }

    /**
     * Lấy tập tin hiện đang được chọn
     * Nếu loại tệp hiện tại không được chọn hoặc không có tệp nào được chọn, hãy quay lạinull
     */
    public java.io.File getSelectedFile() {
        if (fileEditor != null && TableUIConstants.FILE_TYPE.equals(currentType)) {
            return fileEditor.getSelectedFile();
        }
        return null;
    }

    @Override
    public boolean isCellEditable(java.util.EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(java.util.EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        boolean textStopped = textEditor.stopCellEditing();
        boolean fileStopped = fileEditor == null || fileEditor.stopCellEditing();
        return textStopped && fileStopped;
    }

    @Override
    public void cancelCellEditing() {
        textEditor.cancelCellEditing();
        if (fileEditor != null) fileEditor.cancelCellEditing();
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        textEditor.addCellEditorListener(l);
        if (fileEditor != null) fileEditor.addCellEditorListener(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        textEditor.removeCellEditorListener(l);
        if (fileEditor != null) fileEditor.removeCellEditorListener(l);
    }
}