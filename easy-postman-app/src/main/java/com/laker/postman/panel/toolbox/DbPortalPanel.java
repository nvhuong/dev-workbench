package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Font;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

/** JDBC workbench for connecting to databases, exploring metadata, and executing SQL. */
@Slf4j
public class DbPortalPanel extends JPanel {
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int MAX_RESULT_ROWS = 500;
    private static final List<String> SQL_SUGGESTIONS = List.of(
            "SELECT * FROM ", "INSERT INTO ", "UPDATE ", "DELETE FROM ", "CREATE TABLE ",
            "ALTER TABLE ", "DROP TABLE ", "JOIN ", "WHERE ", "GROUP BY ", "ORDER BY ", "LIMIT "
    );

    private final JComboBox<DatabasePreset> databaseType = new JComboBox<>(DatabasePreset.values());
    private final JTextField jdbcUrl = new JTextField();
    private final JTextField driverClass = new JTextField();
    private final JTextField driverJar = new JTextField();
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel status = new JLabel(" ");
    private final JTree objectTree = new JTree(new DefaultMutableTreeNode("Database"));
    private final RSyntaxTextArea sqlEditor = new RSyntaxTextArea();
    private final JComboBox<String> suggestions = new JComboBox<>();
    private final DefaultTableModel resultModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable resultTable = new JTable(resultModel);
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton executeButton;
    private JButton refreshObjectsButton;
    private JButton browseDriverButton;
    private Connection connection;
    private URLClassLoader driverClassLoader;
    private List<String> metadataSuggestions = new ArrayList<>();
    private DatabasePreset previousPreset = DatabasePreset.GENERIC;

