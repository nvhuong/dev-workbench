package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * Request an editor Tab Khởi tạo bộ lập lịch.
 * <p>
 * Nội dung trình chỉnh sửa sử dụng quá trình khởi tạo bị trì hoãn và logic lập lịch tập trung ở đây để tránh mã bố cục bảng điều khiển bị trộn vào các chi tiết khởi động và làm nóng trước.
 */
@RequiredArgsConstructor
final class RequestEditorTabInitializationScheduler {
    private final JTabbedPane tabbedPane;
    private final BooleanSupplier startupRestoreSelectionInProgress;

    void initializeSelectedTabSoon() {
        if (startupRestoreSelectionInProgress.getAsBoolean()) {
            return;
        }
        scheduleSelectedRequestTabInitialization(false);
    }

    void initializeSelectedStartupRestoreTab() {
        scheduleSelectedRequestTabInitialization(true);
    }

    void warmUpDeferredRequestTabsAfterStartup() {
        SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(0));
    }

    private void scheduleSelectedRequestTabInitialization(boolean animatePlaceholderTransition) {
        SwingUtilities.invokeLater(() -> {
            if (startupRestoreSelectionInProgress.getAsBoolean()) {
                return;
            }
            ensureSelectedRequestTabInitialized(animatePlaceholderTransition);
        });
    }

    private void ensureSelectedRequestTabInitialized(boolean animatePlaceholderTransition) {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof RequestEditSubPanel requestEditSubPanel
                && !requestEditSubPanel.isEditorInitialized()) {
            requestEditSubPanel.ensureEditorInitialized(animatePlaceholderTransition);
        }
    }

    private void warmUpDeferredRequestTabsSequentially(int startIndex) {
        for (int i = Math.max(0, startIndex); i < tabbedPane.getTabCount(); i++) {
            if (i == tabbedPane.getSelectedIndex()) {
                continue;
            }
            Component component = tabbedPane.getComponentAt(i);
            if (!(component instanceof RequestEditSubPanel requestEditSubPanel)
                    || requestEditSubPanel.isEditorInitialized()) {
                continue;
            }
            requestEditSubPanel.ensureEditorInitialized(false);
            int nextIndex = i + 1;
            SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(nextIndex));
            return;
        }
    }
}
