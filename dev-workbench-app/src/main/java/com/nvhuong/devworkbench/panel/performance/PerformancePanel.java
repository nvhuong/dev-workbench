package com.nvhuong.devworkbench.panel.performance;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.DebouncedSaveSupport;
import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ExportButton;
import com.nvhuong.devworkbench.common.component.button.ImportButton;
import com.nvhuong.devworkbench.common.component.button.RefreshButton;
import com.nvhuong.devworkbench.common.component.button.StartButton;
import com.nvhuong.devworkbench.common.component.button.StopButton;
import com.nvhuong.devworkbench.common.component.dialog.TextInputDialog;
import com.nvhuong.devworkbench.common.constants.AppConstants;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.panel.performance.assertion.AssertionPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.control.PerformanceSaveShortcutSupport;
import com.nvhuong.devworkbench.panel.performance.control.PerformanceRunUiController;
import com.nvhuong.devworkbench.panel.performance.control.PerformanceRunUiEventBridge;
import com.nvhuong.devworkbench.panel.performance.control.PerformanceStatisticsCoordinator;
import com.nvhuong.devworkbench.panel.performance.control.PerformanceTimerManager;
import com.nvhuong.devworkbench.panel.performance.config.CsvDataSetPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.controller.ConditionPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.controller.LoopPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.controller.WhilePropertyPanel;
import com.nvhuong.devworkbench.panel.performance.extractor.ExtractorPropertyPanel;
import com.nvhuong.devworkbench.http.runtime.okhttp.HttpClientRuntimeConfig;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.model.PerformanceRealtimeMetrics;
import com.nvhuong.devworkbench.performance.core.model.PerformanceStatsCollector;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendWindowCollector;
import com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlan;
import com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlanJsonStorage;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerEndpoint;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerEndpointParser;
import com.nvhuong.devworkbench.performance.execution.PerformanceExecutionConfig;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.model.PerformanceResultListener;
import com.nvhuong.devworkbench.performance.model.PerformanceStatsCollectorListener;
import com.nvhuong.devworkbench.performance.model.PerformanceTrendWindowCollectorListener;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanConfiguration;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanDocument;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanImportResult;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanImportService;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanWorkspace;
import com.nvhuong.devworkbench.performance.plan.PerformanceRemoteWorkerSettings;
import com.nvhuong.devworkbench.performance.plan.PerformanceRunPlanFactory;
import com.nvhuong.devworkbench.performance.plan.PerformanceSavedPlan;
import com.nvhuong.devworkbench.performance.core.run.PerformanceServiceResource;
import com.nvhuong.devworkbench.performance.result.PerformanceResultCollector;
import com.nvhuong.devworkbench.performance.runtime.PerformanceExecutionEngine;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunSession;
import com.nvhuong.devworkbench.panel.performance.result.PerformanceReportPanel;
import com.nvhuong.devworkbench.panel.performance.result.PerformanceResultTablePanel;
import com.nvhuong.devworkbench.panel.performance.result.PerformanceResultTableVisualizer;
import com.nvhuong.devworkbench.panel.performance.result.PerformanceTrendView;
import com.nvhuong.devworkbench.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.timer.TimerPropertyPanel;
import com.nvhuong.devworkbench.panel.performance.tree.PerformanceSwingTreePlanAdapter;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.service.EnvironmentService;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.service.GlobalVariablesService;
import com.nvhuong.devworkbench.service.PerformancePersistenceService;
import com.nvhuong.devworkbench.service.collections.RequestSaveEventPublisher;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.FileChooserUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import com.nvhuong.devworkbench.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cây đa cấp ở bên trái (nhóm người dùng-yêu cầu-khẳng định-đồng hồ bấm giờ), khu vực thuộc tính bên phải, phía dướiTabKhu vực kết quả
 */
@Slf4j
public class PerformancePanel extends UiSingletonPanel {
    public static final String EMPTY = "empty";
    public static final String THREAD_GROUP = "threadGroup";
    public static final String CSV_DATA_SET = "csvDataSet";
    public static final String LOOP = "loop";
    public static final String SIMPLE = "simple";
    public static final String CONDITION = "condition";
    public static final String WHILE = "while";
    public static final String ONCE_ONLY = "onceOnly";
    public static final String REQUEST = "request";
    public static final String ASSERTION = "assertion";
    public static final String EXTRACTOR = "extractor";
    public static final String TIMER = "timer";
    public static final String SSE_CONNECT = "sseConnect";
    public static final String SSE_READ = "sseRead";
    public static final String WS_CONNECT = "wsConnect";
    public static final String WS_SEND = "wsSend";
    public static final String WS_READ = "wsRead";
    public static final String WS_CLOSE = "wsClose";
    private JTree performanceTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // Khu vực thuộc tính bên phải (CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // kết quảTab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private CsvDataSetPropertyPanel csvDataSetPanel;
    private LoopPropertyPanel loopPanel;
    private ConditionPropertyPanel conditionPanel;
    private WhilePropertyPanel whilePanel;
    private AssertionPropertyPanel assertionPanel;
    private ExtractorPropertyPanel extractorPanel;
    private TimerPropertyPanel timerPanel;
    private SseStagePropertyPanel sseConnectPanel;
    private SseStagePropertyPanel sseReadPanel;
    private WebSocketStagePropertyPanel wsConnectPanel;
    private WebSocketStagePropertyPanel wsSendPanel;
    private WebSocketStagePropertyPanel wsReadPanel;
    private WebSocketStagePropertyPanel wsClosePanel;
    private JPanel requestEditorHost;
    private volatile boolean running = false;
    private transient Thread runThread;
    private StartButton runBtn;
    private StopButton stopBtn;
    private ImportButton importBtn;
    private ExportButton exportBtn;
    private RefreshButton refreshBtn;
    private JComboBox<String> planSelector;
    private JButton addPlanButton;
    private JButton duplicatePlanButton;
    private JButton renamePlanButton;
    private JButton deletePlanButton;
    private JCheckBox remoteModeCheckBox;
    private JTextField workerEndpointsField;
    private JCheckBox efficientCheckBox; // Hợp lý hóa hộp kiểm chi tiết
    private JCheckBox trendCheckBox; // Công tắc lấy mẫu xu hướng
    private JComboBox<String> reportRefreshModeBox; // Chế độ làm mới báo cáo
    private JLabel progressLabel; // Nhãn tiến độ
    private JLabel limitLabel;
    private JPanel topPanel; // Bảng thanh công cụ trên cùng được sử dụng để cập nhật đường viền khi chuyển đổi chủ đề
    private long startTime;
    private final transient PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
    private final transient PerformanceTrendWindowCollector trendWindowCollector = new PerformanceTrendWindowCollector();

    // Người quản lý hẹn giờ - Quản lý thống nhất việc lấy mẫu biểu đồ xu hướng và hẹn giờ làm mới báo cáo
    private transient PerformanceTimerManager timerManager;

