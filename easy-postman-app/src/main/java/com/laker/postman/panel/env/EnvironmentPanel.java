package com.laker.postman.panel.env;

import com.laker.postman.common.component.notification.NotificationCenter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.button.PlusButton;
import com.laker.postman.common.component.button.SaveButton;
import com.laker.postman.common.component.combobox.EnvironmentComboBox;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.component.list.EnvironmentListCellRenderer;
import com.laker.postman.common.component.table.EasyVariableTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Environment;
import com.laker.postman.environment.EnvironmentItem;
import com.laker.postman.model.Variable;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.ideahttp.IntelliJHttpEnvParser;
import com.laker.postman.service.postman.PostmanEnvironmentParser;
import com.laker.postman.panel.workspace.WorkspaceTransferCoordinator;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng quản lý biến môi trường
 */
@Slf4j
public class EnvironmentPanel extends UiSingletonPanel {
    public static final String EXPORT_FILE_NAME = "Dev Workbench-Environments.json";
    private EasyVariableTablePanel variablesTablePanel;
    private transient Environment currentEnvironment;
    private JList<EnvironmentItem> environmentList;
    private DefaultListModel<EnvironmentItem> environmentListModel;
    private SearchTextField searchField;
    private PlusButton plusBtn;
    private String originalVariablesSnapshot; // Ảnh chụp biến ban đầu, được sử dụng trực tiếpjsonchuỗi
    private boolean isLoadingData = false; // Được sử dụng để kiểm soát xem dữ liệu có đang tải hay không và ngăn chặn việc lưu tự động
    private SearchTextField tableSearchField; // Hộp tìm kiếm bảng
    private JPanel toolbarPanel; // Bảng thanh công cụ bảng

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);
        setPreferredSize(new Dimension(700, 400));

        // Bảng danh sách môi trường ở bên trái
        JPanel leftPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(leftPanel);
        leftPanel.setPreferredSize(new Dimension(AppToolWindowChrome.DEFAULT_SIDE_WIDTH, 200));
        leftPanel.setMinimumSize(new Dimension(220, 160));
        // Nút xuất tìm kiếm và nhập hàng đầu
        leftPanel.add(getSearchAndImportPanel(), BorderLayout.NORTH);

        // Danh sách môi trường
        environmentListModel = new DefaultListModel<>();
        environmentList = new JList<>(environmentListModel);
        environmentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Hỗ trợ nhiều lựa chọn
        environmentList.setFixedCellHeight(28); // Đặt chiều cao của mỗi hàng
        environmentList.setCellRenderer(new EnvironmentListCellRenderer());
        environmentList.setFixedCellWidth(0); // cho phépJListChiều rộng thích ứng
        environmentList.setVisibleRowCount(-1); // cho phépJListHiển thị tất cả các hàng
        JScrollPane envListScroll = new JScrollPane(environmentList);
        envListScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // Tắt thanh cuộn ngang
        ToolWindowSurfaceStyle.applyListScrollPaneCard(envListScroll, environmentList);
        leftPanel.add(envListScroll, BorderLayout.CENTER);

        // bên phải Nhập khẩu Xuất khẩu Bảng biến và các phép toán
        JPanel rightPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(rightPanel);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Thanh công cụ trên cùng: nút lưu và hộp tìm kiếm
        JPanel tableToolbarPanel = createTableToolbar();
        rightPanel.add(tableToolbarPanel, BorderLayout.NORTH);

        // Vùng chứa bảng biến, thêm lề
        JPanel tableContainer = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(tableContainer);
        tableContainer.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        variablesTablePanel = new EasyVariableTablePanel();
        ToolWindowSurfaceStyle.applyCard(variablesTablePanel);
        tableContainer.add(variablesTablePanel, BorderLayout.CENTER);
        rightPanel.add(tableContainer, BorderLayout.CENTER);


        // sử dụng JSplitPane Kết hợp các bảng bên trái và bên phải và hỗ trợ kéo và thay đổi kích thước
        JSplitPane splitPane = AppToolWindowChrome.createHorizontalCardSplitPane(
                leftPanel,
                rightPanel,
                AppToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        splitPane.setResizeWeight(0.3); // Đặt trọng lượng điều chỉnh bảng bên trái (30%）

        add(splitPane, BorderLayout.CENTER);

        // Khởi tạo xác thực biểu mẫu và chức năng tự động lưu
        initTableValidationAndAutoSave();
    }

    /**
     * Tạo thanh công cụ bảng: nút lưu và hộp tìm kiếm
     */
    private JPanel createTableToolbar() {
        SaveButton saveButton;

        // Nút chỉnh sửa hàng loạt
        EditButton bulkEditButton = new EditButton(IconUtil.SIZE_MEDIUM);
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());

        // Nút lưu
        saveButton = new SaveButton();
        saveButton.addActionListener(e -> saveVariablesManually());

        // Hộp tìm kiếm bảng
        tableSearchField = new SearchTextField();
        tableSearchField.setPreferredSize(new Dimension(200, ToolWindowSidebarToolbar.SEARCH_HEIGHT));
        tableSearchField.setMaximumSize(new Dimension(200, ToolWindowSidebarToolbar.SEARCH_HEIGHT));
        tableSearchField.addActionListener(e -> filterTableRows());

        // Theo dõi các thay đổi trong tùy chọn tìm kiếm và kích hoạt quá trình lọc lại
        tableSearchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });
        tableSearchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });

        toolbarPanel = ToolWindowActionToolbar.right(bulkEditButton, saveButton, tableSearchField);

        return toolbarPanel;
    }


    /**
     * Khởi tạo chức năng tự động lưu biểu mẫu
     * Tương tự Postman，Các biến môi trường có hiệu lực ngay sau khi sửa đổi, không cần lưu thủ công
     */
    private void initTableValidationAndAutoSave() {

        // Thêm trình nghe mô hình bảng để lưu tự động ngay lập tức (tương tự Postman）
        variablesTablePanel.addTableModelListener(e -> {
            if (currentEnvironment == null || isLoadingData) return;

            // Ngăn không cho kích hoạt tính năng tự động lưu trong khi tải dữ liệu
            if (e.getType() == TableModelEvent.INSERT ||
                    e.getType() == TableModelEvent.UPDATE ||
                    e.getType() == TableModelEvent.DELETE) {

                // sử dụng SwingUtilities.invokeLater Đảm bảo thực hiện lưu sau khi xử lý sự kiện hoàn tất
                SwingUtilities.invokeLater(() -> {
                    // Bỏ qua tính năng tự động lưu trong khi kéo để tránh lưu trạng thái trung gian
                    if (!isLoadingData && !variablesTablePanel.isDragging() && isVariablesChanged()) {
                        autoSaveVariables();
                    }
                });
            }
        });
    }

    /**
     * Tự động lưu biến (lưu thầm, không nhắc)
     * Tương tự Postman Một trải nghiệm tiết kiệm ngay lập tức
     */
    private void autoSaveVariables() {
        if (currentEnvironment == null) return;

        try {
            // Trực tiếp từ model Đọc mà không dừng chỉnh sửa ô để tránh làm gián đoạn quá trình nhập đang diễn ra của người dùng (ví dụ: Tab điều hướng).
            // Phương pháp này được cung cấp bởi  TableModelListener（invokeLater）kích hoạt,model đã là giá trị cam kết mới nhất.
            List<Variable> variableList = variablesTablePanel.getVariableListFromModel();
            currentEnvironment.setVariableList(new ArrayList<>(variableList)); // Sử dụng các bản sao để tránh sửa đổi đồng thời
            EnvironmentService.saveEnvironment(currentEnvironment);
            // Cập nhật ảnh chụp nhanh sau khi lưu
            originalVariablesSnapshot = JSONUtil.toJsonStr(currentEnvironment.getVariableList());
            log.debug("Auto-saved environment: {}", currentEnvironment.getName());
        } catch (Exception ex) {
            log.error("Failed to auto-save environment variables", ex);
        }
    }

    private JPanel getSearchAndImportPanel() {
        plusBtn = new PlusButton();
        plusBtn.setToolTipText("New / Import");
        plusBtn.addActionListener(e -> showPlusMenu());

        searchField = new SearchTextField();

        return new ToolWindowSidebarToolbar(plusBtn, searchField);
    }

    private void showPlusMenu() {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        // ── Tạo một môi trường mới
        JMenuItem newEnvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_ADD),
                IconUtil.createThemed("icons/plus.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        newEnvItem.addActionListener(e -> addEnvironment());
        menu.add(newEnvItem);

        menu.addSeparator();

        // ── Nhập khẩu
        JMenuItem importEasyToolsItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_EASY),
                IconUtil.create("icons/easy.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importEasyToolsItem.addActionListener(e -> importEnvironments());
        menu.add(importEasyToolsItem);

        JMenuItem importPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_POSTMAN),
                IconUtil.create("icons/postman.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importPostmanItem.addActionListener(e -> importPostmanEnvironments());
        menu.add(importPostmanItem);

        JMenuItem importIntelliJItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_INTELLIJ),
                IconUtil.create("icons/idea-http.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importIntelliJItem.addActionListener(e -> importIntelliJEnvironments());
        menu.add(importIntelliJItem);

        menu.show(plusBtn, 0, plusBtn.getHeight());
    }

    @Override
    protected void registerListeners() {
        // Hộp thả xuống ở góc trên bên phải của thanh menu được liên kết
        EnvironmentComboBox topComboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        if (topComboBox != null) {
            topComboBox.setOnEnvironmentChange(env -> {
                environmentListModel.clear();
                List<Environment> envs = EnvironmentService.getAllEnvironments();
                for (Environment envItem : envs) {
                    environmentListModel.addElement(new EnvironmentItem(envItem));
                }
                if (!environmentListModel.isEmpty()) {
                    environmentList.setSelectedIndex(topComboBox.getSelectedIndex()); // Đặt môi trường hiện đang hoạt động đã chọn
                }
                loadActiveEnvironmentVariables();
            });
        }
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }
        });

        // Trình nghe hộp tìm kiếm bảng
        tableSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterTableRows();
            }

            public void removeUpdate(DocumentEvent e) {
                filterTableRows();
            }

            public void changedUpdate(DocumentEvent e) {
                filterTableRows();
            }
        });
        environmentList.addListSelectionListener(e -> { // Nhấp chuột trái vào danh sách môi trường nghe
            if (!e.getValueIsAdjusting()) {
                EnvironmentItem item = environmentList.getSelectedValue();
                if (item == null || item.getEnvironment() == currentEnvironment) {
                    return; // Không có chuyển đổi môi trường và không xử lý
                }
                currentEnvironment = item.getEnvironment();
                loadVariables(currentEnvironment);
            }
        });
        // Danh sách môi trường menu chuột phải
        addRightMenuList();

        // Thêm phím tắt lưu thủ công (mặc dù có tính năng lưu tự động nhưng việc lưu thủ công mang lại cho người dùng cảm giác kiểm soát)
        addSaveKeyStroke();

        // Các biến môi trường hiện đang hoạt động được tải theo mặc định
        loadActiveEnvironmentVariables();

        // Tải và tìm kiếm danh sách môi trường
        reloadEnvironmentList("");

    }

    /**
     * Đã thêm phím tắt lưu thủ công (Cmd+S / Ctrl+S）
     * Mặc dù tính năng lưu tự động đã có sẵn nhưng phím tắt lưu thủ công vẫn được giữ lại để mang lại cho người dùng cảm giác chủ động kiểm soát
     */
    private void addSaveKeyStroke() {
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke("meta S"); // Mac Command+S
        KeyStroke saveKeyStroke2 = KeyStroke.getKeyStroke("control S"); // Windows/Linux Ctrl+S
        String actionKey = "saveEnvironmentVariables";
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke, actionKey);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke2, actionKey);
        this.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Đầu tiên hãy chuyển trọng tâm sang bảng điều khiển và kích hoạt nó table cell editor của
                // terminateEditOnFocusLost Cơ chế đảm bảo rằng việc chỉnh sửa đang được thực hiện cell
                // Giá trị được gửi tới model，Sau đó thực hiện logic lưu.
                EnvironmentPanel.this.requestFocusInWindow();
                SwingUtilities.invokeLater(() -> saveVariablesManually());
            }
        });
    }


    private void addRightMenuList() {
        JPopupMenu envListMenu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(envListMenu);
        JMenuItem addItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_ADD),
                IconUtil.createThemed("icons/environments.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addItem.addActionListener(e -> addEnvironment());
        envListMenu.add(addItem);
        envListMenu.addSeparator();
        JMenuItem renameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_RENAME),
                IconUtil.createThemed("icons/refresh.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        // cài đặt F2 Các phím tắt được hiển thị
        renameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        JMenuItem copyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DUPLICATE),
                IconUtil.createThemed("icons/duplicate.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)); // Sao chép mục menu
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE),
                IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        // cài đặt Delete Các phím tắt được hiển thị
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // Tự động cập nhật văn bản mục menu xóa (hiển thị số đã chọn)
        envListMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                int selectedCount = environmentList.getSelectedIndices().length;
                if (selectedCount > 1) {
                    deleteItem.setText(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE) + " (" + selectedCount + ")");
                } else {
                    deleteItem.setText(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE));
                }

                // Sao chép và đổi tên chỉ có sẵn cho một lựa chọn
                renameItem.setEnabled(selectedCount == 1);
                copyItem.setEnabled(selectedCount == 1);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // Không cần xử lý khi menu bị ẩn
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // Không cần xử lý khi menu bị hủy
            }
        });

        JMenuItem exportPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_EXPORT_POSTMAN),
                IconUtil.create("icons/postman.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)); // Màu sắc
        exportPostmanItem.addActionListener(e -> exportSelectedEnvironmentAsPostman());
        renameItem.addActionListener(e -> renameSelectedEnvironment());
        copyItem.addActionListener(e -> copySelectedEnvironment()); // Sao chép sự kiện
        deleteItem.addActionListener(e -> deleteSelectedEnvironments()); // Thay đổi để xóa hàng loạt
        envListMenu.add(renameItem);
        envListMenu.add(copyItem);
        envListMenu.add(deleteItem);
        envListMenu.addSeparator();
        envListMenu.add(exportPostmanItem);

        // Di chuyển đến không gian làm việc khác
        JMenuItem moveToWorkspaceItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_MENU_ITEM),
                IconUtil.createThemed("icons/workspace.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        moveToWorkspaceItem.addActionListener(e -> moveEnvironmentToWorkspace());
        envListMenu.add(moveToWorkspaceItem);

        // Thêm trình nghe bàn phím, hỗ trợ F2 Đổi tên và Delete xóa bỏ
        environmentList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedCount = environmentList.getSelectedIndices().length;
                if (selectedCount > 0) {
                    if (e.getKeyCode() == KeyEvent.VK_F2 && selectedCount == 1) {
                        // F2 Đổi tên (chỉ một lựa chọn)
                        renameSelectedEnvironment();
                    } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        // Delete hoặc Backspace Xóa hàng loạt
                        deleteSelectedEnvironments();
                    }
                }
            }
        });

        environmentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) { // Trình đơn nhấp chuột phải
                    int idx = environmentList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        // Lựa chọn chỉ được thay đổi nếu mục được nhấp không có trong danh sách hiện được chọn
                        int[] selectedIndices = environmentList.getSelectedIndices();
                        boolean isSelected = false;
                        for (int selectedIdx : selectedIndices) {
                            if (selectedIdx == idx) {
                                isSelected = true;
                                break;
                            }
                        }
                        // Nếu mục được nhấp không được chọn, nó sẽ được đặt thành mục hiện được chọn
                        if (!isSelected) {
                            environmentList.setSelectedIndex(idx);
                        }
                        // Nếu mục được bấm đã được chọn thì trạng thái đa lựa chọn hiện tại sẽ được duy trì
                    }
                    envListMenu.show(environmentList, e.getX(), e.getY());
                }
                // Nhấp đúp để kích hoạt môi trường và kích hoạt hộp thả xuống
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int idx = environmentList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        environmentList.setSelectedIndex(idx);
                        EnvironmentItem item = environmentList.getModel().getElementAt(idx);
                        if (item != null) {
                            Environment env = item.getEnvironment();
                            // Kích hoạt môi trường
                            EnvironmentService.setActiveEnvironment(env.getId());
                            // Hộp thả xuống trên cùng được liên kết
                            EnvironmentComboBox comboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
                            if (comboBox != null) {
                                comboBox.setSelectedEnvironment(env);
                            }
                            // Làm mới bảng điều khiển
                            refreshUI();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) mousePressed(e);
            }
        });
        // Hỗ trợ sắp xếp kéo và thả
        environmentList.setDragEnabled(true);
        environmentList.setDropMode(DropMode.INSERT);
        environmentList.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = environmentList.getSelectedIndex();
                EnvironmentItem selected = environmentList.getSelectedValue();
                return new StringSelection(selected != null ? selected.toString() : "");
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop();
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int toIndex = dl.getIndex();
                if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false;
                EnvironmentItem moved = environmentListModel.getElementAt(fromIndex);
                environmentListModel.remove(fromIndex);
                if (toIndex > fromIndex) toIndex--;
                environmentListModel.add(toIndex, moved);
                environmentList.setSelectedIndex(toIndex);
                // 1. Đồng bộ hóa trình tự với EnvironmentService
                persistEnvironmentOrder();
                // 2. Đồng bộ hóa với hộp thả xuống trên cùng
                syncComboBoxOrder();
                return true;
            }
        });
    }

    /**
     * Chỉ các biến môi trường hiện đang hoạt động mới được tải
     */
    public void loadActiveEnvironmentVariables() {
        Environment env = EnvironmentService.getActiveEnvironment();
        currentEnvironment = env;
        loadVariables(env);
    }

    private void loadVariables(Environment env) {
        // Đặt cờ để bắt đầu tải dữ liệu (phải ở phía trước để ngăn mọi thao tác kích hoạt lưu tự động)
        isLoadingData = true;

        try {
            variablesTablePanel.stopCellEditing();
            currentEnvironment = env;
            variablesTablePanel.clear();

            if (env != null) {
                variablesTablePanel.setVariableList(env.getVariableList());
                originalVariablesSnapshot = JSONUtil.toJsonStr(env.getVariableList()); // Sử dụngrowsChụp ảnh nhanh để đảm bảo đồng bộ hóa
            } else {
                variablesTablePanel.clear();
                originalVariablesSnapshot = JSONUtil.toJsonStr(new ArrayList<>()); // Ảnh chụp nhanh trống
            }
        } finally {
            // sử dụng finally Đảm bảo rằng cờ sẽ bị xóa ngay cả khi có ngoại lệ xảy ra
            isLoadingData = false;
        }
    }

    /**
     * Lưu các biến của môi trường hiện tại vào một tệp (được gọi nội bộ, được lưu âm thầm)
     * Để tự động lưu và gọi nội bộ của chương trình
     */
    private void saveVariables() {
        if (currentEnvironment == null) return;
        variablesTablePanel.stopCellEditing();

        // Lưu sang định dạng mới variableList
        List<Variable> variableList = variablesTablePanel.getVariableList();
        currentEnvironment.setVariableList(new ArrayList<>(variableList)); // Sử dụng các bản sao để tránh sửa đổi đồng thời
        EnvironmentService.saveEnvironment(currentEnvironment);
        // Sau khi lưu, hãy cập nhật ảnh chụp nhanh lênjsonchuỗi
        originalVariablesSnapshot = JSONUtil.toJsonStr(currentEnvironment.getVariableList());
    }

    /**
     * Lưu thủ công các biến môi trường (người dùng chủ động nhấn Cmd+S / Ctrl+S khi được gọi)
     * Hiển thị thông báo lưu thành công và đưa ra phản hồi cho người dùng
     */
    private void saveVariablesManually() {
        if (currentEnvironment == null) return;

        // Gọi logic lưu
        saveVariables();

        // Hiển thị thông báo lưu thành công (chỉ hiển thị khi lưu thủ công, lưu tự động không làm phiền người dùng)
        NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_SAVE_SUCCESS));
    }

    /**
     * Xuất tất cả các biến môi trường dưới dạng JSONtập tin
     */
    public void exportEnvironments() {
        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "environments.export",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_TITLE));
        fileChooser.setSelectedFile(new File(EXPORT_FILE_NAME));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
                java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
                writer.write(JSONUtil.toJsonPrettyStr(envs));
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_SUCCESS));
            } catch (Exception ex) {
                log.error("Export Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Nhập các biến môi trườngJSONtập tin
     */
    private void importEnvironments() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "environments.import.easyPostman",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                java.util.List<Environment> envs = JSONUtil.toList(JSONUtil.readJSONArray(fileToOpen, StandardCharsets.UTF_8), Environment.class);
                // Nhập môi trường mới
                refreshListAndComboFromAdd(envs);
            } catch (Exception ex) {
                log.error("Import Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshListAndComboFromAdd(List<Environment> envs) {
        EnvironmentComboBox environmentComboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        for (Environment env : envs) {
            EnvironmentService.saveEnvironment(env);
            environmentComboBox.addItem(new EnvironmentItem(env)); // Thêm vào hộp thả xuống
            environmentListModel.addElement(new EnvironmentItem(env)); // Thêm vào danh sách
        }
        NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_SUCCESS));
    }

    /**
     * Nhập khẩuPostmancác biến môi trườngJSONtập tin
     */
    private void importPostmanEnvironments() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "environments.import.postman",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            java.io.File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);
                if (!envs.isEmpty()) {
                    // Nhập môi trường mới
                    refreshListAndComboFromAdd(envs);
                } else {
                    JOptionPane.showMessageDialog(this,
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_INVALID),
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_TITLE), JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Import Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Nhập khẩuIntelliJ IDEA HTTP Clientcác biến môi trườngJSONtập tin
     */
    private void importIntelliJEnvironments() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "environments.import.intellijHttp",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                List<Environment> envs = IntelliJHttpEnvParser.parseIntelliJEnvironments(json);
                if (!envs.isEmpty()) {
                    // Nhập môi trường mới
                    refreshListAndComboFromAdd(envs);
                } else {
                    JOptionPane.showMessageDialog(this,
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_INVALID),
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_TITLE), JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Import IntelliJ Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Thêm môi trường mới
    private void addEnvironment() {
        TextInputDialog.showRequiredName(this,
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_ADD_TITLE),
                "",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_NAME_EMPTY)
        ).ifPresent(name -> {
            Environment env = new Environment(name);
            env.setId("env-" + IdUtil.simpleUUID());
            EnvironmentService.saveEnvironment(env);
            environmentListModel.addElement(new EnvironmentItem(env));
            environmentList.setSelectedValue(new EnvironmentItem(env), true);
            EnvironmentComboBox environmentComboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
            if (environmentComboBox != null) {
                environmentComboBox.addItem(new EnvironmentItem(env));
            }
        });
    }

    private void reloadEnvironmentList(String filter) {
        environmentListModel.clear();
        java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
        int activeIdx = -1;
        for (Environment env : envs) {
            if (filter == null || filter.isEmpty() || env.getName().toLowerCase().contains(filter.toLowerCase())) {
                environmentListModel.addElement(new EnvironmentItem(env));
                if (env.isActive()) {
                    activeIdx = environmentListModel.size() - 1;
                }
            }
        }
        if (!environmentListModel.isEmpty()) {
            environmentList.setSelectedIndex(Math.max(activeIdx, 0));
        }
        // Ô tìm kiếm chuyển sang màu đỏ khi không có kết quả và có cụm từ tìm kiếm
        boolean noResult = environmentListModel.isEmpty() && filter != null && !filter.isEmpty();
        searchField.setNoResult(noResult);
    }

    private void renameSelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        TextInputDialog.showRequiredName(this,
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_RENAME_TITLE),
                env.getName(),
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_NAME_EMPTY)
        ).ifPresent(newName -> {
            if (newName.equals(env.getName())) {
                return;
            }
            env.setName(newName);
            EnvironmentService.saveEnvironment(env);
            environmentListModel.setElementAt(new EnvironmentItem(env), environmentList.getSelectedIndex());
            // Làm mới đồng bộ hộp thả xuống môi trường trên cùng
            UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();
        });
    }

    /**
     * Xóa các môi trường đã chọn theo đợt
     */
    private void deleteSelectedEnvironments() {
        List<EnvironmentItem> selectedItems = environmentList.getSelectedValuesList();
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        int selectedCount = selectedItems.size();
        String message;
        String title = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_TITLE);

        if (selectedCount == 1) {
            // Xóa đơn
            Environment env = selectedItems.get(0).getEnvironment();
            message = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_PROMPT, env.getName());
        } else {
            // Xóa hàng loạt
            message = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_BATCH_PROMPT, selectedCount);
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int deletedCount = 0;
            // Xóa môi trường đã chọn
            for (EnvironmentItem item : selectedItems) {
                try {
                    Environment env = item.getEnvironment();
                    environmentListModel.removeElement(item);
                    EnvironmentService.deleteEnvironment(env.getId());
                    deletedCount++;
                    log.info("Đã xóa môi trường: {}", env.getName());
                } catch (Exception e) {
                    log.error("Không thể xóa môi trường: {}", item.getEnvironment().getName(), e);
                }
            }

            // Làm mới hộp thả xuống trên cùng
            UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();

            // Đặt bảng biến hiện tại làm môi trường hoạt động
            loadActiveEnvironmentVariables();

            // Một thông báo xóa thành công được hiển thị
            if (deletedCount > 0) {
                NotificationCenter.showSuccess(
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_SUCCESS, deletedCount)
                );
            }
        }
    }


    // Sao chép các phương thức môi trường
    private void copySelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        try {
            Environment copy = new Environment(env.getName() + " " + I18nUtil.getMessage(MessageKeys.ENV_NAME_COPY_SUFFIX));
            copy.setId("env-" + IdUtil.simpleUUID());
            // Sao chép các biến
            for (String key : env.getVariables().keySet()) {
                copy.addVariable(key, env.getVariable(key));
            }
            EnvironmentService.saveEnvironment(copy);
            EnvironmentItem copyItem = new EnvironmentItem(copy);
            environmentListModel.addElement(copyItem);
            EnvironmentComboBox environmentComboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
            if (environmentComboBox != null) {
                environmentComboBox.addItem(copyItem);
            }
            environmentList.setSelectedValue(copyItem, true);
        } catch (Exception ex) {
            log.error("Không thể sao chép môi trường", ex);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.ENV_DIALOG_COPY_FAIL, ex.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Làm mới toàn bộ bảng điều khiển môi trường (danh sách và bảng biến, giữ cho môi trường hoạt động được đánh dấu và chọn)
     */
    public void refreshUI() {
        // Nhận môi trường kích hoạt hiện tạiid
        Environment active = EnvironmentService.getActiveEnvironment();
        String activeId = active != null ? active.getId() : null;
        // Tải lại danh sách môi trường
        environmentListModel.clear();
        java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
        int selectIdx = -1;
        for (int i = 0; i < envs.size(); i++) {
            Environment env = envs.get(i);
            EnvironmentItem item = new EnvironmentItem(env);
            environmentListModel.addElement(item);
            if (activeId != null && activeId.equals(env.getId())) {
                selectIdx = i;
            }
        }
        // Bỏ chọn trước rồi chọn để buộc kích hoạt selection sự kiện để đảm bảo bảng được làm mới
        environmentList.clearSelection();
        if (selectIdx >= 0) {
            environmentList.setSelectedIndex(selectIdx);
            environmentList.ensureIndexIsVisible(selectIdx);
        }
        // Buộc làm mới bảng biến để ngăn chặnselectionSự kiện không được kích hoạt
        EnvironmentItem selectedItem = environmentList.getSelectedValue();
        if (selectedItem != null) {
            loadVariables(selectedItem.getEnvironment());
        } else {
            variablesTablePanel.clear();
        }
    }

    // Sử dụng để xác định xem nội dung bảng hiện tại có nhất quán với ảnh chụp nhanh hay khôngJSONSo sánh tuần tự hóa
    private boolean isVariablesChanged() {
        String curJson = JSONUtil.toJsonStr(variablesTablePanel.getVariableListFromModel());
        boolean isVariablesChanged = !CharSequenceUtil.equals(curJson, originalVariablesSnapshot);
        if (isVariablesChanged) {
            log.debug("env name: {}", currentEnvironment != null ? currentEnvironment.getName() : "null");
            log.debug("current  variables: {}", curJson);
            log.debug("original variables: {}", originalVariablesSnapshot);
        }
        return isVariablesChanged;
    }

    // Xuất môi trường đã chọn dưới dạngPostmanđịnh dạng
    private void exportSelectedEnvironmentAsPostman() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "environments.export.postman",
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_TITLE));
        fileChooser.setSelectedFile(new File(env.getName() + "-postman-env.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                // Chỉ xuất môi trường hiện tại dưới dạngPostmanđịnh dạng
                String postmanEnvJson = PostmanEnvironmentParser.toPostmanEnvironmentJson(env);
                FileUtil.writeUtf8String(postmanEnvJson, fileToSave);
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_SUCCESS));
            } catch (Exception ex) {
                log.error("Xuất khẩuPostmanMôi trường thất bại", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Thứ tự liên tục sau khi kéo
     */
    private void persistEnvironmentOrder() {
        List<String> idOrder = new ArrayList<>();
        for (int i = 0; i < environmentListModel.size(); i++) {
            idOrder.add(environmentListModel.get(i).getEnvironment().getId());
        }
        EnvironmentService.saveEnvironmentOrder(idOrder);
    }

    /**
     * Đồng bộ hóa thứ tự các ô thả xuống trên cùng sau khi kéo
     */
    private void syncComboBoxOrder() {
        EnvironmentComboBox comboBox = UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        if (comboBox != null) {
            List<EnvironmentItem> items = new ArrayList<>();
            for (int i = 0; i < environmentListModel.size(); i++) {
                items.add(environmentListModel.get(i));
            }
            comboBox.setModel(new DefaultComboBoxModel<>(items.toArray(new EnvironmentItem[0])));
        }
    }

    /**
     * Chuyển sang tệp dữ liệu môi trường của không gian làm việc được chỉ định và làm mới nóUI
     */
    public void switchWorkspaceAndRefreshUI(String envFilePath) {
        EnvironmentService.setDataFilePath(envFilePath);
        this.refreshUI();
        // Làm mới đồng bộ hộp thả xuống môi trường trên cùng
        UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();
    }

    /**
     * Di chuyển môi trường sang không gian làm việc khác
     */
    private void moveEnvironmentToWorkspace() {
        EnvironmentItem selectedItem = environmentList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }

        Environment environment = selectedItem.getEnvironment();

        // Sử dụng lớp trợ giúp chuyển không gian làm việc (hiển thị thông báo thành công)
        WorkspaceTransferCoordinator.transferToWorkspace(
                environment.getName(),
                (targetWorkspace, itemName) -> performEnvironmentMove(environment, targetWorkspace)
        );
    }


    /**
     * Thực hiện các hoạt động chuyển giao môi trường
     */
    private void performEnvironmentMove(Environment environment, Workspace targetWorkspace) {
        // 1. Sao chép sâu các đối tượng môi trường
        Environment copiedEnvironment = new Environment(environment.getName());
        copiedEnvironment.setId(environment.getId()); // Vẫn như cũID
        // Sao chép tất cả các biến
        for (String key : environment.getVariables().keySet()) {
            copiedEnvironment.addVariable(key, environment.getVariable(key));
        }

        // 2. Lấy đường dẫn tệp môi trường của không gian làm việc đích
        String targetEnvPath = SystemUtil.getEnvPathForWorkspace(targetWorkspace);

        // 3. Tạm thời chuyển sang dịch vụ môi trường của không gian làm việc mục tiêu
        String originalDataFilePath = EnvironmentService.getDataFilePath();
        try {
            // Chuyển sang không gian làm việc mục tiêu
            EnvironmentService.setDataFilePath(targetEnvPath);

            // 4. Lưu môi trường vào không gian làm việc mục tiêu
            EnvironmentService.saveEnvironment(copiedEnvironment);

            // 5. Chuyển về không gian làm việc ban đầu và xóa môi trường ban đầu
            EnvironmentService.setDataFilePath(originalDataFilePath);
            EnvironmentService.deleteEnvironment(environment.getId());

            // 6. Làm mới bảng điều khiển hiện tại
            refreshUI();

            // 7. Làm mới hộp thả xuống môi trường trên cùng
            UiSingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();

            log.info("Successfully moved environment '{}' to workspace '{}'",
                    environment.getName(), targetWorkspace.getName());

        } catch (Exception e) {
            // Nếu ngoại lệ xảy ra, hãy đảm bảo khôi phục đường dẫn tệp dữ liệu gốc
            EnvironmentService.setDataFilePath(originalDataFilePath);
            throw new RuntimeException("Không thể chuyển môi trường: " + e.getMessage(), e);
        }
    }

    /**
     * Lọc các hàng trong bảng và hiển thị các hàng đáp ứng tiêu chí dựa trên nội dung của hộp tìm kiếm
     * Phạm vi tìm kiếm:Name Tổng cột Value cột
     */
    private void filterTableRows() {
        String keyword = tableSearchField.getText();
        boolean caseSensitive = tableSearchField.isCaseSensitive();
        boolean wholeWord = tableSearchField.isWholeWord();

        if (keyword == null || keyword.trim().isEmpty()) {
            variablesTablePanel.getTable().setRowSorter(null);
            tableSearchField.setNoResult(false);
            return;
        }

        // sử dụng TableRowSorter để lọc
        TableRowSorter<TableModel> sorter =
                new TableRowSorter<>(variablesTablePanel.getTable().getModel());

        // Từ khóa chuyển đổi được sử dụng cho các tìm kiếm
        final String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        // Tạo một bộ lọc
        RowFilter<TableModel, Object> rowFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ?> entry) {
                Object nameObj = entry.getValue(1);
                Object valueObj = entry.getValue(2);
                String name = nameObj != null ? nameObj.toString() : "";
                String value = valueObj != null ? valueObj.toString() : "";
                String searchName = caseSensitive ? name : name.toLowerCase();
                String searchValue = caseSensitive ? value : value.toLowerCase();
                if (wholeWord) {
                    return matchesWholeWord(searchName, searchKeyword) ||
                            matchesWholeWord(searchValue, searchKeyword);
                } else {
                    return searchName.contains(searchKeyword) ||
                            searchValue.contains(searchKeyword);
                }
            }
        };

        sorter.setRowFilter(rowFilter);
        variablesTablePanel.getTable().setRowSorter(sorter);

        // Hộp tìm kiếm chuyển sang màu đỏ khi không nhìn thấy hàng nào
        boolean noResult = variablesTablePanel.getTable().getRowCount() == 0;
        tableSearchField.setNoResult(noResult);
    }

    /**
     * Xác định xem văn bản có chứa từ khóa khớp toàn bộ từ hay không
     */
    private boolean matchesWholeWord(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }

        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            int start = index;
            int end = index + keyword.length();

            // Kiểm tra ký tự trước đó
            if (start > 0) {
                char prevChar = text.charAt(start - 1);
                if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                    index++;
                    continue;
                }
            }

            // Kiểm tra ký tự tiếp theo
            if (end < text.length()) {
                char nextChar = text.charAt(end);
                if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                    index++;
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Hiển thị hộp thoại chỉnh sửa hàng loạt
     * Được hỗ trợ bởi "Key: Value" Định dạng dán hàng loạt và chỉnh sửa các biến môi trường
     */
    private void showBulkEditDialog() {
        if (currentEnvironment == null) {
            NotificationCenter.showWarning("Please select an environment first");
            return;
        }

        // 1. Chuyển đổi dữ liệu bảng hiện tại sang định dạng văn bản (Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<Variable> currentVariables = variablesTablePanel.getVariableList();
        for (Variable variable : currentVariables) {
            if (!variable.getKey().isEmpty()) {
                text.append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }

        // 2. Tạo vùng soạn thảo văn bản
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        ToolWindowSurfaceStyle.applyTextComponentInput(textArea);
        textArea.setCaretColor(ModernColors.getPrimary());

        // Đặt con trỏ ở cuối văn bản
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        ToolWindowSurfaceStyle.applyDialogInputBorder(scrollPane, false);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 3. Tạo nhãn nhắc nhở - Sử dụng quốc tế hóa, sắp xếp theo chiều dọc
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // Văn bản nhắc nhở chính
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // Hỗ trợ mô tả định dạng
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6)); // Thêm khoảng cách
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 4. Lắp ráp bảng nội dung
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // 5. Tạo một bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        // 6. Tạo hộp thoại tùy chỉnh
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_VARIABLES), Dialog.ModalityType.APPLICATION_MODAL);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Đặt thuộc tính hộp thoại
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        // 7. Xử lý sự kiện nút
        okButton.addActionListener(e -> {
            parseBulkText(textArea.getText());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. Hỗ trợ ESC phím để đóng hộp thoại
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. Đặt nút mặc định
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. Hiển thị hộp thoại
        dialog.setVisible(true);
    }

    /**
     * Phân tích nội dung văn bản chỉnh sửa hàng loạt
     * Các định dạng được hỗ trợ: .
     * - Key: Value
     * - Key:Value
     * - Key = Value
     * - Key=Value
     * Các dòng trống và nhận xét sẽ bị bỏ qua
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // Nếu văn bản trống, hãy xóa tất cả các biến
            variablesTablePanel.setVariableList(new ArrayList<>());
            return;
        }

        List<Variable> variables = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue; // Các dòng trống và nhận xét sẽ bị bỏ qua
            }

            // Hỗ trợ ":" và "=" Hai dấu phân cách
            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            }

            if (parts != null && parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, value));
                }
            } else if (line.contains(":") || line.contains("=")) {
                // Nếu có dấu phân cách nhưng phân tích cú pháp không thành công thì có thể giá trị đó trống
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, ""));
                }
            }
        }

        // Cập nhật dữ liệu bảng
        variablesTablePanel.setVariableList(variables);
    }
}
