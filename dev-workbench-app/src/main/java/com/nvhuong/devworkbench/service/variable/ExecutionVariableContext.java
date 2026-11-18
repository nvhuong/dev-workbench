package com.nvhuong.devworkbench.service.variable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bối cảnh thực thi tập lệnh rõ ràng.
 * <p>
 * Sẽ được yêu cầu một lần/Một lần chạy cần được chia sẻ execution variables Và iteration data
 * Việc tách khỏi chính luồng sẽ tạo điều kiện thuận lợi cho việc tái sử dụng rõ ràng trong các tình huống gọi lại đa luồng.
 */
public class ExecutionVariableContext {

    private final Map<String, String> variables;
    private final Map<String, String> iterationData;
    private Set<String> onceOnlyState = ConcurrentHashMap.newKeySet();
    private int iterationIndex;
    private int iterationCount = 1;

    public ExecutionVariableContext() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public ExecutionVariableContext(Map<String, String> variables, Map<String, String> iterationData) {
        this.variables = variables != null ? variables : new ConcurrentHashMap<>();
        this.iterationData = iterationData != null ? iterationData : new ConcurrentHashMap<>();
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, String> getIterationData() {
        return iterationData;
    }

    public int getIterationIndex() {
        return iterationIndex;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationInfo(int iterationIndex, int iterationCount) {
        this.iterationIndex = Math.max(0, iterationIndex);
        this.iterationCount = Math.max(0, iterationCount);
    }

    public void replaceIterationData(Map<String, String> values) {
        iterationData.clear();
        if (values != null && !values.isEmpty()) {
            iterationData.putAll(values);
        }
    }

    public void attachOnceOnlyState(Set<String> onceOnlyState) {
        this.onceOnlyState = onceOnlyState == null ? ConcurrentHashMap.newKeySet() : onceOnlyState;
    }

    public boolean markOnceOnlyExecuted(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return onceOnlyState.add(key);
    }
}
