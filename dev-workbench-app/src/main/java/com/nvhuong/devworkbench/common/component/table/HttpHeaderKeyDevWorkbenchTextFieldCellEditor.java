package com.nvhuong.devworkbench.common.component.table;

import com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * Trình chỉnh sửa ô hỗ trợ tự động hoàn thành
 * Đã đặt trước DevWorkbenchTextField Tất cả các chức năng (đánh dấu biến, hoàn tác và làm lại, v.v.)
 * Đã thêm chức năng tự động hoàn thành
 */
public class HttpHeaderKeyDevWorkbenchTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final AutoCompleteDevWorkbenchTextField textField;

    public HttpHeaderKeyDevWorkbenchTextFieldCellEditor(List<String> suggestions) {
        this.textField = new AutoCompleteDevWorkbenchTextField(1);
        this.textField.setBorder(null);

        if (suggestions != null && !suggestions.isEmpty()) {
            this.textField.setSuggestions(suggestions);
            this.textField.setAutoCompleteEnabled(true);

            // Tối ưu hóa: chỉ hiển thị tất cả gợi ý khi văn bản trống
            this.textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    String text = textField.getText();
                    if (text == null || text.trim().isEmpty()) {
                        SwingUtilities.invokeLater(textField::showAllSuggestions);
                    }
                }
            });
        }
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textField.setText(value == null ? "" : value.toString());
        TableUIConstants.styleTextCellEditor(textField, table, row);
        // Chọn tất cả khi được kích hoạt để tạo điều kiện ghi đè trực tiếp đầu vào
        SwingUtilities.invokeLater(textField::selectAll);
        return textField;
    }
}
