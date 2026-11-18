package com.nvhuong.devworkbench.common.component;

import com.nvhuong.devworkbench.util.UiI18n;
import com.nvhuong.devworkbench.util.UiMessageKeys;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

/**
 * Vùng chứa trình soạn thảo văn bản có chức năng tìm kiếm và thay thế
 * <p>
 * Sẽ RSyntaxTextArea và SearchReplacePanel Kết hợp với nhau,
 * Việc thực hiện cũng tương tự DevWorkbench Chức năng tìm kiếm (Cmd+F thở ra, lơ lửng ở góc trên bên phải)
 * </p>
 *
 * <h2>Ví dụ sử dụng</h2>
 * <pre>{@code
 * // Tạo trình soạn thảo văn bản có chức năng tìm kiếm và thay thế (chức năng thay thế được bật theo mặc định)
 * SearchableTextArea searchableArea = new SearchableTextArea(textArea);
 *
 * // Tạo trình soạn thảo văn bản chỉ tìm kiếm (đã tắt chức năng thay thế)
 * SearchableTextArea searchOnlyArea = new SearchableTextArea(textArea, false);
 *
 * // Cấu hình trình soạn thảo văn bản
 * textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
 *
 * // Thêm vùng chứa vào giao diện
 * panel.add(searchableArea, BorderLayout.CENTER);
 *
 * // Cmd+F / Ctrl+F Một tìm kiếm sẽ được kích hoạt tự động
 * // Cmd+Shift+F / Ctrl+Shift+F Việc thay thế được kích hoạt tự động (chỉ khả dụng khi tính năng Thay thế được bật)
 * }</pre>
 */
public class SearchableTextArea extends JPanel {
    private static final String EDITOR_SHORTCUT_HINTS_INSTALLED = "devWorkbench.editorShortcutHintsInstalled";
    private static final String POPUP_MESSAGE_KEY_PROPERTY = "devWorkbench.editorPopupMessageKey";

    @Getter
    private final RSyntaxTextArea textArea;
    private final SyntaxEditorScrollPane scrollPane;
    private final SearchReplacePanel searchPanel;
    private final JPanel overlayPanel;
    private final boolean enableReplace;
    private JsonCopyTargetResolver.CopyTarget currentJsonCopyTarget;
    private int jsonContextPopupOffset = -1;

    /**
     * Tạo trình soạn thảo văn bản có chức năng tìm kiếm và thay thế (chức năng thay thế được bật theo mặc định)
     */
    public SearchableTextArea(RSyntaxTextArea textArea) {
        this(textArea, true);
    }

