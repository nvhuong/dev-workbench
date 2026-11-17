package com.laker.postman.request.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * HTTP Form Urlencoded model with enabled state
 * Được sử dụng cho application/x-www-form-urlencoded loại dữ liệu biểu mẫu
 */
@Data
@NoArgsConstructor
public class HttpFormUrlencoded implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
    private String description = "";

    public HttpFormUrlencoded(boolean enabled, String key, String value) {
        this(enabled, key, value, "");
    }

    public HttpFormUrlencoded(boolean enabled, String key, String value, String description) {
        this.enabled = enabled;
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
