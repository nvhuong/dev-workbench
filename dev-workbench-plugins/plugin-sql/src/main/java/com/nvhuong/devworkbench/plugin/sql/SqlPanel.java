package com.nvhuong.devworkbench.plugin.sql;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** JDBC workbench for connecting to databases, exploring metadata, and executing SQL. */
@Slf4j
public class SqlPanel extends JPanel {
    private static final String PROFILE_FILE = "connection-profiles.txt";
    private static final String ACTIVE_PROFILE_FILE = "active-profile.txt";
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int METADATA_TIMEOUT_SECONDS = 60;
    private static final int METADATA_TIMEOUT_MILLIS = METADATA_TIMEOUT_SECONDS * 1000;
    private static final int MAX_RESULT_ROWS = 500;
    private static final java.util.regex.Pattern PROCEDURAL_BLOCK_END =
            java.util.regex.Pattern.compile("(?i)\\bEND\\s*;$");
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
    private final JTabbedPane sqlTabs = new JTabbedPane();
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final JTextArea outputLog = new JTextArea();
    private int sqlTabSequence = 1;
    private final JComboBox<String> suggestions = new JComboBox<>();
    private final JComboBox<ConnectionProfile> profiles = new JComboBox<>();
    private final DefaultTableModel resultModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable resultTable = new JTable(resultModel);
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton executeButton;
    private JButton refreshObjectsButton;
    private JButton browseDriverButton;
    private JButton saveProfileButton;
    private JButton copyProfileButton;
    private JButton deleteProfileButton;
    private JButton newProfileButton;
    private final PluginStorage storage;
    private boolean loadingProfiles;
    private Connection connection;
    private URLClassLoader driverClassLoader;
    private SwingWorker<MetadataSnapshot, Void> metadataWorker;
    private Timer metadataTimeoutTimer;
    private long metadataLoadSequence;
    private long timedOutMetadataSequence = -1;
    private List<String> metadataSuggestions = new ArrayList<>();
    private DatabasePreset previousPreset = DatabasePreset.GENERIC;
    /** Schema (or catalog) that unqualified SQL runs against; shown in bold in the navigator. */
    private MetadataScope defaultScope;
    /** Shared by every SQL tab's in-editor completion popup; rebuilt from keywords + loaded metadata. */
    private final org.fife.ui.autocomplete.DefaultCompletionProvider completionProvider = createCompletionProvider();
    private static final List<String> SQL_KEYWORDS = List.of(
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "NULL", "IS NULL", "IS NOT NULL", "IN", "LIKE", "BETWEEN",
            "EXISTS", "DISTINCT", "AS", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "ON",
            "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "UNION", "UNION ALL", "CASE", "WHEN", "THEN",
            "ELSE", "END", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", "CREATE TABLE", "ALTER TABLE",
            "DROP TABLE", "CREATE INDEX", "PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "COUNT(*)", "SUM(", "AVG(",
            "MIN(", "MAX(", "COALESCE(", "ASC", "DESC", "TRUNCATE TABLE", "WITH");

    public SqlPanel() {
        this(PluginStorage.noop());
    }

    public SqlPanel(PluginStorage storage) {
        this.storage = storage == null ? PluginStorage.noop() : storage;
        initUI();
        refreshCompletions();
        loadProfiles(null);
    }

    private void initUI() {
        SqlWorkbench.applyRoot(this);
        add(createConnectionPanel(), BorderLayout.NORTH);
        add(createWorkbench(), BorderLayout.CENTER);
        status.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        add(SqlWorkbench.statusBar(status), BorderLayout.SOUTH);
        updateConnectionControls(false);
        databaseType.addActionListener(event -> applyPreset());
        applyPreset();
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, novisualpadding, fillx, wrap",
                "[pref!][grow,fill][pref!][grow,fill][pref!][grow,fill][pref!][grow,fill]",
                "[]4[]4[]4[]4[]"));
        panel.setOpaque(false);

        profiles.setRenderer((list, value, index, selected, focus) ->
                new javax.swing.DefaultListCellRenderer().getListCellRendererComponent(
                        list, value == null ? "" : value.name(), index, selected, focus));
        profiles.addActionListener(event -> {
            if (!loadingProfiles && profiles.getSelectedItem() instanceof ConnectionProfile profile) applyProfile(profile);
        });
        newProfileButton = new JButton(SqlI18n.t("profile_new"));
        saveProfileButton = new JButton(SqlI18n.t("profile_save"));
        copyProfileButton = new JButton(SqlI18n.t("profile_copy"));
        deleteProfileButton = new JButton(SqlI18n.t("profile_delete"));
        newProfileButton.addActionListener(event -> clearProfileForm());
        saveProfileButton.addActionListener(event -> saveProfile());
        copyProfileButton.addActionListener(event -> copyProfile());
        deleteProfileButton.addActionListener(event -> deleteProfile());
        panel.add(label("profile"));
        panel.add(profiles, "span 3,growx,wmin 120");
        panel.add(newProfileButton, "split 4");
        panel.add(saveProfileButton);
        panel.add(copyProfileButton);
        panel.add(deleteProfileButton, "wrap");

        connectButton = new JButton(SqlI18n.t("connect"));
        disconnectButton = new JButton(SqlI18n.t("disconnect"));
        browseDriverButton = new JButton(SqlI18n.t("browse"));
        connectButton.addActionListener(event -> connect());
        disconnectButton.addActionListener(event -> disconnect());
        browseDriverButton.addActionListener(event -> chooseDriverJar());
        jdbcUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                SqlI18n.t("url_hint"));
        driverClass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                SqlI18n.t("driver_hint"));
        driverJar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                SqlI18n.t("driver_jar_hint"));

        panel.add(label("type"));
        panel.add(databaseType, "wmin 120");
        panel.add(label("url"));
        panel.add(jdbcUrl, "span 5, growx, wmin 240, wrap");
        panel.add(label("driver_class"));
        panel.add(driverClass, "span 7, growx, wrap");
        panel.add(label("driver_jar"));
        panel.add(driverJar, "span 5, growx");
        panel.add(browseDriverButton, "span 2, wrap");
        panel.add(label("username"));
        panel.add(username, "wmin 100");
        panel.add(label("password"));
        panel.add(password, "wmin 100");
        panel.add(connectButton, "split 2");
        panel.add(disconnectButton, "wrap");
        JLabel autocommitHint = new JLabel(SqlI18n.t("autocommit_hint"));
        autocommitHint.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        panel.add(autocommitHint, "span 8, wrap");
        return panel;
    }

    private JPanel createWorkbench() {
        JPanel browser = new JPanel(new BorderLayout(0, 4));
        browser.setOpaque(false);
        // Title on its own line: sharing the narrow navigator width with Refresh cut it to "Database Navigat...".
        JPanel browserHeader = new JPanel(new MigLayout("insets 0, novisualpadding, wrap 1", "[]", "[]4[]"));
        browserHeader.setOpaque(false);
        browserHeader.add(SqlWorkbench.sectionTitle(SqlI18n.t("objects")));
        refreshObjectsButton = new JButton(SqlI18n.t("refresh"));
        refreshObjectsButton.addActionListener(event -> loadMetadata());
        browserHeader.add(refreshObjectsButton);
        browser.add(browserHeader, BorderLayout.NORTH);
        objectTree.setRootVisible(false);
        objectTree.setShowsRootHandles(true);
        objectTree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                                   boolean expanded, boolean leaf, int row, boolean focus) {
                java.awt.Component component = super.getTreeCellRendererComponent(
                        tree, value, selected, expanded, leaf, row, focus);
                boolean isDefault = value instanceof DefaultMutableTreeNode node
                        && node.getUserObject() instanceof MetadataScope scope && scope.equals(defaultScope);
                component.setFont(component.getFont().deriveFont(isDefault ? Font.BOLD : Font.PLAIN));
                return component;
            }
        });
        objectTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object value = ((DefaultMutableTreeNode) event.getPath().getLastPathComponent()).getUserObject();
                if (value instanceof MetadataScope) loadTables((DefaultMutableTreeNode) event.getPath().getLastPathComponent());
                if (value instanceof TableRef) loadColumns((DefaultMutableTreeNode) event.getPath().getLastPathComponent());
            }

            @Override public void treeWillCollapse(TreeExpansionEvent event) { }
        });
        objectTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (javax.swing.SwingUtilities.isRightMouseButton(event)) {
                    int row = objectTree.getClosestRowForLocation(event.getX(), event.getY());
                    objectTree.setSelectionRow(row);
                    showNavigatorMenu(event.getX(), event.getY());
                    return;
                }
                if (event.getClickCount() == 2) {
                    Object selected = objectTree.getLastSelectedPathComponent();
                    if (selected instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TableRef table) {
                        openTableQuery(table);
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(objectTree);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(treeScroll);
        browser.add(treeScroll, BorderLayout.CENTER);

        JPanel editor = new JPanel(new BorderLayout(0, 4));
        editor.setOpaque(false);
        JToolBar editorToolbar = new JToolBar();
        editorToolbar.setFloatable(false);
        editorToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        JButton newSqlTab = new JButton(SqlI18n.t("new_sql_tab"));
        newSqlTab.addActionListener(event -> addSqlTab(null, true));
        JButton closeSqlTab = new JButton(SqlI18n.t("close_sql_tab"));
        closeSqlTab.addActionListener(event -> closeCurrentSqlTab());
        editorToolbar.add(newSqlTab);
        editorToolbar.add(closeSqlTab);
        editorToolbar.addSeparator();
        suggestions.setEditable(false);
        suggestions.setMaximumSize(new java.awt.Dimension(260, 30));
        editorToolbar.add(suggestions);
        JButton insertSuggestion = new JButton(SqlI18n.t("insert"));
        insertSuggestion.addActionListener(event -> insertSelectedSuggestion());
        executeButton = new JButton(SqlI18n.t("execute"));
        executeButton.addActionListener(event -> executeSql());
        editorToolbar.add(insertSuggestion);
        editorToolbar.addSeparator();
        editorToolbar.add(executeButton);
        editor.add(editorToolbar, BorderLayout.NORTH);

        addSqlTab(null, false);
        JPanel sqlSection = SqlWorkbench.editorSection(SqlI18n.t("sql"), sqlTabs);
        JScrollPane resultScroll = new JScrollPane(resultTable);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(resultScroll);
        outputLog.setEditable(false);
        outputLog.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        outputLog.setRows(4);
        resultTabs.addTab(SqlI18n.t("data"), resultScroll);
        resultTabs.addTab(SqlI18n.t("output"), new JScrollPane(outputLog));
        JPanel resultSection = SqlWorkbench.editorSection(SqlI18n.t("results"), resultTabs);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setCellSelectionEnabled(true);
        JSplitPane verticalSplit = SqlWorkbench.editorSplit(sqlSection, resultSection, 180);
        verticalSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.45);
        editor.add(verticalSplit, BorderLayout.CENTER);

        JSplitPane split = SqlWorkbench.horizontalSplit(browser, editor, 220);
        split.setResizeWeight(0.23);
        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);
        host.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        host.add(split, BorderLayout.CENTER);
        return host;
    }

    private RSyntaxTextArea createSqlEditor() {
        RSyntaxTextArea editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editor.setCodeFoldingEnabled(true);
        editor.setTabSize(4);
        editor.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        EditorThemeUtil.loadTheme(editor);
        javax.swing.KeyStroke runKey = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        editor.getInputMap().put(runKey, "run-current-sql");
        editor.getActionMap().put("run-current-sql", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { executeSql(); }
        });
        // In-editor completion: pops up while typing (and on Ctrl+Space) with SQL keywords, tables and columns.
        org.fife.ui.autocomplete.AutoCompletion completion = new org.fife.ui.autocomplete.AutoCompletion(completionProvider);
        completion.setAutoActivationEnabled(true);
        completion.setAutoActivationDelay(300);
        completion.setShowDescWindow(false);
        completion.setTriggerKey(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
        completion.install(editor);
        return editor;
    }

    private static org.fife.ui.autocomplete.DefaultCompletionProvider createCompletionProvider() {
        org.fife.ui.autocomplete.DefaultCompletionProvider provider = new org.fife.ui.autocomplete.DefaultCompletionProvider() {
            @Override
            protected boolean isValidChar(char ch) {
                // Allow "schema.table" and quoted identifiers to be completed as one word.
                return super.isValidChar(ch) || ch == '.' || ch == '"' || ch == '`';
            }
        };
        provider.setAutoActivationRules(true, ".");
        return provider;
    }

    /** Rebuilds the editor completion list from SQL keywords and the metadata loaded so far. */
    private void refreshCompletions() {
        java.util.Set<String> words = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        words.addAll(SQL_KEYWORDS);
        for (String suggestion : metadataSuggestions) {
            if (suggestion != null && !suggestion.isBlank()) words.add(suggestion.trim());
        }
        completionProvider.clear();
        for (String word : words) {
            completionProvider.addCompletion(new org.fife.ui.autocomplete.BasicCompletion(completionProvider, word));
        }
    }

    /** Completion candidates for a typed prefix, as offered by the editor popup. */
    List<String> completionsFor(String prefix) {
        String typed = prefix == null ? "" : prefix;
        javax.swing.JTextArea area = new javax.swing.JTextArea(typed);
        area.setCaretPosition(typed.length());
        List<String> result = new ArrayList<>();
        for (org.fife.ui.autocomplete.Completion completion : completionProvider.getCompletions(area)) {
            result.add(completion.getReplacementText());
        }
        return result;
    }

    private void addSqlTab(String sql, boolean focus) {
        RSyntaxTextArea editor = createSqlEditor();
        if (sql != null) editor.setText(sql);
        JScrollPane scroll = new JScrollPane(editor);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scroll);
        String title = SqlI18n.t("sql_console", sqlTabSequence++);
        sqlTabs.addTab(title, scroll);
        sqlTabs.setSelectedComponent(scroll);
        if (focus) editor.requestFocusInWindow();
    }

    private void closeCurrentSqlTab() {
        if (sqlTabs.getTabCount() <= 1) {
            activeSqlEditor().setText("");
            return;
        }
        sqlTabs.removeTabAt(sqlTabs.getSelectedIndex());
    }

    private RSyntaxTextArea activeSqlEditor() {
        JScrollPane scroll = (JScrollPane) sqlTabs.getSelectedComponent();
        return (RSyntaxTextArea) scroll.getViewport().getView();
    }

    private void openTableQuery(TableRef table) {
        addSqlTab("SELECT * FROM " + table.qualifiedName() + ";", true);
    }

    private void showNavigatorMenu(int x, int y) {
        Object selected = objectTree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode node)) return;
        JPopupMenu menu = new JPopupMenu();
        if (node.getUserObject() instanceof MetadataScope scope) {
            JMenuItem setDefault = new JMenuItem(SqlI18n.t("set_default_schema"));
            setDefault.setEnabled(connection != null && !scope.equals(defaultScope));
            setDefault.addActionListener(event -> setDefaultScope(node));
            menu.add(setDefault);
            JMenuItem refresh = new JMenuItem(SqlI18n.t("refresh"));
            refresh.addActionListener(event -> {
                restorePendingNode(node);
                objectTree.expandPath(new TreePath(node.getPath()));
            });
            menu.add(refresh);
        }
        if (node.getUserObject() instanceof TableRef table) {
            JMenuItem viewData = new JMenuItem(SqlI18n.t("view_data"));
            viewData.addActionListener(event -> openTableQuery(table));
            menu.add(viewData);
            JMenuItem viewColumns = new JMenuItem(SqlI18n.t("view_columns"));
            viewColumns.addActionListener(event -> showColumnDetails(table));
            menu.add(viewColumns);
            JMenuItem copyName = new JMenuItem(SqlI18n.t("copy_qualified_name"));
            copyName.addActionListener(event -> copyText(table.qualifiedName()));
            menu.add(copyName);
        }
        if (menu.getComponentCount() > 0) menu.show(objectTree, x, y);
    }

    private void restorePendingNode(DefaultMutableTreeNode node) {
        node.removeAllChildren();
        node.add(new DefaultMutableTreeNode(SqlI18n.t("expand_to_load")));
        ((DefaultTreeModel) objectTree.getModel()).reload(node);
    }

    private void copyText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        setStatus(SqlI18n.t("copied", text));
    }

    private JLabel label(String key) {
        return new JLabel(SqlI18n.t(key) + ":");
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

    private ConnectionProfile currentProfile(String profileName) {
        DatabasePreset preset = (DatabasePreset) databaseType.getSelectedItem();
        return new ConnectionProfile(profileName, preset == null ? DatabasePreset.GENERIC.name() : preset.name(),
                jdbcUrl.getText().trim(), driverClass.getText().trim(), driverJar.getText().trim(),
                username.getText().trim(), new String(password.getPassword()));
    }

    private void applyProfile(ConnectionProfile profile) {
        if (connection != null) disconnect();
        DatabasePreset preset;
        try { preset = DatabasePreset.valueOf(profile.databaseType()); }
        catch (Exception ignored) { preset = DatabasePreset.GENERIC; }
        databaseType.setSelectedItem(preset);
        jdbcUrl.setText(profile.jdbcUrl());
        driverClass.setText(profile.driverClass());
        driverJar.setText(profile.driverJar());
        username.setText(profile.username());
        password.setText(profile.password());
        try { storage.writeString(ACTIVE_PROFILE_FILE, encode(profile.name())); }
        catch (Exception exception) { log.warn("Could not persist active SQL profile", exception); }
    }

    private void clearProfileForm() {
        if (connection != null) disconnect();
        profiles.setSelectedItem(null);
        databaseType.setSelectedItem(DatabasePreset.GENERIC);
        applyPreset();
        jdbcUrl.setText(DatabasePreset.GENERIC.exampleUrl());
        driverClass.setText("");
        driverJar.setText("");
        username.setText("");
        password.setText("");
    }

    private void saveProfile() {
        ConnectionProfile selected = (ConnectionProfile) profiles.getSelectedItem();
        String profileName = selected == null ? "" : selected.name();
        if (profileName.isBlank()) {
            profileName = JOptionPane.showInputDialog(this, SqlI18n.t("profile_name"),
                    "SQL - " + jdbcUrl.getText().trim());
            if (profileName == null || profileName.isBlank()) return;
            profileName = profileName.trim();
            if (findProfile(profileName) != null) {
                setStatus(SqlI18n.t("profile_exists", profileName));
                return;
            }
        }
        List<ConnectionProfile> all = readProfiles();
        ConnectionProfile updated = currentProfile(profileName);
        int index = indexOfProfile(all, profileName);
        if (index >= 0) all.set(index, updated); else all.add(updated);
        writeProfiles(all);
        loadProfiles(profileName);
        setStatus(SqlI18n.t("profile_saved", profileName));
    }

    private void copyProfile() {
        String base = profiles.getSelectedItem() instanceof ConnectionProfile selected
                ? selected.name() + " copy" : "SQL profile copy";
        String profileName = JOptionPane.showInputDialog(this, SqlI18n.t("profile_copy_prompt"), base);
        if (profileName == null || profileName.isBlank()) return;
        profileName = profileName.trim();
        if (findProfile(profileName) != null) {
            setStatus(SqlI18n.t("profile_exists", profileName));
            return;
        }
        List<ConnectionProfile> all = readProfiles();
        all.add(currentProfile(profileName));
        writeProfiles(all);
        loadProfiles(profileName);
        setStatus(SqlI18n.t("profile_saved", profileName));
    }

    private void deleteProfile() {
        if (!(profiles.getSelectedItem() instanceof ConnectionProfile selected)) return;
        if (JOptionPane.showConfirmDialog(this, SqlI18n.t("profile_delete_confirm", selected.name()),
                SqlI18n.t("profile_delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        List<ConnectionProfile> all = readProfiles();
        all.removeIf(profile -> profile.name().equals(selected.name()));
        writeProfiles(all);
        loadProfiles(all.isEmpty() ? null : all.get(0).name());
        setStatus(SqlI18n.t("profile_deleted", selected.name()));
    }

    private ConnectionProfile findProfile(String name) {
        return readProfiles().stream().filter(profile -> profile.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private int indexOfProfile(List<ConnectionProfile> all, String name) {
        for (int i = 0; i < all.size(); i++) if (all.get(i).name().equalsIgnoreCase(name)) return i;
        return -1;
    }

    private void loadProfiles(String preferredName) {
        loadingProfiles = true;
        profiles.removeAllItems();
        List<ConnectionProfile> all = readProfiles();
        all.forEach(profiles::addItem);
        String activeName = preferredName;
        if (activeName == null) activeName = readActiveProfileName();
        String selectedName = activeName;
        ConnectionProfile selected = all.stream().filter(profile -> profile.name().equals(selectedName)).findFirst()
                .orElse(all.isEmpty() ? null : all.get(0));
        profiles.setSelectedItem(selected);
        loadingProfiles = false;
        if (selected != null) applyProfile(selected);
        deleteProfileButton.setEnabled(selected != null);
        if (selected == null) clearProfileForm();
    }

    private List<ConnectionProfile> readProfiles() {
        List<ConnectionProfile> all = new ArrayList<>();
        try {
            for (String line : storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 7) continue;
                all.add(new ConnectionProfile(decode(fields[0]), decode(fields[1]), decode(fields[2]),
                        decode(fields[3]), decode(fields[4]), decode(fields[5]), decode(fields[6])));
            }
        } catch (Exception exception) { log.warn("Could not read SQL connection profiles", exception); }
        return all;
    }

    private void writeProfiles(List<ConnectionProfile> all) {
        try {
            StringBuilder content = new StringBuilder();
            for (ConnectionProfile profile : all) {
                content.append(encode(profile.name())).append('\t').append(encode(profile.databaseType())).append('\t')
                        .append(encode(profile.jdbcUrl())).append('\t').append(encode(profile.driverClass())).append('\t')
                        .append(encode(profile.driverJar())).append('\t').append(encode(profile.username())).append('\t')
                        .append(encode(profile.password())).append('\n');
            }
            storage.writeString(PROFILE_FILE, content.toString());
        } catch (Exception exception) { log.warn("Could not save SQL connection profiles", exception); }
    }

    private String readActiveProfileName() {
        try { return storage.readString(ACTIVE_PROFILE_FILE).map(SqlPanel::decode).orElse(""); }
        catch (Exception exception) { log.warn("Could not read active SQL profile", exception); return ""; }
    }

    private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static String decode(String value) { return new String(Base64.getUrlDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8); }
    private record ConnectionProfile(String name, String databaseType, String jdbcUrl, String driverClass,
                                     String driverJar, String username, String password) {
        @Override public String toString() { return name; }
    }

    private void chooseDriverJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                SqlI18n.t("driver_jar"), "jar"));
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
            setStatus(SqlI18n.t("invalid_url"));
            return;
        }
        setStatus(SqlI18n.t("connecting"));
        connectButton.setEnabled(false);
        setConnectionFieldsEnabled(false);
        String jar = driverJar.getText().trim();
        String driverClassName = driverClass.getText().trim();
        String user = username.getText().trim();
        char[] secret = password.getPassword();
        new SwingWorker<Connection, Void>() {
            @Override protected Connection doInBackground() throws Exception {
                URLClassLoader loader = null;
                if (!jar.isBlank()) {
                    URL[] jars = java.util.Arrays.stream(jar.split(";"))
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .map(Path::of)
                            .peek(path -> {
                                if (!path.toFile().isFile()) {
                                    throw new IllegalArgumentException(SqlI18n.t("driver_jar_missing", path.getFileName()));
                                }
                            })
                            .map(path -> {
                                try { return path.toUri().toURL(); }
                                catch (Exception exception) {
                                    throw new IllegalArgumentException(SqlI18n.t("driver_jar_invalid"));
                                }
                            })
                            .toArray(URL[]::new);
                    loader = new URLClassLoader(jars, SqlPanel.class.getClassLoader());
                }
                try {
                    Driver driver = loadDriver(loader, driverClassName, url);
                    Properties properties = new Properties();
                    if (!user.isBlank()) properties.setProperty("user", user);
                    if (secret.length > 0) properties.setProperty("password", new String(secret));
                    if (url.startsWith("jdbc:oracle:")) {
                        // Without it ojdbc's getColumns returns nothing for synonyms.
                        properties.putIfAbsent("includeSynonyms", "true");
                    }
                    Connection opened = driver == null
                            ? DriverManager.getConnection(url, properties)
                            : driver.connect(url, properties);
                    if (opened == null) throw new IllegalArgumentException(SqlI18n.t("driver_rejected_url"));
                    try {
                        opened.setNetworkTimeout(ForkJoinPool.commonPool(), METADATA_TIMEOUT_MILLIS);
                    } catch (Exception | AbstractMethodError unsupported) {
                        log.debug("JDBC driver does not support connection network timeout", unsupported);
                    }
                    synchronized (SqlPanel.this) {
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
                    updateConnectionControls(true);
                    setStatus(SqlI18n.t("connected"));
                    loadMetadata();
                } catch (Exception exception) {
                    updateConnectionControls(false);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    log.warn("JDBC connection failed: {}", cause.getClass().getSimpleName());
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        }.execute();
    }

    private Driver loadDriver(ClassLoader loader, String className, String url) throws Exception {
        if (!className.isBlank()) {
            ClassLoader effectiveLoader = loader == null ? SqlPanel.class.getClassLoader() : loader;
            Class<?> driverType;
            try {
                driverType = Class.forName(className, true, effectiveLoader);
            } catch (ClassNotFoundException missing) {
                throw new IllegalArgumentException(SqlI18n.t("driver_class_not_found", className), missing);
            }
            return (Driver) driverType.getDeclaredConstructor().newInstance();
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
        cancelMetadataLoad();
        long sequence = ++metadataLoadSequence;
        executeButton.setEnabled(false);
        refreshObjectsButton.setEnabled(false);
        setStatus(SqlI18n.t("loading_objects"));
        SwingWorker<MetadataSnapshot, Void> worker = new SwingWorker<>() {
            @Override protected MetadataSnapshot doInBackground() throws Exception {
                return readMetadata(active);
            }

            @Override protected void done() {
                if (sequence != metadataLoadSequence || connection != active
                        || timedOutMetadataSequence == sequence) return;
                stopMetadataTimeout();
                metadataWorker = null;
                try {
                    MetadataSnapshot snapshot = get();
                    executeButton.setEnabled(true);
                    refreshObjectsButton.setEnabled(true);
                    objectTree.setModel(new DefaultTreeModel(snapshot.root()));
                    metadataSuggestions = new ArrayList<>(snapshot.suggestions());
                    refreshSuggestions();
                    setStatus(SqlI18n.t("scopes_loaded", snapshot.schemaCount()));
                    // Show the tables users work with straight away: mark the connection's current schema as the
                    // default and expand it (system schemas such as pg_catalog stay collapsed).
                    DefaultMutableTreeNode current = findScopeNode(snapshot.root(), snapshot.currentCatalog(),
                            snapshot.currentSchema());
                    if (current != null) {
                        defaultScope = (MetadataScope) current.getUserObject();
                        objectTree.expandPath(new TreePath(current.getPath()));
                        objectTree.scrollPathToVisible(new TreePath(current.getPath()));
                    }
                } catch (Exception exception) {
                    executeButton.setEnabled(true);
                    refreshObjectsButton.setEnabled(true);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        };
        metadataWorker = worker;
        metadataTimeoutTimer = new Timer(METADATA_TIMEOUT_MILLIS, event -> {
            if (sequence != metadataLoadSequence || connection != active) return;
            timedOutMetadataSequence = sequence;
            worker.cancel(true);
            metadataWorker = null;
            executeButton.setEnabled(true);
            refreshObjectsButton.setEnabled(true);
            setStatus(SqlI18n.t("metadata_timeout", METADATA_TIMEOUT_SECONDS));
        });
        metadataTimeoutTimer.setRepeats(false);
        metadataTimeoutTimer.start();
        worker.execute();
    }

    private void cancelMetadataLoad() {
        stopMetadataTimeout();
        if (metadataWorker != null && !metadataWorker.isDone()) {
            metadataWorker.cancel(true);
        }
        metadataWorker = null;
    }

    private void stopMetadataTimeout() {
        if (metadataTimeoutTimer != null) {
            metadataTimeoutTimer.stop();
            metadataTimeoutTimer = null;
        }
    }

    private MetadataSnapshot readMetadata(Connection active) throws Exception {
        DatabaseMetaData metadata = active.getMetaData();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(metadata.getDatabaseProductName());
        List<String> completion = new ArrayList<>(SQL_SUGGESTIONS);
        int schemaCount = 0;
        List<String[]> schemas = new ArrayList<>();
        try (ResultSet result = metadata.getSchemas()) {
            while (result.next()) {
                String catalog;
                try { catalog = result.getString("TABLE_CATALOG"); }
                catch (Exception ignored) { catalog = null; }
                schemas.add(new String[]{catalog, result.getString("TABLE_SCHEM")});
            }
        } catch (Exception ignored) { }
        if (schemas.isEmpty()) {
            try (ResultSet catalogs = metadata.getCatalogs()) {
                while (catalogs.next()) {
                    String catalog = catalogs.getString("TABLE_CAT");
                    root.add(lazyNode(new MetadataScope(catalog, null, catalog)));
                    schemaCount++;
                }
            } catch (Exception ignored) { }
            if (root.getChildCount() == 0) {
                root.add(lazyNode(new MetadataScope(null, null, SqlI18n.t("default_scope"))));
                schemaCount = 1;
            }
        } else {
            for (String[] schema : schemas) {
                String catalog = schema[0];
                String schemaName = schema[1];
                String displayName = catalog == null || catalog.isBlank() ? schemaName : catalog + "." + schemaName;
                root.add(lazyNode(new MetadataScope(catalog, schemaName, displayName)));
                schemaCount++;
            }
        }
        String currentCatalog = null;
        String currentSchema = null;
        try { currentCatalog = active.getCatalog(); } catch (Exception | AbstractMethodError ignored) { }
        currentSchema = currentSchemaOf(active);
        return new MetadataSnapshot(root, completion.stream().distinct().toList(), schemaCount,
                currentCatalog, currentSchema);
    }

    private DefaultMutableTreeNode lazyNode(Object value) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(value);
        node.add(new DefaultMutableTreeNode(SqlI18n.t("expand_to_load")));
        return node;
    }

    private void loadTables(DefaultMutableTreeNode scopeNode) {
        if (!isPendingNode(scopeNode) || connection == null) return;
        MetadataScope scope = (MetadataScope) scopeNode.getUserObject();
        markLoading(scopeNode);
        Connection active = connection;
        setStatus(SqlI18n.t("loading_tables", scope.displayName()));
        new SwingWorker<List<DefaultMutableTreeNode>, Void>() {
            @Override protected List<DefaultMutableTreeNode> doInBackground() throws Exception {
                return readTables(active.getMetaData(), scope);
            }

            @Override protected void done() {
                if (connection != active) return;
                try {
                    List<DefaultMutableTreeNode> tables = get();
                    scopeNode.removeAllChildren();
                    tables.forEach(scopeNode::add);
                    tables.stream()
                            .map(DefaultMutableTreeNode::getUserObject)
                            .filter(TableRef.class::isInstance)
                            .map(TableRef.class::cast)
                            .forEach(ref -> {
                                metadataSuggestions.add(ref.table());
                                metadataSuggestions.add(ref.qualifiedName());
                            });
                    ((DefaultTreeModel) objectTree.getModel()).reload(scopeNode);
                    setStatus(SqlI18n.t("tables_loaded", tables.size(), scope.displayName()));
                    refreshSuggestions();
                } catch (Exception exception) {
                    restorePending(scopeNode);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        }.execute();
    }

    private List<DefaultMutableTreeNode> readTables(DatabaseMetaData metadata, MetadataScope scope) throws Exception {
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        String identifierQuote;
        try {
            identifierQuote = metadata.getIdentifierQuoteString();
        } catch (Exception ignored) {
            identifierQuote = "";
        }
        String escape = searchStringEscape(metadata);
        try (ResultSet tables = metadata.getTables(scope.catalog(), escapePattern(scope.schema(), escape), null,
                browsableTableTypes(metadata))) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                TableRef ref = new TableRef(scope.catalog(), scope.schema(), table, identifierQuote);
                nodes.add(lazyNode(ref));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(new DefaultMutableTreeNode(SqlI18n.t("no_tables")));
        }
        return nodes;
    }

    /**
     * Object types shown in the navigator. Oracle application users usually reach tables through SYNONYMs, so limiting
     * the list to TABLE/VIEW made their schema look empty; materialized/partitioned tables were missing as well.
     */
    static String[] browsableTableTypes(DatabaseMetaData metadata) {
        java.util.Set<String> wanted = java.util.Set.of("TABLE", "BASE TABLE", "VIEW", "SYNONYM", "ALIAS",
                "MATERIALIZED VIEW", "PARTITIONED TABLE", "FOREIGN TABLE");
        List<String> types = new ArrayList<>();
        try (ResultSet result = metadata.getTableTypes()) {
            while (result.next()) {
                String type = result.getString("TABLE_TYPE");
                if (type != null && wanted.contains(type.trim().toUpperCase(java.util.Locale.ROOT))) {
                    types.add(type.trim());
                }
            }
        } catch (Exception | AbstractMethodError ignored) {
            return new String[]{"TABLE", "VIEW"};
        }
        return types.isEmpty() ? new String[]{"TABLE", "VIEW"} : types.toArray(String[]::new);
    }

    /** JDBC metadata name arguments are LIKE patterns: escape "_" and "%" so ORDER_ITEMS does not match ORDERXITEMS. */
    static String escapePattern(String name, String escape) {
        if (name == null || escape == null || escape.isEmpty()) {
            return name;
        }
        return name.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
    }

    private static String searchStringEscape(DatabaseMetaData metadata) {
        try {
            return metadata.getSearchStringEscape();
        } catch (Exception | AbstractMethodError ignored) {
            return null;
        }
    }

    private void loadColumns(DefaultMutableTreeNode tableNode) {
        if (!isPendingNode(tableNode) || connection == null) return;
        TableRef table = (TableRef) tableNode.getUserObject();
        markLoading(tableNode);
        Connection active = connection;
        setStatus(SqlI18n.t("loading_columns", table.table()));
        new SwingWorker<List<ColumnInfo>, Void>() {
            @Override protected List<ColumnInfo> doInBackground() throws Exception {
                List<ColumnInfo> columns = new ArrayList<>();
                for (ColumnDetail detail : readColumnDetails(active.getMetaData(), table)) {
                    columns.add(new ColumnInfo(detail.name(), detail.summary()));
                }
                return columns;
            }

            @Override protected void done() {
                if (connection != active) return;
                try {
                    List<ColumnInfo> columns = get();
                    tableNode.removeAllChildren();
                    columns.forEach(column -> {
                        tableNode.add(new DefaultMutableTreeNode(column.displayName()));
                        metadataSuggestions.add(column.name());
                    });
                    if (columns.isEmpty()) {
                        tableNode.add(new DefaultMutableTreeNode(SqlI18n.t("no_columns")));
                    }
                    ((DefaultTreeModel) objectTree.getModel()).reload(tableNode);
                    setStatus(SqlI18n.t("columns_loaded", columns.size(), table.table()));
                    refreshSuggestions();
                } catch (Exception exception) {
                    restorePending(tableNode);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        }.execute();
    }

    private boolean isPendingNode(DefaultMutableTreeNode node) {
        return node.getChildCount() == 1 && SqlI18n.t("expand_to_load").equals(
                ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject());
    }

    private void markLoading(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(0);
        child.setUserObject(SqlI18n.t("loading"));
        ((DefaultTreeModel) objectTree.getModel()).nodeChanged(child);
    }

    private void restorePending(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(0);
        child.setUserObject(SqlI18n.t("expand_to_load"));
        ((DefaultTreeModel) objectTree.getModel()).nodeChanged(child);
    }

    private void refreshSuggestions() {
        Object selected = suggestions.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(metadataSuggestions.toArray(String[]::new));
        suggestions.setModel(model);
        if (selected != null) suggestions.setSelectedItem(selected);
        refreshCompletions();
    }

    /** Scope node matching the connection's current schema, or its catalog for catalog-only databases (MySQL). */
    private DefaultMutableTreeNode findScopeNode(DefaultMutableTreeNode root, String catalog, String schema) {
        DefaultMutableTreeNode catalogMatch = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            if (!(child.getUserObject() instanceof MetadataScope scope)) continue;
            if (schema != null && scope.schema() != null && scope.schema().equalsIgnoreCase(schema)
                    && (catalog == null || scope.catalog() == null || scope.catalog().equalsIgnoreCase(catalog))) {
                return child;
            }
            if (scope.schema() == null && catalog != null && scope.catalog() != null
                    && scope.catalog().equalsIgnoreCase(catalog)) {
                catalogMatch = child;
            }
        }
        if (catalogMatch == null && root.getChildCount() == 1
                && ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject() instanceof MetadataScope) {
            // Schemaless databases (SQLite...) expose a single default scope; it is the one being worked in.
            return (DefaultMutableTreeNode) root.getChildAt(0);
        }
        return catalogMatch;
    }

    /**
     * Sets the session's default schema. Drivers predating JDBC 4.1 (e.g. ojdbc6/7 for Oracle 11g) lack
     * Connection.setSchema; Oracle then needs ALTER SESSION SET CURRENT_SCHEMA.
     */
    static void applyDefaultSchema(Connection connection, String schema) throws java.sql.SQLException {
        try {
            connection.setSchema(schema);
            return;
        } catch (SQLFeatureNotSupportedException | AbstractMethodError | UnsupportedOperationException unsupported) {
            if (!isOracle(connection)) throw unsupported instanceof java.sql.SQLException sql ? sql
                    : new SQLFeatureNotSupportedException("Connection.setSchema is not supported by this driver", unsupported);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER SESSION SET CURRENT_SCHEMA = \"" + schema.replace("\"", "\"\"") + "\"");
        }
    }

    /** Current schema of the session; for Oracle drivers without getSchema it is the login user. */
    static String currentSchemaOf(Connection connection) {
        try {
            String schema = connection.getSchema();
            if (schema != null && !schema.isBlank()) return schema;
        } catch (Exception | AbstractMethodError ignored) {
            // fall through to the Oracle default
        }
        try {
            return isOracle(connection) ? connection.getMetaData().getUserName() : null;
        } catch (Exception | AbstractMethodError ignored) {
            return null;
        }
    }

    private static boolean isOracle(Connection connection) {
        try {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(java.util.Locale.ROOT).contains("oracle");
        } catch (Exception | AbstractMethodError ignored) {
            return false;
        }
    }

    /** Makes unqualified SQL run against the selected schema (or catalog) and expands it. */
    private void setDefaultScope(DefaultMutableTreeNode node) {
        Connection active = connection;
        if (active == null || !(node.getUserObject() instanceof MetadataScope scope)) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (scope.schema() != null) {
                    applyDefaultSchema(active, scope.schema());
                } else if (scope.catalog() != null) {
                    active.setCatalog(scope.catalog());
                }
                return null;
            }

            @Override protected void done() {
                if (connection != active) return;
                try {
                    get();
                    defaultScope = scope;
                    objectTree.repaint();
                    objectTree.expandPath(new TreePath(node.getPath()));
                    setStatus(SqlI18n.t("default_schema_set", scope.displayName()));
                    appendOutput(SqlI18n.t("default_schema_set", scope.displayName()));
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        }.execute();
    }

    private void insertSelectedSuggestion() {
        Object selected = suggestions.getSelectedItem();
        if (selected == null) return;
        RSyntaxTextArea editor = activeSqlEditor();
        editor.insert(selected.toString(), editor.getCaretPosition());
        editor.requestFocusInWindow();
    }

    private void executeSql() {
        Connection active = connection;
        // Ctrl+Enter bypasses the button, so honour its state to avoid concurrent use of one JDBC connection.
        if (active == null || !executeButton.isEnabled()) return;
        String sql = executableSql(currentSqlStatement(activeSqlEditor()));
        if (sql.isBlank()) return;
        executeButton.setEnabled(false);
        setStatus(SqlI18n.t("executing"));
        appendOutput(SqlI18n.t("output_running", compactSql(sql)));
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
                        resultTabs.setSelectedIndex(0);
                        setStatus(SqlI18n.t("query_complete", result.count(), MAX_RESULT_ROWS));
                        appendOutput(SqlI18n.t("output_rows", result.count()));
                    } else if (result.count() < 0) {
                        setStatus(SqlI18n.t("statement_complete"));
                        appendOutput(SqlI18n.t("statement_complete"));
                    } else {
                        setStatus(SqlI18n.t("update_complete", result.count()));
                        appendOutput(SqlI18n.t("output_updated", result.count()));
                    }
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                    appendOutput(SqlI18n.t("error", safeMessage(cause)));
                    resultTabs.setSelectedIndex(1);
                }
            }
        }.execute();
    }

    private String currentSqlStatement(RSyntaxTextArea editor) {
        String selected = editor.getSelectedText();
        if (selected != null && !selected.isBlank()) return selected.trim();
        return statementAt(editor.getText(), editor.getCaretPosition());
    }

    /**
     * Returns the semicolon-delimited statement around {@code caret}. When the caret sits after a statement's
     * terminating semicolon (optionally followed by whitespace) and nothing else follows, that statement is used.
     */
    static String statementAt(String text, int caret) {
        if (text == null || text.isBlank()) return "";
        int position = Math.max(0, Math.min(caret, text.length()));
        int anchor = position;
        while (anchor > 0 && (text.charAt(anchor - 1) == ' ' || text.charAt(anchor - 1) == '\t')) anchor--;
        if (anchor > 0 && text.charAt(anchor - 1) == ';') return segmentAt(text, anchor - 1);
        String statement = segmentAt(text, position);
        if (!statement.isBlank()) return statement;
        while (anchor > 0 && Character.isWhitespace(text.charAt(anchor - 1))) anchor--;
        if (anchor > 0 && text.charAt(anchor - 1) == ';') anchor--;
        return anchor == position ? statement : segmentAt(text, anchor);
    }

    /**
     * Drops the statement terminator, which drivers such as Oracle reject, while keeping it for procedural blocks
     * ending in {@code END;} where it is part of the syntax.
     */
    static String executableSql(String statement) {
        String sql = statement == null ? "" : statement.trim();
        if (PROCEDURAL_BLOCK_END.matcher(sql).find()) return sql;
        while (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
        return sql;
    }

    private static String segmentAt(String text, int position) {
        int start = position == 0 ? -1 : text.lastIndexOf(';', position - 1);
        int end = text.indexOf(';', position);
        start = start < 0 ? 0 : start + 1;
        end = end < 0 ? text.length() : end + 1;
        return text.substring(start, end).trim();
    }

    private String compactSql(String sql) {
        String compact = sql.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 117) + "…";
    }

    private void appendOutput(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        outputLog.append("[" + time + "] " + message + System.lineSeparator());
        outputLog.setCaretPosition(outputLog.getDocument().getLength());
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private void disconnect() {
        try {
            metadataLoadSequence++;
            cancelMetadataLoad();
            closeResources();
            connection = null;
            objectTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Database")));
            resultModel.setRowCount(0);
            resultModel.setColumnCount(0);
            metadataSuggestions = new ArrayList<>();
            defaultScope = null;
            refreshSuggestions();
            updateConnectionControls(false);
            setStatus(SqlI18n.t("disconnected"));
        } catch (Exception exception) {
            setStatus(SqlI18n.t("error", safeMessage(exception)));
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
        profiles.setEnabled(!connected);
        newProfileButton.setEnabled(!connected);
        saveProfileButton.setEnabled(!connected);
        copyProfileButton.setEnabled(!connected);
        deleteProfileButton.setEnabled(!connected && profiles.getSelectedItem() != null);
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

    private record MetadataSnapshot(DefaultMutableTreeNode root, List<String> suggestions, int schemaCount,
                                    String currentCatalog, String currentSchema) { }
    private record MetadataScope(String catalog, String schema, String displayName) {
        @Override public String toString() { return displayName; }
    }
    private record ColumnInfo(String name, String displayName) { }

    /** One column of a table as reported by JDBC metadata, with its primary key flag. */
    record ColumnDetail(String name, String typeName, int size, int scale, boolean nullable, String defaultValue,
                        boolean primaryKey, String remarks) {
        String displayType() {
            String upper = typeName == null ? "" : typeName.toUpperCase(java.util.Locale.ROOT);
            boolean sized = upper.contains("CHAR") || upper.contains("BINARY") || upper.contains("RAW")
                    || upper.contains("DECIMAL") || upper.contains("NUMERIC") || upper.equals("NUMBER");
            if (!sized || size <= 0 || upper.contains("(")) return typeName;
            return typeName + "(" + size + (scale > 0 ? "," + scale : "") + ")";
        }

        String summary() {
            return name + "  ·  " + displayType() + (nullable ? "" : "  ·  NOT NULL") + (primaryKey ? "  ·  PK" : "");
        }
    }

    static List<ColumnDetail> readColumnDetails(DatabaseMetaData metadata, TableRef table) throws java.sql.SQLException {
        java.util.Set<String> primaryKeys = new java.util.HashSet<>();
        try (ResultSet keys = metadata.getPrimaryKeys(table.catalog(), table.schema(), table.table())) {
            while (keys.next()) primaryKeys.add(keys.getString("COLUMN_NAME"));
        } catch (java.sql.SQLException | AbstractMethodError ignored) {
            // views and synonyms have no primary key metadata on some drivers
        }
        String escape = searchStringEscape(metadata);
        List<ColumnDetail> columns = new ArrayList<>();
        try (ResultSet result = metadata.getColumns(table.catalog(), escapePattern(table.schema(), escape),
                escapePattern(table.table(), escape), null)) {
            while (result.next()) {
                String name = result.getString("COLUMN_NAME");
                columns.add(new ColumnDetail(name, result.getString("TYPE_NAME"), result.getInt("COLUMN_SIZE"),
                        result.getInt("DECIMAL_DIGITS"), result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                        safeString(result, "COLUMN_DEF"), primaryKeys.contains(name), safeString(result, "REMARKS")));
            }
        }
        return columns;
    }

    private static String safeString(ResultSet result, String column) {
        try {
            String value = result.getString(column);
            return value == null ? "" : value.trim();
        } catch (java.sql.SQLException ignored) {
            return "";
        }
    }

    /** Lists every column property of a table in the results grid ("View column details"). */
    private void showColumnDetails(TableRef table) {
        Connection active = connection;
        if (active == null) return;
        setStatus(SqlI18n.t("loading_columns", table.table()));
        new SwingWorker<List<ColumnDetail>, Void>() {
            @Override protected List<ColumnDetail> doInBackground() throws Exception {
                return readColumnDetails(active.getMetaData(), table);
            }

            @Override protected void done() {
                if (connection != active) return;
                try {
                    List<ColumnDetail> columns = get();
                    resultModel.setRowCount(0);
                    resultModel.setColumnIdentifiers(new Object[]{SqlI18n.t("column_name"), SqlI18n.t("column_type"),
                            SqlI18n.t("column_nullable"), SqlI18n.t("column_primary_key"), SqlI18n.t("column_default"),
                            SqlI18n.t("column_remarks")});
                    for (ColumnDetail column : columns) {
                        resultModel.addRow(new Object[]{column.name(), column.displayType(),
                                column.nullable() ? "YES" : "NO", column.primaryKey() ? "PK" : "",
                                column.defaultValue(), column.remarks()});
                    }
                    resultTabs.setSelectedIndex(0);
                    setStatus(SqlI18n.t("columns_loaded", columns.size(), table.qualifiedName()));
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(SqlI18n.t("error", safeMessage(cause)));
                }
            }
        }.execute();
    }
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
