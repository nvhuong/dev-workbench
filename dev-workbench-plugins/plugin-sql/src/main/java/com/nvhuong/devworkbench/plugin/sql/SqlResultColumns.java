package com.nvhuong.devworkbench.plugin.sql;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Component;

/**
 * Sizes result columns to their content. The result table scrolls horizontally, so without this every column
 * keeps Swing's default 75 px and most values are cut off.
 */
final class SqlResultColumns {
    static final int MIN_WIDTH = 48;
    static final int MAX_WIDTH = 420;
    private static final int SAMPLE_ROWS = 200;
    private static final int PADDING = 18;

    private SqlResultColumns() {
    }

    static void fit(JTable table) {
        int rows = Math.min(table.getRowCount(), SAMPLE_ROWS);
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            int width = headerWidth(table, tableColumn, column);
            for (int row = 0; row < rows && width < MAX_WIDTH; row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component cell = table.prepareRenderer(renderer, row, column);
                width = Math.max(width, cell.getPreferredSize().width);
            }
            tableColumn.setPreferredWidth(Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width + PADDING)));
        }
    }

    private static int headerWidth(JTable table, TableColumn column, int index) {
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null && table.getTableHeader() != null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        if (renderer == null) {
            return table.getFontMetrics(table.getFont()).stringWidth(String.valueOf(column.getHeaderValue()));
        }
        Component header = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, index);
        return header.getPreferredSize().width;
    }
}
