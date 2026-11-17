package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.panel.collections.editor.CollectionTreeEditorGateway;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.sub.*;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Bảng điều khiển chỉnh sửa yêu cầu duy nhất, bao gồm URL、Lựa chọn phương pháp,Headers、Body và hiển thị đáp ứng
 */
public class RequestEditSubPanel extends JPanel {
    private static final int MAX_REDIRECT_COUNT = 10;

    private final RequestEditorDataController dataController;
    private final RequestItemProtocolEnum protocol;
    private final boolean deferEditorInitialization;
    @Getter
    private boolean editorInitialized;
    private Boolean pendingLayoutVertical;
    private final transient RequestEditorDeferredInitializationController deferredInitializationController;

    @Getter
    private final RequestEditSubPanelType panelType;

    @Getter
    private final SavedResponse savedResponse;
    private final HttpRequestItem savedResponseOwner;

    private RequestViewComponents view;
    // Bảng điều khiển chính chỉ còn lại“Sự sắp xếp”và“Trạng thái giữ nguyên”，Giao các hành vi cụ thể cho cộng tác viên với trách nhiệm rõ ràng để tránh phát triển thành các lớp học khổng lồ một lần nữa.
    private final RequestPreparationFeedbackPresenter requestPreparationFeedbackPresenter = new RequestPreparationFeedbackPresenter();
    private final RequestEditorSendPreparationController sendPreparationController =
            RequestEditorSendPreparationController.createDefault(
                    this::ensureEditorInitialized,
                    this::pinTransientTab,
                    () -> view.requestSettingsPanel.validateSettings(),
                    this::getCurrentRequest
            );
    private RequestEditorBinder requestEditorBinder;
    private RequestEditorDefaultTabSelector requestEditorDefaultTabSelector;
    private RequestEditorTabsVisibilityController requestEditorTabsVisibilityController;
    private RequestTabStateController requestTabStateController;
    private RequestUrlParamsSynchronizer requestUrlParamsSynchronizer;
    private RequestEditorRuntimeController runtimeController;
    private final SavedResponseUiController savedResponseUiController = new SavedResponseUiController(new CollectionTreeEditorGateway());
    private RequestSavedResponseController savedResponseController;

    // Cờ tải dữ liệu để ngăn kích hoạt tính năng lưu tự động và cập nhật được liên kết trong khi tải
    private boolean isLoadingData = false;

    // Lưu yêu cầu và phản hồi cuối cùng, dùng để lưu chức năng phản hồi
    private PreparedRequest lastRequest;
    private HttpResponse lastResponse;

    private RequestItemProtocolEnum getEffectiveProtocol() {
        return dataController.effectiveProtocol();
    }

    private boolean isBaseHttpProtocol() {
        return protocol != null && protocol.isHttpProtocol();
    }

    private boolean isEffectiveHttpProtocol() {
        return getEffectiveProtocol() != null && getEffectiveProtocol().isHttpProtocol();
    }

    private boolean isEffectiveWebSocketProtocol() {
        return getEffectiveProtocol() != null && getEffectiveProtocol().isWebSocketProtocol();
    }

    /**
     * Xác định xem bảng hiện tại có phải là tab phản hồi đã lưu hay không
     */
    public boolean isSavedResponseTab() {
        return panelType == RequestEditSubPanelType.SAVED_RESPONSE;
    }

    public boolean saveSavedResponseRequest() {
        if (!isSavedResponseTab() || savedResponseOwner == null || savedResponse == null) {
            return false;
        }
        HttpRequestItem editedRequest = getCurrentRequest();
        boolean saved = savedResponseUiController.updateExampleRequest(
                savedResponseOwner.getId(), savedResponse, editedRequest);
        if (saved) {
            dataController.setOriginalRequestItem(editedRequest);
        }
        return saved;
    }

    private SavedResponse responseExampleForSave() {
        return savedResponse;
    }

    private HttpRequestItem responseExampleOwnerForSave() {
        return savedResponseOwner;
    }

    private boolean isPerformanceSnapshot() {
        return panelType == RequestEditSubPanelType.PERFORMANCE_SNAPSHOT;
    }

    /**
     * Trình xây dựng bảng chỉnh sửa yêu cầu thông thường
     */
    public RequestEditSubPanel(String id, RequestItemProtocolEnum protocol) {
        this(id, protocol, false);
    }

