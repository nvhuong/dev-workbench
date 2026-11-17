package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * Đánh dấu các trường, hàm tạo hoặc phương thức yêu cầu chèn tự động
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * Có cần thiết không, nếu vậytrueVà cái tương ứng không thể được tìm thấybeanMột ngoại lệ được ném ra
     */
    boolean required() default true;
}
