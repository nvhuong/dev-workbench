package com.laker.postman.performance.report;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class PerformanceReportPdfExporterTest {
    @Test
    public void exportsUnicodeMetricsAndPaginatedTables() throws Exception {
        Path destination = Path.of("target/performance-report-demo.pdf");
        Files.createDirectories(destination.toAbsolutePath().getParent());
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int avg = 80 + i * 37;
            rows.add(new Object[]{"GET /api/orders/" + i + " · Kiểm thử 性能测试", 128, 126, 2,
                    "98.44%", "0.71", "12.5", "48.0", avg + " ms", (avg - 35) + " ms",
                    (avg + 190) + " ms", (avg + 45) + " ms", (avg + 85) + " ms", (avg + 165) + " ms"});
        }
        rows.add(new Object[]{"Total", 1024, 1008, 16, "98.44%", "5.69", "100.0", "384.0",
                "218 ms", "42 ms", "986 ms", "415 ms", "532 ms", "781 ms"});
        var table = new PerformanceReportMarkdownBuilder.ReportTable("HTTP",
                Arrays.asList(PerformanceReportTableSchema.httpColumns()), rows);
        PerformanceReportPdfExporter.exportDemo(destination);
        try (PDDocument document = Loader.loadPDF(destination.toFile())) {
            assertTrue(document.getNumberOfPages() >= 3);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.indexOf("Test setup") < text.indexOf("1. Summary"), text);
            assertTrue(text.indexOf("1. Summary") < text.indexOf("1.1 Response time"), text);
            assertTrue(text.contains("Response time"), text);
            assertTrue(text.contains("266.2 ms"), text);
            assertTrue(text.contains("req/s"), text);
            assertTrue(text.contains("Throughput"), text);
            assertTrue(text.contains("Requests with slowest response times"), text);
            assertTrue(text.contains("Metrics for each request"), text);
            assertTrue(text.contains("Errors"), text);
            assertFalse(text.contains("Endpoint success rate"), text);
            assertTrue(text.contains("GET /api/orders/7"));
            assertEquals(document.getDocumentInformation().getTitle(), "Performance Test Report · Demo");
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 110), "png",
                    Path.of("target/performance-report-preview.png").toFile());
        }
    }

    @Test
    public void exportsEveryProtocolAndSplitsVeryLongEndpointRows() throws Exception {
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
                tables.add(new PerformanceReportMarkdownBuilder.ReportTable(protocols[i],
                        Arrays.asList(schemas[i]), List.of(endpoint, total)));
            }
            PerformanceReportPdfExporter.export(destination, "Performance test report", tables);
            try (PDDocument document = Loader.loadPDF(destination.toFile())) {
                String text = new PDFTextStripper().getText(document);
                assertTrue(text.contains("WebSocket"));
                assertTrue(text.contains("SSE"));
                assertTrue(text.replaceAll("\\s", "").contains("END-OF-PATH"));
                assertTrue(document.getNumberOfPages() > 8);
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
}
