package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

/**
 * Lớp công cụ hiển thị thời gian
 * Cung cấp chuyển đổi thời gian mili giây thành định dạng hiển thị thân thiện hơn
 */
@UtilityClass
public class TimeDisplayUtil {

    /**
     * Chuyển đổi thời gian mili giây sang định dạng hiển thị thân thiện hơn
     * - Ít hơn1Giây được hiển thị dưới dạng  "xx ms"
     * - Ít hơn1Phút được hiển thị dưới dạng  "x.xx s"
     * - Lớn hơn hoặc bằng1Phút được hiển thị dưới dạng  "xm xs"
     *
     * @param elapsedTimeMillis thời gian mili giây
     * @return Chuỗi thời gian được định dạng
     */
    public static String formatElapsedTime(long elapsedTimeMillis) {
        if (elapsedTimeMillis < 1000) {
            return elapsedTimeMillis + " ms";
        } else if (elapsedTimeMillis < 60000) {
            return String.format("%.2f s", elapsedTimeMillis / 1000.0);
        } else {
            int minutes = (int) (elapsedTimeMillis / 60000);
            int seconds = (int) ((elapsedTimeMillis % 60000) / 1000);
            return minutes + "m " + seconds + "s";
        }
    }
}
