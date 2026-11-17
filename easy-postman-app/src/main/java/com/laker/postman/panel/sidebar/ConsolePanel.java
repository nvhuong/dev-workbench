package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.AutoScrollToggleButton;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.NextButton;
import com.laker.postman.common.component.button.PreviousButton;
import com.laker.postman.util.IconUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConsolePanel extends UiSingletonPanel {
    private JTextPane consoleLogArea;
    private transient StyledDocument consoleDoc;
    private SearchTextField searchField;
    private JButton hideButton;
    private PreviousButton prevBtn;
    private NextButton nextBtn;
    private JLabel matchCountLabel;
    private AutoScrollToggleButton autoScrollBtn;
    private JComboBox<String> logLevelFilter;
    private boolean autoScroll = true;

    // Tìm kiếm liên quan
    private final transient List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;

    // Liên quan đến lọc nhật ký
    private String currentFilter = "All";
    private final transient List<LogEntry> allLogs = new ArrayList<>();
    private static final int MAX_LOG_ENTRIES = 10000; // Tiết kiệm nhiều nhất10000log để tránh tràn bộ nhớ
    private static final int MAX_DISPLAY_LINES = 5000; // Hiển thị nhiều nhất5000Nếu số lượng hàng vượt quá giới hạn, các hàng cũ sẽ bị xóa để tối ưu hóa hiệu suất hiển thị
    private static final int FILTER_WIDTH = 90;
    private static final int SEARCH_WIDTH = 260;
    private static final int TOOLBAR_CONTROL_HEIGHT = ToolWindowActionToolbar.ACTION_SIZE;
    private static final String TOOL_WINDOW_HIDE_ICON = "icons/tool-window-hide.svg";
    private static final String HIDE_CONSOLE_TOOLTIP = "Hide console";

    // Loại nhật ký
    public enum LogType {
        INFO, ERROR, SUCCESS, WARN, DEBUG, TRACE, CUSTOM
    }

    // Lớp nhập nhật ký
    private static class LogEntry {
        final String message;
        final LogType type;

        LogEntry(String message, LogType type) {
            this.message = message;
            this.type = type;
        }
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        createConsolePanel();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateConsoleColors();
    }

    /**
     * Cập nhật màu bảng điều khiển để phù hợp với chủ đề hiện tại
     */
    private void updateConsoleColors() {
        if (matchCountLabel != null) {
            matchCountLabel.setForeground(ConsoleTheme.matchCountForeground());
        }
        // Làm mới nhật ký được hiển thị để áp dụng bảng màu mới
        if (consoleLogArea != null && !allLogs.isEmpty()) {
            refreshDisplayedLogs();
        }
    }

    @Override
    protected void registerListeners() {
        // Tìm kiếm lại khi văn bản hộp tìm kiếm thay đổi
        searchField.addActionListener(e -> performSearch());

        // Tìm kiếm trong thời gian thực
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                performSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                performSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                performSearch();
            }
        });

        // Theo dõi những thay đổi về độ phân biệt chữ hoa chữ thường và các tùy chọn khớp toàn bộ từ
        searchField.addPropertyChangeListener("caseSensitive", evt -> performSearch());
        searchField.addPropertyChangeListener("wholeWord", evt -> performSearch());

        nextBtn.addActionListener(e -> navigateToNextMatch());
        prevBtn.addActionListener(e -> navigateToPreviousMatch());

        // Chuyển đổi cuộn tự động
        autoScrollBtn.addActionListener(e -> {
            autoScroll = autoScrollBtn.isSelected();
            if (autoScroll) {
                // Nếu tính năng tự động cuộn được bật, hãy cuộn xuống phía dưới ngay lập tức
                SwingUtilities.invokeLater(() -> {
                    try {
                        consoleLogArea.setCaretPosition(consoleDoc.getLength());
                    } catch (Exception ex) {
                        // ignore
                    }
                });
            }
        });

        // Lọc cấp độ nhật ký
        logLevelFilter.addActionListener(e -> {
            currentFilter = (String) logLevelFilter.getSelectedItem();
            refreshDisplayedLogs();
        });

        registerKeyboardShortcuts();
    }

    /**
     * Đăng ký phím tắt
     */
    private void registerKeyboardShortcuts() {
        // phím tắt:Ctrl/Cmd+F Tập trung vào hộp tìm kiếm
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier), "focusSearch"
        );
        getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        // Esc Xóa tìm kiếm
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch"
        );
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearSearch();
            }
        });
    }

    /**
     * Xóa tìm kiếm
     */
    private void clearSearch() {
        searchField.setText("");
        consoleLogArea.getHighlighter().removeAllHighlights();
        matchPositions.clear();
        currentMatchIndex = -1;
        updateMatchCounter();
    }

    /**
     * Thực hiện tìm kiếm và tìm thấy tất cả các kết quả khớp (hỗ trợ khớp cả chữ và chữ hoa chữ thường)
     */
    private void performSearch() {
        String keyword = searchField.getText();
        matchPositions.clear();
        currentMatchIndex = -1;
        consoleLogArea.getHighlighter().removeAllHighlights();

        if (keyword.isEmpty()) {
            searchField.setNoResult(false);
            updateMatchCounter();
            return;
        }

        String text = consoleLogArea.getText();
        boolean caseSensitive = searchField.isCaseSensitive();
        boolean wholeWord = searchField.isWholeWord();

        // Chuẩn bị văn bản và từ khóa để tìm kiếm
        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = 0;
        while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
            // Nếu tính năng khớp toàn bộ từ được bật thì cần phải kiểm tra giới hạn
            if (wholeWord) {
                boolean isWordStart = pos == 0 || !Character.isLetterOrDigit(text.charAt(pos - 1));
                boolean isWordEnd = (pos + searchKeyword.length() >= text.length()) ||
                        !Character.isLetterOrDigit(text.charAt(pos + searchKeyword.length()));

                if (isWordStart && isWordEnd) {
                    matchPositions.add(pos);
                }
            } else {
                matchPositions.add(pos);
            }
            pos += searchKeyword.length();
        }

        if (!matchPositions.isEmpty()) {
            currentMatchIndex = 0;
            highlightAllMatches(keyword.length());
            highlightCurrentMatch(keyword.length());
        }

        // Hộp tìm kiếm chuyển sang màu đỏ khi không có kết quả phù hợp và trở lại bình thường khi có kết quả phù hợp
        searchField.setNoResult(matchPositions.isEmpty());

        updateMatchCounter();
    }

    /**
     * Điều hướng đến trận đấu tiếp theo
     */
    private void navigateToNextMatch() {
        if (matchPositions.isEmpty()) return;

        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        highlightCurrentMatch(searchField.getText().length());
        updateMatchCounter();
    }

    /**
     * Điều hướng đến trận đấu trước đó
     */
    private void navigateToPreviousMatch() {
        if (matchPositions.isEmpty()) return;

        currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
        highlightCurrentMatch(searchField.getText().length());
        updateMatchCounter();
    }

    /**
     * Làm nổi bật tất cả các trận đấu
     */
    private void highlightAllMatches(int length) {
        try {
            Color highlightColor = ConsoleTheme.searchHighlightBackground();
            DefaultHighlighter.DefaultHighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(highlightColor);

            for (int pos : matchPositions) {
                consoleLogArea.getHighlighter().addHighlight(pos, pos + length, painter);
            }
        } catch (BadLocationException ex) {
            log.debug("Highlight failed", ex);
        }
    }

    /**
     * Làm nổi bật trận đấu hiện tại
     */
    private void highlightCurrentMatch(int length) {
        if (currentMatchIndex < 0 || currentMatchIndex >= matchPositions.size()) return;

        try {
            int pos = matchPositions.get(currentMatchIndex);

            // Làm nổi bật lại tất cả các trận đấu
            consoleLogArea.getHighlighter().removeAllHighlights();
            highlightAllMatches(length);

            Color currentHighlightColor = ConsoleTheme.searchCurrentHighlightBackground();
            DefaultHighlighter.DefaultHighlightPainter currentPainter =
                    new DefaultHighlighter.DefaultHighlightPainter(currentHighlightColor);
            consoleLogArea.getHighlighter().addHighlight(pos, pos + length, currentPainter);

            // Di chuyển đến trận đấu hiện tại
            consoleLogArea.setCaretPosition(pos);
        } catch (BadLocationException ex) {
            log.debug("Highlight current match failed", ex);
        }
    }

    /**
     * Cập nhật bộ đếm trận đấu
     */
    private void updateMatchCounter() {
        if (matchPositions.isEmpty()) {
            matchCountLabel.setText("");
        } else {
            matchCountLabel.setText(String.format("%d/%d", currentMatchIndex + 1, matchPositions.size()));
        }
    }

    private void createConsolePanel() {
        consoleLogArea = new JTextPane();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFocusable(true);
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        ToolWindowSurfaceStyle.applyTextComponentCard(consoleLogArea);
        consoleDoc = consoleLogArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(consoleLogArea);
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ToolWindowSurfaceStyle.applyScrollPaneCard(logScroll);

        // Thanh công cụ trên cùng
        JPanel topPanel = new JPanel(new BorderLayout(5, 0)) {
            @Override
            public void updateUI() {
                super.updateUI();
                applyConsoleToolbarStyle(this);
            }
        };
        applyConsoleToolbarStyle(topPanel);

        logLevelFilter = new JComboBox<>(new String[]{"All", "INFO", "ERROR", "WARN", "DEBUG"});
        lockToolbarControlSize(logLevelFilter, FILTER_WIDTH);
        logLevelFilter.setFocusable(false);
        logLevelFilter.setToolTipText("Filter logs by level");

        searchField = new SearchTextField();
        lockToolbarControlSize(searchField, SEARCH_WIDTH);

        prevBtn = new PreviousButton();
        prevBtn.setToolTipText("Previous match (Shift+Enter)");
        prevBtn.setPreferredSize(new Dimension(28, 28));

        nextBtn = new NextButton();
        nextBtn.setToolTipText("Next match (Enter)");
        nextBtn.setPreferredSize(new Dimension(28, 28));

        matchCountLabel = new JLabel("");
        matchCountLabel.setForeground(ConsoleTheme.matchCountForeground());
        matchCountLabel.setPreferredSize(new Dimension(50, 28));
        matchCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel centerPanel = ToolWindowActionToolbar.inlineLeft(
                logLevelFilter,
                searchField,
                prevBtn,
                nextBtn,
                matchCountLabel
        );

        // Bên phải: Các nút công cụ
        autoScrollBtn = new AutoScrollToggleButton();

        ClearButton clearBtn = new ClearButton(IconUtil.SIZE_SMALL);
        clearBtn.addActionListener(e -> clearConsole());

        hideButton = createToolWindowToolbarButton(TOOL_WINDOW_HIDE_ICON, HIDE_CONSOLE_TOOLTIP);
        JPanel rightPanel = ToolWindowActionToolbar.inlineRight(autoScrollBtn, clearBtn, hideButton);

        topPanel.add(centerPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
    }

    private static void applyConsoleToolbarStyle(JComponent component) {
        ToolWindowSurfaceStyle.applyToolWindowToolbarSeparator(component, 4, 6, 4, 6);
    }

    private static void lockToolbarControlSize(JComponent component, int width) {
        Dimension size = new Dimension(width, TOOLBAR_CONTROL_HEIGHT);
        component.setMinimumSize(size);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
    }

    private static JButton createToolWindowToolbarButton(String iconPath, String toolTipText) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        button.setToolTipText(toolTipText);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return button;
    }

    /**
     * Xóa bảng điều khiển
     */
    private void clearConsole() {
        try {
            consoleDoc.remove(0, consoleDoc.getLength());
            allLogs.clear(); // Xóa tất cả nhật ký
            matchPositions.clear();
            currentMatchIndex = -1;
            updateMatchCounter();
        } catch (BadLocationException ex) {
            log.debug("Clear console failed", ex);
        }
    }

    public synchronized void appendConsoleLog(String msg) {
        appendConsoleLog(msg, LogType.INFO);
    }

    public synchronized void appendConsoleLog(String msg, LogType type) {
        // Đã thêm vào tất cả danh sách nhật ký
        allLogs.add(new LogEntry(msg, type));

        // Nếu vượt quá số lượng tối đa, nhật ký cũ nhất sẽ bị xóa (FIFO）
        if (allLogs.size() > MAX_LOG_ENTRIES) {
            allLogs.remove(0);
        }

        SwingUtilities.invokeLater(() -> {
            // Kiểm tra xem nhật ký này có được hiển thị không
            if (!shouldDisplayLog(type)) {
                return;
            }

            Style style = consoleLogArea.addStyle("logStyle", null);
            applyLogStyle(style, type);

            try {
                // Lưu vị trí con trỏ hiện tại
                int currentCaretPosition = consoleLogArea.getCaretPosition();

                consoleDoc.insertString(consoleDoc.getLength(), msg + "\n", style);

                // Giới hạn số lượng hàng hiển thị để tránh các vấn đề về hiệu suất
                limitDisplayLines();

                // Vị trí con trỏ được xác định dựa trên cài đặt cuộn tự động
                if (autoScroll) {
                    // Cuộn xuống phía dưới
                    consoleLogArea.setCaretPosition(consoleDoc.getLength());
                } else {
                    // Duy trì vị trí duyệt hiện tại của người dùng mà không cần cuộn tự động
                    try {
                        // Nếu vị trí ban đầu vẫn còn hiệu lực, hãy khôi phục nó về vị trí ban đầu
                        if (currentCaretPosition <= consoleDoc.getLength()) {
                            consoleLogArea.setCaretPosition(currentCaretPosition);
                        }
                    } catch (Exception ex) {
                        // Nếu vị trí khôi phục không thành công thì không có quá trình xử lý nào được thực hiện
                    }
                }

                // Nếu bạn đang tìm kiếm, hãy cập nhật kết quả tìm kiếm
                if (!searchField.getText().isEmpty()) {
                    performSearch();
                }
            } catch (BadLocationException e) {
                log.debug("Append console log failed", e);
            }
        });
    }

    /**
     * Giới hạn số lượng hàng hiển thị, loại bỏ những hàng cũ nhất
     */
    private void limitDisplayLines() {
        try {
            Element root = consoleDoc.getDefaultRootElement();
            int lineCount = root.getElementCount();

            // Nếu vượt quá số lượng hàng hiển thị tối đa, hàng cũ nhất sẽ bị xóa
            if (lineCount > MAX_DISPLAY_LINES) {
                int linesToRemove = lineCount - MAX_DISPLAY_LINES;
                Element firstLine = root.getElement(linesToRemove - 1);
                int endOffset = firstLine.getEndOffset();
                consoleDoc.remove(0, endOffset);
            }
        } catch (BadLocationException e) {
            log.debug("Limit display lines failed", e);
        }
    }

    /**
     * Xác định xem nhật ký này có nên được hiển thị hay không
     */
    private boolean shouldDisplayLog(LogType type) {
        if ("All".equals(currentFilter)) {
            return true;
        }
        return type.name().equals(currentFilter);
    }

    /**
     * Làm mới nhật ký được hiển thị (sau khi lọc)
     */
    private void refreshDisplayedLogs() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Xóa màn hình hiện tại
                consoleDoc.remove(0, consoleDoc.getLength());

                // Thêm lại nhật ký phù hợp với điều kiện lọc
                for (LogEntry entry : allLogs) {
                    if (shouldDisplayLog(entry.type)) {
                        Style style = consoleLogArea.addStyle("logStyle", null);
                        applyLogStyle(style, entry.type);
                        consoleDoc.insertString(consoleDoc.getLength(), entry.message + "\n", style);
                    }
                }

                // Cuộn xuống phía dưới
                if (autoScroll) {
                    consoleLogArea.setCaretPosition(consoleDoc.getLength());
                }

                // Cập nhật kết quả tìm kiếm
                if (!searchField.getText().isEmpty()) {
                    performSearch();
                }
            } catch (BadLocationException e) {
                log.debug("Refresh logs failed", e);
            }
        });
    }

    /**
     * Áp dụng kiểu nhật ký - Sử dụng bảng màu chủ đề thống nhất
     */
    private void applyLogStyle(Style style, LogType type) {
        // Đặt khoảng cách dòng lớn hơn cho tất cả các loại nhật ký để cải thiện khả năng đọc
        StyleConstants.setLineSpacing(style, 0.2f);
        StyleConstants.setForeground(style, ConsoleTheme.logForeground(type));

        switch (type) {
            case ERROR:
                StyleConstants.setBold(style, true);
                break;
            case SUCCESS:
                StyleConstants.setBold(style, true);
                break;
            case WARN:
                StyleConstants.setBold(style, true);
                break;
            case DEBUG:
                StyleConstants.setBold(style, false);
                break;
            case TRACE:
                StyleConstants.setBold(style, false);
                break;
            case CUSTOM:
                StyleConstants.setBold(style, false);
                break;
            case INFO:
            default:
                StyleConstants.setBold(style, false);
        }
    }

    // Phương pháp proxy tĩnh để thực hiện các cuộc gọi bên ngoài dễ dàng
    public static void appendLog(String msg) {
        UiSingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg);
    }

    public static void appendLog(String msg, LogType type) {
        UiSingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg, type);
    }

    public void setHideAction(ActionListener listener) {
        hideButton.addActionListener(listener);
    }
}
