package com.laker.postman.panel.functional.table;

import com.laker.postman.functional.model.RunnerRowData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FunctionalRunnerTableModel extends AbstractTableModel {

    private static final int COLUMN_COUNT = 7;
    private final List<RunnerRowData> rows = new ArrayList<>();

    @Override
    public synchronized int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 1 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_NAME);
            case 2 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_URL);
            case 3 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_METHOD);
            case 4 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_STATUS);
            case 5 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_TIME);
            case 6 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_RESULT);
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class;
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        RunnerRowData row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.selected;
            case 1 -> row.name;
            case 2 -> row.url;
            case 3 -> row.method;
            case 4 -> row.status != null && !row.status.isEmpty() ? row.status : "-";
            case 5 -> row.cost > 0 ? TimeDisplayUtil.formatElapsedTime(row.cost) : "-";
            case 6 -> row.assertion != null ? row.assertion : "-";
            default -> null;
        };
    }

    @Override
    public synchronized void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        RunnerRowData row = rows.get(rowIndex);
        if (columnIndex == 0) {
            row.selected = (Boolean) aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public synchronized void addRow(RunnerRowData row) {
        rows.add(row);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public synchronized void clear() {
        int size = rows.size();
        if (size > 0) {
            rows.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    public synchronized RunnerRowData getRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex);
    }

    public synchronized void moveRow(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        if (fromIndex < 0 || fromIndex >= rows.size() || toIndex < 0 || toIndex >= rows.size()) {
            return;
        }
        RunnerRowData row = rows.remove(fromIndex);
        if (toIndex > fromIndex) toIndex--;
        rows.add(toIndex, row);
        fireTableDataChanged();
    }

    /**
     * Xóa hàng được chỉ định
     */
    public synchronized void removeRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < rows.size()) {
            rows.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /**
     * Xóa tất cả các hàng đã chọn
     *
     * @return Số hàng cần xóa
     */
    public synchronized int removeSelectedRows() {
        List<RunnerRowData> toRemove = new ArrayList<>();
        for (RunnerRowData row : rows) {
            if (row.selected) {
                toRemove.add(row);
            }
        }
        rows.removeAll(toRemove);
        if (!toRemove.isEmpty()) {
            fireTableDataChanged();
        }
        return toRemove.size();
    }

    /**
     * Đặt trạng thái đã chọn của tất cả các hàng
     */
    public synchronized void setAllSelected(boolean selected) {
        for (RunnerRowData row : rows) {
            row.selected = selected;
        }
        fireTableDataChanged();
    }

    /**
     * Lấy số lượng yêu cầu đã chọn
     */
    public synchronized int getSelectedCount() {
        return (int) rows.stream().filter(row -> row.selected).count();
    }

    /**
     * Nhận tất cả dữ liệu hàng (được sử dụng để lưu bộ kiểm tra)
     */
    public synchronized List<RunnerRowData> getAllRows() {
        return new ArrayList<>(rows);
    }

    /**
     * Kiểm tra xem có hàng được chọn hay không
     */
    public synchronized boolean hasSelectedRows() {
        return rows.stream().anyMatch(row -> row.selected);
    }
}
