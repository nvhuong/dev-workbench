package com.laker.postman.common.constants;

import com.formdev.flatlaf.FlatLaf;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
import java.awt.*;

/**
 * hiện đại hóaUICách phối màu
 * Các hằng số khớp màu thống nhất đảm bảo tính nhất quán về mặt hình ảnh trên toàn bộ ứng dụng
 * Hỗ trợ đầy đủ việc thích ứng chủ đề sáng và tối
 * <p>
 * Ý tưởng thiết kế:
 * - Màu chủ đạo:Blue（Niềm tin, tính chuyên nghiệp, công nghệ)
 * - Màu phụ: .Sky Blue（Mới mẻ và hiện đại)
 * - Màu nhấn:Cyan（sức sống, sự nổi bật)
 * - Màu sắc trung tính:Slate（Thanh lịch và dễ đọc)
 * <p>
 * - Nền, văn bản, đường viền, v.vUITất cả các màu của phần tử đều thích ứng với chủ đề
 * Màu nền của bảng điều khiển (chủ đề tối - ranh giới bên ngoài)38, 40, 44
 * @panelBg=#26282c
 * Màu nền của bảng điều khiển (chủ đề nhẹ - Màu nền chính là xám nhạt)
 * @panelBg=#e9eaee
 *
 */
@UtilityClass
public final class ModernColors {

    // ==================== Phát hiện chủ đề ====================

    /**
     * Kiểm tra xem chủ đề tối hiện tại có tồn tại không
     * @return true Nếu chủ đề tối hiện tại là.false Nếu đó là một chủ đề màu sắc tươi sáng
     */
    public static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    private static Color color(String key, Color lightFallback, Color darkFallback) {
        return ThemeColors.color(key, isDarkTheme() ? darkFallback : lightFallback);
    }

    private static Color color(String key, Color fallback) {
        return ThemeColors.color(key, fallback);
    }

    private static Color uiColor(String key, Color lightFallback, Color darkFallback) {
        Color color = UIManager.getColor(key);
        if (color != null) {
            return ThemeColors.explicit(color);
        }
        return ThemeColors.explicit(isDarkTheme() ? darkFallback : lightFallback);
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return ThemeColors.explicit(color != null ? color : fallback);
    }

    // ==================== Màu sắc chính ====================

    /**
     * Màu chính - IDEA-inspired Blue
     */
    public static final Color PRIMARY = new Color(55, 113, 225);

    /**
     * Màu chủ đạo là màu tối - IDEA-inspired Blue-600
     */
    public static final Color PRIMARY_DARK = new Color(47, 98, 201);

    /**
     * Màu sắc chủ đạo là siêu tối - IDEA-inspired Blue-700
     */
    public static final Color PRIMARY_DARKER = new Color(40, 84, 173);

    /**
     * Màu sắc chủ đạo là ánh sáng - IDEA-inspired Blue-400
     */
    public static final Color PRIMARY_LIGHT = new Color(94, 143, 240);

    /**
     * Màu sắc chủ đạo là siêu nhẹ - IDEA-inspired Blue-100
     */
    public static final Color PRIMARY_LIGHTER = new Color(212, 227, 255);

    // ==================== Màu sắc phụ trợ ====================

    /**
     * Màu thứ cấp - Sky-500
     */
    public static final Color SECONDARY = new Color(14, 165, 233);

    /**
     * Màu thứ cấp tối - Sky-600
     */
    public static final Color SECONDARY_DARK = new Color(2, 132, 199);

    /**
     * Ánh sáng màu thứ cấp - Sky-400
     */
    public static final Color SECONDARY_LIGHT = new Color(56, 189, 248);

    /**
     * Màu thứ cấp siêu nhẹ - Sky-100
     */
    public static final Color SECONDARY_LIGHTER = new Color(224, 242, 254);

    // ==================== Màu sắc nhấn mạnh ====================

    /**
     * Màu nhấn - Cyan-500
     */
    public static final Color ACCENT = new Color(6, 182, 212);

    /**
     * Màu sắc nhấn là ánh sáng - Cyan-400
     */
    public static final Color ACCENT_LIGHT = new Color(34, 211, 238);

    // ==================== thành công/sai lầm/Màu cảnh báo ====================

