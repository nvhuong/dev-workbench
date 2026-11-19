package com.nvhuong.devworkbench.platform.update.version;

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
        return (version.startsWith("v") || version.startsWith("V")) ? version.substring(1) : version;
    }

    private static String coreVersion(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 ? version.substring(0, separatorIndex) : version;
    }

    private static String qualifier(String version) {
        // SemVer: "-" starts the pre-release, "+" starts build metadata, which never affects precedence.
        int plus = version.indexOf('+');
        String withoutBuild = plus >= 0 ? version.substring(0, plus) : version;
        int dash = withoutBuild.indexOf('-');
        return dash >= 0 ? withoutBuild.substring(dash + 1) : "";
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
        // SemVer 11.4: dot-separated identifiers; numeric ones compare numerically and sort before alphanumeric
        // ones; a shorter identifier list sorts first. Digit runs inside identifiers ("rc10") compare numerically.
        String[] ids1 = q1.split("\\.");
        String[] ids2 = q2.split("\\.");
        for (int i = 0; i < Math.min(ids1.length, ids2.length); i++) {
            boolean numeric1 = ids1[i].matches("\\d+");
            boolean numeric2 = ids2[i].matches("\\d+");
            int result;
            if (numeric1 && numeric2) {
                result = new java.math.BigInteger(ids1[i]).compareTo(new java.math.BigInteger(ids2[i]));
            } else if (numeric1 || numeric2) {
                result = numeric1 ? -1 : 1;
            } else {
                result = compareNatural(ids1[i], ids2[i]);
            }
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(ids1.length, ids2.length);
    }

    private static int compareNatural(String s1, String s2) {
        java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("\\d+|\\D+").matcher(s1);
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("\\d+|\\D+").matcher(s2);
        while (m1.find() && m2.find()) {
            String t1 = m1.group();
            String t2 = m2.group();
            boolean digits = Character.isDigit(t1.charAt(0)) && Character.isDigit(t2.charAt(0));
            int result = digits ? new java.math.BigInteger(t1).compareTo(new java.math.BigInteger(t2))
                    : t1.compareToIgnoreCase(t2);
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(s1.length(), s2.length());
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
