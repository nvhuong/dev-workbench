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

    /** The navigator title shared a 220px row with the Refresh button and was cut to "Database Navigat...". */
    @Test public void sectionTitlesAreNotTruncated() throws Exception {
        java.util.Locale original = com.nvhuong.devworkbench.util.I18nUtil.currentLocale();
        try {
            for (String language : new String[]{"vi", "en"}) {
                List<String> problems = new ArrayList<>();
                SwingUtilities.invokeAndWait(() -> {
                    com.nvhuong.devworkbench.util.I18nUtil.setLocale(java.util.Locale.forLanguageTag(language));
                    SqlPanel panel = new SqlPanel();
                    panel.setSize(1200, 720);
                    layoutRecursively(panel);
                    collectLabels(panel).forEach(label -> {
                        if (label.getWidth() + 1 < label.getPreferredSize().width) {
                            problems.add("'" + label.getText() + "' " + label.getWidth() + "/" + label.getPreferredSize().width);
                        }
                    });
                });
                org.testng.Assert.assertTrue(problems.isEmpty(), language + ": " + problems);
            }
        } finally {
            com.nvhuong.devworkbench.util.I18nUtil.setLocale(original);
        }
    }

    private static List<JLabel> collectLabels(java.awt.Container container) {
        List<JLabel> labels = new ArrayList<>();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof JLabel label && label.isVisible() && label.getText() != null
                    && !label.getText().isBlank() && label.getText().length() < 40) {
                labels.add(label);
            } else if (child instanceof java.awt.Container nested && !(child instanceof javax.swing.JTable)
                    && !(child instanceof javax.swing.JTree) && !(child instanceof javax.swing.JComboBox)) {
                labels.addAll(collectLabels(nested));
            }
        }
        return labels;
    }

    private static void layoutRecursively(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
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

    @Test public void stripsTerminatingSemicolonButKeepsProceduralBlocks() {
        assertEquals(SqlPanel.executableSql("SELECT 1;"), "SELECT 1");
        assertEquals(SqlPanel.executableSql("  SELECT 1 ;; "), "SELECT 1");
        assertEquals(SqlPanel.executableSql("SELECT 1"), "SELECT 1");
        assertEquals(SqlPanel.executableSql("BEGIN\n  NULL;\nEND;"), "BEGIN\n  NULL;\nEND;");
        assertEquals(SqlPanel.executableSql("begin null; end ;"), "begin null; end ;");
    }

    @Test public void explainsMissingDriverClass() throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SqlPanel();
            ((JTextField) field(panel[0], "jdbcUrl")).setText("jdbc:postgresql://localhost:5432/db");
            ((JTextField) field(panel[0], "driverClass")).setText("org.postgresql.Driver");
            invoke(panel[0], "connect");
        });
        JLabel status = (JLabel) field(panel[0], "status");
        waitUntil(() -> onEdt(() -> status.getText().contains("org.postgresql.Driver")),
                () -> "status=" + status.getText());
        SwingUtilities.invokeAndWait(() -> assertEquals(status.getText(),
                SqlI18n.t("error", SqlI18n.t("driver_class_not_found", "org.postgresql.Driver"))));
    }

    @Test public void ignoresRunShortcutWhileExecutionIsDisabled() throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SqlPanel();
            ((JTextField) field(panel[0], "jdbcUrl")).setText("jdbc:h2:mem:sql_panel_busy;DB_CLOSE_DELAY=-1");
            ((JTextField) field(panel[0], "driverClass")).setText("org.h2.Driver");
            ((JTextField) field(panel[0], "username")).setText("sa");
            invoke(panel[0], "connect");
        });
        javax.swing.JButton execute = (javax.swing.JButton) field(panel[0], "executeButton");
        JLabel status = (JLabel) field(panel[0], "status");
        waitUntil(() -> onEdt(execute::isEnabled), () -> "never connected; status=" + status.getText());
        javax.swing.JTextArea output = (javax.swing.JTextArea) field(panel[0], "outputLog");
        SwingUtilities.invokeAndWait(() -> {
            execute.setEnabled(false); // simulates a query or metadata load in progress
            org.fife.ui.rsyntaxtextarea.RSyntaxTextArea editor = activeEditor(panel[0]);
            editor.setText("SELECT 1;");
            invoke(panel[0], "executeSql");
            assertEquals(output.getText(), "", "Ctrl+Enter must not start a second statement while busy");
            invoke(panel[0], "disconnect");
        });
    }

    private static org.fife.ui.rsyntaxtextarea.RSyntaxTextArea activeEditor(SqlPanel panel) {
        try {
            Method method = SqlPanel.class.getDeclaredMethod("activeSqlEditor");
            method.setAccessible(true);
            return (org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) method.invoke(panel);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
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

    /** Tables were loaded into the model but model.reload() collapsed the schema, so users saw no tables. */
    @Test public void expandedSchemaStaysOpenAndShowsTables() throws Exception {
        SqlPanel panel = connectH2("sql_panel_visible", "CREATE TABLE IF NOT EXISTS PUBLIC.ORDERS(ID INT)");
        JTree tree = (JTree) field(panel, "objectTree");
        DefaultMutableTreeNode publicScope = awaitScope(panel, "PUBLIC");

        javax.swing.tree.TreePath scopePath = new javax.swing.tree.TreePath(publicScope.getPath());
        SwingUtilities.invokeAndWait(() -> tree.expandPath(scopePath));
        waitUntil(() -> onEdt(() -> childEndingWith(publicScope, "ORDERS") != null), () -> "tables not loaded");
        Thread.sleep(200);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(tree.isExpanded(scopePath), "schema must stay expanded after its tables load");
            DefaultMutableTreeNode orders = childEndingWith(publicScope, "ORDERS");
            assertTrue(tree.getRowForPath(new javax.swing.tree.TreePath(orders.getPath())) >= 0,
                    "ORDERS must be visible in the navigator");
            invoke(panel, "disconnect");
        });
    }

    /** After connecting, the connection's current schema is the default and its tables are shown without a click. */
    @Test public void currentSchemaIsDefaultAndExpandedAfterConnecting() throws Exception {
        SqlPanel panel = connectH2("sql_panel_auto_default", "CREATE TABLE IF NOT EXISTS PUBLIC.CUSTOMERS(ID INT)");
        JTree tree = (JTree) field(panel, "objectTree");
        DefaultMutableTreeNode publicScope = awaitScope(panel, "PUBLIC");

        waitUntil(() -> onEdt(() -> childEndingWith(publicScope, "CUSTOMERS") != null),
                () -> "tables of the current schema were not shown automatically");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(field(panel, "defaultScope"), publicScope.getUserObject());
            assertTrue(tree.isExpanded(new javax.swing.tree.TreePath(publicScope.getPath())));
            invoke(panel, "disconnect");
        });
    }

    /**
     * Databases without schemas/catalogs (SQLite) get a single "default" scope; it must also be expanded after
     * connecting so its tables are visible, and completion must offer them.
     */
    @Test public void schemalessDatabaseShowsTablesAfterConnecting() throws Exception {
        java.nio.file.Path db = java.nio.file.Files.createTempFile("sql-panel-", ".sqlite");
        try (java.sql.Connection setup = java.sql.DriverManager.getConnection("jdbc:sqlite:" + db);
             java.sql.Statement statement = setup.createStatement()) {
            statement.execute("CREATE TABLE invoices(id INTEGER PRIMARY KEY, total REAL)");
        }
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SqlPanel();
            ((JTextField) field(panel[0], "jdbcUrl")).setText("jdbc:sqlite:" + db);
            ((JTextField) field(panel[0], "driverClass")).setText("org.sqlite.JDBC");
            invoke(panel[0], "connect");
        });
        JTree tree = (JTree) field(panel[0], "objectTree");
        JLabel status = (JLabel) field(panel[0], "status");
        DefaultMutableTreeNode[] scope = new DefaultMutableTreeNode[1];
        waitUntil(() -> onEdt(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            scope[0] = root.getChildCount() == 1 ? (DefaultMutableTreeNode) root.getChildAt(0) : null;
            return scope[0] != null && scope[0].getUserObject().getClass().getSimpleName().equals("MetadataScope");
        }), () -> "scope not loaded; status=" + status.getText());
        try {
            waitUntil(() -> onEdt(() -> childEndingWith(scope[0], "invoices") != null),
                    () -> "tables of a schemaless database were not shown; status=" + status.getText());
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(tree.isExpanded(new javax.swing.tree.TreePath(scope[0].getPath())));
                try {
                    Method completions = SqlPanel.class.getDeclaredMethod("completionsFor", String.class);
                    completions.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>) completions.invoke(panel[0], "inv");
                    assertTrue(values.contains("invoices"), values.toString());
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> invoke(panel[0], "disconnect"));
            java.nio.file.Files.deleteIfExists(db);
        }
    }

    /** Unqualified SQL ran against the driver's default schema; users could not pick the schema to work in. */
    @Test public void selectedDefaultSchemaIsUsedForUnqualifiedSql() throws Exception {
        SqlPanel panel = connectH2("sql_panel_default_schema",
                "CREATE SCHEMA IF NOT EXISTS APP\\;CREATE TABLE IF NOT EXISTS APP.ITEMS(ID INT)\\;"
                        + "DELETE FROM APP.ITEMS\\;INSERT INTO APP.ITEMS VALUES(1)");
        DefaultMutableTreeNode appScope = awaitScope(panel, "APP");
        JLabel status = (JLabel) field(panel, "status");

        SwingUtilities.invokeAndWait(() -> {
            try {
                Method setDefault = SqlPanel.class.getDeclaredMethod("setDefaultScope", DefaultMutableTreeNode.class);
                setDefault.setAccessible(true);
                setDefault.invoke(panel, appScope);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
        waitUntil(() -> onEdt(() -> status.getText().contains("APP")), () -> "status=" + status.getText());

        javax.swing.table.DefaultTableModel results = (javax.swing.table.DefaultTableModel) field(panel, "resultModel");
        SwingUtilities.invokeAndWait(() -> {
            activeEditor(panel).setText("SELECT COUNT(*) AS N FROM ITEMS;");
            invoke(panel, "executeSql");
        });
        waitUntil(() -> onEdt(() -> results.getRowCount() == 1 || status.getText().contains("rror")),
                () -> "status=" + status.getText());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(results.getRowCount(), 1, status.getText());
            assertEquals(String.valueOf(results.getValueAt(0, 0)), "1");
            invoke(panel, "disconnect");
        });
    }

    /** Suggestions were only offered in a toolbar combo box; the editor itself showed none while typing. */
    @Test public void editorOffersSqlAndMetadataCompletions() throws Exception {
        SqlPanel panel = connectH2("sql_panel_completion", "CREATE TABLE IF NOT EXISTS PUBLIC.ORDERS(ORDER_ID INT)");
        JTree tree = (JTree) field(panel, "objectTree");
        DefaultMutableTreeNode publicScope = awaitScope(panel, "PUBLIC");
        SwingUtilities.invokeAndWait(() -> tree.expandPath(new javax.swing.tree.TreePath(publicScope.getPath())));
        waitUntil(() -> onEdt(() -> childEndingWith(publicScope, "ORDERS") != null), () -> "tables not loaded");

        SwingUtilities.invokeAndWait(() -> {
            org.fife.ui.rsyntaxtextarea.RSyntaxTextArea editor = activeEditor(panel);
            assertNotNull(editor.getInputMap().get(javax.swing.KeyStroke.getKeyStroke(
                            java.awt.event.KeyEvent.VK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK)),
                    "Ctrl+Space must open SQL completion in the editor");
            try {
                Method completions = SqlPanel.class.getDeclaredMethod("completionsFor", String.class);
                completions.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<String> keyword = (List<String>) completions.invoke(panel, "sel");
                @SuppressWarnings("unchecked")
                List<String> table = (List<String>) completions.invoke(panel, "ORD");
                assertTrue(keyword.stream().anyMatch(value -> value.startsWith("SELECT")), keyword.toString());
                assertTrue(table.stream().anyMatch(value -> value.contains("ORDERS")), table.toString());
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
            invoke(panel, "disconnect");
        });
    }

    private static SqlPanel connectH2(String database, String init) throws Exception {
        SqlPanel[] panel = new SqlPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SqlPanel();
            ((JTextField) field(panel[0], "jdbcUrl")).setText(
                    "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1;INIT=" + init);
            ((JTextField) field(panel[0], "driverClass")).setText("org.h2.Driver");
            ((JTextField) field(panel[0], "username")).setText("sa");
            invoke(panel[0], "connect");
        });
        return panel[0];
    }

    private static DefaultMutableTreeNode awaitScope(SqlPanel panel, String suffix) throws Exception {
        JTree tree = (JTree) field(panel, "objectTree");
        JLabel status = (JLabel) field(panel, "status");
        DefaultMutableTreeNode[] scope = new DefaultMutableTreeNode[1];
        waitUntil(() -> onEdt(() -> {
            scope[0] = childEndingWith((DefaultMutableTreeNode) tree.getModel().getRoot(), suffix);
            return scope[0] != null;
        }), () -> "schema " + suffix + " not loaded; status=" + status.getText());
        return scope[0];
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
