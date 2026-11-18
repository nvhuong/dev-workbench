package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.service.sync.WebDavSyncScheduler;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Điều phối các bước khởi tạo liên quan đến cửa sổ chính trong quá trình khởi động ứng dụng.
 */
@Slf4j
public class StartupCoordinator {
    private final long launchStartedAtNanos;
    private CompletableFuture<Void> hostIocInitialization;

    public StartupCoordinator(long launchStartedAtNanos) {
        this.launchStartedAtNanos = launchStartedAtNanos;
    }

    /**
     * IOC Chỉ quét để đăng ký Bean Định nghĩa, và Swing Việc cài đặt chủ đề không phụ thuộc vào nhau và có thể được thực hiện cùng một lúc.
     */
    public synchronized void startHostIocInitialization() {
        if (hostIocInitialization != null) {
            return;
        }
        hostIocInitialization = CompletableFuture.runAsync(() -> {
            long startedAt = System.nanoTime();
            AppLauncher.markStartupCheckpoint("initializing host IOC container");
            log.info("GUI startup stage: initializing host IOC container");
            GuiStartupBootstrap.initBeanFactory();
            AppLauncher.markStartupCheckpoint("host IOC container initialized");
            log.info("GUI startup stage complete: host IOC container initialized in {} ms",
                    (System.nanoTime() - startedAt) / 1_000_000);
            long workspaceStartedAt = System.nanoTime();
            GuiStartupBootstrap.initWorkspaceState();
            log.info("GUI startup stage complete: workspace state initialized in {} ms",
                    (System.nanoTime() - workspaceStartedAt) / 1_000_000);
        }, task -> {
            Thread thread = new Thread(task, "GUI-IOC-Startup");
            thread.setDaemon(true);
            thread.start();
        });
    }

    public void initializePluginRuntimeAfterHostReady() {
        awaitHostIocInitialization();
        long startedAt = System.nanoTime();
        AppLauncher.markStartupCheckpoint("initializing plugin runtime");
        log.info("GUI startup stage: initializing plugin runtime");
        GuiStartupBootstrap.initPluginRuntime();
        AppLauncher.markStartupCheckpoint("plugin runtime initialized");
        log.info("GUI startup stage complete: plugin runtime initialized in {} ms",
                (System.nanoTime() - startedAt) / 1_000_000);
    }

    private void awaitHostIocInitialization() {
        startHostIocInitialization();
        hostIocInitialization.join();
    }

    public MainFrame prepareMainFrameShell(StartupProgressListener progressListener) throws Exception {
        notifyProgress(progressListener, StartupStage.STARTING);
        awaitHostIocInitialization();
        notifyProgress(progressListener, StartupStage.LOADING_PLUGINS);
        initializePluginRuntimeAfterHostReady();

        notifyProgress(progressListener, StartupStage.LOADING_MAIN);
        AppLauncher.markStartupCheckpoint("creating main frame on EDT");
        log.info("GUI startup stage: creating and initializing main frame on EDT");
        long frameStartedAt = System.nanoTime();
        MainFrame mainFrame = createAndInitializeMainFrameOnEdt();
        AppLauncher.markStartupCheckpoint("main frame initialized");
        log.info("GUI startup stage complete: main frame initialized in {} ms",
                (System.nanoTime() - frameStartedAt) / 1_000_000);

        notifyProgress(progressListener, StartupStage.READY);
        return mainFrame;
    }

    public void showMainFrameAndLoadContent(MainFrame mainFrame) {
        if (mainFrame == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            showMainFrameAndLoadContentOnEdt(mainFrame);
            return;
        }
        SwingUtilities.invokeLater(() -> showMainFrameAndLoadContentOnEdt(mainFrame));
    }

    public void scheduleBackgroundUpdateCheck() {
        StartupUpdateScheduler.scheduleBackgroundUpdateCheck();
    }

    public void scheduleBackgroundTasks() {
        scheduleBackgroundUpdateCheck();
        BeanFactory.getBean(WebDavSyncScheduler.class).start();
    }

    public void runAfterMainContentReady(MainFrame mainFrame,
                                         Runnable onReady,
                                         Consumer<Throwable> onFailure) {
        waitForMainFrameReadiness(mainFrame, true, onReady, onFailure);
    }

    public void runAfterStartupShellReady(MainFrame mainFrame,
                                          Runnable onReady,
                                          Consumer<Throwable> onFailure) {
        waitForMainFrameReadiness(mainFrame, false, onReady, onFailure);
    }

    private void waitForMainFrameReadiness(MainFrame mainFrame,
                                           boolean waitForMainContent,
                                           Runnable onReady,
                                           Consumer<Throwable> onFailure) {
        new MainFrameReadinessWatcher(mainFrame, waitForMainContent, onReady, onFailure).start();
    }

    private void notifyProgress(StartupProgressListener progressListener, StartupStage stage) {
        if (progressListener != null && stage != null) {
            progressListener.onStageChanged(stage);
        }
    }

    private MainFrame createAndInitializeMainFrameOnEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return createMainFrame();
        }

        MainFrame[] mainFrameHolder = new MainFrame[1];
        Throwable[] errorHolder = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                mainFrameHolder[0] = createMainFrame();
            } catch (Throwable throwable) {
                errorHolder[0] = throwable;
            }
        });

        if (errorHolder[0] != null) {
            if (errorHolder[0] instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(errorHolder[0]);
        }
        return mainFrameHolder[0];
    }

    private MainFrame createMainFrame() {
        log.info("Constructing main frame");
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        log.info("Initializing main frame components");
        mainFrame.initComponents();
        log.info("Main frame components initialized");
        return mainFrame;
    }

    private void showMainFrameAndLoadContentOnEdt(MainFrame mainFrame) {
        showMainFrameOnEdt(mainFrame);
        log.info("Scheduling main content loading");
        // Để shell khởi động nhẹ hoàn tất việc hiển thị khung hình đầu tiên trước, sau đó chuyển sang nội dung chính hoàn chỉnh để giảm tình trạng chặn màn hình đầu tiên.
        SwingUtilities.invokeLater(mainFrame::loadMainContentAsync);
    }

    private void showMainFrameOnEdt(MainFrame mainFrame) {
        log.info("Showing main frame on EDT");
        mainFrame.whenMainContentLoaded(() -> {
            AppLauncher.markStartupCheckpoint("main content ready");
            log.info("GUI main content ready in {} ms since launch entry",
                    (System.nanoTime() - launchStartedAtNanos) / 1_000_000);
        });
        mainFrame.setVisible(true);
        AppLauncher.markStartupCheckpoint("main frame made visible");
        AppSingleInstanceController.registerReadyMainFrame(mainFrame);
        mainFrame.toFront();
        mainFrame.requestFocus();
        log.info("Main frame is visible in {} ms since launch entry",
                (System.nanoTime() - launchStartedAtNanos) / 1_000_000);
    }

    public enum StartupStage {
        STARTING,
        LOADING_PLUGINS,
        LOADING_MAIN,
        READY
    }

    @FunctionalInterface
    public interface StartupProgressListener {
        void onStageChanged(StartupStage stage);
    }
}
