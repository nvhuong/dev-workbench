package com.nvhuong.devworkbench.plugin.kafka.shared.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Nhãn có màu nền trước được thay đổi linh hoạt theo màu ngữ nghĩa: sáng/Không cần thiết lập lại sau khi chuyển sang chủ đề tối setForeground，Màu chủ đề hiện tại được lấy khi vẽ.
 */
public class ThemedLabel extends JLabel {

    private transient Supplier<Color> foregroundSupplier;

    public ThemedLabel(String text, Supplier<Color> foregroundSupplier) {
        super(text);
        this.foregroundSupplier = foregroundSupplier;
    }

    public void setForegroundSupplier(Supplier<Color> foregroundSupplier) {
        this.foregroundSupplier = foregroundSupplier;
        repaint();
    }

    @Override
    public Color getForeground() {
        Supplier<Color> supplier = foregroundSupplier;
        Color color = supplier == null ? null : supplier.get();
        return color != null ? color : super.getForeground();
    }
}
