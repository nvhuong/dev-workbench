package com.nvhuong.devworkbench.common.component;


import com.nvhuong.devworkbench.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phiên bản nâng cao hỗ trợ tự động hoàn thành DevWorkbenchTextField
 * Kế thừa từ DevWorkbenchTextField，Tất cả chức năng ban đầu được giữ lại (đánh dấu biến, hoàn tác và làm lại, v.v.)
 * Mới: Hỗ trợ tự động hoàn thành văn bản thông thường
 */
public class AutoCompleteDevWorkbenchTextField extends DevWorkbenchTextField {
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 28;
    private static final int MIN_POPUP_WIDTH = 200;
    private static final int POPUP_BORDER_WIDTH = 4;

    private JWindow popup;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private List<String> suggestions;
    private boolean autoCompleteEnabled = false;
    private boolean isUpdatingSuggestions = false;

    public AutoCompleteDevWorkbenchTextField(int columns) {
        super(columns);
        initAutoComplete();
    }

    public AutoCompleteDevWorkbenchTextField(String text, int columns) {
        super(text, columns);
        initAutoComplete();
    }

    public AutoCompleteDevWorkbenchTextField(String text, int columns, String placeholderText) {
        super(text, columns, placeholderText);
        initAutoComplete();
    }

    /**
     * Thiết lập danh sách đề xuất tự động hoàn thành
     */
    public void setSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            this.suggestions = Collections.emptyList();
            this.autoCompleteEnabled = false;
            hidePopup();
        } else {
            // Chỉ cập nhật khi danh sách thực sự thay đổi
            if (!suggestions.equals(this.suggestions)) {
                this.suggestions = new ArrayList<>(suggestions);
            }
        }
    }

    /**
     * Bật hoặc tắt tính năng tự động hoàn thành
     */
    public void setAutoCompleteEnabled(boolean enabled) {
        this.autoCompleteEnabled = enabled;
        if (!enabled) {
            hidePopup();
        }
    }

    private void initAutoComplete() {
        this.suggestions = Collections.emptyList();
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.popup = new JWindow();

        popup.setFocusableWindowState(false);
        popup.setType(Window.Type.POPUP);

        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(MAX_VISIBLE_ROWS);
        suggestionList.setFont(getFont());
        suggestionList.setFocusable(false);

        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        // Sử dụng màu đường viền nhận biết chủ đề, màu tối/Tất cả đều được thể hiện rõ ràng dưới các chủ đề nhẹ nhàng
        scrollPane.setBorder(new LineBorder(UIManager.getColor("Component.borderColor") != null
                ? UIManager.getColor("Component.borderColor")
                : ModernColors.getDividerBorderColor(), 1));
        popup.add(scrollPane);

        setupAutoCompleteListeners();
    }

    private void setupAutoCompleteListeners() {
        // Theo dõi thay đổi văn bản - Tối ưu hóa: Giảm các cập nhật không cần thiết
        getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void scheduleUpdate() {
                if (autoCompleteEnabled && !isUpdatingSuggestions) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }
        });

        // Xử lý sự kiện bàn phím
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible() || !autoCompleteEnabled) {
                    return;
                }
                handleKeyboardNavigation(e);
            }
        });

        // Click chuột để chọn
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    acceptSelectedSuggestion();
                }
            }
        });

        // Ẩn khi mất tiêu điểm
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!popup.isActive()) {
                        hidePopup();
                    }
                });
            }
        });
    }

    private void handleKeyboardNavigation(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                e.consume();
                moveSelection(1);
                break;
            case KeyEvent.VK_UP:
                e.consume();
                moveSelection(-1);
                break;
            case KeyEvent.VK_ENTER:
                // Enter Chấp nhận đề xuất hiện được chọn
                if (suggestionList.getSelectedIndex() >= 0) {
                    e.consume();
                    acceptSelectedSuggestion();
                }
                break;
            case KeyEvent.VK_TAB:
                // Tab Đóng cửa sổ bật lên và để biểu mẫu Tab Điều hướng xử lý chuyển động lấy nét
                // Đừng tiêu thụ sự kiện và để sự kiện tiếp tục được chuyển lên bàn InputMap
                hidePopup();
                break;
            case KeyEvent.VK_ESCAPE:
                e.consume();
                hidePopup();
                break;
            default:
                // Do nothing for other keys
                break;
        }
    }

    private void moveSelection(int direction) {
        int currentIndex = suggestionList.getSelectedIndex();
        int newIndex = currentIndex + direction;

        if (newIndex >= 0 && newIndex < listModel.getSize()) {
            suggestionList.setSelectedIndex(newIndex);
            suggestionList.ensureIndexIsVisible(newIndex);
        }
    }

    private void updateSuggestions() {
        if (!autoCompleteEnabled || suggestions.isEmpty() || isUpdatingSuggestions) {
            hidePopup();
            return;
        }

        isUpdatingSuggestions = true;
        try {
            String text = getText();
            text = (text == null) ? "" : text.trim();

            listModel.clear();

            if (text.isEmpty()) {
                addAllSuggestions();
            } else {
                filterAndAddSuggestions(text);
            }

            updatePopupVisibility();
        } finally {
            isUpdatingSuggestions = false;
        }
    }

    private void addAllSuggestions() {
        for (String suggestion : suggestions) {
            listModel.addElement(suggestion);
        }
    }

    private void filterAndAddSuggestions(String text) {
        // Kiểm tra sự trùng khớp chính xác
        if (hasExactMatch(text)) {
            return; // Không hiển thị danh sách gợi ý cho các kết quả khớp chính xác
        }

        // Chiến lược kết hợp ba cấp độ: tiền tố chính xác > Tiền tố trường hợp bị bỏ qua > Chứa các trận đấu
        if (tryAddPrefixMatches(text, true)) {
            return;
        }
        if (tryAddPrefixMatches(text, false)) {
            return;
        }
        addContainsMatches(text);
    }

    private boolean hasExactMatch(String text) {
        for (String suggestion : suggestions) {
            if (suggestion.equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryAddPrefixMatches(String text, boolean caseSensitive) {
        boolean found = false;
        String compareText = caseSensitive ? text : text.toLowerCase();

        for (String suggestion : suggestions) {
            String compareSuggestion = caseSensitive ? suggestion : suggestion.toLowerCase();
            if (compareSuggestion.startsWith(compareText)) {
                listModel.addElement(suggestion);
                found = true;
            }
        }
        return found;
    }

    private void addContainsMatches(String text) {
        String lowerText = text.toLowerCase();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().contains(lowerText)) {
                listModel.addElement(suggestion);
            }
        }
    }

    private void updatePopupVisibility() {
        if (listModel.isEmpty()) {
            hidePopup();
        } else {
            suggestionList.setSelectedIndex(0);
            showPopup();
        }
    }

    private void showPopup() {
        if (!isShowing()) {
            hidePopup();
            return;
        }

        try {
            Point location = getLocationOnScreen();
            Rectangle screenBounds = getCurrentScreenBounds();

            int popupWidth = Math.max(getWidth(), MIN_POPUP_WIDTH);
            int popupHeight = Math.min(listModel.getSize() * ROW_HEIGHT + POPUP_BORDER_WIDTH,
                                      MAX_VISIBLE_ROWS * ROW_HEIGHT + POPUP_BORDER_WIDTH);

            int x = location.x;
            int y = location.y + getHeight();

            // Kiểm tra đường viền bên phải
            if (x + popupWidth > screenBounds.x + screenBounds.width) {
                x = screenBounds.x + screenBounds.width - popupWidth - 10;
            }
            if (x < screenBounds.x) {
                x = screenBounds.x;
            }

            // Kiểm tra viền dưới và hiển thị nó ở trên nếu không đủ chỗ
            if (y + popupHeight > screenBounds.y + screenBounds.height) {
                y = location.y - popupHeight;
                // Nếu phần trên không đủ, hãy hiển thị càng nhiều càng tốt ở phần dưới
                if (y < screenBounds.y) {
                    y = location.y + getHeight();
                    popupHeight = Math.min(popupHeight, screenBounds.y + screenBounds.height - y - 10);
                }
            }

            popup.setLocation(x, y);
            popup.setSize(popupWidth, popupHeight);

            if (!popup.isVisible()) {
                popup.setVisible(true);
            }
        } catch (IllegalComponentStateException e) {
            // Bỏ qua nếu thành phần không hiển thị
            hidePopup();
        }
    }

    private Rectangle getCurrentScreenBounds() {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        if (graphicsConfiguration != null) {
            return graphicsConfiguration.getBounds();
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(0, 0, screenSize.width, screenSize.height);
    }

    private void hidePopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
        }
    }

    private void acceptSelectedSuggestion() {
        String selected = suggestionList.getSelectedValue();
        if (selected != null) {
            isUpdatingSuggestions = true;
            try {
                setText(selected);
                setCaretPosition(selected.length());
            } finally {
                isUpdatingSuggestions = false;
            }
            hidePopup();
        }
    }

    /**
     * Hiển thị tất cả các đề xuất (thường được gọi là tiêu điểm hoặc nhấp chuột)
     */
    public void showAllSuggestions() {
        if (autoCompleteEnabled && !suggestions.isEmpty()) {
            updateSuggestions();
        }
    }

    /**
     * Trả về xem cửa sổ bật lên tự động hoàn thành hiện có hiển thị hay không
     */
    public boolean isAutoCompletePopupVisible() {
        return popup != null && popup.isVisible();
    }

    /**
     * Chấp nhận đề xuất hiện được đánh dấu (đối với các cuộc gọi bên ngoài, tương đương với việc người dùng nhấn Enter）
     */
    public void acceptCurrentSuggestion() {
        if (popup != null && popup.isVisible() && suggestionList.getSelectedIndex() >= 0) {
            acceptSelectedSuggestion();
        }
    }
}
