package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;
import javax.swing.SwingUtilities;
import static org.testng.Assert.assertTrue;

public class SqlPanelTest {
    @Test public void buildsJdbcWorkbench() throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SqlPanel());
        assertTrue(panel[0].getComponentCount() > 0);
    }
}
