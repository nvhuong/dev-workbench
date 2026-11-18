package com.nvhuong.devworkbench.service.variable;

import com.nvhuong.devworkbench.variable.VariableType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhà cung cấp bối cảnh biến đổi
 * <p>
 * Chịu trách nhiệm quản lý bối cảnh thực thi hiện tại pm.variables。
 * <ul>
 *   <li>Mức độ ưu tiên cao nhất (ưu tiên = 1）</li>
 *   <li>Phạm vi được xác định bởi người gọi và có thể bao gồm một lần chạy hoặc một chuỗi người dùng ảo</li>
 *   <li>sử dụng ThreadLocal Đảm bảo an toàn cho sợi</li>
 * </ul>
 */
@Slf4j
public class VariablesService implements VariableProvider {

    /**
     * Ví dụ Singleton
     */
    private static final VariablesService INSTANCE = new VariablesService();

    /**
     * Lưu trữ biến bối cảnh thực thi, sử dụng ThreadLocal Đảm bảo an toàn cho sợi
     */
    private static final ThreadLocal<Map<String, String>> VARIABLES = new ThreadLocal<>();

    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo bên ngoài
     */
    private VariablesService() {
    }

    /**
     * Nhận một ví dụ đơn lẻ
     */
    public static VariablesService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Map<String, String> variables = VARIABLES.get();
        if (variables == null) {
            return null;
        }
        return variables.get(key);
    }

    @Override
    public boolean has(String key) {
        Map<String, String> variables = VARIABLES.get();
        return key != null && variables != null && variables.containsKey(key);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> variables = VARIABLES.get();
        if (variables == null || variables.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(variables));
    }

    @Override
    public int getPriority() {
        return VariableType.VARIABLE.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.VARIABLE;
    }

    // ==================== Thực hiện các phương pháp cụ thể cho biến ngữ cảnh ====================

    /**
     * Đặt các biến bối cảnh thực thi
     *
     * @param key   tên biến
     * @param value giá trị thay đổi
     */
    public void set(String key, String value) {
        if (key != null && value != null) {
            getOrCreateContextMap().put(key, value);
            log.debug("Set variable: {} = {}", key, value);
        }
    }

    /**
     * Đặt các biến bối cảnh thực thi theo đợt
     *
     * @param variables các biến Map
     */
    public void setAll(Map<String, String> variables) {
        if (variables != null && !variables.isEmpty()) {
            getOrCreateContextMap().putAll(variables);
            log.debug("Set {} variables", variables.size());
        }
    }

    /**
     * Xóa các biến bối cảnh thực thi
     *
     * @param key tên biến
     */
    public void remove(String key) {
        Map<String, String> variables = VARIABLES.get();
        if (key != null && variables != null) {
            variables.remove(key);
            log.debug("Removed variable: {}", key);
        }
    }

    /**
     * Xóa các giá trị biến trong ngữ cảnh thực thi hiện tại, nhưng vẫn giữ lại chính ngữ cảnh đó.
     * <p>
     * This method corresponds to the one in the script pm.variables.clear() Semantics:
     * Các tập lệnh tiếp theo vẫn có thể tiếp tục trong cùng bối cảnh thực thi set giá trị mới.
     */
    public void clearValues() {
        Map<String, String> variables = VARIABLES.get();
        int size = variables != null ? variables.size() : 0;
        if (variables != null) {
            variables.clear();
        }
        log.debug("Cleared {} execution-scoped variables", size);
    }

    /**
     * Hoàn toàn hủy liên kết bối cảnh thực thi hiện tại.
     * <p>
     * This method should only be requested once/Được gọi bởi lớp khung khi kết thúc quá trình chạy.
     * Không nên sử dụng để dọn dẹp biến trong tập lệnh.
     */
    public void detachContext() {
        Map<String, String> variables = VARIABLES.get();
        int size = variables != null ? variables.size() : 0;
        if (variables != null) {
            variables.clear();
        }
        VARIABLES.remove();
        log.debug("Detached execution-scoped variable context with {} entries", size);
    }

    /**
     * Lấy số lượng biến bối cảnh thực thi hiện tại
     *
     * @return Số lượng biến bối cảnh thực thi hiện tại
     */
    public int size() {
        Map<String, String> variables = VARIABLES.get();
        return variables != null ? variables.size() : 0;
    }

    public Map<String, String> getCurrentContextMap() {
        return VARIABLES.get();
    }

    public void attachContextMap(Map<String, String> variables) {
        if (variables == null) {
            VARIABLES.remove();
            return;
        }
        VARIABLES.set(variables);
    }

    private Map<String, String> getOrCreateContextMap() {
        Map<String, String> variables = VARIABLES.get();
        if (variables == null) {
            variables = new ConcurrentHashMap<>();
            VARIABLES.set(variables);
        }
        return variables;
    }
}
