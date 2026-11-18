package com.nvhuong.devworkbench.http.request;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.mapper.PreparedRequestMapper;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.TransportAuth;


import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.VariableResolver;
import com.nvhuong.devworkbench.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static com.nvhuong.devworkbench.common.constants.HttpConstants.HEADER_AUTHORIZATION;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_API_KEY;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_DIGEST;

/**
 * Logic đóng cuối cùng trước khi gửi:
 * 1. Thay thế các phần giữ chỗ có thể thay đổi trong các yêu cầu
 * 2. nếu kịch bản/Tiêu đề yêu cầu chưa có ở đó Authorization，Nhấn lại auth tab Tự động hoàn thành
 * <p>
 * Quy ước ưu tiên xác thực:
 * <ol>
 *   <li>pre-script được viết rõ ràng bằng  Authorization</li>
 *   <li>Đã có trong danh sách tiêu đề yêu cầu Authorization</li>
 *   <li>auth tab Tự động tạo ra Authorization</li>
 * </ol>
 * <p>
 * Để giữ cho các quy tắc đơn giản và ổn định,pm.request.headers.remove("Authorization")
 * Không còn thể hiện nữa“Tàn tật auth tab Chứng nhận”，Nó chỉ có nghĩa là xóa cái hiện tại header。
 * Nếu vẫn không có tiêu đề yêu cầu ở giai đoạn cuối Authorization，Và auth tab Xác thực được cấu hình lại.
 * finalizer Tiêu đề xác thực sẽ được nạp lại theo cấu hình.
 */
@UtilityClass
public class PreparedRequestFinalizer {

    /**
     * Hoàn thành các quyết định xác thực và thay thế biến một cách thống nhất trước khi yêu cầu thực sự được đưa ra.
     * Tất cả các đường dẫn gửi (gửi bình thường, .Functional、Performance、Sao chép cURL）
     * Tất cả nên được tái sử dụng ở đây càng nhiều càng tốt để tránh hành vi không nhất quán ở các lối vào khác nhau.
     */
    public void finalizeForSend(PreparedRequest request,
                                PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        try {
            replaceVariablesInHeadersList(request.headersList);
            replaceVariablesInFormDataList(request.formDataList);
            replaceVariablesInUrlencodedList(request.urlencodedList);

            request.url = VariableResolver.resolve(request.url);
            replaceVariablesInParamsList(request.pathVariablesList);
            request.url = HttpUrlUtil.replacePathVariables(request.url, request.pathVariablesList);
            replaceVariablesInParamsList(request.paramsList);
            applyDeferredQueryAuthorization(request, deferredAuthorization);
            request.url = HttpUrlUtil.normalizeIpv6Url(
                    HttpUrlUtil.buildEncodedUrl(request.url, request.paramsList)
            );
            request.body = VariableResolver.resolve(request.body);

            applyDeferredAuthorization(request, deferredAuthorization);
        } finally {
            RequestExecutionContext.clearCurrentScope();
        }
    }

    public void finalizeForSend(PreparedRequest request) {
        finalizeForSend(request, (PreparedRequestMapper.DeferredAuthorization) null);
    }

    public void finalizeForSend(PreparedRequest request, HttpRequestItem item) {
        finalizeForSend(request, PreparedRequestFactory.resolveDeferredAuthorization(item));
    }

    private void applyDeferredAuthorization(PreparedRequest request,
                                            PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (request == null) {
            return;
        }
        request.transportAuth = null;

        if (deferredAuthorization == null) {
            return;
        }

        applyDeferredHeaderAuthorization(request, deferredAuthorization);
        if (hasNonPreviewAuthorizationHeader(request.headersList, deferredAuthorization)) {
            return;
        }
        applyTransportAuthorization(request, deferredAuthorization);
    }

    private void applyDeferredHeaderAuthorization(PreparedRequest request,
                                                  PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        HttpHeader authHeader = createAuthHeader(deferredAuthorization);
        List<HttpHeader> stalePreviewHeaders = findPreviewHeaders(request.headersList, deferredAuthorization);

        if (authHeader == null) {
            removeHeaders(request.headersList, stalePreviewHeaders);
            return;
        }

        if (!sameHeaderKey(deferredAuthorization.previewHeaderKey(), authHeader.getKey())) {
            removeHeaders(request.headersList, stalePreviewHeaders);
        }

        List<HttpHeader> sameKeyHeaders = findEnabledHeaders(request.headersList, authHeader.getKey());
        List<HttpHeader> previewHeaders = findPreviewHeaders(sameKeyHeaders, deferredAuthorization);
        if (previewHeaders.size() != sameKeyHeaders.size()) {
            removeHeaders(request.headersList, stalePreviewHeaders);
            return;
        }

        if (sameKeyHeaders.isEmpty()) {
            if (request.headersList == null) {
                request.headersList = new ArrayList<>();
            }
            request.headersList.add(authHeader);
            return;
        }

        HttpHeader primaryPreviewHeader = previewHeaders.get(0);
        primaryPreviewHeader.setKey(authHeader.getKey());
        primaryPreviewHeader.setValue(authHeader.getValue());
        primaryPreviewHeader.setEnabled(true);
        removeHeaders(request.headersList, previewHeaders.subList(1, previewHeaders.size()));
    }

