package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Lớp công cụ phông chữ cung cấp phông chữ mặc định của hệ thống và giữ lại chuỗi hạ cấp phông chữ hoàn chỉnh để được hỗ trợ emoji và các ký tự đặc biệt khác
 */
@UtilityClass
public class FontsUtil {

    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 32;
    private static final int DEFAULT_FONT_SIZE = 13;
    private static final String DEFAULT_FONT_KEY = "defaultFont";
    private static final String LABEL_FONT_KEY = "Label.font";
    private static final String MONOSPACED_FONT_KEY = "monospaced.font";

    private static int getBaseFontSize() {
        return getBaseUiFont().getSize();
    }

    private static Font getBaseUiFont() {
        Font baseFont = UIManager.getFont(DEFAULT_FONT_KEY);
        if (baseFont == null) {
            baseFont = UIManager.getFont(LABEL_FONT_KEY);
        }
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        return baseFont == null ? new Font(Font.DIALOG, Font.PLAIN, DEFAULT_FONT_SIZE) : baseFont;
    }

    /**
     * Nhận phông chữ mặc định, từ  UIManager Bắt nguồn từ việc duy trì chuỗi hạ cấp, được hỗ trợ emoji và các ký tự đặc biệt khác
     *
     * @param style Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size  Cỡ chữ
     * @return Font Đối tượng, bắt nguồn từ phông chữ mặc định của hệ thống
     */
    private static Font getDefaultFont(int style, int size) {
        return getBaseUiFont().deriveFont(style, (float) size);
    }

    /**
     * Lấy phông chữ mặc định, sử dụng cỡ chữ do người dùng đặt
     * Từ UIManager Bắt nguồn từ việc duy trì chuỗi hạ cấp, được hỗ trợ emoji và các ký tự đặc biệt khác
     *
     * @param style Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @return Font Đối tượng, sử dụng cỡ chữ do người dùng đặt
     */
    public static Font getDefaultFont(int style) {
        return getDefaultFont(style, getBaseFontSize());
    }

    /**
     * Lấy phông chữ mặc định, sử dụng độ lệch tương ứng với kích thước phông chữ do người dùng đặt
     * Ví dụ: Nếu người dùng đặt phông chữ thành  14，offset vì -2，Sau đó quay trở lại 12 cỡ chữ
     *
     * @param style  Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param offset Độ lệch (có thể dương hoặc âm) so với kích thước phông chữ do người dùng đặt
     * @return Font đối tượng
     */
    public static Font getDefaultFontWithOffset(int style, int offset) {
        int fontSize = getBaseFontSize() + offset;
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        return getDefaultFont(style, fontSize);
    }

    /**
     * Nhận phông chữ có chiều rộng cố định, với kích thước phông chữ theo người dùng UI Cài đặt kích thước phông chữ.
     *
     * @param style  Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param offset Độ lệch (có thể dương hoặc âm) so với kích thước phông chữ do người dùng đặt
     * @return Font đối tượng
     */
    /**
     * Được gọi trước văn bản tự vẽ: Sử dụng cài đặt khử răng cưa cho văn bản trên màn hình hệ thống (khử răng cưa thang độ xám được bật theo mặc định) và Swing Kết xuất văn bản thành phần vẫn nhất quán.
     */
    public static void applyTextRenderingHints(Graphics2D g2) {
        Object desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map<?, ?> hints) {
            g2.addRenderingHints(hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    public static Font getMonospacedFontWithOffset(int style, int offset) {
        Font baseFont = UIManager.getFont(MONOSPACED_FONT_KEY);
        if (baseFont == null) {
            baseFont = new Font(Font.MONOSPACED, Font.PLAIN, getBaseFontSize());
        }
        int fontSize = getBaseFontSize() + offset;
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        return baseFont.deriveFont(style, (float) fontSize);
    }
}
