package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.frame.MainFrame;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Không tạo cửa sổ: độc lập JVM Không có gì trong xác minh Splash Bắt đầu tổng số duy trì của nhiệm vụ EDT Ngữ nghĩa hoàn chỉnh.
 */
public class NoSplashStartupWorkerTest {
    @Test
    public void shouldKeepProcessAliveUntilSlowStartupCompletesOnEdt() throws Exception {
        ProcessResult result = runProbe("success");

        assertEquals(result.exitCode(), 0, result.output());
        assertTrue(result.output().contains("MAIN_FRAME_SHOWN_ON_EDT"), result.output());
        assertTrue(result.output().contains("STARTUP_CONTINUATION_ON_EDT"), result.output());
    }

    @Test
    public void shouldKeepProcessAliveUntilSlowStartupFailureIsHandled() throws Exception {
        ProcessResult result = runProbe("failure");

        assertEquals(result.exitCode(), 1, result.output());
        assertTrue(result.output().contains("SLOW_STARTUP_FAILURE"), result.output());
    }

    private ProcessResult runProbe(String mode) throws Exception {
        Path directory = Files.createTempDirectory("devworkbench-startup-probe-");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true",
                "-Duser.home=" + directory,
                "-DCONSOLE_LOG_LEVEL=ERROR",
                "-Dtest.log.level=ERROR",
                "-cp", System.getProperty("java.class.path"),
                StartupProbe.class.getName(), mode
        ).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Startup probe did not terminate");
            return new ProcessResult(process.exitValue(),
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            process.destroyForcibly();
            try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }

    public static class StartupProbe {
        public static void main(String[] args) {
            boolean fail = "failure".equals(args[0]);
            SwingUtilities.invokeLater(() -> new NoSplashStartupWorker(new StartupCoordinator(System.nanoTime()) {
                @Override
                public MainFrame prepareMainFrameShell(StartupProgressListener listener) throws Exception {
                    if (Thread.currentThread().isDaemon()) {
                        throw new IllegalStateException("Startup task must keep the JVM alive");
                    }
                    // Quá AWT Không có thời gian đóng cửa sổ tự động; có nghĩa là chậm IOC hoặc khởi tạo plugin.
                    Thread.sleep(3_000);
                    if (fail) {
                        throw new IllegalStateException("SLOW_STARTUP_FAILURE");
                    }
                    return null;
                }

                @Override
                public void showMainFrameAndLoadContent(MainFrame mainFrame) {
                    assertEdtAndStartupThread();
                    System.out.println("MAIN_FRAME_SHOWN_ON_EDT");
                }

                @Override
                public void runAfterMainContentReady(MainFrame mainFrame, Runnable onReady,
                                                     java.util.function.Consumer<Throwable> onFailure) {
                    assertEdtAndStartupThread();
                    System.out.println("STARTUP_CONTINUATION_ON_EDT");
                }

                private void assertEdtAndStartupThread() {
                    if (!SwingUtilities.isEventDispatchThread()) {
                        throw new IllegalStateException("Startup completion must run on EDT");
                    }
                    boolean startupThreadAlive = Thread.getAllStackTraces().keySet().stream()
                            .anyMatch(thread -> "GUI-No-Splash-Startup".equals(thread.getName())
                                    && thread.isAlive() && !thread.isDaemon());
                    if (!startupThreadAlive) {
                        throw new IllegalStateException("Startup thread exited before EDT completion");
                    }
                }
            }).start());
        }
    }
}
