package com.laker.postman.frame;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.placeholder.StartupShellPlaceholderPanel;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.panel.MainPanel;
import com.laker.postman.panel.lifecycle.AppExitCoordinator;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * Lớp cửa sổ chính, được kế thừa từ JFrame。
 */
@Slf4j
public class MainFrame extends JFrame {
    private final transient MainFrameStartupLifecycle startupLifecycle;
    private final transient MainWindowStateController windowStateController;
    private transient JPanel startupShellPanel;
    private boolean closing;

    // Chế độ Singleton đảm bảo rằng chỉ có một phiên bản
    private MainFrame() {
        super();
        startupLifecycle = new MainFrameStartupLifecycle();
        windowStateController = new MainWindowStateController(this);

        setName(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setTitle(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setIconImage(Icons.LOGO.getImage());
        MainWindowChrome.applyInitialDecorations(this);
    }

    public void initComponents() {
        setJMenuBar(UiSingletonFactory.getInstance(TopMenuBar.class));
        initStartupShell();
    }

    private void initStartupShell() {
        installStartupShell();

        // Đặt kích thước cửa sổ tối thiểu để ngăn cửa sổ bị kéo quá nhỏ
        Dimension minSize = windowStateController.getMinWindowSize();
        setMinimumSize(minSize);
        if (startupShellPanel != null) {
            startupShellPanel.setPreferredSize(windowStateController.getInitialWindowSize());
        }

        windowStateController.initWindowSize();
        initWindowCloseListener();
        windowStateController.installStateListeners();

        // Được sử dụng nếu không có trạng thái cửa sổ được lưu pack() Kích thước thành phần thích ứng
        if (!windowStateController.hasSavedWindowState()) {
            pack();
        }

        // Chỉ căn giữa cửa sổ ở trạng thái không được tối đa hóa để tránh phải nhảy thêm trên đường khôi phục tối đa hóa
        boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        if (!isMaximized) {
            setLocationRelativeTo(null);
        }
        log.info("MainFrame component initialization completed: bounds={}, state={}",
                getBounds(), getExtendedState());
    }

    public void loadMainContentAsync() {
        if (!startupLifecycle.markMainContentLoadRequested()) {
            return;
        }
        // Nội dung chính được hoãn lại để tải cho đến khi shell khởi động được hiển thị để tránh bị chặn bởi quá trình khởi tạo không gian làm việc hoàn chỉnh khi cửa sổ được hiển thị lần đầu tiên.
        Runnable task = () -> {
            if (closing) {
                return;
            }
            try {
                log.info("Main content initialization started");
                replaceStartupShellWithContent(UiSingletonFactory.getInstance(MainPanel.class));
                startupShellPanel = null;
                startupLifecycle.markMainContentLoaded();
                log.info("Main content initialization completed");
            } catch (Throwable throwable) {
                log.error("Failed to initialize main content", throwable);
                startupLifecycle.markMainContentLoadFailed(throwable);
            }
        };

        SwingUtilities.invokeLater(task);
    }

    private void installStartupShell() {
        startupShellPanel = createStartupShellPanel();
        setContentPane(startupShellPanel);
        MainWindowChrome.applyBackground(this);
    }

    public void refreshWindowChrome() {
        MainWindowChrome.refresh(this);
    }

    private JPanel createStartupShellPanel() {
        JPanel root = new StartupShellPlaceholderPanel() {
            private boolean firstPaintHandled;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!firstPaintHandled) {
                    firstPaintHandled = true;
                    startupLifecycle.markStartupShellPainted();
                }
            }
        };
        root.setOpaque(true);
        root.setBackground(ModernColors.getBackgroundColor());
        return root;
    }

    private void replaceStartupShellWithContent(Container nextContentPane) {
        setContentPane(nextContentPane);
        MainWindowChrome.applyBackground(this);
        revalidate();
        repaint();
    }

    public void whenMainContentLoaded(Runnable callback) {
        startupLifecycle.whenMainContentLoaded(callback);
    }

    public void whenMainContentLoadFailed(Consumer<Throwable> callback) {
        startupLifecycle.whenMainContentLoadFailed(callback);
    }

    public void whenStartupShellPainted(Runnable callback) {
        startupLifecycle.whenStartupShellPainted(callback);
    }

    private void initWindowCloseListener() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                BeanFactory.getBean(AppExitCoordinator.class).exitApplication();
            }
        });
    }

    /**
     * Sau khi người dùng xác nhận thoát, trạng thái cửa sổ sẽ được lưu và các hoạt động trong nền cửa sổ sẽ dừng lại.
     */
    public void prepareForExit() {
        closing = true;
        windowStateController.stop();
        windowStateController.saveWindowState();
        setEnabled(false);
    }
}
