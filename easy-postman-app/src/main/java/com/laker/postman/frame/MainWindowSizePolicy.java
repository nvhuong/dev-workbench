package com.laker.postman.frame;

import lombok.experimental.UtilityClass;

import java.awt.Dimension;

/**
 * Chính sách kích thước cửa sổ chính phân biệt giữa kích thước mặc định khi khởi động và kích thước tối thiểu mà người dùng có thể thay đổi kích thước.
 */
@UtilityClass
class MainWindowSizePolicy {
    private static final int SCREEN_WIDTH_4K = 3840;
    private static final int SCREEN_WIDTH_2K = 2560;
    private static final int SCREEN_WIDTH_FHD = 1920;
    private static final int SCREEN_WIDTH_HD = 1280;
    private static final int SCREEN_WIDTH_MAXIMIZED_THRESHOLD = 1366;

    private static final Dimension DEFAULT_SIZE_4K = new Dimension(1920, 1200);
    private static final Dimension DEFAULT_SIZE_2K = new Dimension(1600, 1000);
    private static final Dimension DEFAULT_SIZE_FHD = new Dimension(1400, 900);
    private static final Dimension DEFAULT_SIZE_HD = new Dimension(1280, 800);
    private static final Dimension DEFAULT_SIZE_WXGA = new Dimension(1100, 700);

    /**
     * Giới hạn dưới đối với thiết kế bố cục ứng dụng trên màn hình thông thường. Kích thước tối thiểu thực tế cũng tùy thuộc vào không gian làm việc có sẵn của tất cả các màn hình.
     * Ngăn kích thước tối thiểu do màn hình chính tạo ra khiến cửa sổ không thể khôi phục hoặc thu nhỏ trên màn hình phụ nhỏ hơn.
     */
    private static final Dimension DESIGN_MINIMUM_SIZE = new Dimension(1100, 700);

    static Dimension preferredSizeForScreenWidth(double screenWidth) {
        if (screenWidth >= SCREEN_WIDTH_4K) {
            return copyOf(DEFAULT_SIZE_4K);
        }
        if (screenWidth >= SCREEN_WIDTH_2K) {
            return copyOf(DEFAULT_SIZE_2K);
        }
        if (screenWidth >= SCREEN_WIDTH_FHD) {
            return copyOf(DEFAULT_SIZE_FHD);
        }
        if (screenWidth >= SCREEN_WIDTH_HD) {
            return copyOf(DEFAULT_SIZE_HD);
        }
        return copyOf(DEFAULT_SIZE_WXGA);
    }

    static Dimension minimumSizeForDisplayAreas(Dimension... usableDisplayAreas) {
        int minimumWidth = DESIGN_MINIMUM_SIZE.width;
        int minimumHeight = DESIGN_MINIMUM_SIZE.height;

        if (usableDisplayAreas != null) {
            for (Dimension displayArea : usableDisplayAreas) {
                if (displayArea == null || displayArea.width <= 0 || displayArea.height <= 0) {
                    continue;
                }
                minimumWidth = Math.min(minimumWidth, displayArea.width);
                minimumHeight = Math.min(minimumHeight, displayArea.height);
            }
        }

        return new Dimension(minimumWidth, minimumHeight);
    }

    static Dimension constrainToUsableArea(Dimension requestedSize,
                                           Dimension minimumSize,
                                           Dimension usableDisplayArea) {
        int usableWidth = positiveOrMaxValue(usableDisplayArea == null ? 0 : usableDisplayArea.width);
        int usableHeight = positiveOrMaxValue(usableDisplayArea == null ? 0 : usableDisplayArea.height);
        int minimumWidth = Math.min(positiveOrOne(minimumSize == null ? 0 : minimumSize.width), usableWidth);
        int minimumHeight = Math.min(positiveOrOne(minimumSize == null ? 0 : minimumSize.height), usableHeight);
        int requestedWidth = positiveOrOne(requestedSize == null ? 0 : requestedSize.width);
        int requestedHeight = positiveOrOne(requestedSize == null ? 0 : requestedSize.height);

        int width = Math.min(Math.max(requestedWidth, minimumWidth), usableWidth);
        int height = Math.min(Math.max(requestedHeight, minimumHeight), usableHeight);
        return new Dimension(width, height);
    }

    static boolean shouldStartMaximized(double screenWidth) {
        return screenWidth <= SCREEN_WIDTH_MAXIMIZED_THRESHOLD;
    }

    private static int positiveOrOne(int value) {
        return value > 0 ? value : 1;
    }

    private static int positiveOrMaxValue(int value) {
        return value > 0 ? value : Integer.MAX_VALUE;
    }

    private static Dimension copyOf(Dimension dimension) {
        return new Dimension(dimension);
    }
}
