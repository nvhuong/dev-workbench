package com.nvhuong.devworkbench.panel.functional;

import com.nvhuong.devworkbench.functional.model.AssertionResult;
import com.nvhuong.devworkbench.functional.model.BatchExecutionHistory;
import com.nvhuong.devworkbench.functional.model.IterationResult;
import com.nvhuong.devworkbench.functional.model.RequestResult;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.TimeDisplayUtil;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Renders a functional run as a shareable PNG report: header, summary cards, every request row and the failure
 * details. It always uses a light palette so the image reads the same whichever theme the app is in.
 */
final class FunctionalReportImage {
    static final int WIDTH = 1280;
    static final int MAX_ROWS = 2000;
    static final int MAX_FAILURES = 50;

    private static final int MARGIN = 40;
    private static final int ROW_HEIGHT = 30;
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color BRAND = new Color(0x34B36B);
    private static final Color TEXT = new Color(0x0F172A);
    private static final Color MUTED = new Color(0x64748B);
    private static final Color BORDER = new Color(0xE2E8F0);
    private static final Color HEADER_BG = new Color(0xF4F6F8);
    private static final Color ZEBRA = new Color(0xFAFBFC);
    private static final Color SUCCESS = new Color(0x15803D);
    private static final Color ERROR = new Color(0xDC2626);
    private static final Color WARNING = new Color(0xB45309);
    private static final Color GET = new Color(0x2E7D32);
    private static final Color POST = new Color(0xB45309);
    private static final Color PUT = new Color(0x1D4ED8);
    private static final Color DELETE = new Color(0xDC2626);

    /** Texts are passed in so the renderer stays independent of the app's i18n setup (and testable). */
    record Labels(String title, String generated, String[] statLabels, String completed, String[] columns,
                  String pass, String fail, String noTests, String failures, String moreRows, String skippedStatus) {
    }

    private record Row(String iteration, RequestResult request) {
    }

    private FunctionalReportImage() {
    }

    static BufferedImage render(BatchExecutionHistory history, Labels labels, Image logo, long generatedAt) {
        Objects.requireNonNull(history, "history");
        List<Row> rows = new ArrayList<>();
        for (IterationResult iteration : history.getIterations()) {
            for (RequestResult request : iteration.getRequestResults()) {
                rows.add(new Row("#" + (iteration.getIterationIndex() + 1), request));
            }
        }
        List<String> failures = failureLines(rows, labels.skippedStatus());
        int shownRows = Math.min(rows.size(), MAX_ROWS);
        int height = MARGIN + 70 + 24 + 2 * 76 + 24 + ROW_HEIGHT * (shownRows + 1)
                + (rows.size() > shownRows ? ROW_HEIGHT : 0)
                + (failures.isEmpty() ? 0 : 40 + failures.size() * 24)
                + 60 + MARGIN;

        BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, WIDTH, height);
            g.setColor(BRAND);
            g.fillRect(0, 0, WIDTH, 6);

