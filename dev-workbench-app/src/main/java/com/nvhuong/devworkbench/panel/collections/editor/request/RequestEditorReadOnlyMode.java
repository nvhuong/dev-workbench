package com.nvhuong.devworkbench.panel.collections.editor.request;

import lombok.experimental.UtilityClass;

/**
 * Yêu cầu kiểm soát chỉ đọc của trình chỉnh sửa.
 * <p>
 * Ảnh chụp nhanh hiệu suất và phản hồi đã lưu không thể ghi lại dữ liệu thu thập. Tất cả các thành phần đầu vào được chuyển thống nhất sang chỉ đọc ở đây để tránh sự phân tán của các nhánh khởi tạo bảng điều khiển.
 */
@UtilityClass
class RequestEditorReadOnlyMode {

    static void apply(RequestViewComponents components) {
        components.requestLinePanel.setReadOnlySnapshotMode();
        components.descriptionEditor.setEditable(false);
        components.pathVariablesPanel.setEditable(false);
        components.paramsPanel.setEditable(false);
        components.authTabPanel.setEditable(false);
        components.headersPanel.setEditable(false);
        components.requestBodyPanel.setEditable(false);
        components.requestSettingsPanel.setEditable(false);
        components.scriptPanel.setEditable(false);
    }
}
