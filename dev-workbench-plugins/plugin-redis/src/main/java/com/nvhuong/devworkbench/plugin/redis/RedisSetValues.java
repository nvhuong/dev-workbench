package com.nvhuong.devworkbench.plugin.redis;

import java.util.List;

/** Resolves the value written by SET without altering what the user or script provided. */
final class RedisSetValues {
    private RedisSetValues() {
    }

    /**
     * Panel: the value editor wins whenever it has content and is stored verbatim; the args field is only a fallback
     * for an empty editor ("SET key word1 word2").
     */
    static String forPanel(String editorValue, List<String> args) {
        if (editorValue != null && !editorValue.isBlank()) {
            return editorValue;
        }
        String fromArgs = joinArgs(args);
        if (fromArgs.isEmpty()) {
            throw new IllegalArgumentException(RedisI18n.t(MessageKeys.TOOLBOX_REDIS_ERR_SET_VALUE_REQUIRED));
        }
        return fromArgs;
    }

    /** Script API: an explicit value (even "") is stored as given; args are used only when no value was passed. */
    static String forScript(Object value, List<String> args) {
        if (value != null) {
            return String.valueOf(value);
        }
        String fromArgs = joinArgs(args);
        if (fromArgs.isEmpty()) {
            throw new IllegalArgumentException("SET requires a value");
        }
        return fromArgs;
    }

    private static String joinArgs(List<String> args) {
        return args == null ? "" : String.join(" ", args);
    }
}
