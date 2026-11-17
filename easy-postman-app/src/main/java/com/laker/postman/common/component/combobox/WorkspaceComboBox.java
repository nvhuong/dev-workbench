package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

/**
 * Thành phần hộp thả xuống không gian làm việc
 */
public class WorkspaceComboBox extends MenuBarComboBox<Workspace> {
    private boolean isUpdating = false;
    @Setter
    private Consumer<Workspace> onWorkspaceChange;

    public WorkspaceComboBox() {
        setRenderer(new WorkspaceItemRenderer());
        addActionListener(e -> {
            if (isUpdating) return;
            Workspace workspace = (Workspace) getSelectedItem();
            if (workspace != null && onWorkspaceChange != null) {
                onWorkspaceChange.accept(workspace);
            }
        });
        reload();
    }

    /**
     * Tải lại danh sách không gian làm việc
     */
    public void reload() {
        isUpdating = true;
        removeAllItems();

        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            List<Workspace> workspaces = workspaceService.getAllWorkspaces();
            Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

            for (Workspace workspace : workspaces) {
                addItem(workspace);
            }

            // Đặt không gian làm việc hiện được chọn
            if (currentWorkspace != null) {
                setSelectedItem(currentWorkspace);
            } else if (getItemCount() > 0) {
                setSelectedIndex(0);
            }
        } catch (Exception e) {
            // Xử lý âm thầm khi tải không thành công
        } finally {
            isUpdating = false;
        }
    }
}
