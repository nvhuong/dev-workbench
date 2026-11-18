package com.nvhuong.devworkbench.common.themes;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * DevWorkbench DevWorkbench Chủ đề tùy chỉnh (được kế thừa từ  FlatLightLaf）。
 * <p>
 * Tệp cấu hình:com/nvhuong/devworkbench/common/themes/DevWorkbenchLightLaf.properties
 * <p>
 * tham khảo:<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf Tài liệu tùy chỉnh</a>
 */
public class DevWorkbenchLightLaf extends FlatLightLaf {
    public static final String NAME = "DevWorkbenchLightLaf";

    public static boolean setup() {
        return setup(new DevWorkbenchLightLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "DevWorkbenchLightLaf Look and Feel";
    }
}
