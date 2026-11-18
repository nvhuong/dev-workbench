package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.performance.execution.PerformanceRequestExecutionResult;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.model.PerformanceResultListener;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleEvent;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleResult;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceResultSinkListenerAdapterTest {

    @Test
    public void shouldForwardSampleEventsToExistingListeners() {
        List<PerformanceSampleEvent> events = new ArrayList<>();
        PerformanceResultListener listener = events::add;
        PerformanceResultSink sink = new PerformanceResultSinkListenerAdapter(List.of(listener));

        sink.onSample(sampleEvent());

        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getSampleResult().getApiId(), "api-1");
    }

    private static PerformanceSampleEvent sampleEvent() {
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 12;
        response.endTime = 112;
        PerformanceRequestExecutionResult result = new PerformanceRequestExecutionResult(
                "api-1",
                "API",
                null,
                response,
                "",
                List.of(),
                false,
                false,
                PerformanceProtocol.HTTP,
                100,
                12
        );
        return new PerformanceSampleEvent(
                PerformanceSampleResult.fromExecutionResult(result),
                result,
                true
        );
    }
}
