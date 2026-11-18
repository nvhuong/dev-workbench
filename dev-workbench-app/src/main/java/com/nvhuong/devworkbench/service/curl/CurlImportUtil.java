package com.nvhuong.devworkbench.service.curl;

import com.nvhuong.devworkbench.request.model.AuthType;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.nvhuong.devworkbench.http.request.HttpRequestProtocol;
import com.nvhuong.devworkbench.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

/**
 * cURL Nhập lớp công cụ
 * cung cấp cURL Các lệnh được phân tích cú pháp và chuyển đổi thành  HttpRequestItem chức năng
 */
@UtilityClass
public class CurlImportUtil {

    /**
     * phân tích cURL lệnh và chuyển đổi thành HttpRequestItem
     *
     * @param curlText cURL Văn bản lệnh (có thể là  cURL Chuỗi lệnh hoặc được phân tích cú pháp CurlRequest đối tượng)
     * @return Sau khi phân tích HttpRequestItem，Trả về nếu phân tích cú pháp không thành công null
     */
    public static HttpRequestItem fromCurl(String curlText) {
        if (CharSequenceUtil.isBlank(curlText)) {
            return null;
        }

        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            return fromCurlRequest(curlRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse cURL command", e);
        }
    }

    /**
     * Sẽ CurlRequest Chuyển đổi sang  HttpRequestItem
     *
     * @param curlRequest Đã phân tích cú pháp cURL đối tượng yêu cầu
     * @return HttpRequestItem Đối tượng nếu curlRequest Trả lại không hợp lệ null
     */
    public static HttpRequestItem fromCurlRequest(CurlRequest curlRequest) {
        if (curlRequest == null || curlRequest.url == null) {
            return null;
        }

        HttpRequestItem item = new HttpRequestItem();
        item.setName(null);
        item.setUrl(HttpUrlUtil.decodeQueryForDisplay(curlRequest.url));
        item.setMethod(curlRequest.method);

        if (CollUtil.isNotEmpty(curlRequest.headersList)) {
            item.setHeadersList(curlRequest.headersList);
        }

        // Preserve cURL body bytes semantically. Signed or challenge-protected requests can fail
        // if imported JSON is reformatted before sending.
        item.setBody(curlRequest.body == null ? "" : curlRequest.body);

        if (CollUtil.isNotEmpty(curlRequest.paramsList)) {
            item.setParamsList(curlRequest.paramsList);
        }

        if (CollUtil.isNotEmpty(curlRequest.formDataList)) {
            item.setFormDataList(curlRequest.formDataList);
            item.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_DATA);
        }

        if (CollUtil.isNotEmpty(curlRequest.urlencodedList)) {
            item.setUrlencodedList(curlRequest.urlencodedList);
            item.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_URLENCODED);
        }

        if (curlRequest.binaryBody) {
            item.setBodyType(RequestBodyTypes.BODY_TYPE_BINARY);
        } else if (curlRequest.body != null && !curlRequest.body.isEmpty()
                && CollUtil.isEmpty(curlRequest.formDataList)
                && CollUtil.isEmpty(curlRequest.urlencodedList)) {
            item.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
        }

        if (curlRequest.followRedirects) {
            item.setFollowRedirects(Boolean.TRUE);
        }

        if (AuthType.DIGEST.getConstant().equals(curlRequest.authType)
                && CharSequenceUtil.isNotBlank(curlRequest.authUsername)) {
            item.setAuthType(AuthType.DIGEST.getConstant());
            item.setAuthUsername(curlRequest.authUsername);
            item.setAuthPassword(curlRequest.authPassword != null ? curlRequest.authPassword : "");
        }

        // Xác định loại giao thức một cách thông minh
        if (HttpRequestProtocol.isWebSocketUrl(item.getUrl())) {
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        } else {
            item.setProtocol(RequestItemProtocolEnum.HTTP);
        }

        return item;
    }

}
