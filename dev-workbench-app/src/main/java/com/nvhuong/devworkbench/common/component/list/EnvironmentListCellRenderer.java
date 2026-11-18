package com.nvhuong.devworkbench.common.component.list;

import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.environment.EnvironmentItem;
import com.nvhuong.devworkbench.service.EnvironmentService;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

import static com.nvhuong.devworkbench.util.JComponentUtils.ellipsisText;

// Trình kết xuất danh sách môi trường
public class EnvironmentListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof EnvironmentItem item) {
            String envName = item.getEnvironment().getName(); // Lấy tên môi trường
            label.setText(ellipsisText(envName, list));  // Hiển thị hình elip vượt quá chiều rộng
            label.setToolTipText(envName); // ngoài hiển thịtip
            Environment active = EnvironmentService.getActiveEnvironment();

            // Xác định xem đó có phải là môi trường được kích hoạt hay không (cần xử lý id vì null tình huống)
            boolean isActive = false;
            if (active != null && item.getEnvironment() != null) {
                String activeId = active.getId();
                String itemId = item.getEnvironment().getId();
                // Hai id Không null Sau đó mới so sánh
                if (activeId != null && itemId != null) {
                    isActive = activeId.equals(itemId);
                }
            }

            // Đặt biểu tượng và kiểu dáng
            if (isActive) {
                label.setIcon(IconUtil.create("icons/check.svg", 16, 16));
                // Môi trường kích hoạt sử dụng phông chữ đậm
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            } else {
                label.setIcon(IconUtil.createThemed("icons/nocheck.svg", 16, 16));
                // Môi trường bình thường sử dụng phông chữ thông thường
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
            }

            // Đặt lề và căn chỉnh
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setIconTextGap(10); // tăng lêniconvà khoảng cách văn bản
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setHorizontalTextPosition(SwingConstants.RIGHT);

            // Phong cách của trạng thái được chọn được nâng cao
            if (isSelected) {
                label.setOpaque(true);
            }
        }

        return label;
    }

}