    public RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, boolean deferEditorInitialization) {
        this(id, protocol, RequestEditSubPanelType.NORMAL, null, deferEditorInitialization);
    }

    public static RequestEditSubPanel performanceSnapshot(String id,
                                                          RequestItemProtocolEnum protocol,
                                                          boolean deferEditorInitialization) {
        return new RequestEditSubPanel(
                id,
                protocol,
                RequestEditSubPanelType.PERFORMANCE_SNAPSHOT,
                null,
                deferEditorInitialization
        );
    }

    /**
     * Trình tạo bảng phản hồi đã lưu
     */
    public RequestEditSubPanel(SavedResponse savedResponse) {
        this(savedResponse, null);
    }

    public RequestEditSubPanel(SavedResponse savedResponse, HttpRequestItem owner) {
        this(savedResponse.getId(), RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.SAVED_RESPONSE,
                savedResponse, owner, false);
    }

    /**
     * Trình xây dựng hoàn chỉnh
     */
    private RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, RequestEditSubPanelType panelType,
                                SavedResponse savedResponse,
                                boolean deferEditorInitialization) {
        this(id, protocol, panelType, savedResponse, null, deferEditorInitialization);
    }

    private RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, RequestEditSubPanelType panelType,
                                SavedResponse savedResponse, HttpRequestItem savedResponseOwner,
                                boolean deferEditorInitialization) {
        this.protocol = protocol == null ? RequestItemProtocolEnum.HTTP : protocol;
        this.panelType = panelType;
        this.savedResponse = savedResponse;
        this.savedResponseOwner = savedResponseOwner;
        this.deferEditorInitialization = deferEditorInitialization && panelType != RequestEditSubPanelType.SAVED_RESPONSE;
        this.dataController = new RequestEditorDataController(
                id,
                this.protocol,
                () -> editorInitialized,
                this::isPerformanceSnapshot,
                loading -> isLoadingData = loading,
                this::updateTabIndicators
        );
        this.deferredInitializationController = new RequestEditorDeferredInitializationController(
                this,
                () -> this.deferEditorInitialization && !editorInitialized,
                () -> ensureEditorInitialized(true)
        );
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Đặt lề thành 5
        ToolWindowSurfaceStyle.applyCard(this);
        if (!this.deferEditorInitialization) {
            initializeEditorUiIfNeeded(false);
        } else {
            deferredInitializationController.installPlaceholder();
        }
    }

    public void ensureEditorInitialized() {
        ensureEditorInitialized(false);
    }

    public void ensureEditorInitialized(boolean animatePlaceholderTransition) {
        // Khi trang giữ chỗ được nâng cấp lên trình chỉnh sửa thực, cho phép làm mờ một phần, nhưng chỉ có thể làm mờ lớp chuyển tiếp layeredPane Ảnh chụp nhanh vẽ thuần túy được bật.
        // Điều này duy trì độ mịn mà không ảnh hưởng đến lớp bên dưới JSplitPane của hover / resize cursor。
        if (editorInitialized) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            initializeEditorUiIfNeeded(animatePlaceholderTransition);
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> initializeEditorUiIfNeeded(animatePlaceholderTransition));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RequestEditSubPanel on EDT", e);
        }
    }

    public String getId() {
        return dataController.requestId();
    }

    public RequestLinePanel getRequestLinePanel() {
        ensureEditorInitialized();
        return view.requestLinePanel;
    }

    private void initializeEditorUiIfNeeded(boolean animatePlaceholderTransition) {
        if (editorInitialized) {
            return;
        }
        var placeholderSnapshot = animatePlaceholderTransition
                ? deferredInitializationController.capturePlaceholderSnapshot()
                : null;
        removeAll();
        view = RequestViewFactory.create(protocol, panelType, this::sendRequest);
        add(view.requestLinePanel, BorderLayout.NORTH);
        createEditorCollaborators();
        bindBasicEditorInteractions();

        if (!isPerformanceSnapshot()) {
            createRuntimeController();
        } else {
            RequestEditorReadOnlyMode.apply(view);
        }

        finishEditorUiInitialization();

        dataController.populatePendingRequestIfPresent();
        if (pendingLayoutVertical != null) {
            if (runtimeController != null) {
                runtimeController.updateLayoutOrientation(pendingLayoutVertical);
            }
            pendingLayoutVertical = null;
        }
        revalidate();
        repaint();
        deferredInitializationController.startTransition(placeholderSnapshot);
    }

    private void bindBasicEditorInteractions() {
        if (!isPerformanceSnapshot()) {
            RequestEditorUiBinder.bindUrlField(
                    view.urlField,
                    requestPreparationFeedbackPresenter,
                    this::detectAndParseCurl,
                    () -> requestUrlParamsSynchronizer.syncUrlToParams(isLoadingData),
                    this::autoPrependHttpsIfNeeded
            );
            RequestEditorUiBinder.bindParamsSync(view.paramsPanel, () -> requestUrlParamsSynchronizer.syncParamsToUrl(isLoadingData));
        }
    }

    private void createEditorCollaborators() {
        requestUrlParamsSynchronizer = new RequestUrlParamsSynchronizer(view.urlField, view.paramsTabPanel, view.paramsPanel);
        view.requestLinePanel.setPathVariablesSupplier(view.paramsTabPanel::getPathVariablesListFromModel);
        requestEditorBinder = new RequestEditorBinder(view);
        requestEditorDefaultTabSelector = new RequestEditorDefaultTabSelector(view);
        dataController.bindEditor(
                requestEditorBinder,
                requestEditorDefaultTabSelector,
                () -> runtimeController != null ? runtimeController.dirtyStateTracker() : null,
                view.requestSettingsPanel
        );
        requestEditorTabsVisibilityController = new RequestEditorTabsVisibilityController(
                protocol,
                view,
                this::updateTabIndicators
        );
        requestTabStateController = new RequestTabStateController(protocol, view);
    }

    private void createRuntimeController() {
        runtimeController = RequestEditorRuntimeController.create(new RequestEditorRuntimeController.Config(
                this,
                view,
                requestPreparationFeedbackPresenter,
                sendPreparationController::prepareForSending,
                this::sendRequest,
                this::isBaseHttpProtocol,
                this::isEffectiveHttpProtocol,
                this::isEffectiveWebSocketProtocol,
                this::initPanelData,
                this::getCurrentRequestFromModel,
                this::updateParentTabDirty,
                this::recordExchange,
                MAX_REDIRECT_COUNT
        ));
    }

    private void finishEditorUiInitialization() {
        add(view.editorContent, BorderLayout.CENTER);
        RequestEditorUiBinder.applyInitialProtocolUi(
                protocol,
                view.reqTabs,
                view.requestBodyPanel,
                view.paramsTabPanel,
                view.authTabPanel,
                e -> sendWebSocketMessage()
        );
        if (!isPerformanceSnapshot()) {
            requestTabStateController.bindListeners(
                    this::updateTabDirty,
                    isSavedResponseTab() ? () -> {
                    } : this::updateParentTabMethod
            );
        }

        SwingUtilities.invokeLater(this::updateTabIndicators);
        RequestEditorUiBinder.bindSaveResponseButton(protocol, panelType, view.responsePanel,
                e -> getSavedResponseController().showSaveDialog());
        if (!isPerformanceSnapshot()) {
            RequestEditorUiBinder.bindBodyTypeHeaderSync(view.requestBodyPanel, view.headersPanel, () -> isLoadingData);
        }
        editorInitialized = true;
    }

    /**
     * Cập nhật tất cảtabchỉ số nội dung
     */
    private void updateTabIndicators() {
        if (requestTabStateController == null) {
            return;
        }
        requestTabStateController.updateTabIndicators();
    }


    /**
     * Đặt dữ liệu yêu cầu thô (phát hiện dữ liệu bẩn)
     */
    public void setOriginalRequestItem(HttpRequestItem item) {
        dataController.setOriginalRequestItem(item);
    }

    public HttpRequestItem getOriginalRequestItem() {
        return dataController.originalRequestItem();
    }

    /**
     * Xác định xem nội dung biểu mẫu hiện tại có bị sửa đổi hay không (so với yêu cầu ban đầu)
     * Lưu ý: Loại trừ khi so sánh response vì đây là dữ liệu phản hồi lịch sử và không phải là một phần của nội dung chỉnh sửa biểu mẫu
     */
    public boolean isModified() {
        return dataController.isModified();
    }

    /**
     * Từ tableModel Đọc dữ liệu trực tiếp để xây dựng HttpRequestItem，Không được gọi stopCellEditing。
     * Độc quyền có sẵn isModified() / updateTabDirty() Đợi kịch bản so sánh nền được sử dụng.
     * Tránh vào TableModelListener Làm gián đoạn quá trình chỉnh sửa ô của người dùng trong lệnh gọi lại.
     */
    private HttpRequestItem getCurrentRequestFromModel() {
        return dataController.currentRequestFromModel();
    }

    /**
     * Kiểm tra trạng thái bẩn và cập nhậttabtiêu đề
     */
    private void updateTabDirty() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.updateTabDirty();
    }

    private void updateParentTabProtocol(RequestItemProtocolEnum newProtocol) {
        updateParentTabDisplay(newProtocol);
    }

    private void updateParentTabMethod() {
        updateParentTabDisplay(getEffectiveProtocol());
    }

    private void updateParentTabDisplay(RequestItemProtocolEnum protocol) {
        if (isSavedResponseTab() || isPerformanceSnapshot() || isLoadingData || view == null || view.methodBox == null) {
            return;
        }
        UiSingletonFactory.getInstance(RequestEditorPanel.class)
                .updateTabDisplay(this, currentMethodForTabDisplay(), protocol);
    }

    private String currentMethodForTabDisplay() {
        Object selectedMethod = view.methodBox.getSelectedItem();
        return selectedMethod instanceof String method ? method : null;
    }

    private void updateParentTabDirty(boolean dirty) {
        UiSingletonFactory.getInstance(RequestEditorPanel.class).updateTabDirty(this, dirty);
    }

    private void recordExchange(PreparedRequest request, HttpResponse response) {
        lastRequest = request;
        lastResponse = response;
    }

    private void sendRequest(ActionEvent e) {
        if (runtimeController == null) {
            return;
        }
        runtimeController.sendRequest();
    }

    public String validateRequestSettings() {
        return sendPreparationController.validateRequestSettings();
    }

    private void pinTransientTab() {
        SwingUtilities.invokeLater(() -> UiSingletonFactory.getInstance(RequestEditorPanel.class).pinTransientTab());
    }

    // WebSocketLogic gửi tin nhắn
    private void sendWebSocketMessage() {
        if (runtimeController != null) {
            runtimeController.sendWebSocketMessage();
        }
    }

    /**
     * Update form content (for switching requests or refreshing after saving)
     */
    public void initPanelData(HttpRequestItem item) {
        dataController.initPanelData(item);
        if (item != null) {
            updateParentTabDisplay(item.getProtocol() != null ? item.getProtocol() : getEffectiveProtocol());
        }
    }

    /**
     * Lấy nội dung biểu mẫu hiện tại được gói gọn dưới dạngHttpRequestItem
     */
    public HttpRequestItem getCurrentRequest() {
        return dataController.currentRequest();
    }

    /**
     * Giải phóng tài nguyên mạng trước khi đóng tab để tránh kết nối nền UI Sống sót sau khi bị loại bỏ.
     */
    public void disposeResources() {
        if (runtimeController != null) {
            runtimeController.disposeOpenConnections();
        }
        if (!editorInitialized) {
            dataController.clearPending();
        }
    }

    /**
     * nếu URL Nếu không có giao thức, nó sẽ được hoàn thành dựa trên giao thức hiện tại và cấu hình người dùng.
     */
    private void autoPrependHttpsIfNeeded() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.autoPrependProtocolIfNeeded();
    }

    private RequestSavedResponseController getSavedResponseController() {
        if (savedResponseController == null) {
            savedResponseController = new RequestSavedResponseController(
                    this,
                    savedResponseUiController,
                    () -> lastRequest,
                    () -> lastResponse,
                    this::getOriginalRequestItem,
                    this::responseExampleForSave,
                    this::responseExampleOwnerForSave,
                    requestEditorBinder,
                    requestEditorDefaultTabSelector,
                    this::getEffectiveProtocol,
                    loading -> isLoadingData = loading,
                    view.responsePanel,
                    view.requestLinePanel,
                    this::sendRequest
            );
        }
        return savedResponseController;
    }

    /**
     * Tải phản hồi đã lưu vào bảng điều khiển
     */
    public void loadSavedResponse(SavedResponse savedResponse) {
        if (savedResponse == null) {
            return;
        }
        ensureEditorInitialized();
        getSavedResponseController().loadSavedResponse(savedResponse);
    }

    /**
     * Tự động cập nhật hướng bố cục (để chuyển đổi bố cục chung)
     *
     * @param isVertical true=bố cục dọc,false=Bố cục ngang
     */
    public void updateLayoutOrientation(boolean isVertical) {
        if (!editorInitialized) {
            pendingLayoutVertical = isVertical;
            return;
        }
        if (runtimeController == null) {
            return;
        }
        runtimeController.updateLayoutOrientation(isVertical);
    }

    public void updateRequestEditorTabsVisibility() {
        if (!editorInitialized || requestEditorTabsVisibilityController == null) {
            return;
        }
        requestEditorTabsVisibilityController.updateVisibility();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (editorInitialized && runtimeController != null) {
            runtimeController.handleInitialLayout();
        }
    }

    /**
     * Phát hiện và phân tích cURL Đặt hàng
     */
    private void detectAndParseCurl() {
        ensureEditorInitialized();
        if (runtimeController != null) {
            runtimeController.detectAndParseCurl(isLoadingData);
        }
    }
}
