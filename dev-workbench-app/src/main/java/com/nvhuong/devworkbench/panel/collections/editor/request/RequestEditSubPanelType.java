package com.nvhuong.devworkbench.panel.collections.editor.request;

/**
 * Yêu cầu loại bảng điều khiển con được chỉnh sửa
 */
public enum RequestEditSubPanelType {
    /**
     * Bảng chỉnh sửa yêu cầu chung
     */
    NORMAL,
    /**
     * Bảng xem trước phản hồi đã lưu
     */
    SAVED_RESPONSE,
    /**
     * Ảnh chụp nhanh yêu cầu trong kiểm tra hiệu suất chỉ để xem và không được phép chỉnh sửa hoặc gửi trực tiếp.
     */
    PERFORMANCE_SNAPSHOT
}