    /**
     * thành công - IDEA-like muted green.
     */
    public static final Color SUCCESS = new Color(87, 150, 92);

    /**
     * Tối thành công - IDEA-like muted green.
     */
    public static final Color SUCCESS_DARK = new Color(78, 143, 85);

    /**
     * sai lầm - IDEA-like muted red.
     */
    public static final Color ERROR = new Color(219, 88, 96);

    /**
     * Sai màu tối - IDEA-like muted red.
     */
    public static final Color ERROR_DARK = new Color(199, 84, 80);

    /**
     * Lỗi rất sâu sắc - IDEA-like muted red.
     */
    public static final Color ERROR_DARKER = new Color(166, 69, 69);

    /**
     * Cảnh báo - IDEA-like muted amber.
     */
    public static final Color WARNING = new Color(194, 158, 73);

    /**
     * Cảnh báo màu tối - IDEA-like muted amber.
     */
    public static final Color WARNING_DARK = new Color(168, 135, 63);

    /**
     * Cảnh báo siêu sâu - IDEA-like muted amber.
     */
    public static final Color WARNING_DARKER = new Color(138, 109, 50);

    /**
     * thông tin - IDEA-like cyan.
     */
    public static final Color INFO = new Color(79, 174, 208);

    // ==================== Màu sắc trung tính (cho Close v.v.)====================

    /**
     * trung lập - Slate-500
     */
    public static final Color NEUTRAL = new Color(100, 116, 139);

    /**
     * Màu tối trung tính - Slate-600
     */
    public static final Color NEUTRAL_DARK = new Color(71, 85, 105);

    /**
     * Trung tính cực sâu - Slate-700
     */
    public static final Color NEUTRAL_DARKER = new Color(51, 65, 85);

    // ==================== Git Màu sắc hoạt động (phong cách tối giản hiện đại)====================

    /**
     * Git Commit（Gửi)- Màu xanh ngọc dịu dàng
     */
    public static final Color GIT_COMMIT = SUCCESS;

    /**
     * Git Push（đẩy)- Màu xanh đậm sang trọng
     */
    public static final Color GIT_PUSH = new Color(59, 130, 246);  // Blue-500

    /**
     * Git Pull（kéo)- Màu tím tươi
     */
    public static final Color GIT_PULL = new Color(168, 85, 247);  // Purple-500

    // ==================== Màu sắc đa dạng ====================

    public static final Color VARIABLE_CONTEXT = WARNING;
    public static final Color VARIABLE_ITERATION_DATA = new Color(25, 118, 210);
    public static final Color VARIABLE_GROUP = new Color(77, 182, 172);
    public static final Color VARIABLE_ENVIRONMENT = SUCCESS;
    public static final Color VARIABLE_GLOBAL = new Color(67, 56, 202);
    public static final Color VARIABLE_BUILT_IN = new Color(156, 39, 176);

    public static Color getPrimary() {
        return color(ThemeColors.PRIMARY, PRIMARY);
    }

    public static Color getPrimaryDark() {
        return color(ThemeColors.PRIMARY_DARK, PRIMARY_DARK);
    }

    public static Color getPrimaryDarker() {
        return color(ThemeColors.PRIMARY_DARKER, PRIMARY_DARKER);
    }

    public static Color getPrimaryLight() {
        return color(ThemeColors.PRIMARY_LIGHT, PRIMARY_LIGHT);
    }

    public static Color getPrimaryLighter() {
        return color(ThemeColors.PRIMARY_LIGHTER, PRIMARY_LIGHTER);
    }

    public static Color getSecondary() {
        return color(ThemeColors.SECONDARY, SECONDARY);
    }

    public static Color getSecondaryDark() {
        return color(ThemeColors.SECONDARY_DARK, SECONDARY_DARK);
    }

    public static Color getSecondaryLight() {
        return color(ThemeColors.SECONDARY_LIGHT, SECONDARY_LIGHT);
    }

    public static Color getSecondaryLighter() {
        return color(ThemeColors.SECONDARY_LIGHTER, SECONDARY_LIGHTER);
    }

    public static Color getAccent() {
        return color(ThemeColors.ACCENT, ACCENT);
    }

    public static Color getAccentLight() {
        return color(ThemeColors.ACCENT_LIGHT, ACCENT_LIGHT);
    }

