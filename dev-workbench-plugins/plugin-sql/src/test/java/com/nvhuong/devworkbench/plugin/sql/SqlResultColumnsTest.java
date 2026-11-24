package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SqlResultColumnsTest {

    @Test
    public void widensColumnsToFitHeaderAndValues() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "FULL_NAME_OF_CUSTOMER"}, 0);
        model.addRow(new Object[]{1, "Nguyễn Văn An"});
        model.addRow(new Object[]{2, "Trần Thị Bình — một tên khá dài để kiểm tra độ rộng cột"});
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        SqlResultColumns.fit(table);

        int idWidth = table.getColumnModel().getColumn(0).getPreferredWidth();
        int nameWidth = table.getColumnModel().getColumn(1).getPreferredWidth();
        int longText = table.getFontMetrics(table.getFont())
                .stringWidth("Trần Thị Bình — một tên khá dài để kiểm tra độ rộng cột");
        assertTrue(idWidth >= SqlResultColumns.MIN_WIDTH, "id column keeps a minimum width: " + idWidth);
        assertTrue(idWidth < 120, "short column stays narrow: " + idWidth);
        assertTrue(nameWidth > longText, "long values are not cut off: " + nameWidth + " <= " + longText);
    }

    @Test
    public void capsVeryLongValues() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"PAYLOAD"}, 0);
        model.addRow(new Object[]{"x".repeat(5000)});
        JTable table = new JTable(model);

        SqlResultColumns.fit(table);

        assertEquals(table.getColumnModel().getColumn(0).getPreferredWidth(), SqlResultColumns.MAX_WIDTH);
    }
}
