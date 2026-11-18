package com.nvhuong.devworkbench.util;

import com.nvhuong.devworkbench.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Locale;

/**
 * Công cụ quản lý phông chữ
 * Chịu trách nhiệm áp dụng cài đặt phông chữ chung
 * <p>
 * Lưu ý quan trọng - Về emoji Vấn đề hiển thị:
 * Trong tất cả các hệ điều hành,emoji Việc hiển thị phụ thuộc vào chuỗi hạ cấp phông chữ (font fallback chain）。
 * Khi phông chữ chính không thể được hiển thị emoji ký tự, hệ thống sẽ tự động hạ cấp xuống ký tự tương ứng emoji Phông chữ: .
 * <ul>
 *   <li>Windows: Segoe UI Emoji</li>
 *   <li>macOS: Apple Color Emoji</li>
 *   <li>Linux: Noto Color Emoji</li>
 * </ul>
 * <p>
 * sử dụng new Font() Tạo phông chữ trực tiếp sẽ làm mất chuỗi hạ cấp, dẫn đến emoji Không thể hiển thị.
 * Phải được sử dụng deriveFont() Phương pháp bắt nguồn từ phông chữ hệ thống để bảo toàn chuỗi hạ cấp phông chữ.
 */
@Slf4j
@UtilityClass
public class FontManager {

    private static final String DEFAULT_FONT_KEY = "defaultFont";
    private static final String LABEL_FONT_KEY = "Label.font";
    private static volatile Font lookAndFeelDefaultFont;

    /**
     * Áp dụng cài đặt phông chữ đã lưu cho toàn bộ ứng dụng
     */
    public static void applyFontSettings() {
        try {
            installSavedFontDefaults();
            updateExistingWindows();
            log.info("Font applied successfully");
        } catch (Exception e) {
            log.error("Failed to apply font settings", e);
        }
    }

    /**
     * Áp dụng phông chữ được chỉ định cho toàn bộ ứng dụng
     *
     * @param fontName Tên phông chữ, chuỗi trống nghĩa là sử dụng mặc định của hệ thống
     * @param fontSize Cỡ chữ
     */
    public static void applyFont(String fontName, int fontSize) {
        try {
            installFontDefaults(fontName, fontSize);

            // Cập nhật tất cả các cửa sổ hiện có
            updateExistingWindows();

            log.info("Font applied successfully");
        } catch (Exception e) {
            log.error("Failed to apply font settings", e);
        }
    }

    /**
     * Cài đặt cài đặt phông chữ đã lưu vào FlatLaf defaultFont，Đừng làm mới cửa sổ ngay lập tức.
     * Công tắc chủ đề sẽ được xây dựng lại UI defaults，Vì vậy, chuyển đổi Look and Feel Cuối cùng, bạn cần cài đặt lại các phông chữ rồi làm mới chúng một cách thống nhất.
     */
    public static Font installSavedFontDefaults() {
        String fontName = resolveAllowedSavedUiFontName(SettingManager.getUiFontName(), I18nUtil.currentLocale());
        return installFontDefaults(fontName, SettingManager.getUiFontSize());
    }

    /**
     * Nắm bắt hiện tại Look and Feel Bản gốc sau khi cài đặt UI phông chữ.
     * <p>
     * FlatLaf Chuyển đổi chủ đề sẽ xây dựng lại UIDefaults。Sau khi người dùng chuyển sang phông chữ tùy chỉnh.Label.font/defaultFont
     * Có thể đã bị nhiễm phông chữ tùy chỉnh; chọn lại“Mặc định hệ thống”Không thể tiếp tục từ thời điểm hiện tại Label.font Bắt nguồn,
     * Nếu không, các phông chữ vật lý không hỗ trợ tiếng Trung sẽ tiếp tục được giữ lại.
     */
    public static void captureLookAndFeelDefaultFont() {
        lookAndFeelDefaultFont = resolveCurrentLookAndFeelFont();
    }

    /**
     * Cài đặt FlatLaf Đề xuất toàn cầu defaultFont，Đừng làm mới cửa sổ ngay lập tức.
     *
     * @param fontName Tên phông chữ, chuỗi trống nghĩa là sử dụng mặc định của hệ thống
     * @param fontSize Cỡ chữ
     * @return Đã cài đặt phông chữ
     */
    public static Font installFontDefaults(String fontName, int fontSize) {
        log.info("Installing font defaults: {} with size: {}", fontName, fontSize);
        Font newFont = createFontWithFallback(fontName, Font.PLAIN, fontSize);
        UIManager.put(DEFAULT_FONT_KEY, new FontUIResource(newFont));
        return newFont;
    }

