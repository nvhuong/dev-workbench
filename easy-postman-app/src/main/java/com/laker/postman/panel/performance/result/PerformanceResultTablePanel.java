package com.laker.postman.panel.performance.result;

import com.laker.postman.request.model.HttpHeader;



import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.performance.model.PerformanceInternalHeaders;
import com.laker.postman.performance.model.PerformanceProtocolLabels;
import com.laker.postman.performance.model.ResultNodeInfo;
import com.laker.postman.performance.result.PerformanceResultMemoryBudget;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bảng kết quả kiểm tra hiệu suất
 * - 200ms Cơ chế làm mới gia tăng
 * - Hỗ trợ sắp xếp và lọc tìm kiếm sâu
 */
@Slf4j
public class PerformanceResultTablePanel extends JPanel {

    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;
    private TableRowSorter<ResultTableModel> rowSorter;
    private JScrollPane tableScroll;
    private JPanel contentHolder;
    private JSplitPane detailSplit;
    private JPanel detailPanel;
    private JLabel detailTitleLabel;
    private JLabel detailSummaryLabel;
    private JLabel rowCountLabel;
    private JCheckBox failedOnlyCheckBox;
    private int lastDividerLocation = -1;

    private SearchTextField searchField;

    static final int COMPACT_RESULT_ROW_LIMIT = 1_000;

    private final Queue<PendingResult> pendingQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingResultCount = new AtomicInteger(0);
    private final AtomicLong pendingResultBytes = new AtomicLong(0L);
    private final long retentionBudgetBytes = PerformanceResultMemoryBudget.defaultBudgetBytes();

    private static final int BATCH_SIZE = 2000;

    // Tìm kiếm bộ hẹn giờ chống rung (300ms）
    private Timer searchDebounceTimer;

    // UI Bộ hẹn giờ làm mới khung hình (200ms）
    private final Timer uiFrameTimer = new Timer(200, e -> {

        flushQueueOnEDT();
        tableModel.flushIfDirty();
    });

    public PerformanceResultTablePanel() {
        initUI();
        registerListeners();
        uiFrameTimer.start();
    }

