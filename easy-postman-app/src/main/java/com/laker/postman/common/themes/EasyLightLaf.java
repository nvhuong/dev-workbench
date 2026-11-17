package com.laker.postman.common.themes;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Easy Postman Chủ đề tùy chỉnh (được kế thừa từ  FlatLightLaf）。
 * <p>
 * Tệp cấu hình:com/laker/postman/common/themes/EasyLightLaf.properties
 * <p>
 * tham khảo:<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf Tài liệu tùy chỉnh</a>
 */
public class EasyLightLaf extends FlatLightLaf {
    public static final String NAME = "EasyLightLaf";

    public static boolean setup() {
        return setup(new EasyLightLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "EasyLightLaf Look and Feel";
    }
}
