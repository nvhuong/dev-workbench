package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

@Slf4j
public class SearchReplacePanel extends JPanel {

    // Đường dẫn biểu tượng không đổi - chevron-right Cho biết bảng điều khiển thay thế đã bị sập.chevron-down Cho biết bảng thay thế được mở rộng
    private static final String ICON_EXPAND = "icons/chevron-right.svg";
    private static final String ICON_COLLAPSE = "icons/chevron-down.svg";

    private static final int CORNER_RADIUS = 8;
    private static final int FIELD_WIDTH = 180;
    private static final int FIELD_MAX_WIDTH = 200;
    private static final int FIELD_HEIGHT = SearchTextField.DEFAULT_HEIGHT;
    private static final int TOOL_BUTTON_SIZE = 22;
    private static final int STATUS_WIDTH = 82;
    private static final int STATUS_MAX_WIDTH = 96;
    private static final int ROW_GAP = 4;
    private static final int GROUP_GAP = 6;
    private static final int PANEL_HORIZONTAL_PADDING = 7;
    private static final int PANEL_VERTICAL_PADDING = 5;
    private static final String TOOL_BUTTON_STYLE = "arc: 6; margin: 0,0,0,0";
    private static final String TEXT_FIELD_STYLE = "arc: 8; margin: 4,8,4,8";

    private final RSyntaxTextArea textArea;
    private final SearchTextField searchField;
    private final FlatTextField replaceField;
    private final JButton toggleReplaceBtn;
    private final JPanel replacePanel;
    private final Component replaceGap;
    private final JLabel statusLabel;  // Nhãn trạng thái kết quả tìm kiếm
    private final boolean enableReplace;  // Có bật chức năng thay thế hay không

    // Chống rungTimer，Tránh tìm kiếm thường xuyên trong khi gõ
    private Timer searchDebounceTimer;

    /**
     * Tạo bảng tìm kiếm và thay thế (thay thế được bật theo mặc định)
     */
    public SearchReplacePanel(RSyntaxTextArea textArea) {
        this(textArea, true);
    }

    /**
     * Tạo bảng tìm kiếm và thay thế
     *
     * @param textArea      Khu vực văn bản mục tiêu
     * @param enableReplace Có bật chức năng thay thế hay không
     */
    public SearchReplacePanel(RSyntaxTextArea textArea, boolean enableReplace) {
        this.textArea = textArea;
        this.enableReplace = enableReplace;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(
                PANEL_VERTICAL_PADDING,
                PANEL_HORIZONTAL_PADDING,
                PANEL_VERTICAL_PADDING,
                PANEL_HORIZONTAL_PADDING
        ));
        // Giữ nền và đường viền trong suốt paintComponent vẽ.
        setOpaque(false);
        // Bảng tìm kiếm
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setOpaque(false);  // Trong suốt để hiển thị nền tròn của bảng điều khiển chính

        // Mở rộng/Nút thu gọn bảng thay thế (ở ngoài cùng bên trái, tương tự Postman）
        toggleReplaceBtn = new JButton(IconUtil.createThemed(ICON_EXPAND, 16, 16));
        toggleReplaceBtn.setToolTipText(UiI18n.get(UiMessageKeys.SEARCH_TOGGLE_REPLACE));
        toggleReplaceBtn.setFocusable(false);
        setFixedSize(toggleReplaceBtn, TOOL_BUTTON_SIZE, TOOL_BUTTON_SIZE);
        toggleReplaceBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleReplaceBtn.setVisible(enableReplace);
        toggleReplaceBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        toggleReplaceBtn.putClientProperty(FlatClientProperties.STYLE, TOOL_BUTTON_STYLE);
        Component toggleGap = Box.createHorizontalStrut(ROW_GAP);
        toggleGap.setVisible(enableReplace);

