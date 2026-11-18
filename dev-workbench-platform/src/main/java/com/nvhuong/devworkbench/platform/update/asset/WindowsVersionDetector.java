package com.nvhuong.devworkbench.platform.update.asset;

import com.nvhuong.devworkbench.util.AppRuntimeLayout;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Windows Trình kiểm tra phiên bản - Xác định xem đó là phiên bản di động hay phiên bản cài đặt
 */
@Slf4j
@UtilityClass
public class WindowsVersionDetector {

    /**
     * Kiểm tra xem nó có phải là phiên bản di động không
     *
     * <p>Căn cứ xét xử:</p>
     * <ol>
     *   <li>Kiểm tra xem thư mục gốc của ứng dụng có tồn tại không .portable Tệp nhận dạng (ưu tiên cao nhất)</li>
     *   <li>Kiểm tra xem có từ không Program Files hoặc AppData Chạy thư mục (tính năng phiên bản cài đặt)</li>
     *   <li>Mặc định được coi là phiên bản cài đặt</li>
     * </ol>
     *
     * @return true Cho biết phiên bản di động.false Cho biết phiên bản cài đặt
     */
    public static boolean isPortableVersion() {
        try {
            boolean portable = AppRuntimeLayout.isPortableMode(WindowsVersionDetector.class);
            log.info("Detected Windows package mode: {}", portable ? "portable" : "installed");
            return portable;
        } catch (Exception e) {
            log.warn("Failed to detect Windows version type: {}", e.getMessage());
            return false;
        }
    }
}
