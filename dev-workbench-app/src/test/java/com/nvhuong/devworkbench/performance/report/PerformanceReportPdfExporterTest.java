package com.nvhuong.devworkbench.performance.report;

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

    /**
     * PerformanceTrendPanel.reportPoints() builds window metrics with a sample count of 0; the exporter required a
     * positive count, ignored the sampled trend and showed "Chart data is unavailable" whenever no request results
     * were retained.
     */
    @Test
    public void chartsUseTrendPointsAsProducedByTheTrendPanel() {
        List<TrendReportPoint> points = new ArrayList<>();
        long start = System.currentTimeMillis() - 60_000;
        for (int i = 0; i < 12; i++) {
            double average = 120 + i * 10;
            // Same shape as PerformanceTrendPanel.reportPoints(): samples/failures are not tracked there.
            var metrics = new PerformanceTrendSnapshot.ProtocolWindowMetrics(0, 0, 1.5, 8.0 + i,
                    average, 0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, average * 2);
            points.add(new TrendReportPoint(start + i * 5_000,
                    new PerformanceTrendSnapshot(10, 0, 0, metrics, PerformanceTrendSnapshot.terminalIdle().http(),
                            PerformanceTrendSnapshot.terminalIdle().webSocket(), PerformanceTrendSnapshot.terminalIdle().sse())));
        }

        String html = PerformanceHtmlReportExporter.renderHtml(context(points, List.of()), httpTable());

        assertFalse(html.contains("Chart data is unavailable"), "sampled trend data must be charted");
        assertTrue(html.contains("Sampled during the run."), html);
        assertTrue(html.matches("(?s).*<path fill=\"none\" stroke=\"#2574d9\"[^>]* d=\"M[0-9.]+ [0-9.]+ L.*"),
                "the response time series must have plotted points");
    }

    /**
     * Durations are formatted as "999 ms", "1.50 s" or "1m 5s"; the slowest-requests ranking stripped non-digits, so
     * "1m 5s" became 15 and "1.50 s" 1.5 and requests taking seconds were ranked below sub-second ones.
     */
    @Test
    public void slowestRequestsAreRankedAcrossDurationUnits() {
        List<Object[]> rows = new ArrayList<>();
        String[] averages = {"120 ms", "1.50 s", "900 ms", "1m 5s", "2.25 s"};
        String[] names = {"GET /fast", "GET /second-and-half", "GET /almost-second", "GET /minute", "GET /two-seconds"};
        for (int i = 0; i < averages.length; i++) {
            rows.add(new Object[]{names[i], 10, 10, 0, "100.00%", "1.00", "1.00", "1.00",
                    averages[i], "1 ms", averages[i], averages[i], averages[i], averages[i]});
        }
        rows.add(new Object[]{"Total", 50, 50, 0, "100.00%", "5.00", "5.00", "5.00",
                "13.95 s", "1 ms", "1m 5s", "2.25 s", "1m 5s", "1m 5s"});
        var table = new PerformanceReportMarkdownBuilder.ReportTable("HTTP",
                Arrays.asList(PerformanceReportTableSchema.httpColumns()), rows);

        String html = PerformanceHtmlReportExporter.renderHtml(context(List.of()), List.of(table));

        String slowest = html.substring(html.indexOf("1.3 Requests with slowest response times"),
                html.indexOf("2. Metrics for each request"));
        int minute = slowest.indexOf("GET /minute");
        int twoSeconds = slowest.indexOf("GET /two-seconds");
        int secondAndHalf = slowest.indexOf("GET /second-and-half");
        int almostSecond = slowest.indexOf("GET /almost-second");
        int fast = slowest.indexOf("GET /fast");
        assertTrue(minute >= 0 && minute < twoSeconds && twoSeconds < secondAndHalf
                && secondAndHalf < almostSecond && almostSecond < fast, slowest);
    }

    /** Without trend data the charts are rebuilt from retained samples; those points also put data in the wrong slot. */
    @Test
    public void chartsRebuiltFromSamplesHavePlottedSeries() {
        List<PerformanceSampleResult> samples = new ArrayList<>();
        long start = System.currentTimeMillis() - 30_000;
        for (int i = 0; i < 40; i++) {
            samples.add(PerformanceSampleResult.builder().apiId("orders").apiName("orders").responseCode(200)
                    .successful(true).startTimeMs(start + i * 500L).endTimeMs(start + i * 500L + 80 + i)
                    .elapsedTimeMs(80 + i).build());
        }

        String html = PerformanceHtmlReportExporter.renderHtml(context(List.of(), samples), httpTable());

        assertTrue(html.contains("Reconstructed from retained request results."), html);
        assertTrue(html.matches("(?s).*<path fill=\"none\" stroke=\"#2574d9\"[^>]* d=\"M[0-9.]+ [0-9.]+ L.*"),
                "the response time series rebuilt from samples must have plotted points");
    }

    /** Error messages may carry control characters (ANSI colours, NUL); they made the XHTML invalid for the PDF renderer. */
    @Test
    public void pdfExportSurvivesControlCharactersInErrorMessages() throws Exception {
        String html = PerformanceHtmlReportExporter.renderHtml(context(trendPoints(), List.of(
                failedSample("orders", 0, "\u001b[31mConnection reset\u001b[0m\u0000 by peer\u000b"))), httpTable());
        Path pdfFile = Files.createTempFile("performance-report-control-", ".pdf");
        try {
            PerformanceHtmlReportExporter.exportPdf(pdfFile, html);
            try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
                String text = new PDFTextStripper().getText(document);
                assertTrue(text.contains("Connection reset"), text);
            }
        } finally {
            Files.deleteIfExists(pdfFile);
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

    private PerformanceReportPdfExporter.ReportContext context(List<TrendReportPoint> points) {
        return context(points, List.of());
    }

    private PerformanceReportPdfExporter.ReportContext context(List<TrendReportPoint> points,
                                                                 List<PerformanceSampleResult> samples) {
        return new PerformanceReportPdfExporter.ReportContext("Performance Test Report · Demo", null,
                "Staging", points, samples, "", List.of(new PerformanceServiceResource("orders", 3, 2, 4)));
    }

    private PerformanceSampleResult failedSample(String api, int code, String detail) {
        return PerformanceSampleResult.builder().apiId(api).apiName(api).responseCode(code)
                .successful(false).errorMsg(detail).testResults(List.of(new TestResult("HTTP status", false, detail))).build();
    }

    private List<TrendReportPoint> trendPoints() {
        List<TrendReportPoint> points = new ArrayList<>();
        long now = System.currentTimeMillis() - 120_000;
        for (int i = 0; i < 20; i++) {
            double average = 110 + i * 8;
            var metrics = new PerformanceTrendSnapshot.ProtocolWindowMetrics(100, 2, 2.0, 5.5,
                    average, 0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, average * 1.6);
            points.add(new TrendReportPoint(now + i * 6_000,
                    new PerformanceTrendSnapshot(10, 0, 0, metrics, metrics,
                            PerformanceTrendSnapshot.terminalIdle().webSocket(), PerformanceTrendSnapshot.terminalIdle().sse())));
        }
        return List.copyOf(points);
    }
}
