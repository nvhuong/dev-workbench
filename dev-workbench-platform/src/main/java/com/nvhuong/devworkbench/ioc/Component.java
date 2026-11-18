package com.nvhuong.devworkbench.ioc;

import java.lang.annotation.*;

/**
 * Đánh dấu một lớp là một thành phần sẽ đượcIOCQuản lý thùng chứa
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    /**
     * BeanTên, chữ cái đầu tiên của tên lớp mặc định là chữ thường
     */
    String value() default "";
}
