package com.laker.postman.common.constants;

import com.laker.postman.model.Workspace;
import com.laker.postman.util.AppRuntimeLayout;
import com.laker.postman.util.SystemUtil;
import lombok.experimental.UtilityClass;

import java.io.File;

/**
 * Các hằng số đường dẫn tệp cấu hình được quản lý thống nhất
 * Tất cả các đường dẫn tệp cấu hình được xác định ở đây để dễ dàng bảo trì
 */
@UtilityClass
public class ConfigPathConstants {

    /**
     * Lấy thư mục gốc dữ liệu
     * Portable Mode: Thư mục chứa chương trình/data/
     * Chế độ bình thường: thư mục chính của người dùng/EasyPostman/
     */
    private static String getDataRootPath() {
        return SystemUtil.getEasyPostmanPath();
    }

    // ==================== Đường dẫn tệp cấu hình không đổi ====================

    /**
     * Tệp cấu hình chứng chỉ ứng dụng khách
     */
    public static final String CLIENT_CERTIFICATES = getDataRootPath() + "client_certificates.json";

    /**
     * Thư mục không gian làm việc mặc định (ngang hàng với các không gian làm việc khác, thư mục gốc không tham gia git）
     */
    public static final String DEFAULT_WORKSPACE_DIR = getDataRootPath() + "workspaces" + File.separator + "default" + File.separator;

    /**
     * Tệp cấu hình bộ sưu tập (không gian làm việc mặc định)
     */
    public static final String COLLECTIONS = DEFAULT_WORKSPACE_DIR + "collections.json";

    /**
     * Tệp cài đặt ứng dụng
     */
    public static final String EASY_POSTMAN_SETTINGS = getDataRootPath() + "easy_postman_settings.properties";

    /**
     * Tệp cấu hình biến môi trường (không gian làm việc mặc định)
     */
    public static final String ENVIRONMENTS = DEFAULT_WORKSPACE_DIR + "environments.json";

    /**
     * Tệp cấu hình biến toàn cục (cấp ứng dụng, không tuân theo chuyển đổi không gian làm việc)
     */
    public static final String GLOBAL_VARIABLES = getDataRootPath() + "global_variables.json";

    /**
     * Tệp cấu hình kiểm tra chức năng
     */
    public static final String FUNCTIONAL_CONFIG = getDataRootPath() + "functional_config.json";

    /**
     * Tệp cấu hình kiểm tra hiệu suất
     */
    public static final String PERFORMANCE_CONFIG = getDataRootPath() + "performance_config.json";

    /**
     * Mock Server Tệp cấu hình (không gian làm việc mặc định).
     */
    public static final String MOCK_SERVERS = DEFAULT_WORKSPACE_DIR + "mock_servers.json";

    /**
     * Elasticsearch Hồ sơ kết nối
     */
    public static final String ELASTICSEARCH_CONNECTION_PROFILES = getDataRootPath() + "elasticsearch_connection_profiles.json";

    /**
     * InfluxDB Hồ sơ kết nối
     */
    public static final String INFLUXDB_CONNECTION_PROFILES = getDataRootPath() + "influxdb_connection_profiles.json";

    /**
     * Yêu cầu tập tin lịch sử
     */
    public static final String REQUEST_HISTORY = getDataRootPath() + "request_history.json";

    /**
     * Tệp cấu hình phím tắt
     */
    public static final String SHORTCUTS = getDataRootPath() + "shortcuts.properties";

    /**
     * Tệp cài đặt người dùng
     */
    public static final String USER_SETTINGS = getDataRootPath() + "user_settings.json";

    /**
     * Tệp cấu hình không gian làm việc
     */
    public static final String WORKSPACES = getDataRootPath() + "workspaces.json";

    /**
     * Tệp cài đặt không gian làm việc
     */
    public static final String WORKSPACE_SETTINGS = getDataRootPath() + "workspace_settings.json";

    /**
     * Thư mục không gian làm việc
     */
    public static final String WORKSPACES_DIR = getDataRootPath() + "workspaces" + File.separator;

    /**
     * Mở tập tin yêu cầu
     */
    public static final String OPENED_REQUESTS = getDataRootPath() + "opened_requests.json";

    /**
     * Thư mục nhật ký
     * Portable Mode: Thư mục chứa chương trình/logs/
     * Chế độ bình thường: thư mục chính của người dùng/EasyPostman/logs/
     */
    public static final String LOG_DIR = AppRuntimeLayout.logRootDirectory(ConfigPathConstants.class).toString()
            + File.separator;

    // ==================== Phương pháp đường dẫn liên quan đến không gian làm việc ====================

    /**
     * Lấy đường dẫn tệp bộ sưu tập của không gian làm việc được chỉ định.
     * {@link com.laker.postman.model.Workspace#getPath()} Hãy đảm bảo luôn mang nó đến cuối cùng {@link java.io.File#separator}，
     * Chỉ cần nối trực tiếp tên tệp.
     */
    public static String getCollectionsPath(Workspace workspace) {
        if (workspace == null) {
            return COLLECTIONS;
        }
        return workspace.getPath() + "collections.json";
    }

    /**
     * Lấy đường dẫn tệp biến môi trường của không gian làm việc được chỉ định.
     * {@link com.laker.postman.model.Workspace#getPath()} Hãy đảm bảo luôn mang nó đến cuối cùng {@link java.io.File#separator}，
     * Chỉ cần nối trực tiếp tên tệp.
     */
    public static String getEnvironmentsPath(Workspace workspace) {
        if (workspace == null) {
            return ENVIRONMENTS;
        }
        return workspace.getPath() + "environments.json";
    }

    /**
     * Lấy đường dẫn tệp cấu hình kiểm tra chức năng cho không gian làm việc được chỉ định.
     */
    public static String getFunctionalConfigPath(Workspace workspace) {
        if (workspace == null || workspace.getPath() == null || workspace.getPath().isBlank()) {
            return DEFAULT_WORKSPACE_DIR + "functional_config.json";
        }
        return workspace.getPath() + "functional_config.json";
    }

    /**
     * Nhận đường dẫn tệp cấu hình kiểm tra hiệu suất của không gian làm việc được chỉ định.
     */
    public static String getPerformanceConfigPath(Workspace workspace) {
        if (workspace == null || workspace.getPath() == null || workspace.getPath().isBlank()) {
            return DEFAULT_WORKSPACE_DIR + "performance_config.json";
        }
        return workspace.getPath() + "performance_config.json";
    }

    /**
     * Nhận không gian làm việc được chỉ định Mock Server Đường dẫn tập tin cấu hình.
     */
    public static String getMockServersPath(Workspace workspace) {
        if (workspace == null || workspace.getPath() == null || workspace.getPath().isBlank()) {
            return MOCK_SERVERS;
        }
        return workspace.getPath() + "mock_servers.json";
    }
}
