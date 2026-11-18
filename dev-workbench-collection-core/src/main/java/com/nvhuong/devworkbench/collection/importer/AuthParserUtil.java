package com.nvhuong.devworkbench.collection.importer;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_API_KEY;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_DIGEST;

/**
 * Lớp công cụ phân tích xác thực
 * Cung cấp logic phân tích thông tin xác thực thống nhất cho từng thông tin Parser Tái sử dụng
 */
@Slf4j
@UtilityClass
public class AuthParserUtil {

    // Định nghĩa hằng số
    private static final String DEV_WORKBENCH_AUTH_TYPE_BASIC = "basic";
    private static final String DEV_WORKBENCH_AUTH_TYPE_BEARER = "bearer";
    private static final String DEV_WORKBENCH_AUTH_TYPE_DIGEST = "digest";
    private static final String DEV_WORKBENCH_AUTH_TYPE_API_KEY = "apikey";
    private static final String KEY_VALUE = "value";

    /**
     * BasicThông tin xác thực
     */
    @Data
    public static class BasicAuthCredentials {
        private final String username;
        private final String password;
    }

    /**
     * Từ DevWorkbench định dạng auth Đối tượng được giải quyết RequestGroup
     *
     * @param auth DevWorkbench auth JSON đối tượng
     * @param group Đối tượng nhóm mục tiêu
     */
    public static void parseDevWorkbenchAuthToGroup(JSONObject auth, RequestGroup group) {
        String authType = auth.getStr("type", "");
        if (DEV_WORKBENCH_AUTH_TYPE_BASIC.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BASIC);
            JSONArray basicArr = auth.getJSONArray(DEV_WORKBENCH_AUTH_TYPE_BASIC);
            String username = null;
            String password = null;
            if (basicArr != null) {
                for (Object o : basicArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("username".equals(oObj.getStr("key"))) {
                        username = oObj.getStr(KEY_VALUE, "");
                    }
                    if ("password".equals(oObj.getStr("key"))) {
                        password = oObj.getStr(KEY_VALUE, "");
                    }
                }
            }
            group.setAuthUsername(username);
            group.setAuthPassword(password);
        } else if (DEV_WORKBENCH_AUTH_TYPE_API_KEY.equals(authType)) {
            group.setAuthType(AUTH_TYPE_API_KEY);
            JSONArray apiKeyArr = auth.getJSONArray(DEV_WORKBENCH_AUTH_TYPE_API_KEY);
            applyDevWorkbenchApiKeyAuth(apiKeyArr, group::setAuthApiKeyName, group::setAuthApiKeyValue, group::setAuthApiKeyPlacement);
        } else if (DEV_WORKBENCH_AUTH_TYPE_BEARER.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BEARER);
            JSONArray bearerArr = auth.getJSONArray(DEV_WORKBENCH_AUTH_TYPE_BEARER);
            if (bearerArr != null && !bearerArr.isEmpty()) {
                for (Object o : bearerArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("token".equals(oObj.getStr("key"))) {
                        group.setAuthToken(oObj.getStr(KEY_VALUE, ""));
                    }
                }
            }
        } else if (DEV_WORKBENCH_AUTH_TYPE_DIGEST.equals(authType)) {
            group.setAuthType(AUTH_TYPE_DIGEST);
            JSONArray digestArr = auth.getJSONArray(DEV_WORKBENCH_AUTH_TYPE_DIGEST);
            if (digestArr != null) {
                for (Object o : digestArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("username".equals(oObj.getStr("key"))) {
                        group.setAuthUsername(oObj.getStr(KEY_VALUE, ""));
                    }
                    if ("password".equals(oObj.getStr("key"))) {
                        group.setAuthPassword(oObj.getStr(KEY_VALUE, ""));
                    }
                }
            }
        }
    }

    public static void applyDevWorkbenchApiKeyAuth(JSONArray apiKeyArr,
                                              java.util.function.Consumer<String> keyConsumer,
                                              java.util.function.Consumer<String> valueConsumer,
                                              java.util.function.Consumer<String> placementConsumer) {
        if (apiKeyArr == null) {
            return;
        }
        for (Object o : apiKeyArr) {
            JSONObject oObj = (JSONObject) o;
            String key = oObj.getStr("key");
            if ("key".equals(key)) {
                keyConsumer.accept(oObj.getStr(KEY_VALUE, ""));
            } else if ("value".equals(key)) {
                valueConsumer.accept(oObj.getStr(KEY_VALUE, ""));
            } else if ("in".equals(key)) {
                placementConsumer.accept(AuthApiKeyPlacement.fromDevWorkbenchValue(oObj.getStr(KEY_VALUE, "header")).getConstant());
            }
        }
    }

    /**
     * Từ Basic Authorization Thông tin xác thực phân tích cú pháp tiêu đề
     * Hỗ trợ Base64 Mã hóa và định dạng biến đổi
     *
     * @param authValue Authorization Giá trị tiêu đề (ví dụ: ."Basic dXNlcm5hbWU6cGFzc3dvcmQ="）
     * @return BasicThông tin xác thực, được trả về nếu phân tích cú pháp không thành côngnull
     */
    public static BasicAuthCredentials parseBasicAuthHeader(String authValue) {
        if (authValue == null || !authValue.startsWith("Basic ")) {
            return null;
        }

        String credentials = authValue.substring(6).trim();

        // Kiểm tra xem đó có phải là định dạng giữ chỗ có thể thay đổi hay không: .Basic {{username}} {{password}}
        if (credentials.contains("{{") && credentials.contains("}}")) {
            // Định dạng biến đổi
            String[] parts = credentials.split("\\s+");
            if (parts.length >= 2) {
                // Định dạng:Basic {{username}} {{password}}
                return new BasicAuthCredentials(parts[0], parts[1]);
            } else {
                // Định dạng:Basic {{username}}:{{password}}
                int colonIndex = credentials.indexOf(':');
                if (colonIndex > 0) {
                    return new BasicAuthCredentials(
                        credentials.substring(0, colonIndex),
                        credentials.substring(colonIndex + 1)
                    );
                } else {
                    return new BasicAuthCredentials(credentials, "");
                }
            }
        } else {
            // Base64 Định dạng mã hóa
            try {
                String decoded = new String(Base64.getDecoder().decode(credentials));
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) {
                    return new BasicAuthCredentials(parts[0], parts[1]);
                } else {
                    return new BasicAuthCredentials(decoded, "");
                }
            } catch (Exception e) {
                log.warn("phân tích Basic Xác thực không thành công, có thể do định dạng thay đổi", e);
                return new BasicAuthCredentials(credentials, "");
            }
        }
    }
}
