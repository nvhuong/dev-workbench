package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.laker.postman.common.component.LoadingOverlay;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Bảng phản hồi bao gồm nội dung phản hồi, tiêu đề phản hồi, kết quả kiểm tra, nhật ký mạng, mức tiêu thụ thời gian, v.v
 */
public class ResponsePanel extends JPanel {

    private final ResponseStatusBar statusBar;
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    @Getter
    private final NetworkLogPanel networkLogPanel;
    private final TimelinePanel timelinePanel;
    private final JEditorPane testsPane;
    private final JButton[] tabButtons;
    private final JPanel tabBar; // cứutabBarTài liệu tham khảo, được sử dụng để chuyển đổi
    private final JPanel topResponseBar; // cứutopResponseBarTrích dẫn
    private final JPanel cardPanel;
    private final String[] tabNames;
    private final ResponseTabBadgeController tabBadgeController;
    private final ResponseTabNavigationController tabNavigationController;
    @Getter
    private final RequestItemProtocolEnum protocol;
    @Getter
    private final WebSocketResponsePanel webSocketResponsePanel;
    @Getter
    private final SSEResponsePanel sseResponsePanel;
    private final LoadingOverlay loadingOverlay;
    private boolean hasResponseData = false;

    public ResponsePanel(RequestItemProtocolEnum protocol, boolean enableSaveButton) {
        this.protocol = protocol;
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);
        statusBar = new ResponseStatusBar();
        TabBarBuilder.TabConfig tabConfig;

        // Khởi tạo bảng tương ứng theo loại giao thức (sử dụng TabBarBuilderđơn giản hóa)
        if (protocol.isWebSocketProtocol()) {
            // WebSocket Bố cục chuyên dụng
            tabConfig = TabBarBuilder.createWebSocketTabs();
            tabNames = tabConfig.tabNames;
            tabButtons = TabBarBuilder.createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);

