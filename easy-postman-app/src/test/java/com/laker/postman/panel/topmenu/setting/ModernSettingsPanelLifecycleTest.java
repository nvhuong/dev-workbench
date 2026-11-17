package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ModernSettingsPanelLifecycleTest {

    @Test(description = "ModernSettingsPanel Các lớp con không nên được gọi trong giai đoạn xây dựng của lớp cha buildContent")
    public void testSubclassFieldsAreAvailableWhenBuildContentRuns() {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();

        assertTrue(panel.buildContentCalled);
        assertTrue(panel.registerListenersCalled);
        assertEquals(panel.fieldCountAtBuildContent, 1);
    }

    @Test(description = "Chiều cao mặc định của hộp thoại cài đặt phải phù hợp với các trường mới trên trang kiểm tra sức chịu đựng")
    public void settingsDialogPreferredHeightShouldFitPerformanceSettings() throws Exception {
        Field heightField = ModernSettingsDialog.class.getDeclaredField("PREFERRED_HEIGHT");
        heightField.setAccessible(true);

        assertTrue(heightField.getInt(null) >= 640);
    }

    @Test(description = "Đặt hộp thoại có kích thước cố định để tránh bị cắt nội dung sau khi người dùng kéo")
    public void settingsDialogShouldDisableResize() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("setResizable(false)"));
        assertFalse(source.contains("setResizable(true)"));
    }

    @Test(description = "Cài đặt khởi tạo và chuyển đổi trang tab nên quay lại đầu nội dung")
    public void settingsPanelShouldResetMainScrollPaneToTop() throws Exception {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();
        JScrollPane scrollPane = findScrollPane(panel);

        scrollPane.getViewport().setViewPosition(new Point(0, 120));
        panel.resetScrollPositionToTop();
        SwingUtilities.invokeAndWait(() -> {
        });

        assertEquals(scrollPane.getViewport().getViewPosition().y, 0);
    }

    @Test(description = "Đặt nội dung chính của trang nên tuân theo viewport Chiều rộng để ngăn nội dung phù hợp bị cắt")
    public void settingsPanelContentShouldTrackViewportWidth() {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();
        JScrollPane scrollPane = findScrollPane(panel);

        assertTrue(scrollPane.getViewport().getView() instanceof Scrollable);
        Scrollable view = (Scrollable) scrollPane.getViewport().getView();
        assertTrue(view.getScrollableTracksViewportWidth());
    }

    @Test(description = "Nội dung trang cài đặt ngắn hơn viewport Nó phải được lấp đầy đến hết chiều cao để tránh khoảng trống màu xám vô nghĩa ở phía dưới")
    public void settingsPanelContentShouldTrackViewportHeightWhenShorterThanViewport() {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();
        JScrollPane scrollPane = findScrollPane(panel);

        JViewport viewport = scrollPane.getViewport();
        Component viewComponent = viewport.getView();
        Scrollable view = (Scrollable) viewComponent;
        Dimension preferredSize = viewComponent.getPreferredSize();

        viewport.setSize(new Dimension(preferredSize.width, preferredSize.height + 80));
        assertTrue(view.getScrollableTracksViewportHeight());

        viewport.setSize(new Dimension(preferredSize.width, Math.max(1, preferredSize.height - 80)));
        assertFalse(view.getScrollableTracksViewportHeight());
    }

    private static JScrollPane findScrollPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
            if (component instanceof Container childContainer) {
                try {
                    return findScrollPane(childContainer);
                } catch (IllegalStateException ignored) {
                    // Keep scanning sibling components.
                }
            }
        }
        throw new IllegalStateException("No JScrollPane found");
    }

    private static final class SampleSettingsPanel extends ModernSettingsPanel {
        private final List<String> values = List.of("ready");
        private boolean buildContentCalled;
        private boolean registerListenersCalled;
        private int fieldCountAtBuildContent;

        @Override
        protected void buildContent(JPanel contentPanel) {
            buildContentCalled = true;
            fieldCountAtBuildContent = values.size();
            contentPanel.add(Box.createVerticalStrut(400));
        }

        @Override
        protected void registerListeners() {
            registerListenersCalled = true;
        }
    }
}
