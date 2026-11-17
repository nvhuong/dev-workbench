package com.laker.postman.performance.report;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Date;
import java.util.Comparator;
import java.util.Locale;
import java.util.LinkedHashMap;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.panel.performance.result.PerformanceTrendView;
import com.laker.postman.performance.model.PerformanceSampleResult;

/** Portable, selectable-text report. All input values are an immutable UI snapshot. */
@UtilityClass
public class PerformanceReportPdfExporter {
    public record ReportContext(String title,
                                PerformanceRunPlan plan,
                                String environment,
                                List<PerformanceTrendView.TrendReportPoint> trendPoints,
                                List<PerformanceSampleResult> samples,
                                String error) {
        public ReportContext {
            trendPoints = trendPoints == null ? List.of() : List.copyOf(trendPoints);
            samples = samples == null ? List.of() : List.copyOf(samples);
            error = error == null ? "" : error;
        }
    }

    public void exportDemo(Path destination) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            int avg = 80 + index * 37;
            rows.add(new Object[]{"GET /api/orders/" + index, 128, 126, 2, "98.44%", "0.71", "12.5", "48.0",
                    avg + " ms", (avg - 35) + " ms", (avg + 190) + " ms", (avg + 45) + " ms",
                    (avg + 85) + " ms", (avg + 165) + " ms"});
        }
        rows.add(new Object[]{"Total", 1024, 1008, 16, "98.44%", "5.69", "100.0", "384.0",
                "218 ms", "42 ms", "986 ms", "415 ms", "532 ms", "781 ms"});
        var table = new PerformanceReportMarkdownBuilder.ReportTable("HTTP",
                Arrays.asList(PerformanceReportTableSchema.httpColumns()), rows);
        List<PerformanceTrendView.TrendReportPoint> points = new ArrayList<>();
        long now = System.currentTimeMillis() - 180_000;
        for (int index = 0; index < 22; index++) {
            double average = 90 + 60 * Math.sin(index * 0.48) + (index > 15 ? index * 7 : 0);
            double qps = index < 3 ? 0.15 : 0.42 + 0.18 * Math.sin(index * 0.35);
            double max = average * (1.5 + (index % 4) * 0.3);
            var http = new PerformanceTrendSnapshot.ProtocolWindowMetrics(42, 1, 2.34, qps, average,
                    0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, max);
            var idle = PerformanceTrendSnapshot.terminalIdle();
            points.add(new PerformanceTrendView.TrendReportPoint(now + index * 8_000L,
                    new PerformanceTrendSnapshot(10, 0, 0, http, http, idle.webSocket(), idle.sse())));
        }
        var environment = new com.laker.postman.performance.core.run.PerformanceRunEnvironment(
                "demo", "Staging", List.of());
        var groupData = new com.laker.postman.performance.core.threadgroup.ThreadGroupData();
        groupData.threadMode = com.laker.postman.performance.core.threadgroup.ThreadGroupData.ThreadMode.RAMP_UP;
        groupData.numThreads = 10;
        groupData.rampUpDuration = 180;
        var plan = com.laker.postman.performance.core.run.PerformanceRunPlan.builder()
                .generatedBy("EasyPostman 6.3.3 · Demo")
                .environment(environment)
                .testPlan(new com.laker.postman.performance.core.plan.PerformanceCorePlanDocument(
                        com.laker.postman.performance.core.plan.PerformanceCorePlanNode.builder()
                                .name("Test plan")
                                .type(com.laker.postman.performance.core.model.NodeType.ROOT)
                                .children(List.of(com.laker.postman.performance.core.plan.PerformanceCorePlanNode.builder()
                                        .name("Ramp-up group")
                                        .type(com.laker.postman.performance.core.model.NodeType.THREAD_GROUP)
                                        .threadGroupData(groupData)
                                        .build()))
                                .build()))
                .build();
        export(destination, new ReportContext("Performance Test Report · Demo", plan, "Staging", points,
                List.of(), ""), List.of(table));
    }

    public void export(Path destination, String title,
                       List<PerformanceReportMarkdownBuilder.ReportTable> tables) throws IOException {
        export(destination, new ReportContext(title, null, "", List.of(), List.of(), ""), tables);
    }

    public void export(Path destination, ReportContext context,
                       List<PerformanceReportMarkdownBuilder.ReportTable> tables) throws IOException {
        List<PerformanceReportMarkdownBuilder.ReportTable> populated = tables.stream()
                .filter(table -> !table.rows().isEmpty()).toList();
        if (populated.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_MARKDOWN_EMPTY));
        }
        // Write beside the destination and replace only after the complete document is saved.
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(), ".performance-", ".pdf");
        try (PDDocument document = new PDDocument(); Layout layout = new Layout(document, context)) {
            layout.setHttpTable(populated.stream()
                    .filter(table -> table.columns().size() == 14)
                    .findFirst().orElse(null));
            layout.testSetup();
            layout.summaryAndCharts(populated);
            for (var table : populated) {
                if (table.columns().size() != 14) layout.protocolDetails(table);
            }
            layout.resultDetails();
            layout.closePage();
            document.getDocumentInformation().setTitle(context.title());
            document.getDocumentInformation().setCreator("EasyPostman");
            document.save(temporary.toFile());
            Files.move(temporary, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class Layout implements AutoCloseable {
        private static final float WIDTH = PDRectangle.A4.getWidth();
        private static final float MARGIN = 40;
        private static final float CONTENT = WIDTH - 2 * MARGIN;
        private static final Color INK = new Color(24, 42, 68);
        private static final Color BLUE = new Color(36, 93, 204);
        private static final Color MUTED = new Color(92, 108, 128);
        private static final Color PALE = new Color(241, 245, 251);
        private final PDDocument document;
        private final PDFont latin;
        private final PDFont cjk;
        private final String title;
        private final ReportContext context;
        private final Map<Integer, String> glyphCache = new HashMap<>();
        private final Map<String, PDFont> fontCache = new HashMap<>();
        private final Map<String, Float> widthCache = new HashMap<>();
        private final String generated = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        private PDPageContentStream stream;
        private float y;
        private PerformanceReportMarkdownBuilder.ReportTable activeTable;
        private PerformanceReportMarkdownBuilder.ReportTable httpTable;

        private Layout(PDDocument document, ReportContext context) throws IOException {
            this.document = document;
            this.context = context;
            this.title = context.title();
            latin = font("DejaVuSans.ttf");
            cjk = font("DroidSansFallback.ttf");
        }

        private PDFont font(String name) throws IOException {
            try (InputStream resource = PerformanceReportPdfExporter.class.getResourceAsStream("/fonts/" + name)) {
                if (resource == null) throw new IOException("Missing report font: " + name);
                return PDType0Font.load(document, resource);
            }
        }

        private void page(String section) throws IOException {
            closePage();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            rectangle(0, 802, WIDTH, 40, INK);
            text("EasyPostman", MARGIN, 817, 12, Color.WHITE);
            text(section, MARGIN, 774, 10, BLUE);
            text(title, MARGIN, 744, 23, INK);
            text(generated, MARGIN, 721, 9, MUTED);
            y = 688;
        }

        private void protocolDetails(PerformanceReportMarkdownBuilder.ReportTable table) throws IOException {
            activeTable = table;
            if (table.columns().size() == 14) return;
            page(table.title() + " / " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DETAILS));
            text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_OUTCOMES), MARGIN, y, 13, INK);
            y -= 24;
            int endpoints = Math.max(0, table.rows().size() - 1);
            for (int i = 0; i < Math.min(endpoints, 10); i++) {
                Object[] row = table.rows().get(i);
                List<String> name = wrap(value(row[0]), 9, 210);
                if (name.size() > 2) name = List.of(name.get(0), name.get(1) + "…");
                for (int line = 0; line < name.size(); line++) text(name.get(line), MARGIN, y - line * 12, 9, INK);
                double count = number(row[1]);
                double success = number(row[2]);
                float ratio = count <= 0 ? 0 : (float) Math.max(0, Math.min(1, success / count));
                rectangle(MARGIN + 226, y - 2, 190, 8, new Color(240, 205, 210));
                rectangle(MARGIN + 226, y - 2, 190 * ratio, 8, new Color(25, 147, 125));
                text(value(row[4]), MARGIN + 430, y - 1, 9, INK);
                y -= Math.max(23, name.size() * 12 + 6);
            }
            int groupStart = 1;
            while (groupStart < table.columns().size()) {
                if (y < 150) page(table.title() + " / " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DETAILS));
                int groupEnd = Math.min(groupStart + 5, table.columns().size());
                List<Integer> indices = new ArrayList<>();
                indices.add(0);
                for (int col = groupStart; col < groupEnd; col++) indices.add(col);
                row(table.columns().toArray(), indices, true, false);
                for (int i = 0; i < table.rows().size(); i++) {
                    Object[] values = table.rows().get(i);
                    if (y - height(values, indices, false) < 64) {
                        page(table.title() + " / " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DETAILS));
                        row(table.columns().toArray(), indices, true, false);
                    }
                    row(values, indices, false, i % 2 == 0 || i == table.rows().size() - 1);
                }
                groupStart = groupEnd;
            }
        }

        private void setHttpTable(PerformanceReportMarkdownBuilder.ReportTable table) {
            httpTable = table;
        }

        private void testSetup() throws IOException {
            page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_SETUP));
            section(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_SETUP));
            List<String[]> setup = setupRows();
            float cardWidth = (CONTENT - 16) / 3;
            float startY = y;
            for (int i = 0; i < setup.size(); i++) {
                String[] entry = setup.get(i);
                float x = MARGIN + (i % 3) * (cardWidth + 8);
                float top = startY - (i / 3) * 48;
                rectangle(x, top - 42, cardWidth, 40, PALE);
                text(entry[0], x + 8, top - 15, 8, MUTED);
                text(entry[1], x + 8, top - 32, 10, INK);
            }
            y = startY - ((setup.size() + 2) / 3) * 48 - 18;
        }

        private void summaryAndCharts(List<PerformanceReportMarkdownBuilder.ReportTable> tables) throws IOException {
            page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_SUMMARY));
            PerformanceReportMarkdownBuilder.ReportTable http = tables.stream()
                    .filter(table -> table.columns().size() == 14).findFirst().orElse(null);
            if (http != null) {
                Object[] total = http.rows().getLast();
                cards(http, total, new int[]{1, 2, 3, 4});
                cards(http, total, new int[]{5, 8, 12, 13});
            }
            if (!context.trendPoints().isEmpty()) charts(context.trendPoints());
            else {
                text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CHART_UNAVAILABLE), MARGIN, y, 9, MUTED);
                y -= 22;
            }
        }

        private void charts(List<PerformanceTrendView.TrendReportPoint> points) throws IOException {
            text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_RESPONSE_TIME), MARGIN, y, 13, INK);
            y -= 14;
            text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CHART_NOTE), MARGIN, y, 8, MUTED);
            y -= 12;
            chart(points, 0, y - 132, CONTENT, 120, I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_RESPONSE_TIME), 1);
            y -= 152;
            text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_THROUGHPUT), MARGIN, y, 13, INK);
            y -= 18;
            chart(points, 0, y - 126, CONTENT, 112, I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_THROUGHPUT), 0);
            y -= 148;
        }

        private void chart(List<PerformanceTrendView.TrendReportPoint> points,
                           float offsetX, float bottom, float width, float height,
                           String label, int metric) throws IOException {
            float left = MARGIN + offsetX + 38;
            float right = MARGIN + offsetX + width - 10;
            float top = bottom + height - 14;
            float plotBottom = bottom + 20;
            double maximum = 0;
            for (var point : points) {
                double value = metric == 0 ? point.snapshot().http().sampleRate()
                        : point.snapshot().http().avgDurationMs();
                if (Double.isFinite(value)) maximum = Math.max(maximum, value);
            }
            if (maximum <= 0) maximum = 1;
            rectangle(left, plotBottom, right - left, 0.6f, new Color(190, 201, 216));
            rectangle(left, plotBottom, 0.6f, top - plotBottom, new Color(190, 201, 216));
            float chartWidth = right - left;
            float chartHeight = top - plotBottom;
            String unit = metric == 0 ? "req/s" : "ms";
            for (int grid = 0; grid <= 4; grid++) {
                float fraction = grid / 4f;
                float gy = plotBottom + chartHeight * fraction;
                rectangle(left, gy, chartWidth, 0.35f, new Color(226, 232, 240));
                String tick = String.format(Locale.ROOT, "%.1f %s", maximum * fraction, unit);
                text(tick, MARGIN, gy - 2, 7, MUTED);
            }
            text(unit, MARGIN, top + 3, 7, MUTED);
            int count = points.size();
            for (int tick = 0; tick <= 4 && count > 0; tick++) {
                int index = Math.min(count - 1, tick * (count - 1) / 4);
                String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(
                        new Date(points.get(index).timestampMs()).toInstant().atZone(java.time.ZoneId.systemDefault()));
                float x = left + (count <= 1 ? 0 : chartWidth * index / (count - 1));
                text(time, x - textWidth(time, 6) / 2, bottom + 5, 6, MUTED);
            }
            drawSeries(points, left, plotBottom, chartWidth, chartHeight, maximum, metric, BLUE);
            if (metric == 1) {
                drawSeries(points, left, plotBottom, chartWidth, chartHeight, maximum, 2, new Color(139, 92, 186));
            }
            rectangle(left + 8, bottom - 4, 14, 2, BLUE);
            text(metric == 0 ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS)
                    : I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS), left + 27, bottom - 7, 7, MUTED);
            if (metric == 1) {
                rectangle(left + 170, bottom - 4, 14, 2, new Color(139, 92, 186));
                text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_MAX_RESPONSE), left + 189, bottom - 7, 7, MUTED);
            }
        }

        private void drawSeries(List<PerformanceTrendView.TrendReportPoint> points, float left, float bottom,
                                float width, float height, double maximum, int metric, Color color) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(1.5f);
            boolean open = false;
            float previousX = 0, previousY = 0;
            for (int i = 0; i < points.size(); i++) {
                var snapshot = points.get(i).snapshot();
                double value = metric == 0 ? snapshot.http().sampleRate()
                        : metric == 1 ? snapshot.http().avgDurationMs() : snapshot.http().maxDurationMs();
                if (!Double.isFinite(value)) { open = false; continue; }
                float x = left + (points.size() <= 1 ? 0 : width * i / (points.size() - 1));
                float pointY = bottom + (float) Math.max(0, Math.min(1, value / maximum)) * height;
                if (open) {
                    stream.moveTo(previousX, previousY);
                    stream.lineTo(x, pointY);
                    stream.stroke();
                }
                previousX = x; previousY = pointY; open = true;
            }
        }

        private void resultDetails() throws IOException {
            List<Object[]> requestRows = httpTable == null ? List.of() : httpTable.rows().stream()
                    .filter(row -> !String.valueOf(row[0]).equalsIgnoreCase(value(httpTable.rows().getLast()[0])))
                    .toList();
            page("1. Detailed results");
            section(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_SLOWEST));
            if (requestRows.isEmpty()) unavailable();
            else aggregateRequestTable(requestRows, true);
            float metricRowsHeight = requestRows.isEmpty() ? 0 : Math.min(requestRows.size(), 8) * 30;
            if (y - (24 + metricRowsHeight + 12) - 20 < 54
                    || (requestRows.size() > 0 && y - 44 < 54 + Math.min(requestRows.size(), 8) * 24)) {
                page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CONTINUED));
            }
            section(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_REQUEST_METRICS));
            if (requestRows.isEmpty()) unavailable();
            else aggregateRequestTable(requestRows, false);
            List<PerformanceSampleResult> errors = context.samples().stream()
                    .filter(sample -> !sample.isSuccessful()).toList();
            startDetailSection(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_ERRORS),
                    errors.isEmpty() && context.error().isBlank() ? 55 : 100,
                    errors.isEmpty() && context.error().isBlank() ? 0 : 1);
            if (errors.isEmpty() && context.error().isBlank() && !requestRows.isEmpty()
                    && requestRows.stream().allMatch(row -> durationNumber(row[4]) == 0)) {
                unavailable();
            } else if (errors.isEmpty() && context.error().isBlank()) {
                rectangle(MARGIN, y - 42, CONTENT, 34, new Color(235, 248, 244));
                text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_NO_ERRORS), MARGIN, y, 10,
                        new Color(25, 147, 125));
                y -= 48;
            } else {
                for (var error : errors) {
                    ensureSpace(30);
                    text(error.getApiName() + "  " + error.getResponseCode(), MARGIN, y, 9, INK);
                    y -= 13;
                    text(error.getErrorMsg(), MARGIN + 10, y, 8, new Color(184, 51, 66));
                    y -= 17;
                }
                if (!context.error().isBlank()) {
                    for (String line : wrap(context.error(), 9, CONTENT)) { ensureSpace(20); text(line, MARGIN, y, 9, new Color(184, 51, 66)); y -= 14; }
                }
            }
        }

        private void startDetailSection(String title, float required, int minRows) throws IOException {
            if (y - required - 20 < 54 || (minRows > 0 && y - 44 < 54 + minRows * 24)) {
                page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CONTINUED));
            }
            section(title);
        }

        private void section(String title) throws IOException {
            ensureSpace(42);
            text(title, MARGIN, y, 13, INK);
            y -= 20;
        }

        private void unavailable() throws IOException {
            ensureSpace(30);
            text(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DATA_UNAVAILABLE), MARGIN, y, 9, MUTED);
            y -= 18;
        }

        private void aggregateRequestTable(List<Object[]> rows, boolean slowest) throws IOException {
            String[] headers = slowest
                    ? new String[]{"Request", "Avg ms", "P90", "P95", "P99", "Min", "Max"}
                    : new String[]{"Request", "Total", "Requests/s", "Min", "Avg ms", "P90", "Max", "Error %"};
            float[] widths = slowest ? new float[]{208, 48, 40, 40, 40, 45, 45}
                    : new float[]{154, 48, 56, 42, 48, 40, 42, 48};
            List<Object[]> sorted = new ArrayList<>(rows);
            sorted.sort(Comparator.comparingDouble((Object[] row) -> durationNumber(row[8])).reversed());
            if (slowest) sorted = sorted.stream().limit(5).toList();
            else if (sorted.size() > 250) sorted = sorted.subList(0, 250);
            float required = 24 + sorted.size() * 24 + 12;
            tableHeader(headers, widths);
            for (int i = 0; i < sorted.size(); i++) {
                Object[] row = sorted.get(i);
                String[] cells = slowest
                        ? new String[]{value(row[0]), value(row[8]), value(row[11]), value(row[12]), value(row[13]), value(row[9]), value(row[10])}
                        : new String[]{value(row[0]), value(row[1]), value(row[5]), value(row[9]), value(row[8]), value(row[11]), value(row[10]), value(row[4])};
                if (y - 24 < 54) {
                    page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CONTINUED));
                    tableHeader(headers, widths);
                }
                tableRow(cells, widths, i % 2 == 0);
                y -= 6;
            }
        }

        private void tableHeader(String[] headers, float[] widths) throws IOException {
            if (y - 24 < 54) page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CONTINUED));
            float height = 24;
            rectangle(MARGIN, y - height, CONTENT, height, INK);
            float x = MARGIN;
            for (int i = 0; i < headers.length; i++) {
                text(headers[i], x + 5, y - 16, 7, Color.WHITE);
                x += widths[i];
            }
            y -= height;
        }

        private void tableRow(String[] cells, float[] widths, boolean shaded) throws IOException {
            float height = 24;
            if (shaded) rectangle(MARGIN, y - height, CONTENT, height, PALE);
            float x = MARGIN;
            for (int i = 0; i < cells.length; i++) {
                List<String> lines = wrap(cells[i], 7, widths[i] - 10);
                text(lines.getFirst(), x + 5, y - 15, 7, INK);
                x += widths[i];
            }
            y -= height;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < 54) {
                page(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_CONTINUED));
            }
        }

        private List<String[]> setupRows() {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_ENVIRONMENT),
                    context.environment().isBlank() ? "—" : context.environment()});
            rows.add(new String[]{I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_RUN_SOURCE),
                    context.plan() == null ? "—" : context.plan().getGeneratedBy()});
            rows.add(new String[]{I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DURATION),
                    context.plan() == null ? "—" : planDuration(context.plan())});
            if (context.plan() != null) {
                var root = context.plan().getTestPlan() == null ? null : context.plan().getTestPlan().getRoot();
                collectThreadGroups(root, rows);
            }
            if (rows.size() == 3) rows.add(new String[]{I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_LOAD_PROFILE), "—"});
            return rows;
        }

        private void collectThreadGroups(PerformanceCorePlanNode node, List<String[]> rows) {
            if (node == null || !node.isEnabled()) return;
            if (node.getType() == NodeType.THREAD_GROUP && node.getThreadGroupData() != null) {
                var group = node.getThreadGroupData();
                rows.add(new String[]{node.getName(), group.numThreads + " VU · " + group.threadMode
                        + " · " + threadGroupDuration(group) + " s"});
            }
            for (var child : node.getChildren()) collectThreadGroups(child, rows);
        }

        private String planDuration(PerformanceRunPlan plan) {
            int[] max = {0};
            collectDuration(plan.getTestPlan() == null ? null : plan.getTestPlan().getRoot(), max);
            return max[0] > 0 ? max[0] + " s" : "—";
        }

        private void collectDuration(PerformanceCorePlanNode node, int[] max) {
            if (node == null || !node.isEnabled()) return;
            if (node.getType() == NodeType.THREAD_GROUP && node.getThreadGroupData() != null) {
                max[0] = Math.max(max[0], threadGroupDuration(node.getThreadGroupData()));
            }
            node.getChildren().forEach(child -> collectDuration(child, max));
        }

        private int threadGroupDuration(com.laker.postman.performance.core.threadgroup.ThreadGroupData group) {
            return switch (group.threadMode) {
                case RAMP_UP -> group.rampUpDuration;
                case SPIKE -> group.spikeDuration;
                case STAIRS -> group.stairsDuration;
                default -> group.duration;
            };
        }

        private void cards(PerformanceReportMarkdownBuilder.ReportTable table, Object[] total,
                           int[] indices) throws IOException {
            float cardWidth = (CONTENT - 24) / 4;
            for (int i = 0; i < indices.length; i++) {
                int index = indices[i];
                float x = MARGIN + i * (cardWidth + 8);
                rectangle(x, y - 68, cardWidth, 68, PALE);
                List<String> label = wrap(table.columns().get(index), 9, cardWidth - 16);
                for (int line = 0; line < label.size(); line++) {
                    text(label.get(line), x + 8, y - 16 - line * 11, 9, MUTED);
                }
                String metric = value(total[index]);
                float size = Math.min(16, 16 * (cardWidth - 16) / Math.max(1, textWidth(metric, 16)));
                text(metric, x + 8, y - 55, size, index == 3 ? new Color(184, 51, 66) : INK);
            }
            y -= 84;
        }

        private float cellWidth(int column, int count) {
            return column == 0 ? 175 : (CONTENT - 175) / (count - 1);
        }

        private float height(Object[] values, List<Integer> indices, boolean header) throws IOException {
            int lines = 1;
            for (int i = 0; i < indices.size(); i++) {
                lines = Math.max(lines, wrap(value(values[indices.get(i)]), header ? 8 : 9,
                        cellWidth(i, indices.size()) - 12).size());
            }
            return lines * 12 + 16;
        }

        private void row(Object[] values, List<Integer> indices, boolean header, boolean shaded) throws IOException {
            List<List<String>> cells = new ArrayList<>();
            int lineCount = 1;
            for (int i = 0; i < indices.size(); i++) {
                List<String> lines = wrap(value(values[indices.get(i)]), header ? 8 : 9,
                        cellWidth(i, indices.size()) - 12);
                cells.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }
            int offset = 0;
            while (offset < lineCount) {
                int available = Math.max(1, (int) ((y - 64 - 16) / 12));
                int count = Math.min(lineCount - offset, available);
                float height = count * 12 + 16;
                if (header || shaded) rectangle(MARGIN, y - height, CONTENT, height, header ? INK : PALE);
                float x = MARGIN;
                for (int i = 0; i < indices.size(); i++) {
                    List<String> lines = cells.get(i);
                    for (int line = offset; line < Math.min(offset + count, lines.size()); line++) {
                        text(lines.get(line), x + 6, y - 16 - (line - offset) * 12,
                                header ? 8 : 9, header ? Color.WHITE : INK);
                    }
                    x += cellWidth(i, indices.size());
                }
                y -= height;
                offset += count;
                if (offset < lineCount) {
                    page(activeTable.title() + " / " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_PDF_DETAILS));
                    row(activeTable.columns().toArray(), indices, true, false);
                }
            }
        }

        private List<String> wrap(String value, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            float used = 0;
            for (int cp : value.codePoints().toArray()) {
                String glyph = glyph(cp);
                float advance = advance(glyph, size);
                if (cp == '\n') {
                    lines.add(line.toString());
                    line.setLength(0);
                    used = 0;
                    continue;
                }
                if (used + advance > width && !line.isEmpty()) {
                    int boundary = line.lastIndexOf(" ");
                    if (boundary > 0) {
                        lines.add(line.substring(0, boundary));
                        String remainder = line.substring(boundary + 1);
                        line.setLength(0);
                        line.append(remainder);
                        used = textWidth(remainder, size);
                    } else {
                        lines.add(line.toString());
                        line.setLength(0);
                        used = 0;
                    }
                }
                if (line.isEmpty() && Character.isWhitespace(cp)) continue;
                line.append(glyph);
                used += advance;
            }
            if (!line.isEmpty() || lines.isEmpty()) lines.add(line.toString());
            return lines;
        }

        private float textWidth(String value, float size) throws IOException {
            float width = 0;
            for (int cp : value.codePoints().toArray()) {
                String glyph = glyph(cp);
                width += advance(glyph, size);
            }
            return width;
        }

        private String glyph(int cp) {
            return glyphCache.computeIfAbsent(cp, codePoint -> {
                if (Character.isISOControl(codePoint)) return " ";
                String value = new String(Character.toChars(codePoint));
                try { latin.encode(value); return value; } catch (IOException | IllegalArgumentException ignored) { }
                try { cjk.encode(value); return value; } catch (IOException | IllegalArgumentException ignored) { return "?"; }
            });
        }

        private PDFont select(String glyph) {
            return fontCache.computeIfAbsent(glyph, value -> {
                try { latin.encode(value); return latin; } catch (IOException | IllegalArgumentException ignored) { return cjk; }
            });
        }

        private float advance(String glyph, float size) throws IOException {
            Float width = widthCache.get(glyph);
            if (width == null) {
                width = select(glyph).getStringWidth(glyph) / 1000;
                widthCache.put(glyph, width);
            }
            return width * size;
        }

        private void text(String value, float x, float baseline, float size, Color color) throws IOException {
            stream.beginText();
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            for (int cp : value.codePoints().toArray()) {
                String glyph = glyph(cp);
                stream.setFont(select(glyph), size);
                stream.showText(glyph);
            }
            stream.endText();
        }

        private void rectangle(float x, float bottom, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, bottom, width, height);
            stream.fill();
        }

        private void closePage() throws IOException {
            if (stream == null) return;
            text("EasyPostman  /  " + document.getNumberOfPages(), MARGIN, 30, 8, MUTED);
            stream.close();
            stream = null;
        }

        @Override public void close() throws IOException { closePage(); }
        private static String value(Object value) { return value == null ? "" : value.toString(); }
        private static double number(Object value) {
            if (value instanceof Number number) return number.doubleValue();
            try { return Double.parseDouble(value(value)); } catch (NumberFormatException ignored) { return 0; }
        }
        private static double durationNumber(Object value) {
            String raw = value(value).trim().toLowerCase(Locale.ROOT).replace("ms", "").trim();
            try { return Double.parseDouble(raw); } catch (NumberFormatException ignored) { return 0; }
        }
    }
}
