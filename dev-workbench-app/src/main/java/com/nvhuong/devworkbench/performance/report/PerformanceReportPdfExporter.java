package com.nvhuong.devworkbench.performance.report;

import com.nvhuong.devworkbench.panel.performance.result.PerformanceTrendView;
import com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlan;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleResult;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.nvhuong.devworkbench.performance.core.run.PerformanceServiceResource;

/** Compatibility facade for report callers. PDF rendering is performed from the same HTML users can open. */
@UtilityClass
public class PerformanceReportPdfExporter {
    public record ReportContext(String title,
                                PerformanceRunPlan plan,
                                String environment,
                                List<PerformanceTrendView.TrendReportPoint> trendPoints,
                                List<PerformanceSampleResult> samples,
                                String error,
                                List<PerformanceServiceResource> serviceResources) {
        public ReportContext {
            trendPoints = trendPoints == null ? List.of() : List.copyOf(trendPoints);
            samples = samples == null ? List.of() : List.copyOf(samples);
            serviceResources = PerformanceServiceResource.sanitize(serviceResources);
            error = error == null ? "" : error;
        }
    }

    public void export(Path destination, String title,
                       List<PerformanceReportMarkdownBuilder.ReportTable> tables) throws IOException {
        export(destination, new ReportContext(title, null, "", List.of(), List.of(), "", List.of()), tables);
    }

    public void export(Path destination, ReportContext context,
                       List<PerformanceReportMarkdownBuilder.ReportTable> tables) throws IOException {
        String html = PerformanceHtmlReportExporter.renderHtml(context, tables);
        PerformanceHtmlReportExporter.exportPdf(destination, html);
    }

    /** Small public sample for manual report preview. */
    public void exportDemo(Path destination) throws IOException {
        List<Object[]> rows = java.util.Collections.singletonList(
                new Object[]{"GET /api/orders", 1024, 1008, 16, "98.44%", "5.69", "100.0", "384.0",
                        "266.2 ms", "42 ms", "986 ms", "415 ms", "532 ms", "781 ms"});
        var table = new PerformanceReportMarkdownBuilder.ReportTable("HTTP",
                List.of("Request", "Total", "Success", "Fail", "Success rate", "Throughput", "Sent KB/s",
                        "Received KB/s", "Average", "Min", "Max", "P90", "P95", "P99"), rows);
        export(destination, "Performance Test Report · Demo", List.of(table));
    }
}
