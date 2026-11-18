package com.nvhuong.devworkbench.ioc;


import java.lang.annotation.*;

/* Đánh dấu một phương thức trong BeanThực hiện sau khi khởi tạo
 * This method will be called automatically after dependency injection is completed
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostConstruct {
}

