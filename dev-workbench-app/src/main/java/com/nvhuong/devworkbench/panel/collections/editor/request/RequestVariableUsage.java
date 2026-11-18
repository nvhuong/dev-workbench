package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.variable.VariableType;

record RequestVariableUsage(String name, String value, VariableType type) {
    boolean defined() {
        return type != null && value != null;
    }
}
