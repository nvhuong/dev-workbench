package com.nvhuong.devworkbench.panel.functional.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Hỗ trợJTableKéo và thảTransferHandler
 */
public class TableRowTransferHandler extends TransferHandler {
    private final JTable table;
    private int[] rows = null;
    private int addIndex = -1; // Chỉ mục của hàng mới được chèn
    private int addCount = 0; // Số lượng hàng được chèn
    /** Gọi lại sau khi kéo xong (để duy trì) */
    private transient Runnable onRowOrderChanged;

    public TableRowTransferHandler(JTable table) {
        this.table = table;
    }

    public void setOnRowOrderChanged(Runnable callback) {
        this.onRowOrderChanged = callback;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        rows = table.getSelectedRows();
        return new StringSelection(""); // Nội dung không liên quan
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.getComponent() == table;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
        int index = dl.getRow();
        TableModel model = table.getModel();
        if (rows == null) return false;
        if (model instanceof FunctionalRunnerTableModel runnerModel) {
            // Chỉ hỗ trợ kéo một dòng (có thể mở rộng thành nhiều dòng)
            for (int from : rows) {
                int to = index;
                runnerModel.moveRow(from, to);
                if (to > from) index--; // Chỉ số dưới thay đổi sau khi di chuyển
            }
            table.getSelectionModel().setSelectionInterval(index, index + rows.length - 1);
            return true;
        }
        // Sao chép dữ liệu hàng
        DefaultTableModel defModel = (DefaultTableModel) model;
        Object[][] rowData = new Object[rows.length][defModel.getColumnCount()];
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < defModel.getColumnCount(); j++) {
                rowData[i][j] = defModel.getValueAt(rows[i], j);
            }
        }
        // Chèn vào vị trí mục tiêu
        addIndex = index;
        addCount = rowData.length;
        for (Object[] rowDatum : rowData) {
            defModel.insertRow(index++, rowDatum);
        }
        // Chọn hàng mới được chèn
        table.getSelectionModel().setSelectionInterval(addIndex, addIndex + addCount - 1);
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        if (action == MOVE && rows != null) {
            TableModel model = table.getModel();
            if (model instanceof FunctionalRunnerTableModel) {
                // Dữ liệu đã có sẵn rồi moveRow Đã đồng bộ, không cần xóa lại
            } else {
                DefaultTableModel defModel = (DefaultTableModel) model;
                // Xóa các hàng ban đầu (từ sau ra trước)
                if (addIndex > rows[0]) {
                    for (int i = rows.length - 1; i >= 0; i--) {
                        defModel.removeRow(rows[i]);
                    }
                } else {
                    for (int i = rows.length - 1; i >= 0; i--) {
                        defModel.removeRow(rows[i] + addCount);
                    }
                }
            }
            // Một cuộc gọi lại liên tục được kích hoạt sau khi quá trình kéo hoàn tất
            if (onRowOrderChanged != null) {
                SwingUtilities.invokeLater(onRowOrderChanged);
            }
        }
        rows = null;
        addCount = 0;
        addIndex = -1;
    }
}