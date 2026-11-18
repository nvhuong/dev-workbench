package com.nvhuong.devworkbench.http.runtime.transport;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.okhttp.OkHttpRequestBuilder;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import lombok.experimental.UtilityClass;
import okhttp3.Request;

@UtilityClass
class PreparedOkHttpRequestFactory {

    static Request build(PreparedRequest req) {
        if (req.isMultipart) {
            return OkHttpRequestBuilder.buildMultipartRequest(req);
        }
        if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            return OkHttpRequestBuilder.buildFormRequest(req);
        }
        if (RequestBodyTypes.BODY_TYPE_BINARY.equals(req.bodyType)) {
            return OkHttpRequestBuilder.buildBinaryRequest(req);
        }
        return OkHttpRequestBuilder.buildRequest(req);
    }
}
