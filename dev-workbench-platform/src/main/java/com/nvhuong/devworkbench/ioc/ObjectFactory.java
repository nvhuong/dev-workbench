package com.nvhuong.devworkbench.ioc;

/**
 * Giao diện nhà máy đối tượng
 * Được sử dụng để tạo các đối tượng một cách lười biếng và hỗ trợ độ phân giải phụ thuộc vòng tròn
 */
@FunctionalInterface
public interface ObjectFactory<T> {

    /**
     * Lấy ví dụ đối tượng
     *
     * @return Ví dụ đối tượng
     * @throws Exception Nếu việc mua lại không thành công
     */
    T getObject() throws Exception;
}

