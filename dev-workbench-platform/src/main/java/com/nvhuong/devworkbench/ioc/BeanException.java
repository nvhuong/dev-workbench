package com.nvhuong.devworkbench.ioc;

/**
 * IOCLớp cơ sở cho các ngoại lệ liên quan đến vùng chứa
 */
public class BeanException extends RuntimeException {

    public BeanException(String message) {
        super(message);
    }

    public BeanException(String message, Throwable cause) {
        super(message, cause);
    }
}

