package com.laker.postman.panel.collections.editor;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.RequestAuthTypes;
import com.laker.postman.request.model.HttpHeader;


import cn.hutool.core.collection.CollUtil;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyVariablesPanel;
import com.laker.postman.panel.collections.editor.request.sub.ScriptPanel;
import com.laker.postman.http.request.AppRequestHeaderDefaults;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_INHERIT;
import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_NONE;

/**
 * Bảng chỉnh sửa nhóm - Phiên bản hiện đại hóa
 * Hiển thị trong bảng điều khiển chính bên phải, để tham khảo Postman thiết kế
 * Tái sử dụng AuthTabPanel và ScriptPanel thành phần
 * <p>
 * Mô tả thiết kế:
 * - Tham khảo Postman，Với chế độ tự động lưu ngay lập tức, không cần phải có nút lưu rõ ràng
 * - Khi dữ liệu thay đổi, nó sẽ tự động được lưu vào mô hình và được duy trì
 * - Không cần theo dõi dữ liệu bẩn và cảnh báo chấm đỏ (tệp Postman của Collection/Folder Không có dấu chấm màu đỏ trong cài đặt)
 */
public class GroupEditPanel extends JPanel {
    @Getter
    private final DefaultMutableTreeNode groupNode;
    @Getter
    private final RequestGroup group;
    private final Runnable onSave;

    // UI Components
    private JTextField nameField;
    private MarkdownEditorPanel descriptionEditor;
    private AuthTabPanel authTabPanel;
    private ScriptPanel scriptPanel;
    private EasyRequestHttpHeadersPanel headersPanel;
    private EasyVariablesPanel variablesPanel;
    private JTabbedPane tabbedPane;

    // Tab indicators for showing content status
    private IndicatorTabComponent headersTabIndicator;
    private IndicatorTabComponent variablesTabIndicator;
    private IndicatorTabComponent authTabIndicator;
    private IndicatorTabComponent scriptsTabIndicator;

    // Ảnh chụp nhanh dữ liệu thô để phát hiện các thay đổi
    private String originalName;
    private String originalDescription;
    private String originalAuthType;
    private String originalAuthUsername;
    private String originalAuthPassword;
    private String originalAuthToken;
    private String originalAuthApiKeyName;
    private String originalAuthApiKeyValue;
    private String originalAuthApiKeyPlacement;
    private String originalPrescript;
    private String originalPostscript;
    private List<HttpHeader> originalHeaders;
    private List<Variable> originalVariables;

    // Hẹn giờ chống rung
    private Timer autoSaveTimer;
    private static final int AUTO_SAVE_DELAY = 300; // 300ms Lưu chậm (cân bằng khả năng phản hồi và hiệu suất)

