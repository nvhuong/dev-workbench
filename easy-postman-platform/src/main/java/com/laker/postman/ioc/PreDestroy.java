package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * Đánh dấu một phương thức trong BeanThực hiện trước khi tiêu hủy
 * This method is automatically called when the container is closed
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreDestroy {
}
