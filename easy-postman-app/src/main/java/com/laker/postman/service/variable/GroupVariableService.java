package com.laker.postman.service.variable;

import com.laker.postman.variable.VariableType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Dịch vụ biến cấp nhóm (chế độ đơn)
 * <p>
 * Cung cấp các biến được kế thừa từ nhóm mà yêu cầu được thực hiện
 * Mức độ ưu tiên thay đổi: nhóm bên trong > Nhóm bên ngoài
 * <p>
 * LƯU Ý: Sử dụng toàn cầu RequestExecutionContext Nhận thực hiện yêu cầu hiện tại scope
 */
@Slf4j
public class GroupVariableService implements VariableProvider {

    /**
     * Ví dụ Singleton
     */
    private static final GroupVariableService INSTANCE = new GroupVariableService();

    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo bên ngoài
     */
    private GroupVariableService() {
    }

    /**
     * Nhận một ví dụ đơn lẻ
     *
     * @return GroupVariableService Singleton
     */
    public static GroupVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        RequestExecutionScope executionScope = RequestExecutionContext.getCurrentScope();
        if (executionScope != null) {
            return executionScope.getGroupVariable(key);
        }

        return null;
    }

    @Override
    public boolean has(String key) {
        return get(key) != null;
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();

        RequestExecutionScope executionScope = RequestExecutionContext.getCurrentScope();
        if (executionScope != null) {
            result.putAll(executionScope.getGroupVariables());
            return result;
        }

        return result;
    }

    @Override
    public int getPriority() {
        return VariableType.GROUP.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.GROUP;
    }
}