    public DbPortalPanel() {
        initUI();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);
        add(createConnectionPanel(), BorderLayout.NORTH);
        add(createWorkbench(), BorderLayout.CENTER);
        status.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        add(ToolboxWorkbench.statusBar(status), BorderLayout.SOUTH);
        updateConnectionControls(false);
        databaseType.addActionListener(event -> applyPreset());
        applyPreset();
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, novisualpadding, fillx, wrap",
                "[pref!][grow,fill][pref!][grow,fill][pref!][grow,fill][pref!][grow,fill]",
                "[]4[]4[]4[]4[]"));
        panel.setOpaque(false);

        connectButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_CONNECT));
        disconnectButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_DISCONNECT));
        browseDriverButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_BROWSE));
        connectButton.addActionListener(event -> connect());
        disconnectButton.addActionListener(event -> disconnect());
        browseDriverButton.addActionListener(event -> chooseDriverJar());
        jdbcUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_URL_HINT));
        driverClass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_DRIVER_HINT));
        driverJar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_DRIVER_JAR_HINT));

        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_TYPE));
        panel.add(databaseType, "wmin 120");
        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_URL));
        panel.add(jdbcUrl, "span 5, growx, wmin 240, wrap");
        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_DRIVER_CLASS));
        panel.add(driverClass, "span 7, growx, wrap");
        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_DRIVER_JAR));
        panel.add(driverJar, "span 5, growx");
        panel.add(browseDriverButton, "span 2, wrap");
        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_USERNAME));
        panel.add(username, "wmin 100");
        panel.add(label(MessageKeys.TOOLBOX_DBPORTAL_PASSWORD));
        panel.add(password, "wmin 100");
        panel.add(connectButton, "split 2");
        panel.add(disconnectButton, "wrap");
        JLabel autocommitHint = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_AUTOCOMMIT_HINT));
        autocommitHint.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        panel.add(autocommitHint, "span 8, wrap");
        return panel;
    }

    private JPanel createWorkbench() {
        JPanel browser = new JPanel(new BorderLayout(0, 4));
        browser.setOpaque(false);
        JPanel browserHeader = new JPanel(new BorderLayout(4, 0));
        browserHeader.setOpaque(false);
        browserHeader.add(ToolboxWorkbench.sectionTitle(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_OBJECTS)), BorderLayout.CENTER);
        refreshObjectsButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_REFRESH));
        refreshObjectsButton.addActionListener(event -> loadMetadata());
        browserHeader.add(refreshObjectsButton, BorderLayout.EAST);
        browser.add(browserHeader, BorderLayout.NORTH);
        objectTree.setRootVisible(false);
        objectTree.setShowsRootHandles(true);
        objectTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Object selected = objectTree.getLastSelectedPathComponent();
                    if (selected instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TableRef table) {
                        sqlEditor.setText("SELECT * FROM " + table.qualifiedName() + ";");
                        sqlEditor.setCaretPosition(0);
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(objectTree);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(treeScroll);
        browser.add(treeScroll, BorderLayout.CENTER);

        JPanel editor = new JPanel(new BorderLayout(0, 6));
        editor.setOpaque(false);
        JPanel editorToolbar = new JPanel(new MigLayout("insets 0, novisualpadding, gapx 6", "[][grow,fill][]", "[]"));
        editorToolbar.setOpaque(false);
        editorToolbar.add(label(MessageKeys.TOOLBOX_DBPORTAL_SUGGESTIONS));
        suggestions.setEditable(false);
        editorToolbar.add(suggestions, "wmin 100");
        JButton insertSuggestion = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_INSERT));
        insertSuggestion.addActionListener(event -> insertSelectedSuggestion());
        executeButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_EXECUTE));
        executeButton.addActionListener(event -> executeSql());
        editorToolbar.add(insertSuggestion, "split 2");
        editorToolbar.add(executeButton);
        editor.add(editorToolbar, BorderLayout.NORTH);

        sqlEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        sqlEditor.setCodeFoldingEnabled(true);
        sqlEditor.setTabSize(4);
        sqlEditor.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        EditorThemeUtil.loadTheme(sqlEditor);
        JScrollPane sqlScroll = new JScrollPane(sqlEditor);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(sqlScroll);
        JPanel sqlSection = ToolboxWorkbench.editorSection(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_SQL), sqlScroll);
        JScrollPane resultScroll = new JScrollPane(resultTable);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(resultScroll);
        JPanel resultSection = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_RESULTS), resultScroll);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setFillsViewportHeight(true);
        JSplitPane verticalSplit = ToolboxWorkbench.editorSplit(sqlSection, resultSection, 180);
        verticalSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.45);
        editor.add(verticalSplit, BorderLayout.CENTER);

        JSplitPane split = ToolboxWorkbench.horizontalSplit(browser, editor, 220);
        split.setResizeWeight(0.23);
        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);
        host.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        host.add(split, BorderLayout.CENTER);
        return host;
    }

    private JLabel label(String key) {
        return new JLabel(I18nUtil.getMessage(key) + ":");
    }

    private void applyPreset() {
        DatabasePreset preset = (DatabasePreset) databaseType.getSelectedItem();
        if (preset == null) return;
        if (jdbcUrl.getText().isBlank() || jdbcUrl.getText().equals(previousPreset.exampleUrl())) {
            jdbcUrl.setText(preset.exampleUrl());
        }
        if (driverClass.getText().isBlank() || driverClass.getText().equals(previousPreset.driverClass())) {
            driverClass.setText(preset.driverClass());
        }
        previousPreset = preset;
    }

    private void chooseDriverJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_DRIVER_JAR), "jar"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File[] selectedFiles = chooser.getSelectedFiles();
            if (selectedFiles.length == 0 && chooser.getSelectedFile() != null) {
                selectedFiles = new java.io.File[]{chooser.getSelectedFile()};
            }
            driverJar.setText(java.util.Arrays.stream(selectedFiles)
                    .map(file -> file.getAbsolutePath())
                    .collect(java.util.stream.Collectors.joining(";")));
        }
    }

    private void connect() {
        String url = jdbcUrl.getText().trim();
        if (url.isBlank() || !url.startsWith("jdbc:")) {
            setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_INVALID_URL));
            return;
        }
        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_CONNECTING));
        connectButton.setEnabled(false);
        setConnectionFieldsEnabled(false);
        new SwingWorker<Connection, Void>() {
            @Override protected Connection doInBackground() throws Exception {
                URLClassLoader loader = null;
                String jar = driverJar.getText().trim();
                if (!jar.isBlank()) {
                    URL[] jars = java.util.Arrays.stream(jar.split(";"))
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .map(Path::of)
                            .peek(path -> {
                                if (!path.toFile().isFile()) {
                                    throw new IllegalArgumentException(I18nUtil.getMessage(
                                            MessageKeys.TOOLBOX_DBPORTAL_DRIVER_JAR_MISSING, path.getFileName()));
                                }
                            })
                            .map(path -> {
                                try { return path.toUri().toURL(); }
                                catch (Exception exception) {
                                    throw new IllegalArgumentException(I18nUtil.getMessage(
                                            MessageKeys.TOOLBOX_DBPORTAL_DRIVER_JAR_INVALID));
                                }
                            })
                            .toArray(URL[]::new);
                    loader = new URLClassLoader(jars, DbPortalPanel.class.getClassLoader());
                }
                try {
                    Driver driver = loadDriver(loader, driverClass.getText().trim(), url);
                    Properties properties = new Properties();
                    if (!username.getText().isBlank()) properties.setProperty("user", username.getText().trim());
                    char[] secret = password.getPassword();
                    if (secret.length > 0) properties.setProperty("password", new String(secret));
                    Connection opened = driver == null
                            ? DriverManager.getConnection(url, properties)
                            : driver.connect(url, properties);
                    if (opened == null) throw new IllegalArgumentException(I18nUtil.getMessage(
                            MessageKeys.TOOLBOX_DBPORTAL_DRIVER_REJECTED_URL));
                    synchronized (DbPortalPanel.this) {
                        closeResources();
                        driverClassLoader = loader;
                        loader = null;
                    }
                    return opened;
                } finally {
                    if (loader != null) loader.close();
                }
            }

            @Override protected void done() {
                try {
                    connection = get();
                    password.setText("");
                    updateConnectionControls(true);
                    setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_CONNECTED));
                    loadMetadata();
                } catch (Exception exception) {
                    updateConnectionControls(false);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    log.warn("JDBC connection failed: {}", cause.getClass().getSimpleName());
                    setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_ERROR, safeMessage(cause)));
                }
            }
        }.execute();
    }

    private Driver loadDriver(ClassLoader loader, String className, String url) throws Exception {
        if (!className.isBlank()) {
            ClassLoader effectiveLoader = loader == null ? DbPortalPanel.class.getClassLoader() : loader;
            return (Driver) Class.forName(className, true, effectiveLoader).getDeclaredConstructor().newInstance();
        }
        if (loader != null) {
            for (Driver driver : ServiceLoader.load(Driver.class, loader)) {
                if (driver.acceptsURL(url)) return driver;
            }
        }
        return null;
    }

    private void loadMetadata() {
        Connection active = connection;
        if (active == null) return;
        executeButton.setEnabled(false);
        refreshObjectsButton.setEnabled(false);
        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_LOADING_OBJECTS));
        new SwingWorker<MetadataSnapshot, Void>() {
            @Override protected MetadataSnapshot doInBackground() throws Exception {
                return readMetadata(active);
            }

            @Override protected void done() {
                if (connection != active) return;
                try {
                    MetadataSnapshot snapshot = get();
                    executeButton.setEnabled(true);
                    refreshObjectsButton.setEnabled(true);
                    objectTree.setModel(new DefaultTreeModel(snapshot.root()));
                    for (int row = 0; row < objectTree.getRowCount(); row++) {
                        TreePath path = objectTree.getPathForRow(row);
                        if (path != null && path.getPathCount() == 2) objectTree.expandRow(row);
                    }
                    metadataSuggestions = snapshot.suggestions();
                    refreshSuggestions();
                    setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_OBJECTS_LOADED,
                            snapshot.tableCount(), snapshot.schemaCount()));
                } catch (Exception exception) {
                    executeButton.setEnabled(true);
                    refreshObjectsButton.setEnabled(true);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_ERROR, safeMessage(cause)));
                }
            }
        }.execute();
    }

    private MetadataSnapshot readMetadata(Connection active) throws Exception {
        DatabaseMetaData metadata = active.getMetaData();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(metadata.getDatabaseProductName());
        List<String> completion = new ArrayList<>(SQL_SUGGESTIONS);
        int schemaCount = 0;
        int tableCount = 0;
        List<String[]> schemas = new ArrayList<>();
        try (ResultSet result = metadata.getSchemas()) {
            while (result.next()) schemas.add(new String[]{result.getString("TABLE_CATALOG"), result.getString("TABLE_SCHEM")});
        } catch (Exception ignored) { }
        if (schemas.isEmpty()) {
            try (ResultSet catalogs = metadata.getCatalogs()) {
                while (catalogs.next()) {
                    String catalog = catalogs.getString("TABLE_CAT");
                    DefaultMutableTreeNode catalogNode = new DefaultMutableTreeNode(catalog);
                    int count = addTables(metadata, catalog, null, catalogNode, completion);
                    root.add(catalogNode);
                    tableCount += count;
                }
            } catch (Exception ignored) { }
            if (root.getChildCount() == 0) tableCount = addTables(metadata, null, null, root, completion);
        } else {
            for (String[] schema : schemas) {
                String catalog = schema[0];
                String schemaName = schema[1];
                DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(schemaName);
                int count = addTables(metadata, catalog, schemaName, schemaNode, completion);
                if (catalog != null && !catalog.isBlank()) {
                    DefaultMutableTreeNode catalogNode = findOrAdd(root, catalog);
                    catalogNode.add(schemaNode);
                } else root.add(schemaNode);
                schemaCount++;
                tableCount += count;
            }
        }
        return new MetadataSnapshot(root, completion.stream().distinct().toList(), tableCount, schemaCount);
    }

    private DefaultMutableTreeNode findOrAdd(DefaultMutableTreeNode root, String name) {
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
            if (name.equals(node.getUserObject())) return node;
        }
        DefaultMutableTreeNode created = new DefaultMutableTreeNode(name);
        root.add(created);
        return created;
    }

    private int addTables(DatabaseMetaData metadata, String catalog, String schema,
                          DefaultMutableTreeNode parent, List<String> completion) throws Exception {
        int count = 0;
        String identifierQuote;
        try {
            identifierQuote = metadata.getIdentifierQuoteString();
        } catch (Exception ignored) {
            identifierQuote = "";
        }
        try (ResultSet tables = metadata.getTables(catalog, schema, null, new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                TableRef ref = new TableRef(catalog, schema, table, identifierQuote);
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(ref);
                parent.add(tableNode);
                completion.add(ref.qualifiedName());
                try (ResultSet columns = metadata.getColumns(catalog, schema, table, null)) {
                    while (columns.next()) {
                        String column = columns.getString("COLUMN_NAME");
                        String dataType = columns.getString("TYPE_NAME");
                        tableNode.add(new DefaultMutableTreeNode(column + "  ·  " + dataType));
                        completion.add(column);
                    }
                } catch (Exception ignored) { }
                count++;
            }
        } catch (Exception ignored) { }
        return count;
    }

    private void refreshSuggestions() {
        Object selected = suggestions.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(metadataSuggestions.toArray(String[]::new));
        suggestions.setModel(model);
        if (selected != null) suggestions.setSelectedItem(selected);
    }

    private void insertSelectedSuggestion() {
        Object selected = suggestions.getSelectedItem();
        if (selected == null) return;
        sqlEditor.insert(selected.toString(), sqlEditor.getCaretPosition());
        sqlEditor.requestFocusInWindow();
    }

    private void executeSql() {
        Connection active = connection;
        String sql = sqlEditor.getText().trim();
        if (active == null || sql.isBlank()) return;
        executeButton.setEnabled(false);
        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_EXECUTING));
        new SwingWorker<QueryResult, Void>() {
            @Override protected QueryResult doInBackground() throws Exception {
                try (Statement statement = active.createStatement()) {
                    try { statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS); }
                    catch (SQLFeatureNotSupportedException ignored) { }
                    try { statement.setMaxRows(MAX_RESULT_ROWS); }
                    catch (SQLFeatureNotSupportedException ignored) { }
                    boolean hasResult = statement.execute(sql);
                    if (!hasResult) return new QueryResult(List.of(), List.of(), statement.getUpdateCount(), false);
                    try (ResultSet result = statement.getResultSet()) {
                        ResultSetMetaData metadata = result.getMetaData();
                        List<String> columns = new ArrayList<>();
                        for (int i = 1; i <= metadata.getColumnCount(); i++) columns.add(metadata.getColumnLabel(i));
                        List<Object[]> rows = new ArrayList<>();
                        while (rows.size() < MAX_RESULT_ROWS && result.next()) {
                            Object[] row = new Object[columns.size()];
                            for (int i = 0; i < row.length; i++) row[i] = result.getObject(i + 1);
                            rows.add(row);
                        }
                        return new QueryResult(columns, rows, rows.size(), true);
                    }
                }
            }

            @Override protected void done() {
                if (connection != active) return;
                executeButton.setEnabled(connection != null);
                try {
                    QueryResult result = get();
                    resultModel.setRowCount(0);
                    resultModel.setColumnIdentifiers(result.columns().toArray());
                    for (Object[] row : result.rows()) resultModel.addRow(row);
                    if (result.hasRows()) {
                        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_QUERY_COMPLETE,
                                result.count(), MAX_RESULT_ROWS));
                    } else if (result.count() < 0) {
                        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_STATEMENT_COMPLETE));
                    } else {
                        setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_UPDATE_COMPLETE, result.count()));
                    }
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_ERROR, safeMessage(cause)));
                }
            }
        }.execute();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private void disconnect() {
        try {
            closeResources();
            connection = null;
            objectTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Database")));
            resultModel.setRowCount(0);
            resultModel.setColumnCount(0);
            metadataSuggestions = new ArrayList<>();
            refreshSuggestions();
            updateConnectionControls(false);
            setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_DISCONNECTED));
        } catch (Exception exception) {
            setStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_DBPORTAL_ERROR, safeMessage(exception)));
        }
    }

    private synchronized void closeResources() throws Exception {
        if (connection != null) connection.close();
        connection = null;
        if (driverClassLoader != null) driverClassLoader.close();
        driverClassLoader = null;
    }

    private void updateConnectionControls(boolean connected) {
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        executeButton.setEnabled(connected);
        refreshObjectsButton.setEnabled(connected);
        setConnectionFieldsEnabled(!connected);
    }

    private void setConnectionFieldsEnabled(boolean enabled) {
        databaseType.setEnabled(enabled);
        jdbcUrl.setEnabled(enabled);
        driverClass.setEnabled(enabled);
        driverJar.setEnabled(enabled);
        browseDriverButton.setEnabled(enabled);
        username.setEnabled(enabled);
        password.setEnabled(enabled);
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setToolTipText(text);
    }

    private record MetadataSnapshot(DefaultMutableTreeNode root, List<String> suggestions,
                                    int tableCount, int schemaCount) { }
    private record QueryResult(List<String> columns, List<Object[]> rows, int count, boolean hasRows) { }
    private record TableRef(String catalog, String schema, String table, String identifierQuote) {
        String qualifiedName() {
            return java.util.stream.Stream.of(catalog, schema, table)
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> quoteIdentifier(value, identifierQuote))
                    .reduce((left, right) -> left + "." + right).orElse(table);
        }
        @Override public String toString() { return table; }
    }

    private static String quoteIdentifier(String identifier, String quote) {
        if (quote == null || quote.isBlank()) return identifier;
        String normalizedQuote = quote.trim();
        return normalizedQuote + identifier.replace(normalizedQuote, normalizedQuote + normalizedQuote) + normalizedQuote;
    }

    private enum DatabasePreset {
        GENERIC("Generic JDBC", ""),
        POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
        MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
        ORACLE("Oracle", "oracle.jdbc.OracleDriver"),
        SQL_SERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        SQLITE("SQLite", "org.sqlite.JDBC");

        private final String label;
        private final String driverClass;
        DatabasePreset(String label, String driverClass) {
            this.label = label;
            this.driverClass = driverClass;
        }
        String exampleUrl() {
            return switch (this) {
                case GENERIC -> "jdbc:<vendor>:<connection-string>";
                case POSTGRESQL -> "jdbc:postgresql://localhost:5432/database";
                case MYSQL -> "jdbc:mysql://localhost:3306/database";
                case ORACLE -> "jdbc:oracle:thin:@//localhost:1521/service";
                case SQL_SERVER -> "jdbc:sqlserver://localhost:1433;databaseName=database";
                case SQLITE -> "jdbc:sqlite:database.sqlite";
            };
        }
        String driverClass() { return driverClass; }
        @Override public String toString() { return label; }
    }
}
