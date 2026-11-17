package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.mock.MockServerPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.toolbox.ToolboxPanel;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Thanh bên Tab liệt kê
 * Quản lý mọi thứ một cách tập trung Tab Thông tin cấu hình để tránh trùng lặp mã
 */
@Getter
public enum SidebarTab {
    COLLECTIONS(
            MessageKeys.MENU_COLLECTIONS,
            "icons/collections.svg",
            () -> UiSingletonFactory.getInstance(RequestCollectionsPanel.class)
    ),
    ENVIRONMENTS(
            MessageKeys.MENU_ENVIRONMENTS,
            "icons/environments.svg",
            () -> UiSingletonFactory.getInstance(EnvironmentPanel.class)
    ),
    WORKSPACES(
            MessageKeys.MENU_WORKSPACES,
            "icons/workspace.svg",
            () -> UiSingletonFactory.getInstance(WorkspacePanel.class)
    ),
    MOCK_SERVER(
            MessageKeys.MENU_MOCK_SERVER,
            "icons/mock-server.svg",
            () -> UiSingletonFactory.getInstance(MockServerPanel.class)
    ),
    FUNCTIONAL(
            MessageKeys.MENU_FUNCTIONAL,
            "icons/functional.svg",
            () -> UiSingletonFactory.getInstance(FunctionalPanel.class)
    ),
    PERFORMANCE(
            MessageKeys.MENU_PERFORMANCE,
            "icons/performance.svg",
            () -> UiSingletonFactory.getInstance(PerformancePanel.class)
    ),
    TOOLBOX(
            MessageKeys.MENU_TOOLBOX,
            "icons/tools.svg",
            () -> UiSingletonFactory.getInstance(ToolboxPanel.class)
    ),
    HISTORY(
            MessageKeys.MENU_HISTORY,
            "icons/history.svg",
            () -> UiSingletonFactory.getInstance(HistoryPanel.class)
    );

    private final String titleKey;
    private final String iconPath;
    private final Supplier<JPanel> panelSupplier;

    // Bộ nhớ đệm tải chậm
    private Icon icon;
    private Icon selectedIcon;  // Biểu tượng được chọn ở trạng thái mở rộng
    private Icon selectedOnPrimaryIcon;  // Biểu tượng trên nền được chọn màu xanh
    private String title;
    private JPanel panel;

    SidebarTab(String titleKey, String iconPath, Supplier<JPanel> panelSupplier) {
        this.titleKey = titleKey;
        this.iconPath = iconPath;
        this.panelSupplier = panelSupplier;
    }

    /**
     * Lấy tiêu đề (lag tải, hỗ trợ quốc tế hóa)
     */
    public String getTitle() {
        if (title == null) {
            title = I18nUtil.getMessage(titleKey);
        }
        return title;
    }

    /**
     * Lấy tiêu đề bằng ngôn ngữ hiện tại mà không cần sử dụng bộ đệm, thuận tiện cho việc thiết lập trang và làm mới khi chạy.
     */
    public String getDisplayTitle() {
        return I18nUtil.getMessage(titleKey);
    }

    /**
     * Nhận biểu tượng (tải chậm để tránh tạo lại)- Trạng thái bình thường
     */
    public Icon getIcon() {
        if (icon == null) {
            icon = IconUtil.create(iconPath, IconUtil.SIZE_TAB, IconUtil.SIZE_TAB)
                    .setColorFilter(new FlatSVGIcon.ColorFilter(color ->
                            SidebarTheme.inactiveTabIconForeground()));
        }
        return icon;
    }

    public Icon getSelectedOnPrimaryIcon() {
        if (selectedOnPrimaryIcon == null) {
            selectedOnPrimaryIcon = IconUtil.createOnPrimary(iconPath, IconUtil.SIZE_TAB, IconUtil.SIZE_TAB);
        }
        return selectedOnPrimaryIcon;
    }

    public Icon getSelectedIcon() {
        if (selectedIcon == null) {
            selectedIcon = IconUtil.createPrimary(iconPath, IconUtil.SIZE_TAB, IconUtil.SIZE_TAB);
        }
        return selectedIcon;
    }

    /**
     * Lấy bảng điều khiển (tải chậm)
     */
    public JPanel getPanel() {
        if (panel == null) {
            panel = panelSupplier.get();
        }
        return panel;
    }

    /**
     * Chuyển đổi sang  TabInfo đối tượng
     */
    public TabInfo toTabInfo() {
        return new TabInfo(getDisplayTitle(), getIcon(), this::getPanel);
    }

    public static SidebarTab fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (SidebarTab tab : values()) {
            if (tab.name().equalsIgnoreCase(name.trim())) {
                return tab;
            }
        }
        return null;
    }

    public static List<SidebarTab> resolveOrderedTabs(List<String> orderedNames) {
        List<SidebarTab> resolvedTabs = new ArrayList<>();
        Set<SidebarTab> addedTabs = new HashSet<>();

        if (orderedNames != null) {
            for (String orderedName : orderedNames) {
                SidebarTab tab = fromName(orderedName);
                if (tab != null && addedTabs.add(tab)) {
                    resolvedTabs.add(tab);
                }
            }
        }

        for (SidebarTab tab : values()) {
            if (addedTabs.add(tab)) {
                resolvedTabs.add(tab);
            }
        }

        return resolvedTabs;
    }

    public static List<SidebarTab> resolveVisibleTabs(List<String> orderedNames, Set<String> hiddenNames) {
        List<SidebarTab> orderedTabs = resolveOrderedTabs(orderedNames);
        Set<String> normalizedHiddenNames = new HashSet<>();
        if (hiddenNames != null) {
            for (String hiddenName : hiddenNames) {
                if (hiddenName != null && !hiddenName.isBlank()) {
                    normalizedHiddenNames.add(hiddenName.trim().toUpperCase());
                }
            }
        }

        List<SidebarTab> visibleTabs = new ArrayList<>();
        for (SidebarTab tab : orderedTabs) {
            if (!normalizedHiddenNames.contains(tab.name())) {
                visibleTabs.add(tab);
            }
        }

        return visibleTabs.isEmpty() ? new ArrayList<>(List.of(values())) : visibleTabs;
    }

}
