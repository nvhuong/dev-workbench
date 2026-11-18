package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.model.PerformanceSampleRecord;
import com.nvhuong.devworkbench.performance.core.model.RequestResult;


import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.performance.execution.PerformanceRequestExecutionResult;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class PerformanceSampleResult {
    String apiId;
    String apiName;
    PreparedRequest request;
    HttpResponse response;
    String errorMsg;
    List<TestResult> testResults;
    List<PerformanceAssertionResult> assertionResults;
    boolean executionFailed;
    boolean interrupted;
    PerformanceProtocol protocol;
    long startTimeMs;
    long endTimeMs;
    long elapsedTimeMs;
    int responseCode;
    long bodySize;
    long headersSize;
    int sentMessages;
    int receivedMessages;
    int matchedMessages;
    // Số byte được gửi: tiêu đề yêu cầu + Yêu cầu nội dung, thống kê Sent KB/s khi sử dụng
    long sentBytes;
    // Số byte nhận được: tiêu đề phản hồi + Cơ quan phản hồi, thống kê Received KB/s và Avg. Bytes khi sử dụng
    long receivedBytes;
    long firstMessageLatencyMs;
    boolean successful;

    public static PerformanceSampleResult fromExecutionResult(PerformanceRequestExecutionResult executionResult) {
        if (executionResult == null) {
            return null;
        }
        PerformanceSampleRecord record = PerformanceSampleRecordFactory.fromExecutionResult(executionResult);
        if (record == null) {
            return null;
        }
        HttpResponse response = executionResult.response;
        List<TestResult> testResults = executionResult.testResults == null
                ? List.of()
                : List.copyOf(executionResult.testResults);
        return PerformanceSampleResult.builder()
                .apiId(record.getApiId())
                .apiName(record.getApiName())
                .request(executionResult.request)
                .response(response)
                .errorMsg(record.getErrorMsg())
                .testResults(testResults)
                .assertionResults(testResults.stream()
                        .map(PerformanceAssertionResult::fromTestResult)
                        .toList())
                .executionFailed(record.isExecutionFailed())
                .interrupted(record.isInterrupted())
                .protocol(record.getProtocol())
                .startTimeMs(record.getStartTimeMs())
                .endTimeMs(record.getEndTimeMs())
                .elapsedTimeMs(record.getElapsedTimeMs())
                .responseCode(record.getResponseCode())
                .bodySize(record.getBodySize())
                .headersSize(record.getHeadersSize())
                .sentMessages(record.getSentMessages())
                .receivedMessages(record.getReceivedMessages())
                .matchedMessages(record.getMatchedMessages())
                .sentBytes(record.getSentBytes())
                .receivedBytes(record.getReceivedBytes())
                .firstMessageLatencyMs(record.getFirstMessageLatencyMs())
                .successful(record.isSuccessful())
                .build();
    }

    public RequestResult toRequestResult() {
        return toSampleRecord().toRequestResult();
    }

    public PerformanceSampleRecord toSampleRecord() {
        return PerformanceSampleRecord.builder()
                .apiId(apiId)
                .apiName(apiName)
                .errorMsg(errorMsg)
                .executionFailed(executionFailed)
                .interrupted(interrupted)
                .protocol(protocol)
                .startTimeMs(startTimeMs)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(elapsedTimeMs)
                .responseCode(responseCode)
                .bodySize(bodySize)
                .headersSize(headersSize)
                .sentMessages(sentMessages)
                .receivedMessages(receivedMessages)
                .matchedMessages(matchedMessages)
                .sentBytes(sentBytes)
                .receivedBytes(receivedBytes)
                .firstMessageLatencyMs(firstMessageLatencyMs)
                .successful(successful)
                .build();
    }
}
