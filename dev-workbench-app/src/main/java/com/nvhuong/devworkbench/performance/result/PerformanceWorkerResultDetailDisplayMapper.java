package com.nvhuong.devworkbench.performance.result;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.performance.model.ResultNodeInfo;
import com.nvhuong.devworkbench.performance.model.ResultTiming;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerResultDetail;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerResultDetail.DetailRequest;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerResultDetail.DetailResponse;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerResultDetail.DetailTestResult;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PerformanceWorkerResultDetailDisplayMapper {

    public ResultNodeInfo toResultNodeInfo(PerformanceWorkerResultDetail detail) {
        if (detail == null) {
            return null;
        }
        HttpResponse response = toResponse(detail.getResponse(), detail.getResponseCode(), detail.getCostMs());
        return new ResultNodeInfo(
                detail.getName(),
                detail.getErrorMsg(),
                toRequest(detail.getRequest()),
                response,
                toTests(detail.getTestResults()),
                detail.isExecutionFailed(),
                detail.isCancelled(),
                toProtocol(detail.getProtocol()),
                ResultTiming.of(resolveStartTimeMs(detail, response), detail.getLatencyMs(), detail.getConnectMs())
        );
    }

    // cũ worker Thời gian bắt đầu không được thông qua và thời gian được tính bằng cách trừ đi thời gian kết thúc.
    private long resolveStartTimeMs(PerformanceWorkerResultDetail detail, HttpResponse response) {
        if (detail.getStartTimeMs() > 0) {
            return detail.getStartTimeMs();
        }
        return response.endTime > 0 ? response.endTime - Math.max(0L, response.costMs) : ResultTiming.UNKNOWN;
    }

    private PreparedRequest toRequest(DetailRequest source) {
        if (source == null) {
            return null;
        }
        PreparedRequest request = new PreparedRequest();
        request.method = source.getMethod();
        request.url = source.getUrl();
        request.sentRequestBody = source.getBody();
        request.sentHeadersList = toHttpHeaders(source.getHeaders());
        return request;
    }

    private HttpResponse toResponse(DetailResponse source, int responseCode, int costMs) {
        HttpResponse response = new HttpResponse();
        if (source != null) {
            response.code = source.getCode();
            response.protocol = source.getProtocol();
            response.headers = source.getHeaders();
            response.body = source.getBody();
            response.costMs = source.getCostMs();
            response.endTime = source.getEndTimeMs();
            response.bodySize = source.getBodySize();
            response.headersSize = source.getHeadersSize();
            response.fileName = source.getFileName();
            response.detectedFileExtension = source.getDetectedFileExtension();
            response.fileContentOmitted = source.isFileContentOmitted();
        }
        if (response.code <= 0) {
            response.code = Math.max(0, responseCode);
        }
        if (response.costMs <= 0) {
            response.costMs = Math.max(0, costMs);
        }
        return response;
    }

    private List<TestResult> toTests(List<DetailTestResult> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(test -> new TestResult(test.getName(), test.isPassed(), test.getMessage()))
                .toList();
    }

    private List<HttpHeader> toHttpHeaders(Map<String, List<String>> headers) {
        List<HttpHeader> result = new ArrayList<>();
        if (headers == null || headers.isEmpty()) {
            return result;
        }
        headers.forEach((key, values) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            if (values == null || values.isEmpty()) {
                result.add(new HttpHeader(true, key, ""));
                return;
            }
            for (String value : values) {
                result.add(new HttpHeader(true, key, value == null ? "" : value));
            }
        });
        return result;
    }

    private PerformanceProtocol toProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            return PerformanceProtocol.HTTP;
        }
        try {
            return PerformanceProtocol.valueOf(protocol);
        } catch (IllegalArgumentException ignored) {
            return PerformanceProtocol.HTTP;
        }
    }
}
