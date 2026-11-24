package com.nvhuong.devworkbench.panel.functional;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import com.nvhuong.devworkbench.common.constants.Icons;
import com.nvhuong.devworkbench.common.component.ToolWindowActionToolbar;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.functional.model.AssertionResult;
import com.nvhuong.devworkbench.functional.model.BatchExecutionHistory;
import com.nvhuong.devworkbench.functional.model.IterationResult;
import com.nvhuong.devworkbench.functional.model.RequestResult;
import com.nvhuong.devworkbench.common.component.HttpRequestDisplayMetadata;
import com.nvhuong.devworkbench.service.render.HttpHtmlRenderer;
import com.nvhuong.devworkbench.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Bảng hiển thị kết quả thực hiện - Tương tự DevWorkbench của Runner Results
 */
public class ExecutionResultsPanel extends JPanel {
    public static final String TEXT_HTML = "text/html";
    private static final String ICON_HTTP = "icons/http.svg";

    private JTree resultsTree;
    private DefaultTreeModel treeModel;
    private JPanel detailPanel;
    private JTabbedPane detailTabs;
    private transient BatchExecutionHistory executionHistory; // Lịch sử thực hiện hiện tại
    private TreePath lastSelectedPath; // Lưu đường dẫn đã chọn cuối cùng
    private int lastSelectedRequestDetailTabIndex = 0; // Hãy nhớ người dùng đang ở đóRequestDetailđã chọntabchỉ số

    public ExecutionResultsPanel() {
        initUI();
        registerListeners();
    }

    /**
     * Định dạng dấu thời gian dưới dạng HH:mm:ss định dạng
     */
    private String formatTimestamp(long timestamp) {
        // Trình định dạng ngày
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return dateFormat.format(new Date(timestamp));
    }

