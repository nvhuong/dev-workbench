package com.nvhuong.devworkbench.model;

import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

public enum GitOperation {
    COMMIT(MessageKeys.GIT_OPERATION_COMMIT),
    PUSH(MessageKeys.GIT_OPERATION_PUSH),
    PULL(MessageKeys.GIT_OPERATION_PULL);

    private final String displayNameKey;

    GitOperation(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    public String getDisplayName() {
        return I18nUtil.getMessage(displayNameKey);
    }
}
