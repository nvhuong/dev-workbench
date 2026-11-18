package com.nvhuong.devworkbench.request.edit;

import com.nvhuong.devworkbench.request.model.HttpRequestVersions;
import com.nvhuong.devworkbench.request.model.HttpRequestProxyPolicy;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpRequestSettingsDraft {
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    @Builder.Default
    HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT;
    @Builder.Default
    String httpVersion = HttpRequestVersions.AUTO;
    Integer requestTimeoutMs;
    Integer webSocketPingIntervalMs;
}