    public GroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group, Runnable onSave) {
        this.groupNode = groupNode;
        this.group = group;
        this.onSave = onSave;
        initAutoSaveTimer();
        initUI();
        loadGroupData();
        setupAutoSaveListeners();
        setupTabIndicatorListeners();
        setupSaveShortcut();

        // khởi tạotabTrạng thái chỉ báo
        SwingUtilities.invokeLater(this::updateTabIndicators);
    }

    private void initAutoSaveTimer() {
        // Tạo bộ hẹn giờ chống rung để tránh lưu thường xuyên
        autoSaveTimer = new Timer(AUTO_SAVE_DELAY, e -> {
            autoSaveGroupData(false);  // Bộ hẹn giờ tự động kích hoạt và không ngừng chỉnh sửa
            autoSaveTimer.stop();
        });
        autoSaveTimer.setRepeats(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        styleCardSurface(this);

        // Thanh tiêu đề hàng đầu
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Khu vực nội dung trung bình - sử dụng Tabs
        tabbedPane = new JTabbedPane();
        styleCardSurface(tabbedPane);
        tabbedPane.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));

        // General Tab
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TAB_GENERAL), createGeneralPanel());

        // Headers Tab - Cấu hình tiêu đề yêu cầu công khai
        headersPanel = new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy());
        JPanel headersWrapperPanel = new JPanel(new BorderLayout());
        styleCardSurface(headersWrapperPanel);
        headersWrapperPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headersWrapperPanel.add(headersPanel, BorderLayout.CENTER);

        JPanel headersInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        styleCardSurface(headersInfoPanel);
        JLabel headersInfoLabel = createInfoLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_HEADERS_INFO), 11);
        headersInfoPanel.add(headersInfoLabel);
        headersWrapperPanel.add(headersInfoPanel, BorderLayout.SOUTH);

        headersTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_HEADERS));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_HEADERS), headersWrapperPanel);
        tabbedPane.setTabComponentAt(1, headersTabIndicator);

        // Variables Tab - Các biến cấp độ nhóm
        variablesPanel = new EasyVariablesPanel();
        JPanel variablesWrapperPanel = new JPanel(new BorderLayout());
        styleCardSurface(variablesWrapperPanel);
        variablesWrapperPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        variablesWrapperPanel.add(variablesPanel, BorderLayout.CENTER);

        JPanel variablesInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        styleCardSurface(variablesInfoPanel);
        JLabel variablesInfoLabel = createInfoLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_VARIABLES_INFO), 11);
        variablesInfoPanel.add(variablesInfoLabel);
        variablesWrapperPanel.add(variablesInfoPanel, BorderLayout.SOUTH);

        variablesTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_VARIABLES));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_VARIABLES), variablesWrapperPanel);
        tabbedPane.setTabComponentAt(2, variablesTabIndicator);


        // Authorization Tab - Tái sử dụng AuthTabPanel
        authTabPanel = new AuthTabPanel();
        JPanel authWrapperPanel = new JPanel(new BorderLayout());
        styleCardSurface(authWrapperPanel);
        authWrapperPanel.add(authTabPanel, BorderLayout.CENTER);

        JPanel authInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        styleCardSurface(authInfoPanel);
        JLabel authInfoLabel = createInfoLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_AUTH_INFO), 11);
        authInfoPanel.add(authInfoLabel);
        authWrapperPanel.add(authInfoPanel, BorderLayout.SOUTH);

        authTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authWrapperPanel);
        tabbedPane.setTabComponentAt(3, authTabIndicator);

        // Scripts Tab - Tái sử dụng ScriptPanel
        scriptPanel = new ScriptPanel();
        JPanel scriptWrapperPanel = new JPanel(new BorderLayout());
        styleCardSurface(scriptWrapperPanel);
        scriptWrapperPanel.add(scriptPanel, BorderLayout.CENTER);

        JPanel scriptInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        styleCardSurface(scriptInfoPanel);
        JLabel scriptInfoLabel = createInfoLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_SCRIPT_INFO), 11);
        scriptInfoPanel.add(scriptInfoLabel);
        scriptWrapperPanel.add(scriptInfoPanel, BorderLayout.SOUTH);

        scriptsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptWrapperPanel);
        tabbedPane.setTabComponentAt(4, scriptsTabIndicator);


        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        styleCardSurface(headerPanel);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Bên trái: tiêu đề
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +6));
        leftPanel.add(titleLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // Bộ chia đáy
        JSeparator separator = new JSeparator();
        separator.setForeground(ModernColors.getBorderLightColor());
        JPanel separatorPanel = new JPanel(new BorderLayout());
        separatorPanel.setOpaque(false);
        separatorPanel.add(separator, BorderLayout.SOUTH);
        headerPanel.add(separatorPanel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        styleCardSurface(panel);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        styleCardSurface(formPanel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_NAME_LABEL));
        nameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        nameField = new EasyTextField(30);
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1)); // Lớn hơn kích thước phông chữ tiêu chuẩn1Không
        formPanel.add(nameField, gbc);

        // Điền theo chiều ngang
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // Description Markdown Editor - Chiếm hai dòng (gridy 0 và 1)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;  // Chiếm tất cả các cột
        gbc.gridheight = 2; // Chiếm hai dòng
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        descriptionEditor = new MarkdownEditorPanel();
        // Sử dụng kích thước tối thiểu thay vì kích thước cố định để cho phép trình chỉnh sửa thích ứng với kích thước vùng chứa
        descriptionEditor.setMinimumSize(new Dimension(600, 250));
        descriptionEditor.setPreferredSize(new Dimension(800, 350));
        formPanel.add(descriptionEditor, gbc);

        // Description hint
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel hintLabel = createInfoLabel(
                I18nUtil.getMessage(MessageKeys.GROUP_EDIT_DESCRIPTION_PLACEHOLDER),
                10
        );
        formPanel.add(hintLabel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    private void styleCardSurface(JComponent component) {
        ToolWindowSurfaceStyle.applyCard(component);
    }

    private JLabel createInfoLabel(String message, int fontSize) {
        String hintColor = ModernColors.toHtmlColor(ModernColors.getTextHint());
        return new JLabel("<html><i style='color:" + hintColor + "; font-size:" + fontSize + "px;'>ℹ "
                + message
                + "</i></html>");
    }

    /**
     * Thiết lập trình nghe tự động lưu
     */
    private void setupAutoSaveListeners() {
        // Theo dõi các thay đổi của trường tên
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void removeUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void changedUpdate(DocumentEvent e) {
                triggerAutoSave();
            }
        });

        // Theo dõi các thay đổi của trường mô tả
        descriptionEditor.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void removeUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void changedUpdate(DocumentEvent e) {
                triggerAutoSave();
            }
        });

        // Giám sát các thay đổi của bảng xác thực
        authTabPanel.addDirtyListener(this::triggerAutoSave);

        // Giám sát các thay đổi của bảng tập lệnh
        scriptPanel.addDirtyListeners(this::triggerAutoSave);

        // Giám sát các thay đổi của bảng tiêu đề yêu cầu
        headersPanel.addTableModelListener(e -> triggerAutoSave());

        // Theo dõi những thay đổi trong bảng biến
        variablesPanel.addTableModelListener(e -> triggerAutoSave());
    }

    /**
     * cài đặt tab Người nghe chỉ báo
     */
    private void setupTabIndicatorListeners() {

        // Giám sát các thay đổi của bảng tiêu đề yêu cầu
        headersPanel.addTableModelListener(e -> updateTabIndicators());

        // Theo dõi những thay đổi trong bảng biến
        variablesPanel.addTableModelListener(e -> updateTabIndicators());

        // Giám sát các thay đổi của bảng xác thực
        authTabPanel.addDirtyListener(this::updateTabIndicators);

        // Giám sát các thay đổi của bảng tập lệnh
        scriptPanel.addDirtyListeners(this::updateTabIndicators);
    }

    /**
     * Cập nhật tất cả tab chỉ số nội dung
     */
    private void updateTabIndicators() {
        SwingUtilities.invokeLater(() -> {
            if (headersTabIndicator != null) {
                headersTabIndicator.setShowIndicator(hasHeadersContent());
            }
            if (variablesTabIndicator != null) {
                variablesTabIndicator.setShowIndicator(hasVariablesContent());
            }
            if (authTabIndicator != null) {
                authTabIndicator.setShowIndicator(hasAuthContent());
            }
            if (scriptsTabIndicator != null) {
                scriptsTabIndicator.setShowIndicator(hasScriptsContent());
            }
        });
    }


    /**
     * nghiên cứu Headers tab Có nội dung hay không
     */
    private boolean hasHeadersContent() {
        List<HttpHeader> headers = headersPanel.getHeadersListFromModel();
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        // Kiểm tra xem có giá trị nào không rỗng không header
        return headers.stream().anyMatch(header ->
                header.getKey() != null && !header.getKey().trim().isEmpty()
        );
    }

    /**
     * nghiên cứu Variables tab Có nội dung hay không
     */
    private boolean hasVariablesContent() {
        List<Variable> variables = variablesPanel.getVariableListFromModel();
        if (variables == null || variables.isEmpty()) {
            return false;
        }
        // Kiểm tra xem có biến nào không trống không
        return variables.stream().anyMatch(variable ->
                variable.getKey() != null && !variable.getKey().trim().isEmpty()
        );
    }

    /**
     * nghiên cứu Auth tab Có nội dung hay không
     */
    private boolean hasAuthContent() {
        String authType = authTabPanel.getAuthType();
        // Nếu loại xác thực không có "inherit" hoặc "none"，Nó được coi là có nội dung
        return authType != null &&
                !AUTH_TYPE_INHERIT.equals(authType) &&
                !AUTH_TYPE_NONE.equals(authType);
    }

    /**
     * nghiên cứu Scripts tab Có nội dung hay không
     */
    private boolean hasScriptsContent() {
        String prescript = scriptPanel.getPrescript();
        String postscript = scriptPanel.getPostscript();

        boolean hasPrescript = prescript != null && !prescript.trim().isEmpty();
        boolean hasPostscript = postscript != null && !postscript.trim().isEmpty();

        return hasPrescript || hasPostscript;
    }

    /**
     * Đặt phím tắt lưu (Command+S / Ctrl+S)
     */
    private void setupSaveShortcut() {
        // Nhận công cụ sửa đổi phím tắt (Mac sử dụng Command，được sử dụng bởi các hệ thống khác Ctrl）
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Tạo KeyStroke (Cmd+S / Ctrl+S)
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, modifierKey);

        // Liên kết các phím tắt với InputMap
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        String actionKey = "saveGroup";
        inputMap.put(saveKeyStroke, actionKey);

        // Xác định hành động phím tắt: lưu ngay lập tức (bỏ qua chống rung)
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Dừng bộ hẹn giờ chống rung
                if (autoSaveTimer.isRunning()) {
                    autoSaveTimer.stop();
                }
                // Hãy tập trung trước cell editor Di chuyển đến bảng điều khiển, kích hoạt terminateEditOnFocusLost
                // Cơ chế cho phép bạn chỉnh sửa cell Giá trị được gửi tới model，Sử dụng lại invokeLater Thực hiện lưu.
                GroupEditPanel.this.requestFocusInWindow();
                SwingUtilities.invokeLater(() -> {
                    autoSaveGroupData(true);
                    NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.SAVE_SUCCESS));
                });
            }
        });
    }

    /**
     * Kích hoạt tự động lưu (chống rung)
     */
    private void triggerAutoSave() {
        // Khởi động lại bộ hẹn giờ chống rung
        if (autoSaveTimer.isRunning()) {
            autoSaveTimer.restart();
        } else {
            autoSaveTimer.start();
        }
    }

    /**
     * Tự động lưu dữ liệu được nhóm
     *
     * @param forceSave true = Trình kích hoạt thủ công (Cmd+S），Gửi những gì bạn đang chỉnh sửa đầu tiên cell Lưu lại;
     *                  false = Bộ đếm thời gian được tự động kích hoạt và đọc trực tiếp model Đừng ngắt lời Tab điều hướng.
     */
    private void autoSaveGroupData(boolean forceSave) {
        if (forceSave) {
            // Lưu thủ công: Gửi nội dung hiện đang được chỉnh sửa cell，Đảm bảo đầu vào mới nhất cũng được lưu
            headersPanel.stopCellEditing();
            variablesPanel.stopCellEditing();
        }

        // Đầu tiên hãy kiểm tra xem có bất kỳ sửa đổi nào không
        if (!isModified()) {
            return; // Không sửa đổi, không cần lưu
        }

        String newName = nameField.getText().trim();

        // Xác minh tên
        if (newName.isEmpty()) {
            // Khôi phục tên gốc
            nameField.setText(group.getName());
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.GROUP_EDIT_NAME_EMPTY),
                    I18nUtil.getMessage(MessageKeys.GROUP_EDIT_VALIDATION_ERROR),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiểm tra xem tên đã thay đổi chưa
        boolean nameChanged = !safeEquals(group.getName(), newName);

        // Lưu dữ liệu vào mô hình
        group.setName(newName);
        group.setDescription(descriptionEditor.getText());
        group.setAuthType(authTabPanel.getAuthType());
        group.setAuthUsername(authTabPanel.getUsername());
        group.setAuthPassword(authTabPanel.getPassword());
        group.setAuthToken(authTabPanel.getToken());
        group.setAuthApiKeyName(authTabPanel.getApiKeyName());
        group.setAuthApiKeyValue(authTabPanel.getApiKeyValue());
        group.setAuthApiKeyPlacement(authTabPanel.getApiKeyPlacement());
        group.setPrescript(scriptPanel.getPrescript());
        group.setPostscript(scriptPanel.getPostscript());
        group.setHeaders(headersPanel.getHeadersListFromModel());
        group.setVariables(variablesPanel.getVariableListFromModel());

        // Thông báo rằng quá trình lưu đã hoàn tất (kích hoạt tính bền bỉ)
        if (onSave != null) {
            onSave.run();
        }
        UiSingletonFactory.getInstance(RequestEditorPanel.class).refreshSelectedRequestExecutionScope();

        // Nếu tên thay đổi, hãy cập nhật Tab tiêu đề
        if (nameChanged) {
            SwingUtilities.invokeLater(() -> UiSingletonFactory.getInstance(
                    RequestEditorPanel.class
            ).updateGroupTabTitle(this, newName));
        }

        // Cập nhật ảnh chụp nhanh dữ liệu gốc
        updateOriginalSnapshot();
    }

    /**
     * Kiểm tra xem dữ liệu đã được sửa đổi chưa
     */
    private boolean isModified() {
        if (!safeEquals(originalName, nameField.getText())) return true;
        if (!safeEquals(originalDescription, descriptionEditor.getText())) return true;
        if (!safeEquals(originalAuthType, authTabPanel.getAuthType())) return true;
        if (!safeEquals(originalAuthUsername, authTabPanel.getUsername())) return true;
        if (!safeEquals(originalAuthPassword, authTabPanel.getPassword())) return true;
        if (!safeEquals(originalAuthToken, authTabPanel.getToken())) return true;
        if (!safeEquals(originalAuthApiKeyName, authTabPanel.getApiKeyName())) return true;
        if (!safeEquals(originalAuthApiKeyValue, authTabPanel.getApiKeyValue())) return true;
        if (!safeEquals(originalAuthApiKeyPlacement, authTabPanel.getApiKeyPlacement())) return true;
        if (!safeEquals(originalPrescript, scriptPanel.getPrescript())) return true;
        if (!safeEquals(originalPostscript, scriptPanel.getPostscript())) return true;
        if (!headersEquals(originalHeaders, headersPanel.getHeadersListFromModel())) return true;
        if (!variablesEquals(originalVariables, variablesPanel.getVariableListFromModel())) return true;
        return false;
    }

    /**
     * So sánh chuỗi an toàn (xử lý null Tình trạng)
     */
    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * So sánh hai Headers Các danh sách có giống nhau không?
     */
    private boolean headersEquals(List<HttpHeader> a, List<HttpHeader> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            HttpHeader ha = a.get(i);
            HttpHeader hb = b.get(i);
            if (ha.isEnabled() != hb.isEnabled()) return false;
            if (!safeEquals(ha.getKey(), hb.getKey())) return false;
            if (!safeEquals(ha.getValue(), hb.getValue())) return false;
        }
        return true;
    }

    /**
     * So sánh hai Variables Các danh sách có giống nhau không?
     */
    private boolean variablesEquals(List<Variable> a,
                                    List<Variable> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Variable va = a.get(i);
            Variable vb = b.get(i);
            if (va.isEnabled() != vb.isEnabled()) return false;
            if (!safeEquals(va.getKey(), vb.getKey())) return false;
            if (!safeEquals(va.getValue(), vb.getValue())) return false;
        }
        return true;
    }

    /**
     * Cập nhật ảnh chụp nhanh dữ liệu gốc
     */
    private void updateOriginalSnapshot() {
        originalName = nameField.getText();
        originalDescription = descriptionEditor.getText();
        originalAuthType = authTabPanel.getAuthType();
        originalAuthUsername = authTabPanel.getUsername();
        originalAuthPassword = authTabPanel.getPassword();
        originalAuthToken = authTabPanel.getToken();
        originalAuthApiKeyName = authTabPanel.getApiKeyName();
        originalAuthApiKeyValue = authTabPanel.getApiKeyValue();
        originalAuthApiKeyPlacement = authTabPanel.getApiKeyPlacement();
        originalPrescript = scriptPanel.getPrescript();
        originalPostscript = scriptPanel.getPostscript();
        originalHeaders = new java.util.ArrayList<>(headersPanel.getHeadersListFromModel());
        originalVariables = new java.util.ArrayList<>(variablesPanel.getVariableListFromModel());
    }

    private void loadGroupData() {
        // Load general data
        nameField.setText(group.getName());
        descriptionEditor.setText(group.getDescription() != null ? group.getDescription() : "");

        // Load auth data using AuthTabPanel
        authTabPanel.setAuthType(group.getAuthType());
        authTabPanel.setUsername(group.getAuthUsername() != null ? group.getAuthUsername() : "");
        authTabPanel.setPassword(group.getAuthPassword() != null ? group.getAuthPassword() : "");
        authTabPanel.setToken(group.getAuthToken() != null ? group.getAuthToken() : "");
        authTabPanel.setApiKeyName(group.getAuthApiKeyName() != null ? group.getAuthApiKeyName() : "");
        authTabPanel.setApiKeyValue(group.getAuthApiKeyValue() != null ? group.getAuthApiKeyValue() : "");
        authTabPanel.setApiKeyPlacement(group.getAuthApiKeyPlacement());

        // Load script data using ScriptPanel
        scriptPanel.setPrescript(group.getPrescript() != null ? group.getPrescript() : "");
        scriptPanel.setPostscript(group.getPostscript() != null ? group.getPostscript() : "");

        // Load headers data using EasyRequestHttpHeadersPanel
        if (group.getHeaders() != null && !group.getHeaders().isEmpty()) {
            headersPanel.setHeadersList(group.getHeaders());
        }

        // Load variables data using EasyPostmanEnvironmentTablePanel
        if (group.getVariables() != null && !group.getVariables().isEmpty()) {
            variablesPanel.setVariableList(group.getVariables());
        }

        // nếu headers Có nhiều hơn thế 4 tiêu đề yêu cầu, tiêu đề thứ hai được chọn theo mặc định tab (Headers)
        if (group.getHeaders() != null && group.getHeaders().size() > 4) {
            tabbedPane.setSelectedIndex(1);
        }

        // nếu variables Có các biến trong , biến thứ ba được chọn theo mặc định tab (Variables)
        if (CollUtil.isNotEmpty(group.getVariables())) {
            tabbedPane.setSelectedIndex(2);
        }

        // Khởi tạo ảnh chụp nhanh dữ liệu thô
        updateOriginalSnapshot();
    }
}
