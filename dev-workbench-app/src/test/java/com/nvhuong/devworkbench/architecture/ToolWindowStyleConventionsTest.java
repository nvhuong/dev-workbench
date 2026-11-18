package com.nvhuong.devworkbench.architecture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

public class ToolWindowStyleConventionsTest {
    private static final Path ROOT = findProjectRoot();
    private static final List<String> ALLOWED_NATIVE_SPLIT_PANES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/RequestViewFactory.java"
    );
    private static final List<String> ALLOWED_RAW_TOOL_WINDOW_SPLITS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/dialog/SnippetDialog.java"
    );
    private static final List<String> THEME_CONSTANTS = List.of(
            "ModernColors.PRIMARY",
            "ModernColors.PRIMARY_DARK",
            "ModernColors.SECONDARY",
            "ModernColors.ACCENT",
            "ModernColors.SUCCESS",
            "ModernColors.ERROR",
            "ModernColors.WARNING",
            "ModernColors.INFO",
            "ModernColors.NEUTRAL"
    );
    private static final List<String> NOTIFICATION_OVERLAYS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/update/AutoUpdateNotification.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/update/PluginUpdateNotification.java"
    );
    private static final List<String> SIDEBAR_SEARCH_TOOLBARS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/tree/CollectionTreeToolbar.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ToolboxPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/ui/KafkaTopicPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final List<String> SIDEBAR_SECTION_HEADERS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/ui/KafkaTopicPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final List<String> TOOL_WINDOW_ACTION_TOOLBARS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/ExecutionResultsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/FunctionalPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanelViewFactory.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/config/CsvDataSetPropertyPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/result/PerformanceReportPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/result/PerformanceTrendPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/ConsolePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/global/GlobalVariablesPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CronPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CryptoPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/DiffPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/EncoderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/HashPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/JsonToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/SqlToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/TimestampPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/UuidPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java",
            "dev-workbench-plugins/plugin-client-cert/src/main/java/com/nvhuong/devworkbench/plugin/clientcert/ClientCertificateSettingsPanel.java",
            "dev-workbench-plugins/plugin-decompiler/src/main/java/com/nvhuong/devworkbench/plugin/decompiler/DecompilerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/consumer/ui/KafkaConsumerPanel.java"
    );
    private static final List<String> TOOLBOX_EDITOR_WORKBENCH_PAGES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CronPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CryptoPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/DiffPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/EncoderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/HashPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/JsonToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/MarkdownToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/SqlToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/TimestampPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/UuidPanel.java"
    );
    private static final List<String> COMPACT_TOOL_WINDOW_FONT_PANELS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/dialog/WorkspaceSelectionDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/AuthTabPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerConfigDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CronPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/CryptoPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/DiffPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/EncoderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/HashPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/JsonToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/MarkdownToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/SqlToolPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/TimestampPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ToolboxPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/UuidPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/update/AutoUpdateNotification.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/update/PluginUpdateNotification.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanelViewFactory.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/components/ConvertToGitDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/components/GitOperationDialog.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/consumer/ui/KafkaConsumerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/producer/ui/KafkaProducerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/shared/ui/KafkaPropertiesEditorPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final Pattern ABSOLUTE_UI_FONT_PATTERN = Pattern.compile(
            "deriveFont\\([^\\n;]*\\d+(?:\\.\\d+)?f\\)|new Font\\("
    );
    private static final List<String> PRIMARY_TOOL_WINDOW_SURFACES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/MainPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/SidebarTabPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/RequestCollectionsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/FunctionalPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ToolboxPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/global/GlobalVariablesPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/ConsolePanel.java",
            "dev-workbench-plugins/plugin-capture/src/main/java/com/nvhuong/devworkbench/plugin/capture/CapturePanel.java",
            "dev-workbench-plugins/plugin-client-cert/src/main/java/com/nvhuong/devworkbench/plugin/clientcert/ClientCertificateSettingsPanel.java",
            "dev-workbench-plugins/plugin-decompiler/src/main/java/com/nvhuong/devworkbench/plugin/decompiler/DecompilerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/KafkaPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final List<String> LARGE_TOOL_WINDOW_LAYOUTS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/RequestCollectionsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/FunctionalPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ToolboxPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java"
    );
    private static final List<String> PRIMARY_HORIZONTAL_SPLITS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/RequestCollectionsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/ExecutionResultsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ToolboxPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/components/GitOperationDialog.java"
    );
    private static final List<String> STACKED_TOOL_WINDOW_SPLITS = List.of();
    private static final List<String> TOOL_WINDOW_DIALOG_SHELLS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/dialog/SnippetDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerConfigDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/cookie/CookieManagerDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/global/GlobalVariablesDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/topmenu/setting/ModernSettingsDialog.java"
    );
    private static final List<String> MAC_TITLE_BAR_AWARE_DIALOGS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerConfigDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/components/GitOperationDialog.java"
    );
    private static final List<String> SETTINGS_TABLE_SURFACES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/topmenu/setting/ClientCertificateSettingsPanelModern.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/topmenu/setting/TrustedCertificatesSettingsPanelModern.java"
    );
    private static final List<String> LIST_SCROLL_SURFACES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/mock/MockServerPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/WorkspacePanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/ui/KafkaTopicPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final List<String> TREE_SCROLL_SURFACES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/tree/CollectionTreePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/functional/ExecutionResultsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/PerformancePanelViewFactory.java",
            "dev-workbench-plugins/plugin-decompiler/src/main/java/com/nvhuong/devworkbench/plugin/decompiler/DecompilerPanel.java"
    );
    private static final List<String> BORDERLESS_SECTION_SURFACES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/MarkdownEditorPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/placeholder/RequestEditorPlaceholderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/placeholder/StartupShellPlaceholderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/AuthTabPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/RequestSettingsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/ResponsePanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/ResponseSizeTooltipWindow.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/ResponseTooltipBuilder.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/tree/CollectionGroupSelectionDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/history/HistoryPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/sidebar/global/GlobalVariablesPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/topmenu/plugin/PluginManagerDialog.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/topmenu/setting/ModernSettingsPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/workspace/components/ConvertToGitDialog.java",
            "dev-workbench-plugins/plugin-capture/src/main/java/com/nvhuong/devworkbench/plugin/capture/CapturePanel.java",
            "dev-workbench-plugins/plugin-decompiler/src/main/java/com/nvhuong/devworkbench/plugin/decompiler/DecompilerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/connection/ui/KafkaConnectionPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/consumer/ui/KafkaConsumerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/producer/ui/KafkaProducerPanel.java",
            "dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/shared/ui/KafkaPropertiesEditorPanel.java",
            "dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java"
    );
    private static final List<String> PLACEHOLDER_SHELLS_WITHOUT_DRAWN_DIVIDERS = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/placeholder/RequestEditorPlaceholderPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/placeholder/StartupShellPlaceholderPanel.java"
    );
    private static final List<String> ALLOWED_FUNCTIONAL_BORDER_FILES = List.of(
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/CsvDataPanel.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/DevWorkbenchTextField.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/StepIndicator.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/common/component/table/TableUIConstants.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/collections/editor/request/sub/RequestBodyVariableAutocompleteController.java",
            "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/performance/result/PerformanceResultTablePanel.java"
    );

    @Test
    public void swingPanelsShouldNotIntroduceTitledBorders() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("TitledBorder") || source.contains("createTitledBorder");
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use explicit label/header sections instead of TitledBorder: " + violations);
    }

    @Test
    public void toolWindowsShouldUseSharedSplitPaneChrome() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> readUnchecked(path).contains("new JSplitPane"))
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_NATIVE_SPLIT_PANES.contains(path))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowChrome.create*SplitPane for tool-window layouts: " + violations);
    }

    @Test
    public void largeToolWindowsShouldUseCardSplitPaneConvenienceApi() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("ToolWindowChrome.createHorizontalSplitPane(")
                            || source.contains("ToolWindowChrome.createVerticalSplitPane(")
                            || source.contains("AppToolWindowChrome.createHorizontalSplitPane(")
                            || source.contains("AppToolWindowChrome.createVerticalSplitPane(")
                            || source.contains("ToolWindowChrome.createHorizontalDragGapSplitPane(")
                            || source.contains("ToolWindowChrome.createVerticalDragGapSplitPane(");
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_RAW_TOOL_WINDOW_SPLITS.contains(path))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowChrome.create*CardSplitPane for rounded tool-window layouts: " + violations);
    }

    @Test
    public void popupMenusShouldUseSharedToolWindowPopupStyle() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> {
                    String[] lines = readUnchecked(path).split("\\R");
                    for (int i = 0; i < lines.length; i++) {
                        if (!lines[i].contains("new JPopupMenu(")) {
                            continue;
                        }
                        String nextLines = String.join("\n",
                                lines[Math.min(i + 1, lines.length - 1)],
                                lines[Math.min(i + 2, lines.length - 1)]);
                        if (!nextLines.contains("ToolWindowSurfaceStyle.applyPopupMenuCard")) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle.applyPopupMenuCard for popup menus: " + violations);
    }

    @Test
    public void toolWindowsShouldUseSemanticThemeColorsForDividersAndSecondaryText() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("UIManager.getColor(\"Separator.foreground\")")
                            || source.contains("UIManager.getColor(\"Label.disabledForeground\")")
                            || source.contains("UIManager.getColor(\"List.hoverBackground\")")
                            || source.contains("UIManager.getColor(SEPARATOR_FG)")
                            || source.contains("Color.GRAY")
                            || source.contains("Color.LIGHT_GRAY")
                            || source.contains("new Color(128, 128, 128)")
                            || source.contains("new Color(150, 150, 150)")
                            || source.contains("new Color(220, 53, 69)");
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors semantic colors for dividers and secondary text: " + violations);
    }

    @Test
    public void customPaintedNotificationsShouldUseSemanticNotificationColors() {
        List<String> violations = NOTIFICATION_OVERLAYS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return source.contains("UIManager.getColor(\"Panel.background\")")
                            || source.contains("UIManager.getColor(\"List.selectionBackground\")")
                            || source.contains("ModernColors.getBorderLightColor()");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors notification colors for painted notifications: " + violations);
    }

    @Test
    public void primarySidebarSearchBarsShouldUseSharedToolbarMetrics() {
        List<String> violations = SIDEBAR_SEARCH_TOOLBARS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSidebarToolbar"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSidebarToolbar for primary sidebar search bars: " + violations);
    }

    @Test
    public void primarySidebarSectionHeadersShouldUseSharedHeaderMetrics() {
        List<String> violations = SIDEBAR_SECTION_HEADERS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSidebarHeader"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSidebarHeader for compact sidebar section headers: " + violations);
    }

    @Test
    public void compactToolWindowActionRowsShouldUseSharedToolbarMetrics() {
        List<String> violations = TOOL_WINDOW_ACTION_TOOLBARS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowActionToolbar")
                            && !source.contains("ToolWindowSidebarToolbar")
                            && !source.contains("ToolboxWorkbench.");
                })
                .toList();

        assertTrue(violations.isEmpty(),
                "Use shared toolbar metrics for compact tool-window action rows: " + violations);
    }

    @Test
    public void toolboxEditorPagesShouldUseSharedWorkbenchChrome() {
        List<String> violations = TOOLBOX_EDITOR_WORKBENCH_PAGES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolboxWorkbench."))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolboxWorkbench for consistent toolbox editor page chrome: " + violations);
    }

    @Test
    public void environmentNewItemShouldUseThemeAwarePlusIcon() {
        String source = readUnchecked(ROOT.resolve(
                "dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/env/EnvironmentPanel.java"));

        assertTrue(source.contains("IconUtil.createThemed(\"icons/plus.svg\""),
                "The environment popup's neutral plus icon must use IconUtil.createThemed so it follows light/dark themes");
    }

    @Test
    public void primaryToolWindowPanelsShouldUseSharedSurfaceStyle() {
        List<String> violations = PRIMARY_TOOL_WINDOW_SURFACES.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowSurfaceStyle.applyBackground(this)")
                            && !source.contains("ToolWindowSurfaceStyle.applyCard(this)")
                            && !source.contains("ToolWindowSurfaceStyle.applyDialogSurface(this)");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle at primary tool-window roots: " + violations);
    }

    @Test
    public void compactToolWindowPanelsShouldRespectConfiguredUiFontSize() {
        List<String> violations = COMPACT_TOOL_WINDOW_FONT_PANELS.stream()
                .filter(path -> ABSOLUTE_UI_FONT_PATTERN.matcher(readUnchecked(ROOT.resolve(path))).find())
                .toList();

        assertTrue(violations.isEmpty(), "Use FontsUtil offsets instead of absolute UI font sizes: " + violations);
    }

    @Test
    public void largeToolWindowLayoutsShouldUseSharedChromeWrappers() {
        List<String> violations = LARGE_TOOL_WINDOW_LAYOUTS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowChrome.createHorizontalCardSplitPane")
                            && !source.contains("ToolWindowChrome.createHorizontalDragGapCardSplitPane")
                            && !source.contains("AppToolWindowChrome.createHorizontalCardSplitPane")
                            && !source.contains("ToolWindowChrome.createVerticalCardSplitPane")
                            && !source.contains("ToolWindowChrome.createVerticalDragGapCardSplitPane")
                            && !source.contains("ToolWindowChrome.createVerticalDragGapStackedCardSplitPane")
                            && !source.contains("AppToolWindowChrome.createVerticalCardSplitPane")
                            && !source.contains("AppToolWindowChrome.createVerticalStackedCardSplitPane")
                            && !source.contains("ToolWindowChrome.wrapToolWindow")
                            && !source.contains("AppToolWindowChrome.wrapToolWindow")
                            && !source.contains("ToolWindowChrome.wrapInsetToolWindow")
                            && !source.contains("AppToolWindowChrome.wrapInsetToolWindow")
                            && !source.contains("ToolWindowChrome.wrapLeftInsetToolWindow")
                            && !source.contains("AppToolWindowChrome.wrapLeftInsetToolWindow");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowChrome wrappers for large tool-window layouts: " + violations);
    }

    @Test
    public void primaryHorizontalSplitsShouldUseSharedDefaultSidebarWidth() {
        List<String> violations = PRIMARY_HORIZONTAL_SPLITS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowChrome.DEFAULT_SIDE_WIDTH")
                            && !source.contains("AppToolWindowChrome.DEFAULT_SIDE_WIDTH");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use shared default side width for primary side panes: " + violations);
    }

    @Test
    public void stackedToolWindowSplitsShouldAvoidDoubleWrappingNestedCards() {
        List<String> violations = STACKED_TOOL_WINDOW_SPLITS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("createVerticalStackedCardSplitPane")
                            && !source.contains("createVerticalDragGapStackedCardSplitPane");
                })
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowChrome.createVertical*StackedCardSplitPane when the top pane already has card chrome: "
                        + violations);
    }

    @Test
    public void toolWindowDialogsShouldUseSharedDialogSurfaces() {
        List<String> violations = TOOL_WINDOW_DIALOG_SHELLS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowChrome.wrapDialogToolWindow")
                            && !source.contains("ToolWindowChrome.wrapDialogInsetToolWindow")
                            && !source.contains("AppToolWindowChrome.wrapDialogToolWindow")
                            && !source.contains("AppToolWindowChrome.wrapDialogInsetToolWindow")
                            && !source.contains("ToolWindowSurfaceStyle.applyDialogSurface");
                })
                .toList();

        assertTrue(violations.isEmpty(),
                "Use shared dialog surface styling for tool-window dialogs: " + violations);
    }

    @Test
    public void macTitleBarAwareDialogsShouldUseSharedChromeInset() {
        List<String> violations = MAC_TITLE_BAR_AWARE_DIALOGS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowSurfaceStyle.applyDialogWindowChrome(this)")
                            || source.contains("getRootPane().setBorder");
                })
                .toList();

        assertTrue(violations.isEmpty(),
                "Use shared dialog chrome and do not reset rootPane border/titlebar inset: " + violations);
    }

    @Test
    public void appCodeShouldKeepHostSpecificDragGapBehindAppChromeWrapper() throws IOException {
        List<String> violations = appJavaSources()
                .filter(path -> !path.endsWith("AppToolWindowChrome.java"))
                .filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("ToolWindowChrome.DRAG_GAP")
                            || source.contains("ToolWindowChrome.createHorizontalDragGap")
                            || source.contains("ToolWindowChrome.createVerticalDragGap")
                            || source.contains("ToolWindowChrome.SplitDividerStyle.DRAG_GAP");
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use AppToolWindowChrome for host-specific drag-gap split chrome: " + violations);
    }

    @Test
    public void pluginsShouldNotUseHostSpecificDragGapSplitChrome() throws IOException {
        List<String> violations = pluginJavaSources()
                .filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("DragGap")
                            || source.contains("DRAG_GAP")
                            || source.contains("SplitDividerStyle.DRAG_GAP");
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .toList();

        assertTrue(violations.isEmpty(),
                "Plugins should use stable ToolWindowChrome defaults, not host-specific drag-gap chrome: " + violations);
    }

    @Test
    public void settingsTablesShouldUseSharedSurfaceStyle() {
        List<String> violations = SETTINGS_TABLE_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyTableScrollPaneCard"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle table wrappers for settings tables: " + violations);
    }

    @Test
    public void sideListAndTreeScrollPanesShouldUseSharedSurfaceStyle() {
        List<String> listViolations = LIST_SCROLL_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyListScrollPaneCard"))
                .toList();
        List<String> treeViolations = TREE_SCROLL_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyTreeScrollPaneCard"))
                .toList();

        assertTrue(listViolations.isEmpty(), "Use shared list scroll-pane surface style: " + listViolations);
        assertTrue(treeViolations.isEmpty(), "Use shared tree scroll-pane surface style: " + treeViolations);
    }

    @Test
    public void highLevelSectionSurfacesShouldNotUseHandDrawnDividers() {
        List<String> violations = BORDERLESS_SECTION_SURFACES.stream()
                .filter(path -> containsLegacySectionBorder(readUnchecked(ROOT.resolve(path))))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowSurfaceStyle.applySectionHeader or borderless section surfaces instead of hand-drawn dividers: "
                        + violations);
    }

    @Test
    public void placeholderShellsShouldNotPaintLegacyDividerLines() {
        List<String> violations = PLACEHOLDER_SHELLS_WITHOUT_DRAWN_DIVIDERS.stream()
                .filter(path -> readUnchecked(ROOT.resolve(path)).contains("drawLine("))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use card gaps/backgrounds in placeholder shells instead of painted divider lines: " + violations);
    }

    @Test
    public void legacySwingBordersShouldRemainLimitedToFunctionalFeedback() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> containsLegacySectionBorder(readUnchecked(path)))
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_FUNCTIONAL_BORDER_FILES.contains(path))
                .toList();

        assertTrue(violations.isEmpty(),
                "Do not add ad-hoc Swing borders outside explicit focus, popup, table-button, or status-stripe feedback: "
                        + violations);
    }

    @Test
    public void appAndPluginCodeShouldUseThemeAwareModernColorGetters() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return THEME_CONSTANTS.stream().anyMatch(source::contains);
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors getter methods so light/dark themes can diverge safely: " + violations);
    }

    private static boolean containsLegacySectionBorder(String source) {
        return source.contains("createMatteBorder")
                || source.contains("createLineBorder")
                || source.contains("createCompoundBorder")
                || source.contains("new MatteBorder");
    }

    private static Stream<Path> javaSources() throws IOException {
        return Stream.concat(appJavaSources(), pluginJavaSources());
    }

    private static Stream<Path> appJavaSources() {
        Path appSourceRoot = ROOT.resolve("dev-workbench-app/src/main/java");
        if (!Files.isDirectory(appSourceRoot)) {
            return Stream.empty();
        }
        return walkUnchecked(appSourceRoot)
                .filter(path -> path.toString().endsWith(".java"));
    }

    private static Stream<Path> pluginJavaSources() throws IOException {
        return pluginMainSourceRoots()
                .filter(Files::isDirectory)
                .flatMap(ToolWindowStyleConventionsTest::walkUnchecked)
                .filter(path -> path.toString().endsWith(".java"));
    }

    private static Stream<Path> pluginMainSourceRoots() throws IOException {
        Path pluginsRoot = ROOT.resolve("dev-workbench-plugins");
        if (!Files.isDirectory(pluginsRoot)) {
            return Stream.empty();
        }
        try (Stream<Path> pluginDirs = Files.list(pluginsRoot)) {
            return pluginDirs
                    .filter(Files::isDirectory)
                    .map(path -> path.resolve("src/main/java"))
                    .toList()
                    .stream();
        }
    }

    private static Stream<Path> walkUnchecked(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("dev-workbench-app"))
                    && Files.isDirectory(current.resolve("dev-workbench-ui"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find dev-workbench project root");
    }
}
