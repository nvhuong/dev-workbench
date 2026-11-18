package com.nvhuong.devworkbench.service.js;

import lombok.Getter;

/**
 * Ngoại lệ thực thi tập lệnh
 */
@Getter
public class ScriptExecutionException extends RuntimeException {
    private final ScriptExecutionContext.ScriptType scriptType;

    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.scriptType = null;
    }

    public ScriptExecutionException(String message, Throwable cause, ScriptExecutionContext.ScriptType scriptType) {
        super(message, cause);
        this.scriptType = scriptType;
    }

}