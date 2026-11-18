package com.nvhuong.devworkbench.panel.collections;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.exception.CancelException;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.service.collections.OpenedRequestTabsStore;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class OpenedRequestTabSessionSaver {

    public static void saveOpenTabsOnExit() {
        // Trình chỉnh sửa được yêu cầu vẫn chưa được tạo trong quá trình hiển thị shell khởi động và không thể tạo trình chỉnh sửa trống và ghi đè phiên cuối cùng để thoát.
        RequestEditorPanel editPanel = UiSingletonFactory.getExistingInstance(RequestEditorPanel.class).orElse(null);
        if (editPanel == null) {
            return;
        }
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        List<Integer> unsavedTabs = OpenedRequestTabSnapshotCollector.findUnsavedTabs(tabbedPane);

        boolean saveAll = false;
        if (!unsavedTabs.isEmpty()) {
            int result = showUnsavedChangesDialog();
            if (result == 2 || result == JOptionPane.CLOSED_OPTION) {
                throw new CancelException();
            }
            if (result == 0) {
                saveUnsavedTabs(editPanel, tabbedPane, unsavedTabs);
                saveAll = true;
            }
        }

        List<HttpRequestItem> openedRequestItems =
                OpenedRequestTabSnapshotCollector.collectOpenedRequestItems(tabbedPane, saveAll);
        // Phiên cũ vẫn còn trên đĩa cho đến khi bộ sưu tập được tải. Giữ nó trước khi hợp nhất các tab người dùng vừa mở.
        if (new File(OpenedRequestTabsStore.PATHNAME).isFile()) {
            if (openedRequestItems.isEmpty()) {
                return;
            }
            openedRequestItems = mergeWithPendingSession(OpenedRequestTabsStore.loadAll(), openedRequestItems);
        }
        OpenedRequestTabsStore.saveAll(OpenedRequestTabSnapshotCollector.limitToMostRecent(
                openedRequestItems,
                SettingManager.getMaxOpenedRequestsCount()
        ));
    }

    static List<HttpRequestItem> mergeWithPendingSession(List<HttpRequestItem> pending,
                                                          List<HttpRequestItem> current) {
        List<HttpRequestItem> merged = new ArrayList<>(pending);
        for (HttpRequestItem item : current) {
            String id = item.getId();
            if (id != null && !id.isBlank()) {
                merged.removeIf(old -> id.equals(old.getId()));
            }
            merged.add(item);
        }
        return merged;
    }

    private static int showUnsavedChangesDialog() {
        String[] options = {
                I18nUtil.getMessage(MessageKeys.EXIT_SAVE_ALL),
                I18nUtil.getMessage(MessageKeys.EXIT_DISCARD_ALL),
                I18nUtil.getMessage(MessageKeys.EXIT_CANCEL)
        };
        return JOptionPane.showOptionDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES),
                I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES_TITLE),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
    }

    static void saveUnsavedTabs(RequestEditorPanel editPanel, JTabbedPane tabbedPane, List<Integer> unsavedTabs) {
        for (Integer i : unsavedTabs) {
            tabbedPane.setSelectedIndex(i);
            if (!editPanel.saveCurrentRequest()) {
                // Những thẻ bị lỗi hoặc chưa được lưu sẽ được giữ lại để người dùng tiếp tục chỉnh sửa và không thể coi là đã lưu rồi thoát ra.
                throw new CancelException();
            }
        }
    }
}
