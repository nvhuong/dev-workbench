package com.laker.postman.service.variable;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.variable.VariableType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * Nhà cung cấp biến môi trường
 * <p>
 * Chịu trách nhiệm lấy các biến môi trường từ môi trường hiện được kích hoạt
 * <ul>
 *   <li>Mức độ ưu tiên trung bình (sau các biến thực thi, dữ liệu lặp, các biến nhóm)</li>
 *   <li>Cao hơn các biến toàn cục và các hàm dựng sẵn</li>
 * </ul>
 */
@Slf4j
public class EnvironmentVariableService implements VariableProvider {

    /**
     * Ví dụ Singleton
     */
    private static final EnvironmentVariableService INSTANCE = new EnvironmentVariableService();

    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo bên ngoài
     */
    private EnvironmentVariableService() {
    }

    /**
     * Nhận một ví dụ đơn lẻ
     */
    public static EnvironmentVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        Environment activeEnv = activeEnvironment();
        if (activeEnv == null) {
            return null;
        }

        return activeEnv.getVariable(key);
    }

    @Override
    public boolean has(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        Environment activeEnv = activeEnvironment();
        if (activeEnv == null) {
            return false;
        }

        return activeEnv.getVariable(key) != null;
    }

    @Override
    public Map<String, String> getAll() {
        Environment activeEnv = activeEnvironment();
        if (activeEnv == null || activeEnv.getVariables() == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(activeEnv.getVariables());
    }

    @Override
    public int getPriority() {
        return VariableType.ENVIRONMENT.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.ENVIRONMENT;
    }

    private Environment activeEnvironment() {
        Environment scopedEnvironment = RunScopedVariableContext.currentEnvironment();
        return scopedEnvironment == null ? EnvironmentService.getActiveEnvironment() : scopedEnvironment;
    }
}
