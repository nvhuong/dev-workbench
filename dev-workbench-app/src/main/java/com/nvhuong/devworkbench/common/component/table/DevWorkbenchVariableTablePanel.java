package com.nvhuong.devworkbench.common.component.table;

import com.nvhuong.devworkbench.model.Variable;
import com.nvhuong.devworkbench.util.FontsUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * DevWorkbench Bảng bảng biến môi trường
 * Được thiết kế đặc biệt để xử lý DevWorkbench Dữ liệu biến môi trường
 * Hỗ trợ Drag+Enable、Key、Value và Delete cấu trúc cột
 */
@Slf4j
public class DevWorkbenchVariableTablePanel extends AbstractTablePanel<Variable> {

    // Column indices - Hình thức này là đặc biệt Drag+Enable Hợp nhất các cột
    private static final int COL_DRAG_ENABLE = 0;  // Kết hợp tay cầm kéo và hộp kiểm được bật
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    @Getter
    private boolean isDragging = false;


    /**
     * Trình xây dựng, tạo bảng bảng biến môi trường mặc định
     */
    public DevWorkbenchVariableTablePanel() {
        this("Name", "Value", true, true);
    }

    /**
     * Trình xây dựng, hỗ trợ cấu hình tùy chỉnh
     *
     * @param nameCol              KeyTên cột
     * @param valueCol             ValueTên cột
     * @param popupMenuEnabled     Có bật menu chuột phải hay không
     * @param autoAppendRowEnabled Có bật tự động điền dòng trống hay không
     */
    public DevWorkbenchVariableTablePanel(String nameCol, String valueCol, boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        super(new String[]{"", nameCol, valueCol, ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        if (popupMenuEnabled) {
            setupTableListeners();
        }
        if (autoAppendRowEnabled) {
            addAutoAppendRowFeature();
        }

        enableRowDragAndDrop();

        // Add initial empty row
        addRow();
    }

    // ========== Thực hiện các phương pháp trừu tượng ==========

    @Override
    protected int getEnabledColumnIndex() {
        return COL_DRAG_ENABLE;  // Drag+Enable Hợp nhất các cột
    }

    @Override
    protected int getDeleteColumnIndex() {
        return COL_DELETE;
    }

    @Override
    protected int getFirstEditableColumnIndex() {
        return COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        return column == COL_KEY || column == COL_VALUE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    // ========== Chức năng kéo và thả ==========

    /**
     * Kích hoạtJTableSắp xếp kéo hàng
     * Lưu ý: Không được sử dụng setDragEnabled(true)，Thay vào đó, thao tác kéo chỉ được kích hoạt thủ công trong khu vực tay cầm kéo.
     * Tránh bức tranh lớn dragEnabled Can thiệp vào hành vi nhấp để chỉnh sửa.
     */
    private void enableRowDragAndDrop() {
        // Không được gọi table.setDragEnabled(true)，ngăn chặn Swing của DragGestureRecognizer
        // Đánh chặn mousePressed sự kiện, khiến nhấp chuột để chỉnh sửa không thành công.
        // Kéo và thả qua addDragHandleMouseListener() trong exportAsDrag Kích hoạt thủ công.
        table.setDropMode(DropMode.INSERT_ROWS);

        // Truyền chức năng gọi lại để kiểm soát trạng thái chỉnh sửa và điền dòng trống tự động trong khi kéo
        table.setTransferHandler(new ImprovedTableRowTransferHandler(tableModel, dragging -> {
            isDragging = dragging;
            log.debug("Drag state changed: {}", dragging);
        }));
    }

    @Override
    protected boolean isCellEditable(int row, int column) {
        // Chỉnh sửa bị vô hiệu hóa trong khi kéo
        if (isDragging) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    @Override
    protected void initializeTableUI() {
        // Gọi hàm chung của lớp chaUIcấu hình
        super.initializeTableUI();

        // cài đặt Drag+Enable Kết hợp độ rộng của các cột (tăng chiều rộng để mang lại hiệu ứng hình ảnh tốt hơn)
        setEnabledColumnWidth(50);
        setDeleteColumnWidth();

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    private void setupCellRenderersAndEditors() {
        // Set custom renderer and editor for drag/enable column
        setColumnRenderer(COL_DRAG_ENABLE, new DragEnableRenderer());
        setColumnEditor(COL_DRAG_ENABLE, new DragEnableEditor());

        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new DevWorkbenchTextFieldCellEditor());
        // vì Value Các cột sử dụng trình chỉnh sửa thông minh tự động chọn các hàng riêng lẻ dựa trên độ dài nội dung/Chỉnh sửa nhiều dòng
        setColumnEditor(COL_VALUE, new DevWorkbenchSmartValueCellEditor());
        setColumnRenderer(COL_KEY, new DevWorkbenchTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new DevWorkbenchTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    /**
     * Custom renderer for drag/enable column that combines drag handle and checkbox
     */
    private class DragEnableRenderer extends JPanel implements TableCellRenderer {
        private final JLabel dragLabel;
        private final JCheckBox checkBox;

        public DragEnableRenderer() {
            // sử dụng FlowLayout Làm cho thanh điều khiển kéo và hộp kiểm nhỏ gọn hơn
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0)); // Khoảng cách ngang được đặt thành  2，Khoảng cách dọc là  0
            setOpaque(true);

            // Drag handle label - Biểu tượng kéo đậm
            dragLabel = new JLabel("⋮⋮");
            dragLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dragLabel.setPreferredSize(new Dimension(16, 28)); // Chiều rộng từ 20 giảm xuống 16，Nhỏ gọn hơn
            dragLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(false);

            add(dragLabel);
            add(checkBox);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row));

            // Set checkbox state
            checkBox.setSelected(value instanceof Boolean && (Boolean) value);

            return this;
        }
    }

    /**
     * Custom editor for drag/enable column
     */
    private class DragEnableEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JLabel dragLabel;
        private final JCheckBox checkBox;

        public DragEnableEditor() {
            super(new JCheckBox());

            // sử dụng FlowLayout Làm cho thanh điều khiển kéo và hộp kiểm nhỏ gọn hơn
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0)); // Khoảng cách ngang được đặt thành  2，Khoảng cách dọc là  0
            panel.setOpaque(true);

            // Drag handle label - Biểu tượng kéo đậm
            dragLabel = new JLabel("⋮⋮");
            dragLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 4)); // Lớn hơn kích thước phông chữ tiêu chuẩn4Số và in đậm
            dragLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dragLabel.setPreferredSize(new Dimension(16, 28)); // Chiều rộng từ 20 giảm xuống 16，Nhỏ gọn hơn
            dragLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(false);

            // Add ActionListener to immediately commit checkbox changes
            checkBox.addActionListener(e -> {
                fireEditingStopped();
            });

            panel.add(dragLabel);
            panel.add(checkBox);

            // Add mouse listener to handle clicks on drag area vs checkbox area
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Rectangle checkBoxBounds = checkBox.getBounds();
                    Point panelPoint = e.getPoint();

                    // If click is on checkbox area, toggle it
                    if (checkBoxBounds.contains(panelPoint)) {
                        checkBox.setSelected(!checkBox.isSelected());
                        fireEditingStopped();
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.setBackground(table.getSelectionBackground());
            checkBox.setSelected(value instanceof Boolean && (Boolean) value);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }

    @Override
    protected void setupTableListeners() {
        addTableRightMouseListener();
        addDeleteButtonListener();
        addDragHandleMouseListener();
    }

    /**
     * Add mouse listener for delete button clicks
     */
    @Override
    protected void addDeleteButtonListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                if (!isDeleteActionAvailableAtViewPosition(row, column)) {
                    return;
                }

                int modelRow = table.getRowSorter() != null
                        ? table.getRowSorter().convertRowIndexToModel(row) : row;

                stopCellEditing();
                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
                hoveredRow = -1;
                hoveredColumn = -1;
                table.setCursor(Cursor.getDefaultCursor());
                table.repaint();
            }
        });
    }

    /**
     * Add mouse listener to show hand cursor when hovering over drag handle area
     */
    private void addDragHandleMouseListener() {
        // Sử dụng trình nghe chuột đơn giản để hiển thị con trỏ
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                if ((column == COL_DRAG_ENABLE && row >= 0) || isDeleteActionAvailableAtViewPosition(row, column)) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                // Khi nhấn vào vùng tay cầm kéo của cột đầu tiên, hàng sẽ được chọn và quá trình kéo được bắt đầu ngay lập tức
                if (column == COL_DRAG_ENABLE && row >= 0) {
                    Rectangle cellRect = table.getCellRect(row, column, false);
                    int relativeX = e.getX() - cellRect.x;

                    // bên trái25pxLà vùng kéo
                    if (relativeX < 25) {
                        // Chọn hàng
                        table.setRowSelectionInterval(row, row);

                        // Dừng chỉnh sửa
                        if (table.isEditing()) {
                            table.getCellEditor().stopCellEditing();
                        }

                        // Được đánh dấu để kéo để tránh vô tình chỉnh sửa
                        isDragging = true;

                        // Kích hoạt thao tác kéo ngay lập tức
                        TransferHandler handler = table.getTransferHandler();
                        if (handler != null) {
                            handler.exportAsDrag(table, e, TransferHandler.MOVE);
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Đặt lại trạng thái kéo sau khi thả chuột
                SwingUtilities.invokeLater(() -> isDragging = false);
            }
        });
    }

    /**
     * Add auto-append row feature when editing the last row
     */
    @Override
    public void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{true, "", "", ""});
            return;
        }
        if (values.length != 4) {
            throw new IllegalArgumentException("Variable table rows must use [enabled, key, value, action]");
        }
        tableModel.addRow(new Object[]{values[0], values[1], values[2], values[3]});
    }

    /**
     * Nhận danh sách các biến môi trường (định dạng mới)
     */
    public List<Variable> getVariableList() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<Variable> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_DRAG_ENABLE);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);

            if (!key.isEmpty()) {
                dataList.add(new Variable(enabled, key, value));
            }
        }
        return dataList;
    }

    /**
     * Từ tableModel Đọc danh sách biến trực tiếp mà không dừng việc chỉnh sửa ô hiện tại.
     * để lưu tự động / tab Các cảnh nền như chỉ báo để tránh làm gián đoạn quá trình nhập liệu đang diễn ra của người dùng.
     * Giá trị ô đang được chỉnh sửa vẫn chưa được cam kết model，Do đó, giá trị gửi cuối cùng được đọc.
     */
    public List<Variable> getVariableListFromModel() {
        List<Variable> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_DRAG_ENABLE);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);

            if (!key.isEmpty()) {
                dataList.add(new Variable(enabled, key, value));
            }
        }
        return dataList;
    }

    /**
     * Đặt danh sách biến môi trường (định dạng mới)
     */
    public void setVariableList(List<Variable> dataList) {
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (dataList != null) {
                for (Variable var : dataList) {
                    tableModel.addRow(new Object[]{var.isEnabled(), var.getKey(), var.getValue(), ""});
                }
            }

            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
