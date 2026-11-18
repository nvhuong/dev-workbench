package com.nvhuong.devworkbench.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

/**
 * Lớp công cụ biểu tượng
 * Quản lý thống nhất tất cả các dự án SVG Tạo và cấu hình màu sắc của các biểu tượng
 *
 * <h2>Ví dụ sử dụng</h2>
 * <pre>{@code
 * // Tạo các biểu tượng phù hợp với chủ đề (được khuyến nghị, phù hợp với mọi tình huống)
 * FlatSVGIcon icon = IconUtil.createThemed("icons/save.svg", 20, 20);
 *
 * // Tạo biểu tượng bằng màu chính (để lựa chọn/trạng thái kích hoạt)
 * FlatSVGIcon icon = IconUtil.createPrimary("icons/check.svg", 16, 16);
 *
 * // Tạo biểu tượng trên nền màu chính (đối với  primary nút, trạng thái đã chọn, v.v.)
 * FlatSVGIcon icon = IconUtil.createOnPrimary("icons/import.svg", 16, 16);
 *
 * // Tạo biểu tượng màu tùy chỉnh
 * FlatSVGIcon icon = IconUtil.createColored("icons/error.svg", 16, 16, Color.RED);
 *
 * // Sử dụng hằng số kích thước
 * FlatSVGIcon icon = IconUtil.createThemed("icons/save.svg", SIZE_MEDIUM, SIZE_MEDIUM);
 * }</pre>
 */
@UtilityClass
public class IconUtil {

    // ==================== Phương pháp tạo lõi ====================

    /**
     * Tạo cơ bản SVG biểu tượng (không có bộ lọc màu)
     *
     * @param iconPath Đường dẫn biểu tượng (ví dụ "icons/save.svg"）
     * @param width    Chiều rộng biểu tượng
     * @param height   Chiều cao biểu tượng
     * @return SVG biểu tượng
     */
    public static FlatSVGIcon create(String iconPath, int width, int height) {
        return create(iconPath, width, height, null);
    }

    public static FlatSVGIcon create(String iconPath, int width, int height, ClassLoader classLoader) {
        return new FlatSVGIcon(iconPath, width, height, resolveClassLoader(classLoader));
    }

    private static ClassLoader resolveClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return IconUtil.class.getClassLoader();
    }

    /**
     * Tạo biểu tượng thích ứng với chủ đề (được khuyến nghị)
     * <p>Màu biểu tượng sẽ tự động theo màu văn bản của chủ đề hiện tại:
     * <ul>
     *   <li>Chủ đề sáng: biểu tượng tối</li>
     *   <li>Chủ đề tối: biểu tượng sáng</li>
     * </ul>
     * <p>Áp dụng cho tất cả các tình huống phổ biến như nút, nhãn, bảng, v.v
     *
     * @param iconPath Đường dẫn biểu tượng (ví dụ "icons/save.svg"）
     * @param width    Chiều rộng biểu tượng
     * @param height   Chiều cao biểu tượng
     * @return Bộ lọc màu được cấu hình SVG biểu tượng
     */
    public static FlatSVGIcon createThemed(String iconPath, int width, int height) {
        return createThemed(iconPath, width, height, null);
    }

    public static FlatSVGIcon createThemed(String iconPath, int width, int height, ClassLoader classLoader) {
        return create(iconPath, width, height, classLoader)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color ->
                        UIManager.getColor("Label.foreground")));
    }

    /**
     * Tạo các biểu tượng sử dụng màu sắc chủ đạo
     * <p>Phù hợp với các biểu tượng cần làm nổi bật như: .
     * <ul>
     *   <li>Biểu tượng trạng thái đã chọn</li>
     *   <li>Biểu tượng tab đang hoạt động</li>
     *   <li>Các biểu tượng chức năng cần được nhấn mạnh</li>
     * </ul>
     *
     * @param iconPath Đường dẫn biểu tượng (ví dụ "icons/check.svg"）
     * @param width    Chiều rộng biểu tượng
     * @param height   Chiều cao biểu tượng
     * @return Bộ lọc màu được cấu hình SVG biểu tượng
     */
    public static FlatSVGIcon createPrimary(String iconPath, int width, int height) {
        return createPrimary(iconPath, width, height, null);
    }

    public static FlatSVGIcon createPrimary(String iconPath, int width, int height, ClassLoader classLoader) {
        return create(iconPath, width, height, classLoader)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getPrimary()));
    }

    /**
     * Tạo biểu tượng để sử dụng trên nền màu chính.
     * <p>Màu sắc sử dụng chủ đề văn bản nghịch đảo, phù hợp primary Các cảnh như nút và nút đã chọn không chuyển đổi với màu nền trước thông thường.</p>
     *
     * @param iconPath Đường dẫn biểu tượng (ví dụ "icons/import.svg"）
     * @param width    Chiều rộng biểu tượng
     * @param height   Chiều cao biểu tượng
     * @return Được cấu hình với bộ lọc văn bản chống màu SVG biểu tượng
     */
    public static FlatSVGIcon createOnPrimary(String iconPath, int width, int height) {
        return createOnPrimary(iconPath, width, height, null);
    }

    public static FlatSVGIcon createOnPrimary(String iconPath, int width, int height, ClassLoader classLoader) {
        return create(iconPath, width, height, classLoader)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextInverse()));
    }

    /**
     * Tạo biểu tượng bằng cách sử dụng màu sắc tùy chỉnh
     * <p>Được sử dụng cho những cảnh yêu cầu màu sắc cụ thể, chẳng hạn như:
     * <ul>
     *   <li>Biểu tượng thành công (màu xanh lá cây)</li>
     *   <li>Biểu tượng cảnh báo (màu cam)</li>
     *   <li>Biểu tượng lỗi (màu đỏ)</li>
     * </ul>
     *
     * @param iconPath    Đường dẫn biểu tượng (ví dụ "icons/error.svg"）
     * @param width       Chiều rộng biểu tượng
     * @param height      Chiều cao biểu tượng
     * @param customColor Màu sắc tùy chỉnh
     * @return Bộ lọc màu được cấu hình SVG biểu tượng
     */
    public static FlatSVGIcon createColored(String iconPath, int width, int height, Color customColor) {
        return createColored(iconPath, width, height, customColor, null);
    }

    public static FlatSVGIcon createColored(String iconPath, int width, int height, Color customColor, ClassLoader classLoader) {
        return create(iconPath, width, height, classLoader)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> customColor));
    }

    // ==================== Hằng số kích thước biểu tượng thường được sử dụng ====================

    /**
     * Kích thước biểu tượng nhỏ (16x16）
     * <p>Áp dụng cho: biểu tượng mục danh sách, biểu tượng thanh công cụ nhỏ, biểu tượng con mắt, v.v
     */
    public static final int SIZE_SMALL = 16;

    /**
     * Kích thước biểu tượng trung bình (20x20）
     * <p>Áp dụng cho: nút biểu tượng, lưu/Nút xuất, v.v
     */
    public static final int SIZE_MEDIUM = 20;

    /**
     * Kích thước biểu tượng lớn (24x24）
     * <p>Áp dụng cho: biểu tượng giao thức (HTTP/WebSocket/SSE）、Biểu tượng nút lớn và hơn thế nữa
     */
    public static final int SIZE_LARGE = 24;

    /**
     * Kích thước biểu tượng thanh tab (20x20）
     * <p>Áp dụng cho: Biểu tượng tab Sidebar
     */
    public static final int SIZE_TAB = 20;
}