    /**
     * Tạo phông chữ, giữ lại chuỗi hạ cấp phông chữ để được hỗ trợ emoji và các ký tự đặc biệt khác
     * <p>
     * Các chiến lược triển khai chính:
     * <ol>
     *   <li>Ưu tiên sử dụng deriveFont() Bắt nguồn từ phông chữ hệ thống, bảo toàn chuỗi hạ cấp phông chữ</li>
     *   <li>Chỉ được sử dụng nếu người dùng đã chỉ định một phông chữ tùy chỉnh khác new Font()</li>
     *   <li>sử dụng new Font() Chuỗi hạ cấp sẽ bị mất, dẫn đến emoji Shows the problem (a warning log will be logged)</li>
     * </ol>
     *
     * @param fontName Tên phông chữ, chuỗi trống nghĩa là sử dụng mặc định của hệ thống
     * @param style    Kiểu phông chữ (Font.PLAIN, Font.BOLD, Font.ITALIC hoặc sự kết hợp của chúng)
     * @param size     Cỡ chữ
     * @return Đã tạo phông chữ
     */
    @SuppressWarnings("MagicConstant") // style Các thông số là int loại, nhưng giá trị thực tế là Font các hằng số kiểu dáng
    private static Font createFontWithFallback(String fontName, int style, float size) {
        Font systemDefaultFont = resolveSystemDefaultFont();

        // các bước2: Được sử dụng nếu không có phông chữ nào được chỉ định hoặc nếu phông chữ mặc định của hệ thống được chỉ định deriveFont Giữ chuỗi hạ cấp
        if (fontName == null || fontName.isEmpty() ||
                systemDefaultFont.getName().equals(fontName) || systemDefaultFont.getFamily().equals(fontName)) {
            // ✅ Cách tiếp cận đúng: sử dụng deriveFont Giữ nguyên chuỗi hạ cấp phông chữ, được hỗ trợ emoji
            return systemDefaultFont.deriveFont(style, size);
        }

        // các bước3: Người dùng chỉ định phông chữ tùy chỉnh, tạo một phiên bản phông chữ mới
        // ⚠️ Cảnh báo: trực tiếp new Font() Chuỗi hạ cấp phông chữ sẽ bị mất
        // Điều này có thể dẫn đến emoji trên tất cả các nền tảng (Windows/macOS/Linux）Những điều sau đây có thể xuất hiện dưới dạng hộp
        // Người dùng nên sử dụng phông chữ mặc định của hệ thống để có kết quả tối ưu emoji và hỗ trợ ký tự đa ngôn ngữ
        log.warn("Creating font '{}' will lose font fallback chain, emoji and some Unicode characters may not display correctly",
                 fontName);
        return new Font(fontName, style, (int) size);
    }

    private static Font resolveSystemDefaultFont() {
        Font capturedFont = lookAndFeelDefaultFont;
        if (capturedFont != null) {
            return capturedFont;
        }

        Font currentLookAndFeelFont = resolveCurrentLookAndFeelFont();
        lookAndFeelDefaultFont = currentLookAndFeelFont;
        return currentLookAndFeelFont;
    }

    public static String resolveAllowedUiFontNameForLocale(String fontName, Locale locale) {
        if (fontName == null || fontName.isBlank()) {
            return "";
        }
        UiFontCatalog.FontSupport support = UiFontCatalog.inspectFamily(fontName);
        return resolveAllowedUiFontName(fontName, locale, support);
    }

    private static String resolveAllowedSavedUiFontName(String fontName, Locale locale) {
        return resolveAllowedUiFontNameForLocale(fontName, locale);
    }

    static String resolveAllowedUiFontName(String fontName, Locale locale, UiFontCatalog.FontSupport support) {
        if (fontName == null || fontName.isBlank()) {
            return "";
        }
        if (UiFontCatalog.isUiFontAllowedForLocale(support, locale)) {
            return fontName;
        }
        log.warn("Ignoring saved UI font '{}' because it is unsafe for locale {}", fontName, locale);
        return "";
    }

    private static Font resolveCurrentLookAndFeelFont() {
        Font baseFont = UIManager.getFont(LABEL_FONT_KEY);
        if (baseFont == null) {
            baseFont = UIManager.getFont(DEFAULT_FONT_KEY);
        }
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        return baseFont == null ? new Font(Font.DIALOG, Font.PLAIN, SettingManager.getUiFontSize()) : baseFont;
    }

    /**
     * Cập nhật tất cả các cửa sổ hiện có
     */
    private static void updateExistingWindows() {
        // Cập nhật tất cả các cửa sổ bằng trình quản lý làm mới thống nhất
        UIRefreshManager.refreshAllWindows();
    }

    /**
     * Cập nhật đệ quy phông chữ của một thành phần và các thành phần phụ của nó
     * <p>
     * ⚠️ Lưu ý:
     * <ul>
     *   <li>Phương pháp này sẽ ghi đè trực tiếp lên phông chữ của thành phần, có thể làm mất chuỗi hạ cấp phông chữ</li>
     *   <li>Nên sử dụng nó đầu tiên {@link #applyFont(String, int)} để cập nhật phông chữ trên toàn cầu</li>
     *   <li>Phương pháp này phù hợp với các tình huống mà bạn cần kiểm soát riêng phông chữ của một thành phần cụ thể</li>
     * </ul>
     *
     * @param component Thành phần cần cập nhật
     * @param font      Phông chữ mới (được khuyến nghị deriveFont Được tạo để duy trì chuỗi hạ cấp)
     */
    public static void updateComponentFont(Component component, Font font) {
        if (component == null || font == null) {
            return;
        }

        component.setFont(font);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                updateComponentFont(child, font);
            }
        }
    }
}
