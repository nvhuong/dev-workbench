package com.laker.postman.platform.update;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Windows Lớp công cụ đăng ký
 * Được sử dụng để kiểm tra xem ứng dụng đã được cài đặt chưa (thông qua sổ đăng ký)
 */
@Slf4j
public class WindowsRegistryChecker {

    // 64Đường dẫn đăng ký bit (mặc định)
    // Lưu ý:Inno Setup của AppId=GUID Một khóa đăng ký sẽ được tạo ra GUID_is1（không có dấu ngoặc nhọn)
    private static final String UNINSTALL_KEY_PATH_64 =
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E_is1";

    // 32Đường dẫn đăng ký bit (trong 64hệ thống bitWOW6432Node）
    private static final String UNINSTALL_KEY_PATH_32 =
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E_is1";

    // Đường dẫn mặc định được sử dụng cho các phương pháp khác
    private static final String UNINSTALL_KEY_PATH = UNINSTALL_KEY_PATH_64;

    // Đã tìm thấy đường dẫn đăng ký thực tế (bộ đệm)
    private static String actualKeyPath = null;

    /**
     * Kiểm tra xem ứng dụng đã được cài đặt chưa (thông qua sổ đăng ký)
     * Sẽ kiểm tra cùng một lúc64tổng bit32Đường dẫn đăng ký bit
     *
     * @return true Nếu ứng dụng được cài đặt, .false Ngược lại
     */
    public static boolean isAppInstalled() {
        // Kiểm tra đầu tiên64đường dẫn bit
        log.info("Checking 64-bit registry path...");
        if (checkRegistryPath(UNINSTALL_KEY_PATH_64)) {
            actualKeyPath = UNINSTALL_KEY_PATH_64;
            log.info("✓ App is INSTALLED (found in 64-bit registry)");
            return true;
        }

        // Kiểm tra lại32đường dẫn bit
        log.info("Checking 32-bit registry path (WOW6432Node)...");
        if (checkRegistryPath(UNINSTALL_KEY_PATH_32)) {
            actualKeyPath = UNINSTALL_KEY_PATH_32;
            log.info("✓ App is INSTALLED (found in 32-bit registry)");
            return true;
        }

        // Hãy thử tìm kiếm DisplayName Tìm ứng dụng
        log.info("Registry key not found, trying to search by DisplayName...");
        if (findAppByDisplayName()) {
            log.info("✓ App is INSTALLED (found by DisplayName search)");
            return true;
        }

        log.info("✗ App is NOT INSTALLED (not found in registry)");
        return false;
    }

    /**
     * Kiểm tra xem đường dẫn đăng ký đã chỉ định có tồn tại hay không
     */
    private static boolean checkRegistryPath(String keyPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", keyPath);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc đầu ra (để gỡ lỗi ghi nhật ký)- sử dụngGBKMã hóa để tránh các ký tự bị cắt xén của Trung Quốc
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("  Registry output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("  Exit code: {}", exitCode);
            return exitCode == 0;

        } catch (Exception e) {
            log.warn("Failed to check registry path {}: {}", keyPath, e.getMessage());
            return false;
        }
    }

    /**
     * Bằng cách tìm kiếm DisplayName Tìm ứng dụng
     * sử dụng reg query /s /f Lệnh để tìm kiếm sổ đăng ký một cách hiệu quả
     *
     * @return true Nếu ứng dụng được tìm thấy, .false Ngược lại
     */
    private static boolean findAppByDisplayName() {
        String[] searchPaths = {
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        };

        for (String basePath : searchPaths) {
            try {
                // sử dụng /s Tìm kiếm các khóa con, /f Tìm chuỗi được chỉ định
                ProcessBuilder pb = new ProcessBuilder("reg", "query", basePath, "/s", "/f", "EasyPostman");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String foundKeyPath = null;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        log.info("  Search output: {}", line);

                        // Tìm thấy có chứa HKEY dòng (đường dẫn khóa đăng ký)
                        if (line.startsWith("HKEY_") && line.contains("Uninstall")) {
                            foundKeyPath = line;
                        }

                        // Xác nhận rằng nó đã được tìm thấy DisplayName
                        if (foundKeyPath != null && line.contains("DisplayName") && line.contains("EasyPostman")) {
                            actualKeyPath = foundKeyPath;
                            log.info("✓ Found app at: {}", foundKeyPath);
                            return true;
                        }
                    }
                }

                process.waitFor();

            } catch (Exception e) {
                log.info("Failed to search registry path {}: {}", basePath, e.getMessage());
            }
        }

        return false;
    }


    /**
     * Tải phiên bản của ứng dụng đã cài đặt
     *
     * @return Số phiên bản đã cài đặt, được trả về nếu nó chưa được cài đặt hoặc truy vấn không thành công null
     */
    public static String getInstalledVersion() {
        // Sử dụng đường dẫn thực tế được tìm thấy hoặc thử đường dẫn mặc định nếu không tìm thấy đường dẫn nào
        String keyPath = actualKeyPath != null ? actualKeyPath : UNINSTALL_KEY_PATH;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query", keyPath, "/v", "DisplayVersion"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Phân tích số phiên bản: DisplayVersion    REG_SZ    4.0.9
                    if (line.contains("DisplayVersion") && line.contains("REG_SZ")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            String version = parts[parts.length - 1];
                            log.info("Installed version from registry: {}", version);
                            return version;
                        }
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            log.warn("Failed to get installed version: {}", e.getMessage());
        }

        return null;
    }
}