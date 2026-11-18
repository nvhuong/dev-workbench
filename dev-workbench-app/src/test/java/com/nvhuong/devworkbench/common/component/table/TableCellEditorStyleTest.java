package com.nvhuong.devworkbench.common.component.table;

import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TableCellEditorStyleTest extends AbstractSwingUiTest {

    @Test
    public void textCellEditorShouldNotInstallUnsupportedFlatLafStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(1, 2);
            DevWorkbenchTextFieldCellEditor editor = new DevWorkbenchTextFieldCellEditor();

            Component component = editor.getTableCellEditorComponent(table, "active", false, 0, 1);

            assertTrue(component instanceof JTextComponent);
            assertNotNull(((JComponent) component).getBorder());
            assertNull(((JComponent) component).getClientProperty("FlatLaf.style"));
        });
    }

    @Test
    public void smartValueCellEditorShouldNotInstallUnsupportedFlatLafStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(1, 2);
            DevWorkbenchSmartValueCellEditor editor = new DevWorkbenchSmartValueCellEditor();

            Component component = editor.getTableCellEditorComponent(table, "active", false, 0, 1);

            assertTrue(component instanceof JComponent);
            assertNotNull(((JComponent) component).getBorder());
            assertNull(editor.textField.getClientProperty("FlatLaf.style"));
        });
    }
}
