package com.nvhuong.devworkbench.http.runtime.transport;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import okhttp3.OkHttpClient;

@FunctionalInterface
public interface HttpBaseClientProvider {
    OkHttpClient getBaseClient(PreparedRequest request);
}
