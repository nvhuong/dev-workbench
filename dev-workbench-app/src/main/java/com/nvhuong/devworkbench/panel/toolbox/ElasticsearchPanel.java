package com.nvhuong.devworkbench.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import com.nvhuong.devworkbench.common.component.HttpRequestDisplayMetadata;
import com.nvhuong.devworkbench.common.component.ListEmptyTextPainter;
import com.nvhuong.devworkbench.common.component.FallbackAwareRSyntaxTextArea;
import com.nvhuong.devworkbench.common.component.SearchableTextArea;
import com.nvhuong.devworkbench.common.component.SearchTextField;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSidebarHeader;
import com.nvhuong.devworkbench.common.component.ToolWindowSidebarToolbar;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ClearButton;
import com.nvhuong.devworkbench.common.component.button.CompactPrimaryButton;
import com.nvhuong.devworkbench.common.component.button.CopyButton;
import com.nvhuong.devworkbench.common.component.button.FormatButton;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.button.RefreshButton;
import com.nvhuong.devworkbench.common.component.connection.ConnectionToolbarUi;
import com.nvhuong.devworkbench.common.component.dialog.TextInputDialog;
import com.nvhuong.devworkbench.common.component.table.EnhancedTablePanel;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.http.runtime.okhttp.OkHttpClientManager;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.JsonUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import tools.jackson.databind.JsonNode;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Elasticsearch Trực quan hóa CRUD + DSL Bảng công cụ
 */
@Slf4j
public class ElasticsearchPanel extends JPanel {

    // ===== kết nối =====
    private JComboBox<ElasticsearchConnectionProfile> profileCombo;
    private JButton newProfileBtn;
    private JButton saveProfileBtn;
    private JButton saveAsProfileBtn;
    private JButton deleteProfileBtn;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<AuthMode> authModeCombo;
    private JPanel connectionForm;
    private JComponent[] authComponents;
    private JButton connectBtn;
    private JButton testConnectionBtn;
    private JButton disconnectBtn;
    private CardLayout btnCardLayout;   // Được sử dụng trong connectBtn/disconnectBtn chuyển đổi giữa
    private JPanel btnCard;
    private final ElasticsearchConnectionProfileStore connectionProfileStore =
            new ElasticsearchConnectionProfileStore();
    private boolean loadingConnectionProfiles;

    // ===== Quản lý chỉ số =====
    private DefaultListModel<String> indexListModel;
    private DefaultListModel<String> indexFilteredModel;
    private JList<String> indexList;
    private JTextField newIndexField;
    private DevWorkbenchJSpinner shardSpinner;
    private DevWorkbenchJSpinner replicaSpinner;
    private JButton createIndexBtn;
    private RefreshButton refreshIndicesBtn;
    private boolean loadingIndices;
    private SearchTextField indexSearchField;
    /**
     * indexName -> docCount (Từ _cat/indices phân tích)
     */
    private final Map<String, Long> indexDocCountMap = new LinkedHashMap<>();

    // ===== DSL Biên tập viên =====
    private JComboBox<String> templateCombo;
    private JComboBox<String> methodCombo;
    private JTextField pathField;
    private RSyntaxTextArea dslEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableResultArea;
    private SearchableTextArea searchableDslArea;

    // ===== Bảng kết quả =====
    private EnhancedTablePanel enhancedTable;
    private EnhancedTablePanel aggTable;
    private JTabbedPane resultTabs;
    private JLabel respStatusLabel;
    private JLabel hitsInfoLabel;
    private CompactPrimaryButton executeBtn;

    // ===== Yêu cầu lịch sử =====
    private static final int MAX_HISTORY = 20;
    /**
     * Mỗi định dạng bản ghi: "METHOD PATH\nBODY"
     */
    private final Deque<HistoryEntry> requestHistory = new ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

    private String baseUrl = "http://localhost:9200";
    private String authHeader = null;
    private boolean connected = false;

    // ===== không thay đổi =====
    private static final String SEARCH_PATH = "/{index}/_search";
    private static final String HTTP_DELETE = "DELETE";
    private static final String CLUSTER_HEALTH_PATH = "/_cluster/health";
    private static final String JSON_UTF8 = "application/json; charset=utf-8";
    private static final String JSON_MIME = "application/json";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String CLIENT_PROP_TOTAL_HITS = "es.totalHits";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;
    // aggregation JSON field names
    private static final String AGG_KEY_AS_STRING = "key_as_string";
    private static final String AGG_KEY = "key";
    private static final String AGG_DOC_COUNT = "doc_count";
    private static final String AGG_VALUE = "value";
    private static final String AGG_BUCKETS = "buckets";
    private static final String CONNECT_CARD = "connect";
    private static final String DISCONNECT_CARD = "disconnect";
    private static final int GROUP_GAP = 12;
    private static final int PROFILE_GROUP_MIN_WIDTH = 150;
    private static final int PROFILE_GROUP_PREF_WIDTH = 240;
    private static final int FIELD_GROUP_MIN_WIDTH = 110;
    private static final int FIELD_GROUP_PREF_WIDTH = 200;

    // ===== Tích hợp sẵn DSL bản mẫu =====
    private static final String[][] DSL_TEMPLATES = {
            {"toolbox.es.tpl.match_all", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}"},
            {"toolbox.es.tpl.match", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"match\": {\n      \"field\": \"value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.term", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"term\": {\n      \"field.keyword\": \"exact_value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.range", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"range\": {\n      \"timestamp\": {\n        \"gte\": \"2024-01-01\",\n        \"lte\": \"2024-12-31\"\n      }\n    }\n  }\n}"},
            {"toolbox.es.tpl.bool", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"bool\": {\n      \"must\": [\n        { \"match\": { \"title\": \"elasticsearch\" } }\n      ],\n      \"filter\": [\n        { \"term\": { \"status\": \"active\" } }\n      ],\n      \"must_not\": [],\n      \"should\": []\n    }\n  }\n}"},
            {"toolbox.es.tpl.agg", "GET", SEARCH_PATH,
                    "{\n  \"size\": 0,\n  \"aggs\": {\n    \"group_by_status\": {\n      \"terms\": {\n        \"field\": \"status.keyword\",\n        \"size\": 10\n      }\n    }\n  }\n}"},
            {"toolbox.es.tpl.index_doc", "POST", "/{index}/_doc",
                    "{\n  \"field1\": \"value1\",\n  \"field2\": \"value2\",\n  \"timestamp\": \"2024-01-01T00:00:00Z\"\n}"},
            {"toolbox.es.tpl.update_doc", "POST", "/{index}/_update/{id}",
                    "{\n  \"doc\": {\n    \"field\": \"new_value\"\n  }\n}"},
            {"toolbox.es.tpl.delete_doc", HTTP_DELETE, "/{index}/_doc/{id}", ""},
            {"toolbox.es.tpl.get_doc", "GET", "/{index}/_doc/{id}", ""},
            {"toolbox.es.tpl.mapping", "GET", "/{index}/_mapping", ""},
            {"toolbox.es.tpl.settings", "GET", "/{index}/_settings", ""},
            {"toolbox.es.tpl.health", "GET", CLUSTER_HEALTH_PATH, ""},
            {"toolbox.es.tpl.cluster_stats", "GET", "/_cluster/stats", ""},
            {"toolbox.es.tpl.list_indices", "GET", "/_cat/indices?v&format=json", ""},
            {"toolbox.es.tpl.create_index", "PUT", "/{index}",
                    "{\n  \"settings\": {\n    \"number_of_shards\": 1,\n    \"number_of_replicas\": 1\n  },\n  \"mappings\": {\n    \"properties\": {\n      \"title\": { \"type\": \"text\" },\n      \"status\": { \"type\": \"keyword\" },\n      \"timestamp\": { \"type\": \"date\" }\n    }\n  }\n}"},
            {"toolbox.es.tpl.reindex", "POST", "/_reindex",
                    "{\n  \"source\": {\n    \"index\": \"source_index\"\n  },\n  \"dest\": {\n    \"index\": \"dest_index\"\n  }\n}"},
            {"toolbox.es.tpl.delete_by_query", "POST", "/{index}/_delete_by_query",
                    "{\n  \"query\": {\n    \"match\": {\n      \"field\": \"value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.update_by_query", "POST", "/{index}/_update_by_query",
                    "{\n  \"script\": {\n    \"source\": \"ctx._source.status = 'updated'\"\n  },\n  \"query\": {\n    \"match_all\": {}\n  }\n}"},
    };

