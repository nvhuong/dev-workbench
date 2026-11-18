package com.nvhuong.devworkbench.common.component.table;

import com.nvhuong.devworkbench.common.component.DevWorkbenchTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class DevWorkbenchTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final DevWorkbenchTextField textField = new DevWorkbenchTextField(1);

    public DevWorkbenchTextFieldCellEditor() {
        textField.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textField.setText(value == null ? "" : value.toString());
        TableUIConstants.styleTextCellEditor(textField, table, row);
        return textField;
    }
}
