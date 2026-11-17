package com.laker.postman.panel.collections.editor.request.sub;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.variable.VariableParser;
import com.laker.postman.variable.VariableSegment;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.FallbackAwareRSyntaxTextArea;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.variable.VariableType;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.Caret;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Optional;

/**
 * yêu cầuBodyCác bảng độc lập liên quan được hỗ trợnone、form-data、x-www-form-urlencoded、raw
 */
@Slf4j
public class RequestBodyPanel extends JPanel {
    private static final int BINARY_FILE_FIELD_COLUMNS = 56;
    private static final int BINARY_FILE_FIELD_MAX_WIDTH = 640;
    private static final int BINARY_FILE_FIELD_MIN_WIDTH = 160;

    public static final String BODY_TYPE_NONE = RequestBodyTypes.BODY_TYPE_NONE;
    public static final String BODY_TYPE_FORM_DATA = RequestBodyTypes.BODY_TYPE_FORM_DATA;
    public static final String BODY_TYPE_FORM_URLENCODED = RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
    public static final String BODY_TYPE_RAW = RequestBodyTypes.BODY_TYPE_RAW;
    public static final String BODY_TYPE_BINARY = RequestBodyTypes.BODY_TYPE_BINARY;
    public static final String RAW_TYPE_JSON = RequestBodyTypes.RAW_TYPE_JSON;
    public static final String RAW_TYPE_TEXT = RequestBodyTypes.RAW_TYPE_TEXT;
    public static final String RAW_TYPE_XML = RequestBodyTypes.RAW_TYPE_XML;

    @Getter
    private EasyComboBox<String> bodyTypeComboBox;
    @Getter
    private EasyComboBox<String> rawTypeComboBox;
    @Getter
    private FormDataTablePanel formDataTablePanel;
    @Getter
    private FormUrlencodedTablePanel formUrlencodedTablePanel;
    @Getter
    private RSyntaxTextArea bodyArea;
    @Getter
    private JTextField binaryFilePathField;
    @Getter
    private JButton binaryBrowseButton;
    @Getter
    private JButton binaryClearButton;
    @Getter
    private JLabel binaryFileSummaryLabel;
    private Icon binaryEmptyIcon;
    private Icon binaryFileIcon;
    private Icon binaryWarningIcon;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_NONE;
    @Getter
    private WebSocketSendButton wsSendButton;
    private EditButton bulkEditButton;
    private FormatButton formatButton;
    private CompressButton compressButton;
    private GenerateModelButton generateModelButton;
    private RequestBodyBulkEditSupport bulkEditSupport;
    private final boolean isWebSocketMode;

    private Timer wsTimer; // Để gửi theo lịch trình
    private WebSocketTimedSendButton wsTimedSendButton; // Nút gửi theo lịch trình
    private JTextField wsIntervalField; // Hộp nhập khoảng thời gian
    private JCheckBox wsClearInputCheckBox; // Xóa hộp kiểm đầu vào
    private SearchableTextArea searchableTextArea; // Trình soạn thảo văn bản có chức năng tìm kiếm tích hợp (HTTPchế độ)
    private SearchButton searchButton; // Nút tìm kiếm (HTTPchế độ)
    private WrapToggleButton wrapButton; // Nút ngắt dòng (HTTPchế độ)
    private boolean editable = true;
    private final UndoManager bodyUndoManager = new UndoManager();

    private RequestBodyVariableAutocompleteController autocompleteController;

    @Setter
    private transient ActionListener wsSendActionListener; // Gọi lại gửi được tiêm bên ngoài

    public RequestBodyPanel(RequestItemProtocolEnum protocol) {
        this.isWebSocketMode = protocol.isWebSocketProtocol();
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        if (isWebSocketMode) {
            initWebSocketBodyPanel();
        } else {
            initHttpBodyPanel();
        }
    }

