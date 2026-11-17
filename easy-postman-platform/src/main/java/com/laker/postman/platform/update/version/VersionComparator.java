package com.laker.postman.platform.update.version;

import lombok.experimental.UtilityClass;

/**
 * Công cụ so sánh phiên bản
 */
@UtilityClass
public class VersionComparator {

    /**
     * So sánh hai số phiên bản
     *
     * @param v1 Phiên bản1（Bạn có thể mang nó 'v' tiền tố)
     * @param v2 Phiên bản2（Bạn có thể mang nó 'v' tiền tố)
     * @return Biểu thị dưới dạng số dương v1 > v2，0 Biểu thị sự bằng nhau và số âm v1 < v2
     */
    public static int compare(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }

        String s1 = removeVersionPrefix(v1);
        String s2 = removeVersionPrefix(v2);

        String[] arr1 = coreVersion(s1).split("\\.");
        String[] arr2 = coreVersion(s2).split("\\.");
        int len = Math.max(arr1.length, arr2.length);

        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parseIntSafely(arr1[i]) : 0;
            int n2 = i < arr2.length ? parseIntSafely(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return compareQualifier(qualifier(s1), qualifier(s2));
    }

    /**
     * Xóa tiền tố số phiên bản (ví dụ: 'v'）
     */
    private static String removeVersionPrefix(String version) {
        return version.startsWith("v") ? version.substring(1) : version;
    }

    private static String coreVersion(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 ? version.substring(0, separatorIndex) : version;
    }

    private static String qualifier(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 && separatorIndex + 1 < version.length()
                ? version.substring(separatorIndex + 1)
                : "";
    }

    private static int qualifierSeparatorIndex(String version) {
        int dash = version.indexOf('-');
        int plus = version.indexOf('+');
        if (dash < 0) {
            return plus;
        }
        if (plus < 0) {
            return dash;
        }
        return Math.min(dash, plus);
    }

    private static int compareQualifier(String q1, String q2) {
        boolean blank1 = q1 == null || q1.isBlank();
        boolean blank2 = q2 == null || q2.isBlank();
        if (blank1 && blank2) {
            return 0;
        }
        if (blank1) {
            return 1;
        }
        if (blank2) {
            return -1;
        }
        return q1.compareToIgnoreCase(q2);
    }

    /**
     * Phân tích số nguyên một cách an toàn (xóa các ký tự không phải số)
     */
    private static int parseIntSafely(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Đánh giá phiên bản1Nó có lớn hơn phiên bản không2
     */
    public static boolean isNewer(String v1, String v2) {
        return compare(v1, v2) > 0;
    }
}