    public static Color getSuccess() {
        return color(ThemeColors.SUCCESS, SUCCESS);
    }

    public static Color getSuccessDark() {
        return color(ThemeColors.SUCCESS_DARK, SUCCESS_DARK);
    }

    public static Color getError() {
        return color(ThemeColors.ERROR, ERROR);
    }

    public static Color getErrorDark() {
        return color(ThemeColors.ERROR_DARK, ERROR_DARK);
    }

    public static Color getErrorDarker() {
        return color(ThemeColors.ERROR_DARKER, ERROR_DARKER);
    }

    public static Color getWarning() {
        return color(ThemeColors.WARNING, WARNING);
    }

    public static Color getWarningDark() {
        return color(ThemeColors.WARNING_DARK, WARNING_DARK);
    }

    public static Color getWarningDarker() {
        return color(ThemeColors.WARNING_DARKER, WARNING_DARKER);
    }

    public static Color getInfo() {
        return color(ThemeColors.INFO, INFO);
    }

    public static Color getNeutral() {
        return color(ThemeColors.NEUTRAL, NEUTRAL);
    }

    public static Color getNeutralDark() {
        return color(ThemeColors.NEUTRAL_DARK, NEUTRAL_DARK);
    }

    public static Color getNeutralDarker() {
        return color(ThemeColors.NEUTRAL_DARKER, NEUTRAL_DARKER);
    }

    public static Color getGitCommit() {
        return color(ThemeColors.GIT_COMMIT, GIT_COMMIT);
    }

    public static Color getGitPush() {
        return color(ThemeColors.GIT_PUSH, GIT_PUSH);
    }

    public static Color getGitPull() {
        return color(ThemeColors.GIT_PULL, GIT_PULL);
    }

    public static Color getVariableContextColor() {
        return color(ThemeColors.VARIABLE_CONTEXT, VARIABLE_CONTEXT);
    }

    public static Color getVariableIterationDataColor() {
        return color(ThemeColors.VARIABLE_ITERATION_DATA, VARIABLE_ITERATION_DATA);
    }

    public static Color getVariableGroupColor() {
        return color(ThemeColors.VARIABLE_GROUP, VARIABLE_GROUP);
    }

    public static Color getVariableEnvironmentColor() {
        return color(ThemeColors.VARIABLE_ENVIRONMENT, VARIABLE_ENVIRONMENT);
    }

    public static Color getVariableGlobalColor() {
        return color(ThemeColors.VARIABLE_GLOBAL, VARIABLE_GLOBAL);
    }

    public static Color getVariableBuiltInColor() {
        return color(ThemeColors.VARIABLE_BUILT_IN, VARIABLE_BUILT_IN);
    }

    // ==================== HTTP phương pháp/Màu giao thức ====================

    public static Color getHttpMethodGet() {
        return color(ThemeColors.HTTP_METHOD_GET, new Color(0x2E7D32), new Color(0x6AAB73));
    }

    public static Color getHttpMethodPost() {
        return color(ThemeColors.HTTP_METHOD_POST, new Color(0xA16207), new Color(0xD5A945));
    }

    public static Color getHttpMethodPut() {
        return color(ThemeColors.HTTP_METHOD_PUT, new Color(0x1565C0), new Color(0x589DF6));
    }

    public static Color getHttpMethodPatch() {
        return color(ThemeColors.HTTP_METHOD_PATCH, new Color(0x00838F), new Color(0x2AACB8));
    }

    public static Color getHttpMethodDelete() {
        return color(ThemeColors.HTTP_METHOD_DELETE, new Color(0xC62828), new Color(0xF06A6A));
    }

    public static Color getHttpMethodDefault() {
        return color(ThemeColors.HTTP_METHOD_DEFAULT, new Color(0x475569), new Color(0x9AA0AA));
    }

    public static Color getHttpProtocolWs() {
        return color(ThemeColors.HTTP_PROTOCOL_WS, new Color(0x00838F), new Color(0x52C7D9));
    }

    public static Color getHttpProtocolSse() {
        return color(ThemeColors.HTTP_PROTOCOL_SSE, new Color(0x00796B), new Color(0x4DB6AC));
    }

    // ==================== Màu sắc logo đa dạng ====================

