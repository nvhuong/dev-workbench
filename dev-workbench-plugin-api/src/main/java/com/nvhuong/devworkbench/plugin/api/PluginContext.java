package com.nvhuong.devworkbench.plugin.api;

import java.util.function.Supplier;

/**
 * Bối cảnh đăng ký được cung cấp cho plugin.
 * <p>
 * plugin onLoad Các giai đoạn chỉ nên giao tiếp với máy chủ thông qua giao diện này“Khai báo khả năng”，
 * Chẳng hạn như kịch bản đăng ký API、Đối tượng phục vụ,Toolbox Bảng điều khiển, hoàn thiện và Snippet。
 * Điều này ngăn plug-in phụ thuộc trực tiếp vào việc triển khai nội bộ của máy chủ.
 * </p>
 */
public interface PluginContext {

    /**
        * Thông tin mô tả của chính plug-in hiện tại.
        */
    PluginDescriptor descriptor();

    /**
     * Không gian lưu giữ riêng tư của plug-in hiện tại.
     */
    default PluginStorage storage() {
        return PluginStorage.noop();
    }

    /**
     * Đăng ký kịch bản API。
     * Ví dụ: một plugin có thể tự hiển thị dưới dạng  pm.plugin("redis") / pm.plugin("kafka")。
     */
    void registerScriptApi(String alias, Supplier<Object> factory);

    /**
     * Đăng ký các đối tượng dịch vụ mà máy chủ và các lớp cầu nối khác có thể sử dụng.
     */
    <T> void registerService(Class<T> type, T service);

    /**
     * Nhận dịch vụ cầu nối được đăng ký bởi máy chủ hoặc trình cắm khác.
     */
    default <T> T getService(Class<T> type) {
        return null;
    }

    /**
     * Đăng ký Toolbox Phần mở rộng của bảng điều khiển.
     */
    void registerToolboxContribution(ToolboxContribution contribution);

    /**
     * Đăng ký tiện ích mở rộng trang cài đặt.
     */
    default void registerSettingsContribution(PluginSettingsContribution contribution) {
        // Runtime implementations can expose this through the host settings dialog.
    }

    /**
     * Đăng ký phần mở rộng hành động menu.
     */
    default void registerMenuContribution(PluginMenuContribution contribution) {
        // Runtime implementations can expose this through host menus.
    }

    /**
     * Đăng ký tiện ích mở rộng mục nhập phím tắt trên thanh trạng thái.
     */
    default void registerStatusBarActionContribution(StatusBarActionContribution contribution) {
        // Runtime implementations can expose this through host status bar.
    }

    /**
     * Đăng ký phần mở rộng siêu dữ liệu cập nhật plugin.
     */
    default void registerUpdateMetadataContribution(PluginUpdateMetadataContribution contribution) {
        // Runtime implementations can merge this into plugin update checks and the plugin market.
    }

    /**
     * Đăng ký tiện ích mở rộng tự động hoàn thành tập lệnh.
     */
    void registerScriptCompletionContributor(ScriptCompletionContributor contributor);

    /**
     * Đăng ký đoạn script.
     */
    void registerSnippet(SnippetDefinition definition);

    /**
     * Plug-in đã đăng ký có gói tài nguyên được quốc tế hóa riêng.
     * <p>
     * bundleName sử dụng {@link java.util.ResourceBundle#getBundle(String)} của base name，
     * Ví dụ {@code redis-messages}。
     * </p>
     */
    default void registerI18nBundle(String bundleName) {
        // Runtime implementations can hook this into the shared i18n registry.
    }
}
