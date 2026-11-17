package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.ModernColors;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cải thiện tính năng kéo hàngTransferHandlerhoàn thành
 * Đã xóa ngưỡng kéo để kéo phản ứng nhanh hơn
 */
@Slf4j
public class ImprovedTableRowTransferHandler extends TransferHandler {
    private final DataFlavor localObjectFlavor;
    private int[] rows = null;
    private int addIndex = -1;
    private int addCount = 0;
    private final DefaultTableModel model;
    private final Consumer<Boolean> dragStateCallback;

    public ImprovedTableRowTransferHandler(DefaultTableModel model, Consumer<Boolean> dragStateCallback) {
        this.model = model;
        this.dragStateCallback = dragStateCallback;
        localObjectFlavor = new DataFlavor(List.class, "List of items");
    }

    @Override
    public void setDragImageOffset(Point p) {
        // Đặt độ lệch hình ảnh khi kéo để kéo tự nhiên hơn
        super.setDragImageOffset(p);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable) c;
        rows = table.getSelectedRows();

        if (rows == null || rows.length == 0) {
            return null;
        }

        // Thông báo bắt đầu kéo, vô hiệu hóa tính năng tự động điền các dòng trống
        if (dragStateCallback != null) {
            SwingUtilities.invokeLater(() -> dragStateCallback.accept(true));
        }

        List<Object[]> transferredObjects = new ArrayList<>();
        for (int row : rows) {
            Object[] rowData = new Object[model.getColumnCount()];
            for (int col = 0; col < model.getColumnCount(); col++) {
                rowData[col] = model.getValueAt(row, col);
            }
            transferredObjects.add(rowData);
        }

        // Tạo hình ảnh kéo để cung cấp phản hồi trực quan
        createDragImage(table);

