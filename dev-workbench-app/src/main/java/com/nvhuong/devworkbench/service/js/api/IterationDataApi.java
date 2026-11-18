package com.nvhuong.devworkbench.service.js.api;

import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.service.variable.IterationDataVariableService;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lặp lại dữ liệu API (pm.iterationData)。
 * <p>
 * Được sử dụng để truy cập chỉ đọc vào các hàng dựa trên dữ liệu được đưa vào ngữ cảnh thực thi hiện tại và không tham gia vào quá trình lưu giữ.
 */
public class IterationDataApi {

    public boolean has(String key) {
        return IterationDataVariableService.getInstance().has(key);
    }

    public String get(String key) {
        return IterationDataVariableService.getInstance().get(key);
    }

    public Object toObject() {
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(IterationDataVariableService.getInstance().getAll());
        return ProxyObject.fromMap(jsObject);
    }

    public String toJSON() {
        return JSONUtil.toJsonStr(new LinkedHashMap<>(IterationDataVariableService.getInstance().getAll()));
    }
}