    /**
     * Màu nền logo của biến được xác định.
     * Ở lại với EasyTextField / RequestBodyPanel Tầm nhìn hiện tại là nhất quán.
     */
    public static Color getDefinedVariableBadgeBackground() {
        return new Color(180, 210, 255, 120);
    }

    /**
     * Màu đường viền logo cho biến được xác định.
     */
    public static Color getDefinedVariableBadgeBorder() {
        return new Color(80, 150, 255);
    }

    /**
     * Màu nền logo của biến không xác định.
     */
    public static Color getUndefinedVariableBadgeBackground() {
        return new Color(255, 200, 200, 120);
    }

    /**
     * Màu đường viền logo của biến không xác định.
     */
    public static Color getUndefinedVariableBadgeBorder() {
        return new Color(255, 100, 100);
    }

    // ==================== Màu văn bản (thích ứng chủ đề)====================

    /**
     * Màu văn bản chính - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-900 (Văn bản tối)
     * Chủ đề tối:Slate-100 (Văn bản nhẹ nhàng)
     */
    public static Color getTextPrimary() {
        return color(ThemeColors.TEXT_PRIMARY, new Color(15, 23, 42), new Color(201, 204, 211));
    }

    /**
     * Màu phụ của văn bản - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-700
     * Chủ đề tối:Slate-300
     */
    public static Color getTextSecondary() {
        return color(ThemeColors.TEXT_SECONDARY, new Color(51, 65, 85), new Color(185, 189, 197));
    }

    /**
     * Màu nhắc văn bản - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-500
     * Chủ đề tối:Slate-400
     */
    public static Color getTextHint() {
        return color(ThemeColors.TEXT_HINT, new Color(100, 116, 139), new Color(123, 128, 137));
    }

    /**
     * Màu văn bản bị vô hiệu hóa - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-400
     * Chủ đề tối:Slate-600
     */
    public static Color getTextDisabled() {
        return color(ThemeColors.TEXT_DISABLED, new Color(148, 163, 184), new Color(86, 91, 100));
    }

    /**
     * Đảo ngược văn bản (đối với nền tối)- Chủ đề thích ứng
     * Cả hai chủ đề đều trả về văn bản nhẹ
     */
    public static Color getTextInverse() {
        return color(ThemeColors.TEXT_INVERSE, new Color(248, 250, 252));
    }

    // ==================== Màu nền (thích ứng chủ đề)====================

    /**
     * Lấy màu nền chính - Thích ứng với chủ đề
     * Chủ đề tươi sáng:IDEA-like workspace gray (233, 234, 238)
     * Chủ đề tối:IDEA-like workspace chrome (38, 40, 44)
     */
    public static Color getBackgroundColor() {
        return color(ThemeColors.BACKGROUND, new Color(233, 234, 238), new Color(38, 40, 44));
    }

