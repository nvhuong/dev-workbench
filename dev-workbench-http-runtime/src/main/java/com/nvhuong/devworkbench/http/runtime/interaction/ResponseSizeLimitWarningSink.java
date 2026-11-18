package com.nvhuong.devworkbench.http.runtime.interaction;

@FunctionalInterface
public interface ResponseSizeLimitWarningSink {
    ResponseSizeLimitWarningSink NOOP = warning -> {
    };

    void warn(ResponseSizeLimitWarning warning);

    static ResponseSizeLimitWarningSink noop() {
        return NOOP;
    }
}
