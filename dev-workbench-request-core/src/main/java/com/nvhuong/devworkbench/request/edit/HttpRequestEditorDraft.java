package com.nvhuong.devworkbench.request.edit;

import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpRequestProxyPolicy;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class HttpRequestEditorDraft {
    String id;
    String name;
    String description;
    String url;
    String method;
    RequestItemProtocolEnum protocol;
    @Builder.Default
    List<HttpHeader> headers = Collections.emptyList();
    @Builder.Default
    List<HttpParam> pathVariables = Collections.emptyList();
    @Builder.Default
    List<HttpParam> params = Collections.emptyList();
    String bodyType;
    String body;
    @Builder.Default
    List<HttpFormData> formData = Collections.emptyList();
    @Builder.Default
    List<HttpFormUrlencoded> urlencoded = Collections.emptyList();
    String authType;
    String authUsername;
    String authPassword;
    String authToken;
    String authApiKeyName;
    String authApiKeyValue;
    String authApiKeyPlacement;
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    @Builder.Default
    HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT;
    String httpVersion;
    Integer requestTimeoutMs;
    Integer webSocketPingIntervalMs;
    String prescript;
    String postscript;
    @Builder.Default
    List<SavedResponse> responses = Collections.emptyList();
}