    private void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Tạo bảng phân chia
        createResultsTree();
        createDetailPanel();
        JSplitPane splitPane = AppToolWindowChrome.createHorizontalInnerSplitPane(
                createTreePanel(),
                detailPanel,
                AppToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        splitPane.setResizeWeight(0.35);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createResultsTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS));
        treeModel = new DefaultTreeModel(root);
        resultsTree = new JTree(treeModel);
        resultsTree.setRootVisible(true);
        resultsTree.setShowsRootHandles(true);
        resultsTree.setCellRenderer(new ExecutionResultTreeCellRenderer());
        resultsTree.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        resultsTree.setRowHeight(26);
        resultsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Bật chú giải công cụ
        ToolTipManager.sharedInstance().registerComponent(resultsTree);
    }

    private JPanel createTreePanel() {
        JPanel treePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(treePanel);
        treePanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Thêm thanh tiêu đề (chứa các nút tiêu đề và công cụ)
        JPanel headerPanel = createTreeHeaderPanel();
        treePanel.add(headerPanel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = new JScrollPane(resultsTree);
        treeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(treeScrollPane, resultsTree);
        treePanel.add(treeScrollPane, BorderLayout.CENTER);

        return treePanel;
    }

    private JPanel createTreeHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(headerPanel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // Tiêu đề bên trái
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_HISTORY));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Các nút công cụ ở bên phải
        JPanel toolBar = createToolBar();
        headerPanel.add(toolBar, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createToolBar() {
        // Chỉ sử dụng các nút biểu tượng để đơn giản
        JButton expandAllBtn = createIconButton("icons/expand.svg",
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_EXPAND_ALL));
        expandAllBtn.addActionListener(e -> expandAll());

        JButton collapseAllBtn = createIconButton("icons/collapse.svg",
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_COLLAPSE_ALL));
        collapseAllBtn.addActionListener(e -> collapseAll());

        JButton refreshBtn = createIconButton("icons/refresh.svg",
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_REFRESH));
        refreshBtn.addActionListener(e -> refreshData());

        JButton exportImageBtn = createIconButton("icons/export.svg",
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_EXPORT));
        exportImageBtn.addActionListener(e -> exportReportImage(exportImageBtn));

        return ToolWindowActionToolbar.inlineRight(exportImageBtn, expandAllBtn, collapseAllBtn, refreshBtn);
    }

    /**
     * Saves the whole run (summary, every request row and failure details) as a PNG report.
     */
    private void exportReportImage(JButton button) {
        if (executionHistory == null || executionHistory.getIterations().isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_EMPTY));
            return;
        }
        SystemFileChooser chooser = FileChooserUtil.createSaveFileChooser(
                "functional.report.image", I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_EXPORT));
        chooser.setFileFilter(FileChooserUtil.extensionFilter(
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_FILE_FILTER) + " (*.png)", "png"));
        chooser.setSelectedFile(new java.io.File("functional-report-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".png"));
        if (chooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) return;
        java.nio.file.Path selected = chooser.getSelectedFile().toPath();
        java.nio.file.Path destination = selected.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")
                ? selected : java.nio.file.Path.of(selected + ".png");
        // Labels and data are read on the EDT; rendering and writing happen in the background.
        BatchExecutionHistory history = executionHistory;
        FunctionalReportImage.Labels labels = reportLabels();
        Image logo = Icons.LOGO.getImage();
        button.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                FunctionalReportImage.write(
                        FunctionalReportImage.render(history, labels, logo, System.currentTimeMillis()), destination);
                return null;
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    get();
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_SAVED) + " " + destination);
                } catch (Exception ex) {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_FAILED));
                }
            }
        }.execute();
    }

    static FunctionalReportImage.Labels reportLabels() {
        return new FunctionalReportImage.Labels(
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_TITLE),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_GENERATED),
                new String[]{
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_ITERATIONS),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_REQUESTS),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_SUCCESS_RATE),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_TIME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_START_TIME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_END_TIME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_AVERAGE_TIME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS)},
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS_COMPLETED),
                new String[]{
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ITERATION),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_REQUEST_NAME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_COLUMN_URL),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_METHOD),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_STATUS),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_TIME),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_RESULT)},
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_RESULT_PASS),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_RESULT_FAIL),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_RESULT_NONE),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_FAILURES),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_REPORT_MORE_ROWS),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_SKIPPED));
    }

    private JButton createIconButton(String iconPath, String tooltip) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, 14, 14));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(24, 24));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void createDetailPanel() {
        detailPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(detailPanel);
        detailPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        // Tạo một tab chi tiết
        detailTabs = new JTabbedPane();
        ToolWindowSurfaceStyle.applyTabbedPaneCard(detailTabs);
        detailTabs.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        detailTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Trang chào mừng được hiển thị theo mặc định
        showWelcomePanel();
        detailPanel.add(detailTabs, BorderLayout.CENTER);


        detailTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Xác định chỉ số tab đã nhấp dựa trên vị trí click chuột chứ không phải sử dụnggetSelectedIndex() Bởi vì nó có thể không chính xác sau khi chuyển đổi
                int clickedTabIndex = detailTabs.indexAtLocation(e.getX(), e.getY());
                if (clickedTabIndex >= 0 && clickedTabIndex < detailTabs.getTabCount()) {
                    lastSelectedRequestDetailTabIndex = clickedTabIndex;
                }
            }

        });
    }


    private void registerListeners() {
        // Sự kiện chuột - Tối ưu hóa độ nhạy
        resultsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Xử lý các lựa chọn ngay lập tức khi nhấn chuột, cải thiện khả năng phản hồi
                TreePath path = resultsTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    resultsTree.setSelectionPath(path);
                    resultsTree.scrollPathToVisible(path);
                }
            }
        });

        // Sự kiện bàn phím - Cải thiện điều hướng bàn phím
        resultsTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        // Sự kiện chọn cây - Nội dung chính thể hiện logic
        resultsTree.addTreeSelectionListener(e -> {
            TreePath newPath = e.getNewLeadSelectionPath();
            if (newPath != null) {
                lastSelectedPath = newPath;
                // Trì hoãn hiển thị chi tiết để tránh làm mới thường xuyên
                SwingUtilities.invokeLater(() -> {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                    showNodeDetail(node);
                });
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        TreePath path = resultsTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        // Hãy chắc chắn rằng đường dẫn được chọn
        if (!resultsTree.isPathSelected(path)) {
            resultsTree.setSelectionPath(path);
        }

    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE:
                handleTreeSelection();
                break;
            case KeyEvent.VK_LEFT:
                TreePath selectedPath = resultsTree.getSelectionPath();
                if (selectedPath != null && resultsTree.isExpanded(selectedPath)) {
                    resultsTree.collapsePath(selectedPath);
                }
                break;
            case KeyEvent.VK_RIGHT:
                TreePath selected = resultsTree.getSelectionPath();
                if (selected != null && !resultsTree.isExpanded(selected)) {
                    resultsTree.expandPath(selected);
                }
                break;
            default:
                // Không có tổ hợp phím bổ sung cần phải được xử lý
                break;
        }
    }

    private void handleTreeSelection() {
        TreePath path = resultsTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            showNodeDetail(node);
        }
    }


    /**
     * Cập nhật dữ liệu lịch sử thực hiện
     */
    public void updateExecutionHistory(BatchExecutionHistory history) {
        SwingUtilities.invokeLater(() -> {
            this.executionHistory = history;
            // Đặt lạitabChỉ mục này đứng đầu vì đây là dữ liệu lịch sử thực thi mới
            lastSelectedRequestDetailTabIndex = 0;
            rebuildTree();
        });
    }

    private void rebuildTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        if (executionHistory == null) {
            root.setUserObject(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS_NO_DATA));
            treeModel.nodeStructureChanged(root);
            showWelcomePanel();
            return;
        }

        // Đặt thông tin nút gốc
        long totalTime = executionHistory.getExecutionTime();
        int totalIterations = executionHistory.getIterations().size();
        root.setUserObject(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS_SUMMARY,
                totalIterations, TimeDisplayUtil.formatElapsedTime(totalTime)));

        // Thêm một nút lặp
        for (IterationResult iteration : executionHistory.getIterations()) {
            DefaultMutableTreeNode iterationNode = new DefaultMutableTreeNode(
                    new IterationNodeData(iteration));

            // Thêm nút yêu cầu
            for (RequestResult request : iteration.getRequestResults()) {
                DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(
                        new RequestNodeData(request));
                iterationNode.add(requestNode);
            }

            root.add(iterationNode);
        }

        treeModel.nodeStructureChanged(root);
        expandAll(); // Tất cả các nút được mở rộng theo mặc định

        // Cố gắng khôi phục trạng thái đã chọn trước đó
        restoreSelection();
    }

    /**
     * Chọn nút gốc (tóm tắt kết quả thực hiện) và mở rộng chi tiết
     * Được sử dụng để tự động hiển thị kết quả sau khi thực hiện xong
     */
    public void selectFirstIteration() {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            if (root.getChildCount() > 0) {
                // Chọn nút gốc (hiển thị thông tin tóm tắt như tổng thời gian thực hiện, số lần lặp, v.v.)
                TreePath rootPath = new TreePath(root);

                // Chọn và mở rộng nút gốc
                resultsTree.setSelectionPath(rootPath);
                resultsTree.scrollPathToVisible(rootPath);
                resultsTree.expandPath(rootPath);
            }
        });
    }

    private void restoreSelection() {
        if (lastSelectedPath != null) {
            // Cố gắng tìm đường dẫn nút tương tự và chọn nó
            SwingUtilities.invokeLater(() -> {
                TreePath newPath = findSimilarPath();
                resultsTree.setSelectionPath(newPath);
                resultsTree.scrollPathToVisible(newPath);
            });
        }
    }

    private TreePath findSimilarPath() {
        // Cách thực hiện đơn giản: cố gắng chọn nút con đầu tiên bên dưới nút gốc
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
            return new TreePath(new Object[]{root, firstChild});
        }
        return new TreePath(root);
    }

    private void refreshData() {
        if (executionHistory != null) {
            rebuildTree();
        }
    }

    private void showWelcomePanel() {
        detailTabs.removeAll();
        JPanel welcomePanel = createWelcomePanel();
        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_OVERVIEW),
                IconUtil.createThemed("icons/info.svg", 16, 16), welcomePanel);
        detailTabs.revalidate();
        detailTabs.repaint();
    }

    private void showNodeDetail(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        detailTabs.removeAll();

        if (userObject instanceof IterationNodeData iterationNodeData) {
            showIterationDetail(iterationNodeData);
        } else if (userObject instanceof RequestNodeData requestNodeData) {
            showRequestDetail(requestNodeData);
        } else {
            showOverviewDetail();
        }

        detailTabs.revalidate(); // Xác nhận lại bố cục
        detailTabs.repaint(); // Vẽ lại bảng tab
    }

    private void showOverviewDetail() {
        if (executionHistory == null) {
            detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_OVERVIEW), createWelcomePanel());
            return;
        }

        JPanel overviewPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(overviewPanel);
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tạo số liệu thống kê
        JPanel statsPanel = createStatsPanel();
        overviewPanel.add(statsPanel, BorderLayout.NORTH);

        // Tạo một bảng tóm tắt
        JTable summaryTable = createSummaryTable();
        JScrollPane scrollPane = new JScrollPane(summaryTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, summaryTable);
        overviewPanel.add(scrollPane, BorderLayout.CENTER);

        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_OVERVIEW), overviewPanel);
    }

    private void showIterationDetail(IterationNodeData iterationData) {
        IterationResult iteration = iterationData.iteration;

        JPanel iterationPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(iterationPanel);
        iterationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lặp lại thông tin - Sử dụng thay thế GridBagLayout Kiểm soát chính xác key/value Căn chỉnh
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 12, 8));

        String[][] rows = {
                {I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_ROUND),
                        I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_ROUND_FORMAT, iteration.getIterationIndex() + 1)},
                {I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_START_TIME),
                        formatTimestamp(iteration.getStartTime())},
                {I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_EXECUTION_TIME),
                        TimeDisplayUtil.formatElapsedTime(iteration.getExecutionTime())},
                {I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_REQUEST_COUNT),
                        String.valueOf(iteration.getRequestResults().size())},
        };

        GridBagConstraints kc = new GridBagConstraints();
        kc.anchor = GridBagConstraints.WEST;
        kc.insets = new Insets(3, 0, 3, 16);

        GridBagConstraints vc = new GridBagConstraints();
        vc.anchor = GridBagConstraints.WEST;
        vc.weightx = 1.0;
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.gridwidth = GridBagConstraints.REMAINDER;
        vc.insets = new Insets(3, 0, 3, 0);

        for (String[] row : rows) {
            JLabel keyLabel = new JLabel(row[0] + ":");
            keyLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            keyLabel.setForeground(ModernColors.getTextHint());
            JLabel valLabel = new JLabel(row[1]);
            valLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

            kc.gridx = 0; kc.gridy = GridBagConstraints.RELATIVE;
            infoPanel.add(keyLabel, kc);
            vc.gridx = 1;
            infoPanel.add(valLabel, vc);
        }
        // Đệm đàn hồi ở phía dưới
        GridBagConstraints filler = new GridBagConstraints();
        filler.weighty = 1.0; filler.gridwidth = GridBagConstraints.REMAINDER;
        infoPanel.add(new JLabel(), filler);

        iterationPanel.add(infoPanel, BorderLayout.NORTH);

        // CSV Dữ liệu (nếu có)
        if (iteration.getCsvData() != null && !iteration.getCsvData().isEmpty()) {
            JPanel csvPanel = createCsvDataPanel(iteration.getCsvData());
            iterationPanel.add(csvPanel, BorderLayout.CENTER);
        }

        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_ITERATION), iterationPanel);
    }

    private void showRequestDetail(RequestNodeData requestData) {
        RequestResult request = requestData.request;
        // Yêu cầu thông tin
        JEditorPane reqPane = new JEditorPane();
        reqPane.setContentType(TEXT_HTML);
        reqPane.setEditable(false);
        reqPane.setText(HttpHtmlRenderer.renderRequest(request.getReq()));
        reqPane.setCaretPosition(0);
        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST), createHtmlDetailScrollPane(reqPane));

        // Thông tin phản hồi
        JEditorPane respPane = new JEditorPane();
        respPane.setContentType(TEXT_HTML);
        respPane.setEditable(false);
        respPane.setText(HttpHtmlRenderer.renderResponseWithError(request));
        respPane.setCaretPosition(0);
        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_RESPONSE), createHtmlDetailScrollPane(respPane));

        // Timing & Event Info
        if (request.getResponse() != null && request.getResponse().httpEventInfo != null) {
            JEditorPane timingPane = new JEditorPane();
            timingPane.setContentType(TEXT_HTML);
            timingPane.setEditable(false);
            timingPane.setText(HttpHtmlRenderer.renderTimingInfo(request.getResponse()));
            timingPane.setCaretPosition(0);
            detailTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_TIMING), createHtmlDetailScrollPane(timingPane));

            JEditorPane eventInfoPane = new JEditorPane();
            eventInfoPane.setContentType(TEXT_HTML);
            eventInfoPane.setEditable(false);
            eventInfoPane.setText(HttpHtmlRenderer.renderEventInfo(request.getResponse()));
            eventInfoPane.setCaretPosition(0);
            detailTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_EVENTS), createHtmlDetailScrollPane(eventInfoPane));
        }

        // Tests
        if (request.getTestResults() != null && !request.getTestResults().isEmpty()) {
            JEditorPane testsPane = new JEditorPane();
            testsPane.setContentType(TEXT_HTML);
            testsPane.setEditable(false);
            testsPane.setText(HttpHtmlRenderer.renderTestResults(request.getTestResults()));
            testsPane.setCaretPosition(0);
            detailTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_TESTS), createHtmlDetailScrollPane(testsPane));
        }

        // Khôi phục lựa chọn cuối cùng tab
        if (lastSelectedRequestDetailTabIndex >= detailTabs.getTabCount()) {
            lastSelectedRequestDetailTabIndex = 0;
        }
        detailTabs.setSelectedIndex(lastSelectedRequestDetailTabIndex);
    }

    private void expandAll() {
        for (int i = 0; i < resultsTree.getRowCount(); i++) {
            resultsTree.expandRow(i);
        }
    }

    private void collapseAll() {
        for (int i = resultsTree.getRowCount() - 1; i >= 0; i--) {
            resultsTree.collapseRow(i);
        }
    }

    // Lớp dữ liệu nút cây
    private static class IterationNodeData {
        final IterationResult iteration;

        IterationNodeData(IterationResult iteration) {
            this.iteration = iteration;
        }

        @Override
        public String toString() {
            long passedCount = iteration.getRequestResults().stream()
                    .filter(req -> AssertionResult.PASS.equals(req.getAssertion()))
                    .count();
            return I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_PASSED_FORMAT,
                    iteration.getIterationIndex() + 1,
                    passedCount,
                    iteration.getRequestResults().size(),
                    TimeDisplayUtil.formatElapsedTime(iteration.getExecutionTime()));
        }
    }

    private static class RequestNodeData {
        final RequestResult request;

        RequestNodeData(RequestResult request) {
            this.request = request;
        }

        @Override
        public String toString() {

            return String.format("%s | %s",
                    request.getRequestName(),
                    request.getStatus());
        }
    }

    // Trình kết xuất cây tùy chỉnh
    private static class ExecutionResultTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof IterationNodeData iterationData) {
                renderIterationNode(iterationData, sel);
            } else if (userObject instanceof RequestNodeData requestData) {
                renderRequestNode(requestData, sel);
            } else {
                // nút gốc
                setIcon(IconUtil.createThemed("icons/history.svg", 16, 16));
            }

            return this;
        }

        private void renderIterationNode(IterationNodeData iterationData, boolean selected) {
            IterationResult iteration = iterationData.iteration;
            boolean hasFailures = iteration.getRequestResults().stream()
                    .anyMatch(req -> AssertionResult.FAIL.equals(req.getAssertion()));
            boolean hasTests = iteration.getRequestResults().stream()
                    .anyMatch(req -> !AssertionResult.NO_TESTS.equals(req.getAssertion()));

            if (hasFailures) {
                // Có lỗi: biểu tượng và văn bản màu đỏ
                setIcon(new FlatSVGIcon("icons/fail.svg", 16, 16));
                if (!selected) {
                    setForeground(ModernColors.getErrorDark());
                }
            } else if (hasTests) {
                // Tất cả đều đạt: biểu tượng và văn bản màu xanh lá cây
                setIcon(new FlatSVGIcon("icons/pass.svg", 16, 16));
                if (!selected) {
                    setForeground(ModernColors.getSuccessDark());
                }
            } else {
                // Không kiểm tra: biểu tượng mặc định
                setIcon(IconUtil.createThemed("icons/functional.svg", 16, 16));
                if (!selected) {
                    setForeground(ModernColors.getTextSecondary());
                }
            }
        }

        private void renderRequestNode(RequestNodeData requestData, boolean selected) {
            AssertionResult assertion = requestData.request.getAssertion();
            String status = requestData.request.getStatus();

            if (assertion == null) {
                // Không được thực thi: biểu tượng và văn bản màu xám
                setIcon(new FlatSVGIcon(ExecutionResultsPanel.ICON_HTTP, 16, 16));
                setForegroundIfNotSelected(ModernColors.getTextDisabled(), selected);
            } else if (AssertionResult.FAIL.equals(assertion) || FunctionalPanel.ERROR.equals(status)) {
                // Lỗi: biểu tượng và văn bản màu đỏ
                setIcon(new FlatSVGIcon("icons/fail.svg", 16, 16));
                setForegroundIfNotSelected(ModernColors.getErrorDark(), selected);
            } else if (AssertionResult.NO_TESTS.equals(assertion)) {
                // Không kiểm tra: biểu tượng và văn bản màu xanh
                setIcon(IconUtil.createThemed("icons/nocheck.svg", 16, 16));
                setForegroundIfNotSelected(ModernColors.getTextSecondary(), selected);
            } else if (AssertionResult.PASS.equals(assertion)) {
                // Qua: biểu tượng và văn bản màu xanh lá cây
                setIcon(new FlatSVGIcon("icons/pass.svg", 16, 16));
                setForegroundIfNotSelected(ModernColors.getSuccessDark(), selected);

            } else {
                // Các trạng thái khác: Biểu tượng mặc định
                setIcon(new FlatSVGIcon(ExecutionResultsPanel.ICON_HTTP, 16, 16));
                setForegroundIfNotSelected(ModernColors.getTextSecondary(), selected);
            }
        }

        private void setForegroundIfNotSelected(Color color, boolean selected) {
            if (!selected) {
                setForeground(color);
            }
        }
    }

    private JPanel createWelcomePanel() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyCard(welcomePanel);
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        // Biểu tượng lớn
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/functional.svg", 48, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(iconLabel);
        inner.add(Box.createVerticalStrut(16));

        // Văn bản nhắc nhở chính - Thực hiện theo các màu sắc chủ đề
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +2));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(titleLabel);
        inner.add(Box.createVerticalStrut(8));

        // Văn bản nhắc nhở phụ
        JLabel subtitleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_SUBTITLE));
        subtitleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        subtitleLabel.setForeground(ModernColors.getTextHint());
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(subtitleLabel);

        welcomePanel.add(inner);
        return welcomePanel;
    }

    private JPanel createStatsPanel() {
        FunctionalRunStats stats = FunctionalRunStats.of(executionHistory,
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_SKIPPED));
        int totalIterations = stats.iterations();
        int totalRequests = stats.requests();
        long totalTime = stats.totalTimeMs();
        double successRate = stats.successRate();
        long avgTime = stats.averageTimeMs();

        // sử dụng GridBagLayout hoàn thành4Thẻ cột, chiều rộng thích ứng
        JPanel statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 14, 8));

        // Xác định dữ liệu thẻ:[giá trị số, Nhãn, màu sắc(null=Màu chính)]
        Object[][] cards = {
                {String.valueOf(totalIterations), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_ITERATIONS), null},
                {String.valueOf(totalRequests), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_REQUESTS), null},
                {String.format("%.1f%%", successRate), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_SUCCESS_RATE),
                        successRate >= 100 ? ModernColors.getSuccessDark() : (successRate >= 80 ? ModernColors.getWarningDarker() : ModernColors.getErrorDark())},
                {TimeDisplayUtil.formatElapsedTime(totalTime), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_TIME), null},
                {formatTimestamp(executionHistory.getStartTime()), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_START_TIME), null},
                {formatTimestamp(executionHistory.getEndTime()), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_END_TIME), null},
                {avgTime + " ms", I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_AVERAGE_TIME), null},
                {I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS_COMPLETED), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS),
                        ModernColors.getSuccessDark()},
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 4, 6);

        for (int i = 0; i < cards.length; i++) {
            gbc.gridx = i % 4;
            gbc.gridy = i / 4;
            statsPanel.add(createStatCard((String) cards[i][0], (String) cards[i][1], (Color) cards[i][2]), gbc);
        }

        return statsPanel;
    }

    /**
     * Tạo một thẻ thống kê duy nhất: các giá trị lớn và đậm ở trên cùng, còn các nhãn nhỏ và màu xám ở dưới cùng
     */
    private JPanel createStatCard(String value, String label, Color valueColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +2));
        valueLabel.setForeground(valueColor != null ? valueColor : ModernColors.getTextPrimary());
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        nameLabel.setForeground(ModernColors.getTextHint());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(nameLabel);
        return card;
    }

    private JTable createSummaryTable() {
        String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ITERATION),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_REQUEST_NAME),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_METHOD),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_STATUS),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_TIME),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_RESULT)
        };

        java.util.List<Object[]> tableData = new java.util.ArrayList<>();
        for (IterationResult iteration : executionHistory.getIterations()) {
            for (RequestResult request : iteration.getRequestResults()) {
                Object[] row = {
                        "#" + (iteration.getIterationIndex() + 1),
                        request.getRequestName(),
                        request.getMethod(),
                        request.getStatus(),
                        request.getCost(), // Lưu giá trị ban đầu và trình kết xuất sẽ định dạng nó
                        request.getAssertion()
                };
                tableData.add(row);
            }
        }

        Object[][] data = tableData.toArray(new Object[0][]);
        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tất cả các ô đều không thể chỉnh sửa được
            }
        };

        // ứng dụng ModernColors Cách phối màu
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));
        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Đặt chiều rộng cột
        setTableColumnWidths(table);

        // Thiết lập trình kết xuất
        setTableRenderers(table);

        return table;
    }

    private void setTableColumnWidths(JTable table) {
        if (table.getColumnModel().getColumnCount() > 0) {
            // Iteration cột - Cần phải được hiển thị "Iteration"（9ký tự)+ "#1"
            table.getColumnModel().getColumn(0).setMinWidth(75);
            table.getColumnModel().getColumn(0).setMaxWidth(110);
            table.getColumnModel().getColumn(0).setPreferredWidth(80);

            // Request Name cột - Có thể rộng hơn để hiển thị tên yêu cầu đầy đủ
            table.getColumnModel().getColumn(1).setMinWidth(150);
            table.getColumnModel().getColumn(1).setPreferredWidth(200);

            // Method cột - Cần phải được hiển thị "Method"（6ký tự)+ "DELETE"（7ký tự)
            table.getColumnModel().getColumn(2).setMinWidth(75);
            table.getColumnModel().getColumn(2).setMaxWidth(135);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);

            // Status cột - Cần phải được hiển thị "Status"（6ký tự)+ Mã trạng thái (3số chữ số)
            table.getColumnModel().getColumn(3).setMinWidth(70);
            table.getColumnModel().getColumn(3).setMaxWidth(115);
            table.getColumnModel().getColumn(3).setPreferredWidth(100);

            // Time cột - Cần phải được hiển thị "Time"（4ký tự)+ thời gian (ví dụ: "123 ms"）
            table.getColumnModel().getColumn(4).setMinWidth(75);
            table.getColumnModel().getColumn(4).setMaxWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(100);

            // Result cột - Cần phải được hiển thị "Result"（6ký tự)+ emoji/trạng thái
            table.getColumnModel().getColumn(5).setMinWidth(65);
            table.getColumnModel().getColumn(5).setMaxWidth(100);
            table.getColumnModel().getColumn(5).setPreferredWidth(85);
        }
    }

    private void setTableRenderers(JTable table) {
        // Trình kết xuất cột phương thức
        table.getColumnModel().getColumn(2).setCellRenderer(createMethodRenderer());
        // Trình kết xuất cột trạng thái
        table.getColumnModel().getColumn(3).setCellRenderer(createStatusRenderer());
        // Trình kết xuất cột tốn thời gian
        table.getColumnModel().getColumn(4).setCellRenderer(createTimeRenderer());
        // Trình kết xuất cột kết quả
        table.getColumnModel().getColumn(5).setCellRenderer(createResultRenderer());
    }

    private DefaultTableCellRenderer createMethodRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    c.setForeground(HttpRequestDisplayMetadata.methodColor(value.toString()));
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && !"-".equals(value)) {
                    applyStatusColors(c, value.toString());
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    private void applyStatusColors(java.awt.Component c, String status) {
        java.awt.Color foreground = ModernColors.getTextPrimary();

        // Kiểm tra xem có"Bỏ qua"trạng thái
        String skippedText = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_SKIPPED);
        if (skippedText.equals(status)) {
            foreground = ModernColors.getTextHint();
        } else {
            try {
                int code = Integer.parseInt(status);
                if (code >= 200 && code < 300) {
                    foreground = ModernColors.getSuccessDark();
                } else if (code >= 400 && code < 500) {
                    foreground = ModernColors.getWarningDarker();
                } else if (code >= 500) {
                    foreground = ModernColors.getErrorDarker();
                }
            } catch (NumberFormatException e) {
                foreground = ModernColors.getErrorDark();
            }
        }
        c.setForeground(foreground);
    }

    private DefaultTableCellRenderer createTimeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Long cost) {
                    value = TimeDisplayUtil.formatElapsedTime(cost);
                }
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createResultRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);

                String skippedText = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_SKIPPED);
                // Lấy Status Giá trị cột xác định có nên bỏ qua hay không
                String status = "";
                try {
                    Object sv = table.getValueAt(row, 3);
                    if (sv != null) status = sv.toString();
                } catch (Exception ignored) { /* ignore */ }

                if (skippedText.equals(status)) {
                    setText("-");
                    c.setForeground(ModernColors.getTextHint());
                } else if (value instanceof AssertionResult ar) {
                    setText(ar.getDisplayValue());
                    if (AssertionResult.PASS.equals(ar)) {
                        c.setForeground(isSelected ? c.getForeground() : ModernColors.getSuccessDark());
                    } else if (AssertionResult.FAIL.equals(ar)) {
                        c.setForeground(isSelected ? c.getForeground() : ModernColors.getErrorDark());
                    } else {
                        c.setForeground(isSelected ? c.getForeground() : ModernColors.getTextSecondary());
                    }
                } else {
                    setText("-");
                    c.setForeground(ModernColors.getTextDisabled());
                }
                return c;
            }
        };
    }

    private JPanel createCsvDataPanel(java.util.Map<String, String> csvData) {
        JPanel csvPanel = new JPanel(new BorderLayout());
        csvPanel.setOpaque(false);
        csvPanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 0, 8));
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_CSV_DATA));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        csvPanel.add(titleLabel, BorderLayout.NORTH);

        JTable csvTable = new JTable();
        String[] headers = csvData.keySet().toArray(new String[0]);
        Object[][] data = new Object[1][headers.length];
        for (int i = 0; i < headers.length; i++) {
            data[0][i] = csvData.get(headers[i]);
        }

        csvTable.setModel(new DefaultTableModel(data, headers));
        csvTable.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // Nhỏ hơn kích thước phông chữ tiêu chuẩn1Không
        csvTable.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1)); // Nhỏ hơn kích thước phông chữ tiêu chuẩn1số (in đậm)
        csvTable.setRowHeight(20);

        JScrollPane csvScrollPane = new JScrollPane(csvTable);
        csvScrollPane.setPreferredSize(new Dimension(0, 100));
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(csvScrollPane, csvTable);
        csvPanel.add(csvScrollPane, BorderLayout.CENTER);

        return csvPanel;
    }

    private JScrollPane createHtmlDetailScrollPane(JEditorPane pane) {
        ToolWindowSurfaceStyle.applyTextComponentCard(pane);
        JScrollPane scrollPane = new JScrollPane(pane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);
        return scrollPane;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

}
