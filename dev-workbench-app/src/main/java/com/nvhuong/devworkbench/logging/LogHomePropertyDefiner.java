package com.nvhuong.devworkbench.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.nvhuong.devworkbench.util.AppRuntimeLayout;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Phân tích thư mục nhật ký theo trạng thái đang chạy và đảm bảo rằng nó nằm trong thư mục chứa trình ghi nhật ký phiên bản di động logs。
 */
public class LogHomePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        Path logDir = AppRuntimeLayout.logRootDirectory(LogHomePropertyDefiner.class);
        try {
            Files.createDirectories(logDir);
        } catch (Exception ignored) {
            // Xử lý khi tạo thư mục không thành công logback Báo cáo lỗi cụ thể trong các giai đoạn khởi tạo tiếp theo.
        }
        return logDir.toString();
    }
}
