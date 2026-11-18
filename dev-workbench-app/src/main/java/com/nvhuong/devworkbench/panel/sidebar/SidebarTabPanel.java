package com.nvhuong.devworkbench.panel.sidebar;

import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestSideAssistantPanel;
import com.nvhuong.devworkbench.panel.env.EnvironmentPanel;
import com.nvhuong.devworkbench.panel.performance.PerformanceUiWarmup;
import com.nvhuong.devworkbench.panel.sidebar.cookie.CookieManagerDialog;
import com.nvhuong.devworkbench.panel.sidebar.global.GlobalVariablesDialog;
import com.nvhuong.devworkbench.panel.toolbox.ToolboxPanel;
import com.nvhuong.devworkbench.plugin.api.StatusBarActionContribution;
import com.nvhuong.devworkbench.plugin.host.PluginAccess;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bảng điều khiển trang tab ở bên trái
 */
@Slf4j
public class SidebarTabPanel extends UiSingletonPanel {

    @Getter
    private JTabbedPane tabbedPane;
    private transient List<TabInfo> tabInfos;
    private transient List<SidebarTab> visibleTabs;
    private SidebarBottomBar bottomBar;
    private SidebarConsoleArea consoleArea;
    private RequestSideAssistantPanel requestSideAssistantPanel;
    private boolean sidebarExpanded = false; // Thanh bên được mở rộng
    private CookieManagerDialog cookieManagerDialog; // CookieVí dụ về hộp thoại trình quản lý
    private GlobalVariablesDialog globalVariablesDialog; // Ví dụ về hộp thoại biến toàn cục
    private int lastSelectedIndex = -1; // Ghi lại chỉ mục được chọn cuối cùng để tối ưu hóa cập nhật màu

    // Bộ nhớ đệm phông chữ để tránh tạo trùng lặp
    private Font normalFont;      // PLAIN 12 - TabVăn bản và số phiên bản được chia sẻ
    private Font boldFont;        // BOLD 12 - TabTrạng thái chọn văn bản được chia sẻ với thanh dưới cùng
    // Bộ đệm chiều rộng và chiều cao thích ứng
    private int calculatedExpandedTabWidth = -1; // Trạng thái mở rộng được tính toántabChiều rộng
    private int calculatedCollapsedTabWidth = -1; // Trạng thái thu gọn được tính toántabChiều rộng
    private int calculatedExpandedTabHeight = -1; // Trạng thái mở rộng được tính toántabchiều cao
    private int calculatedCollapsedTabHeight = -1; // Trạng thái thu gọn được tính toántabchiều cao
    private SidebarTabComponentFactory tabComponentFactory;

    @Override
    protected void initUI() {
        refreshSidebarFonts();
        // Đầu tiên hãy đọc trạng thái mở rộng thanh bên
        sidebarExpanded = SettingManager.isSidebarExpanded();
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);

        // Tạo một trang tab
        tabbedPane = createSidebarTabbedPane();
        reloadTabInfosFromSettings();
        installVisibleTabs(Map.of());

