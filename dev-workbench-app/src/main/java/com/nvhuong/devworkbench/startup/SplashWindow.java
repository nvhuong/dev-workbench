package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.common.constants.Icons;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;

/**
 * Khởi chạy cửa sổ chào mừng (Splash Window），Được sử dụng để chuyển tiếp khi chương trình chính được tải.
 * Sử dụng không viền JFrame thay vào đó JWindow，thấp hơn macOS Chi phí bổ sung của việc tạo cửa sổ cấp cao nhất lần đầu tiên.
 */
@Slf4j
class SplashWindow extends JFrame {
    @Serial
    private static final long serialVersionUID = 1L;
    private JLabel statusLabel;
    private volatile boolean isDisposed = false;

    public SplashWindow() {
        super();
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * Vào EDT Được khởi tạo trong chuỗi UI thành phần.
     */
    public void init() {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                initUI();
            } else {
                SwingUtilities.invokeAndWait(this::initUI);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Khôi phục trạng thái bị gián đoạn
            log.error("khởi tạo SplashWindow đã bị gián đoạn", e);
            dispose();
            throw new SplashWindowInitializationException("Failed to initialize splash window", e);
        } catch (Exception e) {
            log.error("khởi tạo SplashWindow thất bại", e);
            // Nếu quá trình khởi tạo không thành công, hãy đảm bảo cửa sổ được giải phóng chính xác
            dispose();
            throw new SplashWindowInitializationException("Failed to initialize splash window", e);
        }
    }

    /**
     * khởi tạo UI thành phần
     */
    private void initUI() {
        JPanel content = createContentPanel();
        initializeWindow(content);
    }

    /**
     * Tạo bảng nội dung chính
     */
    private JPanel createContentPanel() {
        JPanel content = createSplashContentPanel();

        content.add(createLogoPanel(), BorderLayout.CENTER);

        content.add(createInfoPanel(), BorderLayout.NORTH);

        content.add(createStatusPanel(), BorderLayout.SOUTH);
        return content;
    }

    /**
     * TạoLogobảng điều khiển
     */
    private JPanel createLogoPanel() {
        // Tạo một bảng chứa để vẽ nền hình tròn
        JPanel logoContainer = new JPanel() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 100; // Giảm kích thước vòng tròn
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Sơn quầng sáng bên ngoài
                g2.setColor(getDecorativeDotColor(25));
                g2.fillOval(x - 6, y - 6, size + 12, size + 12);

                // Vẽ một ánh sáng ở mức độ trung bình
                g2.setColor(getDecorativeDotColor(40));
                g2.fillOval(x - 4, y - 4, size + 8, size + 8);

                // Vẽ nền hình tròn
                g2.setColor(getDecorativeDotColor(95));
                g2.fillOval(x, y, size, size);

                // Sơn nội thất với các bóng tinh tế để thêm kích thước
                g2.setColor(getDecorativeDotColor(15));
                g2.fillOval(x + 2, y + 2, size - 4, size - 4);

                // Vẽ các điểm nổi bật ở đường viền
                g2.setColor(ModernColors.withAlpha(ModernColors.getTextInverse(), 120));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);

