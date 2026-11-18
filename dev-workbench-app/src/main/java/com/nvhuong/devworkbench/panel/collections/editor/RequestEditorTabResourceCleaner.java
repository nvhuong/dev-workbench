package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * Request an editor Tab Công cụ dọn dẹp tài nguyên.
 * <p>
 * RequestEditSubPanel Giữ các kết nối mạng, tác vụ nền và các tài nguyên đang chạy khác.Tab Chúng phải được giải phóng đồng đều khi thay thế hoặc đóng cửa.
 */
@UtilityClass
class RequestEditorTabResourceCleaner {
    void cleanup(Component component) {
        if (component instanceof RequestEditSubPanel requestEditSubPanel) {
            requestEditSubPanel.disposeResources();
        }
    }
}