    /**
     * khởi tạo HTTP cách thức Body bảng điều khiển
     */
    private void initHttpBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(bodyTypePanel);
        JPanel topPanel = new JPanel();
        ToolWindowSurfaceStyle.applyCard(topPanel);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));

        String[] bodyTypes = new String[]{BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW, BODY_TYPE_BINARY};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        topPanel.add(bodyTypeComboBox);
        topPanel.add(Box.createHorizontalStrut(4));

        bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());
        topPanel.add(bulkEditButton);
        topPanel.add(Box.createHorizontalStrut(4));

        String[] rawTypes = {RAW_TYPE_JSON, RAW_TYPE_XML, RAW_TYPE_TEXT};
        rawTypeComboBox = new EasyComboBox<>(rawTypes, EasyComboBox.WidthMode.DYNAMIC);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        boolean showFormatControls = isBodyTypeRAW();
        rawTypeComboBox.setVisible(showFormatControls);
        topPanel.add(rawTypeComboBox);
        topPanel.add(Box.createHorizontalStrut(2));

        // Nút tìm kiếm - Bấm vào để bật lên SearchReplacePanel
        searchButton = new SearchButton();
        searchButton.addActionListener(e -> {
            if (searchableTextArea != null) {
                searchableTextArea.getTextArea().requestFocusInWindow();
                searchableTextArea.showSearch();
            }
        });
        topPanel.add(searchButton);
        topPanel.add(Box.createHorizontalStrut(1));

        // Nút ngắt dòng
        wrapButton = new WrapToggleButton();
        wrapButton.addActionListener(e -> toggleLineWrap());
        topPanel.add(wrapButton);
        topPanel.add(Box.createHorizontalStrut(1));

        formatButton = new FormatButton();
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        topPanel.add(formatButton);
        topPanel.add(Box.createHorizontalStrut(1));

        compressButton = new CompressButton();
        compressButton.addActionListener(e -> compressBody());
        compressButton.setVisible(isBodyTypeRAW());
        topPanel.add(compressButton);
        topPanel.add(Box.createHorizontalStrut(1));

        generateModelButton = new GenerateModelButton(I18nUtil.getMessage(MessageKeys.JSON_MODEL_GENERATOR_TITLE));
        generateModelButton.setVisible(isBodyTypeRAW());
        generateModelButton.addActionListener(e -> JsonModelGeneratorDialog.showDialog(this, getRawBody(), "Request"));
        topPanel.add(generateModelButton);
        topPanel.add(Box.createHorizontalGlue());

        bodyTypePanel.add(topPanel, BorderLayout.NORTH);

        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        ToolWindowSurfaceStyle.applyCard(bodyCardPanel);
        bodyCardPanel.add(createNonePanel(), BODY_TYPE_NONE);
        bodyCardPanel.add(createFormDataPanel(), BODY_TYPE_FORM_DATA);
        bodyCardPanel.add(createFormUrlencodedPanel(), BODY_TYPE_FORM_URLENCODED);
        bodyCardPanel.add(createRawPanel(), BODY_TYPE_RAW);
        bodyCardPanel.add(createBinaryPanel(), BODY_TYPE_BINARY);
        add(bodyCardPanel, BorderLayout.CENTER);
        bulkEditSupport = new RequestBodyBulkEditSupport(this, formDataTablePanel, formUrlencodedTablePanel);
        bodyCardLayout.show(bodyCardPanel, currentBodyType);
        updateBodyActionVisibility(currentBodyType);
    }

    /**
     * khởi tạo WebSocket cách thức Body bảng điều khiển
     */
    private void initWebSocketBodyPanel() {
        currentBodyType = BODY_TYPE_RAW;
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(bodyTypePanel);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_SEND_MESSAGE));
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_RAW};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(BODY_TYPE_RAW);
        bodyTypeComboBox.setVisible(false);
        leftPanel.add(bodyTypeComboBox);
        rawTypeComboBox = null;
        formatButton = null;
        compressButton = null;
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        ToolWindowSurfaceStyle.applyCard(bodyCardPanel);
        JPanel rawPanel = createRawPanel();
        bodyCardPanel.add(rawPanel, BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, BODY_TYPE_RAW);
        // WebSocketCác nút thao tác phía dưới
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        ToolWindowSurfaceStyle.applyCard(bottomPanel);
        wsClearInputCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_CHECKBOX_CLEAR));
        bottomPanel.add(wsClearInputCheckBox);
        bottomPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_TIMEOUT)));
        wsIntervalField = new JTextField("1000", 5); // Mặc định1000ms
        bottomPanel.add(wsIntervalField);

        wsTimedSendButton = new WebSocketTimedSendButton();
        wsTimedSendButton.addActionListener(e -> toggleWsTimer());
        bottomPanel.add(wsTimedSendButton);

        wsSendButton = new WebSocketSendButton();
        wsSendButton.addActionListener(e -> wsSendAndMaybeClear());
        bottomPanel.add(wsSendButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private boolean isBodyTypeRAW() {
        return BODY_TYPE_RAW.equals(currentBodyType);
    }

    private JPanel createNonePanel() {
        JPanel nonePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(nonePanel);
        nonePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_NONE), SwingConstants.CENTER), BorderLayout.CENTER);
        return nonePanel;
    }

    private JPanel createFormDataPanel() {
        formDataTablePanel = new FormDataTablePanel();
        return formDataTablePanel;
    }

    private JPanel createFormUrlencodedPanel() {
        formUrlencodedTablePanel = new FormUrlencodedTablePanel();
        return formUrlencodedTablePanel;
    }

    private JPanel createRawPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        bodyArea = new VariableAwareSyntaxTextArea();
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS); // Mặc địnhJSONLàm nổi bật
        bodyArea.setCodeFoldingEnabled(true); // Kích hoạt tính năng gấp mã
        bodyArea.setLineWrap(false); // Tắt tính năng ngắt dòng để cải thiện hiệu suất văn bản lớn
        bodyArea.setShowMatchedBracketPopup(false);
        // Tải thống nhất các chủ đề, phông chữ soạn thảo và bản vẽ dự phòng cho các ký tự bị thiếu
        EditorThemeUtil.loadTheme(bodyArea);
        EditorThemeUtil.installViewportClippedTokenPainter(bodyArea);

        // ====== Đã thêm rút tiền/Làm lại chức năng ======
        bodyArea.getDocument().addUndoableEditListener(bodyUndoManager);

        // Undo
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        bodyArea.getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (bodyUndoManager.canUndo()) bodyUndoManager.undo();
                } catch (CannotUndoException ignored) {
                }
            }
        });

        // Redo
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        bodyArea.getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (bodyUndoManager.canRedo()) bodyUndoManager.redo();
                } catch (CannotRedoException ignored) {
                }
            }
        });

        // ====== Thêm chức năng tự động hoàn thành biến ======
        autocompleteController = new RequestBodyVariableAutocompleteController(this, bodyArea);
        autocompleteController.install();

        // sử dụng SearchableTextArea Bưu kiện bodyArea，Tích hợp chức năng tìm kiếm và thay thế
        searchableTextArea = new SearchableTextArea(bodyArea);
        panel.add(searchableTextArea, BorderLayout.CENTER);

        // ====== Các mẹo đánh dấu và di chuột có thể thay đổi ======
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            void repaintBadges() {
                if (bodyArea != null) {
                    bodyArea.repaint();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                repaintBadges();
            }

            public void removeUpdate(DocumentEvent e) {
                repaintBadges();
            }

            public void changedUpdate(DocumentEvent e) {
                repaintBadges();
            }
        });
        SwingUtilities.invokeLater(bodyArea::repaint);
        // Dấu nhắc di chuột
        bodyArea.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = bodyArea.viewToModel2D(e.getPoint());
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = VariableParser.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    if (pos >= seg.start && pos <= seg.end) {
                        String varName = seg.name;

                        // Nhận loại và giá trị biến
                        VariableType varType = VariableResolver.getVariableType(varName);
                        String varValue = VariableResolver.resolveVariable(varName);

                        if (varType != null && varValue != null) {
                            // Biến được xác định
                            bodyArea.setToolTipText(RequestBodyVariableTooltipBuilder.variableTooltip(varName, varValue, varType));
                        } else {
                            // Biến không được xác định
                            bodyArea.setToolTipText(RequestBodyVariableTooltipBuilder.undefinedVariableTooltip(varName));
                        }
                        return;
                    }
                }
                bodyArea.setToolTipText(null);
            }
        });
        // màn hình rawTypeComboBox Tùy chọn thay đổi, chuyển đổi kiểu đánh dấu
        if (rawTypeComboBox != null) {
            rawTypeComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selected = (String) e.getItem();
                    switch (selected) {
                        case RAW_TYPE_JSON:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
                            break;
                        case RAW_TYPE_XML:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                            break;
                        case RAW_TYPE_TEXT:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                            break;
                        default:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                    }
                    updateGenerateModelButtonState();
                }
            });
        }
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateGenerateModelButtonState(); }
            @Override public void removeUpdate(DocumentEvent event) { updateGenerateModelButtonState(); }
            @Override public void changedUpdate(DocumentEvent event) { updateGenerateModelButtonState(); }
        });
        return panel;
    }

    private JPanel createBinaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        JPanel filePickerPanel = new JPanel(new BorderLayout());
        filePickerPanel.setOpaque(false);

        binaryFilePathField = new JTextField();
        binaryFilePathField.setColumns(BINARY_FILE_FIELD_COLUMNS);
        binaryFilePathField.getAccessibleContext().setAccessibleName(
                I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_FILE));
        binaryFilePathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_PLACEHOLDER));
        capBinaryFilePathFieldWidth();

        binaryBrowseButton = new JButton(
                I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_BROWSE),
                IconUtil.createThemed("icons/file.svg", 16, 16)
        );
        binaryBrowseButton.setToolTipText(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_SELECT_FILE));
        binaryBrowseButton.addActionListener(e -> chooseBinaryFile());

        binaryClearButton = new ClearButton(16);
        binaryClearButton.setToolTipText(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_CLEAR_TOOLTIP));
        binaryClearButton.getAccessibleContext().setAccessibleName(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_CLEAR));
        binaryClearButton.addActionListener(e -> setBinaryFilePath(""));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(binaryBrowseButton);
        actionPanel.add(binaryClearButton);

        binaryFileSummaryLabel = new JLabel();
        binaryFileSummaryLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        binaryEmptyIcon = IconUtil.createThemed("icons/info.svg", 14, 14);
        binaryFileIcon = IconUtil.createPrimary("icons/file.svg", 14, 14);
        binaryWarningIcon = IconUtil.createColored("icons/warning.svg", 14, 14, ModernColors.getError());

        // BoxLayout shrinks the path field between its minimum and preferred widths on narrow editors,
        // instead of GridBagLayout collapsing it to a few pixels.
        JPanel pickerRow = new JPanel();
        pickerRow.setOpaque(false);
        pickerRow.setLayout(new BoxLayout(pickerRow, BoxLayout.X_AXIS));
        actionPanel.setMaximumSize(actionPanel.getPreferredSize());
        pickerRow.add(binaryFilePathField);
        pickerRow.add(Box.createHorizontalStrut(8));
        pickerRow.add(actionPanel);
        pickerRow.add(Box.createHorizontalGlue());

        binaryFileSummaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        filePickerPanel.add(pickerRow, BorderLayout.NORTH);
        filePickerPanel.add(binaryFileSummaryLabel, BorderLayout.CENTER);

        binaryFilePathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBinaryFileSummary();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBinaryFileSummary();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBinaryFileSummary();
            }
        });
        updateBinaryFileSummary();

        panel.add(filePickerPanel, BorderLayout.NORTH);
        return panel;
    }

    private void capBinaryFilePathFieldWidth() {
        int height = binaryFilePathField.getPreferredSize().height;
        int preferredWidth = Math.min(binaryFilePathField.getPreferredSize().width, BINARY_FILE_FIELD_MAX_WIDTH);
        binaryFilePathField.setPreferredSize(new Dimension(preferredWidth, height));
        binaryFilePathField.setMinimumSize(new Dimension(BINARY_FILE_FIELD_MIN_WIDTH, height));
        binaryFilePathField.setMaximumSize(new Dimension(BINARY_FILE_FIELD_MAX_WIDTH, height));
    }

    private class VariableAwareSyntaxTextArea extends FallbackAwareRSyntaxTextArea {
        private VariableAwareSyntaxTextArea() {
            super(5, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            String text = getText();
            for (VariableSegment seg : VariableParser.getVariableSegments(text)) {
                boolean isDefined = VariableResolver.isVariableDefined(seg.name);
                Color fillColor = isDefined ? RequestBodyTheme.definedVariableHighlight() : RequestBodyTheme.undefinedVariableHighlight();
                Color borderColor = isDefined ? RequestBodyTheme.definedVariableBorder() : RequestBodyTheme.undefinedVariableBorder();
                paintVariableBadge((Graphics2D) g, seg, text, fillColor, borderColor);
            }

            paintCaretAboveBadges(g);
        }

        private void paintCaretAboveBadges(Graphics g) {
            Caret caret = getCaret();
            if (caret == null || !hasFocus() || !isEnabled()) {
                return;
            }
            caret.paint(g);
        }

        private void paintVariableBadge(Graphics2D g, VariableSegment seg, String text, Color fillColor, Color borderColor) {
            try {
                Rectangle startRect = modelToView2D(seg.start).getBounds();
                Rectangle endRect = modelToView2D(Math.max(seg.start, seg.end - 1)).getBounds();
                if (startRect == null || endRect == null || startRect.y != endRect.y) {
                    return;
                }

                int x = startRect.x;
                int width = Math.max(10, endRect.x + endRect.width - startRect.x);
                int y = startRect.y + 1;
                int height = Math.max(14, startRect.height - 2);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try {
                    Stroke oldStroke = g2.getStroke();
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x, y, width, height, 10, 10);
                    g2.setStroke(oldStroke);
                } finally {
                    g2.dispose();
                }
            } catch (Exception ex) {
                log.debug("paintVariableBadge failed", ex);
            }
        }
    }

    // WebSocketGửi và theo dõicheckboxXóa đầu vào
    private void wsSendAndMaybeClear() {
        if (wsSendActionListener != null) {
            wsSendActionListener.actionPerformed(new ActionEvent(wsSendButton, ActionEvent.ACTION_PERFORMED, null));
        }
        if (wsClearInputCheckBox != null && wsClearInputCheckBox.isSelected()) {
            bodyArea.setText("");
        }
    }

    // Logic gửi thời gian
    private void toggleWsTimer() {
        // Chỉ được kết nốiWebSocketđể bắt đầu hẹn giờ
        if (wsSendButton == null || !wsSendButton.isEnabled()) {
            // Khi không được kết nối, nó sẽ quay trở lại trực tiếp và không cho phép khởi động bộ hẹn giờ
            return;
        }
        if (wsTimer != null && wsTimer.isRunning()) {
            wsTimer.stop();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
            wsIntervalField.setEnabled(true);
            wsClearInputCheckBox.setEnabled(true);
            wsSendButton.setEnabled(true);
        } else {
            int interval = 1000;
            try {
                interval = Integer.parseInt(wsIntervalField.getText().trim());
                if (interval < 100) interval = 100; // Tối thiểu100ms
            } catch (Exception ignored) {
            }
            wsTimer = new Timer(interval, e -> {
                if (wsSendButton.isEnabled()) {
                    wsSendAndMaybeClear();
                }
            });
            wsTimer.start();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_STOP));
            wsIntervalField.setEnabled(false);
            wsClearInputCheckBox.setEnabled(false);
            wsSendButton.setEnabled(true);
        }
    }

    /**
     * WebSocketĐược gọi khi trạng thái kết nối thay đổi, kiểm soát tính khả dụng của các nút gửi và thời gian
     *
     * @param connected được kết nối
     */
    public void setWebSocketConnected(boolean connected) {
        if (wsSendButton != null) wsSendButton.setEnabled(connected);
        if (wsTimedSendButton != null) wsTimedSendButton.setEnabled(connected);
    }

    private void switchBodyType(String bodyType) {
        currentBodyType = bodyType;
        bodyCardLayout.show(bodyCardPanel, bodyType);
        updateBodyActionVisibility(bodyType);
    }

    private void updateBodyActionVisibility(String bodyType) {
        if (isWebSocketMode) {
            return;
        }
        boolean isRaw = BODY_TYPE_RAW.equals(bodyType);
        boolean isBulkEditable = BODY_TYPE_FORM_DATA.equals(bodyType) || BODY_TYPE_FORM_URLENCODED.equals(bodyType);
        if (rawTypeComboBox != null) {
            rawTypeComboBox.setVisible(isRaw);
        }
        if (formatButton != null) {
            formatButton.setVisible(editable && isRaw);
        }
        if (compressButton != null) {
            compressButton.setVisible(editable && isRaw);
        }
        if (generateModelButton != null) {
            updateGenerateModelButtonState();
        }
        if (searchButton != null) {
            searchButton.setVisible(isRaw);
        }
        if (wrapButton != null) {
            wrapButton.setVisible(isRaw);
        }
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable && isBulkEditable);
        }
    }

    private void updateGenerateModelButtonState() {
        if (generateModelButton == null) return;
        boolean jsonRawBody = editable && isBodyTypeRAW() && rawTypeComboBox != null
                && RAW_TYPE_JSON.equals(rawTypeComboBox.getSelectedItem());
        generateModelButton.setVisible(jsonRawBody);
        generateModelButton.setEnabled(jsonRawBody && JsonUtil.isTypeJSON(getRawBody()));
    }

    private void showBulkEditDialog() {
        if (bulkEditSupport != null) {
            bulkEditSupport.showBulkEditDialog(currentBodyType);
        }
    }

    private void formatBody() {
        if (!isBodyTypeRAW()) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_ONLY_RAW));
            return;
        }
        String bodyText = bodyArea.getText();
        if (CharSequenceUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_EMPTY));
            return;
        }
        String selectedFormat = (String) rawTypeComboBox.getSelectedItem();
        Optional<String> formatted = formatBodyTextForDisplay(selectedFormat, bodyText);
        if (formatted.isPresent()) {
            bodyArea.setText(formatted.get());
            return;
        }

        JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_INVALID));
    }

    static Optional<String> formatBodyTextForDisplay(String selectedFormat, String bodyText) {
        if (RAW_TYPE_JSON.equals(selectedFormat)) {
            if (!JsonUtil.isTypeJSON(bodyText)) {
                return Optional.empty();
            }
            try {
                return Optional.of(JsonUtil.toJsonPrettyStr(bodyText));
            } catch (RuntimeException ex) {
                log.debug("JSON body format failed", ex);
                return Optional.empty();
            }
        }
        if (RAW_TYPE_XML.equals(selectedFormat)) {
            return Optional.of(XmlUtil.formatXml(bodyText));
        }
        log.debug("Unsupported format type or content is not JSON/XML");
        return Optional.empty();
    }

    private void compressBody() {
        if (!isBodyTypeRAW()) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_ONLY_RAW));
            return;
        }
        String bodyText = bodyArea.getText();
        if (CharSequenceUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_EMPTY));
            return;
        }
        String selectedFormat = (String) rawTypeComboBox.getSelectedItem();
        Optional<String> compressed = compressBodyTextForDisplay(selectedFormat, bodyText);
        if (compressed.isPresent()) {
            bodyArea.setText(compressed.get());
            return;
        }

        JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_INVALID));
    }

    static Optional<String> compressBodyTextForDisplay(String selectedFormat, String bodyText) {
        if (!RAW_TYPE_JSON.equals(selectedFormat)) {
            log.debug("Unsupported compress type or content is not JSON");
            return Optional.empty();
        }
        if (!JsonUtil.isTypeJSON(bodyText)) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonUtil.cleanJsonComments(bodyText));
        } catch (RuntimeException ex) {
            log.debug("JSON body compress failed", ex);
            return Optional.empty();
        }
    }

    /**
     * Chuyển đổi trạng thái ngắt dòng tự động
     */
    private void toggleLineWrap() {
        if (bodyArea != null && wrapButton != null) {
            boolean isWrapEnabled = wrapButton.isSelected();
            bodyArea.setLineWrap(isWrapEnabled);
        }
    }


    // getterPhương thức, được gọi bởi bảng điều khiển chính
    public String getBodyType() {
        return currentBodyType;
    }

    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText() : null;
    }

    public String getBodyContent(String bodyType) {
        if (BODY_TYPE_RAW.equals(bodyType)) {
            return getRawBody();
        }
        if (BODY_TYPE_BINARY.equals(bodyType)) {
            return getBinaryFilePath();
        }
        return "";
    }

    public String getBinaryFilePath() {
        return binaryFilePathField != null ? binaryFilePathField.getText() : "";
    }

    public void setBinaryFilePath(String filePath) {
        if (binaryFilePathField != null) {
            binaryFilePathField.setText(filePath == null ? "" : filePath);
            binaryFilePathField.setCaretPosition(binaryFilePathField.getDocument().getLength());
            updateBinaryFileSummary();
        }
    }

    private void chooseBinaryFile() {
        Optional<File> selectedFile = FileChooserUtil.showOpenFile(
                this,
                "request.body.binary",
                I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_SELECT_FILE)
        );
        selectedFile.ifPresent(file -> setBinaryFilePath(file.getAbsolutePath()));
    }

    private void updateBinaryFileSummary() {
        if (binaryFilePathField == null || binaryFileSummaryLabel == null) {
            return;
        }
        String filePath = binaryFilePathField.getText();
        String trimmedPath = filePath == null ? "" : filePath.trim();
        boolean hasPath = CharSequenceUtil.isNotBlank(trimmedPath);
        if (binaryClearButton != null) {
            binaryClearButton.setEnabled(editable && hasPath);
        }
        binaryFilePathField.setToolTipText(hasPath
                ? trimmedPath
                : I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_PLACEHOLDER));
        binaryFileSummaryLabel.setToolTipText(hasPath ? trimmedPath : null);
        if (!hasPath) {
            binaryFileSummaryLabel.setText(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_NO_FILE));
            binaryFileSummaryLabel.setForeground(ModernColors.getTextHint());
            binaryFileSummaryLabel.setIcon(binaryEmptyIcon);
            return;
        }
        File file = FileMimeTypeUtil.toFile(trimmedPath);
        if (FileMimeTypeUtil.isReadableRegularFile(file)) {
            binaryFileSummaryLabel.setText(I18nUtil.getMessage(
                    MessageKeys.REQUEST_BODY_BINARY_FILE_SUMMARY,
                    file.getName(),
                    formatBinaryFileSize(file.length()),
                    FileMimeTypeUtil.detectMimeType(file)
            ));
            binaryFileSummaryLabel.setForeground(ModernColors.getTextSecondary());
            binaryFileSummaryLabel.setIcon(binaryFileIcon);
            return;
        }
        binaryFileSummaryLabel.setText(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_BINARY_FILE_MISSING));
        binaryFileSummaryLabel.setForeground(ModernColors.getError());
        binaryFileSummaryLabel.setIcon(binaryWarningIcon);
    }

    private static String formatBinaryFileSize(long sizeBytes) {
        if (sizeBytes <= Integer.MAX_VALUE) {
            return FileSizeDisplayUtil.formatSize((int) sizeBytes);
        }
        return String.format("%.2f GB", sizeBytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Lịch sử hoàn tác phải được đặt lại khi tải nội dung nội dung yêu cầu theo chương trình.
     * <p>
     * RSTA Đi kèm với một nội thất UndoManager，Ở đây một lần nữa để tương thích Ctrl/Cmd Các phím tắt đã được duy trì
     * bodyUndoManager。Chuyển đổi yêu cầu, tải yêu cầu lịch sử, v.v setText Đó không phải là sự chỉnh sửa của người dùng nếu không được xóa cùng lúc
     * Hai ngăn xếp hoàn tác, lần đầu tiên Ctrl+Z sẽ bị thu hồi“Tải hành động”，Điều này biểu hiện khi nội dung của người chỉnh sửa bị xóa hoặc quay lại yêu cầu trước đó.
     */
    public void setRawBodyText(String text) {
        if (bodyArea == null) {
            return;
        }
        bodyArea.setText(text == null ? "" : text);
        bodyArea.setCaretPosition(0);
        bodyArea.discardAllEdits();
        bodyUndoManager.discardAllEdits();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (bodyTypeComboBox != null) {
            bodyTypeComboBox.setEnabled(editable);
        }
        if (rawTypeComboBox != null) {
            rawTypeComboBox.setEnabled(editable);
        }
        if (formDataTablePanel != null) {
            formDataTablePanel.setEditable(editable);
        }
        if (formUrlencodedTablePanel != null) {
            formUrlencodedTablePanel.setEditable(editable);
        }
        if (bodyArea != null) {
            bodyArea.setEditable(editable);
        }
        if (binaryFilePathField != null) {
            binaryFilePathField.setEditable(editable);
            binaryFilePathField.setEnabled(editable);
        }
        if (binaryBrowseButton != null) {
            binaryBrowseButton.setEnabled(editable);
        }
        if (binaryClearButton != null) {
            binaryClearButton.setEnabled(editable && CharSequenceUtil.isNotBlank(getBinaryFilePath()));
        }
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable && (BODY_TYPE_FORM_DATA.equals(currentBodyType)
                    || BODY_TYPE_FORM_URLENCODED.equals(currentBodyType)));
            bulkEditButton.setEnabled(editable);
        }
        if (formatButton != null) {
            formatButton.setVisible(editable && isBodyTypeRAW());
            formatButton.setEnabled(editable);
        }
        if (compressButton != null) {
            compressButton.setVisible(editable && isBodyTypeRAW());
            compressButton.setEnabled(editable);
        }
        if (generateModelButton != null) {
            updateGenerateModelButtonState();
        }
        if (wsClearInputCheckBox != null) {
            wsClearInputCheckBox.setEnabled(editable);
        }
        if (wsIntervalField != null) {
            wsIntervalField.setEditable(editable);
            wsIntervalField.setEnabled(editable);
        }
        if (wsTimedSendButton != null) {
            wsTimedSendButton.setVisible(editable);
            wsTimedSendButton.setEnabled(editable);
        }
        if (wsSendButton != null) {
            wsSendButton.setVisible(editable);
            wsSendButton.setEnabled(editable);
        }
    }

}
