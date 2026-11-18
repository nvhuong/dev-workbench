package com.nvhuong.devworkbench.service.variable;

import com.nvhuong.devworkbench.variable.VariableType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhà cung cấp biến dữ liệu lặp.
 * <p>
 * Lưu bối cảnh thực hiện hiện tại CSV/JSON dữ liệu hàng và được cung cấp trong cùng một luồng thực thi
 * pm.iterationData và {{variable}} Phân tích tái sử dụng.
 */
public class IterationDataVariableService implements VariableProvider {

    private static final IterationDataVariableService INSTANCE = new IterationDataVariableService();

    private static final ThreadLocal<Map<String, String>> ITERATION_DATA = new ThreadLocal<>();

    private IterationDataVariableService() {
    }

    public static IterationDataVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null) {
            return null;
        }
        return iterationData.get(key);
    }

    @Override
    public boolean has(String key) {
        Map<String, String> iterationData = ITERATION_DATA.get();
        return key != null && iterationData != null && iterationData.containsKey(key);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null || iterationData.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(iterationData));
    }

    @Override
    public int getPriority() {
        return VariableType.ITERATION_DATA.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.ITERATION_DATA;
    }

    public void replaceAll(Map<String, String> variables) {
        Map<String, String> current = getOrCreateContextMap();
        current.clear();
        if (variables != null && !variables.isEmpty()) {
            current.putAll(variables);
        }
    }

    public void remove(String key) {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (key != null && iterationData != null) {
            iterationData.remove(key);
        }
    }

    public void clearValues() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData != null) {
            iterationData.clear();
        }
    }

    /**
     * Hủy liên kết hoàn toàn bối cảnh dữ liệu lặp lại hiện tại.
     * <p>
     * Được lớp khung gọi khi kết thúc quá trình chạy để tránh sử dụng lại lớp khung cũ cho các yêu cầu tiếp theo CSV/JSON dữ liệu hàng.
     */
    public void detachContext() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData != null) {
            iterationData.clear();
        }
        ITERATION_DATA.remove();
    }

    public Map<String, String> getCurrentContextMap() {
        return ITERATION_DATA.get();
    }

    public void attachContextMap(Map<String, String> variables) {
        if (variables == null) {
            ITERATION_DATA.remove();
            return;
        }
        ITERATION_DATA.set(variables);
    }

    private Map<String, String> getOrCreateContextMap() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null) {
            iterationData = new ConcurrentHashMap<>();
            ITERATION_DATA.set(iterationData);
        }
        return iterationData;
    }
}
