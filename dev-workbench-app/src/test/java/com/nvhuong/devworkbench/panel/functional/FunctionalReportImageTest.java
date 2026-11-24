package com.nvhuong.devworkbench.panel.functional;

import com.nvhuong.devworkbench.functional.model.AssertionResult;
import com.nvhuong.devworkbench.functional.model.BatchExecutionHistory;
import com.nvhuong.devworkbench.functional.model.IterationResult;
import com.nvhuong.devworkbench.functional.model.RequestResult;
import com.nvhuong.devworkbench.script.model.TestResult;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FunctionalReportImageTest {

    private static final FunctionalReportImage.Labels LABELS = new FunctionalReportImage.Labels(
            "Báo cáo functional test", "Tạo lúc",
            new String[]{"Lặp", "Request", "Tỷ lệ", "Tổng", "Bắt đầu", "Kết thúc", "Trung bình", "Trạng thái"},
            "Hoàn thành",
            new String[]{"#", "Tên", "URL", "Phương thức", "Trạng thái", "Thời gian", "Kết quả"},
            "Đạt", "Lỗi", "Không có test", "Chi tiết lỗi", "… và {0} dòng khác", "Bỏ qua");

    @Test
    public void statsCountPassesAndFailures() {
        BatchExecutionHistory history = history(3, true);

        FunctionalRunStats stats = FunctionalRunStats.of(history, "Bỏ qua");

        assertEquals(stats.iterations(), 1);
        assertEquals(stats.requests(), 5);
        assertEquals(stats.passed(), 3);
        assertEquals(stats.failed(), 1);
        assertEquals(stats.successRate(), 75.0, 0.001);
    }

    @Test
    public void rendersEveryRowAndAddsFailureDetails() throws Exception {
        BufferedImage passing = FunctionalReportImage.render(history(30, false), LABELS, null, 1_700_000_000_000L);
        BufferedImage withFailure = FunctionalReportImage.render(history(30, true), LABELS, null, 1_700_000_000_000L);

        assertEquals(passing.getWidth(), FunctionalReportImage.WIDTH);
        // 31 request rows plus the header must fit, i.e. rows are not clipped to the visible table.
        assertTrue(passing.getHeight() > 32 * 30, "height " + passing.getHeight());
        assertTrue(withFailure.getHeight() > passing.getHeight(), "failure details add a section");
        assertTrue(hasBrandAccent(passing), "header accent bar is drawn");

        Path file = Files.createTempDirectory("functional-report").resolve("nested/report.png");
        FunctionalReportImage.write(passing, file);
        BufferedImage reread = ImageIO.read(file.toFile());
        assertEquals(reread.getWidth(), passing.getWidth());
        assertEquals(reread.getHeight(), passing.getHeight());
    }

    @Test
    public void capsHugeRunsWithASummaryRow() {
        BufferedImage image = FunctionalReportImage.render(history(FunctionalReportImage.MAX_ROWS + 500, false),
                LABELS, null, 0L);

        assertTrue(image.getHeight() < (FunctionalReportImage.MAX_ROWS + 100) * 30, "height " + image.getHeight());
    }

    @Test
    public void ellipsizesLongCells() {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FontMetrics metrics = scratch.createGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        String fitted = FunctionalReportImage.ellipsize("https://example.com/" + "a".repeat(300), metrics, 200);

        assertTrue(fitted.endsWith("…"));
        assertTrue(metrics.stringWidth(fitted) <= 200);
        assertEquals(FunctionalReportImage.ellipsize("short", metrics, 200), "short");
    }

    private static boolean hasBrandAccent(BufferedImage image) {
        int rgb = image.getRGB(image.getWidth() / 2, 2) & 0xFFFFFF;
        return rgb == 0x34B36B;
    }

    /** One iteration with {@code passing} passing requests, one request without tests and optionally a failure. */
    private static BatchExecutionHistory history(int passing, boolean withFailure) {
        BatchExecutionHistory history = new BatchExecutionHistory();
        IterationResult iteration = new IterationResult(0, Map.of());
        for (int i = 0; i < passing; i++) {
            iteration.addRequestResult(new RequestResult("GET Example " + i, "GET", "https://httpbin.org/get?i=" + i,
                    null, null, 120 + i, "200", AssertionResult.PASS,
                    List.of(new TestResult("Status code is 200", true, null)), null));
        }
        iteration.addRequestResult(new RequestResult("PUT Example", "PUT", "https://httpbin.org/put",
                null, null, 300, "200", AssertionResult.NO_TESTS, List.of(), null));
        if (withFailure) {
            iteration.addRequestResult(new RequestResult("Tạo đơn hàng", "POST", "https://httpbin.org/post",
                    null, null, 450, "500", AssertionResult.FAIL,
                    List.of(new TestResult("Mã trạng thái là 201", false, "expected 500 to equal 201")), null));
        }
        iteration.complete();
        history.addIteration(iteration);
        history.complete();
        return history;
    }
}