                g2.dispose();
            }
        };
        logoContainer.setOpaque(false);
        logoContainer.setLayout(new BorderLayout());
        // Thay đổi kích thước vùng chứa (100 + 12 = 112，Đặt thành 120 để lại lề)
        logoContainer.setPreferredSize(new Dimension(120, 120));

        Image scaledImage = Icons.LOGO.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        ImageIcon logoIcon = new ImageIcon(scaledImage);
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoContainer.add(logoLabel, BorderLayout.CENTER);

        return logoContainer;
    }

    /**
     * Tạo bảng thông tin ứng dụng
     */
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        infoPanel.setOpaque(false);

        // Tên ứng dụng
        JLabel appNameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.APP_NAME), SwingConstants.CENTER);
        appNameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 8));
        appNameLabel.setForeground(getTextColor());
        infoPanel.add(appNameLabel);

        // Số phiên bản
        JLabel versionLabel = new JLabel(SystemUtil.getCurrentVersion(), SwingConstants.CENTER);
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 4));
        versionLabel.setForeground(getTextColor());
        infoPanel.add(versionLabel);

        return infoPanel;
    }

    /**
     * Tạo bảng trạng thái
     */
    private JPanel createStatusPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 3));
        bottomPanel.setOpaque(false);
        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SPLASH_STATUS_STARTING), SwingConstants.CENTER);
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 3)); // Lớn hơn kích thước phông chữ tiêu chuẩn3Không
        statusLabel.setForeground(getTextColor());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    /**
     * Cửa sổ khởi tạo
     */
    private void initializeWindow(JPanel content) {
        setContentPane(content);
        setSize(340, 250); // Đặt kích thước cửa sổ
        setLocationRelativeTo(null);  // Hiển thị ở giữa

        setupWindowProperties();

        setVisible(true); // Hiển thị cửa sổ
        log.info("Splash window is visible");
    }

    /**
     * Thuộc tính cửa sổ cài đặt bảo mật
     */
    private void setupWindowProperties() {
        try {
            setBackground(new Color(0, 0, 0, 0)); // Nền trong suốt
        } catch (Exception e) {
            log.warn("Không thể đặt nền trong suốt, nền mặc định được sử dụng", e);
        }

        try {
            setAlwaysOnTop(true); // Cửa sổ luôn ở trên cùng
        } catch (Exception e) {
            log.warn("Không thể đặt cửa sổ lên trên cùng", e);
        }
    }

    private static JPanel createSplashContentPanel() {
        JPanel content = new JPanel() { // Tùy chỉnh bảng điều khiển, vẽ nền gradient và các góc tròn
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color gradientStart = ModernColors.getSplashGradientStartColor();
                Color gradientEnd = ModernColors.getSplashGradientEndColor();
                GradientPaint gp = new GradientPaint(
                        0, 0, gradientStart,
                        getWidth(), getHeight(), gradientEnd
                );
                g2d.setPaint(gp);
                // Nền các góc tròn
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);

                // Thêm hiệu ứng bóng tinh tế
                Color highlightStart = ModernColors.withAlpha(ModernColors.getTextInverse(), 40);
                Color highlightEnd = ModernColors.withAlpha(ModernColors.getTextInverse(), 0);
                GradientPaint glossPaint = new GradientPaint(
                        0, 0, highlightStart,
                        0, getHeight() / 2.0f, highlightEnd
                );
                g2d.setPaint(glossPaint);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 32, 32);

                Color borderColor = ModernColors.withAlpha(ModernColors.getBorderMediumColor(), 100);
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 32, 32);

                g2d.dispose();
            }
        };
        content.setLayout(new BorderLayout(0, 10)); // sử dụng BorderLayout cách trình bày
        content.setOpaque(false); // Đặt nền trong suốt
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24)); // Đặt phần đệm
        return content;
    }

    public void setStatus(String statusKey) {
        if (!isDisposed) {
            SwingUtilities.invokeLater(() -> updateStatusLabel(statusKey));
        }
    }

    /**
     * Cập nhật nhãn trạng thái
     */
    private void updateStatusLabel(String statusKey) {
        if (!isDisposed && statusLabel != null) {
            statusLabel.setText(I18nUtil.getMessage(statusKey));
        }
    }

    public void startMainFrameInitialization(StartupCoordinator startupCoordinator) {
        new SplashStartupWorker(this, startupCoordinator).execute();
    }

    /**
     * Xử lý lỗi tải cửa sổ chính
     */
    void handleMainFrameLoadError(Throwable e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        StartupFailureHandler.showStartupErrorAndExit(e);
    }

    /**
     * Bắt đầu hoạt ảnh mờ dần
     */
    void startFadeOutAnimation(MainFrame mainFrame, StartupCoordinator startupCoordinator) {
        if (isDisposed) return;

        // Cửa sổ chính được hiển thị đầu tiên; Sau khi hoàn thành bản vẽ ổn định của trình giữ chỗ, nó sẽ thoát ra càng sớm càng tốt mà không bị chặn và chờ nội dung chính được tải đầy đủ.
        startupCoordinator.showMainFrameAndLoadContent(mainFrame);
        startupCoordinator.runAfterStartupShellReady(
                mainFrame,
                () -> closeSplash(startupCoordinator),
                this::handleMainFrameLoadError
        );
    }

    boolean isDisposed() {
        return isDisposed;
    }

    private void closeSplash(StartupCoordinator startupCoordinator) {
        disposeSafely();
        startupCoordinator.scheduleBackgroundTasks();
    }

    /**
     * Giải phóng tài nguyên một cách an toàn
     */
    private void disposeSafely() {
        if (isDisposed) return;

        isDisposed = true;

        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
        });
    }

    /**
     * Viết lạidisposephương pháp để đảm bảo rằng tài nguyên được giải phóng đầy đủ
     */
    @Override
    public void dispose() {
        if (!isDisposed) {
            disposeSafely();
        } else {
            super.dispose();
        }
    }

    /**
     * Nhận màu văn bản phù hợp với chủ đề
     */
    private Color getTextColor() {
        return ModernColors.withAlpha(ModernColors.getTextInverse(), 220);
    }

    /**
     * Lấy màu điểm trang trí phù hợp với chủ đề
     */
    private Color getDecorativeDotColor(int alpha) {
        return ModernColors.withAlpha(ModernColors.getTextInverse(), alpha);
    }
}
