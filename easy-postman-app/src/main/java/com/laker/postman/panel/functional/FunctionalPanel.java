package com.laker.postman.panel.functional;

import com.laker.postman.common.component.notification.NotificationCenter;

import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.functional.model.BatchExecutionHistory;
import com.laker.postman.functional.execution.FunctionalRequestExecutionResult;
import com.laker.postman.functional.execution.FunctionalRequestExecutor;
import com.laker.postman.functional.model.IterationResult;
import com.laker.postman.functional.model.RequestResult;
import com.laker.postman.functional.model.RunnerRowData;
import com.laker.postman.request.model.HttpRequestItem;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.DebouncedSaveSupport;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.functional.model.FunctionalConfigRow;
import com.laker.postman.functional.model.FunctionalConfigSnapshot;
import com.laker.postman.functional.model.FunctionalCsvDataState;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.panel.collections.RequestSelectionDialogSupport;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.functional.table.FunctionalRunnerTableModel;
import com.laker.postman.panel.functional.table.TableRowTransferHandler;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.FunctionalPersistenceService;
import com.laker.postman.common.component.HttpRequestDisplayMetadata;
import com.laker.postman.service.collections.CollectionRequestLookup;
import com.laker.postman.service.collections.RequestSaveEventPublisher;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class FunctionalPanel extends UiSingletonPanel {
    public static final String ERROR = "Error";
    private JTable table;
    private FunctionalRunnerTableModel tableModel;
    private StartButton runBtn;
    private StopButton stopBtn;    // Nút dừng
    private JLabel timeLabel;     // Thẻ thời gian thực hiện
    private JLabel progressLabel; // Nhãn tiến độ
    private long startTime;       // Ghi lại thời gian bắt đầu
    private Timer executionTimer; // Hẹn giờ thực hiện
    private volatile boolean isStopped = false; // Dấu hiệu dừng lại
    private volatile int executionGeneration = 0;

    // CSV Bảng quản lý dữ liệu
    private CsvDataPanel csvDataPanel;

    // Lịch sử thực hiện hàng loạt
    private transient BatchExecutionHistory executionHistory;
    private JTabbedPane mainTabbedPane;
    private ExecutionResultsPanel resultsPanel;

    // Dịch vụ kiên trì
    private transient FunctionalPersistenceService persistenceService;
    private final transient CollectionRequestLookup requestLookup = new CollectionRequestLookup();
    private final transient DebouncedSaveSupport autoSaveSupport = new DebouncedSaveSupport(500, this::saveAsync);
    private final transient FunctionalRequestExecutor requestExecutor =
            new FunctionalRequestExecutor(error -> com.laker.postman.panel.sidebar.ConsolePanel.appendLog(
                    "[Request Error]\n" + error,
                    com.laker.postman.panel.sidebar.ConsolePanel.LogType.ERROR
            ));


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);
        // Loại bỏ mã hóa cứng setPreferredSize，Thay vào đó, hãy sử dụng các ràng buộc về kích thước tối thiểu và để vùng chứa chính phân bổ một cách thích ứng
        setMinimumSize(new Dimension(500, 300));

        // Tạo bảng tab chính
        mainTabbedPane = new JTabbedPane();
        ToolWindowSurfaceStyle.applyTabbedPaneCard(mainTabbedPane);
        mainTabbedPane.setBorder(BorderFactory.createEmptyBorder());
        mainTabbedPane.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));

        JPanel executionPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(executionPanel);
        executionPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        executionPanel.add(createTopPanel(), BorderLayout.NORTH);
        executionPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_REQUEST_CONFIG),
                new FlatSVGIcon("icons/functional.svg", 16, 16)
                        .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())),
                executionPanel);

        resultsPanel = new ExecutionResultsPanel();
        mainTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_EXECUTION_RESULTS),
                new FlatSVGIcon("icons/history.svg", 16, 16)
                        .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())),
                resultsPanel);

        add(AppToolWindowChrome.wrapInsetToolWindow(mainTabbedPane), BorderLayout.CENTER);

        this.persistenceService = BeanFactory.getBean(FunctionalPersistenceService.class);
        loadSaved();
    }

    private JPanel createTopPanel() {
        // Sử dụng thay thế FlowLayout Bố trí linh hoạt để tránh chiều cao cố định
        JPanel topPanel = new JPanel(new BorderLayout(6, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        topPanel.setOpaque(false);

        // khởi tạo CSV Bảng dữ liệu
        csvDataPanel = new CsvDataPanel();
        csvDataPanel.setChangeListener(this::scheduleSave);

        // Nhóm nút trái
        topPanel.add(createButtonPanel(), BorderLayout.WEST);

        // giữa CSV Bảng trạng thái (thu gọn tự nhiên khi không có dữ liệu)
        topPanel.add(csvDataPanel, BorderLayout.CENTER);

        // Đúng: thời gian thực hiện + Tiến độ (sắp xếp nhỏ gọn)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        // Thời gian thực hiện
        timeLabel = new JLabel("0 ms");
        timeLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        JLabel timeIcon = new JLabel(new FlatSVGIcon("icons/time.svg", 16, 16)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())));
        rightPanel.add(timeIcon);
        rightPanel.add(timeLabel);

        // dải phân cách
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 16));
        rightPanel.add(sep);

        // tiến bộ
        progressLabel = new JLabel("0/0");
        progressLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        JLabel taskIcon = new JLabel(new FlatSVGIcon("icons/functional.svg", 16, 16)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())));
        rightPanel.add(taskIcon);
        rightPanel.add(progressLabel);

        topPanel.add(rightPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createButtonPanel() {
        JButton loadBtn = new LoadButton();
        loadBtn.addActionListener(e -> showLoadRequestsDialog());

        runBtn = new StartButton();
        runBtn.addActionListener(e -> {
            runSelectedRequests();
        });

        stopBtn = new StopButton();
        stopBtn.setEnabled(false); // Bị vô hiệu hóa ban đầu và được kích hoạt khi thực thi
        stopBtn.addActionListener(e -> {
            isStopped = true;
            stopBtn.setEnabled(false);
        });

        JButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> refreshRequestsFromCollections());

        JButton clearBtn = new ClearButton();
        clearBtn.addActionListener(e -> {
            tableModel.clear();
            runBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            resetProgress();
            resultsPanel.updateExecutionHistory(null);
            autoSaveSupport.cancel();
            if (csvDataPanel != null) {
                csvDataPanel.restoreState(null);
            }
            persistenceService.clear();
        });

        return ToolWindowActionToolbar.inlineLeft(loadBtn, runBtn, stopBtn, refreshBtn, clearBtn);
    }

    // Chạy theo đợt
    private void runSelectedRequests() {
        isStopped = false;
        int rowCount = tableModel.getRowCount();
        int selectedCount = (int) IntStream.range(0, rowCount).mapToObj(i -> tableModel.getRow(i)).filter(row -> row != null && row.selected).count();
        if (selectedCount == 0) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_NO_RUNNABLE_REQUEST));
            return;
        }

        // Kiểm tra nếu sử dụng CSV dữ liệu
        int iterations = 1;
        if (csvDataPanel.hasData()) {
            iterations = csvDataPanel.getRowCount();
            int response = JOptionPane.showConfirmDialog(this,
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_CSV_DETECTED, iterations),
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_CSV_TITLE),
                    JOptionPane.YES_NO_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                iterations = 1;
            }
        }

        final int totalExecutions = selectedCount * iterations;
        BatchExecutionHistory currentHistory = new BatchExecutionHistory();
        currentHistory.setTotalIterations(iterations);
        currentHistory.setTotalRequests(totalExecutions);
        executionHistory = currentHistory;
        int generation = ++executionGeneration;

        clearRunResults(rowCount);
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true); // Kích hoạt khi bắt đầu thực hiện Stop

        progressLabel.setText("0/" + totalExecutions);


        startTime = System.currentTimeMillis();
        executionTimer = new Timer(100, e -> updateExecutionTime());
        executionTimer.start();

        final int finalIterations = iterations;
        new Thread(() -> executeBatchRequestsWithCsv(rowCount, selectedCount, finalIterations, generation, currentHistory)).start();
    }

    private void clearRunResults(int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            RunnerRowData row = tableModel.getRow(i);
            if (row != null) {
                row.response = null;
                row.cost = 0;
                row.status = null;
                row.assertion = null;
                row.testResults = null;
                tableModel.fireTableRowsUpdated(i, i);
            }
        }
    }

    private void executeBatchRequestsWithCsv(int rowCount, int selectedCount, int iterations,
                                             int generation, BatchExecutionHistory currentHistory) {
        int totalFinished = 0;

        for (int iteration = 0; iteration < iterations && isExecutionActive(generation); iteration++) {
            // Lấy lần lặp hiện tại CSV dữ liệu
            Map<String, String> currentCsvRow = getCsvDataForIteration(iteration);
            Map<String, String> currentIterationData = IterationDataRuntimeSupport.prepare(currentCsvRow);
            ExecutionVariableContext iterationContext = new ExecutionVariableContext();
            iterationContext.setIterationInfo(iteration, iterations);
            iterationContext.replaceIterationData(currentIterationData);

            // Tạo một bản ghi về kết quả của lần lặp hiện tại
            IterationResult iterationResult = new IterationResult(iteration, currentIterationData);

            totalFinished = processIterationRequests(rowCount, selectedCount, iterations, totalFinished,
                    iterationResult, currentIterationData, iterationContext, generation);

            // Hoàn thành lần lặp hiện tại và thêm vào lịch sử (lưu kết quả của lần lặp hiện tại dù có dừng hay không)
            iterationResult.complete();
            currentHistory.addIteration(iterationResult);

            // Cập nhật theo thời gian thực cho bảng kết quả
            SwingUtilities.invokeLater(() -> {
                if (isExecutionGenerationCurrent(generation)) {
                    resultsPanel.updateExecutionHistory(currentHistory);
                }
            });

            if (isStopped) break;
        }

        currentHistory.complete();
        finalizeExecution(generation, currentHistory);
    }

    private Map<String, String> getCsvDataForIteration(int iteration) {
        if (csvDataPanel.hasData() && iteration < csvDataPanel.getRowCount()) {
            return csvDataPanel.getRowData(iteration);
        }
        return Collections.emptyMap();
    }

    private int processIterationRequests(int rowCount, int selectedCount, int iterations,
                                         int totalFinished, IterationResult iterationResult,
                                         Map<String, String> currentCsvRow,
                                         ExecutionVariableContext iterationContext,
                                         int generation) {
        int finished = totalFinished;

        for (int i = 0; i < rowCount && isExecutionActive(generation); i++) {
            RunnerRowData row = tableModel.getRow(i);

            if (!isValidRow(row)) {
                continue;
            }

            if (row.selected) {
                finished = executeAndRecordRequest(row, currentCsvRow, iterationResult, finished,
                        selectedCount, iterations, iterationContext, generation);
            }
        }

        return finished;
    }

    private boolean isValidRow(RunnerRowData row) {
        if (row == null || row.requestItem == null) {
            log.warn("Row is invalid, skipping execution");
            return false;
        }
        return true;
    }

    private boolean isExecutionActive(int generation) {
        return isExecutionGenerationCurrent(generation) && !isStopped;
    }

    private boolean isExecutionGenerationCurrent(int generation) {
        return generation == executionGeneration;
    }

    private int executeAndRecordRequest(RunnerRowData row, Map<String, String> currentCsvRow,
                                        IterationResult iterationResult, int totalFinished,
                                        int selectedCount, int iterations,
                                        ExecutionVariableContext iterationContext,
                                        int generation) {
        if (!isExecutionGenerationCurrent(generation)) {
            return totalFinished;
        }

        // Tìm chỉ mục của hàng hiện tại
        int rowIndex = -1;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRow(i) == row) {
                rowIndex = i;
                break;
            }
        }

        // Đánh dấu dòng hiện đang thực hiện
        final int currentRowIndex = rowIndex;
        if (currentRowIndex >= 0) {
            SwingUtilities.invokeLater(() -> {
                if (isExecutionGenerationCurrent(generation) && currentRowIndex < tableModel.getRowCount()) {
                    table.setRowSelectionInterval(currentRowIndex, currentRowIndex);
                    table.scrollRectToVisible(table.getCellRect(currentRowIndex, 0, true));
                }
            });
        }

        FunctionalRequestExecutionResult result = requestExecutor.execute(
                row,
                iterationContext,
                () -> isExecutionActive(generation)
        );
        if (!isExecutionGenerationCurrent(generation)) {
            return totalFinished;
        }

        // Cập nhật kết quả thực hiện vào bảng
        if (currentRowIndex >= 0) {
            row.status = result.getStatus();
            row.cost = result.getCost();
            row.assertion = result.getAssertion();
            row.response = result.getResponse();
            row.testResults = result.getTestResults();

            SwingUtilities.invokeLater(() -> {
                if (isExecutionGenerationCurrent(generation) && currentRowIndex < tableModel.getRowCount()) {
                    tableModel.fireTableRowsUpdated(currentRowIndex, currentRowIndex);
                }
            });
        }

        // Ghi lại kết quả yêu cầu vào lịch sử thực hiện
        RequestResult requestResult = new RequestResult(
                row.requestItem.getName(),
                row.requestItem.getMethod(),
                result.getRequest().url,
                result.getRequest(),
                result.getResponse(),
                result.getCost(),
                result.getStatus(),
                result.getAssertion(),
                result.getTestResults(),
                result.getErrorMessage()
        );
        iterationResult.addRequestResult(requestResult);

        int newTotalFinished = totalFinished + 1;
        SwingUtilities.invokeLater(() -> {
            if (isExecutionGenerationCurrent(generation)) {
                progressLabel.setText(newTotalFinished + "/" + (selectedCount * iterations));
            }
        });

        return newTotalFinished;
    }

    private void finalizeExecution(int generation, BatchExecutionHistory currentHistory) {
        SwingUtilities.invokeLater(() -> {
            if (!isExecutionGenerationCurrent(generation)) {
                return;
            }
            runBtn.setEnabled(true);
            stopBtn.setEnabled(false);

            // Dừng đồng hồ bấm giờ
            stopExecutionTimer();

            // Cuối cùng cập nhật bảng kết quả
            resultsPanel.updateExecutionHistory(currentHistory);

            // Cho dù người dùng hoàn thành bình thường hay dừng lại, hãy chuyển sang bảng kết quả để hiển thị kết quả thực hiện
            mainTabbedPane.setSelectedIndex(1); // Chuyển sang bảng kết quả thực hiện

            // Tự động chọn nút lặp đầu tiên và mở rộng chi tiết
            SwingUtilities.invokeLater(() -> resultsPanel.selectFirstIteration());
        });
    }

    private void stopExecutionTimer() {
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }
    }

    // Cập nhật hiển thị thời gian thực hiện
    private void updateExecutionTime() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        timeLabel.setText(TimeDisplayUtil.formatElapsedTime(elapsedTime));
    }

    // Đặt lại tiến trình và hiển thị thời gian
    private void resetProgress() {
        // Nếu bộ hẹn giờ đang chạy, hãy dừng nó lại
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }

        // Đặt lại văn bản nhãn
        timeLabel.setText("0 ms");
        progressLabel.setText("0/0");
    }

    private JScrollPane createTablePanel() {
        tableModel = new FunctionalRunnerTableModel();
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Chỉ được phép chỉnh sửa cột đầu tiên (cột đã chọn)
            }
        };
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // Sử dụng cỡ chữ do người dùng đặt
        table.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1)); // Lớn hơn kích thước phông chữ tiêu chuẩn1số (in đậm)

        // Thêm trình nghe nhấp chuột tiêu đề và nhấp chuột"Chọn"Chọn tất cả các tiêu đề danh sách/Cuộc bầu cử phản đối
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == 0) { // Bấm để chọn một cột
                    boolean hasSelected = tableModel.hasSelectedRows();
                    tableModel.setAllSelected(!hasSelected);
                    scheduleSave();
                }
            }
        });
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                scheduleSave();
            }
        });

        setTableColumnWidths();
        setTableRenderers();

        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        TableRowTransferHandler transferHandler = new TableRowTransferHandler(table);
        transferHandler.setOnRowOrderChanged(this::scheduleSave); // Tự động duy trì sau khi kéo xong
        table.setTransferHandler(transferHandler);

        JScrollPane scrollPane = new JScrollPane(table);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, table);
        return scrollPane;
    }

    private void setTableColumnWidths() {
        if (table.getColumnModel().getColumnCount() > 0) {
            // Select column (✓) - Chiều rộng cố định, không cần quá rộng
            table.getColumnModel().getColumn(0).setMinWidth(45);
            table.getColumnModel().getColumn(0).setMaxWidth(55);
            table.getColumnModel().getColumn(0).setPreferredWidth(50);

            // Name column - Tương đối linh hoạt và có thể rộng hơn
            table.getColumnModel().getColumn(1).setMinWidth(120);
            table.getColumnModel().getColumn(1).setPreferredWidth(180);

            // URL column - Cột rộng nhất, có thể chiếm nhiều không gian hơn
            table.getColumnModel().getColumn(2).setMinWidth(150);
            table.getColumnModel().getColumn(2).setPreferredWidth(250);

            // Method column - Cần đủ chiều rộng để hiển thị "Method"（6ký tự)+ nội dung(DELETEdài nhất7ký tự)
            table.getColumnModel().getColumn(3).setMinWidth(75);
            table.getColumnModel().getColumn(3).setMaxWidth(90);
            table.getColumnModel().getColumn(3).setPreferredWidth(80);

            // Status column - Cần đủ chiều rộng để hiển thị "Status"（6ký tự)+ Mã trạng thái (3số chữ số)
            table.getColumnModel().getColumn(4).setMinWidth(70);
            table.getColumnModel().getColumn(4).setMaxWidth(85);
            table.getColumnModel().getColumn(4).setPreferredWidth(75);

            // Time column - Cần đủ chiều rộng để hiển thị "Time"（4ký tự)+ thời gian (ví dụ: "123 ms"）
            table.getColumnModel().getColumn(5).setMinWidth(75);
            table.getColumnModel().getColumn(5).setMaxWidth(100);
            table.getColumnModel().getColumn(5).setPreferredWidth(85);

            // Result column - Cần đủ chiều rộng để hiển thị "Result"（6ký tự)+ emoji
            table.getColumnModel().getColumn(6).setMinWidth(65);
            table.getColumnModel().getColumn(6).setMaxWidth(80);
            table.getColumnModel().getColumn(6).setPreferredWidth(70);
        }
    }

    private void setTableRenderers() {
        table.getColumnModel().getColumn(3).setCellRenderer(createMethodRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(createStatusRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(createResultRenderer());
    }

    private DefaultTableCellRenderer createMethodRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    c.setForeground(HttpRequestDisplayMetadata.methodColor(value.toString()));
                }
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null && !"-".equals(value)) {
                    applyStatusColors(c, value.toString());
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    /**
     * Áp dụng màu dựa trên mã trạng thái - Chỉ đặt màu văn bản
     */
    private void applyStatusColors(Component c, String status) {
        Color foreground = ModernColors.getTextPrimary();

        if (ERROR.equalsIgnoreCase(status)) {
            // Tình trạng lỗi: Sử dụng sai màu
            foreground = ModernColors.getErrorDark();
        } else {
            // Hãy thử phân tích mã trạng thái
            try {
                int code = Integer.parseInt(status);
                if (code >= 200 && code < 300) {
                    // Thành công: Sử dụng màu xanh lá cây
                    foreground = ModernColors.getSuccessDark();
                } else if (code >= 400 && code < 500) {
                    // Lỗi máy khách: sử dụng màu cảnh báo
                    foreground = ModernColors.getWarningDarker();
                } else if (code >= 500) {
                    // Lỗi máy chủ: Sử dụng sai màu
                    foreground = ModernColors.getErrorDarker();
                }
            } catch (NumberFormatException e) {
                // Trạng thái không phải là số (chẳng hạn như thông báo lỗi)
                foreground = ModernColors.getErrorDark();
            }

        }
        // Chỉ đặt màu văn bản
        c.setForeground(foreground);
    }

    private DefaultTableCellRenderer createResultRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (value instanceof AssertionResult ar) {
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

    @Override
    protected void registerListeners() {
        RequestSaveEventPublisher.register(this::syncRequestItem);
        // Thêm trình nghe chuột bảng
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;

                if (SwingUtilities.isRightMouseButton(e)) { // Nhấp chuột phải - Chỉ có menu được hiển thị
                    table.setRowSelectionInterval(row, row);
                    showTableContextMenu(e, row);
                } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { // Nhấp đúp chuột trái
                    showRequestDetail(row);
                }
            }
        });
    }

    /**
     * Hiển thị chi tiết yêu cầu
     */
    private void showRequestDetail(int rowIndex) {
        RunnerRowData row = tableModel.getRow(rowIndex);
        if (row != null && row.requestItem != null) {
            // Mở bảng chỉnh sửa yêu cầu
            RequestEditorPanel editPanel =
                    UiSingletonFactory.getInstance(RequestEditorPanel.class);
            editPanel.showOrCreateTab(row.requestItem);

            // Chuyển sangCollectionsNhãn
            SidebarTabPanel sidebarPanel =
                    UiSingletonFactory.getInstance(SidebarTabPanel.class);
            sidebarPanel.getTabbedPane().setSelectedIndex(0);
        }
    }

    /**
     * Hiển thị bảng menu chuột phải
     */
    private void showTableContextMenu(java.awt.event.MouseEvent e, int rowIndex) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        RunnerRowData row = tableModel.getRow(rowIndex);

        // Xem chi tiết
        JMenuItem viewItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_VIEW_DETAIL));
        viewItem.addActionListener(evt -> showRequestDetail(rowIndex));
        menu.add(viewItem);

        menu.addSeparator();

        // Kiểm tra / Bỏ chọn
        if (row != null) {
            String toggleText = row.selected
                    ? I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_UNCHECK)
                    : I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_CHECK);
            JMenuItem toggleItem = new JMenuItem(toggleText);
            toggleItem.addActionListener(evt -> {
                row.selected = !row.selected;
                tableModel.fireTableRowsUpdated(rowIndex, rowIndex);
                scheduleSave();
            });
            menu.add(toggleItem);
        }

        menu.addSeparator();

        // Di chuyển lên
        JMenuItem upItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_MOVE_UP));
        upItem.setEnabled(rowIndex > 0);
        upItem.addActionListener(evt -> {
            tableModel.moveRow(rowIndex, rowIndex - 1);
            table.setRowSelectionInterval(rowIndex - 1, rowIndex - 1);
            scheduleSave();
        });
        menu.add(upItem);

        // Di chuyển xuống
        JMenuItem downItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_MOVE_DOWN));
        downItem.setEnabled(rowIndex < tableModel.getRowCount() - 1);
        downItem.addActionListener(evt -> {
            tableModel.moveRow(rowIndex, rowIndex + 1);
            table.setRowSelectionInterval(rowIndex + 1, rowIndex + 1);
            scheduleSave();
        });
        menu.add(downItem);

        menu.addSeparator();

        // Xóa hàng hiện tại
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_REMOVE));
        deleteItem.addActionListener(evt -> {
            tableModel.removeRow(rowIndex);
            if (tableModel.getRowCount() == 0) {
                runBtn.setEnabled(false);
            }
            scheduleSave();
        });
        menu.add(deleteItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Một yêu cầu lựa chọn bật lên/Hộp thoại nhóm
    private void showLoadRequestsDialog() {
        RequestSelectionDialogSupport.showMultiSelectRequestDialog(
                selected -> {
                    if (selected == null || selected.isEmpty()) return;
                    // Lọc để chỉ giữ lạiHTTPloại yêu cầu
                    List<HttpRequestItem> httpOnlyList = selected.stream()
                            .filter(reqItem -> reqItem.getProtocol() != null && reqItem.getProtocol().isHttpProtocol())
                            .toList();
                    if (httpOnlyList.isEmpty()) {
                        NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.MSG_ONLY_HTTP_SUPPORTED));
                        return;
                    }
                    loadRequests(httpOnlyList);
                }
        );
    }

    // Tải các yêu cầu đã chọn vào bảng
    public void loadRequests(List<HttpRequestItem> requests) {
        // Xây dựng đã được yêu cầu ID Bộ sưu tập, được sử dụng để chống trùng lặp
        Set<String> existingIds = tableModel.getAllRows().stream()
                .filter(r -> r != null && r.requestItem != null && r.requestItem.getId() != null)
                .map(r -> r.requestItem.getId())
                .collect(java.util.stream.Collectors.toSet());

        int skippedCount = 0;
        for (HttpRequestItem item : requests) {
            if (item.getId() != null && existingIds.contains(item.getId())) {
                skippedCount++;
                continue; // Bỏ qua các yêu cầu hiện có để tránh trùng lặp
            }
            tableModel.addRow(new RunnerRowData(item));
            if (item.getId() != null) {
                existingIds.add(item.getId()); // Các bản sao cũng bị loại bỏ khỏi cùng một đợt
            }
        }

        if (skippedCount > 0) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_DUPLICATE_SKIPPED, skippedCount));
        }

        table.setEnabled(true);
        runBtn.setEnabled(true);
        // Lưu cấu hình sau khi tải yêu cầu
        scheduleSave();
    }

    /**
     * Tải cấu hình đã lưu
     */
    private void loadSaved() {
        try {
            FunctionalConfigSnapshot snapshot = persistenceService.loadSnapshot();
            csvDataPanel.restoreState(toCsvState(snapshot.getCsvState()));
            List<RunnerRowData> savedRows = restoreRows(snapshot.getRows());
            if (!savedRows.isEmpty()) {
                for (RunnerRowData row : savedRows) {
                    tableModel.addRow(row);
                }
                table.setEnabled(true);
                runBtn.setEnabled(true);
                log.info("Loaded {} saved test configurations", savedRows.size());
            }
        } catch (Exception e) {
            log.error("Failed to load saved config", e);
        }
    }

    public void switchWorkspaceAndRefreshUI() {
        if (!isReadyForWorkspaceSwitch()) {
            return;
        }

        executionGeneration++;
        isStopped = true;
        stopExecutionTimer();
        // chuyển đổi workspace Hủy các tác vụ đang chờ xử lý trước khi lưu để tránh những tác vụ cũ workspace của CSV/Cấu hình hàng được ghi thành mới với độ trễ workspace。
        autoSaveSupport.cancel();
        tableModel.clear();
        table.clearSelection();
        table.setEnabled(false);
        runBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        resetProgress();
        executionHistory = null;
        resultsPanel.updateExecutionHistory(null);
        mainTabbedPane.setSelectedIndex(0);
        loadSaved();
    }

    private boolean isReadyForWorkspaceSwitch() {
        return tableModel != null
                && table != null
                && persistenceService != null
                && csvDataPanel != null
                && runBtn != null
                && stopBtn != null
                && timeLabel != null
                && progressLabel != null
                && resultsPanel != null
                && mainTabbedPane != null;
    }

    /**
     * Đồng bộ hóa mới nhất HttpRequestItem đến hàng tương ứng trong bảng (bằng  Collections được gọi khi lưu)
     * Tránh người dùng editSubPanel Sau khi sửa đổi và lưu,FunctionalPanel Dữ liệu cũ vẫn được giữ lại.
     *
     * @param item Dữ liệu yêu cầu đã lưu mới nhất
     */
    public void syncRequestItem(HttpRequestItem item) {
        if (item == null || item.getId() == null) return;
        List<RunnerRowData> rows = tableModel.getAllRows();
        for (int i = 0; i < rows.size(); i++) {
            RunnerRowData row = rows.get(i);
            if (row != null && row.requestItem != null && item.getId().equals(row.requestItem.getId())) {
                row.requestItem = item;
                row.name = item.getName();
                row.url = item.getUrl();
                row.method = item.getMethod();
                tableModel.fireTableRowsUpdated(i, i);
                log.debug("FunctionalPanel syncRequestItem: id={}, name={}", item.getId(), item.getName());
            }
        }
    }

    /**
     * Lưu cấu hình hiện tại
     */
    public void save() {
        try {
            if (tableModel == null || persistenceService == null) {
                return;
            }
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            autoSaveSupport.cancel();
            persistenceService.save(toConfigSnapshot(tableModel.getAllRows()));
        } catch (Exception e) {
            log.error("Failed to save config", e);
        }
    }

    private void scheduleSave() {
        // Bảng và CSV Việc thay đổi bảng điều khiển diễn ra thường xuyên và tính năng lưu chống rung được thực hiện một cách thống nhất nhằm giảm áp lực tải trong quá trình chỉnh sửa liên tục.
        autoSaveSupport.requestSave();
    }

    private void saveAsync() {
        try {
            if (tableModel == null || persistenceService == null) {
                return;
            }
            persistenceService.saveAsync(toConfigSnapshot(tableModel.getAllRows()));
        } catch (Exception e) {
            log.error("Failed to schedule functional config save", e);
        }
    }

    private List<RunnerRowData> restoreRows(List<FunctionalConfigRow> configRows) {
        List<RunnerRowData> rows = new ArrayList<>();
        if (configRows == null || configRows.isEmpty()) {
            return rows;
        }
        for (FunctionalConfigRow configRow : configRows) {
            if (configRow == null || configRow.getRequestId() == null || configRow.getRequestId().isBlank()) {
                continue;
            }
            try {
                Optional<HttpRequestItem> requestItem = requestLookup.findRequestItemById(configRow.getRequestId());
                if (requestItem.isEmpty()) {
                    log.warn("Request with ID {} not found in collections, skipping", configRow.getRequestId());
                    continue;
                }
                RunnerRowData row = new RunnerRowData(requestItem.get());
                row.selected = configRow.isSelected();
                rows.add(row);
            } catch (Exception e) {
                log.warn("Failed to restore functional row for request {}: {}", configRow.getRequestId(), e.getMessage());
            }
        }
        return rows;
    }

    private FunctionalConfigSnapshot toConfigSnapshot(List<RunnerRowData> rows) {
        List<FunctionalConfigRow> configRows = new ArrayList<>();
        if (rows != null) {
            for (RunnerRowData row : rows) {
                if (row != null && row.requestItem != null && row.requestItem.getId() != null) {
                    configRows.add(new FunctionalConfigRow(row.requestItem.getId(), row.selected));
                }
            }
        }
        return new FunctionalConfigSnapshot(configRows, exportFunctionalCsvState());
    }

    private FunctionalCsvDataState exportFunctionalCsvState() {
        if (csvDataPanel == null) {
            return null;
        }
        CsvDataPanel.CsvState state = csvDataPanel.exportState();
        return toFunctionalCsvDataState(state);
    }

    private static FunctionalCsvDataState toFunctionalCsvDataState(CsvDataPanel.CsvState state) {
        if (state == null) {
            return null;
        }
        return new FunctionalCsvDataState(state.getSourceName(), state.getHeaders(), state.getRows());
    }

    private static CsvDataPanel.CsvState toCsvState(FunctionalCsvDataState state) {
        if (state == null) {
            return null;
        }
        return new CsvDataPanel.CsvState(state.getSourceName(), state.getHeaders(), state.getRows());
    }

    /**
     * Làm mới dữ liệu yêu cầu từ bộ sưu tập
     * Tải lại cấu hình mới nhất cho tất cả các yêu cầu
     */
    private void refreshRequestsFromCollections() {
        List<RunnerRowData> currentRows = tableModel.getAllRows();
        if (currentRows.isEmpty()) {
            NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_NO_RUNNABLE_REQUEST));
            return;
        }

        int updatedCount = 0;
        int removedCount = 0;
        List<Integer> rowsToRemove = new ArrayList<>();

        for (int i = 0; i < currentRows.size(); i++) {
            RunnerRowData row = currentRows.get(i);
            if (row == null || row.requestItem == null) {
                rowsToRemove.add(i);
                removedCount++;
                continue;
            }

            // vượt quaIDTìm cấu hình yêu cầu mới nhất từ bộ sưu tập
            HttpRequestItem latestRequestItem = requestLookup.findRequestItemById(row.requestItem.getId()).orElse(null);

            if (latestRequestItem == null) {
                // Yêu cầu đã bị xóa khỏi bộ sưu tập
                log.warn("Request with ID {} not found in collections", row.requestItem.getId());
                rowsToRemove.add(i);
                removedCount++;
            } else {
                // Cập nhật dữ liệu yêu cầu
                try {
                    row.requestItem = latestRequestItem;
                    row.name = latestRequestItem.getName();
                    row.url = latestRequestItem.getUrl();
                    row.method = latestRequestItem.getMethod();
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to refresh request {}: {}", latestRequestItem.getName(), e.getMessage());
                }
            }
        }

        // Xóa các yêu cầu không tồn tại (xóa từ sau ra trước để tránh thay đổi chỉ mục)
        for (int i = rowsToRemove.size() - 1; i >= 0; i--) {
            tableModel.removeRow(rowsToRemove.get(i));
        }

        // Làm mới màn hình bảng
        tableModel.fireTableDataChanged();

        // Lưu cấu hình đã cập nhật
        scheduleSave();

        // Cập nhật trạng thái nút
        if (tableModel.getRowCount() == 0) {
            runBtn.setEnabled(false);
        }

        // Hiển thị kết quả làm mới
        if (removedCount > 0) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_REFRESH_WARNING, removedCount));
        } else {
            NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_REFRESH_SUCCESS, updatedCount));
        }
    }
}
