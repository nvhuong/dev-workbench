package com.nvhuong.devworkbench.panel.workspace.components;

import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

/**
 * Lớp cơ sở hộp thoại có hiển thị tiến trình
 * Cung cấp hiển thị tiến trình chung, điều khiển nút và các chức năng khác
 */
public abstract class ProgressDialog extends JDialog {

    @Getter
    protected boolean confirmed = false;
    @Getter
    protected boolean operationInProgress = false;

    protected ProgressPanel progressPanel;
    protected JButton confirmButton;
    protected JButton cancelButton;
    private SwingWorker<Void, String> currentWorker;

    protected ProgressDialog(Window parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
    }

    /**
     * Khởi tạo hộp thoại
     */
    protected void initDialog() {
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        setupLayout();
        setupEventHandlers();
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * Đặt bố cục - Các lớp con cần được thực hiện
     */
    protected abstract void setupLayout();

    /**
     * Đặt trình xử lý sự kiện - Các lớp con cần được thực hiện
     */
    protected abstract void setupEventHandlers();

    /**
     * Xác thực đầu vào - Các lớp con cần được thực hiện
     */
    protected abstract void validateInput() throws IllegalArgumentException;

    /**
     * Nhận nhiệm vụ vận hành - Các lớp con cần được thực hiện
     *
     * @return Trả lại một cáiSwingWorkerđể thực hiện các tác vụ nền
     */
    protected abstract SwingWorker<Void, String> createWorkerTask();

    /**
     * Xử lý sau khi vận hành thành công - Các lớp con có thể ghi đè
     */
    protected void onOperationSuccess() {
        confirmed = true;
        progressPanel.setProgressText(I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_OPERATION_SUCCESS));
        progressPanel.getStatusLabel().setText(I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_OPERATION_COMPLETED_CLOSING));

        // Trì hoãn việc đóng hộp thoại
        Timer timer = new Timer(1000, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Phải làm gì sau khi một hoạt động thất bại - Các lớp con có thể ghi đè
     */
    protected void onOperationFailure(Exception e) {
        operationInProgress = false;
        confirmButton.setEnabled(true);
        setInputComponentsEnabled(true);
        progressPanel.setVisible(false);
        pack();

        progressPanel.reset();
        progressPanel.setProgressText(I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_OPERATION_FAILED));
        progressPanel.getStatusLabel().setText(I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_CHECK_INPUT_RETRY));

        showError(I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_OPERATION_FAILED_WITH_MESSAGE, e.getMessage()));
    }

    /**
     * Đặt trạng thái kích hoạt thành phần đầu vào - Các lớp con cần được thực hiện
     */
    protected abstract void setInputComponentsEnabled(boolean enabled);

    /**
     * Tạo một bảng nút tiêu chuẩn
     */
    protected JPanel createStandardButtonPanel(String confirmText) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        cancelButton.addActionListener(this::handleCancel);

        confirmButton = ModernButtonFactory.createButton(confirmText, true);
        confirmButton.addActionListener(this::handleConfirm);

        panel.add(cancelButton);
        panel.add(confirmButton);

        // Đặt nút mặc định
        getRootPane().setDefaultButton(confirmButton);

        return panel;
    }

    /**
     * Xử lý các hoạt động hủy bỏ
     */
    private void handleCancel(ActionEvent e) {
        if (operationInProgress) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_CONFIRM_CANCEL_OPERATION),
                    I18nUtil.getMessage(MessageKeys.PROGRESS_DIALOG_CONFIRM_CANCEL_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                cancelCurrentOperation();
                dispose();
            }
        } else {
            dispose();
        }
    }

    /**
     * Hủy thao tác hiện tại
     */
    private void cancelCurrentOperation() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    }

    /**
     * Xử lý các hoạt động xác nhận
     */
    private void handleConfirm(ActionEvent e) {
        try {
            validateInput();
            showProgressAndStartOperation();
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
                showError(ex.getMessage());
            }
        }
    }

    /**
     * Hiển thị tiến trình và bắt đầu hoạt động
     */
    private void showProgressAndStartOperation() {
        // Chuyển trạng thái giao diện
        operationInProgress = true;
        progressPanel.setVisible(true);
        confirmButton.setEnabled(false);

        // Vô hiệu hóa tất cả các thành phần đầu vào
        setInputComponentsEnabled(false);

        // Thay đổi kích thước cửa sổ
        pack();

        // Tạo và thực hiện các tác vụ nền
        currentWorker = createWorkerTask();

        // Theo dõi những thay đổi tiến độ
        currentWorker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressPanel.getProgressBar().setValue((Integer) evt.getNewValue());
            }
        });

        // Đặt trình xử lý cập nhật tiến trình
        currentWorker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) &&
                    SwingWorker.StateValue.DONE == evt.getNewValue()) {
                handleWorkerCompletion();
            }
        });

        currentWorker.execute();
    }

    /**
     * xử lýWorkerSự kiện hoàn chỉnh
     */
    private void handleWorkerCompletion() {
        try {
            currentWorker.get(); // Kiểm tra xem có bất thường gì không
            onOperationSuccess();
        } catch (ExecutionException ex) {
            // ExecutionExceptionBao bọc ngoại lệ thực tế
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                onOperationFailure((RuntimeException) cause);
            } else {
                onOperationFailure(new RuntimeException(cause));
            }
        } catch (InterruptedException ex) {
            // Xử lý các ngoại lệ ngắt
            Thread.currentThread().interrupt(); // Đặt lại trạng thái ngắt
            onOperationFailure(new RuntimeException(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_USER_CANCELLED), ex));
        }
    }

    /**
     * Hiển thị hộp thoại báo lỗi
     */
    protected void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void dispose() {
        // Đảm bảo dọn sạch tài nguyên
        cancelCurrentOperation();
        super.dispose();
    }
}
