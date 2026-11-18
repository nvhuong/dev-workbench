package com.nvhuong.devworkbench.ioc;

/**
 * BeanKhởi tạo giao diện
 * Thực hiện giao diện nàyBeanSẽ được gọi tự động sau khi việc tiêm thuộc tính hoàn tấtafterPropertiesSetphương pháp
 * Đây là@PostConstructPhương thức giao diện thay thế cho chú thích
 */
public interface InitializingBean {

    /**
     * BeanĐược gọi sau khi tất cả thuộc tính đã được thiết lập
     * Có thể được sử dụng để khởi tạo tài nguyên, xác minh cấu hình, v.v
     *
     * @throws Exception Nếu khởi tạo không thành công
     */
    void afterPropertiesSet() throws Exception;
}
