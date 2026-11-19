package com.nvhuong.devworkbench.performance.report;

import com.nvhuong.devworkbench.panel.performance.result.PerformanceTrendView;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;
import com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlan;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleResult;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds the browsable report and renders the same HTML document to PDF. */
@UtilityClass
public class PerformanceHtmlReportExporter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter START_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    public String renderHtml(PerformanceReportPdfExporter.ReportContext context,
                             List<PerformanceReportMarkdownBuilder.ReportTable> tables) {
        List<PerformanceReportMarkdownBuilder.ReportTable> populated = tables == null ? List.of()
                : tables.stream().filter(table -> table != null && table.rows() != null && !table.rows().isEmpty()).toList();
        if (populated.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_MARKDOWN_EMPTY));
        }
        var safe = context == null ? new PerformanceReportPdfExporter.ReportContext("Performance Test Report", null,
                "", List.of(), List.of(), "", List.of()) : context;
        StringBuilder html = new StringBuilder(24_000);
        html.append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"").append(I18nUtil.currentLocale().toLanguageTag()).append("\"><head>")
                .append("<meta charset=\"utf-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>")
                .append("<title>").append(escape(safe.title())).append("</title><style>").append(css()).append("</style></head><body>")
                .append("<main class=\"report\"><header class=\"hero\"><div class=\"brand\">DEV WORKBENCH <span>PERFORMANCE</span></div>")
                .append("<h1>").append(escape(safe.title())).append("</h1><p class=\"muted\">")
                .append(escape(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_GENERATED))).append(" · ")
                .append(escape(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(java.time.ZonedDateTime.now())))
                .append("</p></header>");
        appendSetup(html, safe);
        appendSummary(html, populated);
        appendCharts(html, safe.trendPoints(), safe.samples());
        appendSlowest(html, populated);
        html.append("<section><h2>2. Metrics for each request</h2>");
        for (var table : populated) appendTable(html, table);
        html.append("</section>");
        appendErrors(html, safe.error(), populated, safe.samples());
        html.append("<footer>Dev Workbench · Performance Test Report</footer></main></body></html>");
        return html.toString();
    }

    public void exportHtml(Path destination, String html) throws IOException {
        writeAtomically(destination, html);
    }

    public void exportPdf(Path destination, String html) throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temp = Files.createTempFile(absolute.getParent(), ".performance-report-", ".pdf");
        try (OutputStream output = Files.newOutputStream(temp)) {
            PdfRendererBuilder builder = new PdfRendererBuilder()
                    .useSVGDrawer(new BatikSVGDrawer())
                    .withHtmlContent(html, absolute.getParent().toUri().toString())
                    .toStream(output)
                    .withProducer("Dev Workbench Performance Reports");
            builder.useFont(() -> font("/fonts/DejaVuSans.ttf"), "Dev Workbench");
            builder.useFont(() -> font("/fonts/DroidSansFallback.ttf"), "Dev Workbench CJK");
            builder.run();
        }
        try {
            Files.move(temp, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private InputStream font(String resource) {
        InputStream input = PerformanceHtmlReportExporter.class.getResourceAsStream(resource);
        if (input == null) throw new IllegalStateException("Missing report font " + resource);
        return input;
    }

    private void writeAtomically(Path destination, String content) throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temp = Files.createTempFile(absolute.getParent(), ".performance-report-", ".html");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void appendSetup(StringBuilder html, PerformanceReportPdfExporter.ReportContext context) {
        html.append("<section><h2>Test setup</h2>");
        String users = "—", duration = "—", load = "—", environment = context.environment().isBlank() ? "—" : context.environment();
        if (context.plan() != null) {
            List<ThreadGroupData> groups = new ArrayList<>();
            collectThreadGroups(context.plan().getTestPlan() == null ? null : context.plan().getTestPlan().getRoot(), groups);
            if (!groups.isEmpty()) {
                ThreadGroupData group = groups.getFirst();
                users = String.valueOf(maxUsers(group));
                duration = durationSeconds(group) + " s";
                load = group.threadMode.name().replace('_', ' ');
            }
        }
        html.append("<div class=\"cards setup\">");
        card(html, "Virtual users", users);
        card(html, "Duration", duration);
        card(html, "Load profile", load);
        html.append("</div><div class=\"cards setup\">");
        card(html, "Environment", environment);
        card(html, "Start time", context.plan() == null ? "—" : formatStartTime(context.plan().getGeneratedAt()));
        card(html, "Run source", context.plan() == null ? "Local" : context.plan().getGeneratedBy());
        if (!context.serviceResources().isEmpty()) {
            html.append("</div><h3>Service resources</h3><table><thead><tr><th>Service</th><th>Instances</th><th>CPU / instance</th><th>Total CPU</th><th>RAM / instance</th><th>Total RAM</th></tr></thead><tbody>");
            for (var resource : context.serviceResources()) {
                html.append("<tr><td>").append(escape(resource.getServiceName())).append("</td><td>")
                        .append(resource.getInstances()).append("</td><td>").append(resource.getCpuCores())
                        .append(" cores</td><td>").append(resource.getCpuCores() * resource.getInstances())
                        .append(" cores</td><td>").append(resource.getMemoryGb()).append(" GB</td><td>")
                        .append(resource.getMemoryGb() * resource.getInstances()).append(" GB</td></tr>");
            }
            html.append("</tbody></table><div class=\"cards setup\">");
        }
        html.append("</div></section>");
    }

    private String formatStartTime(String value) {
        try {
            return START_TIME.format(Instant.parse(value));
        } catch (RuntimeException ignored) {
            return value == null ? "—" : value;
        }
    }

    private void appendSummary(StringBuilder html, List<PerformanceReportMarkdownBuilder.ReportTable> tables) {
        var http = tables.stream().filter(table -> table.columns().size() == 14).findFirst().orElse(null);
        html.append("<section><h2>1. Summary</h2><div class=\"cards summary\">");
        if (http != null) {
            Object[] total = http.rows().getLast();
            card(html, "Total requests", cell(total, 1));
            card(html, "Throughput", cell(total, 5) + " requests/s");
            card(html, "Average response time", cell(total, 8));
            double totalRequests = parse(total[1]);
            double failures = parse(total[3]);
            card(html, "Error rate", totalRequests <= 0 ? "0.00%"
                    : String.format(Locale.ROOT, "%.2f%%", failures * 100d / totalRequests));
        } else html.append("<p class=\"muted\">Summary metrics are unavailable for this protocol.</p>");
        html.append("</div></section>");
    }

    private void appendCharts(StringBuilder html, List<PerformanceTrendView.TrendReportPoint> trendPoints,
                              List<PerformanceSampleResult> samples) {
        boolean fromTrend = hasChartData(trendPoints);
        List<PerformanceTrendView.TrendReportPoint> points = fromTrend
                ? trendPoints : chartPointsFromSamples(samples);
        String source = fromTrend ? "Sampled during the run." : "Reconstructed from retained request results.";
        html.append("<section><div class=\"chart-block\"><h2>1.1 Response time</h2><p class=\"muted\">Average and maximum response time over the test duration. ")
                .append(source).append("</p>");
        if (points.isEmpty()) {
            html.append("<div class=\"empty\">Chart data is unavailable.</div>");
        } else {
            chart(html, points, "Response time", "ms", false);
        }
        html.append("</div><div class=\"chart-block\"><h2>1.2 Throughput</h2><p class=\"muted\">Requests completed per second. ")
                .append(source).append("</p>");
        if (points.isEmpty()) html.append("<div class=\"empty\">Chart data is unavailable.</div>");
        else chart(html, points, "Throughput", "req/s", true);
        html.append("</div></section>");
    }

    private boolean hasChartData(List<PerformanceTrendView.TrendReportPoint> points) {
        if (points == null || points.isEmpty()) return false;
        // PerformanceTrendPanel.reportPoints() does not carry per-window sample counts (they are 0), so a positive
        // sample count must not be required; any positive response time or throughput value is chart data.
        return points.stream().filter(point -> point != null && point.snapshot() != null)
                .map(point -> chartMetrics(point.snapshot()))
                .anyMatch(metric -> metric.samples() > 0
                        || positive(metric.avgDurationMs()) || positive(metric.maxDurationMs())
                        || positive(metric.sampleRate()));
    }

    /** HTTP window metrics for charts; falls back to overview when a producer only filled that slot. */
    private static PerformanceTrendSnapshot.ProtocolWindowMetrics chartMetrics(PerformanceTrendSnapshot snapshot) {
        PerformanceTrendSnapshot.ProtocolWindowMetrics http = snapshot.http();
        if (hasValues(http) || snapshot.overview() == null) return http;
        return hasValues(snapshot.overview()) ? snapshot.overview() : http;
    }

    private static boolean hasValues(PerformanceTrendSnapshot.ProtocolWindowMetrics metric) {
        return metric != null && (metric.samples() > 0 || positive(metric.avgDurationMs())
                || positive(metric.maxDurationMs()) || positive(metric.sampleRate()));
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0;
    }

    private List<PerformanceTrendView.TrendReportPoint> chartPointsFromSamples(List<PerformanceSampleResult> samples) {
        if (samples == null || samples.isEmpty()) return List.of();
        List<PerformanceSampleResult> valid = samples.stream()
                .filter(sample -> sample != null && sample.getStartTimeMs() > 0 && sample.getElapsedTimeMs() >= 0)
                .sorted(java.util.Comparator.comparingLong(PerformanceSampleResult::getStartTimeMs))
                .toList();
        if (valid.isEmpty()) return List.of();

        long start = valid.getFirst().getStartTimeMs();
        long end = valid.stream().mapToLong(sample -> Math.max(sample.getEndTimeMs(),
                sample.getStartTimeMs() + sample.getElapsedTimeMs())).max().orElse(start);
        long duration = Math.max(1L, end - start);
        long bucketWidthMs = Math.max(1L, (duration + 59L) / 60L);
        Map<Long, ChartBucket> buckets = new LinkedHashMap<>();
        for (PerformanceSampleResult sample : valid) {
            long index = Math.max(0L, (sample.getStartTimeMs() - start) / bucketWidthMs);
            buckets.computeIfAbsent(index, ignored -> new ChartBucket()).add(sample);
        }

        List<PerformanceTrendView.TrendReportPoint> points = new ArrayList<>(buckets.size());
        for (Map.Entry<Long, ChartBucket> entry : buckets.entrySet()) {
            long bucketStart = start + entry.getKey() * bucketWidthMs;
            long bucketEnd = Math.min(end, bucketStart + bucketWidthMs);
            long actualWidthMs = Math.max(1L, bucketEnd - bucketStart);
            ChartBucket bucket = entry.getValue();
            double averageMs = bucket.totalElapsedMs / (double) bucket.count;
            double throughput = bucket.count * 1000d / actualWidthMs;
            double failurePercent = bucket.count == 0 ? 0 : bucket.failures * 100d / bucket.count;
            var http = new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    bucket.count, bucket.failures, failurePercent, throughput, averageMs,
                    0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, bucket.maximumElapsedMs);
            var idle = PerformanceTrendSnapshot.terminalIdle();
            // Record order is (overview, http, webSocket, sse); the charts read http().
            points.add(new PerformanceTrendView.TrendReportPoint(bucketStart,
                    new PerformanceTrendSnapshot(0, 0, 0, http, http, idle.webSocket(), idle.sse())));
        }
        return List.copyOf(points);
    }

    private static final class ChartBucket {
        private int count;
        private int failures;
        private long totalElapsedMs;
        private long maximumElapsedMs;

        private void add(PerformanceSampleResult sample) {
            count++;
            if (!sample.isSuccessful()) failures++;
            long elapsed = Math.max(0L, sample.getElapsedTimeMs());
            totalElapsedMs += elapsed;
            maximumElapsedMs = Math.max(maximumElapsedMs, elapsed);
        }
    }

    private void chart(StringBuilder html, List<PerformanceTrendView.TrendReportPoint> points,
                       String title, String unit, boolean throughput) {
        final double left = 70, right = 910, top = 22, bottom = 252, width = right - left, height = bottom - top;
        double maximum = 0;
        for (var point : points) {
            PerformanceTrendSnapshot.ProtocolWindowMetrics metric = chartMetrics(point.snapshot());
            double avg = throughput ? metric.sampleRate() : metric.avgDurationMs();
            double max = throughput ? avg : metric.maxDurationMs();
            if (Double.isFinite(avg)) maximum = Math.max(maximum, avg);
            if (Double.isFinite(max)) maximum = Math.max(maximum, max);
        }
        maximum = maximum <= 0 ? 1 : maximum * 1.12;
        html.append("<figure class=\"chart\"><figcaption>").append(escape(title))
                .append("</figcaption><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 960 310\" role=\"img\" aria-label=\"")
                .append(escape(title)).append(" chart\">");
        for (int i = 0; i <= 4; i++) {
            double fraction = i / 4d, y = bottom - fraction * height;
            html.append("<line x1=\"").append(left).append("\" x2=\"").append(right).append("\" y1=\"").append(y).append("\" y2=\"").append(y)
                    .append("\" stroke=\"#e5ebf3\" stroke-width=\"1\"/>")
                    .append("<text x=\"8\" y=\"").append(y + 4).append("\" fill=\"#64758c\" font-size=\"10\">")
                    .append(String.format(Locale.ROOT, "%.2f", maximum * fraction)).append(' ').append(unit).append("</text>");
        }
        html.append("<line x1=\"").append(left).append("\" x2=\"").append(right).append("\" y1=\"").append(bottom).append("\" y2=\"").append(bottom)
                .append("\" stroke=\"#93a2b7\" stroke-width=\"1\"/>");
        appendSeries(html, points, left, bottom, width, height, maximum, throughput, false);
        if (!throughput) appendSeries(html, points, left, bottom, width, height, maximum, false, true);
        for (int i = 0; i <= 4; i++) {
            int index = Math.min(points.size() - 1, i * Math.max(0, points.size() - 1) / 4);
            double x = left + (points.size() <= 1 ? 0 : width * index / (points.size() - 1));
            html.append("<text text-anchor=\"middle\" x=\"").append(x).append("\" y=\"278\" fill=\"#64758c\" font-size=\"10\">")
                    .append(TIME.format(Instant.ofEpochMilli(points.get(index).timestampMs()))).append("</text>");
        }
        html.append("</svg><div class=\"legend\"><span><i class=\"avg\"></i>").append(throughput ? "Requests/s" : "Average (ms)").append("</span>");
        if (!throughput) html.append("<span><i class=\"max\"></i>Maximum (ms)</span>");
        html.append("</div></figure>");
    }

    private void appendSeries(StringBuilder html, List<PerformanceTrendView.TrendReportPoint> points,
                              double left, double bottom, double width, double height, double maximum,
                              boolean throughput, boolean maxSeries) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            PerformanceTrendSnapshot.ProtocolWindowMetrics metric = chartMetrics(points.get(i).snapshot());
            double value = throughput ? metric.sampleRate() : maxSeries ? metric.maxDurationMs() : metric.avgDurationMs();
            if (!Double.isFinite(value)) continue;
            double x = left + (points.size() <= 1 ? 0 : width * i / (points.size() - 1));
            double y = bottom - Math.max(0, Math.min(1, value / maximum)) * height;
            path.append(path.isEmpty() ? "M" : " L").append(String.format(Locale.ROOT, "%.1f %.1f", x, y));
        }
        html.append("<path fill=\"none\" stroke=\"").append(maxSeries ? "#9b67c9" : "#2574d9")
                .append("\" stroke-width=\"").append(maxSeries ? "2.5" : "3").append("\" d=\"").append(path).append("\"/>");
    }

    private void appendSlowest(StringBuilder html, List<PerformanceReportMarkdownBuilder.ReportTable> tables) {
        var http = tables.stream().filter(table -> table.columns().size() == 14).findFirst().orElse(null);
        if (http == null || http.rows().size() <= 1) return;
        html.append("<section><h2>1.3 Requests with slowest response times</h2>");
        appendTable(html, new PerformanceReportMarkdownBuilder.ReportTable("Requests with slowest response times",
                List.of("Request", "Average (ms)", "90th (ms)", "95th (ms)", "99th (ms)", "Min (ms)", "Max (ms)"),
                http.rows().subList(0, http.rows().size() - 1).stream()
                        .sorted((a, b) -> Double.compare(durationMs(b[8]), durationMs(a[8])))
                        .limit(5).map(row -> new Object[]{row[0], row[8], row[11], row[12], row[13], row[9], row[10]}).toList()));
        html.append("</section>");
    }

    private void appendTable(StringBuilder html, PerformanceReportMarkdownBuilder.ReportTable table) {
        html.append("<h3>").append(escape(table.title())).append("</h3><div class=\"table-wrap\"><table><thead><tr>");
        for (String column : table.columns()) html.append("<th>").append(escape(column)).append("</th>");
        html.append("</tr></thead><tbody>");
        for (Object[] row : table.rows()) {
            html.append("<tr>");
            for (Object value : row) html.append("<td>").append(escape(value == null ? "" : value.toString())).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table></div>");
    }

    private void appendErrors(StringBuilder html, String error,
                              List<PerformanceReportMarkdownBuilder.ReportTable> tables,
                              List<PerformanceSampleResult> samples) {
        html.append("<section><h2>3. Errors</h2>");
        Map<ErrorGroupKey, ErrorGroup> groups = new LinkedHashMap<>();
        if (samples != null) {
            for (PerformanceSampleResult sample : samples) {
                if (sample == null || sample.isSuccessful() && !sample.isExecutionFailed() && !sample.isInterrupted()) continue;
                String api = firstNonBlank(sample.getApiName(), sample.getApiId(),
                        message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_UNKNOWN_API));
                String code = errorCode(sample);
                ErrorGroup group = groups.computeIfAbsent(new ErrorGroupKey(api, code), ignored -> new ErrorGroup());
                group.occurrences++;
                String detail = errorDetail(sample);
                if (!detail.isBlank()) group.details.add(detail);
            }
        }
        if (!groups.isEmpty()) {
            html.append("<div class=\"table-wrap errors\"><table><thead><tr>")
                    .append("<th>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_API))).append("</th>")
                    .append("<th>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_CODE))).append("</th>")
                    .append("<th>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_DETAILS))).append("</th>")
                    .append("<th>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_OCCURRENCES))).append("</th>")
                    .append("</tr></thead><tbody>");
            for (Map.Entry<ErrorGroupKey, ErrorGroup> entry : groups.entrySet()) {
                html.append("<tr><td>").append(escape(entry.getKey().api())).append("</td><td><code>")
                        .append(escape(entry.getKey().code())).append("</code></td><td>")
                        .append(escape(String.join("; ", entry.getValue().details))).append("</td><td>")
                        .append(entry.getValue().occurrences).append("</td></tr>");
            }
            html.append("</tbody></table></div></section>");
            return;
        }
        double failures = tables.stream().filter(table -> !table.rows().isEmpty())
                .mapToDouble(table -> parse(table.rows().getLast()[Math.min(3, table.rows().getLast().length - 1)]))
                .sum();
        html.append("<div class=\"empty\">");
        if (error != null && !error.isBlank()) {
            html.append("<strong>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_RUN)))
                    .append("</strong><p>").append(escape(error)).append("</p>");
        } else if (failures > 0) {
            html.append("<strong>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_DATA_UNAVAILABLE)))
                    .append("</strong><p>").append(escape(I18nUtil.getMessage(
                            MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_FAILURES_RECORDED,
                            String.format(Locale.ROOT, "%.0f", failures)))).append("</p>");
        } else html.append("<strong>").append(escape(message(MessageKeys.PERFORMANCE_REPORT_PDF_NO_ERRORS))).append("</strong>");
        html.append("</div></section>");
    }

    private String errorCode(PerformanceSampleResult sample) {
        if (sample.getResponseCode() > 0) return Integer.toString(sample.getResponseCode());
        if (sample.isInterrupted()) return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_INTERRUPTED);
        if (sample.getTestResults() != null && sample.getTestResults().stream().anyMatch(test -> test != null && !test.passed)) {
            return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_ASSERTION);
        }
        if (sample.isExecutionFailed() || sample.getErrorMsg() != null && !sample.getErrorMsg().isBlank()) {
            return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_TRANSPORT);
        }
        return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_UNKNOWN_CODE);
    }

    private String errorDetail(PerformanceSampleResult sample) {
        if (sample.getErrorMsg() != null && !sample.getErrorMsg().isBlank()) return sample.getErrorMsg();
        List<String> failedAssertions = new ArrayList<>();
        if (sample.getTestResults() != null) {
            for (TestResult result : sample.getTestResults()) {
                if (result == null || result.passed) continue;
                failedAssertions.add(firstNonBlank(result.name, message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_ASSERTION))
                        + (result.message == null || result.message.isBlank() ? "" : ": " + result.message));
            }
        }
        if (!failedAssertions.isEmpty()) return String.join("; ", failedAssertions);
        if (sample.isInterrupted()) return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_INTERRUPTED);
        return message(MessageKeys.PERFORMANCE_REPORT_PDF_ERROR_FAILED);
    }

    private String firstNonBlank(String first, String second, String fallback) {
        return first != null && !first.isBlank() ? first : second != null && !second.isBlank() ? second : fallback;
    }

    private String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String message(String key) { return I18nUtil.getMessage(key); }

    private record ErrorGroupKey(String api, String code) { }

    private static final class ErrorGroup {
        private int occurrences;
        private final Set<String> details = new LinkedHashSet<>();
    }

    private void collectThreadGroups(com.nvhuong.devworkbench.performance.core.plan.PerformanceCorePlanNode node,
                                    List<ThreadGroupData> target) {
        if (node == null) return;
        if (node.getThreadGroupData() != null) target.add(node.getThreadGroupData());
        for (var child : node.getChildren()) collectThreadGroups(child, target);
    }

    private int maxUsers(ThreadGroupData group) {
        return switch (group.threadMode) {
            case RAMP_UP -> group.rampUpEndThreads;
            case SPIKE -> group.spikeMaxThreads;
            case STAIRS -> group.stairsEndThreads;
            default -> group.numThreads;
        };
    }

    private int durationSeconds(ThreadGroupData group) {
        return switch (group.threadMode) {
            case RAMP_UP -> group.rampUpDuration;
            case SPIKE -> group.spikeDuration;
            case STAIRS -> group.stairsDuration;
            default -> group.duration;
        };
    }

    /**
     * Converts TimeDisplayUtil.formatElapsedTime output ("999 ms", "1.50 s" / "1,50 s", "1m 5s") back to milliseconds;
     * stripping non-digits turned "1m 5s" into 15 and ranked slow requests below fast ones.
     */
    static double durationMs(Object value) {
        String text = value == null ? "" : value.toString().trim();
        java.util.regex.Matcher minutes = java.util.regex.Pattern.compile("^(\\d+)m\\s*(\\d+)s$").matcher(text);
        if (minutes.matches()) {
            return Long.parseLong(minutes.group(1)) * 60_000d + Long.parseLong(minutes.group(2)) * 1_000d;
        }
        try {
            if (text.endsWith("ms")) {
                return Double.parseDouble(text.substring(0, text.length() - 2).trim().replace(',', '.'));
            }
            if (text.endsWith("s")) {
                return Double.parseDouble(text.substring(0, text.length() - 1).trim().replace(',', '.')) * 1_000d;
            }
        } catch (NumberFormatException ignored) {
            // fall back to plain number parsing
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String cell(Object[] row, int index) { return row.length > index && row[index] != null ? row[index].toString() : "—"; }
    private double parse(Object value) { try { return Double.parseDouble(String.valueOf(value).replaceAll("[^0-9.,-]", "").replace(',', '.')); } catch (Exception ignored) { return 0; } }
    private void card(StringBuilder html, String label, String value) { html.append("<article><span>").append(escape(label)).append("</span><strong>").append(escape(value)).append("</strong></article>"); }
    private String escape(String value) {
        if (value == null) return "";
        return stripInvalidXmlChars(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * The PDF renderer parses the report as XHTML; one control character (ANSI colour codes, NUL...) from an error
     * message made the whole PDF export fail with "An invalid XML character was found".
     */
    static String stripInvalidXmlChars(String value) {
        StringBuilder cleaned = null;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            int length = Character.charCount(codePoint);
            boolean valid = codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                    || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                    || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                    || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
            if (!valid && cleaned == null) {
                cleaned = new StringBuilder(value.length()).append(value, 0, i);
            }
            if (valid && cleaned != null) {
                cleaned.appendCodePoint(codePoint);
            }
            i += length;
        }
        return cleaned == null ? value : cleaned.toString();
    }

    private String css() {
        return "@page{size:A4;margin:15mm 12mm}*{box-sizing:border-box}body{margin:0;color:#172b4d;font:10px 'Dev Workbench','Dev Workbench CJK',Arial,sans-serif;background:#f3f6fb}.report{max-width:1180px;margin:auto;padding:22px;background:white}.hero{padding:22px 25px;background:#12243d;color:white;border-radius:10px;margin-bottom:20px}.brand{font-weight:700;letter-spacing:1.4px;font-size:10px;color:#b9d2ff}.brand span{color:#45d6b0}h1{font-size:24px;margin:14px 0 6px}h2{font-size:16px;margin:23px 0 10px;color:#13294b}h3{font-size:12px;margin:16px 0 8px;color:#304969}.muted{color:#697b94}.cards{display:table;width:100%;table-layout:fixed;border-spacing:7px}.cards article{display:table-cell;width:33.33%;padding:11px 12px;background:#f1f5fb;border:1px solid #e1e8f2;border-radius:8px;min-height:54px;page-break-inside:avoid}.summary article{width:25%}article span{display:block;color:#63758e;font-size:9px;margin-bottom:7px}article strong{font-size:13px;color:#162e50;word-wrap:break-word}.chart-block{page-break-inside:avoid}.chart{margin:8px 0 20px;padding:12px;border:1px solid #e0e7f0;border-radius:9px;page-break-inside:avoid}.chart figcaption{font-weight:700;font-size:12px;margin:0 0 7px}.chart svg{display:block;width:100%;height:auto}.grid{stroke:#e5ebf3;stroke-width:1}.axis{stroke:#93a2b7;stroke-width:1}.tick{font-size:10px;fill:#64758c}.series-avg{fill:none;stroke:#2574d9;stroke-width:3}.series-max{fill:none;stroke:#9b67c9;stroke-width:2.5}.legend{color:#64758c;font-size:9px;margin-top:5px}.legend span{margin-right:18px}.legend i{display:inline-block;width:14px;height:3px;background:#2574d9;margin:0 6px 3px 0}.legend .max{background:#9b67c9}.table-wrap{width:100%}table{border-collapse:collapse;width:100%;font-size:8px;margin:8px 0 18px;table-layout:auto}th{background:#eaf0f8;color:#3b4c64;text-align:left;font-weight:700}th,td{padding:6px 5px;border-bottom:1px solid #e4eaf2;vertical-align:top;word-wrap:break-word}tbody tr:nth-child(even){background:#fafbfd}.empty{background:#f6f8fb;border-radius:8px;padding:22px;text-align:center;color:#65758a}.empty p{margin:8px 0 0}footer{border-top:1px solid #e5eaf1;padding-top:12px;margin-top:24px;color:#8290a4;font-size:8px}section{page-break-inside:auto}tr{page-break-inside:avoid} ";
    }
}
