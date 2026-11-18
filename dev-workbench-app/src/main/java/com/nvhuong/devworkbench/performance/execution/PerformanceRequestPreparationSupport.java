package com.nvhuong.devworkbench.performance.execution;

import com.nvhuong.devworkbench.performance.core.plan.PerformanceAssertionElement;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceExtractorElement;

import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfile;
import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfiles;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class PerformanceRequestPreparationSupport {

    void configurePreparedRequest(PreparedRequest request, boolean eventLoggingEnabled) {
        HttpCaptureProfiles.apply(request, eventLoggingEnabled
                ? HttpCaptureProfile.PERFORMANCE_EVENT_TRACE
                : HttpCaptureProfile.PERFORMANCE_METRICS);
    }

    PreparedRequest.ResponseBodyMode resolveHttpResponseBodyModeForAssertionElements(
            boolean efficientMode,
            List<PerformanceAssertionElement> assertionNodes,
            String postscript) {
        return resolveHttpResponseBodyModeForAssertionElements(efficientMode, assertionNodes, List.of(), postscript);
    }

    PreparedRequest.ResponseBodyMode resolveHttpResponseBodyModeForAssertionElements(
            boolean efficientMode,
            List<PerformanceAssertionElement> assertionNodes,
            List<PerformanceExtractorElement> extractorNodes,
            String postscript) {
        return PerformanceResponseCapturePlan.resolveHttpResponseBodyMode(
                efficientMode,
                assertionNodes,
                extractorNodes,
                postscript
        );
    }

    int resolveResponseBodyPreviewLimitBytes(int previewLimitKb) {
        return PerformanceExecutionConfig.responseBodyPreviewLimitBytes(previewLimitKb);
    }
}
