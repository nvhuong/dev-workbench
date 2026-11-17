package com.laker.postman.frame;

import com.laker.postman.util.UserPreferencesStore;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Quản lý việc lưu và khôi phục kích thước cửa sổ chính và trạng thái tối đa cũng như ghi lại thông tin chẩn đoán vị trí và màn hình chéo.
 */
@Slf4j
class MainWindowStateController {
    private static final int SAVE_DEBOUNCE_DELAY_MS = 500;

    private final JFrame frame;
    private final Dimension fallbackScreenSize;
    private final Timer saveStateTimer;
    private Dimension cachedMinWindowSize;
    private String lastDisplayId;

    MainWindowStateController(JFrame frame) {
        this.frame = frame;
        fallbackScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        saveStateTimer = new Timer(SAVE_DEBOUNCE_DELAY_MS, e -> saveWindowState());
        saveStateTimer.setRepeats(false);
    }

    Dimension getMinWindowSize() {
        if (cachedMinWindowSize == null) {
            return recalculateMinimumWindowSize();
        }
        return new Dimension(cachedMinWindowSize);
    }

    Dimension getInitialWindowSize() {
        return initialWindowSize(getMinWindowSize());
    }

    private Dimension initialWindowSize(Dimension minimumSize) {
        Dimension usableArea = defaultUsableDisplayArea();
        Dimension preferredSize = MainWindowSizePolicy.preferredSizeForScreenWidth(usableArea.getWidth());
        return MainWindowSizePolicy.constrainToUsableArea(
                preferredSize, minimumSize, usableArea);
    }

    void initWindowSize() {
        if (hasSavedWindowState()) {
            restoreWindowState();
            return;
        }

        Dimension usableArea = defaultUsableDisplayArea();
        Dimension initialSize = getInitialWindowSize();
        frame.setSize(initialSize);
        if (MainWindowSizePolicy.shouldStartMaximized(usableArea.getWidth())) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        log.info("Cửa sổ chính sử dụng trạng thái ban đầu: size={}, state={}, display={}",
                initialSize, describeState(frame.getExtendedState()), currentDisplaySnapshot());
    }

    boolean hasSavedWindowState() {
        return UserPreferencesStore.hasWindowState();
    }

