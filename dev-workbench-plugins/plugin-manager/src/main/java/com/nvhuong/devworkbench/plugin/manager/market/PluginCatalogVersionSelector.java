package com.nvhuong.devworkbench.plugin.manager.market;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Từ catalog Được chọn làm máy chủ hiện tại từ các phiên bản lịch sử“Một trong những phù hợp nhất”。
 * <p>
 * luật lệ:
 * 1. Giống nhau plugin id Ưu tiên phiên bản cao nhất tương thích với Host hiện tại
 * 2. Nếu không có phiên bản tương thích thì quay lại phiên bản mới nhất để bày tỏ với người dùng“Máy chủ cần được nâng cấp”
 * </p>
 */
@UtilityClass
public class PluginCatalogVersionSelector {

    private static final Comparator<PluginCatalogEntry> VERSION_DESC =
            (left, right) -> compareVersions(right.version(), left.version());

    public static List<PluginCatalogEntry> selectForHost(List<PluginCatalogEntry> entries,
                                                         String currentAppVersion,
                                                         String currentPlatformVersion) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<String, List<PluginCatalogEntry>> grouped = new LinkedHashMap<>();
        for (PluginCatalogEntry entry : entries) {
            if (entry == null || entry.id() == null || entry.id().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(entry.id(), ignored -> new ArrayList<>()).add(entry);
        }

        List<PluginCatalogEntry> selected = new ArrayList<>();
        for (List<PluginCatalogEntry> candidates : grouped.values()) {
            candidates.sort(VERSION_DESC);
            PluginCatalogEntry compatible = candidates.stream()
                    .filter(entry -> isCompatibleWithHost(entry, currentAppVersion, currentPlatformVersion))
                    .findFirst()
                    .orElse(null);
            selected.add(compatible != null ? compatible : candidates.get(0));
        }

        selected.sort(Comparator.comparing(PluginCatalogEntry::id));
        return selected;
    }

    private static boolean isCompatibleWithHost(PluginCatalogEntry entry,
                                                String currentAppVersion,
                                                String currentPlatformVersion) {
        return isVersionInRange(currentAppVersion, entry.minAppVersion(), entry.maxAppVersion())
                && isVersionInRange(currentPlatformVersion, entry.minPlatformVersion(), entry.maxPlatformVersion());
    }

    private static boolean isVersionInRange(String currentVersion, String minVersion, String maxVersion) {
        if (minVersion != null && !minVersion.isBlank()
                && compareVersions(currentVersion, minVersion) < 0) {
            return false;
        }
        return maxVersion == null || maxVersion.isBlank()
                || compareVersions(currentVersion, maxVersion) <= 0;
    }

    private static int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }
        String normalizedV1 = trimPrefix(v1);
        String normalizedV2 = trimPrefix(v2);
        String[] arr1 = coreVersion(normalizedV1).split("\\.");
        String[] arr2 = coreVersion(normalizedV2).split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parse(arr1[i]) : 0;
            int n2 = i < arr2.length ? parse(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return compareQualifier(qualifier(normalizedV1), qualifier(normalizedV2));
    }

    private static String trimPrefix(String version) {
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

    private static int parse(String token) {
        try {
            return Integer.parseInt(token.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