    // Chi tiết được đơn giản hóa: Thống kê báo cáo được giữ lại hoàn toàn và chỉ giữ lại bảng kết quả lỗi/Chi tiết yêu cầu chậm
    private boolean efficientMode = true;
    private boolean trendEnabled = true;
    private boolean reportRealtimeEnabled = false;
    private boolean remoteExecutionEnabled = false;
    private String remoteWorkerEndpoints = "";
    private List<PerformanceServiceResource> serviceResources = new ArrayList<>();
    private List<PerformanceSavedPlan> savedPlans = new ArrayList<>();
    private String activePlanId;
    private boolean updatingPlanSelector;

    private PerformanceReportPanel performanceReportPanel;
    private PerformanceResultTablePanel performanceResultTablePanel;
    private PerformanceTrendView performanceTrendPanel;

    // Dịch vụ kiên trì
    private transient PerformancePersistenceService persistenceService;

    // Nút yêu cầu hiện được chọn
    private DefaultMutableTreeNode currentRequestNode;
    private transient PerformanceTreeSupport treeSupport;
    private transient PerformanceRequestSyncSupport requestSyncSupport;
    private transient PerformancePropertyPanelSupport propertyPanelSupport;
    private transient PerformanceStatisticsCoordinator statisticsCoordinator;
    private transient PerformanceExecutionEngine executionEngine;
    private transient PerformanceRunControlSupport runControlSupport;
    private transient PerformanceRemoteRunControlSupport remoteRunControlSupport;
    private transient PerformanceRequestEditorSupport requestEditorSupport;
    private transient PerformancePlanImportService planImportService = new PerformancePlanImportService();
    private final transient PerformanceSaveShortcutSupport saveShortcutSupport = new PerformanceSaveShortcutSupport();
    private final transient DebouncedSaveSupport autoSaveSupport = new DebouncedSaveSupport(500, this::saveConfigAsync);

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);
        this.persistenceService = BeanFactory.getBean(PerformancePersistenceService.class);
        initTimerManager();
        PerformancePlanWorkspace persistedWorkspace = persistenceService.loadWorkspace();
        initializePlanState(persistedWorkspace);
        PerformancePlanConfiguration persistedConfiguration = activePlanConfiguration();
        applyPersistedSettings(persistedConfiguration);
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot(persistedConfiguration, activePlanName());

        treeModel = new DefaultTreeModel(root);
        treeSupport = new PerformanceTreeSupport(treeModel);
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        PerformancePanelViewFactory.TreeSection treeSection = viewFactory.createTreeSection(treeModel);
        performanceTree = treeSection.tree();
        requestSyncSupport = new PerformanceRequestSyncSupport(
                treeModel,
                performanceTree,
                this::syncRequestStructure
        );
        PerformancePanelViewFactory.PropertySection propertySection = viewFactory.createPropertySection(
                EMPTY,
                THREAD_GROUP,
                CSV_DATA_SET,
                LOOP,
                SIMPLE,
                CONDITION,
                WHILE,
                ONCE_ONLY,
                REQUEST,
                ASSERTION,
                EXTRACTOR,
                TIMER,
                SSE_CONNECT,
                SSE_READ,
                WS_CONNECT,
                WS_SEND,
                WS_READ,
                WS_CLOSE
        );
        propertyPanel = propertySection.propertyPanel();
        propertyCardLayout = propertySection.propertyCardLayout();
        threadGroupPanel = propertySection.threadGroupPanel();
        csvDataSetPanel = propertySection.csvDataSetPanel();
        loopPanel = propertySection.loopPanel();
        conditionPanel = propertySection.conditionPanel();
        whilePanel = propertySection.whilePanel();
        assertionPanel = propertySection.assertionPanel();
        extractorPanel = propertySection.extractorPanel();
        timerPanel = propertySection.timerPanel();
        sseConnectPanel = propertySection.sseConnectPanel();
        sseReadPanel = propertySection.sseReadPanel();
        wsConnectPanel = propertySection.wsConnectPanel();
        wsSendPanel = propertySection.wsSendPanel();
        wsReadPanel = propertySection.wsReadPanel();
        wsClosePanel = propertySection.wsClosePanel();
        RequestEditSubPanel requestEditSubPanel = propertySection.requestEditSubPanel();
        requestEditorHost = propertySection.requestEditorHost();
        requestEditorSupport = new PerformanceRequestEditorSupport(
                requestEditSubPanel,
                requestEditorHost,
                this::resolveRequestProtocol
        );

        propertyPanelSupport = new PerformancePropertyPanelSupport(
                performanceTree,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                conditionPanel,
                whilePanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                requestEditorSupport::getRequestEditSubPanel,
                () -> currentRequestNode,
                this::saveRequestNodeData,
                treeSupport,
                this::syncRequestStructure
        );
        csvDataSetPanel.setChangeListener(this::handleCsvDataSetChanged);

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection(
                trendEnabled,
                reportRealtimeEnabled,
                efficientMode,
                this,
                value -> efficientMode = value,
                this::applyTrendEnabled,
                this::applyReportRealtimeEnabled,
                this::refreshReportSnapshotIfAllowed,
                this::saveAllPropertyPanelData,
                this::saveConfig
        );
        resultTabbedPane = resultSection.resultTabbedPane();
        performanceResultTablePanel = resultSection.performanceResultTablePanel();
        performanceTrendPanel = resultSection.performanceTrendPanel();
        performanceReportPanel = resultSection.performanceReportPanel();
        serviceResources = persistedConfiguration == null ? new ArrayList<>()
                : new ArrayList<>(persistedConfiguration.getServiceResources());
        performanceReportPanel.setServiceResources(serviceResources);
        efficientCheckBox = resultSection.efficientCheckBox();
        trendCheckBox = resultSection.trendCheckBox();
        reportRefreshModeBox = resultSection.reportRefreshModeBox();
        statisticsCoordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                trendWindowCollector,
                performanceReportPanel,
                performanceTrendPanel,
                resultTabbedPane,
                () -> executionEngine != null ? executionEngine.sampleWindowPeakActiveThreads() : 0,
                () -> executionEngine != null ? executionEngine.getActiveWebSockets() : 0,
                () -> executionEngine != null ? executionEngine.getActiveSseStreams() : 0,
                () -> timerManager != null ? timerManager.getSamplingIntervalMs() : 1000L,
                () -> trendEnabled,
                nowMs -> executionEngine != null
                        ? executionEngine.drainRealtimeMetricsWindow(nowMs)
                        : PerformanceRealtimeMetrics.Sample.empty(),
                nowMs -> executionEngine != null
                        ? executionEngine.liveRealtimeMetrics(nowMs)
                        : PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );
        timerManager.setTrendSamplingCallback(statisticsCoordinator::sampleTrendData);
        timerManager.setReportRefreshCallback(statisticsCoordinator::refreshReport);

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                this::importPerformancePlan,
                this::exportRunPlan,
                this::showUsageGuide,
                this::refreshRequestsFromCollections,
                remoteExecutionEnabled,
                remoteWorkerEndpoints,
                this::setRemoteExecutionEnabled,
                this::setRemoteWorkerEndpoints
        );
        topPanel = toolbarSection.topPanel();
        JButton serviceResourcesButton = new JButton(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_RESOURCES));
        serviceResourcesButton.addActionListener(e -> editServiceResources());
        topPanel.add(serviceResourcesButton, "span 2, left, wrap");
        runBtn = toolbarSection.runBtn();
        stopBtn = toolbarSection.stopBtn();
        importBtn = toolbarSection.importBtn();
        exportBtn = toolbarSection.exportBtn();
        refreshBtn = toolbarSection.refreshBtn();
        planSelector = toolbarSection.planSelector();
        addPlanButton = toolbarSection.addPlanButton();
        duplicatePlanButton = toolbarSection.duplicatePlanButton();
        renamePlanButton = toolbarSection.renamePlanButton();
        deletePlanButton = toolbarSection.deletePlanButton();
        remoteModeCheckBox = toolbarSection.remoteModeCheckBox();
        workerEndpointsField = toolbarSection.workerEndpointsField();
        progressLabel = toolbarSection.progressLabel();
        limitLabel = toolbarSection.limitLabel();
        installPlanToolbarListeners();
        syncPlanSelectorItems();

        add(createWorkspaceSplitPane(treeSection, resultSection), BorderLayout.CENTER);

        List<PerformanceResultListener> resultListeners = List.of(
                new PerformanceStatsCollectorListener(statsCollector),
                new PerformanceTrendWindowCollectorListener(trendWindowCollector),
                new PerformanceResultTableVisualizer(
                        performanceResultTablePanel,
                        SettingManager::getPerformanceSlowRequestThreshold
                )
        );
        PerformanceRunUiController runUiController = new PerformanceRunUiController(
                runBtn,
                stopBtn,
                refreshBtn,
                List.of(
                        remoteModeCheckBox,
                        workerEndpointsField,
                        planSelector,
                        reportRefreshModeBox,
                        addPlanButton,
                        importBtn,
                        duplicatePlanButton,
                        renamePlanButton,
                        deletePlanButton
                )
        );
        executionEngine = new PerformanceExecutionEngine(
                () -> running,
                PerformanceExecutionConfig.uiSupplying(
                        () -> efficientMode,
                        SettingManager::getPerformanceResponseBodyPreviewLimitKb,
                        SettingManager::isPerformanceEventLoggingEnabled
                ),
                new PerformanceResultCollector(resultListeners),
                new PerformanceRunUiEventBridge(this, runUiController, progressLabel),
                () -> new HttpClientRuntimeConfig(
                        SettingManager.getPerformanceMaxIdleConnections(),
                        SettingManager.getPerformanceKeepAliveSeconds(),
                        SettingManager.getPerformanceMaxRequests(),
                        SettingManager.getPerformanceMaxRequestsPerHost()
                )
        );
        PerformanceRunSession runSession = new PerformanceRunSession(
                () -> running,
                value -> running = value,
                executionEngine
        );
        runControlSupport = new PerformanceRunControlSupport(
                this,
                () -> running,
                value -> running = value,
                () -> startTime,
                value -> startTime = value,
                propertyPanelSupport,
                executionEngine,
                runSession,
                statisticsCoordinator,
                timerManager,
                runUiController,
                efficientCheckBox,
                performanceResultTablePanel,
                statsCollector,
                this::clearCachedPerformanceResults
        );
        remoteRunControlSupport = new PerformanceRemoteRunControlSupport(
                () -> running,
                value -> running = value,
                runUiController,
                performanceReportPanel,
                performanceResultTablePanel,
                performanceTrendPanel,
                this::clearCachedPerformanceResults,
                () -> resultTabbedPane.setSelectedIndex(PerformancePanelViewFactory.RESULT_TAB_REPORT),
                () -> trendEnabled,
                () -> reportRealtimeEnabled,
                this::trendSamplingIntervalMs
        );

        treeSupport.syncAllRequestStructures((DefaultMutableTreeNode) treeModel.getRoot());
        syncTrendAvailableProtocols();
        runBtn.addActionListener(e -> startRun(progressLabel, limitLabel));
        stopBtn.addActionListener(e -> stopRun());
        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }
        selectFirstThreadGroup();
        setupSaveShortcut();
    }

    private JSplitPane createWorkspaceSplitPane(PerformancePanelViewFactory.TreeSection treeSection,
                                               PerformancePanelViewFactory.ResultSection resultSection) {
        JComponent treeContentPanel = treeSection.contentPanel();
        JComponent resultPanel = resultSection.resultPanel();
        JSplitPane propertyResultSplit = createPropertyResultSplitPane(resultPanel);
        JPanel rightContentPanel = createRightContentPanel(propertyResultSplit);

        treeContentPanel.setMinimumSize(new Dimension(220, 150));
        rightContentPanel.setMinimumSize(new Dimension(400, 300));

        JSplitPane mainSplit = createPerformanceSplitPane(treeContentPanel, rightContentPanel);
        mainSplit.setResizeWeight(0.18);
        return mainSplit;
    }

    private JSplitPane createPropertyResultSplitPane(JComponent resultPanel) {
        JSplitPane splitPane = AppToolWindowChrome.createVerticalInnerSplitPane(
                propertyPanel,
                resultPanel,
                220
        );
        splitPane.setResizeWeight(0.34);
        splitPane.setDividerLocation(220);
        propertyPanel.setMinimumSize(new Dimension(400, 140));
        resultPanel.setMinimumSize(new Dimension(400, 240));
        return splitPane;
    }

    private JPanel createRightContentPanel(Component centerContent) {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerContent, BorderLayout.CENTER);
        return panel;
    }

    static JSplitPane createPerformanceSplitPane(Component treeContentPanel, Component contentPanel) {
        return AppToolWindowChrome.createHorizontalCardSplitPane(
                treeContentPanel,
                contentPanel,
                AppToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
    }

    public void switchWorkspaceAndRefreshUI() {
        if (!isReadyForWorkspaceSwitch()) {
            return;
        }

        // chuyển đổi workspace Hủy bỏ các nhiệm vụ đang chờ xử lý trước để tránh các kịch bản hiệu suất cũ hoặc CSV Ảnh chụp trì hoãn viết mới workspace。
        autoSaveSupport.cancel();
        if (running) {
            stopRun();
        }

        PerformancePlanWorkspace persistedWorkspace = persistenceService.loadWorkspace();
        initializePlanState(persistedWorkspace);
        PerformancePlanConfiguration persistedConfiguration = activePlanConfiguration();
        applyPersistedSettings(persistedConfiguration);
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot(persistedConfiguration, activePlanName());
        treeModel.setRoot(root);
        treeSupport.syncAllRequestStructures(root);
        syncTrendAvailableProtocols();
        currentRequestNode = null;
        syncPlanSelectorItems();
        if (efficientCheckBox != null) {
            efficientCheckBox.setSelected(efficientMode);
        }
        if (trendCheckBox != null) {
            trendCheckBox.setSelected(trendEnabled);
        }
        if (reportRefreshModeBox != null) {
            reportRefreshModeBox.setSelectedIndex(reportRealtimeEnabled ? 1 : 0);
        }
        if (remoteModeCheckBox != null) {
            remoteModeCheckBox.setSelected(remoteExecutionEnabled);
        }
        if (workerEndpointsField != null) {
            workerEndpointsField.setText(remoteWorkerEndpoints);
        }
        clearCachedPerformanceResults();
        propertyCardLayout.show(propertyPanel, EMPTY);
        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }
        selectFirstThreadGroup();
    }

    private boolean isReadyForWorkspaceSwitch() {
        return treeModel != null
                && persistenceService != null
                && treeSupport != null
                && performanceTree != null
                && propertyPanel != null
                && propertyCardLayout != null
                && threadGroupPanel != null
                && csvDataSetPanel != null
                && extractorPanel != null
                && performanceResultTablePanel != null
                && performanceReportPanel != null
                && performanceTrendPanel != null
                && planSelector != null
                && executionEngine != null
                && remoteRunControlSupport != null;
    }

    private void initializePlanState(PerformancePlanWorkspace workspace) {
        PerformancePlanWorkspace safeWorkspace = workspace;
        if (safeWorkspace == null || safeWorkspace.getPlans().isEmpty()) {
            PerformanceSavedPlan defaultPlan = PerformanceSavedPlan.builder()
                    .id(UUID.randomUUID().toString())
                    .name(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN))
                    .build();
            safeWorkspace = PerformancePlanWorkspace.builder()
                    .activePlanId(defaultPlan.getId())
                    .plans(List.of(defaultPlan))
                    .build();
        }
        savedPlans = new ArrayList<>(safeWorkspace.getPlans());
        activePlanId = safeWorkspace.getActivePlanId();
    }

    private PerformancePlanConfiguration activePlanConfiguration() {
        PerformanceSavedPlan plan = activePlan();
        return plan == null ? null : plan.toConfiguration();
    }

    private PerformanceSavedPlan activePlan() {
        for (PerformanceSavedPlan plan : savedPlans) {
            if (plan.getId().equals(activePlanId)) {
                return plan;
            }
        }
        return savedPlans.isEmpty() ? null : savedPlans.get(0);
    }

    private String activePlanName() {
        PerformanceSavedPlan plan = activePlan();
        return plan == null ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN) : plan.getName();
    }

    private void applyPersistedSettings(PerformancePlanConfiguration configuration) {
        efficientMode = configuration == null || configuration.isEfficientMode();
        trendEnabled = configuration == null || configuration.isTrendEnabled();
        reportRealtimeEnabled = configuration != null && configuration.isReportRealtimeEnabled();
        PerformanceRemoteWorkerSettings remoteSettings = configuration == null
                ? PerformanceRemoteWorkerSettings.disabled()
                : configuration.getRemoteWorkerSettings();
        remoteExecutionEnabled = remoteSettings.isEnabled();
        remoteWorkerEndpoints = remoteSettings.getWorkerEndpoints();
        serviceResources = new ArrayList<>(configuration == null ? List.of() : configuration.getServiceResources());
        if (performanceReportPanel != null) performanceReportPanel.setServiceResources(serviceResources);
        applyTrendEnabled(trendEnabled);
        applyReportRealtimeEnabled(reportRealtimeEnabled);
    }

    private void setRemoteExecutionEnabled(boolean enabled) {
        remoteExecutionEnabled = enabled;
        saveConfig();
    }

    private void setRemoteWorkerEndpoints(String workerEndpoints) {
        remoteWorkerEndpoints = workerEndpoints == null ? "" : workerEndpoints.trim();
        saveConfig();
    }

    private PerformanceRemoteWorkerSettings currentRemoteWorkerSettings() {
        return PerformanceRemoteWorkerSettings.builder()
                .enabled(remoteModeCheckBox == null ? remoteExecutionEnabled : remoteModeCheckBox.isSelected())
                .workerEndpoints(workerEndpointsField == null ? remoteWorkerEndpoints : workerEndpointsField.getText())
                .build();
    }

    private void installPlanToolbarListeners() {
        if (planSelector != null) {
            planSelector.addActionListener(e -> {
                if (updatingPlanSelector || running) {
                    return;
                }
                int selectedIndex = planSelector.getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < savedPlans.size()) {
                    switchActivePlan(savedPlans.get(selectedIndex).getId());
                }
            });
        }
        if (addPlanButton != null) {
            addPlanButton.addActionListener(e -> addPlan());
        }
        if (duplicatePlanButton != null) {
            duplicatePlanButton.addActionListener(e -> duplicatePlan());
        }
        if (renamePlanButton != null) {
            renamePlanButton.addActionListener(e -> renamePlan());
        }
        if (deletePlanButton != null) {
            deletePlanButton.addActionListener(e -> deletePlan());
        }
    }

    private void syncPlanSelectorItems() {
        if (planSelector == null) {
            return;
        }
        updatingPlanSelector = true;
        try {
            planSelector.removeAllItems();
            int selectedIndex = -1;
            for (int i = 0; i < savedPlans.size(); i++) {
                PerformanceSavedPlan plan = savedPlans.get(i);
                planSelector.addItem(plan.getName());
                if (plan.getId().equals(activePlanId)) {
                    selectedIndex = i;
                }
            }
            if (selectedIndex >= 0) {
                planSelector.setSelectedIndex(selectedIndex);
            }
        } finally {
            updatingPlanSelector = false;
        }
    }

    private void addPlan() {
        saveCurrentPlanIntoMemory();
        String planName = nextPlanName(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN));
        PerformanceSavedPlan newPlan = PerformanceSavedPlan.builder()
                .id(UUID.randomUUID().toString())
                .name(planName)
                .build();
        savedPlans.add(newPlan);
        activePlanId = newPlan.getId();
        loadActivePlanIntoUi();
        persistCurrentWorkspaceSync();
    }

    private void duplicatePlan() {
        saveAllPropertyPanelData();
        saveCurrentPlanIntoMemory();
        PerformanceSavedPlan source = activePlan();
        if (source == null) {
            return;
        }
        DefaultMutableTreeNode copiedRoot = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
        PerformancePlanDocument copiedDocument = PerformanceSwingTreePlanAdapter.toDocument(copiedRoot);
        String planName = nextPlanName(source.getName() + " " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_COPY_SUFFIX));
        PerformanceSavedPlan copiedPlan = PerformanceSavedPlan.builder()
                .id(UUID.randomUUID().toString())
                .name(planName)
                .planDocument(copiedDocument)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .remoteWorkerSettings(currentRemoteWorkerSettings())
                .serviceResources(serviceResources)
                .build();
        savedPlans.add(copiedPlan);
        activePlanId = copiedPlan.getId();
        loadActivePlanIntoUi();
        persistCurrentWorkspaceSync();
    }

    private void renamePlan() {
        PerformanceSavedPlan currentPlan = activePlan();
        if (currentPlan == null) {
            return;
        }
        TextInputDialog.showRequiredName(
                this,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_RENAME_TITLE),
                currentPlan.getName(),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_NAME_EMPTY)
        ).ifPresent(newName -> {
            saveCurrentPlanIntoMemory();
            PerformanceSavedPlan latestPlan = activePlan();
            replaceSavedPlan(currentPlan.getId(), (latestPlan == null ? currentPlan : latestPlan).withName(newName));
            renameRootNode(newName);
            syncPlanSelectorItems();
            persistCurrentWorkspaceSync();
        });
    }

    private void deletePlan() {
        PerformanceSavedPlan currentPlan = activePlan();
        if (currentPlan == null) {
            return;
        }
        if (savedPlans.size() <= 1) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_DELETE_LAST_FORBIDDEN));
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_DELETE_CONFIRM, currentPlan.getName()),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_DELETE_TOOLTIP),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        int removedIndex = indexOfPlan(currentPlan.getId());
        savedPlans.removeIf(plan -> plan.getId().equals(currentPlan.getId()));
        int nextIndex = Math.min(Math.max(0, removedIndex), savedPlans.size() - 1);
        activePlanId = savedPlans.get(nextIndex).getId();
        loadActivePlanIntoUi();
        persistCurrentWorkspaceSync();
    }

    private void switchActivePlan(String planId) {
        if (planId == null || planId.equals(activePlanId)) {
            return;
        }
        saveCurrentPlanIntoMemory();
        activePlanId = planId;
        loadActivePlanIntoUi();
        persistCurrentWorkspaceSync();
    }

    private void loadActivePlanIntoUi() {
        PerformancePlanConfiguration configuration = activePlanConfiguration();
        applyPersistedSettings(configuration);
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot(configuration, activePlanName());
        treeModel.setRoot(root);
        treeSupport.syncAllRequestStructures(root);
        syncTrendAvailableProtocols();
        currentRequestNode = null;
        updateOptionControlsFromState();
        clearCachedPerformanceResults();
        propertyCardLayout.show(propertyPanel, EMPTY);
        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }
        selectFirstThreadGroup();
        syncPlanSelectorItems();
    }

    private void updateOptionControlsFromState() {
        if (efficientCheckBox != null) {
            efficientCheckBox.setSelected(efficientMode);
        }
        if (trendCheckBox != null) {
            trendCheckBox.setSelected(trendEnabled);
        }
        if (reportRefreshModeBox != null) {
            reportRefreshModeBox.setSelectedIndex(reportRealtimeEnabled ? 1 : 0);
        }
        if (remoteModeCheckBox != null) {
            remoteModeCheckBox.setSelected(remoteExecutionEnabled);
        }
        if (workerEndpointsField != null) {
            workerEndpointsField.setText(remoteWorkerEndpoints);
        }
    }

    private void saveCurrentPlanIntoMemory() {
        if (treeModel == null) {
            return;
        }
        if (activePlanId == null) {
            activePlanId = UUID.randomUUID().toString();
        }
        replaceSavedPlan(activePlanId, captureCurrentPlan());
    }

    private PerformanceSavedPlan captureCurrentPlan() {
        PerformanceSavedPlan currentPlan = activePlan();
        String planId = activePlanId == null ? UUID.randomUUID().toString() : activePlanId;
        String planName = currentPlan == null ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN) : currentPlan.getName();
        DefaultMutableTreeNode root = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
        PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(root);
        return PerformanceSavedPlan.fromConfiguration(
                planId,
                planName,
                PerformancePlanConfiguration.builder()
                        .planDocument(document)
                        .efficientMode(efficientMode)
                        .trendEnabled(trendEnabled)
                        .reportRealtimeEnabled(reportRealtimeEnabled)
                        .remoteWorkerSettings(currentRemoteWorkerSettings())
                        .serviceResources(serviceResources)
                        .build()
        );
    }

    private PerformancePlanWorkspace currentWorkspaceSnapshot() {
        saveCurrentPlanIntoMemory();
        return PerformancePlanWorkspace.builder()
                .activePlanId(activePlanId)
                .plans(savedPlans)
                .build();
    }

    private void editServiceResources() {
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, new Object[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_INSTANCES),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_CPU),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_RAM)}) {
            @Override public boolean isCellEditable(int row, int column) { return true; }
        };
        for (PerformanceServiceResource resource : serviceResources) {
            model.addRow(new Object[]{resource.getServiceName(), resource.getInstances(),
                    resource.getCpuCores(), resource.getMemoryGb()});
        }
        JTable table = new JTable(model);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton add = new JButton(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_ADD));
        JButton remove = new JButton(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_REMOVE));
        add.addActionListener(e -> model.addRow(new Object[]{"", 1, 1, 1}));
        remove.addActionListener(e -> { int row = table.getSelectedRow(); if (row >= 0) model.removeRow(row); });
        editor.add(com.nvhuong.devworkbench.common.component.ToolWindowActionToolbar.inlineLeft(add, remove), BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, editor,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_RESOURCES),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        List<PerformanceServiceResource> updated = new ArrayList<>();
        try {
            for (int row = 0; row < model.getRowCount(); row++) {
                String name = String.valueOf(model.getValueAt(row, 0)).trim();
                if (!name.isBlank()) {
                    updated.add(new PerformanceServiceResource(name,
                            Integer.parseInt(String.valueOf(model.getValueAt(row, 1))),
                            Double.parseDouble(String.valueOf(model.getValueAt(row, 2))),
                            Double.parseDouble(String.valueOf(model.getValueAt(row, 3)))));
                }
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_INVALID),
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_SERVICE_RESOURCES), JOptionPane.ERROR_MESSAGE);
            return;
        }
        serviceResources = updated;
        performanceReportPanel.setServiceResources(serviceResources);
        saveConfig();
    }

    private void persistCurrentWorkspaceSync() {
        if (persistenceService != null) {
            persistenceService.saveWorkspace(currentWorkspaceSnapshot());
        }
    }

    private void replaceSavedPlan(String planId, PerformanceSavedPlan replacement) {
        if (replacement == null) {
            return;
        }
        for (int i = 0; i < savedPlans.size(); i++) {
            if (savedPlans.get(i).getId().equals(planId)) {
                savedPlans.set(i, replacement);
                return;
            }
        }
        savedPlans.add(replacement);
    }

    private int indexOfPlan(String planId) {
        for (int i = 0; i < savedPlans.size(); i++) {
            if (savedPlans.get(i).getId().equals(planId)) {
                return i;
            }
        }
        return 0;
    }

    private String nextPlanName(String baseName) {
        String safeBaseName = baseName == null || baseName.isBlank()
                ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN)
                : baseName.trim();
        String candidate = safeBaseName;
        int index = 2;
        while (planNameExists(candidate)) {
            candidate = safeBaseName + " " + index;
            index++;
        }
        return candidate;
    }

    private boolean planNameExists(String name) {
        for (PerformanceSavedPlan plan : savedPlans) {
            if (plan.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void renameRootNode(String name) {
        if (treeModel == null || name == null || name.isBlank()) {
            return;
        }
        Object root = treeModel.getRoot();
        if (root instanceof DefaultMutableTreeNode rootNode
                && rootNode.getUserObject() instanceof PerformanceTreeNode nodeData) {
            nodeData.name = name;
            treeModel.nodeChanged(rootNode);
        }
    }

    private boolean isRemoteExecutionSelected() {
        return currentRemoteWorkerSettings().isEnabled();
    }

    private DefaultMutableTreeNode loadPersistedOrDefaultRoot(PerformancePlanConfiguration configuration,
                                                              String rootName) {
        PerformancePlanDocument document = configuration == null ? null : configuration.getPlanDocument();
        DefaultMutableTreeNode savedRoot = PerformanceSwingTreePlanAdapter.toTree(
                document,
                rootName == null || rootName.isBlank()
                        ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN)
                        : rootName
        );
        if (savedRoot != null) {
            return savedRoot;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode(
                rootName == null || rootName.isBlank()
                        ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN)
                        : rootName,
                NodeType.ROOT
        ));
        PerformanceTreeSupport.createDefaultRequest(root);
        return root;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Giữ các thanh công cụ bằng cách sử dụng phần đệm thẻ mà không có dải phân cách khi chuyển đổi chủ đề.
        if (topPanel != null) {
            PerformancePanelViewFactory.applyToolbarInsets(topPanel);
        }
    }

    /**
     * Chọn và nhắm mục tiêu cái đầu tiênThread Groupnút và kích hoạt sự kiện nhấp chuột tương ứng
     */
    private void selectFirstThreadGroup() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstGroup = (DefaultMutableTreeNode) root.getChildAt(0);
            TreePath path = new TreePath(firstGroup.getPath());
            performanceTree.setSelectionPath(path);
            performanceTree.scrollPathToVisible(path);

            // Kích hoạt sự kiện nhấp vào nút để đảm bảo rằng bảng thuộc tính được hiển thị chính xác
            Object userObj = firstGroup.getUserObject();
            if (userObj instanceof PerformanceTreeNode nodeData && nodeData.type == NodeType.THREAD_GROUP) {
                // Hiển thị bảng thuộc tính nhóm chủ đề
                propertyCardLayout.show(propertyPanel, THREAD_GROUP);
                threadGroupPanel.setThreadGroupData(nodeData);
            }
        }
    }

    private RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return treeSupport.resolveRequestProtocol(item);
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        treeSupport.syncRequestStructure(requestNode, requestData);
    }

    private void ensureRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        treeSupport.ensureRequestStructure(requestNode, requestData);
    }

    private void saveSseStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveSseStageNode(stageNode);
    }

    private void saveWebSocketStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveWebSocketStageNode(stageNode);
        if (stageNode != null && stageNode.getUserObject() instanceof PerformanceTreeNode stageNodeData
                && stageNodeData.type == NodeType.WS_CONNECT) {
            treeModel.nodeChanged(stageNode);
        }
    }

    private void switchRequestEditor(HttpRequestItem item) {
        requestEditorSupport.switchRequestEditor(item);
    }

    private void saveRequestNodeData(DefaultMutableTreeNode node) {
        PerformanceProtocol oldProtocol = PerformanceProtocol.HTTP;
        if (node != null && node.getUserObject() instanceof PerformanceTreeNode nodeData) {
            oldProtocol = treeSupport.resolvePerformanceProtocol(nodeData);
        }
        requestEditorSupport.saveRequestNodeData(node, this::syncRequestStructure);
        if (node != null && node.getUserObject() instanceof PerformanceTreeNode nodeData) {
            PerformanceProtocol newProtocol = treeSupport.resolvePerformanceProtocol(nodeData);
            if (oldProtocol != newProtocol) {
                ensureRequestStructure(node, nodeData);
            }
        }
        syncTrendAvailableProtocols();
    }

    /**
     * cài đặt Ctrl/Cmd+S Phím tắt lưu tất cả các cấu hình
     */
    private void setupSaveShortcut() {
        saveShortcutSupport.install(this, this::handleSaveShortcut);
    }

    /**
     * xử lý Ctrl/Cmd+S phím tắt
     * 1. Buộc nộp tất cả DevWorkbenchJSpinner giá trị
     * 2. Lưu tất cả dữ liệu bảng thuộc tính vào các nút cây
     * 3. Kiên trì nộp hồ sơ
     * 4. Hiển thị lời nhắc thành công
     */
    private void handleSaveShortcut() {
        autoSaveSupport.cancel();
        propertyPanelSupport.forceCommitAllSpinners();

        saveAllPropertyPanelData();

        persistenceService.saveWorkspace(currentWorkspaceSnapshot());
        NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SAVE_SUCCESS));
    }

    private void saveAllPropertyPanelData() {
        propertyPanelSupport.saveAllPropertyPanelData();
    }

    private void showUsageGuide() {
        PerformanceUsageGuideDialog.show(this);
    }

    private void importPerformancePlan() {
        if (running) {
            return;
        }
        autoSaveSupport.cancel();
        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();

        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "performance.plan.import",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_IMPORT_TITLE)
        );
        fileChooser.setFileFilter(FileChooserUtil.extensionFilter(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_IMPORT_FILE_FILTER),
                "json"
        ));
        int result = fileChooser.showOpenDialog(this);
        if (result != SystemFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
            return;
        }

        try {
            saveCurrentPlanIntoMemory();
            PerformancePlanImportResult importResult = planImportService.importPlans(selectedFile.toPath());
            int importedCount = appendImportedPlans(importResult);
            if (importedCount <= 0) {
                NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_IMPORT_EMPTY));
                return;
            }
            loadActivePlanIntoUi();
            persistCurrentWorkspaceSync();
            NotificationCenter.showSuccess(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_PLAN_IMPORT_SUCCESS,
                    importedCount
            ));
        } catch (Exception ex) {
            log.error("Failed to import performance plan", ex);
            NotificationCenter.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_PLAN_IMPORT_FAIL,
                    ex.getMessage()
            ));
        }
    }

    int appendImportedPlans(PerformancePlanImportResult importResult) {
        if (importResult == null || importResult.isEmpty()) {
            return 0;
        }
        String firstImportedPlanId = null;
        int importedCount = 0;
        for (var candidate : importResult.getPlans()) {
            if (candidate == null) {
                continue;
            }
            String planId = UUID.randomUUID().toString();
            String planName = nextPlanName(candidate.getName());
            PerformanceSavedPlan importedPlan = PerformanceSavedPlan.fromConfiguration(
                    planId,
                    planName,
                    candidate.getConfiguration()
            );
            savedPlans.add(importedPlan);
            if (firstImportedPlanId == null) {
                firstImportedPlanId = importedPlan.getId();
            }
            importedCount++;
        }
        if (firstImportedPlanId != null) {
            activePlanId = firstImportedPlanId;
        }
        return importedCount;
    }

    private void exportRunPlan() {
        autoSaveSupport.cancel();
        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();

        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "performance.runPlan.export",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_TITLE));
        fileChooser.setSelectedFile(new File("plan.json"));
        int result = fileChooser.showSaveDialog(this);
        if (result != SystemFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = ensureJsonExtension(fileChooser.getSelectedFile());
        try {
            DefaultMutableTreeNode root = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
            PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(root);
            PerformanceRunPlan runPlan = PerformanceRunPlanFactory.create(
                    PerformancePlanConfiguration.builder()
                            .planDocument(document)
                            .efficientMode(efficientMode)
                            .trendEnabled(trendEnabled)
                            .reportRealtimeEnabled(reportRealtimeEnabled)
                            .remoteWorkerSettings(currentRemoteWorkerSettings())
                            .serviceResources(serviceResources)
                            .build(),
                    EnvironmentService.getActiveEnvironment(),
                    GlobalVariablesService.getInstance().getGlobalVariables(),
                    AppConstants.APP_NAME + " " + SystemUtil.getCurrentVersion()
            );
            new PerformanceRunPlanJsonStorage().save(selectedFile.toPath(), runPlan);
            NotificationCenter.showSuccess(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_SUCCESS,
                    selectedFile.getAbsolutePath()
            ));
        } catch (Exception ex) {
            log.error("Failed to export performance run plan", ex);
            NotificationCenter.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_FAIL,
                    ex.getMessage()
            ));
        }
    }

    private File ensureJsonExtension(File selectedFile) {
        if (selectedFile == null) {
            return new File("plan.json");
        }
        String path = selectedFile.getAbsolutePath();
        if (path.toLowerCase().endsWith(".json")) {
            return selectedFile;
        }
        return new File(path + ".json");
    }

    private void handleCsvDataSetChanged() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) performanceTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof PerformanceTreeNode nodeData
                && nodeData.type == NodeType.CSV_DATA_SET) {
            treeModel.nodeChanged(selectedNode);
        }
        saveConfig();
    }

    // ========== Quản lý hẹn giờ ==========

    /**
     * Khởi tạo trình quản lý hẹn giờ
     */
    private void initTimerManager() {
        timerManager = new PerformanceTimerManager(() -> running);
    }

    // ========== Thực thi và dừng logic cốt lõi ==========
    private void startRun(JLabel progressLabel, JLabel limitLabel) {
        if (isRemoteExecutionSelected()) {
            startRemoteRun(progressLabel, limitLabel);
            return;
        }
        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();
        DefaultMutableTreeNode localRoot = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
        PerformanceRunPlan reportPlan = PerformanceRunPlanFactory.create(
                PerformancePlanConfiguration.builder()
                        .planDocument(PerformanceSwingTreePlanAdapter.toDocument(localRoot))
                        .efficientMode(efficientMode)
                        .trendEnabled(trendEnabled)
                        .reportRealtimeEnabled(reportRealtimeEnabled)
                        .remoteWorkerSettings(currentRemoteWorkerSettings())
                        .serviceResources(serviceResources)
                        .build(),
                EnvironmentService.getActiveEnvironment(),
                GlobalVariablesService.getInstance().getGlobalVariables(),
                AppConstants.APP_NAME + " " + SystemUtil.getCurrentVersion()
        );
        runThread = runControlSupport.startRun(
                (DefaultMutableTreeNode) treeModel.getRoot(),
                progressLabel,
                limitLabel,
                efficientMode,
                value -> efficientMode = value
        );
        Environment activeEnvironment = EnvironmentService.getActiveEnvironment();
        performanceReportPanel.setReportContext(reportPlan,
                activeEnvironment == null ? "" : activeEnvironment.getName());
    }

    private void startRemoteRun(JLabel progressLabel, JLabel limitLabel) {
        PerformanceRemoteWorkerSettings remoteSettings = currentRemoteWorkerSettings();
        List<PerformanceWorkerEndpoint> workers;
        try {
            workers = PerformanceWorkerEndpointParser.parse(remoteSettings.getWorkerEndpoints());
        } catch (IllegalArgumentException ex) {
            NotificationCenter.showError(ex.getMessage());
            return;
        }
        if (workers.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_REQUIRED));
            return;
        }

        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();

        try {
            DefaultMutableTreeNode root = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
            PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(root);
            PerformanceRunPlan runPlan = PerformanceRunPlanFactory.create(
                    PerformancePlanConfiguration.builder()
                            .planDocument(document)
                            .efficientMode(efficientMode)
                            .trendEnabled(trendEnabled)
                            .reportRealtimeEnabled(reportRealtimeEnabled)
                            .remoteWorkerSettings(remoteSettings)
                            .build(),
                    EnvironmentService.getActiveEnvironment(),
                    GlobalVariablesService.getInstance().getGlobalVariables(),
                    AppConstants.APP_NAME + " " + SystemUtil.getCurrentVersion()
            );
            performanceReportPanel.setReportContext(runPlan,
                    runPlan.getEnvironment() == null ? "" : runPlan.getEnvironment().getName());
            if (timerManager != null) {
                // Thực thi từ xa bằng worker Thăm dò ý kiến ​​để làm mới xu hướng và báo cáo, dừng cục bộ stats Hẹn giờ để tránh ghi đè ảnh chụp nhanh cục bộ trống master Báo cáo.
                timerManager.stopAll();
            }
            runThread = remoteRunControlSupport.startRun(runPlan, workers, progressLabel, limitLabel);
        } catch (Exception ex) {
            log.error("Failed to start remote performance run", ex);
            NotificationCenter.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_REMOTE_MSG_FAILED,
                    ex.getMessage()
            ));
        }
    }

    @Override
    protected void registerListeners() {
        RequestSaveEventPublisher.register(this::syncRequestItem);
        new PerformanceTreeInteractionSupport(
                this,
                performanceTree,
                treeModel,
                propertyCardLayout,
                propertyPanel,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                conditionPanel,
                whilePanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                treeSupport,
                this::saveRequestNodeData,
                this::saveSseStageNode,
                this::saveWebSocketStageNode,
                this::switchRequestEditor,
                this::syncRequestStructure,
                this::saveConfig,
                () -> currentRequestNode,
                node -> currentRequestNode = node,
                EMPTY,
                THREAD_GROUP,
                CSV_DATA_SET,
                LOOP,
                SIMPLE,
                CONDITION,
                WHILE,
                ONCE_ONLY,
                REQUEST,
                ASSERTION,
                EXTRACTOR,
                TIMER,
                SSE_CONNECT,
                SSE_READ,
                WS_CONNECT,
                WS_SEND,
                WS_READ,
                WS_CLOSE
        ).install();
    }

    private void stopRun() {
        if (isRemoteExecutionSelected() && remoteRunControlSupport != null) {
            remoteRunControlSupport.stopRun();
            return;
        }
        runControlSupport.stopRun();
    }

    /**
     * Lưu cấu hình hiện tại
     */
    private void saveConfig() {
        // Việc chỉnh sửa nút cây và các thay đổi trong bảng thuộc tính sẽ kích hoạt quá trình lưu và tính năng chống rung thống nhất giúp giảm việc ghi đĩa thường xuyên.
        syncTrendAvailableProtocols();
        autoSaveSupport.requestSave();
    }

    private void saveConfigAsync() {
        try {
            // Lưu tất cả dữ liệu bảng thuộc tính vào các nút cây
            saveAllPropertyPanelData();
            syncTrendAvailableProtocols();
            persistenceService.saveWorkspaceAsync(currentWorkspaceSnapshot());
        } catch (Exception e) {
            log.error("Failed to save performance config", e);
        }
    }

    /**
     * Lưu cấu hình kiểm tra hiệu năng (đối với các cuộc gọi bên ngoài, chẳng hạn như khi thoát)
     */
    public void save() {
        try {
            autoSaveSupport.cancel();
            if (treeModel == null || persistenceService == null) {
                return;
            }
            propertyPanelSupport.forceCommitAllSpinners();
            saveAllPropertyPanelData();
            persistenceService.saveWorkspace(currentWorkspaceSnapshot());
        } catch (Exception e) {
            log.error("Failed to save performance config", e);
        }
    }

    /**
     * Đồng bộ hóa mới nhất HttpRequestItem tới nút tương ứng trong cây kiểm tra hiệu năng (by Collections được gọi khi lưu)
     * Tránh người dùng editSubPanel Sau khi sửa đổi và lưu,PerformancePanel Dữ liệu cũ vẫn được giữ lại.
     *
     * @param item Dữ liệu yêu cầu đã lưu mới nhất
     */
    public void syncRequestItem(HttpRequestItem item) {
        requestSyncSupport.syncRequestItem(
                (DefaultMutableTreeNode) treeModel.getRoot(),
                item,
                currentRequestNode,
                this::switchRequestEditor
        );
    }

    /**
     * Dọn dẹp tài nguyên (được gọi khi thoát ứng dụng)
     * Đảm bảo bộ hẹn giờ được dừng chính xác để tránh rò rỉ tài nguyên
     */
    public void cleanup() {
        // 1. Dừng chạy thử nghiệm
        if (remoteRunControlSupport != null) {
            remoteRunControlSupport.stopRun();
        }
        if (running && !isRemoteExecutionSelected()) {
            runControlSupport.stopRun();
        }

        // 2. Dừng đồng hồ bấm giờ
        if (timerManager != null) {
            timerManager.stopAll();
            timerManager.dispose();
        }
        if (statisticsCoordinator != null) {
            statisticsCoordinator.dispose();
        }
        if (performanceResultTablePanel != null) {
            performanceResultTablePanel.dispose();
        }
    }

    /**
     * Chờ tất cả các điều khiển từ xa ở chế độ nền worker Các yêu cầu dừng đã hoàn thành chia sẻ ngân sách hết thời gian chờ thoát chung.
     */
    public boolean awaitRemoteStopOnExit(long timeout, TimeUnit unit)
            throws InterruptedException {
        return remoteRunControlSupport == null || remoteRunControlSupport.awaitStopOnExit(timeout, unit);
    }

    private void clearCachedPerformanceResults() {
        syncTrendAvailableProtocols();
        if (statisticsCoordinator != null) {
            statisticsCoordinator.resetForNewRun();
        }
        performanceResultTablePanel.clearResults();
        performanceReportPanel.clearReport();
        performanceTrendPanel.clearTrendDataset();
        statsCollector.clear();
        trendWindowCollector.clear();
        trendWindowCollector.setEnabled(trendEnabled);
        executionEngine.resetVirtualUsers();
    }

    public PerformanceResultTablePanel getPerformanceResultTablePanel() {
        return performanceResultTablePanel;
    }

    private void applyTrendEnabled(boolean enabled) {
        trendEnabled = enabled;
        trendWindowCollector.setEnabled(enabled);
        if (timerManager != null) {
            timerManager.setTrendSamplingEnabled(enabled);
        }
        syncTrendResultTabState();
    }

    private void applyReportRealtimeEnabled(boolean enabled) {
        reportRealtimeEnabled = enabled;
        if (timerManager != null) {
            timerManager.setReportRefreshEnabled(enabled);
        }
    }

    private void refreshReportSnapshotIfAllowed() {
        if (!shouldRefreshReportSnapshotNow()) {
            return;
        }
        refreshReportSnapshot();
    }

    boolean shouldRefreshReportSnapshotNow() {
        return !running || reportRealtimeEnabled;
    }

    private void refreshReportSnapshot() {
        if (isRemoteExecutionSelected()) {
            return;
        }
        if (statisticsCoordinator != null) {
            statisticsCoordinator.updateReportWithLatestDataSync();
        }
    }

    private long trendSamplingIntervalMs() {
        return Math.max(1, SettingManager.getTrendSamplingIntervalSeconds()) * 1000L;
    }

    private void syncTrendResultTabState() {
        if (trendCheckBox != null) {
            trendCheckBox.setSelected(trendEnabled);
        }
    }

    private void syncTrendAvailableProtocols() {
        if (performanceTrendPanel == null || treeSupport == null || treeModel == null) {
            return;
        }
        Object root = treeModel.getRoot();
        if (root instanceof DefaultMutableTreeNode rootNode) {
            Set<PerformanceProtocol> availableProtocols = treeSupport.collectAvailableProtocols(rootNode);
            performanceTrendPanel.setAvailableProtocols(availableProtocols);
            if (performanceReportPanel != null) {
                performanceReportPanel.setAvailableProtocols(availableProtocols);
            }
        }
    }

    /**
     * Làm mới dữ liệu yêu cầu từ bộ sưu tập
     * Tải lại cấu hình mới nhất cho tất cả các yêu cầu
     */
    private void refreshRequestsFromCollections() {
        currentRequestNode = requestSyncSupport.refreshRequestsFromCollections(
                currentRequestNode,
                this::switchRequestEditor,
                this::saveAllPropertyPanelData,
                this::clearCachedPerformanceResults,
                this::saveConfig
        );
    }
}
