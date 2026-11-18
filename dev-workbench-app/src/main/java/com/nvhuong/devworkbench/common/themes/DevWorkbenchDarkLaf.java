package com.nvhuong.devworkbench.common.themes;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * DevWorkbench DevWorkbench Chủ đề tối tùy chỉnh (kế thừa từ  FlatDarkLaf）。
 * <p>
 * Tệp cấu hình:com/nvhuong/devworkbench/common/themes/DevWorkbenchDarkLaf.properties
 * <p>
 * tham khảo:<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf Tài liệu tùy chỉnh</a>
 */
public class DevWorkbenchDarkLaf extends FlatDarkLaf {
    public static final String NAME = "DevWorkbenchDarkLaf";

    public static boolean setup() {
        return setup(new DevWorkbenchDarkLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "DevWorkbenchDarkLaf Look and Feel";
    }
}
