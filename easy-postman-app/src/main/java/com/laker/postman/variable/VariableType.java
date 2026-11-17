package com.laker.postman.variable;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import java.awt.*;

/**
 * Kiểu liệt kê biến
 * <p>
 * Các loại nguồn biến đổi khác nhau và vị trí của chúng được xác địnhUIKiểu hiển thị trong
 */
@Getter
public enum VariableType {
    /**
     * Các biến bối cảnh thực thi - Nó có mức độ ưu tiên cao nhất và hợp lệ trong yêu cầu hiện tại hoặc bối cảnh đang chạy hiện tại
     */
    VARIABLE(MessageKeys.VARIABLE_TYPE_VARIABLE, "V", 1),

    /**
     * Lặp lại các biến dữ liệu - hiện tại CSV/JSON dữ liệu hàng
     */
    ITERATION_DATA(MessageKeys.VARIABLE_TYPE_ITERATION_DATA, "D", 2),

    /**
     * Nhóm các biến - Các biến được kế thừa từ nhóm mà yêu cầu được thực hiện
     */
    GROUP(MessageKeys.VARIABLE_TYPE_GROUP, "C", 3),

    /**
     * các biến môi trường - Thu được từ môi trường hiện đang hoạt động
     */
    ENVIRONMENT(MessageKeys.VARIABLE_TYPE_ENVIRONMENT, "E", 5),

    /**
     * Các biến toàn cầu - Các biến chia sẻ ở cấp độ ứng dụng
     */
    GLOBAL(MessageKeys.VARIABLE_TYPE_GLOBAL, "G", 8),

    /**
     * Các chức năng tích hợp - Các chức năng động như $guid, $timestamp Đợi đã
     */
    BUILT_IN(MessageKeys.VARIABLE_TYPE_BUILT_IN, "$", 10);

    /**
     * Khóa thông báo được quốc tế hóa cho loại biến
     */
    private final String displayNameKey;

    /**
     * UIBiểu tượng biểu tượng được sử dụng khi kết xuất
     */
    private final String iconSymbol;

    /**
     * Ưu tiên (giá trị càng nhỏ thì mức độ ưu tiên càng cao)
     */
    private final int priority;

    VariableType(String displayNameKey, String iconSymbol, int priority) {
        this.displayNameKey = displayNameKey;
        this.iconSymbol = iconSymbol;
        this.priority = priority;
    }

    /**
     * UIMàu sắc thích ứng theo chủ đề để sử dụng khi kết xuất.
     */
    public Color getColor() {
        return switch (this) {
            case VARIABLE -> ModernColors.getVariableContextColor();
            case ITERATION_DATA -> ModernColors.getVariableIterationDataColor();
            case GROUP -> ModernColors.getVariableGroupColor();
            case ENVIRONMENT -> ModernColors.getVariableEnvironmentColor();
            case GLOBAL -> ModernColors.getVariableGlobalColor();
            case BUILT_IN -> ModernColors.getVariableBuiltInColor();
        };
    }

    /**
     * Lấy tên hiển thị được quốc tế hóa
     *
     * @return Tên hiển thị được trả về dựa trên ngôn ngữ hiện tại
     */
    public String getDisplayName() {
        return I18nUtil.getMessage(displayNameKey);
    }
}
