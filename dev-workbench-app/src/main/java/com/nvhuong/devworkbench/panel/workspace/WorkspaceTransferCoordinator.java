package com.nvhuong.devworkbench.panel.workspace;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.dialog.WorkspaceSelectionDialog;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.service.WorkspaceService;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Chuyển giao không gian làm việc UI Điều phối viên.
 * Cung cấp quy trình lựa chọn không gian làm việc chung, xác nhận và nhắc lỗi để giảm sự trùng lặp mã ở cấp bảng điều khiển.
 */
@Slf4j
@UtilityClass
public class WorkspaceTransferCoordinator {

    /**
     * Quy trình chung để truyền dữ liệu sang không gian làm việc khác (hiển thị thông báo thành công)
     *
     * @param itemName       Tên của dự án sẽ được chuyển giao (được sử dụng cho nhật ký và hộp thoại xác nhận)
     * @param transferAction Hoạt động thực tế của việc thực hiện chuyển giao (selectedWorkspace, itemName) -> void
     */
    public static void transferToWorkspace(String itemName, BiConsumer<Workspace, String> transferAction) {
        try {
            List<Workspace> availableWorkspaces = getAvailableWorkspaces();
            if (availableWorkspaces.isEmpty()) {
                showNoWorkspaceAvailableMessage();
                return;
            }

            String dialogTitle = I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_SELECT_DIALOG_TITLE);
            Workspace selectedWorkspace = showWorkspaceSelectionDialog(dialogTitle, availableWorkspaces);
            if (selectedWorkspace == null) {
                return;
            }

            if (!confirmTransfer(itemName, selectedWorkspace.getName())) {
                return;
            }

            transferAction.accept(selectedWorkspace, itemName);
            log.info("Successfully transferred '{}' to workspace '{}'", itemName, selectedWorkspace.getName());

        } catch (Exception ex) {
            log.error("Transfer to workspace failed for '{}'", itemName, ex);
            showErrorMessage(ex.getMessage());
        }
    }

    private static List<Workspace> getAvailableWorkspaces() {
        WorkspaceService workspaceService = WorkspaceService.getInstance();
        List<Workspace> allWorkspaces = workspaceService.getAllWorkspaces();
        Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

        return allWorkspaces.stream()
                .filter(w -> currentWorkspace == null || !w.getId().equals(currentWorkspace.getId()))
                .toList();
    }

    private static Workspace showWorkspaceSelectionDialog(String title, List<Workspace> workspaces) {
        WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(title, workspaces);
        return dialog.showDialog();
    }

    private static boolean confirmTransfer(String itemName, String targetWorkspaceName) {
        int confirm = JOptionPane.showConfirmDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_MESSAGE, itemName, targetWorkspaceName),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return confirm == JOptionPane.YES_OPTION;
    }

    private static void showNoWorkspaceAvailableMessage() {
        JOptionPane.showMessageDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_NO_OTHER_AVAILABLE),
                I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                JOptionPane.WARNING_MESSAGE);
    }

    private static void showErrorMessage(String errorDetails) {
        JOptionPane.showMessageDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_FAIL, errorDetails),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE);
    }
}