    // Làm mới dữ liệu hàng đợi theo đợt TableModel
    private void flushQueueOnEDT() {
        List<PendingResult> batch = new ArrayList<>(1024);
        PendingResult pending;

        while ((pending = pendingQueue.poll()) != null) {
            pendingResultCount.decrementAndGet();
            pendingResultBytes.addAndGet(-pending.estimatedBytes());
            batch.add(pending);
            if (batch.size() >= BATCH_SIZE) break;
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);
        }
    }

    /**
     * Buộc xóa tất cả các kết quả đang chờ xử lý (kiểm tra đã dừng/được gọi khi hoàn thành)
     * Hãy chắc chắn pendingQueue Tất cả dữ liệu trong được hiển thị trong bảng
     */
    public void flushPendingResults() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::flushPendingResults);
            return;
        }

        List<PendingResult> batch = new ArrayList<>();
        PendingResult pending;

        // Làm mới tất cả các kết quả đang chờ xử lý cùng một lúc
        while ((pending = pendingQueue.poll()) != null) {
            pendingResultCount.decrementAndGet();
            pendingResultBytes.addAndGet(-pending.estimatedBytes());
            batch.add(pending);
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);
            log.debug("Buộc làm mới {} Kết quả đang chờ xử lý sẽ được thêm vào cây kết quả", batch.size());
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        searchField = new SearchTextField();
        searchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_SEARCH_PLACEHOLDER));

        failedOnlyCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_FAILED_ONLY));
        failedOnlyCheckBox.setOpaque(false);
        failedOnlyCheckBox.setFocusable(false);

        rowCountLabel = new JLabel();
        rowCountLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        rowCountLabel.setForeground(PerformanceTheme.resultMutedForeground());

        JPanel filterActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterActions.setOpaque(false);
        filterActions.add(failedOnlyCheckBox);
        filterActions.add(rowCountLabel);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(filterActions, BorderLayout.EAST);

        tableModel = new ResultTableModel(retentionBudgetBytes);
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setFocusable(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(PerformanceTheme.resultGridColor());
        ResultRowRenderer rowRenderer = new ResultRowRenderer();
        table.setDefaultRenderer(Object.class, rowRenderer);
        // Ngược lại, các cột số cũng đi đến cùng một trình kết xuất JTable Mặc định NumberRenderer Việc định dạng và kết hợp màu sắc sẽ bị bỏ qua.
        table.setDefaultRenderer(Number.class, rowRenderer);

        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        configureSorterComparators();
        configureColumnWidths();

        tableScroll = new JScrollPane(table);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(tableScroll, table);
        tableScroll.setMinimumSize(new Dimension(tableMinimumWidth(), 150));

        detailPanel = createDetailPanel();

        detailSplit = AppToolWindowChrome.createHorizontalInnerSplitPane(new JPanel(), new JPanel(), 0);
        detailSplit.setResizeWeight(0.5);

        // Theo mặc định, chỉ có bảng có chiều rộng đầy đủ được hiển thị; các chi tiết bên phải được mở rộng sau khi hàng được chọn.
        contentHolder = new JPanel(new BorderLayout());
        contentHolder.setOpaque(false);
        contentHolder.add(tableScroll, BorderLayout.CENTER);

        add(searchPanel, BorderLayout.NORTH);
        add(contentHolder, BorderLayout.CENTER);
        updateRowCount();
    }

    private JPanel createDetailPanel() {
        detailTitleLabel = new JLabel();
        detailTitleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        detailSummaryLabel = new JLabel();
        detailSummaryLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        detailSummaryLabel.setForeground(PerformanceTheme.resultMutedForeground());

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(detailTitleLabel);
        titles.add(detailSummaryLabel);

        JButton closeButton = new JButton(IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        closeButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_DETAIL_CLOSE));
        closeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        closeButton.setFocusable(false);
        closeButton.addActionListener(e -> table.clearSelection());

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 2));
        header.add(titles, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);

        detailTabs = new JTabbedPane();
        ToolWindowSurfaceStyle.applyTabbedPaneCard(detailTabs);

        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        panel.setMinimumSize(new Dimension(320, 150));
        panel.add(header, BorderLayout.NORTH);
        panel.add(detailTabs, BorderLayout.CENTER);
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeResultDetail");
        panel.getActionMap().put("closeResultDetail", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.clearSelection();
            }
        });
        return panel;
    }

    boolean isDetailVisible() {
        return detailPanel.getParent() == detailSplit && detailSplit.getParent() == contentHolder;
    }

    private void showDetailPanel() {
        if (isDetailVisible()) {
            return;
        }
        detailSplit.setLeftComponent(tableScroll);
        detailSplit.setRightComponent(detailPanel);
        contentHolder.removeAll();
        contentHolder.add(detailSplit, BorderLayout.CENTER);
        int width = contentHolder.getWidth();
        int minTableWidth = tableScroll.getMinimumSize().width;
        int location = lastDividerLocation > 0 && (width <= 0 || lastDividerLocation < width - 320)
                ? lastDividerLocation
                : Math.max(minTableWidth, (int) (width * 0.58));
        detailSplit.setDividerLocation(location);
        contentHolder.revalidate();
        contentHolder.repaint();
    }

    private void hideDetailPanel() {
        if (!isDetailVisible()) {
            return;
        }
        lastDividerLocation = detailSplit.getDividerLocation();
        contentHolder.removeAll();
        contentHolder.add(tableScroll, BorderLayout.CENTER);
        detailSplit.setRightComponent(null);
        contentHolder.revalidate();
        contentHolder.repaint();
    }

    // Cấu hình bộ so sánh sắp xếp
    private void configureSorterComparators() {
        rowSorter.setComparator(ResultTableModel.COL_PROTOCOL, Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER));
        rowSorter.setComparator(ResultTableModel.COL_NAME, Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER));
        // Mã trạng thái được sắp xếp theo số
        rowSorter.setComparator(ResultTableModel.COL_STATUS, Comparator.comparing((Object o) -> {
            String s = o.toString();
            if ("-".equals(s)) return 0;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }));
        rowSorter.setComparator(ResultTableModel.COL_START_TIME, Comparator.comparingLong(Long.class::cast));
        rowSorter.setComparator(ResultTableModel.COL_COST, Comparator.comparingInt(Integer.class::cast));
        rowSorter.setComparator(ResultTableModel.COL_LATENCY, Comparator.comparingLong(Long.class::cast));
        rowSorter.setComparator(ResultTableModel.COL_CONNECT, Comparator.comparingLong(Long.class::cast));
        rowSorter.setComparator(ResultTableModel.COL_ASSERTION, Comparator.comparing(String::toString));
    }

    // Định cấu hình độ rộng cột
    private void configureColumnWidths() {
        configureColumnWidth(ResultTableModel.COL_PROTOCOL, 64, 72, 95);
        configureColumnWidth(ResultTableModel.COL_NAME, 120, 200, Integer.MAX_VALUE);
        configureColumnWidth(ResultTableModel.COL_STATUS, 56, 64, 90);
        configureColumnWidth(ResultTableModel.COL_START_TIME, 92, 100, 120);
        configureColumnWidth(ResultTableModel.COL_COST, 72, 100, 130);
        configureColumnWidth(ResultTableModel.COL_LATENCY, 72, 92, 120);
        configureColumnWidth(ResultTableModel.COL_CONNECT, 64, 84, 120);
        configureColumnWidth(ResultTableModel.COL_ASSERTION, 56, 64, 100);
    }

    private void configureColumnWidth(int column, int min, int preferred, int max) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        // Không thể cắt bớt bản sao tiêu đề (đặc biệt là bằng tiếng Anh) và độ rộng tối thiểu ít nhất phải vừa với tiêu đề.
        int headerWidth = headerPreferredWidth(tableColumn);
        int effectiveMin = Math.max(min, headerWidth);
        tableColumn.setMinWidth(effectiveMin);
        tableColumn.setMaxWidth(Math.max(max, effectiveMin));
        tableColumn.setPreferredWidth(Math.max(preferred, effectiveMin));
    }

    private int headerPreferredWidth(TableColumn tableColumn) {
        javax.swing.table.TableCellRenderer renderer = tableColumn.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component header = renderer.getTableCellRendererComponent(
                table, tableColumn.getHeaderValue(), false, false, -1, tableColumn.getModelIndex());
        return header.getPreferredSize().width + 24;
    }

    private int tableMinimumWidth() {
        int width = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            width += table.getColumnModel().getColumn(i).getMinWidth();
        }
        return width + 4;
    }

    private void registerListeners() {
        // Chọn người nghe - Hiển thị chi tiết
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);
        tableModel.addTableModelListener(e -> SwingUtilities.invokeLater(this::updateRowCount));
        rowSorter.addRowSorterListener(e -> updateRowCount());
        failedOnlyCheckBox.addActionListener(e -> doFilter());


        // Tìm kiếm tính năng ổn định hình ảnh
        searchDebounceTimer = new Timer(300, e -> doFilter());
        searchDebounceTimer.setRepeats(false);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            private void scheduleFilter() {
                searchDebounceTimer.restart();
            }
        });

        // Enter phím để lọc ngay lập tức
        searchField.addActionListener(e -> {
            searchDebounceTimer.stop();
            doFilter();
        });
    }


    /**
     * Thực hiện lọc
     */
    private void doFilter() {
        String text = searchField.getText();
        String keyword = text == null ? "" : text.trim().toLowerCase();
        List<RowFilter<ResultTableModel, Integer>> filters = new ArrayList<>(2);
        if (!keyword.isEmpty()) {
            filters.add(new ResultRowFilter(keyword));
        }
        if (failedOnlyCheckBox.isSelected()) {
            filters.add(new FailedOnlyRowFilter());
        }
        rowSorter.setRowFilter(filters.isEmpty() ? null
                : filters.size() == 1 ? filters.get(0) : RowFilter.andFilter(filters));
        // Chuyển sang màu đỏ khi không có hàng nào hiển thị sau khi lọc từ khóa
        searchField.setNoResult(!keyword.isEmpty() && table.getRowCount() == 0);
    }

    private void updateRowCount() {
        int total = tableModel.getRowCount();
        int visible = table.getRowCount();
        rowCountLabel.setText(visible == total
                ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_ROW_COUNT_TOTAL, String.valueOf(total))
                : I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_ROW_COUNT_FILTERED,
                String.valueOf(visible), String.valueOf(total)));
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int row = table.getSelectedRow();
        if (row < 0) {
            clearDetailTabs();
            hideDetailPanel();
            return;
        }

        // Chuyển đổi chỉ mục xem thành chỉ mục mô hình
        int modelRow = table.convertRowIndexToModel(row);
        ResultNodeInfo info = tableModel.getRow(modelRow);
        if (info == null) {
            return;
        }
        renderDetail(info);
        showDetailPanel();
    }

    public void addResult(ResultNodeInfo info) {
        addResult(info, false);
    }

    public void addResult(ResultNodeInfo info, boolean compactRetention) {
        if (info == null) {
            return;
        }
        int rowLimit = compactRetention
                ? COMPACT_RESULT_ROW_LIMIT
                : SettingManager.getPerformanceResultRowLimit();
        long estimatedBytes = PerformanceResultMemoryBudget.estimateBytes(info);
        pendingQueue.offer(new PendingResult(info, rowLimit, estimatedBytes));
        pendingResultCount.incrementAndGet();
        pendingResultBytes.addAndGet(estimatedBytes);
        trimPendingQueueForRetention(rowLimit);
    }

    public void clearResults() {
        pendingQueue.clear();
        pendingResultCount.set(0);
        pendingResultBytes.set(0L);
        table.clearSelection();
        tableModel.clear();
        clearDetailTabs();
        hideDetailPanel();
    }

    /** Snapshot of currently retained result samples for report export; call on the EDT. */
    public List<com.laker.postman.performance.model.PerformanceSampleResult> reportSamples() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Report samples must be captured on the EDT");
        }
        flushPendingResults();
        List<com.laker.postman.performance.model.PerformanceSampleResult> samples = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ResultNodeInfo info = tableModel.getRow(i);
            if (info == null) continue;
            samples.add(com.laker.postman.performance.model.PerformanceSampleResult.builder()
                    .apiId(info.name).apiName(info.name).errorMsg(info.errorMsg).protocol(info.protocol)
                    .elapsedTimeMs(Math.max(0, info.costMs)).startTimeMs(info.timing.startTimeMs())
                    .endTimeMs(info.timing.startTimeMs() + Math.max(0, info.costMs)).responseCode(info.responseCode)
                    .executionFailed(info.executionFailed).interrupted(info.cancelled)
                    .successful(info.isActuallySuccessful())
                    .testResults(info.testResults == null ? List.of() : List.copyOf(info.testResults)).build());
        }
        return List.copyOf(samples);
    }

    private void trimPendingQueueForRetention(int rowLimit) {
        int effectiveLimit = Math.max(1, rowLimit);
        while (pendingResultCount.get() > effectiveLimit
                || (pendingResultCount.get() > 1 && pendingResultBytes.get() > retentionBudgetBytes)) {
            PendingResult removed = pendingQueue.poll();
            if (removed == null) {
                pendingResultCount.set(0);
                pendingResultBytes.set(0L);
                return;
            }
            pendingResultCount.decrementAndGet();
            pendingResultBytes.addAndGet(-removed.estimatedBytes());
        }
    }

    private void renderDetail(ResultNodeInfo info) {
        renderDetailHeader(info);
        detailTabs.removeAll();

        int responseTabIndex = -1;

        // 1. Request Tab (luôn hiển thị)
        if (info.req != null) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REQUEST),
                    HttpHtmlRenderer.renderRequest(info.req)
            );
        }

        // 2. Response Tab (luôn hiển thị, tổng hợp dữ liệu phản hồi và thông báo lỗi)
        String responseHtml = HttpHtmlRenderer.renderResponseWithError(info);
        addTabWithContent(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESPONSE),
                responseHtml
        );
        responseTabIndex = detailTabs.getTabCount() - 1;

        // 3. Tests Trang tab (chỉ hiển thị khi có kết quả xác nhận)
        if (info.testResults != null && !info.testResults.isEmpty()) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TESTS),
                    HttpHtmlRenderer.renderTestResults(info.testResults)
            );
        }

        // 4. Timing Trang tab (chỉ hiển thị khi có thông tin sự kiện)
        // 5. Event Info Trang tab (chỉ hiển thị khi có thông tin sự kiện)
        if (info.resp != null && info.resp.httpEventInfo != null) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TIMING),
                    HttpHtmlRenderer.renderTimingInfo(info.resp)
            );
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_EVENT_INFO),
                    HttpHtmlRenderer.renderEventInfo(info.resp)
            );
        }

        // Nếu có thông báo lỗi thì tự động chuyển sang Response trang tab
        if (info.errorMsg != null && !info.errorMsg.isEmpty() && responseTabIndex >= 0) {
            detailTabs.setSelectedIndex(responseTabIndex);
        }
    }

    /**
     * Thêm tab và đặt nội dung
     */
    private void addTabWithContent(String title, String html) {
        JEditorPane pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.setText(html);
        pane.setCaretPosition(0);
        ToolWindowSurfaceStyle.applyTextComponentCard(pane);
        JScrollPane scrollPane = new JScrollPane(pane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);
        detailTabs.addTab(title, scrollPane);
    }

    private void renderDetailHeader(ResultNodeInfo info) {
        String url = info.req != null ? info.req.url : null;
        String method = info.req != null ? info.req.method : null;
        detailTitleLabel.setText(info.name == null || info.name.isBlank() ? "-" : info.name);
        detailTitleLabel.setForeground(!info.cancelled && !info.isActuallySuccessful()
                ? PerformanceTheme.resultFailureForeground()
                : PerformanceTheme.tableForeground());
        detailTitleLabel.setToolTipText(url == null ? null : (method == null ? "" : method + " ") + url);
        detailSummaryLabel.setText(buildDetailSummary(info));
    }

    static String buildDetailSummary(ResultNodeInfo info) {
        String status = info.cancelled
                ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_CANCELLED)
                : info.responseCode > 0 ? String.valueOf(info.responseCode) : "-";
        return I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_DETAIL_SUMMARY,
                status,
                info.costMs + " ms",
                formatMillisWithUnit(info.timing.latencyMs()),
                formatMillisWithUnit(info.timing.connectMs()),
                ResultRowRenderer.formatStartTime(info.timing.startTimeMs()));
    }

    private static String formatMillisWithUnit(long millis) {
        return millis >= 0 ? millis + " ms" : "—";
    }

    private void clearDetailTabs() {
        detailTabs.removeAll();
    }

    // Dọn dẹp tài nguyên
    public void dispose() {
        uiFrameTimer.stop();
        if (searchDebounceTimer != null) {
            searchDebounceTimer.stop();
        }
    }

    static class FailedOnlyRowFilter extends RowFilter<ResultTableModel, Integer> {
        @Override
        public boolean include(Entry<? extends ResultTableModel, ? extends Integer> entry) {
            ResultNodeInfo info = entry.getModel().getRow(entry.getIdentifier());
            return info != null && !info.cancelled && !info.isActuallySuccessful();
        }
    }

    // Tùy chỉnh RowFilter - Hỗ trợ tìm kiếm sâu
    static class ResultRowFilter extends RowFilter<ResultTableModel, Integer> {
        private final String keyword;

        public ResultRowFilter(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public boolean include(Entry<? extends ResultTableModel, ? extends Integer> entry) {
            ResultTableModel model = entry.getModel();
            int row = entry.getIdentifier();
            ResultNodeInfo info = model.getRow(row);

            if (info == null) return false;

            // 1. Kiểm tra tên giao diện
            if (info.name != null && info.name.toLowerCase().contains(keyword)) {
                return true;
            }

            // 2. Kiểm tra các lỗi mà người dùng có thể nhìn thấy
            if (info.errorMsg != null && info.errorMsg.toLowerCase().contains(keyword)) {
                return true;
            }

            // 3. Kiểm tra nội dung yêu cầu (URL、Headers、Body）
            if (matchesRequest(info)) {
                return true;
            }

            // 4. Kiểm tra nội dung phản hồi (Headers、Body）
            return matchesResponse(info);
        }

        // Kiểm tra xem nội dung yêu cầu có khớp với từ khóa hay không
        private boolean matchesRequest(ResultNodeInfo info) {
            if (info.req == null) return false;

            // nghiên cứu URL
            if (info.req.url != null && info.req.url.toLowerCase().contains(keyword)) {
                return true;
            }

            // Kiểm tra yêu cầu Headers
            if (info.req.headersList != null) {
                for (HttpHeader header : info.req.headersList) {
                    if (!header.isEnabled()) continue;
                    String headerStr = (header.getKey() + ": " + header.getValue()).toLowerCase();
                    if (headerStr.contains(keyword)) {
                        return true;
                    }
                }
            }

            // Kiểm tra yêu cầu Body
            return info.req.body != null && info.req.body.toLowerCase().contains(keyword);
        }

        // Kiểm tra xem nội dung phản hồi có khớp với từ khóa hay không
        private boolean matchesResponse(ResultNodeInfo info) {
            if (info.resp == null) return false;

            // Kiểm tra phản hồi Headers
            if (info.resp.headers != null) {
                for (var entry : info.resp.headers.entrySet()) {
                    String key = entry.getKey();
                    if (PerformanceInternalHeaders.isInternalHeader(key)) {
                        continue;
                    }
                    List<String> values = entry.getValue();
                    String headerStr = (key + ": " + String.join(", ", values)).toLowerCase();
                    if (headerStr.contains(keyword)) {
                        return true;
                    }
                }
            }

            // Kiểm tra phản hồi Body
            return info.resp.body != null && info.resp.body.toLowerCase().contains(keyword);
        }

    }

    // TableModel - Tối ưu hóa làm mới gia tăng
    record PendingResult(ResultNodeInfo info, int rowLimit, long estimatedBytes) {
    }

    static class ResultTableModel extends AbstractTableModel {

        static final int COL_PROTOCOL = 0;
        static final int COL_NAME = 1;
        static final int COL_STATUS = 2;
        static final int COL_START_TIME = 3;
        static final int COL_COST = 4;
        static final int COL_LATENCY = 5;
        static final int COL_CONNECT = 6;
        static final int COL_ASSERTION = 7;
        static final int COLUMN_COUNT = 8;

        private final List<ResultNodeInfo> dataList = new ArrayList<>(1024);
        private final List<Long> rowBytes = new ArrayList<>(1024);
        private final long retentionBudgetBytes;
        private long retainedBytes = 0L;
        private boolean dirty = false;
        private boolean fullRefreshRequired = false;

        // Theo dõi vị trí bắt đầu của các hàng mới để làm mới dần dần
        private int firstNewRow = -1;

        ResultTableModel() {
            this(PerformanceResultMemoryBudget.defaultBudgetBytes());
        }

        ResultTableModel(long retentionBudgetBytes) {
            this.retentionBudgetBytes = Math.max(1L, retentionBudgetBytes);
        }

        long retainedBytes() {
            return retainedBytes;
        }

        @Override
        public int getRowCount() {
            return dataList.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_PROTOCOL -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_PROTOCOL);
                case COL_NAME -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_NAME);
                case COL_STATUS -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_STATUS);
                case COL_START_TIME -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_START_TIME);
                case COL_COST -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_COST);
                case COL_LATENCY -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_LATENCY);
                case COL_CONNECT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_CONNECT);
                case COL_ASSERTION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_ASSERTION);
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = dataList.get(row);
            return switch (col) {
                case COL_PROTOCOL -> PerformanceProtocolLabels.displayName(r.protocol);
                case COL_NAME -> r.name;
                case COL_STATUS -> r.cancelled
                        ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_CANCELLED)
                        : r.responseCode > 0 ? String.valueOf(r.responseCode) : "-";
                case COL_START_TIME -> r.timing.startTimeMs();
                case COL_COST -> r.costMs;
                case COL_LATENCY -> r.timing.latencyMs();
                case COL_CONNECT -> r.timing.connectMs();
                case COL_ASSERTION -> formatAssertion(r);
                default -> "";
            };
        }

        // Keep assertion symbols themeable; color comes from ResultRowRenderer.
        private String formatAssertion(ResultNodeInfo r) {
            if (r.cancelled) {
                return "—";
            }
            // 1. Nếu có kết quả khẳng định
            if (r.testResults != null && !r.testResults.isEmpty()) {
                return r.hasAssertionFailed() ? "×" : "✓";
            }

            // 2. Không có kiểm tra khẳng định
            if (!r.isActuallySuccessful()) {
                return "×";
            }

            return "—";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_PROTOCOL -> String.class;
                case COL_NAME -> String.class;
                case COL_STATUS -> String.class;
                case COL_START_TIME, COL_LATENCY, COL_CONNECT -> Long.class;
                case COL_COST -> Integer.class;
                case COL_ASSERTION -> String.class;
                default -> Object.class;
            };
        }

        ResultNodeInfo getRow(int row) {
            if (row < 0 || row >= dataList.size()) {
                return null;
            }
            return dataList.get(row);
        }


        void append(List<PendingResult> batch) {
            if (batch.isEmpty()) return;

            // Ghi lại vị trí bắt đầu của dòng mới
            if (firstNewRow == -1) {
                firstNewRow = dataList.size();
            }

            for (PendingResult pending : batch) {
                dataList.add(pending.info());
                rowBytes.add(pending.estimatedBytes());
                retainedBytes += pending.estimatedBytes();
                trimRowsForRetention(pending.rowLimit());
            }
            dirty = true;
        }

        void flushIfDirty() {
            if (!dirty) return;
            dirty = false;

            if (fullRefreshRequired) {
                fullRefreshRequired = false;
                firstNewRow = -1;
                fireTableDataChanged();
                return;
            }

            // Làm mới tăng dần: Chỉ thông báo các hàng mới
            if (firstNewRow != -1 && firstNewRow < dataList.size()) {
                int lastRow = dataList.size() - 1;
                fireTableRowsInserted(firstNewRow, lastRow);
                firstNewRow = -1; // Đặt lại
            }
        }

        void clear() {
            dataList.clear();
            rowBytes.clear();
            retainedBytes = 0L;
            dirty = false;
            fullRefreshRequired = false;
            firstNewRow = -1; // Đặt lại
            fireTableDataChanged();
        }

        private void trimRowsForRetention(int rowLimit) {
            int effectiveLimit = Math.max(1, rowLimit);
            int overflow = Math.max(0, dataList.size() - effectiveLimit);
            long bytesAfterRowTrim = retainedBytes;
            for (int i = 0; i < overflow; i++) {
                bytesAfterRowTrim -= rowBytes.get(i);
            }
            // Evict the oldest rows by bytes too, always keeping the newest row visible.
            while (bytesAfterRowTrim > retentionBudgetBytes && overflow < dataList.size() - 1) {
                bytesAfterRowTrim -= rowBytes.get(overflow);
                overflow++;
            }
            if (overflow > 0) {
                dataList.subList(0, overflow).clear();
                rowBytes.subList(0, overflow).clear();
                retainedBytes = bytesAfterRowTrim;
                fullRefreshRequired = true;
                firstNewRow = -1;
            }
        }
    }

    // Trình kết xuất hàng - Đặt căn chỉnh và màu sắc của các cột khác nhau
    static class ResultRowRenderer extends DefaultTableCellRenderer {
        private static final DateTimeFormatter START_TIME_FORMATTER =
                DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setFont(table.getFont());

            // Lấy chỉ mục cột
            int modelColumn = table.convertColumnIndexToModel(column);
            ResultNodeInfo info = getRowInfo(table, row);

            setBorder(createCellBorder(modelColumn, info, isSelected));
            setToolTipText(null);
            if (!isSelected) {
                setBackground(row % 2 == 1 ? alternateRowBackground(table) : table.getBackground());
                setForeground(table.getForeground());
            }

            // Đặt căn chỉnh và màu sắc
            switch (modelColumn) {
                case ResultTableModel.COL_NAME -> {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    if (info != null && info.req != null && info.req.url != null) {
                        setToolTipText(info.req.method == null ? info.req.url : info.req.method + " " + info.req.url);
                    }
                    if (!isSelected && info != null && !info.cancelled && !info.isActuallySuccessful()) {
                        setForeground(PerformanceTheme.resultFailureForeground());
                        setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                }
                case ResultTableModel.COL_STATUS -> {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    if (!isSelected && value != null && !"-".equals(value)) {
                        applyStatusColors(this, value.toString());
                    }
                }
                case ResultTableModel.COL_START_TIME -> {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setText(formatStartTime(value));
                }
                case ResultTableModel.COL_COST -> setHorizontalAlignment(SwingConstants.RIGHT);
                case ResultTableModel.COL_LATENCY, ResultTableModel.COL_CONNECT -> {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    setText(formatMillis(value));
                }
                case ResultTableModel.COL_PROTOCOL, ResultTableModel.COL_ASSERTION -> {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    if (modelColumn == ResultTableModel.COL_ASSERTION && !isSelected && info != null) {
                        applyAssertionColors(this, info);
                    }
                }
                default -> setHorizontalAlignment(SwingConstants.LEFT);
            }

            return this;
        }

        private static Color alternateRowBackground(JTable table) {
            Color alternate = UIManager.getColor("Table.alternateRowColor");
            if (alternate != null) {
                return alternate;
            }
            Color bg = table.getBackground();
            Color fg = table.getForeground();
            float ratio = 0.035f;
            return new Color(
                    Math.round(bg.getRed() + (fg.getRed() - bg.getRed()) * ratio),
                    Math.round(bg.getGreen() + (fg.getGreen() - bg.getGreen()) * ratio),
                    Math.round(bg.getBlue() + (fg.getBlue() - bg.getBlue()) * ratio));
        }

        static String formatStartTime(Object value) {
            if (value instanceof Long time && time > 0) {
                return START_TIME_FORMATTER.format(Instant.ofEpochMilli(time));
            }
            return "—";
        }

        static String formatMillis(Object value) {
            return value instanceof Long millis && millis >= 0 ? String.valueOf(millis) : "—";
        }

        private ResultNodeInfo getRowInfo(JTable table, int viewRow) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow < 0) {
                return null;
            }
            if (!(table.getModel() instanceof ResultTableModel model)) {
                return null;
            }
            return model.getRow(modelRow);
        }

        private javax.swing.border.Border createCellBorder(int modelColumn, ResultNodeInfo info, boolean isSelected) {
            int leftInset = modelColumn == ResultTableModel.COL_NAME ? 8 : 6;

            if (modelColumn == ResultTableModel.COL_NAME && info != null && !isSelected) {
                Color stripeColor = info.cancelled ? PerformanceTheme.resultMutedForeground()
                        : info.isActuallySuccessful() ? PerformanceTheme.resultSuccessStripe()
                        : PerformanceTheme.resultFailureStripe();
                return BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, stripeColor),
                        BorderFactory.createEmptyBorder(0, leftInset, 0, 6)
                );
            }

            return BorderFactory.createEmptyBorder(0, leftInset, 0, 6);
        }

        /**
         * Áp dụng màu dựa trên mã trạng thái - Tham khảo FunctionalRunnerTableModel
         */
        private void applyStatusColors(Component c, String status) {
            Color foreground = PerformanceTheme.tableForeground();
            if (I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_CANCELLED).equals(status)) {
                c.setForeground(PerformanceTheme.resultMutedForeground());
                return;
            }

            try {
                int code = Integer.parseInt(status);
                if (code >= 200 && code < 300) {
                    // Thành công: Sử dụng màu xanh lá cây
                    foreground = PerformanceTheme.resultSuccessForeground();
                } else if (code >= 400 && code < 500) {
                    // Lỗi máy khách: sử dụng màu cảnh báo
                    foreground = PerformanceTheme.resultWarningForeground();
                } else if (code >= 500) {
                    // Lỗi máy chủ: Sử dụng sai màu
                    foreground = PerformanceTheme.resultFailureForeground();
                }
            } catch (NumberFormatException e) {
                // Trạng thái không phải là số (chẳng hạn như thông báo lỗi)
                foreground = PerformanceTheme.resultFailureForeground();
            }

            // Chỉ đặt màu văn bản
            c.setForeground(foreground);
        }

        private void applyAssertionColors(Component c, ResultNodeInfo info) {
            if (info.cancelled) {
                c.setForeground(PerformanceTheme.resultMutedForeground());
                return;
            }
            if (info.testResults == null || info.testResults.isEmpty()) {
                c.setForeground(info.isActuallySuccessful()
                        ? PerformanceTheme.resultMutedForeground()
                        : PerformanceTheme.resultFailureForeground());
                return;
            }
            c.setForeground(info.hasAssertionFailed()
                    ? PerformanceTheme.resultFailureForeground()
                    : PerformanceTheme.resultSuccessForeground());
        }
    }
}
