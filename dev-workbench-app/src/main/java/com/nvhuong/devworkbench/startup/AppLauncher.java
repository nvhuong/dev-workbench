package com.nvhuong.devworkbench.startup;

import ch.qos.logback.classic.LoggerContext;
import com.formdev.flatlaf.util.SystemInfo;
import com.nvhuong.devworkbench.common.themes.SimpleThemeManager;
import com.nvhuong.devworkbench.http.runtime.app.AppHttpRuntimeBootstrap;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.platform.instance.SingleInstanceCoordinator;
import com.nvhuong.devworkbench.platform.network.SocketDirectProxySelector;
import com.nvhuong.devworkbench.plugin.runtime.PluginRuntime;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.AppRuntimeLayout;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.OptionalInt;

/**
 * Trình khởi chạy ứng dụng: Chịu trách nhiệm điều phối khởi động sau cổng chính.
 */
@Slf4j
@UtilityClass
public class AppLauncher {
    public static final int GUI_STARTED = Integer.MIN_VALUE;
    private static volatile SingleInstanceCoordinator singleInstanceCoordinator;
    private static volatile boolean skipHostRuntimeShutdown;
    private static volatile String startupCheckpoint = "launcher initialized";

    static void markStartupCheckpoint(String checkpoint) {
        startupCheckpoint = checkpoint;
    }

    public int launch(String[] args) {
        long launchStartedAt = System.nanoTime();
        configureBaseRuntimeEnvironment();
        registerShutdownHook();
        OptionalInt commandExitCode = new AppCommandRouter().route(args, System.out, System.err);
        if (commandExitCode.isPresent()) {
            return commandExitCode.getAsInt();
        }
        markStartupCheckpoint("GUI launch selected");
        log.info("Starting GUI launch sequence: os={} {}, arch={}, java={} ({}), headless={}",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                java.awt.GraphicsEnvironment.isHeadless());
        OptionalInt singleInstanceExitCode = coordinateGuiInstance();
        if (singleInstanceExitCode.isPresent()) {
            return singleInstanceExitCode.getAsInt();
        }
        // Trước tiên hãy hoàn thành cấu hình nền tảng và chủ đề, sau đó chuyển sang tạo thành phần EDT。
        configurePlatformWindowDecorations();
        StartupCoordinator startupCoordinator = new StartupCoordinator(launchStartedAt);
        startupCoordinator.startHostIocInitialization();
        initializeLookAndFeel();
        markStartupCheckpoint("look and feel initialized");
        SwingUtilities.invokeLater(() -> startSwingApplication(startupCoordinator));
        return GUI_STARTED;
    }

    private OptionalInt coordinateGuiInstance() {
        markStartupCheckpoint("coordinating GUI instance");
        SingleInstanceCoordinator.LaunchResult result;
        try {
            result = SingleInstanceCoordinator.acquireOrNotify(
                    AppRuntimeLayout.dataRootDirectory(AppLauncher.class),
                    AppSingleInstanceController::requestActivation
            );
        } catch (IOException | RuntimeException exception) {
            skipHostRuntimeShutdown = true;
            StartupFailureHandler.showSingleInstanceSetupErrorAndExit(exception);
            return OptionalInt.of(GUI_STARTED);
        }

        if (result.isPrimary()) {
            singleInstanceCoordinator = result.coordinator();
            markStartupCheckpoint("primary GUI instance acquired");
            log.info("GUI instance coordination selected this process as primary");
            return OptionalInt.empty();
        }
        if (result.status() == SingleInstanceCoordinator.LaunchStatus.EXISTING_INSTANCE_NOTIFIED) {
            skipHostRuntimeShutdown = true;
            log.info("Activated the existing Dev Workbench GUI instance");
            return OptionalInt.of(0);
        }

        skipHostRuntimeShutdown = true;
        StartupFailureHandler.showExistingInstanceUnavailableAndExit();
        return OptionalInt.of(GUI_STARTED);
    }

    private void configureBaseRuntimeEnvironment() {
        // Các cặp cấu hình này GUI và headless CLI là hợp lệ và không nên kích hoạt Swing khởi tạo.
        Thread.setDefaultUncaughtExceptionHandler(new AppUncaughtExceptionHandler());
        System.setProperty("java.net.useSystemProxies", "true");
        // Must precede any HTTP client creation: clients capture ProxySelector.getDefault() when built.
        SocketDirectProxySelector.install();
        AppHttpRuntimeBootstrap.configure();
    }

    private void configurePlatformWindowDecorations() {
        if (!SystemInfo.isLinux) {
            return;
        }
        log.debug("Detected Linux platform, enabling custom window decorations");
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    private void startSwingApplication(StartupCoordinator startupCoordinator) {
        markStartupCheckpoint("Swing EDT initialization started");
        log.info("Starting Swing application initialization on EDT");
        startMainFrame(startupCoordinator);
    }

    /**
     * Look and feel Phải ở đó Swing Hoàn thành trước khi tạo thành phần, nếu không các thành phần màn hình đầu tiên sẽ bị trộn lẫn với thành phần cũ UI defaults。
     */
    private void initializeLookAndFeel() {
        log.info("Initializing application look and feel");
        SimpleThemeManager.initTheme();
        log.info("Application look and feel initialized");
    }

    private void startMainFrame(StartupCoordinator startupCoordinator) {
        boolean splashEnabled = SettingManager.isStartupSplashEnabled();
        log.info("Starting main frame initialization: splashEnabled={}", splashEnabled);
        if (splashEnabled) {
            // Splash Ở chế độ này, cửa sổ chuyển tiếp nhẹ và cửa sổ chính được hiển thị đầu tiên shell Chuyển đổi sau khi chuẩn bị xong.
            startWithSplash(startupCoordinator);
            return;
        }
        // IOC Khởi tạo song song với chủ đề; cửa sổ chính được tạo và hiển thị sau khi plugin sẵn sàng.
        new NoSplashStartupWorker(startupCoordinator).start();
    }

    private void startWithSplash(StartupCoordinator startupCoordinator) {
        SplashWindow splash = new SplashWindow();
        splash.init();
        splash.startMainFrameInitialization(startupCoordinator);
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("JVM shutdown hook started: lastStartupCheckpoint={}", startupCheckpoint);
                if (!skipHostRuntimeShutdown) {
                    // Khi thoát, trước tiên hãy đóng thời gian chạy của trình cắm rồi hủy máy chủ IOC，Ngăn chặn các cuộc gọi lại trình cắm truy cập vào các cuộc gọi lại đã phát hành Bean。
                    runShutdownStep("plugin runtime", PluginRuntime::shutdown);
                    runShutdownStep("host IOC container", BeanFactory::destroy);
                }
                runShutdownStep("single-instance coordinator", AppLauncher::closeSingleInstanceCoordinator);
                log.info("Application shutdown completed");
            } finally {
                // Cuối cùng dừng bối cảnh nhật ký và kích hoạt AsyncAppender bị giới hạn flush，Giữ nhật ký giai đoạn tắt máy.
                if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
                    loggerContext.stop();
                }
            }
        }, "ShutdownHook"));
    }

    private void runShutdownStep(String resource, Runnable shutdown) {
        try {
            shutdown.run();
        } catch (Throwable e) {
            log.error("Error shutting down {}", resource, e);
        }
    }

    private void closeSingleInstanceCoordinator() {
        SingleInstanceCoordinator coordinator = singleInstanceCoordinator;
        singleInstanceCoordinator = null;
        if (coordinator != null) {
            coordinator.close();
        }
    }
}
