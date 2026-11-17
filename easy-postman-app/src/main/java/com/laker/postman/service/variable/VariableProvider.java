package com.laker.postman.service.variable;

import com.laker.postman.variable.VariableType;

import java.util.Map;

/**
 * Giao diện nhà cung cấp biến
 * <p>
 * Xác định hành vi chung của các nhà cung cấp biến. Tất cả các nguồn biến (biến ngữ cảnh thực thi, biến môi trường, hàm dựng sẵn, v.v.) cần triển khai giao diện này
 * <p>
 * Nguyên tắc thiết kế:
 * <ul>
 *   <li>Tính gắn kết cao: Mỗi lớp triển khai chỉ chịu trách nhiệm cho một nguồn biến</li>
 *   <li>Khớp nối thấp: Tách biệt các triển khai cụ thể thông qua các giao diện</li>
 *   <li>Mẫu chiến lược: Các nguồn biến đổi có thể dễ dàng thay thế hoặc mở rộng</li>
 * </ul>
 */
public interface VariableProvider {

    /**
     * Lấy giá trị của một biến
     *
     * @param key tên biến
     * @return Giá trị biến, trả về nếu không tồn tại null
     */
    String get(String key);

    /**
     * Kiểm tra xem biến có tồn tại không
     *
     * @param key tên biến
     * @return tồn tại
     */
    boolean has(String key);

    /**
     * Nhận tất cả các biến
     *
     * @return của tất cả các biến Map，Có thể trống nhưng không null
     */
    Map<String, String> getAll();

    /**
     * Nhận được mức độ ưu tiên của nhà cung cấp biến
     * Giá trị càng nhỏ thì mức độ ưu tiên càng cao
     *
     * @return giá trị ưu tiên
     */
    int getPriority();

    /**
     * Lấy loại nhà cung cấp biến
     *
     * @return Kiểu liệt kê biến
     */
    VariableType getType();
}
