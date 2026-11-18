package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.transport.HttpExchangeTerminalResponseFactory;
import lombok.experimental.UtilityClass;

@UtilityClass
class HttpRequestCancellationResponseSupport {

    static HttpResponse resolveTerminalResponse(PreparedRequest request,
                                                HttpResponse response,
                                                boolean workerCanceled,
                                                long requestStartMs,
                                                long endTimeMs) {
        if (response != null || !workerCanceled) {
            return response;
        }
        return HttpExchangeTerminalResponseFactory.fromCancellation(request, requestStartMs, endTimeMs);
    }
}
