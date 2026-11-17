package com.laker.postman.common.themes;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Easy Postman Chủ đề tối tùy chỉnh (kế thừa từ  FlatDarkLaf）。
 * <p>
 * Tệp cấu hình:com/laker/postman/common/themes/EasyDarkLaf.properties
 * <p>
 * tham khảo:<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf Tài liệu tùy chỉnh</a>
 */
public class EasyDarkLaf extends FlatDarkLaf {
    public static final String NAME = "EasyDarkLaf";

    public static boolean setup() {
        return setup(new EasyDarkLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "EasyDarkLaf Look and Feel";
    }
}
