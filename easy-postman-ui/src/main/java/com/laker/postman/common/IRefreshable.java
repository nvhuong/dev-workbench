package com.laker.postman.common;

/**
 * Giao diện thành phần có thể làm mới
 * <p>
 * Các thành phần triển khai giao diện này sẽ tự động làm mới trong các trường hợp sau:
 * <ul>
 *   <li>When switching languages - Cập nhật tất cả nội dung văn bản</li>
 *   <li>Khi chuyển đổi chủ đề - Cập nhật giao diện thành phần</li>
 *   <li>Khi chuyển đổi phông chữ - Cập nhật kiểu phông chữ</li>
 * </ul>
 *
 * @author laker
 */
public interface IRefreshable {

    /**
     * Làm mới thành phần
     * <p>
     * Phương thức này sẽ được gọi khi ngôn ngữ, chủ đề hoặc phông chữ thay đổi.
     * Lớp thực hiện nên:
     * <ul>
     *   <li>Cập nhật tất cả văn bản được quốc tế hóa (sử dụng  I18nUtil.getMessage）</li>
     *   <li>Thành phần chuyển tiếp (call revalidate）</li>
     *   <li>RedrawComponent(call repaint）</li>
     * </ul>
     */
    void refresh();
}

