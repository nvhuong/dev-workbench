package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;

@FunctionalInterface
public interface RequestSaveListener {
    void onRequestSaved(HttpRequestItem item);
}
