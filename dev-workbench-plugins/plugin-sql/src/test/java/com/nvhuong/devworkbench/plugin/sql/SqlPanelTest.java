package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SqlPanelTest {
    @Test public void buildsJdbcWorkbench() throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SqlPanel());
        assertTrue(panel[0].getComponentCount() > 0);
    }

    @Test public void picksStatementWhenCaretFollowsTerminatingSemicolon() {
        assertEquals(SqlPanel.statementAt("SELECT 1;", 9), "SELECT 1;");
        assertEquals(SqlPanel.statementAt("SELECT 1;  \n", 12), "SELECT 1;");
        assertEquals(SqlPanel.statementAt("SELECT 1", 8), "SELECT 1");
    }

    @Test public void picksStatementAroundCaretInMultiStatementScript() {
        String script = "SELECT 1;\nSELECT 2;\nSELECT 3";
        assertEquals(SqlPanel.statementAt(script, 3), "SELECT 1;");
        assertEquals(SqlPanel.statementAt(script, script.indexOf("SELECT 2")), "SELECT 2;");
        assertEquals(SqlPanel.statementAt(script, script.indexOf("SELECT 2") + "SELECT 2;".length()), "SELECT 2;");
        assertEquals(SqlPanel.statementAt(script, script.length()), "SELECT 3");
        assertEquals(SqlPanel.statementAt("   ", 1), "");
    }

    @Test public void expandsSchemaIntoTablesAfterConnecting() throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SqlPanel();
            ((JTextField) field(panel[0], "jdbcUrl")).setText(
                    "jdbc:h2:mem:sql_panel_test;DB_CLOSE_DELAY=-1;INIT=CREATE TABLE IF NOT EXISTS PUBLIC.ORDERS(ID INT)");
            ((JTextField) field(panel[0], "driverClass")).setText("org.h2.Driver");
            ((JTextField) field(panel[0], "username")).setText("sa");
            invoke(panel[0], "connect");
        });
        JLabel status = (JLabel) field(panel[0], "status");
        JTree tree = (JTree) field(panel[0], "objectTree");
        DefaultMutableTreeNode[] publicScope = new DefaultMutableTreeNode[1];
        waitUntil(() -> onEdt(() -> {
            publicScope[0] = childEndingWith((DefaultMutableTreeNode) tree.getModel().getRoot(), "PUBLIC");
            return publicScope[0] != null;
        }), () -> "schemas not loaded; status=" + status.getText());

        SwingUtilities.invokeAndWait(() -> tree.expandPath(new javax.swing.tree.TreePath(publicScope[0].getPath())));
        waitUntil(() -> onEdt(() -> childEndingWith(publicScope[0], "ORDERS") != null),
                () -> "tables not loaded; status=" + status.getText());

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(status.getText().contains("Exception"), status.getText());
            DefaultMutableTreeNode orders = childEndingWith(publicScope[0], "ORDERS");
            assertNotNull(orders);
            assertTrue(orders.getUserObject().getClass().getSimpleName().equals("TableRef"));
            invoke(panel[0], "disconnect");
        });
    }

    private static DefaultMutableTreeNode childEndingWith(DefaultMutableTreeNode parent, String suffix) {
        if (parent == null) return null;
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) children.add((DefaultMutableTreeNode) parent.getChildAt(i));
        return children.stream().filter(child -> child.toString().endsWith(suffix)).findFirst().orElse(null);
    }

    private static boolean onEdt(BooleanSupplier check) {
        boolean[] result = new boolean[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = check.getAsBoolean());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return result[0];
    }

    private static void waitUntil(BooleanSupplier condition, java.util.function.Supplier<String> failure) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(50);
        }
        throw new AssertionError(failure.get());
    }

    private static Object field(Object target, String name) {
        try {
            Field field = SqlPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void invoke(Object target, String name) {
        try {
            Method method = SqlPanel.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
