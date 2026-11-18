package com.nvhuong.devworkbench.common.component.table;

import com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField;
import com.nvhuong.devworkbench.util.HttpHeaderConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.List;

/**
 * HTTP Header Value Trình chỉnh sửa thông minh
 * kế thừa DevWorkbenchSmartValueCellEditor，Tái sử dụng khả năng soạn thảo nhiều dòng thông minh
 * Đồng thời, nó được hỗ trợ theo tương ứng Header Key Cung cấp sự hoàn thiện thông minh
 */
public class HttpHeaderValueDevWorkbenchTextFieldCellEditor extends DevWorkbenchSmartValueCellEditor {
    private final int keyColumnIndex;
    private final AutoCompleteDevWorkbenchTextField autoCompleteTextField;
    private JTable parentTable;
    private int editingRow;

    public HttpHeaderValueDevWorkbenchTextFieldCellEditor(int keyColumnIndex) {
        super(true); // Cho phép chỉnh sửa nhiều dòng tự động
        this.keyColumnIndex = keyColumnIndex;

        // Thay đổi lớp cha thành một dòng duy nhất textField Thay thế bằng  AutoCompleteDevWorkbenchTextField
        this.autoCompleteTextField = new AutoCompleteDevWorkbenchTextField(1);
        this.autoCompleteTextField.setBorder(null);

        // Tối ưu hóa: Khi tập trung, tất cả đề xuất sẽ được hiển thị nếu văn bản trống
        this.autoCompleteTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Tự động hiển thị tất cả các đề xuất chỉ khi văn bản trống
                String text = autoCompleteTextField.getText();
                if (text == null || text.trim().isEmpty()) {
                    SwingUtilities.invokeLater(autoCompleteTextField::showAllSuggestions);
                }
            }
        });

        // Thay thế các thành phần một hàng thông qua các phương thức của lớp cha và cập nhật đồng bộ CardLayout Thẻ và DocumentListener
        replaceTextField(autoCompleteTextField);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.parentTable = table;
        this.editingRow = row; // view row index

        // Gọi phương thức của lớp cha (lớp cha luôn trả về containerPanel）
        Component editor = super.getTableCellEditorComponent(table, value, isSelected, row, column);

        // gấp đôi invokeLater：Lần đầu chờ đợi stopCellEditing Viết model，
        // Lần thứ hai đảm bảo mọi thứ TableModelEvent Người nghe đều đã được xử lý trước khi đọc Key giá trị
        if (!isMultiLineMode()) {
            SwingUtilities.invokeLater(() ->
                SwingUtilities.invokeLater(this::updateSuggestionsForCurrentKey)
            );
        }

        return editor;
    }

    private void updateSuggestionsForCurrentKey() {
        if (parentTable == null || editingRow < 0) {
            disableAutoComplete();
            return;
        }

        // Chuyển đổi chỉ mục hàng của chế độ xem thành chỉ mục hàng mô hình (RowSorter Thứ tự hàng có thể đã được sắp xếp lại)
        int modelRow = editingRow;
        if (parentTable.getRowSorter() != null) {
            try {
                modelRow = parentTable.getRowSorter().convertRowIndexToModel(editingRow);
            } catch (IndexOutOfBoundsException e) {
                // Chỉ mục hàng xem không hợp lệ (có thể đã bị lọc ra), hãy bỏ cuộc
                disableAutoComplete();
                return;
            }
        }

        if (modelRow < 0 || modelRow >= parentTable.getModel().getRowCount()) {
            disableAutoComplete();
            return;
        }

        // Đọc cái tương ứng từ mô hình Key giá trị
        Object keyValue = parentTable.getModel().getValueAt(modelRow, keyColumnIndex);
        String headerKey = (keyValue != null) ? keyValue.toString().trim() : "";

        // nếu Key Để trống để tắt tính năng tự động hoàn thành
        if (headerKey.isEmpty()) {
            disableAutoComplete();
            return;
        }

        // Nhận danh sách giá trị được đề xuất tương ứng
        List<String> suggestions = HttpHeaderConstants.getCommonValuesForHeader(headerKey);

        if (suggestions == null || suggestions.isEmpty()) {
            disableAutoComplete();
        } else {
            autoCompleteTextField.setSuggestions(suggestions);
            autoCompleteTextField.setAutoCompleteEnabled(true);

            // Nếu văn bản trống, tất cả các đề xuất sẽ tự động được hiển thị
            String currentText = autoCompleteTextField.getText();
            if (currentText == null || currentText.trim().isEmpty()) {
                SwingUtilities.invokeLater(autoCompleteTextField::showAllSuggestions);
            }
        }
    }

    private void disableAutoComplete() {
        if (autoCompleteTextField != null) {
            autoCompleteTextField.setAutoCompleteEnabled(false);
            // Xóa danh sách đề xuất để tránh hiển thị các đề xuất cũ vào lần bật tiếp theo
            autoCompleteTextField.setSuggestions(Collections.emptyList());
        }
    }
}
