package com.nvhuong.devworkbench.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * Tất cả DevWorkbench Lớp cơ sở trừu tượng cho các bảng bảng
 * Cung cấp các hình thức phổ biếnUICấu hình, phương pháp công cụ và cơ chế bảo vệ đệ quy
 *
 * @param <T> Kiểu mô hình dữ liệu dạng bảng
 * @author Nvhuong
 */
@Slf4j
public abstract class AbstractTablePanel<T> extends JPanel {
    private static final int DELETE_COLUMN_WIDTH = 28;

    // ========== các lĩnh vực chung ==========

    /**
     * Mô hình dữ liệu dạng bảng
     */
    protected DefaultTableModel tableModel;

    /**
     * Thành phần bảng
     */
    @Getter
    protected JTable table;

    /**
     * Mảng tên cột
     */
    protected final String[] columns;

    /**
     * Cho dù bảng có thể chỉnh sửa được
     */
    @Getter
    protected boolean editable = true;

    /**
     * Gắn cờ để chặn việc tự động thêm các dòng trống trong quá trình vận hành hàng loạt
     */
    protected boolean suppressAutoAppendRow = false;

    /**
     * Ngăn chặn các cuộc gọi đệ quy stopCellEditing ký tên
     */
    protected boolean isStoppingCellEdit = false;

    /**
     * Chỉ mục hàng di chuột qua hiện tại (chỉ mục xem).-1 Cho biết không có di chuột
     */
    protected int hoveredRow = -1;

    /**
     * Chỉ mục cột (chỉ mục xem) của lần di chuột hiện tại,-1 Cho biết không có di chuột
     */
    protected int hoveredColumn = -1;

    private static final String ACTION_DELETE_ROW = "deleteRow";
    private static final String ACTION_ENTER_NAV  = "enterNav";
    private static final String ACTION_EDITOR_NEXT_CELL = "editorNextCell";
    private static final String ACTION_EDITOR_PREVIOUS_CELL = "editorPreviousCell";

    // ========== Người xây dựng ==========

    /**
     * Người xây dựng
     *
     * @param columns Mảng tên cột
     */
    protected AbstractTablePanel(String[] columns) {
        this.columns = columns;
    }

    // ========== Phương thức trừu tượng (các lớp con phải triển khai nó)==========

    /**
     * Lấy chỉ mục của cột được kích hoạt
     *
     * @return Bật lập chỉ mục cột
     */
    protected abstract int getEnabledColumnIndex();

    /**
     * Lấy chỉ mục của cột đã xóa
     *
     * @return Xóa chỉ mục cột
     */
    protected abstract int getDeleteColumnIndex();

    /**
     * Lấy chỉ mục của cột có thể chỉnh sửa đầu tiên (đối với Tabđiều hướng)
     *
     * @return Chỉ mục cột có thể chỉnh sửa đầu tiên
     */
    protected abstract int getFirstEditableColumnIndex();

    /**
     * Lấy chỉ mục của cột có thể chỉnh sửa cuối cùng (đối với Tabđiều hướng)
     *
     * @return Chỉ mục của cột có thể chỉnh sửa cuối cùng
     */
    protected abstract int getLastEditableColumnIndex();

    /**
     * Lấy Key Bấm cột Enter Cột mục tiêu để chuyển tới sau.
     * Theo mặc định, cột có thể chỉnh sửa cuối cùng sẽ được sử dụng; cái chứa cột mô tả Key/Value Bảng có thể được ghi đè dưới dạng Value cột.
     *
     * @return Enter Chỉ mục cột mục tiêu điều hướng
     */
    protected int getEnterTargetColumnIndex() {
        return getLastEditableColumnIndex();
    }

    /**
     * Kiểm tra xem ô được chỉ định có thể chỉnh sửa được không (đối với Tabđiều hướng)
     *
     * @param row    chỉ số hàng
     * @param column Chỉ số cột
     * @return true Nếu có thể chỉnh sửa
     */
    protected abstract boolean isCellEditableForNavigation(int row, int column);

    /**
     * Kiểm tra xem dòng được chỉ định có nội dung hay không
     *
     * @param row chỉ số hàng
     * @return true Nếu có nội dung
     */
    protected abstract boolean hasContentInRow(int row);

