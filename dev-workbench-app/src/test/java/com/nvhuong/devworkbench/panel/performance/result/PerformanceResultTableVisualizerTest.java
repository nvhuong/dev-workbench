package com.nvhuong.devworkbench.panel.performance.result;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.model.PerformanceStatsCollector;
import com.nvhuong.devworkbench.performance.core.model.PerformanceStatsSnapshot;
import com.nvhuong.devworkbench.performance.execution.PerformanceRequestExecutionResult;
import com.nvhuong.devworkbench.performance.model.PerformanceStatsCollectorListener;
import com.nvhuong.devworkbench.performance.model.ResultNodeInfo;
import com.nvhuong.devworkbench.performance.result.PerformanceResultCollector;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceResultTableVisualizerTest extends AbstractSwingUiTest {

    @Test
    public void shouldShowInterruptedStreamResultAsCancelledWithoutCountingStats() {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        RecordingResultTablePanel tablePanel = new RecordingResultTablePanel();
        try {
            HttpResponse response = new HttpResponse();
            response.headers = new LinkedHashMap<>();
            response.code = 101;
            response.costMs = 2500;
            response.endTime = 3500;
            response.addHeader("X-DevWorkbench-WS-Sent-Count", List.of("12"));
            response.addHeader("X-DevWorkbench-WS-Received-Count", List.of("7"));
            response.addHeader("X-DevWorkbench-WS-Message-Count", List.of("3"));

            PerformanceResultCollector collector = newCollector(statsCollector, tablePanel);

            collector.collect(new PerformanceRequestExecutionResult(
                    "api-ws",
                    "WS API",
                    new PreparedRequest(),
                    response,
                    "",
                    List.of(),
                    false,
                    true,
                    PerformanceProtocol.WEBSOCKET,
                    1000L,
                    0L
            ), false);

            assertEquals(statsCollector.snapshot().totalRequests(), 0L, "Việc lấy mẫu bị hủy không được đưa vào số liệu thống kê");
            assertEquals(tablePanel.recordedRows, 1, "Các mẫu bị hủy vẫn được hiển thị trong bảng kết quả");
            assertTrue(tablePanel.lastResult.cancelled);
            assertFalse(tablePanel.lastResult.isActuallySuccessful());
        } finally {
            tablePanel.dispose();
        }
    }

    @Test
    public void shouldAggregateFastSuccessesWithoutRecordingDetailsInEfficientMode() {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        RecordingResultTablePanel tablePanel = new RecordingResultTablePanel();
        try {
            PerformanceResultCollector collector = newCollector(statsCollector, tablePanel);

            for (int i = 0; i < 10_000; i++) {
                HttpResponse response = new HttpResponse();
                response.code = 200;
                response.costMs = 100;
                response.endTime = i + 100L;

                collector.collect(new PerformanceRequestExecutionResult(
                        "api",
                        "API",
                        new PreparedRequest(),
                        response,
                        "",
                        List.of(),
                        false,
                        false,
                        PerformanceProtocol.HTTP,
                        i,
                        0
                ), true);
            }

            PerformanceStatsSnapshot snapshot = statsCollector.snapshot();

            assertEquals(snapshot.totalRequests(), 10_000L);
            assertEquals(snapshot.successRequests(), 10_000L);
            assertEquals(snapshot.retainedRequestResultCount(), 0L);
            assertEquals(tablePanel.recordedRows, 0);
        } finally {
            tablePanel.dispose();
        }
    }

    private static PerformanceResultCollector newCollector(PerformanceStatsCollector statsCollector,
                                                           PerformanceResultTablePanel tablePanel) {
        return new PerformanceResultCollector(List.of(
                new PerformanceStatsCollectorListener(statsCollector),
                new PerformanceResultTableVisualizer(tablePanel, () -> 3_000)
        ));
    }

    private static final class RecordingResultTablePanel extends PerformanceResultTablePanel {
        private int recordedRows;
        private ResultNodeInfo lastResult;

        @Override
        public void addResult(ResultNodeInfo info) {
            recordedRows++;
            lastResult = info;
        }

        @Override
        public void addResult(ResultNodeInfo info, boolean compactRetention) {
            recordedRows++;
            lastResult = info;
        }
    }
}