    /**
     * Tạo một trình soạn thảo văn bản với khả năng tìm kiếm
     *
     * @param textArea      Trình soạn thảo văn bản
     * @param enableReplace Có bật chức năng thay thế hay không
     */
    public SearchableTextArea(RSyntaxTextArea textArea, boolean enableReplace) {
        this.textArea = textArea;
        this.enableReplace = enableReplace;

        setLayout(new BorderLayout());

        scrollPane = new SyntaxEditorScrollPane(textArea);

        // Tạo bảng tìm kiếm và thay thế
        searchPanel = new SearchReplacePanel(textArea, enableReplace);

        // Theo dõi sự thay đổi kích thước của bảng tìm kiếm và tự động cập nhật vị trí
        searchPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSearchPanelPosition();
            }
        });

        // Tạo overlay Bảng điều khiển cho hộp tìm kiếm nổi
        overlayPanel = new JPanel(null) {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false; // Cho phép các thành phần chồng lên nhau
            }
        };
        overlayPanel.setOpaque(false);

        // sử dụng JLayeredPane Đạt được hiệu ứng nổi
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                super.doLayout();
                // Đảm bảo bảng cuộn lấp đầy toàn bộ layeredPane
                if (getComponentCount() > 0) {
                    scrollPane.setBounds(0, 0, getWidth(), getHeight());
                    overlayPanel.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };

        // Lớp dưới cùng: soạn thảo văn bản
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

        // Lớp trên: Bảng tìm kiếm (nổi ở góc trên bên phải)
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
        overlayPanel.add(searchPanel);

        add(layeredPane, BorderLayout.CENTER);

        installLineNavigationKeyBindings();
        installEditorShortcutHints();

        // Theo dõi sự thay đổi kích thước và điều chỉnh vị trí bảng tìm kiếm
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSearchPanelPosition();
            }
        });

        // Đăng ký phím tắt
        registerKeyBindings();
    }

    /**
     * Đăng ký phím tắt
     */
    private void registerKeyBindings() {
        // Cmd+F / Ctrl+F - Hiển thị tìm kiếm
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutMask());
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(findKey, "showSearch");
        textArea.getActionMap().put("showSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.showSearch();
                updateSearchPanelPosition();
            }
        });

        // Cmd+Shift+F / Ctrl+Shift+F - Hiển thị các thay thế (chỉ được đăng ký khi tính năng thay thế được bật)
        if (enableReplace) {
            KeyStroke replaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                    menuShortcutMask() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);

            textArea.getInputMap(JComponent.WHEN_FOCUSED).put(replaceKey, "showReplace");
            textArea.getActionMap().put("showReplace", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    searchPanel.showReplace();
                    updateSearchPanelPosition();
                }
            });
        }
    }

    private int menuShortcutMask() {
        if (GraphicsEnvironment.isHeadless()) {
            return InputEvent.CTRL_DOWN_MASK;
        }
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    private void installLineNavigationKeyBindings() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        bind(inputMap, KeyEvent.VK_HOME, 0, DefaultEditorKit.beginLineAction);
        bind(inputMap, KeyEvent.VK_END, 0, DefaultEditorKit.endLineAction);
        bind(inputMap, KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK, DefaultEditorKit.selectionBeginLineAction);
        bind(inputMap, KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK, DefaultEditorKit.selectionEndLineAction);
        bind(inputMap, KeyEvent.VK_LEFT, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                DefaultEditorKit.selectionBeginLineAction);
        bind(inputMap, KeyEvent.VK_RIGHT, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                DefaultEditorKit.selectionEndLineAction);
    }

    private void bind(InputMap inputMap, int keyCode, int modifiers, String actionName) {
        inputMap.put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }

    private void installEditorShortcutHints() {
        if (Boolean.TRUE.equals(textArea.getClientProperty(EDITOR_SHORTCUT_HINTS_INSTALLED))) {
            return;
        }

        JPopupMenu sourceMenu = textArea.getPopupMenu();
        if (sourceMenu == null) {
            return;
        }

        JPopupMenu popupMenu = createEnhancedEditorPopupMenu(sourceMenu);
        textArea.setPopupMenu(popupMenu);
        installPopupOffsetTracker();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshEnhancedEditorPopupMenu(popupMenu);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                clearJsonPopupTarget();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                clearJsonPopupTarget();
            }
        });
        refreshEnhancedEditorPopupMenu(popupMenu);
        textArea.putClientProperty(EDITOR_SHORTCUT_HINTS_INSTALLED, Boolean.TRUE);
    }

    private JPopupMenu createEnhancedEditorPopupMenu(JPopupMenu sourceMenu) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyKeyItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_KEY);
        copyKeyItem.addActionListener(e -> copyJsonTarget(true));
        JMenuItem copyValueItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_VALUE);
        copyValueItem.addActionListener(e -> copyJsonTarget(false));
        popupMenu.add(copyKeyItem);
        popupMenu.add(copyValueItem);
        popupMenu.addSeparator();

        if (textArea.isEditable()) {
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.UNDO_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.REDO_ACTION));
            popupMenu.addSeparator();
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.CUT_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.COPY_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.PASTE_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.DELETE_ACTION));
            popupMenu.addSeparator();
        } else {
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.COPY_ACTION));
        }
        JMenuItem copyAllItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_ALL);
        copyAllItem.addActionListener(e -> copyAllText());
        popupMenu.add(copyAllItem);
        addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.SELECT_ALL_ACTION));
        JMenu foldingMenu = findFoldingMenu(sourceMenu);
        if (foldingMenu != null) {
            popupMenu.addSeparator();
            popupMenu.add(foldingMenu);
        }
        popupMenu.addSeparator();
        popupMenu.add(disabledHint(editorLineSelectionShortcutHint()));
        return popupMenu;
    }

    private void refreshEnhancedEditorPopupMenu(JPopupMenu popupMenu) {
        currentJsonCopyTarget = resolveCurrentJsonCopyTarget();
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenu menu) {
                localizeFoldingMenu(menu);
            } else if (component instanceof JMenuItem menuItem) {
                localizeEditorMenuItem(menuItem);
                refreshEditorMenuItemState(menuItem);
            }
        }
    }

    private JMenuItem findActionMenuItem(JPopupMenu menu, int actionConstant) {
        Action action = RTextArea.getAction(actionConstant);
        if (action == null) {
            return null;
        }
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem menuItem && menuItem.getAction() == action) {
                return menuItem;
            }
        }
        return new JMenuItem(action);
    }

    private JMenu findFoldingMenu(JPopupMenu menu) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenu foldingMenu) {
                return foldingMenu;
            }
        }
        return null;
    }

    private void addIfPresent(JPopupMenu popupMenu, JMenuItem menuItem) {
        if (menuItem != null) {
            popupMenu.add(menuItem);
        }
    }

    private JMenuItem createMessageMenuItem(String messageKey) {
        JMenuItem menuItem = new JMenuItem(UiI18n.get(messageKey));
        menuItem.putClientProperty(POPUP_MESSAGE_KEY_PROPERTY, messageKey);
        return menuItem;
    }

    private void localizeEditorMenuItem(JMenuItem menuItem) {
        Object messageKey = menuItem.getClientProperty(POPUP_MESSAGE_KEY_PROPERTY);
        if (messageKey instanceof String key) {
            menuItem.setText(UiI18n.get(key));
            return;
        }

        Action action = menuItem.getAction();
        if (action == RTextArea.getAction(RTextArea.UNDO_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_UNDO));
        } else if (action == RTextArea.getAction(RTextArea.REDO_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_REDO));
        } else if (action == RTextArea.getAction(RTextArea.CUT_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_CUT));
        } else if (action == RTextArea.getAction(RTextArea.COPY_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_SELECTED));
        } else if (action == RTextArea.getAction(RTextArea.PASTE_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_PASTE));
        } else if (action == RTextArea.getAction(RTextArea.DELETE_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_DELETE));
        } else if (action == RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_SELECT_ALL));
        }
    }

    private void refreshEditorMenuItemState(JMenuItem menuItem) {
        Object messageKey = menuItem.getClientProperty(POPUP_MESSAGE_KEY_PROPERTY);
        if (UiMessageKeys.EDITOR_POPUP_COPY_KEY.equals(messageKey)) {
            menuItem.setEnabled(currentJsonCopyTarget != null && currentJsonCopyTarget.key() != null);
        } else if (UiMessageKeys.EDITOR_POPUP_COPY_VALUE.equals(messageKey)) {
            menuItem.setEnabled(currentJsonCopyTarget != null && currentJsonCopyTarget.value() != null);
        } else if (UiMessageKeys.EDITOR_POPUP_COPY_ALL.equals(messageKey)) {
            menuItem.setEnabled(textArea.getDocument().getLength() > 0);
        }
    }

    private void localizeFoldingMenu(JMenu foldingMenu) {
        foldingMenu.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_FOLDING));
        setMenuItemText(foldingMenu, 0, UiMessageKeys.EDITOR_POPUP_TOGGLE_CURRENT_FOLD);
        setMenuItemText(foldingMenu, 1, UiMessageKeys.EDITOR_POPUP_COLLAPSE_COMMENT_FOLDS);
        setMenuItemText(foldingMenu, 2, UiMessageKeys.EDITOR_POPUP_COLLAPSE_ALL_FOLDS);
        setMenuItemText(foldingMenu, 3, UiMessageKeys.EDITOR_POPUP_EXPAND_ALL_FOLDS);
    }

    private void setMenuItemText(JMenu menu, int index, String messageKey) {
        if (index < menu.getItemCount()) {
            JMenuItem item = menu.getItem(index);
            if (item != null) {
                item.setText(UiI18n.get(messageKey));
            }
        }
    }

    private String editorLineSelectionShortcutHint() {
        if (isMac()) {
            return UiI18n.get(UiMessageKeys.EDITOR_SHORTCUTS_SELECT_LINE_MAC);
        }
        return UiI18n.get(UiMessageKeys.EDITOR_SHORTCUTS_SELECT_LINE_DEFAULT);
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private void installPopupOffsetTracker() {
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rememberJsonPopupOffset(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                rememberJsonPopupOffset(e);
            }
        });
    }

    private void rememberJsonPopupOffset(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int offset = textArea.viewToModel2D(e.getPoint());
        rememberJsonPopupOffset(offset);
    }

    void rememberJsonPopupOffset(int offset) {
        if (offset >= 0 && offset <= textArea.getDocument().getLength()) {
            jsonContextPopupOffset = offset;
            if (!isOffsetInsideSelection(offset)) {
                textArea.setCaretPosition(offset);
            }
        }
    }

    private boolean isOffsetInsideSelection(int offset) {
        int selectionStart = textArea.getSelectionStart();
        int selectionEnd = textArea.getSelectionEnd();
        return selectionStart != selectionEnd && offset >= selectionStart && offset < selectionEnd;
    }

    private JsonCopyTargetResolver.CopyTarget resolveCurrentJsonCopyTarget() {
        String text = textArea.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }
        int offset = jsonContextPopupOffset >= 0 ? jsonContextPopupOffset : textArea.getCaretPosition();
        return JsonCopyTargetResolver.resolve(text, offset).orElse(null);
    }

    private void copyJsonTarget(boolean copyKey) {
        JsonCopyTargetResolver.CopyTarget target = currentJsonCopyTarget != null
                ? currentJsonCopyTarget
                : resolveCurrentJsonCopyTarget();
        if (target == null) {
            return;
        }

        copyTextToClipboard(copyKey ? target.key() : target.value());
    }

    private void copyAllText() {
        copyTextToClipboard(textArea.getText());
    }

    private void copyTextToClipboard(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (RuntimeException ignored) {
            // Clipboard may be unavailable in headless or restricted environments.
        }
    }

    private void clearJsonPopupTarget() {
        currentJsonCopyTarget = null;
        jsonContextPopupOffset = -1;
    }

    private JMenuItem disabledHint(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(false);
        return item;
    }

    /**
     * Đã cập nhật vị trí bảng tìm kiếm (góc trên bên phải)
     */
    private void updateSearchPanelPosition() {
        if (!searchPanel.isVisible()) {
            return;
        }

        // Trước tiên hãy để bảng tính toán kích thước tối ưu
        searchPanel.revalidate();
        Dimension panelSize = searchPanel.getPreferredSize();

        // Đảm bảo các tấm có đủ chiều rộng và chiều cao
        int panelWidth = Math.max(panelSize.width, 250);  // Chiều rộng tối thiểu 250px
        int panelHeight = panelSize.height;

        // Tính vị trí của góc trên bên phải
        int x = Math.max(10, overlayPanel.getWidth() - panelWidth - 10);
        int y = 5;

        // Đặt vị trí và kích thước của bảng điều khiển
        searchPanel.setBounds(x, y, panelWidth, panelHeight);
        overlayPanel.setSize(overlayPanel.getParent().getSize());
        overlayPanel.revalidate();
        overlayPanel.repaint();
    }

    /**
     * Hiển thị bảng tìm kiếm
     */
    public void showSearch() {
        searchPanel.showSearch();
        updateSearchPanelPosition();
    }

    /**
     * Hiển thị bảng thay thế
     */
    public void showReplace() {
        searchPanel.showReplace();
        updateSearchPanelPosition();
    }

    /**
     * Định cấu hình có hiển thị số dòng để duy trì trải nghiệm soạn thảo gốc hay không
     */
    public void setLineNumbersEnabled(boolean enabled) {
        scrollPane.setLineNumbersEnabled(enabled);
    }
}