            int y = MARGIN;
            y = drawHeader(g, labels, logo, generatedAt, y);
            y = drawStats(g, history, labels, y + 24);
            y = drawTable(g, rows, shownRows, labels, y + 24);
            if (!failures.isEmpty()) {
                y = drawFailures(g, failures, labels, y + 16);
            }
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(MUTED);
            g.drawString("DevWorkbench", MARGIN, height - MARGIN + 10);
        } finally {
            g.dispose();
        }
        return image;
    }

    static void write(BufferedImage image, Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        if (!ImageIO.write(image, "png", destination.toFile())) {
            throw new IOException("No PNG writer available");
        }
    }

    private static int drawHeader(Graphics2D g, Labels labels, Image logo, long generatedAt, int y) {
        int x = MARGIN;
        if (logo != null) {
            g.drawImage(logo, x, y, 56, 56, null);
            x += 72;
        }
        g.setColor(TEXT);
        g.setFont(font(Font.BOLD, 26));
        g.drawString(labels.title(), x, y + 26);
        g.setColor(MUTED);
        g.setFont(font(Font.PLAIN, 14));
        g.drawString(labels.generated() + " " + timestamp(generatedAt), x, y + 52);
        return y + 70;
    }

    private static int drawStats(Graphics2D g, BatchExecutionHistory history, Labels labels, int y) {
        FunctionalRunStats stats = FunctionalRunStats.of(history, labels.skippedStatus());
        Color rateColor = stats.successRate() >= 100 ? SUCCESS : stats.successRate() >= 80 ? WARNING : ERROR;
        String[] values = {
                String.valueOf(stats.iterations()), String.valueOf(stats.requests()),
                String.format("%.1f%%", stats.successRate()), TimeDisplayUtil.formatElapsedTime(stats.totalTimeMs()),
                timestamp(history.getStartTime()), timestamp(history.getEndTime()),
                stats.averageTimeMs() + " ms", labels.completed()
        };
        Color[] colors = {TEXT, TEXT, rateColor, TEXT, TEXT, TEXT, TEXT, SUCCESS};
        int gap = 16;
        int cardWidth = (WIDTH - 2 * MARGIN - 3 * gap) / 4;
        for (int i = 0; i < values.length; i++) {
            int cx = MARGIN + (i % 4) * (cardWidth + gap);
            int cy = y + (i / 4) * 76;
            g.setColor(HEADER_BG);
            g.fillRoundRect(cx, cy, cardWidth, 64, 12, 12);
            g.setColor(colors[i]);
            g.setFont(font(Font.BOLD, 20));
            drawCentered(g, values[i], cx, cardWidth, cy + 30);
            g.setColor(MUTED);
            g.setFont(font(Font.PLAIN, 13));
            drawCentered(g, labels.statLabels()[i], cx, cardWidth, cy + 52);
        }
        return y + 2 * 76 - 12;
    }

    private static int drawTable(Graphics2D g, List<Row> rows, int shownRows, Labels labels, int y) {
        int[] widths = {80, 240, 0, 130, 110, 100, 120};
        int fixed = 0;
        for (int w : widths) fixed += w;
        widths[2] = WIDTH - 2 * MARGIN - fixed;

        g.setColor(HEADER_BG);
        g.fillRect(MARGIN, y, WIDTH - 2 * MARGIN, ROW_HEIGHT);
        g.setFont(font(Font.BOLD, 14));
        g.setColor(TEXT);
        int x = MARGIN;
        for (int c = 0; c < widths.length; c++) {
            drawCell(g, labels.columns()[c], x, widths[c], y, c >= 3);
            x += widths[c];
        }
        y += ROW_HEIGHT;

        g.setFont(font(Font.PLAIN, 14));
        for (int r = 0; r < shownRows; r++) {
            Row row = rows.get(r);
            RequestResult request = row.request();
            if (r % 2 == 1) {
                g.setColor(ZEBRA);
                g.fillRect(MARGIN, y, WIDTH - 2 * MARGIN, ROW_HEIGHT);
            }
            String[] cells = {
                    row.iteration(), nullToDash(request.getRequestName()), nullToDash(request.getUrl()),
                    nullToDash(request.getMethod()), nullToDash(request.getStatus()),
                    TimeDisplayUtil.formatElapsedTime(request.getCost()), resultText(request.getAssertion(), labels)
            };
            Color[] colors = {MUTED, TEXT, MUTED, methodColor(request.getMethod()), statusColor(request.getStatus()),
                    TEXT, resultColor(request.getAssertion())};
            x = MARGIN;
            for (int c = 0; c < widths.length; c++) {
                g.setColor(colors[c]);
                drawCell(g, cells[c], x, widths[c], y, c >= 3);
                x += widths[c];
            }
            g.setColor(BORDER);
            g.drawLine(MARGIN, y + ROW_HEIGHT - 1, WIDTH - MARGIN, y + ROW_HEIGHT - 1);
            y += ROW_HEIGHT;
        }
        if (rows.size() > shownRows) {
            g.setColor(MUTED);
            drawCell(g, labels.moreRows().replace("{0}", String.valueOf(rows.size() - shownRows)),
                    MARGIN, WIDTH - 2 * MARGIN, y, false);
            y += ROW_HEIGHT;
        }
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(MARGIN, y - ROW_HEIGHT * (shownRows + 1) - (rows.size() > shownRows ? ROW_HEIGHT : 0),
                WIDTH - 2 * MARGIN - 1, ROW_HEIGHT * (shownRows + 1) + (rows.size() > shownRows ? ROW_HEIGHT : 0));
        return y;
    }

    private static int drawFailures(Graphics2D g, List<String> failures, Labels labels, int y) {
        g.setColor(ERROR);
        g.setFont(font(Font.BOLD, 16));
        g.drawString(labels.failures(), MARGIN, y + 20);
        y += 40;
        g.setFont(font(Font.PLAIN, 14));
        g.setColor(TEXT);
        for (String line : failures) {
            drawCell(g, line, MARGIN, WIDTH - 2 * MARGIN, y - 6, false);
            y += 24;
        }
        return y;
    }

    private static List<String> failureLines(List<Row> rows, String skippedStatus) {
        List<String> lines = new ArrayList<>();
        for (Row row : rows) {
            RequestResult request = row.request();
            boolean failed = FunctionalRunStats.isFailure(request, skippedStatus);
            String error = request.getErrorMessage();
            if (!failed && (error == null || error.isBlank())) continue;
            StringBuilder line = new StringBuilder(row.iteration()).append(" · ")
                    .append(nullToDash(request.getRequestName())).append(": ");
            List<String> reasons = new ArrayList<>();
            if (error != null && !error.isBlank()) reasons.add(error.strip());
            for (TestResult test : request.getTestResults()) {
                if (!test.passed) {
                    reasons.add(test.message == null || test.message.isBlank() ? test.name : test.name + " — " + test.message);
                }
            }
            line.append(reasons.isEmpty() ? "-" : String.join("; ", reasons));
            lines.add(line.toString().replaceAll("\\s+", " "));
            if (lines.size() >= MAX_FAILURES) break;
        }
        return lines;
    }

    private static void drawCell(Graphics2D g, String text, int x, int width, int rowTop, boolean center) {
        FontMetrics metrics = g.getFontMetrics();
        String fitted = ellipsize(text, metrics, width - 20);
        int baseline = rowTop + (ROW_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
        int textX = center ? x + (width - metrics.stringWidth(fitted)) / 2 : x + 10;
        g.drawString(fitted, textX, baseline);
    }

    private static void drawCentered(Graphics2D g, String text, int x, int width, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        String fitted = ellipsize(text, metrics, width - 16);
        g.drawString(fitted, x + (width - metrics.stringWidth(fitted)) / 2, baseline);
    }

    static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (text == null) return "";
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) end--;
        return text.substring(0, end) + ellipsis;
    }

    private static String resultText(AssertionResult result, Labels labels) {
        if (AssertionResult.PASS.equals(result)) return labels.pass();
        if (AssertionResult.FAIL.equals(result)) return labels.fail();
        return labels.noTests();
    }

    private static Color resultColor(AssertionResult result) {
        if (AssertionResult.PASS.equals(result)) return SUCCESS;
        if (AssertionResult.FAIL.equals(result)) return ERROR;
        return MUTED;
    }

    private static Color statusColor(String status) {
        if (status == null) return MUTED;
        if (status.startsWith("2")) return SUCCESS;
        if (status.startsWith("3")) return WARNING;
        if (status.startsWith("4") || status.startsWith("5")) return ERROR;
        return MUTED;
    }

    private static Color methodColor(String method) {
        if (method == null) return TEXT;
        return switch (method.toUpperCase()) {
            case "GET" -> GET;
            case "POST" -> POST;
            case "PUT", "PATCH" -> PUT;
            case "DELETE" -> DELETE;
            default -> TEXT;
        };
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String timestamp(long millis) {
        return millis <= 0 ? "-" : new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(millis));
    }

    private static Font font(int style, int size) {
        return FontsUtil.getDefaultFont(style).deriveFont(style, (float) size);
    }
}
