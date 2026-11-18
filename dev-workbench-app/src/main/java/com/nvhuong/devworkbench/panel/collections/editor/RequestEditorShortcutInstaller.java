package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.panel.collections.editor.request.sub.RequestLinePanel;
import com.nvhuong.devworkbench.service.setting.ShortcutManager;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

/**
 * Yêu cầu trình cài đặt lối tắt soạn thảo.
 * <p>
 * Phím tắt thuộc về Swing Logic điều khiển được đặt bên ngoài bảng điều khiển để tránh điều đó RequestEditorPanel Cũng chịu trách nhiệm về bố cục và ràng buộc hành động.
 */
@RequiredArgsConstructor
final class RequestEditorShortcutInstaller {
    private static final String ACTION_SEND_REQUEST = "sendRequest";
    private static final String ACTION_SAVE_REQUEST = "saveRequest";
    private static final String ACTION_NEW_REQUEST_TAB = "newRequestTab";
    private static final String ACTION_CLOSE_CURRENT_TAB = "closeCurrentTab";
    private static final String ACTION_CLOSE_OTHER_TABS = "closeOtherTabs";
    private static final String ACTION_CLOSE_ALL_TABS = "closeAllTabs";

    private final JComponent shortcutOwner;
    private final Supplier<RequestEditSubPanel> currentSubPanelSupplier;
    private final Runnable newRequestTabAction;
    private final Runnable closeCurrentTabAction;
    private final Runnable closeOtherTabsAction;
    private final Runnable closeAllTabsAction;

    void install() {
        InputMap inputMap = shortcutOwner.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = shortcutOwner.getActionMap();

        inputMap.clear();
        actionMap.clear();

        bind(inputMap, actionMap, ShortcutManager.SEND_REQUEST, ACTION_SEND_REQUEST, this::clickCurrentSendButton);
        bind(inputMap, actionMap, ShortcutManager.SAVE_REQUEST, ACTION_SAVE_REQUEST, this::clickCurrentSaveButton);
        bind(inputMap, actionMap, ShortcutManager.NEW_REQUEST, ACTION_NEW_REQUEST_TAB, newRequestTabAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_CURRENT_TAB, ACTION_CLOSE_CURRENT_TAB, closeCurrentTabAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_OTHER_TABS, ACTION_CLOSE_OTHER_TABS, closeOtherTabsAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_ALL_TABS, ACTION_CLOSE_ALL_TABS, closeAllTabsAction);
    }

    private void bind(InputMap inputMap, ActionMap actionMap, String shortcutKey, String actionName, Runnable action) {
        KeyStroke keyStroke = ShortcutManager.getKeyStroke(shortcutKey);
        if (keyStroke == null) {
            return;
        }
        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void clickCurrentSendButton() {
        RequestLinePanel requestLinePanel = currentRequestLinePanel();
        if (requestLinePanel == null) {
            return;
        }
        clickAfterCommittingCellEditor(requestLinePanel.getSendButton());
    }

    private void clickCurrentSaveButton() {
        RequestLinePanel requestLinePanel = currentRequestLinePanel();
        if (requestLinePanel == null) {
            return;
        }
        clickAfterCommittingCellEditor(requestLinePanel.getSaveButton());
    }

    private RequestLinePanel currentRequestLinePanel() {
        RequestEditSubPanel currentSubPanel = currentSubPanelSupplier.get();
        if (currentSubPanel == null) {
            return null;
        }
        return currentSubPanel.getRequestLinePanel();
    }

    private void clickAfterCommittingCellEditor(JButton button) {
        if (button == null || !button.isEnabled()) {
            return;
        }
        // Tiêu điểm rơi vào nút trước tiên, kích hoạt biểu mẫu cell editor của terminateEditOnFocusLost，Sau đó thực hiện các hành động thực tế.
        button.requestFocusInWindow();
        SwingUtilities.invokeLater(button::doClick);
    }
}
