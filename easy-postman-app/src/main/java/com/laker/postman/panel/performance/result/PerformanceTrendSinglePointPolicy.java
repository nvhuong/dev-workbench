package com.laker.postman.panel.performance.result;

import lombok.experimental.UtilityClass;
import org.jfree.data.xy.XYDataset;

/**
 * Biểu đồ xu hướng mặc định chỉ vẽ các đường; chỉ thêm một điểm khi toàn bộ chuỗi chỉ có một mẫu hợp lệ tạm thời để tránh các điểm biệt lập lịch sử bị hiểu sai là các đỉnh bất thường.
 */
@UtilityClass
class PerformanceTrendSinglePointPolicy {

    static boolean shouldShowShape(XYDataset dataset, int series, int item) {
        if (!hasFiniteValue(dataset, series, item)) {
            return false;
        }
        return finiteValueCount(dataset, series) == 1;
    }

    private static int finiteValueCount(XYDataset dataset, int series) {
        if (dataset == null || series < 0 || series >= dataset.getSeriesCount()) {
            return 0;
        }
        int count = 0;
        int itemCount = dataset.getItemCount(series);
        for (int i = 0; i < itemCount; i++) {
            if (Double.isFinite(dataset.getYValue(series, i))) {
                count++;
                if (count > 1) {
                    return count;
                }
            }
        }
        return count;
    }

    private static boolean hasFiniteValue(XYDataset dataset, int series, int item) {
        if (dataset == null || series < 0 || series >= dataset.getSeriesCount()) {
            return false;
        }
        if (item < 0 || item >= dataset.getItemCount(series)) {
            return false;
        }
        return Double.isFinite(dataset.getYValue(series, item));
    }
}