    /**
     * Lấy thẻ/Màu nền vùng - Thích ứng với chủ đề
     * Chủ đề nhẹ: màu trắng (255, 255, 255)
     * Chủ đề tối:IDEA-like inner surface (30, 31, 34)
     */
    public static Color getCardBackgroundColor() {
        return color(ThemeColors.SURFACE, new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * Lấy phần trên cùng của cửa sổ chrome Màu nền - Thanh menu và cửa sổ chính TitlePane đã chia sẻ.
     */
    public static Color getWindowChromeBackgroundColor() {
        return color(ThemeColors.WINDOW_CHROME_BACKGROUND, new Color(233, 234, 238), new Color(38, 40, 44));
    }

    /**
     * Lấy Dialog chrome Màu nền - Chỉ để sử dụng JDialog thanh tiêu đề, nền và footer。
     */
    public static Color getDialogChromeBackgroundColor() {
        return color(ThemeColors.DIALOG_CHROME_BACKGROUND, new Color(247, 248, 249), new Color(30, 31, 34));
    }

    /**
     * Lấy màu nền của hộp nhập liệu - Thích ứng với chủ đề
     * Chủ đề nhẹ: màu trắng (255, 255, 255)
     * Chủ đề tối: Vùng nhập liệu ở màu xám bên ngoài (43, 45, 48)
     */
    public static Color getInputBackgroundColor() {
        return color(ThemeColors.INPUT_BACKGROUND, new Color(255, 255, 255), new Color(43, 45, 48));
    }

    /**
     * Lấy màu nền của trạng thái vô hiệu hóa của hộp nhập liệu.
     * Theo dõi đầu tiên FlatLaf Hộp văn bản chính nó token，Tránh sử dụng lại màu của nút đã bị vô hiệu hóa khiến khối biểu mẫu trở nên quá nặng.
     */
    public static Color getInputDisabledBackgroundColor() {
        return uiColor("TextField.disabledBackground", new Color(241, 243, 246), new Color(40, 42, 46));
    }

    /**
     * Lấy Tab Vùng này không có màu nền được chọn - Thích ứng với chủ đề.
     */
    public static Color getTabBackgroundColor() {
        return color(ThemeColors.TAB_BACKGROUND, new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * Lấy Tab Chọn màu nền - Thích ứng với chủ đề.
     */
    public static Color getTabSelectedBackgroundColor() {
        return color(ThemeColors.TAB_SELECTED_BACKGROUND, new Color(226, 235, 254), new Color(43, 45, 48));
    }

    /**
     * Lấy Tab Màu nền di chuột - Thích ứng với chủ đề.
     */
    public static Color getTabHoverBackgroundColor() {
        return color(ThemeColors.TAB_HOVER_BACKGROUND, new Color(242, 246, 255), new Color(38, 40, 44));
    }

    /**
     * Lấy Tab Màu sắc của đường phân chia khu vực - Thích ứng với chủ đề.
     */
    public static Color getTabSeparatorColor() {
        return color(ThemeColors.TAB_SEPARATOR, new Color(233, 234, 238), new Color(43, 45, 48));
    }

    /**
     * Lấy màu nền di chuột - Thích ứng với chủ đề
     * Chủ đề màu sắc tươi sáng: xám xanh nhạt hover
     * Chủ đề tối: nhạt hơn thẻ một chút
     */
    public static Color getHoverBackgroundColor() {
        return color(ThemeColors.HOVER_BACKGROUND, new Color(239, 244, 255), new Color(43, 45, 48));
    }

    /**
     * Lấy màu nền được chọn phổ quát - Thích ứng với chủ đề
     */
    public static Color getSelectionBackgroundColor() {
        return color(ThemeColors.SELECTION_BACKGROUND, new Color(212, 227, 255), new Color(43, 67, 113));
    }

    /**
     * Lấy màu nền của thân bảng.
     * Đọc đầu tiên FlatLaf tiêu chuẩn Table token，Để tạo điều kiện thống nhất giữa các chủ đề khác nhau.
     */
    public static Color getTableBackgroundColor() {
        return uiColor("Table.background", new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * Lấy màu nền của tiêu đề, đóng IntelliJ IDEA đầu đồng hồ màu xám nhẹ.
     */
    public static Color getTableHeaderBackgroundColor() {
        return uiColor("TableHeader.background", new Color(244, 246, 248), new Color(43, 45, 48));
    }

    /**
     * Lấy lưới bảng/Màu chia.
     */
    public static Color getTableGridColor() {
        return uiColor("Table.gridColor", new Color(232, 235, 239), new Color(43, 45, 48));
    }

    /**
     * Lấy màu nền đã chọn của bảng.
     */
    public static Color getTableSelectionBackgroundColor() {
        return uiColor("Table.selectionBackground", getSelectionBackgroundColor());
    }

    /**
     * Lấy màu văn bản đã chọn của bảng.
     */
    public static Color getTableSelectionForegroundColor() {
        return uiColor("Table.selectionForeground", getTextPrimary());
    }


    /**
     * Lấy nút trạng thái nhấn nút - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-200 (226, 232, 240)
     * Chủ đề tối: Tối hơn nền (34, 36, 39)
     */
    public static Color getButtonPressedColor() {
        return color(ThemeColors.BUTTON_PRESSED_BACKGROUND, new Color(220, 233, 255), new Color(36, 38, 42));
    }

    /**
     * Lấy nền trạng thái nút bị vô hiệu hóa - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-200 (226, 232, 240)
     * Chủ đề tối: màu xám đậm (60, 60, 65)
     */
    public static Color getButtonDisabledBackgroundColor() {
        return color(ThemeColors.BUTTON_DISABLED_BACKGROUND, new Color(226, 232, 240), new Color(40, 42, 46));
    }

    // ==================== Màu viền (thích ứng chủ đề)====================

    /**
     * Lấy màu viền (màu sáng)- Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-200 (226, 232, 240)
     * Dark theme: slightly lighter than the background (59, 61, 66) - tương ứng @borderColor
     */
    public static Color getBorderLightColor() {
        return color(ThemeColors.BORDER_LIGHT, new Color(211, 218, 230), new Color(50, 52, 56));
    }

    /**
     * Lấy màu đường viền (trung bình)- Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-300 (203, 213, 225)
     * Chủ đề tối: đường viền rõ ràng hơn (70, 73, 80)
     */
    public static Color getBorderMediumColor() {
        return color(ThemeColors.BORDER_MEDIUM, new Color(190, 199, 213), new Color(58, 61, 66));
    }

    /**
     * Lấy dải phân cách/Màu viền - Chuyển thể chủ đề
     * Được sử dụng để ngăn, viền, v.v. giữa các bảng
     * Chủ đề nhẹ: xám nhạt (213, 216, 222)
     * Chủ đề tối: Màu xám nhạt hơn nền (49, 51, 56)
     */
    public static Color getDividerBorderColor() {
        return color(ThemeColors.DIVIDER, new Color(213, 216, 222), new Color(43, 45, 48));
    }

    /**
     * Lấy màu nền của ô trống - Chuyển thể chủ đề
     * Chủ đề nhẹ: màu trắng (phân biệt rõ ràng)
     * Dark theme: A slightly lighter color than cells with values ​​(to help distinguish empty cells)
     */
    public static Color getEmptyCellBackground() {
        return color(ThemeColors.EMPTY_CELL_BACKGROUND, Color.WHITE, new Color(40, 42, 46));
    }

    // ==================== Màu thanh cuộn (thích ứng chủ đề)====================

    /**
     * Lấy màu theo dõi thanh cuộn - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-50 (245, 247, 250)
     * Chủ đề tối: Tương tự như mặt nội dung (30, 31, 34)
     */
    public static Color getScrollbarTrackColor() {
        return color(ThemeColors.SCROLLBAR_TRACK, new Color(243, 244, 247), new Color(30, 31, 34));
    }

    /**
     * Lấy màu thanh trượt thanh cuộn - Thích ứng với chủ đề
     * Chủ đề tươi sáng: xám nhạt (200, 204, 212)
     * Chủ đề tối: Màu xám nhạt hơn nền (70, 72, 78)
     */
    public static Color getScrollbarThumbColor() {
        return color(ThemeColors.SCROLLBAR_THUMB, new Color(200, 204, 212), new Color(63, 66, 72));
    }

    /**
     * Lấy màu di chuột của thanh trượt thanh cuộn - Thích ứng với chủ đề
     * Chủ đề nhẹ: xám vừa (183, 189, 200)
     * Chủ đề tối: màu xám nhạt hơn (85, 88, 96)
     */
    public static Color getScrollbarThumbHoverColor() {
        return color(ThemeColors.SCROLLBAR_THUMB_HOVER, new Color(183, 189, 200), new Color(76, 80, 87));
    }

    // ==================== Cảnh báo/Màu sắc nhắc nhở (thích ứng chủ đề)====================

    /**
     * Lấy màu nền cảnh báo - Thích ứng với chủ đề
     * Chủ đề màu sắc tươi sáng: vàng nhạt (255, 243, 205)
     * Chủ đề tối: tông màu vàng đậm (70, 65, 50)
     */
    public static Color getWarningBackgroundColor() {
        return color(ThemeColors.WARNING_BACKGROUND, new Color(255, 243, 205), new Color(58, 52, 39));
    }

    /**
     * Lấy màu đường viền cảnh báo - Thích ứng với chủ đề
     * Chủ đề màu sắc tươi sáng: màu vàng (255, 193, 7)
     * Chủ đề tối: màu vàng nhạt hơn (120, 100, 60)
     */
    public static Color getWarningBorderColor() {
        return color(ThemeColors.WARNING_BORDER, new Color(255, 193, 7), new Color(98, 84, 50));
    }

    /**
     * Nhận màu nền nổi bật phù hợp tìm kiếm phổ biến.
     */
    public static Color getSearchHighlightBackgroundColor() {
        return color(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, new Color(255, 241, 168), new Color(91, 75, 0));
    }

    /**
     * Lấy màu nền nổi bật của kết quả tìm kiếm hiện tại.
     */
    public static Color getSearchCurrentHighlightBackgroundColor() {
        return color(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, new Color(254, 215, 170), new Color(150, 100, 50));
    }

    public static Color getSplashGradientStartColor() {
        return color(ThemeColors.SPLASH_GRADIENT_START, getPrimary(), new Color(65, 67, 69));
    }

    public static Color getSplashGradientEndColor() {
        return color(ThemeColors.SPLASH_GRADIENT_END, getPrimaryLighter(), new Color(30, 31, 34));
    }

    /**
     * Lấy màu bóng - Thích ứng với chủ đề
     * Chủ đề tối sử dụng các sắc thái nhẹ nhàng hơn (màu đen, độ trong suốt nâng cao)
     * Chủ đề nhẹ nhàng sử dụng tông màu xanh đen đậm
     *
     * @param alpha Cơ bản minh bạch (0-255)
     */
    public static Color getShadowColor(int alpha) {
        if (isDarkTheme()) {
            return new Color(0, 0, 0, (int) (alpha * 1.5));
        }
        return new Color(15, 23, 42, alpha);
    }

    // ==================== Console Màu dành riêng cho bảng điều khiển (điều chỉnh theo chủ đề)====================

    /**
     * Console Màu nền của vùng văn bản - Thích ứng với chủ đề
     * Chủ đề nhẹ: xám nhạt (243, 244, 247) - #f3f4f7
     * Chủ đề tối: Phần nội dung có màu xám đậm (30, 31, 34) - #1e1f22
     */
    public static Color getConsoleTextAreaBg() {
        return color(ThemeColors.CONSOLE_TEXT_AREA_BACKGROUND, new Color(243, 244, 247), new Color(30, 31, 34));
    }

    /**
     * Console Màu văn bản bình thường - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-700 (51, 65, 85)
     * Chủ đề tối: xám nhạt (220, 220, 220)
     */
    public static Color getConsoleText() {
        return color(ThemeColors.CONSOLE_TEXT, new Color(51, 65, 85), new Color(201, 204, 211));
    }

    /**
     * Console INFO Màu sắc đẳng cấp - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Blue-700 (37, 99, 235)
     * Chủ đề tối:Blue-400 (96, 165, 250)
     */
    public static Color getConsoleInfo() {
        return color(ThemeColors.CONSOLE_INFO, new Color(37, 99, 235), new Color(96, 165, 250));
    }

    /**
     * Console DEBUG Màu sắc đẳng cấp - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Green-600 (22, 163, 74)
     * Chủ đề tối:Green-400 (74, 222, 128)
     */
    public static Color getConsoleDebug() {
        return color(ThemeColors.CONSOLE_DEBUG, new Color(22, 163, 74), SUCCESS);
    }

    /**
     * Console WARN Màu sắc đẳng cấp - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Orange-600 (234, 88, 12)
     * Chủ đề tối:Orange-400 (251, 146, 60)
     */
    public static Color getConsoleWarn() {
        return color(ThemeColors.CONSOLE_WARN, new Color(234, 88, 12), WARNING);
    }

    /**
     * Console ERROR Màu sắc đẳng cấp - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Red-600 (220, 38, 38)
     * Chủ đề tối:Red-400 (248, 113, 113)
     */
    public static Color getConsoleError() {
        return color(ThemeColors.CONSOLE_ERROR, new Color(220, 38, 38), ERROR);
    }

    /**
     * Console Màu tên lớp - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Purple-700 (147, 51, 234)
     * Chủ đề tối:Purple-400 (192, 132, 252)
     */
    public static Color getConsoleClassName() {
        return color(ThemeColors.CONSOLE_CLASS_NAME, new Color(147, 51, 234), new Color(192, 132, 252));
    }

    /**
     * Console Màu tên phương thức - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Sky-600 (14, 165, 233)
     * Chủ đề tối:Sky-400 (56, 189, 248)
     */
    public static Color getConsoleMethodName() {
        return color(ThemeColors.CONSOLE_METHOD_NAME, new Color(14, 165, 233), new Color(56, 189, 248));
    }

    /**
     * Console Màu nền của thanh công cụ - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-50 (248, 250, 252)
     * Chủ đề tối: màu xám nhạt hơn một chút (58, 60, 62) - #3a3c3e
     */
    public static Color getConsoleToolbarBg() {
        return color(ThemeColors.CONSOLE_TOOLBAR_BACKGROUND, new Color(248, 250, 252), new Color(38, 40, 44));
    }

    /**
     * Console Màu đường viền thanh công cụ - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Slate-200 (226, 232, 240)
     * Chủ đề tối: màu xám đậm (60, 60, 70)
     */
    public static Color getConsoleToolbarBorder() {
        return color(ThemeColors.CONSOLE_TOOLBAR_BORDER, new Color(226, 232, 240), new Color(50, 52, 56));
    }

    /**
     * Console Chọn màu nền văn bản - Thích ứng với chủ đề
     * Chủ đề tươi sáng:Blue-200 (191, 219, 254)
     * Chủ đề tối: xanh đậm (60, 90, 120)
     */
    public static Color getConsoleSelectionBg() {
        return color(ThemeColors.CONSOLE_SELECTION_BACKGROUND, new Color(191, 219, 254), new Color(43, 67, 113));
    }

    public static Color getNotificationBackground() {
        return color(ThemeColors.NOTIFICATION_BACKGROUND, new Color(252, 252, 253), new Color(38, 40, 44));
    }

    public static Color getNotificationBorder() {
        return color(ThemeColors.NOTIFICATION_BORDER, new Color(205, 208, 214), new Color(50, 52, 56));
    }

    public static Color getNotificationDivider() {
        return color(ThemeColors.NOTIFICATION_DIVIDER, new Color(208, 211, 216), new Color(43, 45, 48));
    }

    public static Color getNotificationBodyForeground() {
        return color(ThemeColors.NOTIFICATION_BODY_FOREGROUND, new Color(44, 46, 50), new Color(201, 204, 211));
    }

    // ==================== Phương pháp công cụ ====================

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static String toHtmlColor(Color color) {
        if (color == null) {
            return "#000000";
        }
        return "#%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Nhận màu cảnh báo với độ trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color warningWithAlpha(int alpha) {
        return withAlpha(getWarning(), alpha);
    }

    /**
     * Lấy màu chính với độ trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color primaryWithAlpha(int alpha) {
        return withAlpha(getPrimary(), alpha);
    }

    /**
     * Nhận được màu thứ cấp với độ trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color secondaryWithAlpha(int alpha) {
        return withAlpha(getSecondary(), alpha);
    }

    /**
     * Có được màu nhấn với độ trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color accentWithAlpha(int alpha) {
        return withAlpha(getAccent(), alpha);
    }

    /**
     * Có được màu trắng trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color whiteWithAlpha(int alpha) {
        return new Color(255, 255, 255, alpha);
    }

    /**
     * Nhận màu đen với độ trong suốt
     *
     * @param alpha Minh bạch (0-255)
     */
    public static Color blackWithAlpha(int alpha) {
        return new Color(0, 0, 0, alpha);
    }

    /**
     * Tạo một gradient màu chính
     */
    public static GradientPaint createPrimaryGradient(int width, int height) {
        return new GradientPaint(0, 0, getPrimary(), width, height, getPrimaryDark());
    }

    /**
     * Tạo một gradient màu thứ cấp
     */
    public static GradientPaint createSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, getSecondaryLight(), width, height, getSecondaryDark());
    }

    /**
     * Tạo gradient màu chính đến phụ
     */
    public static GradientPaint createPrimaryToSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, getPrimary(), width, height, getSecondary());
    }


    /**
     * Trộn hai màu
     *
     * @param c1    màu sắc1
     * @param c2    màu sắc2
     * @param ratio Tỷ lệ trộn (0.0 = tất cảc1, 1.0 = tất cảc2)
     */
    public static Color blendColors(Color c1, Color c2, float ratio) {
        float invRatio = 1 - ratio;
        int r = (int) (c1.getRed() * invRatio + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * invRatio + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * invRatio + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * invRatio + c2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }
}
