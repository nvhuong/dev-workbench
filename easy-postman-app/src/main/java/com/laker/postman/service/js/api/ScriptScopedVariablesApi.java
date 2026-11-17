package com.laker.postman.service.js.api;

import com.laker.postman.model.Environment;
import com.laker.postman.service.variable.BuiltInFunctionService;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Biến phạm vi trong tập lệnh API。
 * <p>
 * Chỉ tiếp xúc Postman Tạo kiểu cho các phương thức biến để tránh đặt chúng trực tiếp Environment Các phương thức nội bộ của mô hình được hiển thị dưới dạng tập lệnh.
 */
public class ScriptScopedVariablesApi {
    private static final Pattern VARIABLE_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    private final Environment scope;
    private final Runnable persistAction;

    public ScriptScopedVariablesApi(Environment scope, Runnable persistAction) {
        this.scope = scope;
        this.persistAction = persistAction;
    }

    public boolean has(String key) {
        return scope != null && scope.hasVariable(key);
    }

    public String get(String key) {
        return scope == null ? null : scope.getVariable(key);
    }

    public void set(String key, String value) {
        mutate(() -> scope.set(key, value));
    }

    public void set(String key, Object value) {
        if (value != null) {
            mutate(() -> scope.set(key, String.valueOf(value)));
        }
    }

    public void unset(String key) {
        mutate(() -> scope.removeVariable(key));
    }

    public void clear() {
        mutate(scope::clearVariables);
    }

    public Object toObject() {
        Map<String, String> values = scope == null ? Collections.emptyMap() : scope.getVariables();
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(values);
        return ProxyObject.fromMap(jsObject);
    }

    public String replaceIn(String template) {
        if (template == null || scope == null) {
            return template;
        }

        Matcher matcher = VARIABLE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            BuiltInFunctionService builtIns = BuiltInFunctionService.getInstance();
            String value = builtIns.has(key) ? builtIns.get(key) : null;
            if (value == null) {
                value = scope.getVariable(key);
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(value == null ? matcher.group(0) : value));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private void mutate(Runnable mutation) {
        if (scope == null) {
            return;
        }
        mutation.run();
        if (persistAction != null) {
            persistAction.run();
        }
    }
}
