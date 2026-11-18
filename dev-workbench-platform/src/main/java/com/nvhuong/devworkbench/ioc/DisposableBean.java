package com.nvhuong.devworkbench.ioc;

/**
 * BeanPhá hủy giao diện
 * Thực hiện giao diện nàyBeanSẽ được gọi tự động khi container được đóng lạidestroyphương pháp
 * Đây là@PreDestroyPhương thức giao diện thay thế cho chú thích
 */
public interface DisposableBean {

    /**
     * BeanĐược kêu gọi trước khi hủy diệt
     * Có thể được sử dụng để giải phóng tài nguyên, đóng kết nối và các tác vụ dọn dẹp khác
     *
     * @throws Exception Nếu xảy ra lỗi trong quá trình hủy
     */
    void destroy() throws Exception;
}

