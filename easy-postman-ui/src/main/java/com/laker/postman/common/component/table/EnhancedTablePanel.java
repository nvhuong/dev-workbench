package com.laker.postman.common.component.table;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.FallbackAwareRSyntaxTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Thành phần bảng phổ quát nâng cao
 * <ul>
 *   <li>Tự động lấp đầy chiều rộng khung nhìn (kéo dài khi nội dung bị thu hẹp và hiển thị thanh cuộn ngang khi nội dung siêu rộng)</li>
 *   <li>Thanh công cụ trên cùng: Lọc tìm kiếm trực tiếp + Lọc nhiều lựa chọn cột + Tổng số hàng được hiển thị</li>
 *   <li>Phân trang: thả xuống số lượng mục trên mỗi trang 20/50/100/All、Lật trang, nhảy số trang</li>
 *   <li>Nhấp vào tiêu đề cột để sắp xếp (l/xuống, được biểu thị bằng một mũi tên)</li>
 *   <li>Làm nổi bật hàng di chuột + In ngựa vằn</li>
 *   <li>Menu chuột phải: Sao chép ô, sao chép toàn bộ hàng</li>
 *   <li>Hỗ trợ đặt lại động cấu trúc cột khi chạy ({@link #resetAndSetData}）</li>
 * </ul>
 */
@Slf4j
public class EnhancedTablePanel extends JPanel {

    // ── Tên cột (có thể thay đổi, hỗ trợ thiết lập lại động)───────────────────────────────────────────
    private String[] currentColumns;

    // ── dữ liệu ──────────────────────────────────────────────────────────────
    private final List<Object[]> allRows = new ArrayList<>();
    private final List<Object[]> filteredRows = new ArrayList<>();

    // ── Phân trang ──────────────────────────────────────────────────────────────
    private int pageSize = 20;
    private int currentPage = 0;

    // ── Loại ──────────────────────────────────────────────────────────────
    private int sortCol = -1;
    private boolean sortAsc = true;

    // ── lọc ─────────────────────────────────────────────────────────────
    private String filterText = "";
    /**
     * null = Tìm kiếm tất cả các cột; nếu không thì chỉ tìm kiếm trong các chỉ mục cột này
     */
    private Set<Integer> filterCols = null;

    // ── Hover ─────────────────────────────────────────────────────────────
    private int hoveredRow = -1;
    @Setter
    private boolean autoResizeOnRefresh = true;
    @Getter
    @Setter
    private boolean cellDetailDialogOnDoubleClickEnabled = true;
    @Setter
    private Consumer<JPopupMenu> contextMenuCustomizer;
    /**
     * Được gọi sau khi thay đổi dữ liệu trang hiện có. Điều chỉnh bảng chi tiết để đồng bộ hóa lại nội dung dựa trên hàng hiện được chọn.
     */
    @Setter
    private Runnable viewDataChangedListener;
    @Setter
    private BiFunction<Integer, Integer, String> rowCountFormatter;

    /**
     * Khi vượt quá số lượng ký tự này, ô sẽ hiển thị văn bản bị cắt bớt + dấu chấm lửng và kích hoạt tooltip
     */
    private static final int LARGE_CELL_THRESHOLD = 100;
    /**
     * Tooltip Số ký tự tối đa cần hiển thị
     */
    private static final int TOOLTIP_MAX_LEN = 300;

    // ── Tùy chọn kích thước trang ───────────────────────────────────────────────────────
    private static final int[] PAGE_SIZES = {20, 50, 100, 0};
    private static final String[] SIZE_LABELS = {"20", "50", "100", UiI18n.get(UiMessageKeys.TABLE_PAGE_SIZE_ALL)};

    // ── Swing thành phần ────────────────────────────────────────────────────────
    @Getter
    private JTable table;
    private DefaultTableModel tableModel;
    private SearchTextField searchField;
    private JButton colFilterBtn;
    private JLabel hintLabel;
    private JLabel pageInfoLabel;
    private JButton btnPrev;
    private JButton btnNext;

    // ── Tùy chọn tìm kiếm (với  SearchTextField liên kết)────────────────────────────────
    private boolean caseSensitive = false;
    private boolean wholeWord = false;

    // ── Lớp phủ trạng thái trống ─────────────────────────────────────────────────────
    private JLabel emptyLabel;

    // ─────────────────────────────────────────────────────────────────────

    public EnhancedTablePanel(String[] columns) {
        this.currentColumns = columns.clone();
        buildUI();
    }

    // ═══════════════════ Public API ══════════════════════════════════════

    /**
     * Tự động đặt lại cấu trúc cột và thay thế tất cả dữ liệu.
     */
    public void resetAndSetData(String[] newColumns, List<Object[]> rows) {
        boolean colsChanged = !Arrays.equals(currentColumns, newColumns);
        this.currentColumns = newColumns.clone();
        this.sortCol = -1;
        this.currentPage = 0;
        this.filterText = "";
        searchField.setText("");

        if (colsChanged) {
            // Thay đổi cấu trúc cột: Xóa các chỉ mục không hợp lệ vượt quá số lượng cột mới; đặt nếu tất cả các cột được che phủ sau khi xóa null（Chọn tất cả)
            if (filterCols != null && !filterCols.isEmpty()) {
                filterCols.removeIf(ci -> ci >= newColumns.length);
                if (filterCols.size() >= newColumns.length) filterCols = null;
            }
            updateColFilterBtnLabel();
        }

        // Đặt lại tableModel cột
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        for (String col : currentColumns) tableModel.addColumn(col);

        allRows.clear();
        if (rows != null) allRows.addAll(rows);

        updateHintLabel();
        applyFilterAndSort();
        updateSortHeaders();
    }

    /**
     * Giữ nguyên các cột, chỉ thay thế dữ liệu
     */
    public void setData(List<Object[]> rows) {
        allRows.clear();
        if (rows != null) allRows.addAll(rows);
        currentPage = 0;
        sortCol = -1;
        updateHintLabel();
        applyFilterAndSort();
        updateSortHeaders();
    }

    /**
     * Giữ việc sắp xếp, lọc, phân trang hiện tại, chỉ thay thế dữ liệu.
     */
    public void setDataPreserveView(List<Object[]> rows) {
        allRows.clear();
        if (rows != null) allRows.addAll(rows);
        updateHintLabel();
        applyFilterAndSort();
    }

    /**
     * Lấy dữ liệu hàng trong trang xem hiện tại.EnhancedTablePanel Sử dụng phân trang riêng/lọc/Loại,
     * JTable của convertRowIndexToModel Không thể ánh xạ lại allRows。
     */
    public Object[] getVisibleRowData(int viewRow) {
        if (viewRow < 0 || table == null || viewRow >= table.getRowCount()) {
            return null;
        }
        int dataIndex = getPageOffset() + viewRow;
        if (dataIndex < 0 || dataIndex >= filteredRows.size()) {
            return null;
        }
        Object[] row = filteredRows.get(dataIndex);
        return row == null ? null : row.clone();
    }

    public Object[] getSelectedRowData() {
        return table == null ? null : getVisibleRowData(table.getSelectedRow());
    }

    public boolean hasActiveSort() {
        return sortCol >= 0 && sortCol < currentColumns.length;
    }

    public void setSort(int columnIndex, boolean ascending) {
        if (columnIndex < 0 || columnIndex >= currentColumns.length) {
            clearSort();
            return;
        }
        sortCol = columnIndex;
        sortAsc = ascending;
        currentPage = 0;
        applyFilterAndSort();
        updateSortHeaders();
    }

    public void clearSort() {
        sortCol = -1;
        sortAsc = true;
        currentPage = 0;
        applyFilterAndSort();
        updateSortHeaders();
    }

    /**
     * Xóa dữ liệu (giữ cấu trúc cột)
     */
    public void clearData() {
        allRows.clear();
        filteredRows.clear();
        currentPage = 0;
        sortCol = -1;
        updateHintLabel();
        tableModel.setRowCount(0);
        updateSortHeaders();
        updatePaginationControls();
        updateEmptyState();
        notifyViewDataChanged();
    }

    /**
     * Trả về tổng số hàng dữ liệu (không được phân trang)/hiệu ứng lọc)
     */
    public int getTotalRowCount() {
        return allRows.size();
    }

    /**
     * Tăng dần thêm một hàng dữ liệu (không cần sắp xếp lại/Được lọc lại, hiệu suất tốt hơn âm lượng đầy đủ setData）。
     * Nếu hiện có các điều kiện lọc hoặc sắp xếp, hãy quay lại làm mới hoàn toàn để đảm bảo chế độ xem chính xác.
     */
    public void addRow(Object[] row) {
        if (row == null) return;
        allRows.add(row);
        updateHintLabel();
        if ((filterText != null && !filterText.isBlank()) || hasActiveSort()) {
            // Có lọc/Khi sắp xếp các điều kiện, hãy làm mới toàn bộ để đảm bảo các hàng mới vào đúng vị trí
            applyFilterAndSort();
            return;
        }
        filteredRows.add(row);
        // Nếu bạn hiện đang ở trang cuối cùng (hoặc All），Nối trực tiếp vào tableModel
        boolean isAll = (pageSize == 0);
        int totalFiltered = filteredRows.size();
        int totalPages = isAll ? 1 : (int) Math.ceil((double) totalFiltered / pageSize);
        boolean onLastPage = (currentPage >= totalPages - 1);
        if ((isAll || onLastPage) && (isAll || tableModel.getRowCount() < pageSize)) {
            tableModel.addRow(row);
        }
        updatePaginationControls();
        updateEmptyState();
        notifyViewDataChanged();
    }

    // ═══════════════════ UI Build ════════════════════════════════════════

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder());
        ToolWindowSurfaceStyle.applyCard(this);
        add(buildToolBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildPagination(), BorderLayout.SOUTH);
    }

    // ── Thanh công cụ: Hộp tìm kiếm + Nút lọc cột + Gợi ý số dòng ────────────────────────────
    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new BorderLayout(4, 0));
        ToolWindowSurfaceStyle.applyCard(bar);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        searchField = new SearchTextField();
        // Đã đặt trước SearchTextField Vỏ tích hợp/Nút khớp toàn bộ từ (không ghi đè trailing component）
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                UiI18n.get(UiMessageKeys.TABLE_SEARCH_PLACEHOLDER_ALL));
        searchField.setPreferredSize(new Dimension(220, 28));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onSearch();
            }

            public void removeUpdate(DocumentEvent e) {
                onSearch();
            }

            public void changedUpdate(DocumentEvent e) {
                onSearch();
            }
        });

        // Trường hợp giám sát / Chuyển đổi toàn bộ từ → Lọc lại
        searchField.addPropertyChangeListener("caseSensitive", e -> {
            caseSensitive = searchField.isCaseSensitive();
            if (!filterText.isEmpty()) {
                currentPage = 0;
                applyFilterAndSort();
            }
        });
        searchField.addPropertyChangeListener("wholeWord", e -> {
            wholeWord = searchField.isWholeWord();
            if (!filterText.isEmpty()) {
                currentPage = 0;
                applyFilterAndSort();
            }
        });

        // Escape Xóa hộp tìm kiếm
        searchField.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!searchField.getText().isEmpty()) searchField.setText("");
            }
        });

        colFilterBtn = new JButton(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_BTN));
        colFilterBtn.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        colFilterBtn.setFocusPainted(false);
        colFilterBtn.setToolTipText(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_TOOLTIP));
        colFilterBtn.addActionListener(e -> showColFilterPopup());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(searchField);
        left.add(colFilterBtn);

        hintLabel = new JLabel("0" + UiI18n.get(UiMessageKeys.TABLE_ROWS_SUFFIX));
        hintLabel.setForeground(ModernColors.getTextSecondary());
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        bar.add(left, BorderLayout.WEST);
        bar.add(hintLabel, BorderLayout.EAST);
        return bar;
    }

    // ── Bộ lọc cột popup（Kiên trì để tránh mất trạng thái do mỗi lần xây dựng lại)──────────────────────
    private JPopupMenu colFilterPopup = null;
    private JCheckBox[] colFilterBoxes = null;
    private String[] colFilterPopupColumns = null;

    private void showColFilterPopup() {
        if (currentColumns.length == 0) return;

        if (colFilterPopup == null || !Arrays.equals(colFilterPopupColumns, currentColumns)) {
            colFilterPopup = buildColFilterPopup();
            colFilterPopupColumns = currentColumns.clone();
        } else {
            syncCheckboxesToFilterCols();
        }

        colFilterPopup.show(colFilterBtn, 0, colFilterBtn.getHeight());
    }

    private JPopupMenu buildColFilterPopup() {
        JPopupMenu popup = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(popup);
        popup.setLayout(new BorderLayout(0, 0));

        // ── thanh tiêu đề ──────────────────────────────────────────────────────
        JLabel titleLabel = new JLabel(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        popup.add(titleLabel, BorderLayout.NORTH);

        // ── Danh sách hộp kiểm ───────────────────────────────────────────────────
        JPanel checkPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        checkPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        checkPanel.setOpaque(false);
        colFilterBoxes = new JCheckBox[currentColumns.length];
        for (int i = 0; i < currentColumns.length; i++) {
            boolean checked = (filterCols == null || filterCols.contains(i));
            colFilterBoxes[i] = new JCheckBox(currentColumns[i], checked);
            colFilterBoxes[i].setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            colFilterBoxes[i].setOpaque(false);
            colFilterBoxes[i].setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            checkPanel.add(colFilterBoxes[i]);
        }
        JScrollPane checkScroll = new JScrollPane(checkPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        checkScroll.setBorder(BorderFactory.createEmptyBorder());
        checkScroll.setPreferredSize(new Dimension(200, Math.min(currentColumns.length * 28 + 8, 250)));
        popup.add(checkScroll, BorderLayout.CENTER);

        // ── Thanh hành động phía dưới ───────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyCard(footer);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkRow.setOpaque(false);
        JButton btnAll = makeLinkBtn(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_SELECT_ALL));
        JLabel sep = new JLabel(" | ");
        sep.setForeground(ModernColors.getTextSecondary());
        sep.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        JButton btnNone = makeLinkBtn(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_DESELECT_ALL));
        linkRow.add(btnAll);
        linkRow.add(sep);
        linkRow.add(btnNone);

        JButton btnOk = new JButton(CommonI18n.get(CommonMessageKeys.GENERAL_OK));
        btnOk.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        btnOk.setFocusPainted(false);

        footer.add(linkRow, BorderLayout.WEST);
        footer.add(btnOk, BorderLayout.EAST);
        popup.add(footer, BorderLayout.SOUTH);

        btnAll.addActionListener(e -> {
            for (JCheckBox cb : colFilterBoxes) cb.setSelected(true);
        });
        btnNone.addActionListener(e -> {
            for (JCheckBox cb : colFilterBoxes) cb.setSelected(false);
        });
        btnOk.addActionListener(e -> applyColFilter(popup));

        return popup;
    }

    /**
     * Các nút bấm kiểu liên kết nhỏ (không viền, không nền, sử dụng font chữ gọn)
     */
    private static JButton makeLinkBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        return b;
    }

    private void syncCheckboxesToFilterCols() {
        if (colFilterBoxes == null) return;
        for (int i = 0; i < colFilterBoxes.length; i++) {
            colFilterBoxes[i].setSelected(filterCols == null || filterCols.contains(i));
        }
    }

    private void applyColFilter(JPopupMenu popup) {
        if (colFilterBoxes == null) return;
        Set<Integer> sel = new HashSet<>();
        boolean allChecked = true;
        for (int i = 0; i < colFilterBoxes.length; i++) {
            if (colFilterBoxes[i].isSelected()) sel.add(i);
            else allChecked = false;
        }
        filterCols = allChecked ? null : sel;
        updateColFilterBtnLabel();
        currentPage = 0;
        applyFilterAndSort();
        popup.setVisible(false);
    }

    /**
     * Cập nhật văn bản nút lọc cột + Hộp tìm kiếm placeholder
     */
    private void updateColFilterBtnLabel() {
        if (filterCols == null) {
            // Mặc định: Tìm kiếm tất cả các cột
            colFilterBtn.setText(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_BTN));
            colFilterBtn.setForeground(null);
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    UiI18n.get(UiMessageKeys.TABLE_SEARCH_PLACEHOLDER_ALL));
        } else if (filterCols.isEmpty()) {
            // Bỏ chọn tất cả: chuyển sang màu xám để nhắc người dùng rằng không có phạm vi nào được chỉ định
            colFilterBtn.setText(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_BTN_N, "0"));
            colFilterBtn.setForeground(ModernColors.getTextSecondary());
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    UiI18n.get(UiMessageKeys.TABLE_SEARCH_PLACEHOLDER_NONE));
        } else {
            // Các cột đã chọn: Đánh dấu số lượng.placeholder Hiển thị tên cột
            colFilterBtn.setText(UiI18n.get(UiMessageKeys.TABLE_COL_FILTER_BTN_N,
                    String.valueOf(filterCols.size())));
            colFilterBtn.setForeground(accentColor());
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    UiI18n.get(UiMessageKeys.TABLE_SEARCH_PLACEHOLDER_COLS,
                            buildFilterColNames()));
        }
        searchField.repaint();
    }

    private String buildFilterColNames() {
        List<String> names = new ArrayList<>();
        for (int ci : filterCols) {
            if (ci < currentColumns.length) {
                names.add(currentColumns[ci]);
                if (names.size() >= 3) break;
            }
        }
        String joined = String.join(", ", names);
        return (names.size() >= 3 && filterCols.size() > 3) ? joined + "..." : joined;
    }

    // ── cái bàn ─────────────────────────────────────────────────────────────
    private JComponent buildTableArea() {
        initTableModel();
        initTableWidget();
        JScrollPane scroll = buildTableScroll();
        emptyLabel = buildEmptyLabel();
        return buildLayeredPane(scroll, emptyLabel);
    }

    private void initTableModel() {
        tableModel = new DefaultTableModel(currentColumns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private void initTableWidget() {
        table = new JTable(tableModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        table.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, new HoverRenderer());
        ToolWindowSurfaceStyle.applyTableCard(table);
        table.getTableHeader().addMouseListener(buildHeaderMouseListener());
        table.addMouseMotionListener(buildTableMotionListener());
        table.addMouseListener(buildTableMouseListener());
        // Bấm đúp vào ô → Nội dung lớn hiện lên hộp thoại chi tiết
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (cellDetailDialogOnDoubleClickEnabled
                        && e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        Object val = table.getValueAt(row, col);
                        String text = val == null ? "" : val.toString();
                        if (text.length() > LARGE_CELL_THRESHOLD) {
                            String colName = table.getColumnName(col);
                            showCellDetailDialog(colName, text);
                        }
                    }
                }
            }
        });
    }

    private JScrollPane buildTableScroll() {
        JScrollPane scroll = new JScrollPane(table);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scroll, table);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JLabel buildEmptyLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextSecondary());
        label.setOpaque(false);
        label.setVisible(false);
        return label;
    }

    private static JLayeredPane buildLayeredPane(JScrollPane scroll, JLabel overlay) {
        JLayeredPane layered = new JLayeredPane() {
            @Override
            public void doLayout() {
                for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        ToolWindowSurfaceStyle.applyCard(layered);
        layered.add(scroll, JLayeredPane.DEFAULT_LAYER);
        layered.add(overlay, JLayeredPane.PALETTE_LAYER);
        return layered;
    }

    private MouseAdapter buildHeaderMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col < 0) return;
                if (e.getClickCount() == 2 && isOnColumnResizeBorder(e)) {
                    autoResizeSingleColumn(col);
                    return;
                }
                if (e.getClickCount() == 1) {
                    if (sortCol == col) sortAsc = !sortAsc;
                    else {
                        sortCol = col;
                        sortAsc = true;
                    }
                    applyFilterAndSort();
                    updateSortHeaders();
                }
            }
        };
    }

    private MouseMotionAdapter buildTableMotionListener() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    table.repaint();
                }
            }
        };
    }

    private MouseAdapter buildTableMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredRow != -1) {
                    hoveredRow = -1;
                    table.repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                    showContextMenu(e);
                }
            }
        };
    }

    // ── Thanh phân trang ────────────────────────────────────────────────────────────
    private JPanel buildPagination() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
        ToolWindowSurfaceStyle.applyCard(bar);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        JComboBox<String> pageSizeCombo = new JComboBox<>(SIZE_LABELS);
        pageSizeCombo.setPreferredSize(new Dimension(65, 24));
        pageSizeCombo.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        pageSizeCombo.addActionListener(e -> {
            pageSize = PAGE_SIZES[pageSizeCombo.getSelectedIndex()];
            currentPage = 0;
            refreshView();
        });

        btnPrev = navBtn("‹");
        btnNext = navBtn("›");
        btnPrev.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshView();
            }
        });
        btnNext.addActionListener(e -> {
            if (currentPage < totalPages() - 1) {
                currentPage++;
                refreshView();
            }
        });

        pageInfoLabel = new JLabel("0 / 0");
        pageInfoLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        pageInfoLabel.setPreferredSize(new Dimension(120, 20));
        pageInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField pageJumpField = new JTextField(3);
        pageJumpField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        pageJumpField.setHorizontalAlignment(SwingConstants.CENTER);
        pageJumpField.setPreferredSize(new Dimension(40, 24));
        pageJumpField.addActionListener(e -> {
            try {
                int pg = Integer.parseInt(pageJumpField.getText().trim()) - 1;
                if (pg >= 0 && pg < totalPages()) {
                    currentPage = pg;
                    refreshView();
                }
            } catch (NumberFormatException ex) {
                log.debug("invalid page jump input");
            }
            pageJumpField.setText("");
        });

        bar.add(tiny(UiI18n.get(UiMessageKeys.TABLE_PAGE_SIZE_LABEL)));
        bar.add(pageSizeCombo);
        bar.add(btnPrev);
        bar.add(pageInfoLabel);
        bar.add(btnNext);
        bar.add(tiny(UiI18n.get(UiMessageKeys.TABLE_PAGE_JUMP_LABEL)));
        bar.add(pageJumpField);
        return bar;
    }

    // ═══════════════════ logic cốt lõi ════════════════════════════════════════

    private void onSearch() {
        filterText = searchField.getText().trim();
        if (!caseSensitive) filterText = filterText.toLowerCase();
        currentPage = 0;
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        filteredRows.clear();
        for (Object[] row : allRows) {
            if (matches(row)) filteredRows.add(row);
        }
        searchField.setNoResult(!filterText.isEmpty() && filteredRows.isEmpty());

        if (sortCol >= 0 && sortCol < tableModel.getColumnCount()) {
            final int col = sortCol;
            final boolean asc = sortAsc;
            filteredRows.sort((x, y) -> cmp(
                    col < x.length ? x[col] : null,
                    col < y.length ? y[col] : null, asc));
        }

        updateHintLabel();
        refreshView();
    }

    private void refreshView() {
        tableModel.setRowCount(0);
        int total = filteredRows.size();
        int pages = totalPages();
        if (currentPage >= pages) currentPage = Math.max(0, pages - 1);

        int from = getPageOffset();
        int to = (pageSize == 0) ? total : Math.min(from + pageSize, total);
        for (int i = from; i < to; i++) {
            tableModel.addRow(filteredRows.get(i));
        }

        updatePaginationControls();
        updateEmptyState();
        if (autoResizeOnRefresh) {
            SwingUtilities.invokeLater(this::autoResizeColumns);
        }
        notifyViewDataChanged();
    }

    private void notifyViewDataChanged() {
        if (viewDataChangedListener != null) {
            SwingUtilities.invokeLater(viewDataChangedListener);
        }
    }

    private void updatePaginationControls() {
        int total = filteredRows.size();
        int pages = totalPages();
        String info;
        if (total == 0) {
            info = UiI18n.get(UiMessageKeys.TABLE_PAGE_INFO_EMPTY);
        } else {
            int from = getPageOffset() + 1;
            int to = (pageSize == 0) ? total : Math.min(getPageOffset() + pageSize, total);
            info = UiI18n.get(UiMessageKeys.TABLE_PAGE_INFO,
                    String.valueOf(from),
                    String.valueOf(to),
                    String.valueOf(currentPage + 1),
                    String.valueOf(pages));
        }
        pageInfoLabel.setText(info);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < pages - 1);
    }

    /**
     * Cập nhật lời nhắc số hàng trên cùng (hiển thị tỷ lệ bộ lọc khi lọc)
     */
    private void updateHintLabel() {
        int total = allRows.size();
        int filtered = filteredRows.size();
        if (rowCountFormatter != null) {
            String customText = rowCountFormatter.apply(filtered, total);
            if (customText != null && !customText.isBlank()) {
                hintLabel.setText(customText);
                return;
            }
        }
        String suffix = UiI18n.get(UiMessageKeys.TABLE_ROWS_SUFFIX);
        if (!filterText.isEmpty() && filtered != total) {
            hintLabel.setText(UiI18n.get(UiMessageKeys.TABLE_ROWS_FILTERED,
                    String.valueOf(filtered), String.valueOf(total)));
        } else {
            hintLabel.setText(total + suffix);
        }
    }

    /**
     * Cập nhật văn bản và khả năng hiển thị của lớp phủ trạng thái trống
     */
    private void updateEmptyState() {
        boolean hasData = !filteredRows.isEmpty();
        boolean hasFilter = !filterText.isEmpty();
        if (hasData) {
            emptyLabel.setVisible(false);
        } else if (hasFilter) {
            emptyLabel.setText(UiI18n.get(UiMessageKeys.TABLE_EMPTY_NO_MATCH));
            emptyLabel.setVisible(true);
        } else if (allRows.isEmpty()) {
            emptyLabel.setText(UiI18n.get(UiMessageKeys.TABLE_EMPTY_NO_DATA));
            emptyLabel.setVisible(true);
        } else {
            emptyLabel.setVisible(false);
        }
    }

    // ...existing code...

    private void showContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();

        // "Xem nội dung ô" Chỉ hiển thị nếu nội dung ô vượt quá ngưỡng
        if (r >= 0 && c >= 0) {
            Object val = table.getValueAt(r, c);
            String text = val == null ? "" : val.toString();
            if (text.length() > LARGE_CELL_THRESHOLD) {
                JMenuItem viewCell = new JMenuItem(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_VIEW_CELL));
                viewCell.addActionListener(ev -> showCellDetailDialog(table.getColumnName(c), text));
                menu.add(viewCell);
                menu.addSeparator();
            }
        }

        JMenuItem copyCell = new JMenuItem(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_COPY_CELL));
        copyCell.addActionListener(ev -> {
            copySelectedCell();
            NotificationCenter.showSuccess(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_COPIED));
        });
        JMenuItem copyRow = new JMenuItem(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_COPY_ROW));
        copyRow.addActionListener(ev -> {
            copySelectedRow();
            NotificationCenter.showSuccess(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_COPIED));
        });
        menu.add(copyCell);
        menu.add(copyRow);
        if (contextMenuCustomizer != null) {
            contextMenuCustomizer.accept(menu);
        }
        menu.show(table, e.getX(), e.getY());
    }

    private void copySelectedCell() {
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();
        if (r >= 0 && c >= 0) {
            Object v = table.getValueAt(r, c);
            clip(v == null ? "" : v.toString());
        }
    }

    private void copySelectedRow() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < table.getColumnCount(); c++) {
            if (c > 0) sb.append('\t');
            Object v = table.getValueAt(r, c);
            if (v != null) sb.append(v);
        }
        clip(sb.toString());
    }

    // ═══════════════════ Phương pháp công cụ ════════════════════════════════════════

    private int totalPages() {
        if (pageSize == 0) return 1;
        int n = filteredRows.size();
        return n == 0 ? 1 : (int) Math.ceil((double) n / pageSize);
    }

    private int getPageOffset() {
        return pageSize == 0 ? 0 : currentPage * pageSize;
    }

    private boolean matches(Object[] row) {
        if (filterText.isEmpty()) return true;
        if (filterCols == null) return matchesAnyCell(row);
        return matchesSelectedCols(row);
    }

    private boolean matchesAnyCell(Object[] row) {
        for (Object cell : row) {
            if (cellContains(cell)) return true;
        }
        return false;
    }

    private boolean matchesSelectedCols(Object[] row) {
        for (int ci : filterCols) {
            if (ci < row.length && cellContains(row[ci])) return true;
        }
        return false;
    }

    private boolean cellContains(Object cell) {
        if (cell == null) return false;
        String text = caseSensitive ? cell.toString() : cell.toString().toLowerCase();
        if (wholeWord) {
            for (String word : filterText.split("\\s+")) {
                if (text.startsWith(word) || text.endsWith(word) ||
                        text.contains(" " + word) || text.contains(word + " ")) {
                    return true;
                }
            }
            return false;
        }
        return text.contains(filterText);
    }

    /**
     * Thêm mũi tên sắp xếp vào tiêu đề cột
     */
    private void updateSortHeaders() {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn tc = cm.getColumn(i);
            String name = tableModel.getColumnName(i);
            if (i == sortCol) {
                tc.setHeaderValue(name + (sortAsc ? " ▲" : " ▼"));
            } else {
                tc.setHeaderValue(name);
            }
        }
        table.getTableHeader().repaint();
    }

    /**
     * Tự động vừa với tất cả độ rộng của cột
     */
    private void autoResizeColumns() {
        if (table.getColumnModel().getColumnCount() == 0) return;
        for (int col = 0; col < table.getColumnModel().getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setPreferredWidth(calcColWidth(col));
        }
    }

    /**
     * Tính chiều rộng thích hợp của một cột (quét tối đa 100 Được rồi, giới hạn trên 320px）
     */
    private int calcColWidth(int col) {
        TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        Component hc = hr.getTableCellRendererComponent(
                table, table.getColumnName(col), false, false, -1, col);
        int w = hc.getPreferredSize().width + 20;
        int rowCount = Math.min(table.getRowCount(), 100);
        for (int row = 0; row < rowCount; row++) {
            Component cc = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
            w = Math.max(w, cc.getPreferredSize().width + 16);
        }
        return Math.min(w, 320);
    }

    /**
     * Điều chỉnh độ rộng cột khi bạn bấm đúp vào dấu phân cách cột
     */
    private void autoResizeSingleColumn(int col) {
        int count = table.getColumnModel().getColumnCount();
        if (col >= 0 && col < count) {
            table.getColumnModel().getColumn(col).setPreferredWidth(calcColWidth(col));
        }
    }

    /**
     * Xác định xem chuột có nằm trên vạch phân cách cột hay không (khoảng cách từ vạch phân cách 3px bên trong)
     */
    private boolean isOnColumnResizeBorder(MouseEvent e) {
        int x = e.getX();
        TableColumnModel cm = table.getColumnModel();
        int cumWidth = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cumWidth += cm.getColumn(i).getWidth();
            if (Math.abs(x - cumWidth) <= 3) return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int cmp(Object a, Object b, boolean asc) {
        if (a == null && b == null) return 0;
        if (a == null) return asc ? -1 : 1;
        if (b == null) return asc ? 1 : -1;
        int r;
        if (a instanceof Number na && b instanceof Number nb) {
            r = Double.compare(na.doubleValue(), nb.doubleValue());
        } else if (a instanceof Comparable ca) {
            try {
                r = ca.compareTo(b);
            } catch (ClassCastException ex) {
                r = a.toString().compareToIgnoreCase(b.toString());
            }
        } else {
            r = a.toString().compareToIgnoreCase(b.toString());
        }
        return asc ? r : -r;
    }

    private static void clip(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(s), null);
    }

    private static JButton navBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 1));
        b.setPreferredSize(new Dimension(28, 24));
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 2, 0, 2));
        return b;
    }

    private static JLabel tiny(String t) {
        JLabel l = new JLabel(t);
        l.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        return l;
    }

    // ═══════════════════ Hộp thoại chi tiết ô ═════════════════════════════════

    /**
     * Xuất hiện hộp thoại chi tiết ô (hỗ trợ tô sáng cú pháp,JSON Định dạng,Cmd/Ctrl+F tìm kiếm, sao chép)
     */
    private void showCellDetailDialog(String colName, String text) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = (owner instanceof Frame f)
                ? new JDialog(f, UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_TITLE) + " — " + colName, true)
                : new JDialog((Dialog) owner, UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_TITLE) + " — " + colName, true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface((JPanel) dialog.getContentPane());

        // ── RSyntaxTextArea ─────────────────────────────────────────────
        RSyntaxTextArea textArea = new FallbackAwareRSyntaxTextArea();
        textArea.setEditable(false);
        textArea.setCodeFoldingEnabled(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setHighlightCurrentLine(false);
        EditorThemeUtil.loadTheme(textArea);

        // Xác định nếu có JSON
        boolean isJson = JsonUtil.isTypeJSON(text);
        textArea.setSyntaxEditingStyle(isJson
                ? SyntaxConstants.SYNTAX_STYLE_JSON
                : SyntaxConstants.SYNTAX_STYLE_NONE);

        // ── SearchableTextArea Gói (chỉ tìm kiếm, thay thế bị vô hiệu hóa)──────────────────
        SearchableTextArea searchableTextArea = new SearchableTextArea(textArea, false);

        // ── Thanh thông tin hàng đầu ──────────────────────────────────────────────────
        JLabel lenLabel = new JLabel(UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_LENGTH,
                String.valueOf(text.length())));
        lenLabel.setForeground(ModernColors.getTextSecondary());
        lenLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ToolWindowSurfaceStyle.applyDialogHeader(lenLabel, 4, 8, 4, 8);

        // ── Thanh nút dưới cùng ──────────────────────────────────────────────────
        JButton btnFormatJson = ModernButtonFactory.createButton(UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON), false);
        JButton btnCopy = ModernButtonFactory.createButton(CommonI18n.get(CommonMessageKeys.BUTTON_COPY), false);
        JButton btnClose = ModernButtonFactory.createButton(CommonI18n.get(CommonMessageKeys.BUTTON_CLOSE), true);

        btnFormatJson.setEnabled(isJson);
        if (!isJson) btnFormatJson.setToolTipText(UiI18n.get(UiMessageKeys.TABLE_INVALID_JSON));

        // Định dạng / Khôi phục JSON chuyển đổi
        final boolean[] formatted = {false};
        btnFormatJson.addActionListener(ev -> {
            if (!formatted[0]) {
                try {
                    String pretty = JsonUtil.toJsonPrettyStr(text);
                    textArea.setText(pretty);
                    textArea.setCaretPosition(0);
                    formatted[0] = true;
                    btnFormatJson.setText("⟲ " + UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
                } catch (Exception ex) {
                    log.warn("JSON format failed", ex);
                }
            } else {
                textArea.setText(text);
                textArea.setCaretPosition(0);
                formatted[0] = false;
                btnFormatJson.setText(UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
            }
        });

        btnCopy.addActionListener(ev -> {
            String selected = textArea.getSelectedText();
            clip(selected != null && !selected.isEmpty() ? selected : textArea.getText());
            NotificationCenter.showSuccess(UiI18n.get(UiMessageKeys.TABLE_CONTEXT_COPIED));
        });
        btnClose.addActionListener(ev -> dialog.dispose());

        // JSON Tự động định dạng màn hình
        if (isJson) {
            try {
                String pretty = JsonUtil.toJsonPrettyStr(text);
                textArea.setText(pretty);
                textArea.setCaretPosition(0);
                formatted[0] = true;
                btnFormatJson.setText("⟲ " + UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
            } catch (Exception ex) {
                log.warn("auto JSON format failed", ex);
                textArea.setText(text);
                textArea.setCaretPosition(0);
            }
        } else {
            textArea.setText(text);
            textArea.setCaretPosition(0);
        }

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        ToolWindowSurfaceStyle.applyDialogFooter(footer);
        footer.add(btnFormatJson);
        footer.add(btnCopy);
        footer.add(btnClose);

        dialog.add(lenLabel, BorderLayout.NORTH);
        dialog.add(searchableTextArea, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        // Escape Đóng lại
        dialog.getRootPane().registerKeyboardAction(
                ev -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setSize(720, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static Color accentColor() {
        Color accent = UIManager.getColor("Component.accentColor");
        return accent != null ? accent : ModernColors.getPrimary();
    }

    private static Color tableBackgroundColor() {
        return ModernColors.getCardBackgroundColor();
    }

    private static Color tableHoverBackgroundColor() {
        Color hover = UIManager.getColor("Table.hoverBackground");
        return hover != null ? hover : ModernColors.getHoverBackgroundColor();
    }

    private static Color tableAlternateRowColor() {
        Color alternate = UIManager.getColor("Table.alternateRowColor");
        return alternate != null ? alternate : tableBackgroundColor();
    }

    private static String toCssColor(Color color) {
        Color resolved = color != null ? color : ModernColors.getTextSecondary();
        return String.format("#%02x%02x%02x", resolved.getRed(), resolved.getGreen(), resolved.getBlue());
    }

    // ═══════════════════ Renderer ═════════════════════════════════════════

    private class HoverRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            // Cắt bớt nội dung lớn để hiển thị
            String display = value == null ? "" : value.toString();
            String tooltip = null;
            if (display.length() > LARGE_CELL_THRESHOLD) {
                tooltip = "<html><body style='width:380px;font-family:monospace;font-size:11px;white-space:pre-wrap'>"
                        + escapeHtml(display.length() > TOOLTIP_MAX_LEN
                        ? display.substring(0, TOOLTIP_MAX_LEN) + "…" : display)
                        + "<br><br><i style='color:" + toCssColor(ModernColors.getTextSecondary()) + "'>"
                        + UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_LENGTH, String.valueOf(display.length()))
                        + " — " + UiI18n.get(UiMessageKeys.TABLE_CELL_DETAIL_DOUBLE_CLICK) + "</i></body></html>";
                display = display.substring(0, LARGE_CELL_THRESHOLD) + " …";
            }
            super.getTableCellRendererComponent(t, display, sel, focus, row, col);
            setToolTipText(tooltip);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            if (!sel) {
                if (row == hoveredRow) {
                    setBackground(tableHoverBackgroundColor());
                } else {
                    setBackground(row % 2 == 0 ? tableBackgroundColor() : tableAlternateRowColor());
                }
            }
            return this;
        }
    }

    /**
     * bỏ trốn HTML Ký tự đặc biệt, được sử dụng cho tooltip
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>")
                .replace(" ", "&nbsp;");
    }
}
