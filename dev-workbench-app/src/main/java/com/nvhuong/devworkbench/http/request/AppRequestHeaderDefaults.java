package com.nvhuong.devworkbench.http.request;

import com.nvhuong.devworkbench.request.defaults.GeneratedRequestHeaderPolicy;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class AppRequestHeaderDefaults {

    public static GeneratedRequestHeaderPolicy generatedHeaderPolicy() {
        return GeneratedRequestHeaderPolicy.standard(HttpRequestFactory.DEV_WORKBENCH_CLIENT);
    }

    public static List<HttpHeader> generatedHeaders() {
        return generatedHeaderPolicy().generatedHeaders();
    }
}