    private void applyDeferredQueryAuthorization(PreparedRequest request,
                                                 PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (request == null || deferredAuthorization == null) {
            return;
        }

        HttpParam apiKeyParam = createQueryAuthParam(deferredAuthorization);
        List<HttpParam> stalePreviewParams = findPreviewQueryParams(request.paramsList, deferredAuthorization);
        if (apiKeyParam == null) {
            removeParams(request.paramsList, stalePreviewParams);
            return;
        }

        if (!sameParamKey(deferredAuthorization.previewQueryParamKey(), apiKeyParam.getKey())) {
            removeParams(request.paramsList, stalePreviewParams);
        }

        if (hasQueryParamInUrl(request.url, apiKeyParam.getKey())) {
            removeParams(request.paramsList, stalePreviewParams);
            return;
        }

        List<HttpParam> sameKeyParams = findEnabledParams(request.paramsList, apiKeyParam.getKey());
        List<HttpParam> previewParams = findPreviewQueryParams(sameKeyParams, deferredAuthorization);
        if (previewParams.size() != sameKeyParams.size()) {
            removeParams(request.paramsList, stalePreviewParams);
            return;
        }

        if (sameKeyParams.isEmpty()) {
            if (request.paramsList == null) {
                request.paramsList = new ArrayList<>();
            }
            request.paramsList.add(apiKeyParam);
            return;
        }

        HttpParam primaryPreviewParam = previewParams.get(0);
        primaryPreviewParam.setKey(apiKeyParam.getKey());
        primaryPreviewParam.setValue(apiKeyParam.getValue());
        primaryPreviewParam.setEnabled(true);
        removeParams(request.paramsList, previewParams.subList(1, previewParams.size()));
    }

    private void applyTransportAuthorization(PreparedRequest request,
                                             PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (!AUTH_TYPE_DIGEST.equals(deferredAuthorization.authType())) {
            return;
        }

        String username = VariableResolver.resolve(deferredAuthorization.authUsername());
        String password = VariableResolver.resolve(deferredAuthorization.authPassword());
        if (username == null || username.isEmpty()
                || containsUnresolvedPlaceholder(username)
                || containsUnresolvedPlaceholder(password)) {
            return;
        }

        request.transportAuth = new TransportAuth(
                AUTH_TYPE_DIGEST,
                username,
                password == null ? "" : password
        );
    }

    private List<HttpHeader> findEnabledHeaders(List<HttpHeader> headersList, String key) {
        if (headersList == null || headersList.isEmpty()) {
            return List.of();
        }
        return headersList.stream()
                .filter(h -> h != null && h.isEnabled() && key != null && key.equalsIgnoreCase(h.getKey()))
                .toList();
    }

    private List<HttpHeader> findPreviewHeaders(List<HttpHeader> headersList,
                                                PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        String previewKey = deferredAuthorization.previewHeaderKey();
        String previewValue = deferredAuthorization.previewHeaderValue();
        if (previewKey == null || previewValue == null || headersList == null || headersList.isEmpty()) {
            return List.of();
        }
        return headersList.stream()
                .filter(header -> header != null
                        && header.isEnabled()
                        && previewKey.equalsIgnoreCase(header.getKey())
                        && previewValue.equals(header.getValue()))
                .toList();
    }

    private boolean hasNonPreviewAuthorizationHeader(List<HttpHeader> headersList,
                                                     PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        List<HttpHeader> authorizationHeaders = findEnabledHeaders(headersList, HEADER_AUTHORIZATION);
        if (authorizationHeaders.isEmpty()) {
            return false;
        }
        List<HttpHeader> previewAuthorizationHeaders = findPreviewHeaders(authorizationHeaders, deferredAuthorization);
        return previewAuthorizationHeaders.size() != authorizationHeaders.size();
    }

    private void removeHeaders(List<HttpHeader> headersList, List<HttpHeader> headersToRemove) {
        if (headersList == null || headersList.isEmpty() || headersToRemove == null || headersToRemove.isEmpty()) {
            return;
        }
        headersList.removeIf(headersToRemove::contains);
    }

    private List<HttpParam> findEnabledParams(List<HttpParam> paramsList, String key) {
        if (paramsList == null || paramsList.isEmpty()) {
            return List.of();
        }
        return paramsList.stream()
                .filter(param -> param != null && param.isEnabled() && key != null && key.equals(param.getKey()))
                .toList();
    }

