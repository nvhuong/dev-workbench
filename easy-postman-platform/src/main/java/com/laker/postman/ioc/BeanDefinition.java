package com.laker.postman.ioc;

import lombok.Data;

/**
 * BeanĐịnh nghĩa, mô tả aBeansiêu dữ liệu
 */
@Data
public class BeanDefinition {
    /**
     * Beantên
     */
    private String name;

    /**
     * BeancủaClassloại
     */
    private Class<?> beanClass;

    /**
     * Có phải là singleton hay không (mặc định là singleton)
     */
    private boolean singleton = true;


    public BeanDefinition(String name, Class<?> beanClass) {
        this.name = name;
        this.beanClass = beanClass;
    }

    public BeanDefinition(String name, Class<?> beanClass, boolean singleton) {
        this.name = name;
        this.beanClass = beanClass;
        this.singleton = singleton;
    }
}

