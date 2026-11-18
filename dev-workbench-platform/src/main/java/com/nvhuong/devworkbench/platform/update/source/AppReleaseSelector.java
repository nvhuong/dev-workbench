package com.nvhuong.devworkbench.platform.update.source;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.core.util.StrUtil;
import com.nvhuong.devworkbench.platform.update.version.VersionComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chỉ chọn phiên bản chính thức của ứng dụng máy chủ release，Loại trừ các plugin release、Bản nháp và phát hành trước.
 */
final class AppReleaseSelector {

    private AppReleaseSelector() {
    }

    static JSONObject selectLatestStableAppRelease(JSONArray releases) {
        JSONArray filtered = selectStableAppReleases(releases, 1);
        if (filtered == null || filtered.isEmpty()) {
            return null;
        }
        return filtered.getJSONObject(0);
    }

    static JSONArray selectStableAppReleases(JSONArray releases, int limit) {
        if (releases == null || releases.isEmpty()) {
            return releases;
        }

        List<JSONObject> stableAppReleases = new ArrayList<>();
        for (int i = 0; i < releases.size(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (!isStableAppRelease(release)) {
                continue;
            }
            stableAppReleases.add(release);
        }

        stableAppReleases.sort(Comparator
                .comparing((JSONObject release) -> release.getStr("tag_name"), VersionComparator::compare)
                .reversed());

        JSONArray filtered = new JSONArray();
        for (JSONObject release : stableAppReleases) {
            filtered.add(release);
            if (limit > 0 && filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    static boolean isStableAppRelease(JSONObject release) {
        if (release == null) {
            return false;
        }
        if (release.getBool("draft", false) || release.getBool("prerelease", false)) {
            return false;
        }
        return isAppReleaseTag(release.getStr("tag_name"));
    }

    static boolean isAppReleaseTag(String tagName) {
        if (StrUtil.isBlank(tagName)) {
            return false;
        }
        return tagName.startsWith("v");
    }
}
