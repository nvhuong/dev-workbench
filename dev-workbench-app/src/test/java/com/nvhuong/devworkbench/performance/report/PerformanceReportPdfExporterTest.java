package com.nvhuong.devworkbench.performance.report;

import com.nvhuong.devworkbench.panel.performance.result.PerformanceTrendView;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;
import com.nvhuong.devworkbench.performance.core.run.PerformanceServiceResource;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleResult;
import com.nvhuong.devworkbench.script.model.TestResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class PerformanceReportPdfExporterTest {
    @Test
    public void generatesAccessibleHtmlAndRendersTheSameReportToPdf() throws Exception {
        var tables = httpTable();
        String html = PerformanceHtmlReportExporter.renderHtml(context(trendPoints(), List.of(
                failedSample("orders", 503, "upstream unavailable"),
                failedSample("orders", 503, "gateway timeout"),
                failedSample("inventory", 404, "item not found"))), tables);
        assertTrue(html.contains("<html xmlns=\"http://www.w3.org/1999/xhtml\""));
        assertTrue(html.contains("1. Summary"));
        assertTrue(html.contains("1.1 Response time"));
        assertTrue(html.contains("1.2 Throughput"));
        assertTrue(html.contains("2. Metrics for each request"));
        assertTrue(html.contains("3. Errors"));
        assertTrue(html.contains("<svg"));
        assertTrue(html.contains("req/s"));
        assertTrue(html.matches("(?s).*<text[^>]*>[0-9.]+ req/s</text>.*"));
        assertTrue(html.contains("GET /api/orders/7"));
        assertTrue(html.contains("Total CPU"));
        assertTrue(html.contains("6.0 cores"));
        assertTrue(html.contains("12.0 GB"));
        assertTrue(html.contains("API / request"));
        assertTrue(html.contains("Error code"));
        assertTrue(html.contains("Occurrences"));
        assertTrue(html.contains("orders"));
        assertTrue(html.contains("503"));
        assertTrue(html.contains("upstream unavailable"));
        assertTrue(html.contains("gateway timeout"));
        assertTrue(html.contains("<td>2</td>"));
        assertTrue(html.contains("inventory"));
        assertTrue(html.contains("404"));

        Path directory = Files.createTempDirectory("performance-html-report-");
        Path htmlFile = directory.resolve("index.html");
        Path pdfFile = directory.resolve("performance-report.pdf");
        try {
            PerformanceHtmlReportExporter.exportHtml(htmlFile, html);
            PerformanceHtmlReportExporter.exportPdf(pdfFile, html);
            assertEquals(Files.readString(htmlFile), html);
            assertTrue(Files.size(pdfFile) > 5_000);
            try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
                assertTrue(document.getNumberOfPages() >= 2);
                String text = new PDFTextStripper().getText(document);
                assertTrue(text.indexOf("Test setup") < text.indexOf("1. Summary"), text);
                assertTrue(text.indexOf("1. Summary") < text.indexOf("1.1 Response time"), text);
                assertTrue(text.contains("266.2 ms"), text);
                assertTrue(text.contains("Requests/s"), text);
                assertTrue(text.contains("Requests with slowest response times"), text);
                assertTrue(text.contains("Metrics for each request"), text);
                assertTrue(text.contains("Errors"), text);
                assertTrue(text.contains("GET /api/orders/7"), text);
                assertTrue(text.contains("orders"), text);
                assertTrue(text.contains("Total CPU"), text);
            }
        } finally {
            try (var files = Files.list(directory)) {
                files.forEach(file -> {
                    try { Files.deleteIfExists(file); } catch (Exception ignored) { }
                });
            }
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void includesWebSocketAndSseMetricsAndWrapsLongEndpoints() throws Exception {
        Path destination = Files.createTempFile("protocol-report-", ".pdf");
        try {
            List<PerformanceReportMarkdownBuilder.ReportTable> tables = new ArrayList<>();
            String[][] schemas = {PerformanceReportTableSchema.webSocketColumns(), PerformanceReportTableSchema.sseColumns()};
            String[] protocols = {"WebSocket", "SSE"};
            for (int i = 0; i < schemas.length; i++) {
                Object[] endpoint = new Object[schemas[i].length];
                Arrays.fill(endpoint, "42");
                endpoint[0] = protocols[i] + "/" + "very-long-path/".repeat(300) + "END-OF-PATH";
                endpoint[4] = "100.00%";
                Object[] total = endpoint.clone();
                total[0] = "Total";
                tables.add(new PerformanceReportMarkdownBuilder.ReportTable(protocols[i], Arrays.asList(schemas[i]),
                        List.of(endpoint, total)));
            }
            PerformanceReportPdfExporter.export(destination, "Performance test report", tables);
            try (PDDocument document = Loader.loadPDF(destination.toFile())) {
                String text = new PDFTextStripper().getText(document);
                assertTrue(text.contains("WebSocket"));
                assertTrue(text.contains("SSE"));
                assertTrue(text.replaceAll("\\s", "").contains("END-OF-PATH"));
                assertTrue(document.getNumberOfPages() >= 2);
            }
        } finally {
            Files.deleteIfExists(destination);
        }
    }

    @Test
    public void emptyReportDoesNotReplaceExistingFile() throws Exception {
        Path destination = Files.createTempFile("existing-report-", ".pdf");
        try {
            Files.writeString(destination, "existing report");
            expectThrows(IllegalArgumentException.class,
                    () -> PerformanceReportPdfExporter.export(destination, "Report", List.of()));
            assertEquals(Files.readString(destination), "existing report");
        } finally {
            Files.deleteIfExists(destination);
        }
    }

    private List<PerformanceReportMarkdownBuilder.ReportTable> httpTable() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int average = 80 + i * 37;
            rows.add(new Object[]{"GET /api/orders/" + i + " · Kiểm thử Kiểm tra hiệu suất", 128, 126, 2,
                    "98.44%", "0.71", "12.5", "48.0", average + " ms", (average - 35) + " ms",
                    (average + 190) + " ms", (average + 45) + " ms", (average + 85) + " ms", (average + 165) + " ms"});
        }
        rows.add(new Object[]{"Total", 1024, 1008, 16, "98.44%", "5.69", "100.0", "384.0",
                "266.2 ms", "42 ms", "986 ms", "415 ms", "532 ms", "781 ms"});
        return List.of(new PerformanceReportMarkdownBuilder.ReportTable("HTTP",
                Arrays.asList(PerformanceReportTableSchema.httpColumns()), rows));
    }

    private PerformanceReportPdfExporter.ReportContext context(List<PerformanceTrendView.TrendReportPoint> points) {
        return context(points, List.of());
    }

    private PerformanceReportPdfExporter.ReportContext context(List<PerformanceTrendView.TrendReportPoint> points,
                                                                 List<PerformanceSampleResult> samples) {
        return new PerformanceReportPdfExporter.ReportContext("Performance Test Report · Demo", null,
                "Staging", points, samples, "", List.of(new PerformanceServiceResource("orders", 3, 2, 4)));
    }

    private PerformanceSampleResult failedSample(String api, int code, String detail) {
        return PerformanceSampleResult.builder().apiId(api).apiName(api).responseCode(code)
                .successful(false).errorMsg(detail).testResults(List.of(new TestResult("HTTP status", false, detail))).build();
    }

    private List<PerformanceTrendView.TrendReportPoint> trendPoints() {
        List<PerformanceTrendView.TrendReportPoint> points = new ArrayList<>();
        long now = System.currentTimeMillis() - 120_000;
        for (int i = 0; i < 20; i++) {
            double average = 110 + i * 8;
            var metrics = new PerformanceTrendSnapshot.ProtocolWindowMetrics(100, 2, 2.0, 5.5,
                    average, 0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, average * 1.6);
            points.add(new PerformanceTrendView.TrendReportPoint(now + i * 6_000,
                    new PerformanceTrendSnapshot(10, 0, 0, metrics, metrics,
                            PerformanceTrendSnapshot.terminalIdle().webSocket(), PerformanceTrendSnapshot.terminalIdle().sse())));
        }
        return List.copyOf(points);
    }
}