        return new ImprovedTableRowsTransferable(transferredObjects, localObjectFlavor);
    }

    /**
     * Tạo hình ảnh phản hồi trực quan khi kéo
     */
    private void createDragImage(JTable table) {
        try {
            if (rows != null && rows.length > 0) {
                // Tính kích thước của hình ảnh được kéo
                Rectangle rect = table.getCellRect(rows[0], 0, true);
                int width = table.getWidth();
                int height = rect.height * Math.min(rows.length, 3); // Hiển thị nhiều nhất3Được rồi

                // Tạo hình ảnh kéo bán trong suốt
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();

                // Đặt độ trong suốt
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

                // Vẽ các hàng đã chọn
                for (int i = 0; i < Math.min(rows.length, 3); i++) {
                    Graphics rowGraphics = g2d.create(0, i * rect.height, width, rect.height);
                    table.getRowHeight(rows[i]);

                    // Vẽ đường nền
                    rowGraphics.setColor(table.getSelectionBackground());
                    rowGraphics.fillRect(0, 0, width, rect.height);

                    // Vẽ đường viền
                    rowGraphics.setColor(ModernColors.withAlpha(ModernColors.getBorderMediumColor(), 150));
                    rowGraphics.drawRect(0, 0, width - 1, rect.height - 1);

                    rowGraphics.dispose();
                }

                g2d.dispose();

                setDragImage(image);
                setDragImageOffset(new Point(0, rect.height / 2));
            }
        } catch (Exception e) {
            log.debug("Failed to create drag image: {}", e.getMessage());
            // Nó không ảnh hưởng đến chức năng kéo và thả, nhưng không có phản hồi trực quan
        }
    }

    @Override
    public boolean canImport(TransferSupport info) {
        boolean canImport = info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);

        // Đặt các chỉ báo kéo và thả rõ ràng hơn
        if (canImport) {
            info.setShowDropLocation(true);
        }

        return canImport;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferSupport info) {
        if (!canImport(info)) return false;

        JTable target = (JTable) info.getComponent();
        DefaultTableModel targetModel = (DefaultTableModel) target.getModel();
        int index = ((JTable.DropLocation) info.getDropLocation()).getRow();

        // Sửa chỉ mục: Đảm bảo chỉ mục nằm trong phạm vi hợp lệ
        int max = targetModel.getRowCount();
        if (index < 0) {
            index = max;
        } else if (index > max) {
            index = max;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> values = (List<Object[]>) info.getTransferable().getTransferData(localObjectFlavor);
            addIndex = index;
            addCount = values.size();

            // Chèn hàng theo đợt, giảm bớtUISố lượng cập nhật
            for (int i = 0; i < values.size(); i++) {
                targetModel.insertRow(index + i, values.get(i));
            }

            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Error importing data: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        try {
            if ((action == MOVE) && rows != null && addCount > 0) {
                log.debug("ExportDone - Original rows: {}, addIndex: {}, addCount: {}, total rows: {}",
                    java.util.Arrays.toString(rows), addIndex, addCount, model.getRowCount());

                // Logic đơn giản hóa: xóa các hàng nguồn từ sau ra trước, tính toán lại chỉ số chính xác cho mỗi lần xóa
                for (int i = rows.length - 1; i >= 0; i--) {
                    int originalRow = rows[i];
                    int rowToDelete = originalRow;

                    // Nếu vị trí chèn ở trước hàng nguồn thì chỉ mục của hàng nguồn sẽ bị dịch ngược về phía sauaddCount
                    if (addIndex <= originalRow) {
                        rowToDelete = originalRow + addCount;
                    }

                    // Đối với các hàng tiếp theo, bạn cũng cần xem xét số lượng hàng đã xóa trước đó
                    // Đếm số hàng đã bị xóa trước hàng hiện tại
                    int deletedBeforeCurrent = 0;
                    for (int j = i + 1; j < rows.length; j++) {
                        // Nếu hàng đã xóa trước đó nằm trước hàng hiện tại thì cần điều chỉnh chỉ mục
                        int prevDeletedRow = rows[j];
                        if (addIndex <= prevDeletedRow) {
                            prevDeletedRow = prevDeletedRow + addCount;
                        }
                        if (prevDeletedRow < rowToDelete) {
                            deletedBeforeCurrent++;
                        }
                    }

                    rowToDelete -= deletedBeforeCurrent;

                    log.debug("Removing row - original: {}, calculated: {}, model size: {}",
                        originalRow, rowToDelete, model.getRowCount());

                    // Hãy chắc chắn rằng chỉ mục là hợp lệ
                    if (rowToDelete >= 0 && rowToDelete < model.getRowCount()) {
                        model.removeRow(rowToDelete);
                    } else {
                        log.warn("Invalid row index to remove: {}", rowToDelete);
                    }
                }

                log.debug("ExportDone complete - final row count: {}", model.getRowCount());
            }
        } catch (Exception e) {
            log.error("Error in exportDone: {}", e.getMessage(), e);
        } finally {
            rows = null;
            addCount = 0;
            addIndex = -1;

            // Sau khi kéo xong, tính năng tự động điền các dòng trống sẽ được bật lại và quá trình lưu được kích hoạt
            if (dragStateCallback != null) {
                // sử dụnginvokeLaterHãy chắc chắn rằng bạn có mặt ở đóUIĐược thực thi sau khi cập nhật hoàn tất, chuyển cờ hoàn thành kéo
                SwingUtilities.invokeLater(() -> {
                    dragStateCallback.accept(false);
                    // Trì hoãn lại để đảm bảo rằng sự kiện thay đổi dữ liệu bảng được kích hoạt sau khi trạng thái kéo được đặt lại hoàn toàn
                    // Kích hoạt sự kiện cập nhật mô hình bảng để logic lưu tự động có thể phát hiện các thay đổi
                    SwingUtilities.invokeLater(model::fireTableDataChanged);
                });
            }
        }
    }

    /**
     * Tùy chỉnh Transferable Được sử dụng để kéo hàng của bảng
     */
    private record ImprovedTableRowsTransferable(List<Object[]> data, DataFlavor flavor) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{flavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return this.flavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return data;
        }
    }
}