    // ===== Mục nhập lịch sử =====
    @RequiredArgsConstructor
    private static class HistoryEntry {
        final String method;
        final String path;
        final String body;

        @Override
        public String toString() {
            return method + "  " + path;
        }
    }

    @RequiredArgsConstructor
    private enum AuthMode {
        NONE(MessageKeys.TOOLBOX_ES_AUTH_NONE),
        BASIC(MessageKeys.TOOLBOX_ES_AUTH_BASIC);

        private final String messageKey;

        String displayName() {
            return I18nUtil.getMessage(messageKey);
        }
    }

    public ElasticsearchPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyCard(this);

        // Thanh kết nối phía trên
        add(buildConnectionPanel(), BorderLayout.NORTH);

        // Khu vực nội dung chính: bên trái (index+lịch sử)+ bên phải DSL
        JSplitPane mainSplit = AppToolWindowChrome.createHorizontalInnerSplitPane(
                buildLeftPanel(),
                buildDslPanel(),
                240
        );
        mainSplit.setDividerLocation(240);
        add(mainSplit, BorderLayout.CENTER);
        ToolWindowSurfaceStyle.applyPanelTreeCard(this);
        updateConnectedState(false);
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
    }

    // ===== Kết nối bảng điều khiển =====
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionHeader(panel, 3, 6, 3, 6);

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        ConnectionToolbarUi.compactControl(profileCombo);
        profileCombo.setPrototypeDisplayValue(ElasticsearchConnectionProfile.builder()
                .name("Default Elasticsearch")
                .build());
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(ElasticsearchConnectionProfile::getName));
        profileCombo.addActionListener(e -> applySelectedConnectionProfile());

        newProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NEW),
                "icons/plus.svg", e -> createNewConnectionProfile());
        saveProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE),
                "icons/save.svg", e -> saveCurrentConnectionProfile(true));
        saveProfileBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE) + " (Ctrl+S)");
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE_AS),
                "icons/duplicate.svg", e -> saveCurrentConnectionProfileAs());
        deleteProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE),
                "icons/delete.svg", e -> deleteSelectedConnectionProfile());

        hostField = new JTextField();
        ConnectionToolbarUi.compactControl(hostField);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST_PLACEHOLDER));
        hostField.addActionListener(e -> doConnect());

        authModeCombo = ConnectionToolbarUi.comboBox(AuthMode.values(), AuthMode::displayName);
        authModeCombo.addActionListener(e -> setAuthFieldsEnabled(getSelectedAuthMode() == AuthMode.BASIC));

        usernameField = new JTextField();
        ConnectionToolbarUi.compactControl(usernameField);
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER_PLACEHOLDER));
        usernameField.addActionListener(e -> doConnect());

        passwordField = new JPasswordField();
        ConnectionToolbarUi.compactControl(passwordField);
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS_PLACEHOLDER));
        passwordField.addActionListener(e -> doConnect());

        connectBtn = ModernButtonFactory.createCompactButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT), true, "icons/connect.svg");
        connectBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT_TOOLTIP));
        connectBtn.addActionListener(e -> doConnect());

        disconnectBtn = ModernButtonFactory.createCompactButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT), false, "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> doDisconnect());

        testConnectionBtn = ModernButtonFactory.createCompactButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION), false, "icons/check.svg");
        testConnectionBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION_TOOLTIP));
        testConnectionBtn.addActionListener(e -> testConnection());

        // Sử dụng CardLayout Sẽ connectBtn / disconnectBtn Xếp chồng lên nhau trong cùng một lưới, không còn khoảng trống khi chuyển đổi
        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CONNECT_CARD);
        btnCard.add(disconnectBtn, DISCONNECT_CARD);
        btnCardLayout.show(btnCard, CONNECT_CARD);

        String controlHeight = "wmin 0, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!";
        JPanel profileGroup = new JPanel(new MigLayout(
                "insets 0, novisualpadding, gapx 0",
                "[grow,fill]2[" + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]",
                "[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!,center]"
        ));
        profileGroup.setOpaque(false);
        profileGroup.add(profileCombo, controlHeight);
        profileGroup.add(newProfileBtn);
        profileGroup.add(saveProfileBtn);
        profileGroup.add(saveAsProfileBtn);
        profileGroup.add(deleteProfileBtn);

        JPanel actions = new JPanel(new MigLayout("insets 0, novisualpadding, gapx 6", "[][]", "[]"));
        actions.setOpaque(false);
        String buttonHeight = "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!";
        actions.add(testConnectionBtn, buttonHeight);
        actions.add(btnCard, buttonHeight);

        // Và Kafka/Redis Nhất quán: Hai hàng dùng chung một lưới và các cạnh trái và phải của mỗi cột được căn chỉnh; địa chỉ kéo dài cột, hộp mật khẩu kéo dài cột và nút kết nối được căn phải.
        connectionForm = new JPanel(new MigLayout(
                "insets 0, novisualpadding, gapy 4",
                growFieldColumns(PROFILE_GROUP_MIN_WIDTH, PROFILE_GROUP_PREF_WIDTH) + GROUP_GAP
                        + growFieldColumns(FIELD_GROUP_MIN_WIDTH, FIELD_GROUP_PREF_WIDTH) + GROUP_GAP
                        + growFieldColumns(FIELD_GROUP_MIN_WIDTH, FIELD_GROUP_PREF_WIDTH) + GROUP_GAP
                        + "[pref!,fill]",
                "[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!,center]4["
                        + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!,center]"
        ));
        connectionForm.setOpaque(false);

        // KHÔNG.  1 Dòng: cấu hình + địa chỉ + hoạt động
        connectionForm.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE)));
        connectionForm.add(profileGroup, controlHeight);
        connectionForm.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST)));
        connectionForm.add(hostField, "span 3, growx, " + controlHeight);
        connectionForm.add(actions, "right, wrap");

        // KHÔNG.  2 Dòng: Xác thực
        JLabel userLabel = ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER));
        JLabel passLabel = ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS));
        connectionForm.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_AUTH)));
        connectionForm.add(authModeCombo, controlHeight);
        connectionForm.add(userLabel);
        connectionForm.add(usernameField, controlHeight);
        connectionForm.add(passLabel);
        connectionForm.add(passwordField, "span 2, growx, " + controlHeight);
        authComponents = new JComponent[]{userLabel, usernameField, passLabel, passwordField};

        JPanel wrapper = new JPanel(new MigLayout("insets 0, novisualpadding, fillx", "[grow,fill]", "[]"));
        wrapper.setOpaque(false);
        wrapper.add(connectionForm, "growx, wmin 0");
        panel.add(wrapper, BorderLayout.CENTER);
        ConnectionToolbarUi.lockConnectionPanelHeight(panel, true);
        setAuthFieldsEnabled(false);
        ConnectionToolbarUi.registerSaveShortcut(connectionForm, () -> saveCurrentConnectionProfile(true));
        loadSavedConnectionProfiles(null);

        return panel;
    }

    /**
     * Cột nhãn thích ứng theo nhãn dài nhất trong cột; cửa sổ hẹp của cột trường có thể được nén thành min，Cửa sổ rộng chia đều không gian thừa, kéo dài tới 2 lần.
     */
    private static String growFieldColumns(int minWidth, int prefWidth) {
        return "[pref!,right]4[" + minWidth + ":" + prefWidth + ":" + (prefWidth * 2) + ",fill,grow,shrink]";
    }

    private void loadSavedConnectionProfiles(String preferredProfileId) {
        loadingConnectionProfiles = true;
        profileCombo.removeAllItems();
        List<ElasticsearchConnectionProfile> profiles = connectionProfileStore.loadProfiles();
        ElasticsearchConnectionProfile activeProfile = connectionProfileStore.loadActiveProfile()
                .orElse(ElasticsearchConnectionProfileStore.defaultProfile());
        String selectedProfileId = preferredProfileId == null || preferredProfileId.isBlank()
                ? activeProfile.getId()
                : preferredProfileId;
        ElasticsearchConnectionProfile selectedProfile = null;
        for (ElasticsearchConnectionProfile profile : profiles) {
            profileCombo.addItem(profile);
            if (profile.getId().equals(selectedProfileId)) {
                selectedProfile = profile;
            }
        }
        if (selectedProfile == null && profileCombo.getItemCount() > 0) {
            selectedProfile = profileCombo.getItemAt(0);
        }
        if (selectedProfile != null) {
            profileCombo.setSelectedItem(selectedProfile);
        }
        loadingConnectionProfiles = false;
        applyConnectionProfile(selectedProfile);
        updateProfileActionState();
    }

    private void applySelectedConnectionProfile() {
        if (loadingConnectionProfiles) {
            return;
        }
        ElasticsearchConnectionProfile profile = getSelectedConnectionProfile();
        applyConnectionProfile(profile);
        if (profile != null) {
            connectionProfileStore.saveProfiles(connectionProfileStore.loadProfiles(), profile.getId());
        }
        updateProfileActionState();
    }

    private void applyConnectionProfile(ElasticsearchConnectionProfile profile) {
        if (profile == null) {
            return;
        }
        applyHostHistory(profile);
        authModeCombo.setSelectedItem(profile.isAuthEnabled() ? AuthMode.BASIC : AuthMode.NONE);
        usernameField.setText(profile.getUsername() == null ? "" : profile.getUsername());
        passwordField.setText(profile.getPassword() == null ? "" : profile.getPassword());
        setAuthFieldsEnabled(profile.isAuthEnabled());
    }

    private void createNewConnectionProfile() {
        String initialName = uniqueProfileName(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NEW_DEFAULT));
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationCenter.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
    }

    private void saveCurrentConnectionProfile(boolean notify) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NOT_SELECTED));
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(selectedProfile.getId(), selectedProfile.getName());
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationCenter.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
        }
    }

    private void saveConnectionProfile(String finalUrl, String user, String pass, boolean notify) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = ElasticsearchConnectionProfile.builder()
                .id(selectedProfile.getId())
                .name(selectedProfile.getName())
                .baseUrl(ElasticsearchConnectionProfileStore.normalizeBaseUrl(finalUrl))
                .authEnabled(getSelectedAuthMode() == AuthMode.BASIC)
                .username(user == null ? "" : user.trim())
                .password(pass == null ? "" : pass)
                .hostHistory(currentHostHistoryWith(finalUrl))
                .build();
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationCenter.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
        }
    }

    private void saveCurrentConnectionProfileAs() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        String initialName = selectedProfile == null
                ? connectionProfileNameSuggestion()
                : uniqueProfileName(selectedProfile.getName());
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationCenter.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
    }

    private void deleteSelectedConnectionProfile() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NOT_SELECTED));
            return;
        }
        if (isDefaultProfile(selectedProfile)) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DEFAULT_NOT_DELETABLE));
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE_CONFIRM),
                        selectedProfile.getName()),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        String deletedName = selectedProfile.getName();
        connectionProfileStore.deleteProfile(selectedProfile.getId());
        loadSavedConnectionProfiles(ElasticsearchConnectionProfileStore.DEFAULT_PROFILE_ID);
        NotificationCenter.showInfo(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETED), deletedName));
    }

    private ElasticsearchConnectionProfile buildProfile(String profileId, String profileName) {
        String url = ElasticsearchConnectionProfileStore.normalizeBaseUrl(getCurrentHost());
        return ElasticsearchConnectionProfile.builder()
                .id(profileId)
                .name(profileName)
                .baseUrl(url)
                .authEnabled(getSelectedAuthMode() == AuthMode.BASIC)
                .username(usernameField.getText().trim())
                .password(new String(passwordField.getPassword()))
                .hostHistory(currentHostHistoryWith(url))
                .build();
    }

    private String promptProfileName(String initialValue, String existingProfileId) {
        String name = TextInputDialog.showRequiredName(this,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE_AS_TITLE),
                initialValue,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NAME_REQUIRED)
        ).orElse(null);
        if (name == null) return null;
        if (profileNameExists(name, existingProfileId)) {
            NotificationCenter.showWarning(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NAME_EXISTS), name));
            return null;
        }
        return name;
    }

    private boolean profileNameExists(String name, String ignoredProfileId) {
        String normalizedName = name == null ? "" : name.trim();
        for (ElasticsearchConnectionProfile profile : connectionProfileStore.loadProfiles()) {
            boolean sameProfile = ignoredProfileId != null && ignoredProfileId.equals(profile.getId());
            if (!sameProfile && profile.getName().equalsIgnoreCase(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private String uniqueProfileName(String baseName) {
        String normalizedBaseName = baseName == null || baseName.isBlank()
                ? connectionProfileNameSuggestion()
                : baseName.trim();
        if (!profileNameExists(normalizedBaseName, null)) {
            return normalizedBaseName;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = normalizedBaseName + " " + i;
            if (!profileNameExists(candidate, null)) {
                return candidate;
            }
        }
        return normalizedBaseName + " " + System.currentTimeMillis();
    }

    private String connectionProfileNameSuggestion() {
        return ElasticsearchConnectionProfileStore.normalizeBaseUrl(getCurrentHost());
    }

    private ElasticsearchConnectionProfile getSelectedConnectionProfile() {
        Object selected = profileCombo.getSelectedItem();
        return selected instanceof ElasticsearchConnectionProfile profile ? profile : null;
    }

    private boolean isDefaultProfile(ElasticsearchConnectionProfile profile) {
        return profile != null && ElasticsearchConnectionProfileStore.DEFAULT_PROFILE_ID.equals(profile.getId());
    }

    private void updateProfileActionState() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        boolean hasProfile = selectedProfile != null;
        saveProfileBtn.setEnabled(hasProfile);
        saveAsProfileBtn.setEnabled(hasProfile);
        deleteProfileBtn.setEnabled(hasProfile && !isDefaultProfile(selectedProfile));
    }

    /**
     * Vô hiệu hóa (thay vì ẩn) tên người dùng và mật khẩu mà không cần xác thực: bố cục vẫn ổn định và người dùng có thể xem trường nào có thể điền sau khi chuyển đổi phương thức xác thực.
     */
    private void setAuthFieldsEnabled(boolean enabled) {
        if (authComponents == null) {
            return;
        }
        for (JComponent component : authComponents) {
            component.setEnabled(enabled);
        }
    }

    private AuthMode getSelectedAuthMode() {
        Object selected = authModeCombo == null ? null : authModeCombo.getSelectedItem();
        return selected instanceof AuthMode authMode ? authMode : AuthMode.NONE;
    }

    private void applyHostHistory(ElasticsearchConnectionProfile profile) {
        hostField.setText(ElasticsearchConnectionProfileStore.normalizeBaseUrl(profile.getBaseUrl()));
    }

    private List<String> currentHostHistoryWith(String activeHost) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        List<String> existingHistory = selectedProfile == null ? List.of() : selectedProfile.getHostHistory();
        return ElasticsearchConnectionProfileStore.normalizeHostHistory(existingHistory, activeHost);
    }

    // ===== Bảng bên trái: Quản lý chỉ mục + Lịch sử (JTabbedPane）=====
    private JPanel buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(240, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(leftTabs);
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_TAB), buildIndexPanel());
        leftTabs.setToolTipTextAt(0, I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT));
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY), buildHistoryPanel());

        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    // ===== Bảng quản lý chỉ mục =====
    private JPanel buildIndexPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());

        refreshIndicesBtn = new RefreshButton();
        refreshIndicesBtn.addActionListener(e -> loadIndices());
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT), refreshIndicesBtn);

        // Hộp tìm kiếm: Chỉ thực hiện lọc bao gồm tên, không yêu cầu viết hoa chữ thường/Toàn bộ từ và các tùy chọn khác
        indexSearchField = new SearchTextField();
        indexSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        indexSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        // Danh sách chỉ mục
        indexListModel = new DefaultListModel<>();
        indexFilteredModel = new DefaultListModel<>();
        indexList = new JList<>(indexFilteredModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().getSize() == 0) {
                    ListEmptyTextPainter.paint(this, g, indexListEmptyText());
                }
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int index = locationToIndex(event.getPoint());
                if (index < 0 || !getCellBounds(index, index).contains(event.getPoint())) {
                    return null;
                }
                String name = getModel().getElementAt(index);
                Long count = indexDocCountMap.get(name);
                return count == null ? name : MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_TOOLTIP), name, count);
            }
        };
        ToolTipManager.sharedInstance().registerComponent(indexList);
        indexList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        indexList.setCellRenderer(new IndexCellRenderer());
        indexList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                List<String> selected = indexList.getSelectedValuesList();
                if (selected.size() == 1) {
                    pathField.setText("/" + selected.get(0) + "/_search");
                }
            }
        });
        indexList.addMouseListener(buildIndexListMouseListener());
        registerIndexListShortcuts();
        JScrollPane listScroll = new JScrollPane(indexList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(listScroll, indexList);
        listScroll.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_LIST_HINT));

        // Lọc theo thời gian thực
        indexSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }
        });

        // Tạo một chỉ mục: tên trên một dòng, được phân chia/sao chép/Tạo các nút chia sẻ một hàng để ngăn chặn việc cắt bớt các thanh bên hẹp
        JPanel createPanel = new JPanel(new MigLayout(
                "insets 6 8 6 8, fillx, novisualpadding",
                "[pref!,right]4[grow,fill]8[pref!,right]4[grow,fill]",
                "[]4[]"
        ));
        ToolWindowSurfaceStyle.applySectionHeader(createPanel);

        newIndexField = new JTextField();
        newIndexField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_PLACEHOLDER));
        newIndexField.addActionListener(e -> createIndex());
        shardSpinner = DevWorkbenchJSpinner.intSpinner(1, 1, 100, 1);
        replicaSpinner = DevWorkbenchJSpinner.intSpinner(1, 0, 10, 1);
        createIndexBtn = ModernButtonFactory.createCompactButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE), true, "icons/plus.svg");
        createIndexBtn.addActionListener(e -> createIndex());

        String controlHeight = "wmin 0, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!";
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME)));
        createPanel.add(newIndexField, "span 3, growx, " + controlHeight + ", wrap");
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_SHARDS)));
        createPanel.add(shardSpinner, "wmin 48, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!");
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_REPLICAS)));
        createPanel.add(replicaSpinner, "wmin 48, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!, wrap");
        createPanel.add(createIndexBtn, "span 4, growx, gaptop 2, h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!");

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(titleBar, BorderLayout.NORTH);
        topArea.add(new ToolWindowSidebarToolbar(null, indexSearchField), BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        panel.add(createPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void registerIndexListShortcuts() {
        InputMap inputMap = indexList.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = indexList.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "esQuickSearch");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "esDeleteIndices");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.META_DOWN_MASK), "esDeleteIndices");
        actionMap.put("esQuickSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeIndexQuickQuery(indexList.getSelectedValue());
            }
        });
        actionMap.put("esDeleteIndices", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteIndices(new ArrayList<>(indexList.getSelectedValuesList()));
            }
        });
    }

    String indexListEmptyText() {
        if (loadingIndices) {
            return I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_EMPTY_LOADING);
        }
        if (!connected) {
            return I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_EMPTY_NOT_CONNECTED);
        }
        if (indexListModel.isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_LIST_EMPTY);
        }
        return I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_EMPTY_NO_MATCH);
    }

    /**
     * Danh sách chỉ mục Cell Renderer：Tên chỉ mục + Số lượng tài liệu ở bên phải
     */
    private class IndexCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof String name) {
                Long count = indexDocCountMap.get(name);
                if (count != null) {
                    String countColor = ModernColors.toHtmlColor(isSelected
                            ? lbl.getForeground()
                            : ModernColors.getTextHint());
                    lbl.setText("<html><b>" + name + "</b>"
                            + "&nbsp;<font color='" + countColor + "'>"
                            + count + "</font></html>");
                } else {
                    lbl.setText(name);
                }
            }
            return lbl;
        }
    }

    // ===== Yêu cầu bảng lịch sử =====
    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY), clearHistBtn);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().getSize() == 0) {
                    ListEmptyTextPainter.paint(this, g, I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_EMPTY));
                }
            }
        };
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx >= 0) applyHistory(historyListModel.get(idx));
                }
            }
        });
        historyList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "esApplyHistory");
        historyList.getActionMap().put("esApplyHistory", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyHistory(historyList.getSelectedValue());
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(scroll, historyList);

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Lịch sử Cell Renderer
     */
    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof HistoryEntry h) {
                String color = HttpRequestDisplayMetadata.methodColorHex(h.method);
                String path = h.path.length() > 32 ? h.path.substring(0, 30) + "…" : h.path;
                lbl.setText("<html><b><font color='" + color + "'>" + h.method + "</font></b> " + path + "</html>");
                lbl.setToolTipText(h.method + " " + h.path);
            }
            return lbl;
        }
    }

    private void applyHistory(HistoryEntry entry) {
        if (entry == null) return;
        methodCombo.setSelectedItem(entry.method);
        pathField.setText(entry.path);
        dslEditor.setText(entry.body);
        dslEditor.setCaretPosition(0);
    }

    private void addToHistory(String method, String path, String body) {
        // Chống trùng lặp: Tương tự method+path+body Không thêm nhiều lần
        for (HistoryEntry e : requestHistory) {
            if (e.method.equals(method) && e.path.equals(path) && e.body.equals(body)) {
                // Di chuyển lên trên cùng
                requestHistory.remove(e);
                requestHistory.addFirst(e);
                rebuildHistoryListModel();
                return;
            }
        }
        requestHistory.addFirst(new HistoryEntry(method, path, body));
        while (requestHistory.size() > MAX_HISTORY) requestHistory.removeLast();
        rebuildHistoryListModel();
    }

    private void rebuildHistoryListModel() {
        historyListModel.clear();
        for (HistoryEntry e : requestHistory) historyListModel.addElement(e);
    }

    private MouseAdapter buildIndexListMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                    int idx = indexList.locationToIndex(evt.getPoint());
                    if (idx >= 0) {
                        indexList.setSelectedIndex(idx);
                        executeIndexQuickQuery(indexList.getSelectedValue());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }
        };
    }

    private void maybeShowIndexPopup(MouseEvent evt) {
        int idx = indexList.locationToIndex(evt.getPoint());
        if (idx >= 0 && !indexList.isSelectedIndex(idx)) indexList.setSelectedIndex(idx);
        List<String> selectedIndices = new ArrayList<>(indexList.getSelectedValuesList());
        String primarySelected = indexList.getSelectedValue();

        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        // Sao chép tên chỉ mục
        JMenuItem copyNameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_COPY_NAME));
        copyNameItem.setEnabled(primarySelected != null);
        copyNameItem.addActionListener(e -> {
            if (primarySelected != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(primarySelected), null);
                NotificationCenter.showSuccess(MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_COPIED), primarySelected));
            }
        });
        menu.add(copyNameItem);

        menu.addSeparator();

        // Xem bí danh
        JMenuItem aliasItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_ALIASES));
        aliasItem.setEnabled(primarySelected != null);
        aliasItem.addActionListener(e -> {
            if (primarySelected != null) {
                pathField.setText("/" + primarySelected + "/_alias");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        menu.add(aliasItem);

        menu.add(buildViewMappingItem(selectedIndices.size() == 1 ? primarySelected : null));
        menu.add(buildViewSettingsItem(selectedIndices.size() == 1 ? primarySelected : null));

        menu.addSeparator();

        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE));
        deleteItem.setEnabled(!selectedIndices.isEmpty());
        deleteItem.addActionListener(e -> deleteIndices(selectedIndices));
        menu.add(deleteItem);

        JMenuItem clearItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR));
        clearItem.setEnabled(!selectedIndices.isEmpty());
        clearItem.addActionListener(e -> clearIndices(selectedIndices));
        menu.add(clearItem);

        menu.show(indexList, evt.getX(), evt.getY());
    }

    private void executeIndexQuickQuery(String indexName) {
        if (!connected || indexName == null || indexName.isBlank()) return;
        methodCombo.setSelectedItem("GET");
        pathField.setText("/" + indexName + "/_search");
        String body = dslEditor.getText().trim();
        if (body.isEmpty()) {
            dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
            dslEditor.setCaretPosition(0);
        }
        executeRequest();
    }

    private JMenuItem buildViewMappingItem(String sel) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_MAPPING));
        item.setEnabled(sel != null);
        item.addActionListener(e -> {
            if (sel != null) {
                pathField.setText("/" + sel + "/_mapping");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        return item;
    }

    private JMenuItem buildViewSettingsItem(String sel) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_SETTINGS));
        item.setEnabled(sel != null);
        item.addActionListener(e -> {
            if (sel != null) {
                pathField.setText("/" + sel + "/_settings");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        return item;
    }

    private String indexSearchText() {
        return indexSearchField == null ? "" : indexSearchField.getText();
    }

    private void filterIndices(String kw) {
        String lower = kw == null ? "" : kw.trim().toLowerCase();
        indexFilteredModel.clear();
        for (int i = 0; i < indexListModel.size(); i++) {
            String name = indexListModel.get(i);
            if (lower.isEmpty() || name.toLowerCase().contains(lower)) {
                indexFilteredModel.addElement(name);
            }
        }
    }

    /**
     * phân tích _cat/indices JSON，Điền vào danh sách chỉ mục Model + Số lượng tài liệu Map
     */
    private void parseAndFillIndexList(String json) {
        try {
            indexListModel.clear();
            indexDocCountMap.clear();
            JsonNode arr = JsonUtil.readTree(json);
            if (!arr.isArray()) return;
            for (JsonNode node : arr) {
                JsonNode idx = node.get("index");
                if (idx != null && !idx.isNull()) {
                    String name = idx.toString().replace("\"", "");
                    indexListModel.addElement(name);
                    // Số lượng tài liệu được phân tích cú pháp
                    JsonNode docsCount = node.get("docs.count");
                    if (docsCount != null && !docsCount.isNull()) {
                        try {
                            indexDocCountMap.put(name, Long.parseLong(docsCount.toString().replace("\"", "")));
                        } catch (NumberFormatException ignored) { /* skip */ }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("parseAndFillIndexList error: {}", e.getMessage());
        }
    }

    // ===== DSL Chỉnh sửa + Bảng kết quả =====
    private JPanel buildDslPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        // ---- thanh công cụ (MigLayout）----
        JPanel toolBar = new JPanel(new MigLayout("insets 4, fillx", "[][][][]8[][]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(toolBar);

        templateCombo = new JComboBox<>();
        for (String[] t : DSL_TEMPLATES) templateCombo.addItem(I18nUtil.getMessage(t[0]));
        templateCombo.setPreferredSize(new Dimension(180, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        JButton loadTplBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_LOAD_TEMPLATE),
                "icons/load.svg", e -> applyTemplate(templateCombo.getSelectedIndex()));

        FormatButton formatBtn = new FormatButton();
        formatBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_FORMAT_JSON));
        formatBtn.addActionListener(e -> formatDsl());

        CopyButton copyBtn = new CopyButton();
        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_COPY_RESULT));
        copyBtn.addActionListener(e -> {
            copyResult();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESULT_COPIED));
        });
        ClearButton clearBtn = new ClearButton();
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CLEAR));
        clearBtn.addActionListener(e -> {
            dslEditor.setText("");
            resultArea.setText("");
            clearTable();
            clearAggTable();
            respStatusLabel.setText("");
            hitsInfoLabel.setText("");
        });

        toolBar.add(templateCombo);
        toolBar.add(loadTplBtn);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");
        toolBar.add(formatBtn);
        toolBar.add(copyBtn);
        toolBar.add(clearBtn);

        // ---- Dòng yêu cầu (MigLayout）----
        JPanel requestRow = new JPanel(new MigLayout("insets 4 2 4 2, fillx", "[90!][grow,fill][]", "[]"));
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", HTTP_DELETE, "HEAD"});
        methodCombo.setPreferredSize(new Dimension(90, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        // Method Kết xuất màu
        methodCombo.setRenderer(new MethodComboRenderer());

        pathField = new JTextField(CLUSTER_HEALTH_PATH);
        pathField.setPreferredSize(new Dimension(400, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PATH_PLACEHOLDER));
        // Nhập vào ô đường dẫn để thực hiện
        pathField.addActionListener(e -> executeRequest());

        executeBtn = new CompactPrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_EXECUTE_SHORT), "icons/send.svg");
        executeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_EXECUTE));
        executeBtn.addActionListener(e -> executeRequest());

        requestRow.add(methodCombo);
        requestRow.add(pathField);
        requestRow.add(executeBtn);

        JPanel topArea = new JPanel(new BorderLayout(0, 4));
        topArea.setOpaque(false);
        topArea.add(toolBar, BorderLayout.NORTH);
        topArea.add(requestRow, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        // ---- Bộ phận chính:DSL Biên tập viên(thượng đẳng) + kết quả(Xuống) ----
        // DSL Biên tập viên - Tham khảo RequestBodyPanel，Có thể chỉnh sửa, sử dụng SearchableTextArea Gói (cho phép tìm kiếm và thay thế)
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setOpaque(false);
        // DSL Tiêu đề ở đầu trình soạn thảo + Nút công cụ (MigLayout）
        JPanel dslHeaderBar = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(dslHeaderBar);
        JLabel dslLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DSL_TITLE));
        dslLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        dslHeaderBar.add(dslLabel);

        dslEditor = createJsonEditor(true);
        registerCtrlEnterShortcut(executeBtn);
        dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
        // Tham khảo RequestBodyPanel：Có thể sử dụng trình soạn thảo có thể chỉnh sửa SearchableTextArea(area) Gói (cho phép tìm kiếm và thay thế)
        searchableDslArea = new SearchableTextArea(dslEditor);
        editorPanel.add(dslHeaderBar, BorderLayout.NORTH);
        editorPanel.add(searchableDslArea, BorderLayout.CENTER);

        // Vùng kết quả (Tab: cái bàn + sự polyme hóa + nguyên bản JSON）
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);

        // Thanh tiêu đề đáp ứng (MigLayout）：tiêu đề + hits số liệu thống kê + trạng thái
        JPanel respHeader = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[][]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(respHeader);
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESPONSE_TITLE));
        respLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));

        hitsInfoLabel = new JLabel("");
        hitsInfoLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        hitsInfoLabel.setForeground(ModernColors.getPrimary());
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        respHeader.add(respLabel);
        respHeader.add(hitsInfoLabel);
        respHeader.add(respStatusLabel);

        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(resultTabs);
        enhancedTable = new EnhancedTablePanel(new String[]{});
        aggTable = new EnhancedTablePanel(new String[]{});
        // Tham khảo ResponseBodyPanel：Không có sẵn để sử dụng biên tập chỉnh sửa SearchableTextArea(area, false) Bao bì (chỉ tìm kiếm)
        resultArea = createJsonEditor(false);
        resultArea.setLineWrap(false);
        resultArea.setHighlightCurrentLine(false);
        searchableResultArea = new SearchableTextArea(resultArea, false);

        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_TABLE), enhancedTable);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_AGG), aggTable);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_RAW), searchableResultArea);

        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultTabs, BorderLayout.CENTER);

        JSplitPane editorSplit = AppToolWindowChrome.createVerticalInnerSplitPane(
                editorPanel,
                resultPanel,
                260
        );
        editorSplit.setResizeWeight(0.4);

        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Method Hiển thị màu thả xuống
     */
    private static class MethodComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (!isSelected && value instanceof String method) {
                Color c = switch (method) {
                    case "POST" -> ModernColors.getWarningDark();
                    case "PUT" -> ModernColors.getInfo();
                    case HTTP_DELETE -> ModernColors.getError();
                    case "HEAD" -> ModernColors.getSecondary();
                    default -> ModernColors.getSuccess(); // GET
                };
                lbl.setForeground(c);
                lbl.setFont(FontsUtil.getDefaultFont(Font.BOLD));
            }
            return lbl;
        }
    }

    private void registerCtrlEnterShortcut(JButton btn) {
        AbstractAction executeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btn.isEnabled()) {
                    btn.doClick();
                }
            }
        };
        dslEditor.getActionMap().put("executeRequest", executeAction);
        dslEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "executeRequest");
        dslEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), "executeRequest");
    }

    // ===== Các phương pháp chức năng cốt lõi =====

    private void deleteIndices(List<String> indices) {
        List<String> targets = normalizeIndexTargets(indices);
        if (!connected || targets.isEmpty()) return;

        String confirm = targets.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM), targets.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_BATCH_CONFIRM), targets.size());
        int opt = JOptionPane.showConfirmDialog(this, confirm,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runIndexMutation(targets, true,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_FAILED);
    }

    private void clearIndices(List<String> indices) {
        List<String> targets = normalizeIndexTargets(indices);
        if (!connected || targets.isEmpty()) return;

        String confirm = targets.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM), targets.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_BATCH_CONFIRM), targets.size());
        int opt = JOptionPane.showConfirmDialog(this, confirm,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runIndexMutation(targets, false,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_FAILED);
    }

    private List<String> normalizeIndexTargets(List<String> indices) {
        if (indices == null || indices.isEmpty()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String idx : indices) {
            if (idx != null) {
                String t = idx.trim();
                if (!t.isBlank()) set.add(t);
            }
        }
        return new ArrayList<>(set);
    }

    private void runIndexMutation(List<String> targets, boolean deleteIndex,
                                  String singleSuccessKey, String batchSuccessKey, String failedKey) {
        List<String> finalTargets = new ArrayList<>(targets);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String lastResp = "";
                for (String indexName : finalTargets) {
                    if (deleteIndex) {
                        lastResp = doDelete("/" + indexName, "");
                    } else {
                        lastResp = doPost("/" + indexName + "/_delete_by_query",
                                "{\n  \"query\": {\n    \"match_all\": {}\n  }\n}");
                    }
                }
                return lastResp;
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    loadIndices();
                    if (finalTargets.size() == 1) {
                        NotificationCenter.showSuccess(
                                MessageFormat.format(I18nUtil.getMessage(singleSuccessKey), finalTargets.get(0)));
                    } else {
                        NotificationCenter.showSuccess(
                                MessageFormat.format(I18nUtil.getMessage(batchSuccessKey), finalTargets.size()));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(failedKey), ex.getMessage()));
                }
            }
        }.execute();
    }

    private record ConnectionParams(String baseUrl, String authHeader) {
    }

    /**
     * Đọc và xác minh các tham số kết nối của biểu mẫu hiện tại; nhắc và quay lại khi địa chỉ trống null。
     */
    private ConnectionParams readConnectionParams() {
        String url = getCurrentHost();
        if (url.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_HOST_REQUIRED));
            return null;
        }
        url = ElasticsearchConnectionProfileStore.normalizeBaseUrl(url);
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        String header = getSelectedAuthMode() == AuthMode.BASIC && !user.isEmpty()
                ? "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8))
                : null;
        return new ConnectionParams(url, header);
    }

    /**
     * yêu cầu _cluster/health Xác minh kết nối; không 2xx（Chẳng hạn như 401 Lỗi xác thực) được coi là lỗi và trạng thái kết nối hiện tại không được sửa đổi.
     */
    private ResponseWrapper probeCluster(ConnectionParams params) throws IOException {
        Request.Builder builder = new Request.Builder().url(params.baseUrl() + CLUSTER_HEALTH_PATH).get();
        if (params.authHeader() != null) builder.header(HEADER_AUTHORIZATION, params.authHeader());
        OkHttpClient client = OkHttpClientManager.getClientForUrl(
                params.baseUrl(), true, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, WRITE_TIMEOUT_MS);
        try (Response resp = client.newCall(builder.build()).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                String detail = body.isBlank() ? resp.message() : abbreviate(body, 300);
                throw new IOException("HTTP " + resp.code() + " " + detail);
            }
            return new ResponseWrapper(resp.code(), body);
        }
    }

    private static String clusterStatusOf(String healthJson) {
        try {
            JsonNode node = JsonUtil.readTree(healthJson);
            JsonNode status = node == null ? null : node.get("status");
            return status == null || status.isNull() ? "-" : status.asString();
        } catch (Exception ignored) {
            return "-";
        }
    }

    private static String abbreviate(String text, int maxLength) {
        String value = text == null ? "" : text.strip();
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "…";
    }

    private static String errorMessage(Exception ex) {
        Throwable cause = ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null
                ? ex.getCause() : ex;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    /**
     * Nó chỉ xác minh xem cấu hình biểu mẫu hiện tại có thể được kết nối hay không mà không cần tải chỉ mục hoặc thay đổi trạng thái được kết nối.
     */
    private void testConnection() {
        ConnectionParams params = readConnectionParams();
        if (params == null) {
            return;
        }
        setConnectionBusy(true);
        testConnectionBtn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION_RUNNING));
        final long startNanos = System.nanoTime();
        new SwingWorker<ResponseWrapper, Void>() {
            @Override
            protected ResponseWrapper doInBackground() throws Exception {
                return probeCluster(params);
            }

            @Override
            protected void done() {
                setConnectionBusy(false);
                testConnectionBtn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION));
                long costMs = (System.nanoTime() - startNanos) / 1_000_000L;
                try {
                    ResponseWrapper resp = get();
                    NotificationCenter.showSuccess(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION_SUCCESS),
                            params.baseUrl(), clusterStatusOf(resp.body()), String.valueOf(costMs)));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TEST_CONNECTION_FAILED), errorMessage(ex)));
                    log.warn("Test elasticsearch connection failed", ex);
                }
            }
        }.execute();
    }

    private void doConnect() {
        ConnectionParams params = readConnectionParams();
        if (params == null) {
            return;
        }
        setConnectionBusy(true);
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_CONNECTING));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        new SwingWorker<ResponseWrapper, Void>() {
            @Override
            protected ResponseWrapper doInBackground() throws Exception {
                return probeCluster(params);
            }

            @Override
            protected void done() {
                setConnectionBusy(false);
                try {
                    ResponseWrapper resp = get();
                    baseUrl = params.baseUrl();
                    authHeader = params.authHeader();
                    updateConnectedState(true);
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_CONNECTED), baseUrl));
                    respStatusLabel.setForeground(ModernColors.getSuccess());
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp.body()));
                    resultArea.setCaretPosition(0);
                    addHostHistory(baseUrl);
                    loadIndices();
                    NotificationCenter.showSuccess(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT_SUCCESS), baseUrl));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    updateConnectedState(false);
                    respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
                    respStatusLabel.setForeground(ModernColors.getTextSecondary());
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_CONNECT_FAILED), errorMessage(ex)));
                    log.warn("Elasticsearch connect failed", ex);
                }
            }
        }.execute();
    }

    private void doDisconnect() {
        baseUrl = "http://localhost:9200";
        authHeader = null;
        indexListModel.clear();
        indexFilteredModel.clear();
        indexDocCountMap.clear();
        updateConnectedState(false);
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        hitsInfoLabel.setText("");
        NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT_SUCCESS));
    }

    /**
     * kết nối/Loại trừ lẫn nhau khi kết nối thử nghiệm đang diễn ra, để tránh các lần nhấp chuột lặp lại kích hoạt các kết nối đồng thời.
     */
    private void setConnectionBusy(boolean busy) {
        connectBtn.setEnabled(!busy);
        testConnectionBtn.setEnabled(!busy);
    }

    private void setLoadingIndices(boolean loading) {
        loadingIndices = loading;
        refreshIndicesBtn.setEnabled(connected && !loading);
        indexList.repaint();
    }

    private void updateConnectedState(boolean isConnected) {
        connected = isConnected;
        btnCardLayout.show(btnCard, isConnected ? DISCONNECT_CARD : CONNECT_CARD);
        refreshIndicesBtn.setEnabled(isConnected && !loadingIndices);
        createIndexBtn.setEnabled(isConnected);
        executeBtn.setEnabled(isConnected);
        indexList.repaint();
    }

    /**
     * Lấy lựa chọn hiện tại/đã vào host
     */
    private String getCurrentHost() {
        return hostField.getText().trim();
    }

    /**
     * Sẽ host Ghi lại vào ô nhập địa chỉ, lịch sử được thay đổi so với hiện tại profile cứu.
     */
    private void addHostHistory(String host) {
        host = ElasticsearchConnectionProfileStore.normalizeBaseUrl(host);
        hostField.setText(host);
    }

    private void loadIndices() {
        if (!connected) return;
        String previousSelected = indexList == null ? null : indexList.getSelectedValue();
        String currentPath = pathField == null ? "" : pathField.getText().trim();
        boolean shouldAutoApplyTemplate = currentPath.isBlank() || CLUSTER_HEALTH_PATH.equals(currentPath);
        setLoadingIndices(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return doGet("/_cat/indices?v&format=json&s=index", "");
            }

            @Override
            protected void done() {
                setLoadingIndices(false);
                try {
                    parseAndFillIndexList(get());
                    filterIndices(indexSearchText());
                    if (!indexFilteredModel.isEmpty()) {
                        int restoreIndex = -1;
                        if (previousSelected != null && !previousSelected.isBlank()) {
                            for (int i = 0; i < indexFilteredModel.size(); i++) {
                                if (previousSelected.equals(indexFilteredModel.get(i))) {
                                    restoreIndex = i;
                                    break;
                                }
                            }
                        }
                        indexList.setSelectedIndex(restoreIndex >= 0 ? restoreIndex : 0);
                        if (shouldAutoApplyTemplate) {
                            applyTemplate(0);
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.warn("Failed to load indices: {}", ex.getMessage());
                    NotificationCenter.showError(errorMessage(ex));
                }
            }
        }.execute();
    }

    private void applyTemplate(int idx) {
        if (idx < 0 || idx >= DSL_TEMPLATES.length) return;
        String selectedIndex = indexList.getSelectedValue();
        String path = DSL_TEMPLATES[idx][2];
        if (selectedIndex != null && !selectedIndex.isBlank()) {
            path = path.replace("{index}", selectedIndex);
        }
        methodCombo.setSelectedItem(DSL_TEMPLATES[idx][1]);
        pathField.setText(path);
        dslEditor.setText(DSL_TEMPLATES[idx][3]);
        dslEditor.setCaretPosition(0);
    }

    private void createIndex() {
        if (!connected) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_NOT_CONNECTED));
            return;
        }
        String name = newIndexField.getText().trim();
        if (name.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_REQUIRED));
            return;
        }
        int shards = shardSpinner.getCommittedIntValue();
        int replicas = replicaSpinner.getCommittedIntValue();
        String body = "{\n  \"settings\": {\n    \"number_of_shards\": " + shards
                + ",\n    \"number_of_replicas\": " + replicas + "\n  }\n}";
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return doPut("/" + name, body);
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(JsonUtil.toJsonPrettyStr(get()));
                    resultArea.setCaretPosition(0);
                    newIndexField.setText("");
                    loadIndices();
                    NotificationCenter.showSuccess(
                            MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_SUCCESS), name));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_FAILED), ex.getMessage()));
                }
            }
        }.execute();
    }

    private void executeRequest() {
        if (!connected) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_NOT_CONNECTED));
            return;
        }
        String selectedMethod = (String) methodCombo.getSelectedItem();
        final String method = (selectedMethod == null) ? "GET" : selectedMethod;
        String path = normalizePath(pathField.getText().trim());
        if (path.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_PATH_REQUIRED));
            return;
        }
        pathField.setText(path);
        String body = dslEditor.getText().trim();

        // Tham gia lịch sử
        addToHistory(method, path, body);

        clearTable();
        clearAggTable();
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_REQUESTING));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        hitsInfoLabel.setText("");
        executeBtn.setEnabled(false);
        long start = System.currentTimeMillis();

        new SwingWorker<ResponseWrapper, Void>() {
            @Override
            protected ResponseWrapper doInBackground() throws Exception {
                return switch (method) {
                    case "POST" -> doPostWithCode(path, body);
                    case "PUT" -> doPutWithCode(path, body);
                    case HTTP_DELETE -> doDeleteWithCode(path, body);
                    case "HEAD" -> doHeadWithCode(path);
                    default -> doGetWithCode(path, body);
                };
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - start;
                executeBtn.setEnabled(connected);
                try {
                    handleRequestResult(get(), elapsed);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    respStatusLabel.setText("");
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    resultArea.setText("Error: " + msg);
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_ERROR), elapsed));
                    respStatusLabel.setForeground(ModernColors.getError());
                }
            }
        }.execute();
    }

    /**
     * Chứa HTTP Trình bao bọc phản hồi cho mã trạng thái
     */
    private record ResponseWrapper(int code, String body) {
    }

    private void handleRequestResult(ResponseWrapper rw, long elapsed) {
        String resp = rw.body();
        String formatted = JsonUtil.isTypeJSON(resp) ? JsonUtil.toJsonPrettyStr(resp) : resp;
        resultArea.setText(formatted);
        resultArea.setCaretPosition(0);

        // Màu mã trạng thái
        int code = rw.code();
        Color statusColor;
        if (code >= 200 && code < 300) statusColor = ModernColors.getSuccess();
        else if (code >= 400 && code < 500) statusColor = ModernColors.getWarningDark();
        else if (code >= 500) statusColor = ModernColors.getError();
        else statusColor = ModernColors.getTextSecondary();

        String codeStr = code > 0 ? code + " · " : "";
        String statusKey = code >= 200 && code < 300
                ? MessageKeys.TOOLBOX_ES_STATUS_OK
                : MessageKeys.TOOLBOX_ES_STATUS_ERROR;
        respStatusLabel.setText(codeStr + MessageFormat.format(
                I18nUtil.getMessage(statusKey), elapsed));
        respStatusLabel.setForeground(statusColor);

        // Điền vào mẫu đơn + sự polyme hóa
        if (JsonUtil.isTypeJSON(resp)) {
            populateTable(resp);
            populateAggTable(resp);
        }

        // totalHits số liệu thống kê
        Object totalHits = enhancedTable.getClientProperty(CLIENT_PROP_TOTAL_HITS);
        int tableRows = enhancedTable.getTable().getRowCount();
        if (totalHits instanceof Long th) {
            hitsInfoLabel.setText(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_HITS), tableRows, th));
        } else {
            hitsInfoLabel.setText("");
        }

        // Tự động chọn tốt nhất Tab
        boolean hasAgg = aggTable.getTable().getRowCount() > 0;
        boolean hasHits = enhancedTable.getTable().getRowCount() > 0;
        if (hasAgg) resultTabs.setSelectedIndex(1);
        else if (hasHits) resultTabs.setSelectedIndex(0);
        else resultTabs.setSelectedIndex(2);
    }

    // ===== Đổ đầy bàn =====

    private void populateTable(String json) {
        try {
            JsonNode root = JsonUtil.readTree(json);
            if (root.has("hits")) {
                populateHitsTable(root);
            } else if (json.trim().startsWith("[")) {
                populateArrayTable(json);
            } else {
                List<Object[]> rows = new ArrayList<>();
                flattenJson("", root, rows);
                if (!rows.isEmpty()) rebuildEnhancedTable(new String[]{"Key", "Value"}, rows);
                else enhancedTable.clearData();
            }
        } catch (Exception e) {
            log.debug("populateTable error (non-fatal): {}", e.getMessage());
            enhancedTable.clearData();
        }
    }

    private void populateHitsTable(JsonNode root) {
        JsonNode hits = root.get("hits");
        JsonNode hitsArr = hits.get("hits");
        if (hitsArr == null || !hitsArr.isArray() || hitsArr.isEmpty()) {
            enhancedTable.clearData();
            return;
        }
        long totalHits = parseTotalHits(hits.get("total"));
        List<String> colNames = buildHitColumns(hitsArr);
        List<Object[]> rows = buildHitRows(hitsArr, colNames);
        rebuildEnhancedTable(colNames.toArray(new String[0]), rows);
        enhancedTable.putClientProperty(CLIENT_PROP_TOTAL_HITS, totalHits > rows.size() ? totalHits : null);
    }

    /**
     * phân tích aggregations Các nút và  "name | type | buckets/value" Hiển thị lát gạch
     */
    private void populateAggTable(String json) {
        try {
            JsonNode root = JsonUtil.readTree(json);
            JsonNode aggs = root.get("aggregations");
            if (aggs == null || !aggs.isObject()) {
                aggTable.clearData();
                return;
            }

            List<Object[]> rows = new ArrayList<>();
            flattenAggregations("", aggs, rows);
            if (rows.isEmpty()) {
                aggTable.clearData();
                return;
            }
            rebuildAggTable(new String[]{"Aggregation", "Key", "Value"}, rows);
        } catch (Exception e) {
            log.debug("populateAggTable error: {}", e.getMessage());
            aggTable.clearData();
        }
    }

    private void flattenAggregations(String prefix, JsonNode node, List<Object[]> rows) {
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            String aggName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonNode aggNode = entry.getValue();
            // buckets gõ (terms、range、date_histogram Chờ đợi)
            JsonNode buckets = aggNode.get(AGG_BUCKETS);
            if (buckets != null && buckets.isArray()) {
                for (JsonNode bucket : buckets) {
                    JsonNode keyNode = bucket.get(AGG_KEY_AS_STRING) != null
                            ? bucket.get(AGG_KEY_AS_STRING) : bucket.get(AGG_KEY);
                    String key = nodeText(keyNode);
                    String docCount = nodeText(bucket.get(AGG_DOC_COUNT));
                    rows.add(new Object[]{aggName, key, docCount});
                    // Lồng nhau sub-aggregations
                    for (Map.Entry<String, JsonNode> sub : bucket.properties()) {
                        String sk = sub.getKey();
                        if (!sk.startsWith("_") && !sk.equals(AGG_DOC_COUNT)
                                && !sk.equals(AGG_KEY) && !sk.equals(AGG_KEY_AS_STRING)) {
                            flattenAggregations(aggName + "[" + key + "]." + sk, sub.getValue(), rows);
                        }
                    }
                }
            } else if (aggNode.has(AGG_VALUE)) {
                // metric gõ (sum、avg、max、min Chờ đợi)
                rows.add(new Object[]{aggName, AGG_VALUE, nodeText(aggNode.get(AGG_VALUE))});
            } else if (aggNode.has(AGG_DOC_COUNT)) {
                // filter loại
                rows.add(new Object[]{aggName, AGG_DOC_COUNT, nodeText(aggNode.get(AGG_DOC_COUNT))});
            }
        }
    }

    private static long parseTotalHits(JsonNode totalNode) {
        if (totalNode == null) return -1;
        if (totalNode.isNumber()) return totalNode.longValue();
        if (totalNode.isObject() && totalNode.has("value")) return totalNode.get("value").longValue();
        return -1;
    }

    private static List<String> buildHitColumns(JsonNode hitsArr) {
        LinkedHashSet<String> srcCols = new LinkedHashSet<>();
        for (JsonNode hit : hitsArr) {
            JsonNode src = hit.get("_source");
            if (src != null) src.properties().forEach(e -> srcCols.add(e.getKey()));
        }
        List<String> colNames = new ArrayList<>();
        colNames.add("_index");
        colNames.add("_id");
        colNames.add("_score");
        colNames.addAll(srcCols);
        return colNames;
    }

    private static List<Object[]> buildHitRows(JsonNode hitsArr, List<String> colNames) {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode hit : hitsArr) rows.add(buildHitRow(hit, colNames));
        return rows;
    }

    private static Object[] buildHitRow(JsonNode hit, List<String> colNames) {
        JsonNode src = hit.get("_source");
        Object[] row = new Object[colNames.size()];
        row[0] = nodeText(hit.get("_index"));
        row[1] = nodeText(hit.get("_id"));
        row[2] = nodeText(hit.get("_score"));
        if (src != null) {
            for (int c = 3; c < colNames.size(); c++) row[c] = nodeText(src.get(colNames.get(c)));
        }
        return row;
    }

    private void populateArrayTable(String json) {
        try {
            JsonNode arr = JsonUtil.readTree(json);
            if (!arr.isArray() || arr.isEmpty()) return;
            JsonNode first = arr.get(0);
            List<String> colNames = new ArrayList<>();
            for (Map.Entry<String, JsonNode> e : first.properties()) colNames.add(e.getKey());
            List<Object[]> rows = new ArrayList<>();
            for (JsonNode obj : arr) {
                Object[] row = new Object[colNames.size()];
                for (int c = 0; c < colNames.size(); c++) row[c] = nodeText(obj.get(colNames.get(c)));
                rows.add(row);
            }
            rebuildEnhancedTable(colNames.toArray(new String[0]), rows);
        } catch (Exception e) {
            log.debug("populateArrayTable error: {}", e.getMessage());
        }
    }

    private void flattenJson(String prefix, JsonNode obj, List<Object[]> rows) {
        for (Map.Entry<String, JsonNode> entry : obj.properties()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonNode v = entry.getValue();
            if (v.isObject()) flattenJson(fullKey, v, rows);
            else rows.add(new Object[]{fullKey, nodeText(v)});
        }
    }

    private void rebuildEnhancedTable(String[] cols, List<Object[]> rows) {
        enhancedTable.resetAndSetData(cols, rows);
    }

    private void rebuildAggTable(String[] cols, List<Object[]> rows) {
        aggTable.resetAndSetData(cols, rows);
    }

    private void clearTable() {
        enhancedTable.clearData();
    }

    private void clearAggTable() {
        aggTable.clearData();
    }

    private static String nodeText(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.isValueNode() ? node.toString().replace("\"", "") : node.toString();
    }

    private void formatDsl() {
        String txt = dslEditor.getText().trim();
        if (txt.isEmpty()) return;
        if (JsonUtil.isTypeJSON(txt)) {
            dslEditor.setText(JsonUtil.toJsonPrettyStr(txt));
            dslEditor.setCaretPosition(0);
        }
    }

    private void copyResult() {
        String txt = resultArea.getText();
        if (txt != null && !txt.isEmpty())
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txt), null);
    }

    // ===== HTTP Đóng gói phương thức (return ResponseWrapper bao gồm cả mã trạng thái)=====

    private ResponseWrapper doGetWithCode(String path, String body) throws IOException {
        if (body != null && !body.isEmpty()) {
            RequestBody rb = RequestBody.create(body, MediaType.get(JSON_UTF8));
            return executeHttpWithCode(buildRequest("POST", path, rb));
        }
        return executeHttpWithCode(buildRequest("GET", path, null));
    }

    private ResponseWrapper doPostWithCode(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttpWithCode(buildRequest("POST", path, rb));
    }

    private ResponseWrapper doPutWithCode(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], MediaType.get(JSON_MIME))
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttpWithCode(buildRequest("PUT", path, rb));
    }

    private ResponseWrapper doDeleteWithCode(String path, String body) throws IOException {
        RequestBody rb = (body != null && !body.isEmpty())
                ? RequestBody.create(body, MediaType.get(JSON_UTF8)) : null;
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path));
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        if (rb != null) builder.delete(rb);
        else builder.delete();
        return executeHttpWithCode(builder.build());
    }

    private ResponseWrapper doHeadWithCode(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path)).head();
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        return executeHttpWithCode(builder.build());
    }

    // ===== nguyên bản String HTTP Phương thức (đối với nội bộ không executeRequest gọi)=====
    private String doGet(String path, String body) throws IOException {
        return doGetWithCode(path, body).body();
    }

    private String doPost(String path, String body) throws IOException {
        return doPostWithCode(path, body).body();
    }

    private String doPut(String path, String body) throws IOException {
        return doPutWithCode(path, body).body();
    }

    private String doDelete(String path, String body) throws IOException {
        return doDeleteWithCode(path, body).body();
    }

    private Request buildRequest(String method, String path, RequestBody body) {
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path));
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        builder.method(method, body);
        return builder.build();
    }

    private ResponseWrapper executeHttpWithCode(Request req) throws IOException {
        try (Response resp = getHttpClient().newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful() && body.isEmpty()) {
                throw new IOException("HTTP " + resp.code() + " " + resp.message());
            }
            return new ResponseWrapper(resp.code(), body);
        }
    }

    private OkHttpClient getHttpClient() {
        return OkHttpClientManager.getClientForUrl(
                baseUrl,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
    }

    private String normalizePath(String path) {
        if (path == null) return "";
        String trimmed = path.trim();
        if (trimmed.isBlank()) return "";
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    // ===== Phương pháp công cụ =====

    private RSyntaxTextArea createJsonEditor(boolean editable) {
        RSyntaxTextArea area = new FallbackAwareRSyntaxTextArea(10, 60);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setEditable(editable);
        EditorThemeUtil.loadTheme(area);
        return area;
    }
}
