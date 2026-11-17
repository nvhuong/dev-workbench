package com.laker.postman.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mô hình biến môi trường, được sử dụng để quản lý một tập hợp các biến liên quan
 */
@Setter
@Getter
public class Environment {
    private String id;
    private String name;
    private List<Variable> variableList = new ArrayList<>();
    private boolean active = false;

    public Environment() {
    }

    public Environment(String name) {
        this.name = name;
    }

    public void addVariable(String key, String value) {
        if (key != null && !key.isEmpty()) {
            Variable variable = new Variable(true, key, value);
            variableList.add(variable);
        }
    }


    public void set(String key, String value) {
        if (key != null && !key.isEmpty()) {
            // Tìm nếu điều này tồn tạikeybiến
            boolean found = false;
            if (variableList != null) {
                for (Variable envVar : variableList) {
                    if (key.equals(envVar.getKey())) {
                        envVar.setValue(value);
                        envVar.setEnabled(true);
                        found = true;
                        break;
                    }
                }
            }
            // Nếu nó không tồn tại, biến mới sẽ được thêm vào
            if (!found) {
                if (variableList == null) {
                    variableList = new ArrayList<>();
                }
                variableList.add(new Variable(true, key, value));
            }
        }
    }

    public void removeVariable(String key) {
        if (key != null && variableList != null) {
            variableList.removeIf(envVar -> key.equals(envVar.getKey()));
        }
    }

    public void clearVariables() {
        if (variableList != null) {
            variableList.clear();
        }
    }

    public String get(String key) {
        // Từ variableList Nhận các biến được kích hoạt
        if (variableList != null && !variableList.isEmpty()) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled() && key.equals(envVar.getKey())) {
                    return envVar.getValue();
                }
            }
        }
        return null;
    }

    public String getVariable(String key) {
        return get(key);
    }

    public boolean hasVariable(String key) {
        // Từ variableList Tìm các biến được kích hoạt
        if (variableList != null && !variableList.isEmpty()) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled() && key.equals(envVar.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Nhận tất cả các biến Map bày tỏ
     * Từ variableList Xây dựng Map，Chỉ các biến được kích hoạt mới được bao gồm
     */
    public Map<String, String> getVariables() {
        Map<String, String> result = new LinkedHashMap<>();
        if (variableList != null) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled()) {
                    result.put(envVar.getKey(), envVar.getValue());
                }
            }
        }
        return result;
    }
}