            topResponseBar = createTopResponseBar();
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            cardPanel = createCardPanel();
            webSocketResponsePanel = new WebSocketResponsePanel();
            responseHeadersPanel = new ResponseHeadersPanel();
            networkLogPanel = new NetworkLogPanel();
            cardPanel.add(webSocketResponsePanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.WEBSOCKET_MESSAGES)]);
            cardPanel.add(responseHeadersPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.RESPONSE_HEADERS)]);
            cardPanel.add(networkLogPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.NETWORK_LOG)]);
            timelinePanel = null;
            responseBodyPanel = null;
            testsPane = null;
            sseResponsePanel = null;
        } else {
            // HTTP Yêu cầu chung
            tabConfig = TabBarBuilder.createHttpTabs();
            tabNames = tabConfig.tabNames;
            tabButtons = TabBarBuilder.createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);

            topResponseBar = createTopResponseBar();
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            cardPanel = createCardPanel();
            responseBodyPanel = new ResponseBodyPanel(enableSaveButton); // Xác định xem có bật nút lưu hay không dựa trên các tham số
            responseBodyPanel.setEnabled(false);
            responseBodyPanel.setBodyText(null);
            responseHeadersPanel = new ResponseHeadersPanel();
            JPanel testsPanel = new JPanel(new BorderLayout());
            ToolWindowSurfaceStyle.applyCard(testsPanel);
            // Đặt đường viền
            testsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            ToolWindowSurfaceStyle.applyTextComponentCard(testsPane);
            JScrollPane testsScrollPane = new JScrollPane(testsPane);
            ToolWindowSurfaceStyle.applyScrollPaneCard(testsScrollPane);
            testsPanel.add(testsScrollPane, BorderLayout.CENTER);
            networkLogPanel = new NetworkLogPanel();
            timelinePanel = new TimelinePanel(new ArrayList<>(), null);
            JScrollPane timelineScrollPanel = new JScrollPane(timelinePanel);
            ToolWindowSurfaceStyle.applyScrollPaneCard(timelineScrollPanel);
            // Đặt đường viền
            timelineScrollPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            // Thêm vào theo thứ tự đã chỉ định cardPanel。Sau khi chuyển sang chế độ luồng sự kiện, nội dung phản hồi sẽ bị ẩn và luồng sự kiện trở thành nhãn đầu tiên.
            cardPanel.add(responseBodyPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.RESPONSE_BODY)]);
            sseResponsePanel = new SSEResponsePanel();
            cardPanel.add(sseResponsePanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.EVENT_STREAM)]);
            cardPanel.add(responseHeadersPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.RESPONSE_HEADERS)]);
            cardPanel.add(testsPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.TESTS)]);
            cardPanel.add(networkLogPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.NETWORK_LOG)]);
            cardPanel.add(timelineScrollPanel, tabNames[tabConfig.indexOf(TabBarBuilder.Tab.TIMING)]);
            webSocketResponsePanel = null;
        }
        tabBadgeController = new ResponseTabBadgeController(tabButtons, tabConfig);
        tabNavigationController = new ResponseTabNavigationController(
                topResponseBar,
                tabBar,
                statusBar,
                cardPanel,
                tabButtons,
                tabNames,
                tabConfig.indexOf(TabBarBuilder.Tab.RESPONSE_BODY),
                tabConfig.indexOf(TabBarBuilder.Tab.EVENT_STREAM)
        );
        tabNavigationController.initializeFirstVisibleTab();

        // Kiểm tra trạng thái bố cục ban đầu và quyết định sử dụng nó tabBar Hoặc một hộp thả xuống
        boolean isVertical = SettingManager.isLayoutVertical();
        tabNavigationController.installInitialLayout(!isVertical);

        // sử dụngTabBarBuilderRàng buộctabsự kiện
        tabNavigationController.bindTabActions();

        // Tất cả các nút đều bị tắt theo mặc định
        setResponseTabButtonsEnable(false);

        // Bắt đầu tải lớp mặt nạ
        loadingOverlay = new LoadingOverlay();

        // loading overlay Chỉ che cardPanel，Để lại phần trên không được che phủ tab/status bar。
        // Bằng cách này, bạn vẫn có thể chuyển đổi thẻ phản hồi và xem thông tin trạng thái trong quá trình thực hiện yêu cầu.
        JLayeredPane cardLayeredPane = new JLayeredPane();
        ToolWindowSurfaceStyle.applyCard(cardLayeredPane);
        cardLayeredPane.setLayout(new ResponseCardOverlayLayout());
        cardLayeredPane.add(cardPanel, JLayeredPane.DEFAULT_LAYER);
        cardLayeredPane.add(loadingOverlay, JLayeredPane.PALETTE_LAYER);

        add(topResponseBar, BorderLayout.NORTH);
        add(cardLayeredPane, BorderLayout.CENTER);
    }

    private JPanel createTopResponseBar() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionHeader(panel);
        return panel;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel(new CardLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        return panel;
    }

    public void setResponseTabButtonsEnable(boolean enable) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setResponseTabButtonsEnable(enable));
            return;
        }
        if (tabNavigationController.enabledStateMatches(enable)) {
            return;
        }
        tabNavigationController.setEnabled(enable);
    }

    public void setResponseBody(HttpResponse resp) {
        if (protocol.isWebSocketProtocol()) {
            // WebSocket Phản hồi được duy trì bởi các bảng chuyên dụng.
            return;
        }
        responseBodyPanel.setBodyText(resp);
    }

    public void setResponseHeaders(HttpResponse resp) {
        responseHeadersPanel.setHeaders(resp.headers);
        tabBadgeController.updateResponseHeadersCount(resp.headers != null ? resp.headers.size() : 0);
    }

    public void setTiming(HttpResponse resp) {
        if (timelinePanel == null) return;
        List<TimelinePanel.Stage> stages = new ArrayList<>();
        HttpEventInfo info = null;
        if (resp != null && resp.httpEventInfo != null) {
            info = resp.httpEventInfo;
            stages = TimelinePanel.buildStandardStages(info);
        }
        timelinePanel.setStages(stages);
        timelinePanel.setHttpEventInfo(info);
    }

    /**
     * Đặt mã trạng thái phản hồi
     *
     * @param code HTTP mã trạng thái (ví dụ 200, 404, 500）；vượt qua 0 Hoặc số âm biểu thị mã trạng thái rõ ràng
     */
    public void setStatus(int code) {
        statusBar.setStatus(code);
    }

    public void setResponseTime(long ms) {
        statusBar.setResponseTime(ms);
    }

    /**
     * Đặt hiển thị kích thước phản hồi (với đối tượng phản hồi hoàn chỉnh, có thể đọc được Content-Encoding）
     */
    public void setResponseSize(long bytes, HttpResponse httpResponse) {
        statusBar.setResponseSize(bytes, httpResponse);
    }

    public void setTestResults(List<TestResult> testResults) {
        if (testsPane == null) return; // ngăn chặn NPE
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        testsPane.setText(html);
        testsPane.setCaretPosition(0);
        tabBadgeController.updateTestResults(testResults);
    }

    public void clearAll() {
        hasResponseData = false;
        // Xóa thanh trạng thái
        statusBar.clear();

        responseHeadersPanel.setHeaders(new LinkedHashMap<>());
        tabBadgeController.updateResponseHeadersCount(0);
        if (protocol.isWebSocketProtocol()) {
            webSocketResponsePanel.clearMessages();
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
        }

        if (protocol.isHttpProtocol()) {
            responseBodyPanel.setBodyText(null);
            clearTiming();
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
            sseResponsePanel.clearMessages();
        }

        if (testsPane != null) {
            setTestResults(new ArrayList<>());
        }
    }

    public void clearInFlightRequestDetails() {
        if (networkLogPanel != null) {
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
        }
        clearTiming();
    }

    public void clearSseResponseEvents() {
        if (sseResponsePanel != null) {
            sseResponsePanel.clearMessages();
        }
    }

    public boolean hasResponseData() {
        return hasResponseData;
    }

    public void markResponseDataLoaded() {
        hasResponseData = true;
    }

    /**
     * Hiển thị bình thường dựa trên loại phản hồi thực tế HTTP Chế độ xem luồng phản hồi hoặc sự kiện.
     */
    public void showHttpResponseView(boolean eventStream) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showHttpResponseView(eventStream));
            return;
        }
        if (!protocol.isHttpProtocol()) {
            return;
        }
        tabNavigationController.switchHttpResponseView(eventStream);
    }

    private void clearTiming() {
        if (timelinePanel == null) {
            return;
        }
        timelinePanel.setStages(new ArrayList<>());
        timelinePanel.setHttpEventInfo(null);
    }

    /**
     * Hiển thị mặt nạ tải
     */
    public void showLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            loadingOverlay.showLoading();
        } else {
            SwingUtilities.invokeLater(loadingOverlay::showLoading);
        }
    }

    /**
     * Ẩn mặt nạ tải
     */
    public void hideLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            // sau khi yêu cầu được hoàn thành UI Làm mới đã có sẵn EDT thượng đẳng.
            // Điều này phải được ẩn ngay lập tức, nếu không nó sẽ được xếp hàng đợi sau khi hiển thị phản hồi lớn“Vòng tròn bị kẹt và không quay được”。
            loadingOverlay.hideLoading();
        } else {
            SwingUtilities.invokeLater(loadingOverlay::hideLoading);
        }
    }

    /**
     * Nhận nút lưu phản hồi
     * Đại lý đến ResponseBodyPanel nút lưu
     */
    public JButton getSaveResponseButton() {
        if (responseBodyPanel != null) {
            return responseBodyPanel.getSaveResponseButton();
        }
        return null;
    }

    /**
     * Đặt trạng thái kích hoạt của bảng nội dung phản hồi
     */
    public void setResponseBodyEnabled(boolean enabled) {
        if (responseBodyPanel != null) {
            responseBodyPanel.setEnabled(enabled);
        }
    }

    /**
     * Chuyển sang chỉ mục được chỉ định tab
     *
     * @param tabIndex tab chỉ mục(0-based）
     */
    public void switchToTab(int tabIndex) {
        tabNavigationController.switchToTab(tabIndex);
    }


    /**
     * Cập nhật chi tiết yêu cầu (được ủy quyền cho NetworkLogPanel）
     */
    public void setRequestDetails(PreparedRequest request) {
        if (networkLogPanel != null) {
            networkLogPanel.setRequestDetails(request);
        }
    }

    /**
     * Cập nhật chi tiết phản hồi (được ủy quyền cho NetworkLogPanel）
     */
    public void setResponseDetails(HttpResponse response) {
        if (networkLogPanel != null) {
            networkLogPanel.setResponseDetails(response);
        }
    }

    /**
     * Chuyển đổi dựa trên hướng bố tríTabChế độ hiển thị
     *
     * @param isVertical true=Bố cục dọc (trên và dưới).false=Bố cục ngang (trái và phải)
     */
    public void updateLayoutOrientation(boolean isVertical) {
        tabNavigationController.updateLayoutOrientation(isVertical);
    }

}