        // Cài đặt mặc định chọn nhãn đầu tiên
        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }

        preloadInitialContent();

        bottomBar = createBottomBar();
        requestSideAssistantPanel = new RequestSideAssistantPanel(this::currentRequestSnapshot);
        consoleArea = new SidebarConsoleArea(
                this,
                bottomBar,
                UiSingletonFactory.getInstance(ConsolePanel.class),
                requestSideAssistantPanel
        );
        consoleArea.setTabbedPane(tabbedPane);
    }

    private SidebarBottomBar createBottomBar() {
        return new SidebarBottomBar(
                sidebarExpanded,
                this::toggleSidebarExpansion,
                this::toggleConsoleArea,
                this::toggleLayoutOrientation,
                this::showGlobalVariablesDialog,
                this::showCookieManagerDialog,
                PluginAccess.getStatusBarActionContributions(),
                this::handleStatusBarAction
        );
    }

    private void handleStatusBarAction(StatusBarActionContribution contribution) {
        if (StatusBarActionContribution.TARGET_TOOLBOX.equals(contribution.targetType())) {
            openToolboxTool(contribution.targetId());
        }
    }

    boolean openToolboxTool(String toolId) {
        if (toolId == null || toolId.isBlank() || !showTab(SidebarTab.TOOLBOX)) {
            return false;
        }
        return UiSingletonFactory.getInstance(ToolboxPanel.class).showTool(toolId);
    }

    private void toggleConsoleArea() {
        if (consoleArea != null) {
            consoleArea.toggleConsole();
        }
    }

    private HttpRequestItem currentRequestSnapshot() {
        try {
            return UiSingletonFactory.getExistingInstance(RequestEditorPanel.class)
                    .map(RequestEditorPanel::getCurrentRequestSnapshotForAssistant)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Chuyển đổi mở rộng thanh bên/Trạng thái thu gọn
     */
    private void toggleSidebarExpansion() {
        sidebarExpanded = !sidebarExpanded;
        SettingManager.setSidebarExpanded(sidebarExpanded);
        bottomBar.setSidebarExpanded(sidebarExpanded);
        recreateTabbedPane();
    }

    /**
     * Chuyển hướng bố trí
     */
    private void toggleLayoutOrientation() {
        boolean currentVertical = SettingManager.isLayoutVertical();
        boolean newVertical = !currentVertical;
        SettingManager.setLayoutVertical(newVertical);

        bottomBar.updateLayoutToggleState(newVertical);

        // Cập nhật bố cục của tất cả các tab đang mở
        try {
            RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
            editPanel.updateAllTabsLayout(newVertical);
        } catch (Exception e) {
            log.error("Failed to update layout for all tabs", e);
        }
    }

    /**
     * trình diễn Cookie Hộp thoại quản lý
     */
    private void showCookieManagerDialog() {
        // Nếu hộp thoại đã tồn tại và hiển thị, nó sẽ được đưa lên nền trước
        if (cookieManagerDialog != null && cookieManagerDialog.isVisible()) {
            cookieManagerDialog.toFront();
            cookieManagerDialog.requestFocus();
            return;
        }

        // Tạo một hộp thoại mới
        Window window = SwingUtilities.getWindowAncestor(this);
        cookieManagerDialog = new CookieManagerDialog(window);
        cookieManagerDialog.setVisible(true);
    }

    /**
     * Hiển thị hộp thoại Biến toàn cục
     */
    private void showGlobalVariablesDialog() {
        if (globalVariablesDialog == null) {
            Window window = SwingUtilities.getWindowAncestor(this);
            globalVariablesDialog = new GlobalVariablesDialog(window);
        }

        if (globalVariablesDialog.isVisible()) {
            globalVariablesDialog.toFront();
            globalVariablesDialog.requestFocus();
            return;
        }

        globalVariablesDialog.setVisible(true);
        globalVariablesDialog.toFront();
        globalVariablesDialog.requestFocus();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        refreshSidebarFonts();

        resetTabLayoutCache();
        syncSidebarExpandedFromSettings();

        // updateUI Các tùy chỉnh sẽ được đặt lại UI，Cần phải được áp dụng lại.
        if (tabbedPane != null) {
            recreateTabbedPaneUI();
        }

        if (bottomBar != null) {
            bottomBar.refreshLocalizedText();
        }

        if (consoleArea != null) {
            consoleArea.refreshTheme();
        }

        refreshLocalizedTabComponents();
    }

    /**
     * Tái tạo TabbedPane của sự tùy biến UI
     * Được sử dụng trong updateUI() Sau đó khôi phục hiệu ứng hình ảnh trạng thái đã chọn tùy chỉnh
     */
    private void recreateTabbedPaneUI() {
        // Áp dụng tùy chỉnh UI
        applySidebarTabbedPaneUi(tabbedPane);

        // Buộc làm mới UI,Đảm bảo hiển thị tùy chỉnh có hiệu lực ngay lập tức
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    @Override
    protected void registerListeners() {
        registerTabbedPaneChangeListener();
        SwingUtilities.invokeLater(this::initializeAllTabSelectionStyles);
        PerformanceUiWarmup.schedule();

        // Đăng ký phím tắt
        registerTabShortcuts();
    }

    private void preloadInitialContent() {
        int selectedIndex = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
        ensureTabComponentLoaded(Math.max(selectedIndex, 0));
        initializeAllTabSelectionStyles();
    }

    /**
     * Đăng ký Tab phím tắt
     * Ctrl/Cmd + 1-7 Chuyển sang cái tương ứng Tab
     */
    private void registerTabShortcuts() {
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // MacSử dụngCmd，WindowsSử dụngCtrl

        // cho mỗi người Tab Đăng ký phím tắt Ctrl/Cmd + con số
        for (int i = 0; i < Math.min(tabInfos.size(), 9); i++) {
            final int tabIndex = i;
            int keyCode = KeyEvent.VK_1 + i; // VK_1 Đến VK_9

            // Đăng ký các phím tắt để ánh xạ đầu vào
            String actionKey = "switchToTab" + (i + 1);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(keyCode, modifier), actionKey
            );
            getActionMap().put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tabIndex < tabbedPane.getTabCount()) {
                        tabbedPane.setSelectedIndex(tabIndex);
                    }
                }
            });
        }
    }

    /**
     * Khởi tạo tất cả tab Kiểu đã chọn (được gọi khi tải lần đầu, bắt buộc cho cả trạng thái mở rộng và thu gọn).
     */
    private void initializeAllTabSelectionStyles() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            updateSingleTabSelectionStyle(i, i == selectedIndex);
        }
        lastSelectedIndex = selectedIndex;
    }

    /**
     * Cập nhật các thay đổi tab Kiểu đã chọn (được hỗ trợ ở cả trạng thái mở rộng và thu gọn).
     * Tối ưu hóa: Chỉ cập nhật các mục đã chọn hiện tại và trước đó tab，Tránh lặp đi lặp lại mọi thứ tab
     */
    private void updateChangedTabSelectionStyles() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // Cập nhật đã chọn trước đó tab（nếu có)
        if (lastSelectedIndex >= 0 && lastSelectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabSelectionStyle(lastSelectedIndex, false);
        }

        // Cập nhật cái hiện được chọn tab
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabSelectionStyle(selectedIndex, true);
        }

        // Ghi lại chỉ mục hiện được chọn
        lastSelectedIndex = selectedIndex;
    }

    /**
     * Cập nhật đơn tab biểu tượng, phông chữ tiêu đề và màu tiêu đề.
     */
    private void updateSingleTabSelectionStyle(int tabIndex, boolean isSelected) {
        Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
        SidebarTab sidebarTab = getSidebarTabAt(tabIndex);
        if (sidebarTab == null) return;

        tabComponentFactory().updateSelection(tabComponent, sidebarTab, isSelected);
    }

    // TabNội dung bảng thực chỉ được tải khi chuyển đổi
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex); // Tải chậm cái hiện được chọntabnội dung
        SidebarTab selectedTab = getSidebarTabAt(selectedIndex);
        if (selectedTab == SidebarTab.ENVIRONMENTS) {
            Component comp = SidebarTabContentHost.contentOf(tabbedPane.getComponentAt(selectedIndex));
            if (comp instanceof EnvironmentPanel environmentPanel) {
                environmentPanel.refreshUI();
            }
        }
        if (consoleArea != null) {
            consoleArea.handleSelectedTabChanged();
        }
    }

    private void ensureTabComponentLoaded(int index) {
        if (index < 0 || index >= tabInfos.size()) return;
        TabInfo info = tabInfos.get(index);
        SidebarTabContentHost host = SidebarTabContentHost.from(tabbedPane.getComponentAt(index));
        if (host == null) {
            return;
        }
        Component comp = host.content();
        if (comp == null || comp.getClass() == JPanel.class) {
            JPanel realPanel = info.getPanel(); // Tải chậm nội dung bảng điều khiển thực tế
            host.setContent(realPanel);
        }
    }

    /**
     * Tạo một tab thanh bên chuyên dụng
     */
    private JTabbedPane createSidebarTabbedPane() {
        JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT) {
            @Override
            public void updateUI() {
                super.updateUI();
                applySidebarTabbedPaneUi(this);
            }
        };
        applySidebarTabbedPaneUi(pane);
        return pane;
    }

    private void applySidebarTabbedPaneUi(JTabbedPane pane) {
        pane.setOpaque(true);
        pane.setBackground(SidebarTheme.railBackground());
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setUI(new SidebarTabbedPaneUi(
                () -> sidebarExpanded,
                this::calculateExpandedTabWidth,
                this::calculateCollapsedTabWidth,
                this::calculateExpandedTabHeight,
                this::calculateCollapsedTabHeight
        ));
    }

    private SidebarTabComponentFactory tabComponentFactory() {
        if (tabComponentFactory == null) {
            tabComponentFactory = new SidebarTabComponentFactory(
                    tabbedPane,
                    () -> sidebarExpanded,
                    this::findTabIndex,
                    this::calculateExpandedTabWidth,
                    this::calculateCollapsedTabWidth,
                    () -> normalFont,
                    () -> boldFont
            );
        }
        return tabComponentFactory;
    }

    private void refreshSidebarFonts() {
        normalFont = getLanguageAwareFont(Font.PLAIN);
        boldFont = getLanguageAwareFont(Font.BOLD);
    }

    private void syncSidebarExpandedFromSettings() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        if (this.sidebarExpanded == newExpanded) {
            return;
        }
        this.sidebarExpanded = newExpanded;
        if (bottomBar != null) {
            bottomBar.setSidebarExpanded(sidebarExpanded);
        }
    }

    private void refreshLocalizedTabComponents() {
        if (tabbedPane == null || tabInfos == null || visibleTabs == null) {
            return;
        }

        int tabCount = Math.min(tabInfos.size(), tabbedPane.getTabCount());
        for (int i = 0; i < tabCount; i++) {
            SidebarTab sidebarTab = visibleTabs.get(i);
            TabInfo info = tabInfos.get(i);
            info.setTitle(I18nUtil.getMessage(sidebarTab.getTitleKey()));
            tabbedPane.setTabComponentAt(i, tabComponentFactory().create(sidebarTab, info.getTitle(), info.getIcon()));
        }

        SwingUtilities.invokeLater(this::refreshTabSelectionStyles);
    }

    private void refreshTabSelectionStyles() {
        if (tabbedPane == null) {
            return;
        }
        initializeAllTabSelectionStyles();
        tabbedPane.repaint();
    }

    private void registerTabbedPaneChangeListener() {
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateChangedTabSelectionStyles();
        });
    }

    private int findTabIndex(SidebarTab sidebarTab) {
        if (visibleTabs == null) {
            return -1;
        }
        for (int i = 0; i < visibleTabs.size(); i++) {
            if (visibleTabs.get(i) == sidebarTab) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tính toán ở trạng thái mở rộngtabChiều rộng
     * Dựa trên dài nhấttabChiều rộng văn bản tiêu đề được tính toán linh hoạt
     */
    private int calculateExpandedTabWidth() {
        if (calculatedExpandedTabWidth > 0) {
            return calculatedExpandedTabWidth;
        }

        int maxWidth = 0;
        JLabel tempLabel = new JLabel();
        tempLabel.setFont(boldFont); // Sử dụng phông chữ đậm để tính toán vì chúng trở nên đậm hơn khi được chọn

        for (TabInfo info : tabInfos) {
            tempLabel.setText(info.getTitle());
            int textWidth = tempLabel.getPreferredSize().width;
            maxWidth = Math.max(maxWidth, textWidth);
        }

        calculatedExpandedTabWidth = SidebarTabMetrics.expandedWidth(maxWidth);

        return calculatedExpandedTabWidth;
    }

    /**
     * Tính trạng thái gấptabChiều rộng
     * Tính toán động dựa trên chiều rộng biểu tượng rộng nhất
     */
    private int calculateCollapsedTabWidth() {
        if (calculatedCollapsedTabWidth > 0) {
            return calculatedCollapsedTabWidth;
        }

        int maxIconWidth = 0;
        for (TabInfo info : tabInfos) {
            if (info.getIcon() != null) {
                maxIconWidth = Math.max(maxIconWidth, info.getIcon().getIconWidth());
            }
        }

        calculatedCollapsedTabWidth = SidebarTabMetrics.collapsedWidth(maxIconWidth);

        return calculatedCollapsedTabWidth;
    }

    /**
     * Tính toán ở trạng thái mở rộngtabchiều cao
     * Tính toán động dựa trên chiều cao biểu tượng và chiều cao phông chữ cao nhất
     */
    private int calculateExpandedTabHeight(int fontHeight) {
        if (calculatedExpandedTabHeight > 0) {
            return calculatedExpandedTabHeight;
        }

        int maxIconHeight = 0;
        for (TabInfo info : tabInfos) {
            if (info.getIcon() != null) {
                maxIconHeight = Math.max(maxIconHeight, info.getIcon().getIconHeight());
            }
        }

        calculatedExpandedTabHeight = SidebarTabMetrics.expandedHeight(maxIconHeight, fontHeight);

        return calculatedExpandedTabHeight;
    }

    /**
     * Tính trạng thái gấptabchiều cao
     * Dựa trên chiều cao biểu tượng và lề trên và dưới
     */
    private int calculateCollapsedTabHeight() {
        if (calculatedCollapsedTabHeight > 0) {
            return calculatedCollapsedTabHeight;
        }

        int maxIconHeight = 0;
        for (TabInfo info : tabInfos) {
            if (info.getIcon() != null) {
                maxIconHeight = Math.max(maxIconHeight, info.getIcon().getIconHeight());
            }
        }

        calculatedCollapsedTabHeight = SidebarTabMetrics.collapsedHeight(maxIconHeight);

        return calculatedCollapsedTabHeight;
    }

    public void refreshSidebarConfiguration() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        boolean expansionChanged = this.sidebarExpanded != newExpanded;
        this.sidebarExpanded = newExpanded;
        if (bottomBar != null) {
            bottomBar.setSidebarExpanded(sidebarExpanded);
        }

        List<SidebarTab> configuredTabs = SidebarTabSettingsResolver.getVisibleSidebarTabs();
        boolean tabsChanged = visibleTabs == null || !visibleTabs.equals(configuredTabs);
        if (expansionChanged || tabsChanged) {
            resetTabLayoutCache();
            recreateTabbedPane();
        }
    }

    private void resetTabLayoutCache() {
        calculatedExpandedTabWidth = -1;
        calculatedCollapsedTabWidth = -1;
        calculatedExpandedTabHeight = -1;
        calculatedCollapsedTabHeight = -1;
    }

    public boolean showTab(SidebarTab sidebarTab) {
        int tabIndex = findTabIndex(sidebarTab);
        if (tabIndex < 0 || tabIndex >= tabbedPane.getTabCount()) {
            return false;
        }
        tabbedPane.setSelectedIndex(tabIndex);
        return true;
    }

    /**
     * Tạo lại tab để áp dụng bản mở rộng mới/Trạng thái thu gọn
     */
    private void recreateTabbedPane() {
        if (consoleArea != null) {
            consoleArea.restoreContentHost();
        }
        int selectedIndex = tabbedPane.getSelectedIndex();
        SidebarTab selectedTab = getSidebarTabAt(selectedIndex);

        Map<SidebarTab, Component> loadedPanels = collectLoadedPanels();

        // Tạo cái mới TabbedPane
        tabbedPane = createSidebarTabbedPane();
        tabComponentFactory = null;
        reloadTabInfosFromSettings();
        installVisibleTabs(loadedPanels);
        restoreSelectedTab(selectedTab, selectedIndex);

        if (consoleArea != null) {
            consoleArea.setTabbedPane(tabbedPane);
        }

        registerTabbedPaneChangeListener();

        lastSelectedIndex = -1;

        initializeAllTabSelectionStyles();

        // Làm mớiUI
        revalidate();
        repaint();
    }

    private void installVisibleTabs(Map<SidebarTab, Component> retainedContent) {
        for (int i = 0; i < tabInfos.size(); i++) {
            SidebarTab sidebarTab = visibleTabs.get(i);
            TabInfo info = tabInfos.get(i);
            Component content = retainedContent.getOrDefault(sidebarTab, new JPanel());
            tabbedPane.addTab(info.getTitle(), new SidebarTabContentHost(content));
            tabbedPane.setTabComponentAt(i, tabComponentFactory().create(sidebarTab, info.getTitle(), info.getIcon()));
        }
    }

    private Map<SidebarTab, Component> collectLoadedPanels() {
        Map<SidebarTab, Component> loadedPanels = new EnumMap<>(SidebarTab.class);
        if (visibleTabs == null) {
            return loadedPanels;
        }

        int tabCount = Math.min(visibleTabs.size(), tabbedPane.getTabCount());
        for (int i = 0; i < tabCount; i++) {
            loadedPanels.put(visibleTabs.get(i), SidebarTabContentHost.contentOf(tabbedPane.getComponentAt(i)));
        }
        return loadedPanels;
    }

    private void restoreSelectedTab(SidebarTab selectedTab, int previousSelectedIndex) {
        int restoredIndex = findTabIndex(selectedTab);
        if (restoredIndex >= 0 && restoredIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(restoredIndex);
        } else if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(Math.min(previousSelectedIndex, tabbedPane.getTabCount() - 1));
        }
    }

    private void reloadTabInfosFromSettings() {
        visibleTabs = new ArrayList<>(SidebarTabSettingsResolver.getVisibleSidebarTabs());
        tabInfos = new ArrayList<>(visibleTabs.size());
        for (SidebarTab tab : visibleTabs) {
            tabInfos.add(tab.toTabInfo());
        }
    }

    private SidebarTab getSidebarTabAt(int index) {
        if (visibleTabs == null || index < 0 || index >= visibleTabs.size()) {
            return null;
        }
        return visibleTabs.get(index);
    }

    /**
     * Lấy cái thích hợp dựa trên ngôn ngữ hiện tại Tab Phông chữ tiêu đề
     * Sử dụng cỡ chữ nhỏ hơn bằng tiếng Anh để tránh Tab Văn bản tiêu đề quá dài
     * LƯU Ý: Phương pháp này dành cho  Tab Tiêu đề, thành phần thanh dưới cùng (Console、Cookie、số phiên bản) bằng cách sử dụng phông chữ tiêu chuẩn
     *
     * @param style Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @return Font đối tượng
     */
    private Font getLanguageAwareFont(int style) {
        if (I18nUtil.isVietnamese()) {
            // Tiêu đề tiếng Việt nhỏ hơn một chút để vừa với thanh bên khi mở rộng.
            return FontsUtil.getDefaultFontWithOffset(style, -2);
        } else {
            // Tiếng Anh sử dụng phông chữ nhỏ
            return FontsUtil.getDefaultFontWithOffset(style, -3);
        }
    }

}
