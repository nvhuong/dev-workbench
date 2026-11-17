package com.laker.postman.plugin.api;

/**
 * EasyPostman Mục nhập trình cắm.
 */
public interface EasyPostmanPlugin {

    /**
     * Được gọi khi plug-in được tải, dùng để đăng ký các điểm mở rộng.
     */
    void onLoad(PluginContext context);

    /**
     * Được gọi sau khi tất cả các đăng ký mở rộng đã được hoàn thành.
     */
    default void onStart() {
    }

    /**
     * Được gọi khi ứng dụng thoát.
     */
    default void onStop() {
    }
}
