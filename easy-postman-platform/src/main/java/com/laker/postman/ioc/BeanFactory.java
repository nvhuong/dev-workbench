package com.laker.postman.ioc;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * IOCLớp công cụ vùng chứa cung cấp các phương thức truy cập thuận tiện
 */
@Slf4j
@UtilityClass
public class BeanFactory {

    private final ApplicationContext context = ApplicationContext.getInstance();

    /**
     * khởi tạoIOCContainer, quét gói hàng được chỉ định
     */
    public void init(String... basePackages) {
        context.scan(basePackages);
    }

    /**
     * Nhận nó theo tênBean
     */
    public <T> T getBean(String beanName) {
        return context.getBean(beanName);
    }

    /**
     * Nhận dựa trên loạiBean
     */
    public <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    /**
     * Tiêu diệt tất cảBean，gọi@PreDestroyphương pháp
     * Nên gọi phương thức này khi đóng ứng dụng
     */
    public void destroy() {
        log.info("Destroying IOC container...");
        context.destroy();
    }
}