    private List<HttpParam> findPreviewQueryParams(List<HttpParam> paramsList,
                                                   PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        String previewKey = deferredAuthorization.previewQueryParamKey();
        String previewValue = deferredAuthorization.previewQueryParamValue();
        if (previewKey == null || previewValue == null || paramsList == null || paramsList.isEmpty()) {
            return List.of();
        }
        return paramsList.stream()
                .filter(param -> param != null
                        && param.isEnabled()
                        && previewKey.equals(param.getKey())
                        && previewValue.equals(param.getValue()))
                .toList();
    }

    private void removeParams(List<HttpParam> paramsList, List<HttpParam> paramsToRemove) {
        if (paramsList == null || paramsList.isEmpty() || paramsToRemove == null || paramsToRemove.isEmpty()) {
            return;
        }
        paramsList.removeIf(paramsToRemove::contains);
    }

    private HttpHeader createAuthHeader(PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (deferredAuthorization == null || deferredAuthorization.authType() == null) {
            return null;
        }
        if (AUTH_TYPE_BASIC.equals(deferredAuthorization.authType())) {
            return createBasicAuthHeader(
                    deferredAuthorization.authUsername(),
                    deferredAuthorization.authPassword()
            );
        } else if (AUTH_TYPE_BEARER.equals(deferredAuthorization.authType())) {
            return createBearerAuthHeader(deferredAuthorization.authToken());
        } else if (AUTH_TYPE_API_KEY.equals(deferredAuthorization.authType())
                && AuthApiKeyPlacement.HEADER == AuthApiKeyPlacement.fromConstant(deferredAuthorization.authApiKeyPlacement())) {
            return createApiKeyAuthHeader(
                    deferredAuthorization.authApiKeyName(),
                    deferredAuthorization.authApiKeyValue()
            );
        }
        return null;
    }

    private HttpHeader createBasicAuthHeader(String rawUsername, String rawPassword) {
        String username = VariableResolver.resolve(rawUsername);
        if (username == null || username.isEmpty() || containsUnresolvedPlaceholder(username)) {
            return null;
        }

        String password = VariableResolver.resolve(rawPassword);
        if (containsUnresolvedPlaceholder(password)) {
            return null;
        }
        String credentials = username + ":" + (password == null ? "" : password);
        String token = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Basic " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private HttpHeader createApiKeyAuthHeader(String rawName, String rawValue) {
        String name = VariableResolver.resolve(rawName);
        if (name == null || name.isEmpty() || containsUnresolvedPlaceholder(name)) {
            return null;
        }
        String value = VariableResolver.resolve(rawValue);
        if (value == null || value.isEmpty() || containsUnresolvedPlaceholder(value)) {
            return null;
        }

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(name);
        authHeader.setValue(value);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private HttpParam createQueryAuthParam(PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (AUTH_TYPE_API_KEY.equals(deferredAuthorization.authType())) {
            return createApiKeyQueryParam(deferredAuthorization);
        }
        return null;
    }

    private HttpParam createApiKeyQueryParam(PreparedRequestMapper.DeferredAuthorization deferredAuthorization) {
        if (AuthApiKeyPlacement.QUERY_PARAMS != AuthApiKeyPlacement.fromConstant(deferredAuthorization.authApiKeyPlacement())) {
            return null;
        }
        String name = VariableResolver.resolve(deferredAuthorization.authApiKeyName());
        String value = VariableResolver.resolve(deferredAuthorization.authApiKeyValue());
        return createEnabledParam(name, value);
    }

    private HttpParam createEnabledParam(String name, String value) {
        if (name == null || name.isEmpty() || containsUnresolvedPlaceholder(name)) {
            return null;
        }
        if (value == null || value.isEmpty() || containsUnresolvedPlaceholder(value)) {
            return null;
        }
        return new HttpParam(true, name, value, "");
    }

    private boolean hasQueryParamInUrl(String url, String key) {
        if (url == null || key == null || key.isEmpty() || !url.contains("?")) {
            return false;
        }
        return HttpUrlUtil.extractQueryParamKeys(url, true).contains(HttpUrlUtil.encodeComponent(key));
    }

    private boolean sameHeaderKey(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equalsIgnoreCase(second);
    }

    private boolean sameParamKey(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equals(second);
    }

    private HttpHeader createBearerAuthHeader(String rawToken) {
        String token = VariableResolver.resolve(rawToken);
        if (token == null || token.isEmpty() || containsUnresolvedPlaceholder(token)) {
            return null;
        }

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Bearer " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }

    private void replaceVariablesInHeadersList(List<HttpHeader> list) {
        if (list == null) return;
        for (HttpHeader item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInFormDataList(List<HttpFormData> list) {
        if (list == null) return;
        for (HttpFormData item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInUrlencodedList(List<HttpFormUrlencoded> list) {
        if (list == null) return;
        for (HttpFormUrlencoded item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInParamsList(List<HttpParam> list) {
        if (list == null) return;
        for (HttpParam item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }
}