    void installStateListeners() {
        lastDisplayId = currentDisplaySnapshot().id();
        frame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                logDisplayChangeIfNeeded();
                log.info("Trạng thái của cửa sổ chính thay đổi: oldState={}, newState={}, frameBounds={}, display={}",
                        describeState(e.getOldState()), describeState(e.getNewState()),
                        frame.getBounds(), currentDisplaySnapshot());
                scheduleSaveWindowState();
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scheduleSaveWindowStateIfVisible();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                logDisplayChangeIfNeeded();
                scheduleSaveWindowStateIfVisible();
            }
        });
    }

    void scheduleSaveWindowState() {
        if (saveStateTimer.isRunning()) {
            saveStateTimer.restart();
        } else {
            saveStateTimer.start();
        }
    }

    void saveWindowState() {
        try {
            Dimension minSize = refreshFrameMinimumSize();
            boolean isMaximized = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            Dimension initialSize = initialWindowSize(minSize);

            int width;
            int height;
            if (isMaximized) {
                width = savedWindowWidthOrDefault(initialSize.width, minSize.width);
                height = savedWindowHeightOrDefault(initialSize.height, minSize.height);
            } else {
                Dimension size = frame.getSize();
                width = Math.max(size.width, minSize.width);
                height = Math.max(size.height, minSize.height);
            }

            int extendedState = frame.getExtendedState();
            UserPreferencesStore.saveWindowState(width, height, extendedState);
            if (log.isDebugEnabled()) {
                log.debug("Đã lưu ảnh chụp nhanh trạng thái cửa sổ chính: persistedNormalSize={}x{}, frameBounds={}, state={}, "
                                + "minimumSize={}, display={}",
                        width, height, frame.getBounds(), describeState(extendedState),
                        minSize, currentDisplaySnapshot());
            }
        } catch (Exception e) {
            log.warn("Không lưu được trạng thái cửa sổ", e);
        }
    }

    void stop() {
        if (saveStateTimer.isRunning()) {
            saveStateTimer.stop();
        }
    }

    private Dimension refreshFrameMinimumSize() {
        Dimension minimumSize = recalculateMinimumWindowSize();
        if (!minimumSize.equals(frame.getMinimumSize())) {
            frame.setMinimumSize(minimumSize);
        }
        return minimumSize;
    }

    private void scheduleSaveWindowStateIfVisible() {
        if (frame.isVisible()) {
            scheduleSaveWindowState();
        }
    }

    private int savedWindowWidthOrDefault(int defaultWidth, int minimumWidth) {
        if (!hasSavedWindowState()) {
            return defaultWidth;
        }
        Integer savedWidth = UserPreferencesStore.getWindowWidth();
        return (savedWidth != null && savedWidth > 0)
                ? Math.max(savedWidth, minimumWidth)
                : defaultWidth;
    }

    private int savedWindowHeightOrDefault(int defaultHeight, int minimumHeight) {
        if (!hasSavedWindowState()) {
            return defaultHeight;
        }
        Integer savedHeight = UserPreferencesStore.getWindowHeight();
        return (savedHeight != null && savedHeight > 0)
                ? Math.max(savedHeight, minimumHeight)
                : defaultHeight;
    }

    private void restoreWindowState() {
        try {
            Integer width = UserPreferencesStore.getWindowWidth();
            Integer height = UserPreferencesStore.getWindowHeight();
            Integer extendedState = UserPreferencesStore.getWindowExtendedState();

            Dimension minSize = getMinWindowSize();
            Dimension initialSize = getInitialWindowSize();
            Dimension requestedSize = new Dimension(
                    (width != null && width > 0) ? width : initialSize.width,
                    (height != null && height > 0) ? height : initialSize.height);
            Dimension actualSize = MainWindowSizePolicy.constrainToUsableArea(
                    requestedSize, minSize, defaultUsableDisplayArea());
            int actualState = (extendedState != null) ? extendedState : Frame.NORMAL;

            if ((actualState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                frame.setSize(actualSize);
                frame.setLocationRelativeTo(null);
                frame.setExtendedState(actualState);
            } else {
                frame.setSize(actualSize);
                frame.setExtendedState(actualState);
            }

            log.info("Trạng thái cửa sổ chính đã được khôi phục: savedSize={}, actualSize={}, state={}, display={}",
                    requestedSize, actualSize, describeState(actualState), currentDisplaySnapshot());
        } catch (Exception e) {
            log.warn("Không thể khôi phục trạng thái cửa sổ", e);
        }
    }

    private Dimension recalculateMinimumWindowSize() {
        List<DisplaySnapshot> displays = connectedDisplaySnapshots();
        Dimension[] usableDisplayAreas = displays.stream()
                .map(DisplaySnapshot::usableBounds)
                .map(Rectangle::getSize)
                .toArray(Dimension[]::new);
        Dimension minimumSize = MainWindowSizePolicy.minimumSizeForDisplayAreas(usableDisplayAreas);
        if (!minimumSize.equals(cachedMinWindowSize)) {
            log.info("Kích thước tối thiểu của cửa sổ chính đã được cập nhật: previous={}, current={}, displays={}",
                    cachedMinWindowSize, minimumSize, displays);
        }
        cachedMinWindowSize = minimumSize;
        return new Dimension(minimumSize);
    }

    private List<DisplaySnapshot> connectedDisplaySnapshots() {
        List<DisplaySnapshot> displays = new ArrayList<>();
        try {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice screenDevice : screenDevices) {
                try {
                    displays.add(displaySnapshot(screenDevice.getDefaultConfiguration()));
                } catch (RuntimeException e) {
                    log.debug("Không đọc được thông tin hiển thị: display={}", screenDevice.getIDstring(), e);
                }
            }
        } catch (RuntimeException e) {
            log.debug("Không thể đọc vùng có sẵn trên nhiều màn hình, sử dụng kích thước màn hình mặc định", e);
        }

        if (displays.isEmpty()) {
            displays.add(fallbackDisplaySnapshot());
        }
        return displays;
    }

    private Dimension defaultUsableDisplayArea() {
        return currentDisplaySnapshot().usableBounds().getSize();
    }

    private DisplaySnapshot currentDisplaySnapshot() {
        try {
            GraphicsConfiguration graphicsConfiguration = frame.getGraphicsConfiguration();
            if (graphicsConfiguration == null) {
                graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration();
            }
            return displaySnapshot(graphicsConfiguration);
        } catch (RuntimeException e) {
            log.debug("Không thể đọc vùng màn hình mặc định có sẵn, sử dụng kích thước màn hình mặc định", e);
            return fallbackDisplaySnapshot();
        }
    }

    private DisplaySnapshot displaySnapshot(GraphicsConfiguration graphicsConfiguration) {
        Rectangle bounds = new Rectangle(graphicsConfiguration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        Rectangle usableBounds = new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                Math.max(1, bounds.width - insets.left - insets.right),
                Math.max(1, bounds.height - insets.top - insets.bottom));
        AffineTransform transform = graphicsConfiguration.getDefaultTransform();
        return new DisplaySnapshot(
                graphicsConfiguration.getDevice().getIDstring(),
                bounds,
                usableBounds,
                transform.getScaleX(),
                transform.getScaleY());
    }

    private DisplaySnapshot fallbackDisplaySnapshot() {
        Rectangle fallbackBounds = new Rectangle(fallbackScreenSize);
        return new DisplaySnapshot("fallback", fallbackBounds, fallbackBounds, 1.0, 1.0);
    }

    private void logDisplayChangeIfNeeded() {
        GraphicsConfiguration graphicsConfiguration = frame.getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            return;
        }
        String currentDisplayId = graphicsConfiguration.getDevice().getIDstring();
        if (Objects.equals(lastDisplayId, currentDisplayId)) {
            return;
        }
        DisplaySnapshot display = displaySnapshot(graphicsConfiguration);
        log.info("Cửa sổ chính chuyển màn hình: from={}, to={}, frameBounds={}, display={}",
                lastDisplayId, display.id(), frame.getBounds(), display);
        lastDisplayId = display.id();
    }

    private String describeState(int state) {
        if (state == Frame.NORMAL) {
            return "NORMAL(0)";
        }

        List<String> states = new ArrayList<>();
        if ((state & Frame.ICONIFIED) == Frame.ICONIFIED) {
            states.add("ICONIFIED");
        }
        if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            states.add("MAXIMIZED_BOTH");
        } else {
            if ((state & Frame.MAXIMIZED_HORIZ) == Frame.MAXIMIZED_HORIZ) {
                states.add("MAXIMIZED_HORIZ");
            }
            if ((state & Frame.MAXIMIZED_VERT) == Frame.MAXIMIZED_VERT) {
                states.add("MAXIMIZED_VERT");
            }
        }
        return String.join("|", states) + "(" + state + ")";
    }

    private record DisplaySnapshot(String id,
                                   Rectangle bounds,
                                   Rectangle usableBounds,
                                   double scaleX,
                                   double scaleY) {
    }
}
