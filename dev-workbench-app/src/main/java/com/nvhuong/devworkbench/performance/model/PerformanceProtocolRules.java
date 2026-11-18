package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;



import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceProtocolRules {

    public RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    public boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return resolveRequestProtocol(item).isWebSocketProtocol();
    }
}
