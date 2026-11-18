package com.nvhuong.devworkbench.mock.app;

import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

public record MockCollectionChoice(String id, String name, int requestCount, int configuredResponseCount) {
    @Override
    public String toString() {
        return name + "  " + I18nUtil.getMessage(
                MessageKeys.MOCK_SERVER_COLLECTION_SUMMARY, requestCount, configuredResponseCount);
    }
}
