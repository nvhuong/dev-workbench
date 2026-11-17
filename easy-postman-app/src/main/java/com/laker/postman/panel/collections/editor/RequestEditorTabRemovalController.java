package com.laker.postman.panel.collections.editor;

import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;

/**
 * Request an editor Tab Tháo bộ điều khiển.
 * <p>
 * xóa bỏ Tab Nó cũng chịu trách nhiệm chạy phát hành tài nguyên và tạm thời Tab Chỉnh sửa chỉ mục để tránh logic đóng nằm rải rác trong bảng điều khiển.
 */
@RequiredArgsConstructor
final class RequestEditorTabRemovalController {
    private final JTabbedPane tabbedPane;
    private final RequestEditorTransientTabManager transientTabManager;

    void removeAt(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }

        Component component = tabbedPane.getComponentAt(index);
        RequestEditorTabResourceCleaner.cleanup(component);
        transientTabManager.onTabRemoved(component, index);
        tabbedPane.removeTabAt(index);
    }

    void removeComponent(Component component) {
        int index = tabbedPane.indexOfComponent(component);
        if (index >= 0) {
            removeAt(index);
        }
    }
}