    /**
     * Lấy loại cột của mô hình bảng
     * Các lớp con có thể ghi đè phương thức này để xác định các loại cột đặc biệt (ví dụ:Booleanloại cột hộp kiểm)
     *
     * @param columnIndex Chỉ số cột
     * @return cộtClassloại
     */
    protected Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == getEnabledColumnIndex()) {
            return Boolean.class;
        }
        return Object.class;
    }

    /**
     * Kiểm tra xem một ô có thể chỉnh sửa được không (đối với các mẫu bảng)
     * Các lớp con có thể ghi đè phương thức này để thực hiện logic chỉnh sửa đặc biệt
     *
     * @param row    chỉ số hàng
     * @param column Chỉ số cột
     * @return true Nếu có thể chỉnh sửa
     */
    protected boolean isCellEditable(int row, int column) {
        if (!editable) {
            return false;
        }

        // Checkbox column is always editable
        if (column == getEnabledColumnIndex()) {
            return true;
        }

        // Delete column is not editable (uses custom renderer)
        if (column == getDeleteColumnIndex()) {
            return false;
        }

        // Other columns use navigation logic
        return isCellEditableForNavigation(row, column);
    }

    // ========== Phổ quát UI Phương pháp cấu hình ==========

    /**
     * Phương pháp khởi tạo thành phần chung
     * Tạo mô hình bảng, bảng và bảng cuộn
     */
    protected void initializeComponents() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Initialize table model with custom cell editing logic
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return AbstractTablePanel.this.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return AbstractTablePanel.this.isCellEditable(row, column);
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                Object oldValue = getValueAt(row, column);
                if (Objects.equals(oldValue, aValue)) {
                    return;
                }
                super.setValueAt(aValue, row, column);
            }
        };

        // Create table
        table = new JTable(tableModel);

        if (useTableScrollPane()) {
            // Wrap table in JScrollPane to show headers
            JScrollPane scrollPane = new JScrollPane(table);
            ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            configureTableScrollPane(scrollPane);
            add(scrollPane, BorderLayout.CENTER);
        } else {
            add(table.getTableHeader(), BorderLayout.NORTH);
            add(table, BorderLayout.CENTER);
        }
    }

    protected void configureTableScrollPane(JScrollPane scrollPane) {
        // Default scroll behavior remains owned by Swing/FlatLaf.
    }

    protected boolean useTableScrollPane() {
        return true;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Đặt lại đường viền khi chuyển đổi chủ đề để đảm bảo màu lưới của bảng được cập nhật
        if (getBorder() != null) {
            setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        }
    }

    /**
     * Khởi tạo biểu mẫu UI
     * Chung cho tất cả các bảng UI cấu hình
     */
    protected void initializeTableUI() {
        table.setFillsViewportHeight(false); // Chỉ chiếm chiều cao cần thiết và tránh quá nhiều dòng trống
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // Nhỏ hơn kích thước phông chữ tiêu chuẩn1Không
        table.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1)); // Nhỏ hơn kích thước phông chữ tiêu chuẩn1số (in đậm)
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowMargin(1);
        ToolWindowSurfaceStyle.applyTableCard(table);
        setColumnRenderer(getEnabledColumnIndex(), new EnabledColumnRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        // Configure Tab key behavior to move between columns
        table.setSurrendersFocusOnKeystroke(true);

        // Cài đặt chính: Tự động dừng chỉnh sửa và lưu khi mất tiêu điểm, đã triển khai DevWorkbench Lưu ngay các kiểu
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Nhấn vào đây để vào chế độ chỉnh sửa (DevWorkbench phong cách)
        setupSingleClickEditing();

        // Làm nổi bật hàng di chuột
        setupRowHoverHighlight();

        // Delete / Backspace phím để xóa hàng đã chọn
        setupDeleteKeyShortcut();

        // Enter Phím nhảy giữa các cột
        setupEnterKeyNavigation();
    }

    /**
     * Nhấp để vào chế độ chỉnh sửa trong khi xử lý hàng được di chuột mouseExited Thông thoáng.
     * Hai logic hợp nhất thành một MouseAdapter，Giảm số lượng đối tượng người nghe.
     */
    private void setupSingleClickEditing() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (editable && SwingUtilities.isLeftMouseButton(e)) {
                    triggerSingleClickEdit(e);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredRow != -1 || hoveredColumn != -1) {
                    hoveredRow = -1;
                    hoveredColumn = -1;
                    table.putClientProperty("hoverRow", -1);
                    table.putClientProperty("hoverColumn", -1);
                    updateDeleteButtonCursor(-1, -1);
                    table.repaint();
                }
            }
        });
    }

    /** Kích hoạt chỉnh sửa ô khi nhấp chuột (trì hoãn một khung hình để đảm bảo việc lựa chọn hoàn tất trước) */
    private void triggerSingleClickEdit(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if (row < 0 || col < 0) return;
        if (col == getEnabledColumnIndex() || col == getDeleteColumnIndex()) return;
        if (!table.isCellEditable(row, col)) return;
        SwingUtilities.invokeLater(() -> {
            if (!table.isEditing() || table.getEditingRow() != row || table.getEditingColumn() != col) {
                table.editCellAt(row, col);
            }
            if (table.isEditing()
                    && table.getEditingRow() == row
                    && table.getEditingColumn() == col) {
                Component ed = table.getEditorComponent();
                if (ed != null) {
                    focusEditorTextComponent(ed, true);
                }
            }
        });
    }

    /**
     * Tập trung vào thành phần chỉnh sửa văn bản hiện đang hiển thị.
     *
     * <p>Trình chỉnh sửa giá trị thông minh là a CardLayout Vùng chứa chứa một hàng tại một thời điểm JTextField và nhiều dòng JTextArea。
     * Bạn không thể chỉ tìm kiếm theo loại thành phần JTextField，Nếu không, thành phần một hàng ẩn sẽ được tìm thấy sau khi giá trị dài được chuyển sang thẻ nhiều hàng.
     * Windows Đặc biệt dễ dàng cho thấy rằng việc nhập liệu không thể thực hiện được sau khi nhấp đúp.</p>
     */
    private void focusEditorTextComponent(Component editor, boolean selectSingleLineText) {
        JTextComponent textComponent = findVisibleTextComponent(editor);
        if (textComponent == null) {
            editor.requestFocusInWindow();
            return;
        }

        if (textComponent instanceof JTextArea textArea) {
            installTextAreaTabNavigation(textArea);
        }
        textComponent.requestFocusInWindow();
        if (selectSingleLineText && textComponent instanceof JTextField textField) {
            textField.selectAll();
        }
    }

    /**
     * JTextArea Theo mặc định nó sẽ như vậy Tab Được chèn dưới dạng tab, được ưu tiên hơn tổ tiên của bảng InputMap。
     * Trình chỉnh sửa giá trị dài để sử dụng JTextArea , điều hướng ô cần được chuyển tiếp rõ ràng tới bảng.
     */
    private void installTextAreaTabNavigation(JTextArea textArea) {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0),
                ACTION_EDITOR_NEXT_CELL);
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB,
                        java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                ACTION_EDITOR_PREVIOUS_CELL);

        ActionMap actionMap = textArea.getActionMap();
        actionMap.put(ACTION_EDITOR_NEXT_CELL, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                moveToNextEditableCell(false);
            }
        });
        actionMap.put(ACTION_EDITOR_PREVIOUS_CELL, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                moveToNextEditableCell(true);
            }
        });
    }

    private static JTextComponent findVisibleTextComponent(Component root) {
        return findVisibleTextComponent(root, true);
    }

    private static JTextComponent findVisibleTextComponent(Component root, boolean parentVisible) {
        boolean visible = parentVisible && root.isVisible();
        if (root instanceof JTextComponent textComponent
                && visible
                && textComponent.isEnabled()) {
            return textComponent;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextComponent found = findVisibleTextComponent(child, visible);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    /**
     * Làm nổi bật hàng khi di chuột: Ghi lại chỉ số hàng hiện tại khi chuột di chuyển
     */
    private void setupRowHoverHighlight() {
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                boolean hoverChanged = row != hoveredRow || column != hoveredColumn;
                hoveredRow = row;
                hoveredColumn = column;
                table.putClientProperty("hoverRow", row);
                table.putClientProperty("hoverColumn", column);
                updateDeleteButtonCursor(row, column);
                if (hoverChanged) {
                    table.repaint();
                }
            }
        });
    }

    /**
     * Delete / Backspace để xóa hàng hiện được chọn (chỉ khi ô không ở trạng thái chỉnh sửa)
     */
    private void setupDeleteKeyShortcut() {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = table.getActionMap();

        AbstractAction deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!table.isEditing()) {
                    deleteSelectedRow();
                }
            }
        };

        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), ACTION_DELETE_ROW);
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, 0), ACTION_DELETE_ROW);
        actionMap.put(ACTION_DELETE_ROW, deleteAction);
    }

    /**
     * Enter Điều hướng chính:
     * - Vào Key Bấm cột Enter → Chuyển đến các đồng nghiệp Value cột
     * - Vào Value Bấm cột Enter → Chuyển sang dòng tiếp theo Key cột (chắp thêm nếu không có hàng tiếp theo)
     */
    private void setupEnterKeyNavigation() {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), ACTION_ENTER_NAV);
        actionMap.put(ACTION_ENTER_NAV, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!editable) return;

                // Nếu cửa sổ bật lên tự động hoàn thành hiển thị, hãy nhấp vào  Enter Hãy để cửa sổ bật lên xử lý nó (chấp nhận đề xuất) và không thực hiện điều hướng giữa các dòng
                if (isAutoCompletePopupVisible()) {
                    int row = table.isEditing() ? table.getEditingRow()    : table.getSelectedRow();
                    int col = table.isEditing() ? table.getEditingColumn() : table.getSelectedColumn();
                    dispatchEnterToEditor();
                    // Key Cột tự động nhảy sau khi chấp nhận đề xuất Value Danh sách,Value Cột giữ nguyên sau khi chấp nhận đề xuất (người dùng nhấn lại Enter dòng mới)
                    if (col == getFirstEditableColumnIndex() && row >= 0) {
                        enterFromKeyColumn(row);
                    }
                    return;
                }

                int row = table.isEditing() ? table.getEditingRow()    : table.getSelectedRow();
                int col = table.isEditing() ? table.getEditingColumn() : table.getSelectedColumn();
                if (row < 0) return;

                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }

                if (col == getFirstEditableColumnIndex()) {
                    enterFromKeyColumn(row);
                } else {
                    enterFromValueColumn(row);
                }
            }
        });
    }

    /** Enter Vào Key Cột: Chuyển về cùng một hàng Value cột */
    private void enterFromKeyColumn(int row) {
        int valueCol = getEnterTargetColumnIndex();
        if (table.isCellEditable(row, valueCol)) {
            // invokeLater Hãy chắc chắn stopCellEditing Đã Key Giá trị được viết model，Value Biên tập viên đọc lại
            SwingUtilities.invokeLater(() -> startEditAt(row, valueCol));
        }
    }

    /** Enter Vào Value Cột: Chuyển tới phần có thể chỉnh sửa tiếp theo Key Được rồi */
    private void enterFromValueColumn(int row) {
        int keyCol  = getFirstEditableColumnIndex();
        int nextRow = row + 1;

        if (nextRow >= table.getRowCount()) {
            // Hiện tại dòng cuối cùng: .autoAppendRowFeature Cũng được sử dụng invokeLater Nối thêm dòng mới.
            // Sử dụng gấp đôi invokeLater：Lần đầu tiên hãy để autoAppend của invokeLater Hoàn thành việc thực hiện đầu tiên.
            // Chuyển đến dòng mới được thêm lần thứ hai (lần này rowCount đã được thêm vào).
            SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                int last = table.getRowCount() - 1;
                if (last > row) {
                    // Dòng mới đã được thêm vào, hãy nhảy tới nó Key cột
                    startEditAt(last, keyCol);
                }
                // giống autoAppend Không có phần nối thêm (bản thân dòng hiện tại là một dòng trống) và nó vẫn đứng yên
            }));
            return;
        }

        // Bỏ qua Key Các hàng có cột không thể chỉnh sửa được (như mặc định header Được rồi, nó Key là chỉ đọc)
        while (nextRow < table.getRowCount() && !table.isCellEditable(nextRow, keyCol)) {
            nextRow++;
        }
        if (nextRow < table.getRowCount()) {
            final int targetRow = nextRow;
            SwingUtilities.invokeLater(() -> startEditAt(targetRow, keyCol));
        }
    }

    /**
     * Kiểm tra xem có cửa sổ bật lên tự động hoàn thành nào được hiển thị trong trình chỉnh sửa hiện tại hay không.
     * Được sử dụng cho Enter Đánh giá chính: Nếu cửa sổ bật lên hiển thị,Enter Đề xuất nên được chấp nhận thay vì bỏ qua.
     */
    private boolean isAutoCompletePopupVisible() {
        Component ed = table.getEditorComponent();
        if (ed == null) return false;
        return containsVisibleAutoComplete(ed);
    }

    private static boolean containsVisibleAutoComplete(Component root) {
        // Đường dẫn nhập tránh sự phụ thuộc cứng: đánh giá theo phản ánh tên lớp hoặc kiểm tra trực tiếp các phương thức
        if (root instanceof com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField ac) {
            return ac.isAutoCompletePopupVisible();
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                if (containsVisibleAutoComplete(child)) return true;
            }
        }
        return false;
    }

    /**
     * Bó Enter Các sự kiện chính được chuyển tiếp trực tiếp tới hộp văn bản trong trình chỉnh sửa.
     * cho phép AutoCompleteDevWorkbenchTextField của KeyAdapter Chấp nhận lời đề nghị.
     */
    private void dispatchEnterToEditor() {
        Component ed = table.getEditorComponent();
        if (ed == null) return;
        com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField ac = findAutoComplete(ed);
        if (ac != null) {
            // Gọi trực tiếp logic chấp nhận đề xuất (được kích hoạt thông qua phương thức công khai)
            ac.acceptCurrentSuggestion();
        }
    }

    private static com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField findAutoComplete(Component root) {
        if (root instanceof com.nvhuong.devworkbench.common.component.AutoCompleteDevWorkbenchTextField ac) return ac;
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                var found = findAutoComplete(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Đặt chiều rộng của cột được kích hoạt
     *
     * @param width Chiều rộng
     */
    protected void setEnabledColumnWidth(int width) {
        int colIndex = getEnabledColumnIndex();
        table.getColumnModel().getColumn(colIndex).setPreferredWidth(width);
        table.getColumnModel().getColumn(colIndex).setMaxWidth(width);
        table.getColumnModel().getColumn(colIndex).setMinWidth(width);
    }

    /**
     * Đặt chiều rộng của cột đã xóa
     */
    protected void setDeleteColumnWidth() {
        int colIndex = getDeleteColumnIndex();
        table.getColumnModel().getColumn(colIndex).setPreferredWidth(DELETE_COLUMN_WIDTH);
        table.getColumnModel().getColumn(colIndex).setMaxWidth(DELETE_COLUMN_WIDTH);
        table.getColumnModel().getColumn(colIndex).setMinWidth(DELETE_COLUMN_WIDTH);
        table.getColumnModel().getColumn(colIndex).setResizable(false);
    }

    /**
     * Thiết lập trình chỉnh sửa cột
     *
     * @param column Chỉ số cột
     * @param editor Trình chỉnh sửa di động
     */
    protected void setColumnEditor(int column, TableCellEditor editor) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellEditor(editor);
        }
    }

    /**
     * Đặt trình kết xuất cột
     *
     * @param column   Chỉ số cột
     * @param renderer Trình kết xuất tế bào
     */
    protected void setColumnRenderer(int column, TableCellRenderer renderer) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    // ========== Các phương pháp công cụ phổ biến ==========

    /**
     * Dừng chỉnh sửa ô một cách an toàn
     * Ngăn chặn nó xuất hiện khi sửa đổi dữ liệu bảng ArrayIndexOutOfBoundsException
     */
    public void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    /**
     * Dừng chỉnh sửa với bảo vệ đệ quy
     * Cuộc gọi có thể kích hoạt các sự kiện bảng getter phương pháp được sử dụng
     */
    protected void stopCellEditingWithProtection() {
        if (!isStoppingCellEdit) {
            isStoppingCellEdit = true;
            try {
                stopCellEditing();
            } finally {
                isStoppingCellEdit = false;
            }
        }
    }

    /**
     * Xóa dữ liệu bảng
     */
    public void clear() {
        stopCellEditing();

        // Hãy chắc chắn rằng bạn có mặt ở đó EDT Tránh thực hiện các thao tác bảng trong các luồng RowSorter Vấn đề an toàn chủ đề
        if (SwingUtilities.isEventDispatchThread()) {
            clearInternal();
        } else {
            try {
                SwingUtilities.invokeAndWait(this::clearInternal);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Khôi phục trạng thái bị gián đoạn
                log.warn("Interrupted while clearing table", e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                log.error("Error clearing table", e);
            }
        }
    }

    private void clearInternal() {
        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            tableModel.addRow(createEmptyRow());
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Tạo các hàng dữ liệu trống
     *
     * @return Mảng dòng trống
     */
    protected Object[] createEmptyRow() {
        Object[] row = new Object[columns.length];
        row[getEnabledColumnIndex()] = true; // Được bật theo mặc định
        for (int i = 0; i < columns.length; i++) {
            if (i != getEnabledColumnIndex()) {
                row[i] = "";
            }
        }
        return row;
    }

    /**
     * Kiểm tra xem dòng cuối cùng có nội dung hay không
     *
     * @return true Nếu dòng cuối cùng có nội dung
     */
    protected boolean hasContentInLastRow() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            return false;
        }
        return hasContentInRow(rowCount - 1);
    }

    /**
     * Đảm bảo hàng cuối cùng của bảng trống
     * Tương tự DevWorkbench Tự động thêm chức năng dòng trống
     */
    protected void ensureEmptyLastRow() {
        suppressAutoAppendRow = true;
        try {
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(createEmptyRow());
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Thêm trình nghe mô hình bảng
     *
     * @param listener người nghe
     */
    public void addTableModelListener(TableModelListener listener) {
        if (tableModel != null) {
            tableModel.addTableModelListener(listener);
        }
    }

    /**
     * Thêm một hàng dữ liệu (phương pháp chung)
     *
     * @param values dữ liệu hàng
     */
    public void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(createEmptyRow());
        } else {
            // Create row with provided values
            Object[] row = new Object[columns.length];
            row[getEnabledColumnIndex()] = true; // Được bật theo mặc định

            // Fill in the provided values
            int valueIndex = 0;
            for (int i = 0; i < columns.length; i++) {
                if (i == getDeleteColumnIndex()) {
                    row[i] = ""; // Delete column is always empty
                } else if (i != getEnabledColumnIndex()) {
                    // enabled column already set to true; fill everything else
                    row[i] = (valueIndex < values.length) ? values[valueIndex++] : "";
                }
            }
            tableModel.addRow(row);
        }
    }

    /**
     * Thêm một hàng và cuộn đến cuối
     */
    public void addRowAndScroll() {
        addRow();
        scrollToLastRow();
    }

    /**
     * Xóa hàng hiện được chọn
     */
    public void deleteSelectedRow() {
        stopCellEditing();

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            // Convert view index to model index if using row sorter
            int modelRow = selectedRow;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();

                // Don't delete if it's the only row (keep at least one empty row like DevWorkbench)
                if (rowCount <= 1) {
                    return;
                }

                // Check if this row can be deleted (Các lớp con có thể ghi đè isDeletableRow để thêm các kiểm tra bổ sung)
                if (!isDeletableRow(modelRow)) {
                    return;
                }

                // Delete the row
                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
            }
        }
    }

    /**
     * Kiểm tra xem hàng được chỉ định có thể bị xóa hay không. Các lớp con có thể ghi đè phương thức này để thêm logic kiểm tra bổ sung.
     *
     * @param modelRow Chỉ số hàng mô hình
     * @return true Nếu có thể xóa được
     */
    protected boolean isDeletableRow(int modelRow) {
        return true;
    }

    /**
     * Xác định xem ô xem được chỉ định có phải là nút xóa hiện có thể nhấp hay không.
     */
    protected boolean isDeleteActionAvailableAtViewPosition(int viewRow, int viewColumn) {
        if (viewRow < 0 || viewColumn != getDeleteColumnIndex()) {
            return false;
        }
        int modelRow = (table.getRowSorter() != null)
                ? table.getRowSorter().convertRowIndexToModel(viewRow) : viewRow;
        return isDeleteActionAvailableAtModelRow(modelRow);
    }

    /**
     * Xác định xem hàng mô hình đã chỉ định có được hiển thị hay không và phản hồi nút xóa.
     */
    private boolean isDeleteActionAvailableAtModelRow(int modelRow) {
        if (!editable || modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return false;
        }
        if (!isDeletableRow(modelRow)) {
            return false;
        }

        int rowCount = tableModel.getRowCount();
        boolean isLastRow = modelRow == rowCount - 1;
        boolean isEmpty = !hasContentInRow(modelRow);
        return !isLastRow || (!isEmpty && rowCount > 1);
    }

    /**
     * Nút xóa khi di chuột JTable Tự chuyển đổi chế độ chuột;renderer trên thành phần cursor sẽ không có hiệu lực.
     */
    private void updateDeleteButtonCursor(int viewRow, int viewColumn) {
        int cursorType = isDeleteActionAvailableAtViewPosition(viewRow, viewColumn)
                ? Cursor.HAND_CURSOR
                : Cursor.DEFAULT_CURSOR;
        if (table.getCursor().getType() != cursorType) {
            table.setCursor(Cursor.getPredefinedCursor(cursorType));
        }
    }


    /**
     * Thêm trình nghe menu chuột phải
     */
    protected void addTableRightMouseListener() {
        MouseAdapter tableMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                if (!editable || !isContextMenuEnabled()) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }

                JPopupMenu contextMenu = createContextPopupMenu(viewRow);
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Tạo menu chuột phải
     * Các lớp con có thể ghi đè phương thức này để tùy chỉnh các mục menu
     *
     * @param viewRow Chỉ mục hàng xem hiện được chọn.-1 Cho biết không có hàng nào được chọn
     * @return Trình đơn nhấp chuột phải
     */
    protected JPopupMenu createContextPopupMenu(int viewRow) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        JMenuItem addItem = new JMenuItem("Add");
        addItem.addActionListener(e -> addRowAndScroll());
        menu.add(addItem);

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> deleteSelectedRow());
        menu.add(removeItem);

        return menu;
    }

    protected boolean isContextMenuEnabled() {
        return true;
    }

    /**
     * Thêm trình nghe nút xóa
     */
    protected void addDeleteButtonListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (editable && SwingUtilities.isLeftMouseButton(e)) {
                    handleDeleteButtonClick(e);
                }
            }
        });
    }

    /** Xử lý các sự kiện nhấp chuột để xóa cột */
    private void handleDeleteButtonClick(MouseEvent e) {
        int column = table.columnAtPoint(e.getPoint());
        int row    = table.rowAtPoint(e.getPoint());

        if (!isDeleteActionAvailableAtViewPosition(row, column)) {
            return;
        }

        int modelRow = (table.getRowSorter() != null)
                ? table.getRowSorter().convertRowIndexToModel(row) : row;

        stopCellEditing();
        tableModel.removeRow(modelRow);
        ensureEmptyLastRow();
        hoveredRow = -1;
        hoveredColumn = -1;
        updateDeleteButtonCursor(-1, -1);
        table.repaint();
    }

    /**
     * Thêm trình nghe bảng (menu chuột phải và nút xóa)
     */
    protected void setupTableListeners() {
        addTableRightMouseListener();
        addDeleteButtonListener();
    }

    /**
     * Đặt xem bảng có thể chỉnh sửa được hay không
     *
     * @param editable Cho dù nó có thể chỉnh sửa được
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        updateDeleteButtonCursor(hoveredRow, hoveredColumn);
        table.repaint(); // Làm mới màn hình
    }

    /**
     * Di chuyển đến hàng cuối cùng
     */
    protected void scrollToLastRow() {
        SwingUtilities.invokeLater(() -> {
            int rowCount = table.getRowCount();
            if (rowCount > 0) {
                Rectangle rect = table.getCellRect(rowCount - 1, 0, true);
                table.scrollRectToVisible(rect);
            }
        });
    }

    /**
     * Nhận giá trị chuỗi từ hàng được chỉ định của mô hình dạng bảng
     *
     * @param row chỉ số hàng
     * @param col Chỉ số cột
     * @return giá trị chuỗi,null Chuyển đổi thành chuỗi trống
     */
    protected String getStringValue(int row, int col) {
        Object obj = tableModel.getValueAt(row, col);
        return obj == null ? "" : obj.toString().trim();
    }

    /**
     * Nhận giá trị boolean từ hàng được chỉ định của mô hình dạng bảng
     *
     * @param row chỉ số hàng
     * @param col Chỉ số cột
     * @return Boolean, khôngBooleanNhập mặc định thành true
     */
    protected boolean getBooleanValue(int row, int col) {
        Object obj = tableModel.getValueAt(row, col);
        return !(obj instanceof Boolean b) || b;
    }

    /**
     * Đã thêm chức năng tự động thêm dòng trống
     * Khi dòng cuối cùng có nội dung thì tự động thêm dòng trống mới vào cuối (tương tự như DevWorkbenchhành vi)
     */
    protected void addAutoAppendRowFeature() {
        tableModel.addTableModelListener(e -> {
            if (suppressAutoAppendRow || !editable) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    int rowCount = tableModel.getRowCount();
                    if (rowCount == 0) {
                        return;
                    }

                    // Check if the last row has any content
                    int lastRow = rowCount - 1;
                    boolean lastRowHasContent = hasContentInRow(lastRow);

                    // Add empty row if last row has content
                    if (lastRowHasContent) {
                        suppressAutoAppendRow = true;
                        try {
                            tableModel.addRow(createEmptyRow());
                        } finally {
                            suppressAutoAppendRow = false;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error in auto-append row feature", ex);
                }
            });
        });
    }

    protected class EnabledColumnRenderer extends JCheckBox implements TableCellRenderer {
        private EnabledColumnRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setSelected(value instanceof Boolean selected && selected);
            setBackground(TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row));
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    /**
     * Các cột có thể xóa vẫn có thể nhấp vào và các hàng có thể xóa luôn hiển thị biểu tượng.
     */
    protected class DeleteButtonRenderer extends JLabel implements TableCellRenderer {
        private final FlatSVGIcon deleteIcon;

        public DeleteButtonRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            deleteIcon = IconUtil.createThemed("icons/delete.svg", 18, 18);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Set background
            setBackground(TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row));
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

            // Clear icon by default
            setIcon(null);
            setCursor(Cursor.getDefaultCursor());

            // Convert view row to model row
            int modelRow = row;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(row);
            }

            boolean deleteAvailable = isDeleteActionAvailableAtModelRow(modelRow);
            setToolTipText(deleteAvailable ? CommonI18n.get(CommonMessageKeys.BUTTON_DELETE) : null);

            if (deleteAvailable) {
                setIcon(deleteIcon);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (!isSelected && row == hoveredRow && column == hoveredColumn) {
                    setBackground(TableUIConstants.getCellBackground(false, true, false, table, row));
                }
            }

            return this;
        }
    }

    // ========== Tab chức năng điều hướng chính ==========

    /**
     * cài đặtTabPhím điều hướng, hỗ trợ nhảy giữa các ô có thể chỉnh sửa
     */
    protected void setupTabKeyNavigation() {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        // Tab key - move to next editable cell
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0), "nextCell");
        actionMap.put("nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(false);
            }
        });

        // Shift+Tab - move to previous editable cell
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "previousCell");
        actionMap.put("previousCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(true);
            }
        });
    }

    /**
     * Di chuyển đến ô có thể chỉnh sửa tiếp theo (hoặc trước đó)
     *
     * @param reverse true Biểu thị việc di chuyển lùi lại (Shift+Tab），false Cho biết đang di chuyển về phía trước (Tab）
     */
    protected void moveToNextEditableCell(boolean reverse) {
        // Ưu tiên sử dụng hàng và cột trong chỉnh sửa (hơn  selectedRow/Col chính xác hơn).
        // Vì bấm trực tiếp vào chỉnh sửa có thể không nhất thiết phải cập nhật cùng lúc selection。
        int currentRow    = table.isEditing() ? table.getEditingRow()    : table.getSelectedRow();
        int currentColumn = table.isEditing() ? table.getEditingColumn() : table.getSelectedColumn();

        if (currentRow < 0 || currentColumn < 0) {
            startEditAt(0, getFirstEditableColumnIndex());
            return;
        }

        // Gửi bản chỉnh sửa hiện tại trước, sau đó tìm ô mục tiêu
        // stopCellEditing() Có thể trở lại false（Ví dụ DevWorkbenchSmartValueCellEditor của ignoreFocusLost Bảo vệ),
        // được sử dụng vào thời điểm này forceStop（đến trình soạn thảo thông minh) hoặc cancelCellEditing() Chắc chắn là buộc phải bỏ việc Tab Điều hướng luôn có thể tiếp tục.
        if (table.isEditing()) {
            TableCellEditor currentEditor = table.getCellEditor();
            if (currentEditor != null) {
                boolean stopped = false;
                if (currentEditor instanceof DevWorkbenchSmartValueCellEditor smartEditor) {
                    stopped = smartEditor.forceStopCellEditing();
                } else {
                    stopped = currentEditor.stopCellEditing();
                }
                if (!stopped) {
                    currentEditor.cancelCellEditing();
                }
            }
        }

        if (!reverse) {
            // Tab Chuyển tiếp: Phát hiện xem cột có thể chỉnh sửa cuối cùng có ở hàng cuối cùng hay không
            boolean isLastRow = (currentRow == table.getRowCount() - 1);
            boolean isLastEditableCol = (currentColumn == getLastEditableColumnIndex());
            if (isLastRow && isLastEditableCol && editable) {
                // Tương tự Enter Vào Value Hành vi của cột: Nối hàng mới và chuyển đến hàng đó Key cột
                SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                    int last = table.getRowCount() - 1;
                    if (last > currentRow) {
                        startEditAt(last, getFirstEditableColumnIndex());
                    }
                    // giống autoAppend Không được nối thêm (dòng hiện tại là dòng trống), nó vẫn đứng yên
                }));
                return;
            }
        }

        // Tính toán ô đích (stopCellEditing Tuy nhiên, nó đồng bộ model Các bản cập nhật có thể kích hoạt các sự kiện, hãy sử dụng  invokeLater Đảm bảo thời gian)
        final int[] target = reverse
                ? findPreviousEditableCell(currentRow, currentColumn)
                : findNextEditableCell(currentRow, currentColumn);

        final int targetRow = target[0];
        final int targetCol = target[1];
        final int columnCount = table.getColumnCount();

        SwingUtilities.invokeLater(() -> {
            if (targetCol >= 0 && targetCol < columnCount && targetRow >= 0 && targetRow < table.getRowCount()) {
                startEditAt(targetRow, targetCol);
            }
        });
    }

    /** chuyển tiếp (Tab）Tìm kiếm ô có thể chỉnh sửa tiếp theo và quay lại [row, col] */
    private int[] findNextEditableCell(int row, int col) {
        int columnCount  = table.getColumnCount();
        int maxAttempts  = columnCount * (table.getRowCount() + 1);
        for (int i = 0; i < maxAttempts; i++) {
            col++;
            if (col >= columnCount) {
                if (row < table.getRowCount() - 1) {
                    row++;
                    col = getFirstEditableColumnIndex() - 1; // Chu kỳ tiếp theo +1 Điều cuối cùng đã xảy ra firstEditable
                } else {
                    // Đã đến điểm cuối (nhánh này ở moveToNextEditableCell đã được xử lý trước), dừng ở cột có thể chỉnh sửa cuối cùng
                    return new int[]{row, getLastEditableColumnIndex()};
                }
                continue; // Hãy lặp lại col++ Hãy đến đúng nơi
            }
            if (isCellEditableForNavigation(row, col)) {
                return new int[]{row, col};
            }
        }
        return new int[]{row, getFirstEditableColumnIndex()};
    }

    /** lạc hậu(Shift+Tab）Tìm kiếm ô có thể chỉnh sửa trước đó và quay lại [row, col] */
    private int[] findPreviousEditableCell(int row, int col) {
        int columnCount = table.getColumnCount();
        int maxAttempts = columnCount * (table.getRowCount() + 1);
        for (int i = 0; i < maxAttempts; i++) {
            col--;
            if (col < 0) {
                if (row > 0) {
                    row--;
                    col = getLastEditableColumnIndex() + 1; // Chu kỳ tiếp theo -1 Điều cuối cùng đã xảy ra lastEditable
                } else {
                    // Đã đến đầu, dừng ở cột có thể chỉnh sửa đầu tiên
                    return new int[]{row, getFirstEditableColumnIndex()};
                }
                continue;
            }
            if (isCellEditableForNavigation(row, col)) {
                return new int[]{row, col};
            }
        }
        return new int[]{row, getFirstEditableColumnIndex()};
    }

    /** Bắt đầu chỉnh sửa ô đã chỉ định và tập trung vào thành phần chỉnh sửa hiện đang hiển thị */
    private void startEditAt(int row, int col) {
        table.changeSelection(row, col, false, false);
        table.editCellAt(row, col);
        // Thu thập tiêu điểm bị trì hoãn: .editCellAt Có thể được hoàn thành không đồng bộ.invokeLater Hãy chắc chắn rằng trình soạn thảo được hiển thị
        SwingUtilities.invokeLater(() -> {
            Component ed = table.getEditorComponent();
            if (ed == null) return;
            focusEditorTextComponent(ed, true);
        });
    }
}
