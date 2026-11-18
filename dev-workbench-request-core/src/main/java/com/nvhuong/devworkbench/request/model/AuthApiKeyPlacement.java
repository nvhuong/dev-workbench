package com.nvhuong.devworkbench.request.model;

import lombok.Getter;

@Getter
public enum AuthApiKeyPlacement {
    HEADER("Header", "header"),
    QUERY_PARAMS("Query Params", "query");

    private final String constant;
    private final String devworkbenchValue;

    AuthApiKeyPlacement(String constant, String devworkbenchValue) {
        this.constant = constant;
        this.devworkbenchValue = devworkbenchValue;
    }

    public static AuthApiKeyPlacement fromConstant(String constant) {
        if (constant == null || constant.trim().isEmpty()) {
            return HEADER;
        }
        for (AuthApiKeyPlacement placement : values()) {
            if (placement.constant.equals(constant)) {
                return placement;
            }
        }
        return HEADER;
    }

    public static AuthApiKeyPlacement fromDevWorkbenchValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HEADER;
        }
        for (AuthApiKeyPlacement placement : values()) {
            if (placement.devworkbenchValue.equalsIgnoreCase(value.trim())) {
                return placement;
            }
        }
        return HEADER;
    }
}