        // Hộp nhập tìm kiếm - sử dụng SearchTextField Tái sử dụng chức năng phân biệt chữ hoa chữ thường và khớp toàn bộ từ
        searchField = new SearchTextField();
        configureOverlayTextField(searchField);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        findPrevious();
                    } else {
                        findNext();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePanel();
                }
            }
        });

        // Thêm trình nghe thay đổi văn bản để cập nhật trạng thái kết quả tìm kiếm theo thời gian thực (sử dụng tính năng chống rung)
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                // Nếu hộp tìm kiếm bị xóa, lựa chọn và trạng thái của vùng văn bản sẽ bị xóa ngay lập tức
                if (searchField.getText().isEmpty()) {
                    // Hủy bộ hẹn giờ trước đó
                    if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
                        searchDebounceTimer.stop();
                    }
                    clearSearchHighlights();
                    statusLabel.setText("");
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
                } else {
                    scheduleSearchUpdate();
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }
        });

        // Nút tìm
        JButton findPrevBtn = createIconButton("icons/arrow-up.svg", UiI18n.get(UiMessageKeys.SEARCH_PREVIOUS), e -> findPrevious());
        JButton findNextBtn = createIconButton("icons/arrow-down.svg", UiI18n.get(UiMessageKeys.SEARCH_NEXT), e -> findNext());


        // nhãn trạng thái
        statusLabel = new JLabel("");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        statusLabel.setForeground(ModernColors.getTextHint());
        statusLabel.setPreferredSize(new Dimension(STATUS_WIDTH, TOOL_BUTTON_SIZE));
        statusLabel.setMaximumSize(new Dimension(STATUS_MAX_WIDTH, TOOL_BUTTON_SIZE));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Nút đóng
        JButton closeBtn = createIconButton("icons/x.svg", UiI18n.get(UiMessageKeys.SEARCH_CLOSE), e -> hidePanel());


        // Lắp ráp bảng tìm kiếm
        searchPanel.add(toggleReplaceBtn);
        searchPanel.add(toggleGap);
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(ROW_GAP));
        searchPanel.add(findPrevBtn);
        searchPanel.add(findNextBtn);
        searchPanel.add(Box.createHorizontalStrut(GROUP_GAP));
        searchPanel.add(statusLabel);
        searchPanel.add(Box.createHorizontalStrut(ROW_GAP));
        searchPanel.add(closeBtn);
        searchPanel.add(Box.createHorizontalGlue());

        add(searchPanel);

        // Bảng thay thế (ẩn theo mặc định)
        replacePanel = new JPanel();
        replacePanel.setLayout(new BoxLayout(replacePanel, BoxLayout.X_AXIS));
        replacePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        replacePanel.setVisible(false);
        replacePanel.setOpaque(false);  // Trong suốt để hiển thị nền tròn của bảng điều khiển chính

        // Phần giữ chỗ bên trái, luôn căn chỉnh với hộp tìm kiếm (chuyển đổi chiều rộng của nút + khoảng cách ngang)
        int spacerWidth = TOOL_BUTTON_SIZE + ROW_GAP;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        setFixedSize(spacer, spacerWidth, TOOL_BUTTON_SIZE);

        // Thay thế hộp đầu vào
        replaceField = new FlatTextField();
        replaceField.setPlaceholderText(UiI18n.get(UiMessageKeys.SEARCH_REPLACE_PLACEHOLDER));
        configureOverlayTextField(replaceField);
        replaceField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    replace();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePanel();
                }
            }
        });

        // Nút thay thế
        JButton replaceBtn = createIconButton("icons/replace.svg", UiI18n.get(UiMessageKeys.SEARCH_REPLACE), e -> replace());
        JButton replaceAllBtn = createIconButton("icons/replace-all.svg", UiI18n.get(UiMessageKeys.SEARCH_REPLACE_ALL), e -> replaceAll());

        // Lắp ráp các tấm thay thế
        replacePanel.add(spacer);  // Phần giữ chỗ bên trái (bao gồm chiều rộng nút+khoảng cách)
        replacePanel.add(replaceField);
        replacePanel.add(Box.createHorizontalStrut(ROW_GAP));
        replacePanel.add(replaceBtn);
        replacePanel.add(Box.createHorizontalStrut(ROW_GAP));
        replacePanel.add(replaceAllBtn);
        replacePanel.add(Box.createHorizontalGlue());

        replaceGap = Box.createVerticalStrut(ROW_GAP);
        replaceGap.setVisible(false);
        add(replaceGap);
        add(replacePanel);

        // cài đặt toggleReplaceBtn của actionListener（Hiện nay replacePanel đã được khởi tạo)
        toggleReplaceBtn.addActionListener(e -> {
            setReplaceVisible(!replacePanel.isVisible());
        });

        // Bị ẩn theo mặc định
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Bật khử răng cưa để các góc bo tròn mượt mà
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Vẽ một nền tròn
        g2.setColor(ModernColors.getCardBackgroundColor());
        g2.fill(new RoundRectangle2D.Float(
                0,
                0,
                width - 1.0f,
                height - 1.0f,
                CORNER_RADIUS,
                CORNER_RADIUS
        ));

        // Vẽ đường viền tinh tế
        g2.setColor(ModernColors.getBorderLightColor());
        g2.draw(new RoundRectangle2D.Float(
                0,
                0,
                width - 1.0f,
                height - 1.0f,
                CORNER_RADIUS,
                CORNER_RADIUS
        ));

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int rowHeight = replacePanel.isVisible()
                ? FIELD_HEIGHT + ROW_GAP + FIELD_HEIGHT
                : FIELD_HEIGHT;
        int height = rowHeight + PANEL_VERTICAL_PADDING * 2;
        return new Dimension(size.width, Math.max(size.height, height));
    }

    /**
     * Tạo một nút biểu tượng - Thiết kế phẳng hiện đại
     */
    private JButton createIconButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(IconUtil.createThemed(iconPath, 16, 16));
        btn.setToolTipText(tooltip);
        setFixedSize(btn, TOOL_BUTTON_SIZE, TOOL_BUTTON_SIZE);
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btn.putClientProperty(FlatClientProperties.STYLE, TOOL_BUTTON_STYLE);
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }

    private static void configureOverlayTextField(JComponent field) {
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_MAX_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
    }

    private static void setFixedSize(Component component, int width, int height) {
        Dimension size = new Dimension(width, height);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    private void setReplaceVisible(boolean visible) {
        replacePanel.setVisible(visible);
        replaceGap.setVisible(visible);
        toggleReplaceBtn.setIcon(IconUtil.createThemed(visible ? ICON_COLLAPSE : ICON_EXPAND, 16, 16));

        invalidate();
        revalidate();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        Rectangle bounds = getBounds();
        setBounds(bounds.x, bounds.y, bounds.width, getPreferredSize().height);
    }


    /**
     * Hiển thị bảng tìm kiếm (chỉ ở chế độ tìm kiếm)
     */
    public void showSearch() {
        setReplaceVisible(false);
        setVisible(true);

        // Nếu có văn bản được chọn, hãy sử dụng nó làm nội dung tìm kiếm
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty() && !selectedText.contains("\n")) {
            searchField.setText(selectedText);
        }

        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Hiển thị bảng tìm kiếm và thay thế (chế độ thay thế)
     */
    public void showReplace() {
        if (!enableReplace) {
            // Nếu tính năng thay thế không được bật, chỉ các tìm kiếm được hiển thị
            showSearch();
            return;
        }
        setReplaceVisible(true);
        setVisible(true);

        // Nếu có văn bản được chọn, hãy sử dụng nó làm nội dung tìm kiếm
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty() && !selectedText.contains("\n")) {
            searchField.setText(selectedText);
        }

        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Ẩn bảng tìm kiếm
     */
    public void hidePanel() {
        // Xóa trạng thái đánh dấu tìm kiếm và lựa chọn
        clearSearchHighlights();
        setVisible(false);
        textArea.requestFocusInWindow();
    }

    /**
     * Tìm tiếp theo
     */
    private void findNext() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            clearSearchHighlights();
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);

        // Đảm bảo vùng văn bản có tiêu điểm để hiển thị hiệu ứng lựa chọn
        textArea.requestFocusInWindow();
    }

    /**
     * Tìm cái trước đó
     */
    private void findPrevious() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            clearSearchHighlights();
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(false);
        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);

        // Đảm bảo vùng văn bản có tiêu điểm để hiển thị hiệu ứng lựa chọn
        textArea.requestFocusInWindow();
    }

    /**
     * Thay thế trận đấu hiện tại
     */
    private void replace() {
        String searchText = searchField.getText();

        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchResult result = SearchEngine.replace(textArea, context);
        if (result.wasFound()) {
            // Sau khi thay thế thành công, cái tiếp theo sẽ tự động được tìm thấy
            findNext();
        } else {
            statusLabel.setText(noResultsText());
        }
    }

    /**
     * Thay thế tất cả các trận đấu
     */
    private void replaceAll() {
        String searchText = searchField.getText();

        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = createSearchContext();

        SearchResult result = SearchEngine.replaceAll(textArea, context);
        int count = result.getCount();

        // Hiển thị kết quả thay thế
        if (count > 0) {
            statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_REPLACED_COUNT, count));
            // 2Xóa trạng thái sau vài giây
            Timer timer = new Timer(2000, e -> updateSearchStatus());
            timer.setRepeats(false);
            timer.start();
        } else {
            statusLabel.setText(noResultsText());
        }
    }

    /**
     * Tạo bối cảnh tìm kiếm. Bị vô hiệu hóa markAll，Tránh tìm kiếm/Kích hoạt quét toàn bộ điểm nổi bật khi thay thế.
     */
    private SearchContext createSearchContext() {
        SearchContext context = new SearchContext();
        context.setSearchFor(searchField.getText());
        context.setReplaceWith(replaceField.getText());
        context.setMatchCase(searchField.isCaseSensitive());
        context.setRegularExpression(false);  // Biểu thức thông thường không được hỗ trợ
        context.setWholeWord(searchField.isWholeWord());
        context.setMarkAll(false);
        return context;
    }

    /**
     * Cập nhật nhãn trạng thái dựa trên kết quả tìm kiếm và vượt qua outline Tô màu hộp tìm kiếm để phản hồi
     */
    private void updateStatusFromResult(SearchResult result) {
        if (result.wasFound()) {
            // Tính tổng số trận đấu và chỉ số hiện tại
            int totalCount = calculateTotalMatches();
            if (totalCount > 0) {
                // Tính số phù hợp hiện tại
                int currentIndex = getCurrentMatchIndex(totalCount);
                statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_STATUS_COUNT, currentIndex, totalCount));
            } else {
                statusLabel.setText("");
            }
            // Có kết quả: xóa viền đỏ
            searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
        } else {
            statusLabel.setText(noResultsText());
            // Không có kết quả và hộp tìm kiếm không trống: hiển thị đường viền màu đỏ (giống như IDEA hành vi)
            if (!searchField.getText().isEmpty()) {
                searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            }
        }
    }

    /**
     * Đếm tổng số kết quả phù hợp trong tài liệu
     */
    private int calculateTotalMatches() {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                return 0;
            }

            return countMatches(textArea.getText(), searchText);
        } catch (Exception e) {
            log.warn("Failed to calculate total matches", e);
            return 0;
        }
    }

    private int countMatches(String text, String searchText) {
        if (searchText.isEmpty()) {
            return 0;
        }

        int count = 0;
        int searchLength = searchText.length();
        int matchStart = findNextMatch(text, searchText, 0);
        while (matchStart >= 0) {
            count++;
            matchStart = findNextMatch(text, searchText, matchStart + searchLength);
        }
        return count;
    }

    /**
     * Tính chỉ số phù hợp tương ứng với vị trí hiện được chọn
     */
    private int calculateCurrentIndex(int selStart) {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                return 1;
            }

            String text = textArea.getText();
            int searchLength = searchText.length();
            int index = 0;
            int matchStart = findNextMatch(text, searchText, 0);

            while (matchStart >= 0) {
                index++;

                // Tìm vị trí bắt đầu đầu tiên >= Trận đấu ở vị trí hiện được chọn
                if (matchStart >= selStart) {
                    return index;
                }

                matchStart = findNextMatch(text, searchText, matchStart + searchLength);
            }

            // Nếu tất cả các kết quả trùng khớp xảy ra trước vị trí đã chọn thì tổng số sẽ được trả về
            return Math.max(1, index);
        } catch (Exception e) {
            log.warn("Failed to calculate current index", e);
            return 1;
        }
    }

    private int findNextMatch(String text, String searchText, int fromIndex) {
        int searchLength = searchText.length();
        int lastStart = text.length() - searchLength;
        if (searchLength == 0 || fromIndex > lastStart) {
            return -1;
        }

        boolean matchCase = searchField.isCaseSensitive();
        boolean wholeWord = searchField.isWholeWord();
        char firstChar = searchText.charAt(0);

        for (int index = Math.max(0, fromIndex); index <= lastStart; index++) {
            if (!charsEqual(text.charAt(index), firstChar, matchCase)) {
                continue;
            }
            if (!text.regionMatches(!matchCase, index, searchText, 0, searchLength)) {
                continue;
            }
            if (!wholeWord || isWholeWord(text, index, searchLength)) {
                return index;
            }
        }
        return -1;
    }

    private boolean charsEqual(char left, char right, boolean matchCase) {
        return matchCase
                ? left == right
                : (Character.toLowerCase(left) == Character.toLowerCase(right)
                || Character.toUpperCase(left) == Character.toUpperCase(right));
    }

    private boolean isWholeWord(String text, int offset, int length) {
        boolean wordBoundaryBefore = offset == 0 || !Character.isLetterOrDigit(text.charAt(offset - 1));
        int afterOffset = offset + length;
        boolean wordBoundaryAfter = afterOffset >= text.length()
                || !Character.isLetterOrDigit(text.charAt(afterOffset));
        return wordBoundaryBefore && wordBoundaryAfter;
    }

    /**
     * Tính số trận đấu hiện tại
     */
    private int getCurrentMatchIndex(int totalCount) {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty() || totalCount == 0) {
                return 1;
            }

            // Nhận vị trí hiện đang được chọn
            int currentSelStart = textArea.getSelectionStart();

            return calculateCurrentIndex(currentSelStart);
        } catch (Exception e) {
            log.warn("Failed to calculate match index", e);
            return 1;
        }
    }

    /**
     * Lên lịch cập nhật trạng thái tìm kiếm (chống rung)
     */
    private void scheduleSearchUpdate() {
        // Hủy bộ hẹn giờ trước đó
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }

        // Tạo bộ hẹn giờ chống rung mới.300ms được thực hiện sau này
        searchDebounceTimer = new Timer(300, e -> updateSearchStatus());
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    /**
     * Cập nhật trạng thái tìm kiếm (để cập nhật theo thời gian thực)
     */
    private void updateSearchStatus() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
            return;
        }

        // Lưu trạng thái hiện tại
        int savedCaret = textArea.getCaretPosition();
        int savedSelStart = textArea.getSelectionStart();
        int savedSelEnd = textArea.getSelectionEnd();

        try {
            // Tính tổng số trận đấu
            int totalCount = calculateTotalMatches();

            if (totalCount > 0) {
                // Khi nhập văn bản tìm kiếm, tìm kiếm bắt đầu ở đầu tài liệu và đánh dấu kết quả phù hợp đầu tiên
                textArea.setCaretPosition(0);
                SearchContext context = createSearchContext();
                context.setSearchForward(true);
                SearchResult firstResult = SearchEngine.find(textArea, context);

                if (firstResult.wasFound()) {
                    // Hiển thị vị trí trận đấu hiện tại và tổng số trận đấu.
                    statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_STATUS_COUNT, 1, totalCount));
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
                } else {
                    statusLabel.setText(noResultsText());
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                }
            } else {
                statusLabel.setText(noResultsText());
                searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            }
        } finally {
            // Con trỏ không được khôi phục và vẫn ở vị trí khớp đầu tiên
            // Nhưng nếu không tìm thấy thì khôi phục lại vị trí ban đầu
            if (statusLabel.getText().equals(noResultsText())) {
                textArea.setCaretPosition(savedCaret);
                textArea.setSelectionStart(savedSelStart);
                textArea.setSelectionEnd(savedSelEnd);
            }
        }
    }

    private static String noResultsText() {
        return UiI18n.get(UiMessageKeys.SEARCH_NO_RESULTS);
    }

    /**
     * Xóa vùng chọn của vùng văn bản
     */
    private void clearTextSelection() {
        if (textArea != null) {
            textArea.setSelectionStart(textArea.getCaretPosition());
            textArea.setSelectionEnd(textArea.getCaretPosition());
        }
    }

    /**
     * Xóa trạng thái đánh dấu tìm kiếm và lựa chọn
     */
    private void clearSearchHighlights() {
        if (textArea != null) {
            // Xóa lựa chọn
            clearTextSelection();
            // làm SearchEngine Xóa trạng thái tìm kiếm trước đó
            // Đặt lại trạng thái bằng cách thực hiện tìm kiếm trống
            SearchContext context = new SearchContext();
            context.setSearchFor("");
            context.setMarkAll(false);
            SearchEngine.find(textArea, context);
        }
    }
}
