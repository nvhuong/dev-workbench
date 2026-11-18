package com.nvhuong.devworkbench.ioc;

import java.lang.annotation.*;

/**
 * được chỉ địnhBeanphạm vi
 * Mặc định là singleton (singleton）
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    /**
     * Beanphạm vi
     * - "singleton": Chế độ Singleton (mặc định)
     * - "prototype": Mẫu nguyên mẫu (tạo phiên bản mới trên mỗi lần tìm nạp)
     */
    String value() default "singleton";

    /**
     * Hằng số: Singleton
     */
    String SINGLETON = "singleton";

    /**
     * Hằng số: Nguyên mẫu
     */
    String PROTOTYPE = "prototype";
}

