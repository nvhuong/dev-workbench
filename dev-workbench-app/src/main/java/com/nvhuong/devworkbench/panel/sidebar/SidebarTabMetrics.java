package com.nvhuong.devworkbench.panel.sidebar;

import lombok.experimental.UtilityClass;

/**
 * Thanh bên Tab Chiến lược kích thước để tránh sự phân tán trong các thành phần được vẽ magic number。
 */
@UtilityClass
class SidebarTabMetrics {
    static final int TAB_AREA_INSET_TOP = 4;
    static final int TAB_AREA_INSET_LEFT = 2;
    static final int TAB_AREA_INSET_BOTTOM = 4;
    static final int TAB_AREA_INSET_RIGHT = 2;

    // Phía bên phải của trạng thái mở rộng nằm cạnh thẻ nội dung và cũng có chính thẻ đó gutter；Để lại ít hơn một chút ở phía bên phải để cái nhìn tổng thể được cân bằng giữa bên trái và bên phải.
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_LEFT = 10;
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT = 2;
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_VERTICAL = 5;
    static final int COLLAPSED_SELECTED_BACKGROUND_WIDTH = 32;
    static final int COLLAPSED_SELECTED_BACKGROUND_HEIGHT = 32;
    // Cạnh trái của menu thu gọn sát mép cửa sổ, cạnh phải sát với thẻ nội dung gap，Cần phải dịch chuyển sang phải một chút để bù đắp cho độ lệch thị giác bên trái.
    static final int COLLAPSED_VISUAL_CENTER_OFFSET_X = 2;
    // BasicTabbedPaneUI Sẽ cung cấp cho không được kiểm tra tab label một +1px Dịch chuyển, trạng thái được chọn là mặc định -1px。
    // thống nhất label Sau khi dịch chuyển, nền trạng thái cũng chuyển sang thẻ nội dung gutter đền bù 1px，Tài năng và biểu tượng chia sẻ một trung tâm hình ảnh.
    static final int COLLAPSED_TAB_LABEL_SHIFT_X = 1;
    static final int COLLAPSED_STATE_BACKGROUND_GUTTER_OFFSET_X = 1;
    static final int SELECTED_BACKGROUND_ARC = 10;

    static final int EXPANDED_TAB_PADDING_VERTICAL = 6;
    static final int EXPANDED_TAB_PADDING_HORIZONTAL = 12;
    static final int EXPANDED_TAB_SPACING_TOP = 3;
    static final int EXPANDED_TAB_SPACING_MIDDLE = 3;
    static final int EXPANDED_TAB_SPACING_BOTTOM = 4;

    static final int COLLAPSED_TAB_PADDING_VERTICAL = 8;
    // Nền được chọn đã được dịch chuyển trực quan sang bên phải để bù đắp cho biểu tượng padding Đồng bộ hóa để đảm bảo rằng tâm của biểu tượng tuân theo nền đã chọn.
    static final int COLLAPSED_TAB_PADDING_HORIZONTAL = 6;

    static int expandedWidth(int maxTextWidth) {
        int width = maxTextWidth + (EXPANDED_TAB_PADDING_HORIZONTAL * 2);
        return Math.min(Math.max(width, 60), 140);
    }

    static int collapsedWidth(int maxIconWidth) {
        int width = maxIconWidth + (COLLAPSED_TAB_PADDING_HORIZONTAL * 2);
        return Math.max(width, 36);
    }

    static int expandedHeight(int maxIconHeight, int fontHeight) {
        int height = EXPANDED_TAB_PADDING_VERTICAL
                + EXPANDED_TAB_SPACING_TOP
                + maxIconHeight
                + EXPANDED_TAB_SPACING_MIDDLE
                + fontHeight
                + EXPANDED_TAB_SPACING_BOTTOM
                + EXPANDED_TAB_PADDING_VERTICAL;
        return Math.max(height, 60);
    }

    static int collapsedHeight(int maxIconHeight) {
        int height = maxIconHeight + (COLLAPSED_TAB_PADDING_VERTICAL * 2);
        return Math.max(height, 40);
    }

    static int expandedSelectedBackgroundX(int tabX) {
        return tabX + EXPANDED_SELECTED_BACKGROUND_INSET_LEFT;
    }

    static int expandedSelectedBackgroundWidth(int tabWidth) {
        return Math.max(0, tabWidth
                - EXPANDED_SELECTED_BACKGROUND_INSET_LEFT
                - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT);
    }

    static int expandedContentPaddingLeft() {
        return EXPANDED_TAB_PADDING_HORIZONTAL
                + (EXPANDED_SELECTED_BACKGROUND_INSET_LEFT - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT) / 2;
    }

    static int expandedContentPaddingRight() {
        return Math.max(0, EXPANDED_TAB_PADDING_HORIZONTAL
                - (EXPANDED_SELECTED_BACKGROUND_INSET_LEFT - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT) / 2);
    }

    static int collapsedSelectedBackgroundX(int tabX, int tabWidth, int backgroundWidth) {
        int availableSpace = Math.max(0, tabWidth - backgroundWidth);
        int centeredX = availableSpace / 2 + COLLAPSED_VISUAL_CENTER_OFFSET_X;
        return tabX
                + Math.min(centeredX, availableSpace)
                + COLLAPSED_STATE_BACKGROUND_GUTTER_OFFSET_X;
    }

    static int collapsedIconPaddingLeft() {
        return COLLAPSED_TAB_PADDING_HORIZONTAL + COLLAPSED_VISUAL_CENTER_OFFSET_X;
    }

    static int collapsedIconPaddingRight() {
        return Math.max(0, COLLAPSED_TAB_PADDING_HORIZONTAL - COLLAPSED_VISUAL_CENTER_OFFSET_X);
    }
}
