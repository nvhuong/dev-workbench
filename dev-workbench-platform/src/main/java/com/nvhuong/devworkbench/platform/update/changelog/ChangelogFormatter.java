package com.nvhuong.devworkbench.platform.update.changelog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Cập nhật định dạng nhật ký - Định dạng thông tin xuất bản thành văn bản có thể đọc được
 */
@Slf4j
public class ChangelogFormatter {

    private static final int MAX_RELEASES = 10;

    /**
     * Định dạng danh sách thông tin phát hành
     *
     * @param releases Đăng mảng thông tin
     * @return Văn bản được định dạng
     */
    public String format(JSONArray releases) {
        if (releases == null || releases.isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.CHANGELOG_NO_RELEASES);
        }

        // Sắp xếp theo ngày xuất bản theo thứ tự giảm dần
        List<JSONObject> releaseList = sortReleases(releases);

        StringBuilder sb = new StringBuilder();
        int count = Math.min(releaseList.size(), MAX_RELEASES);

        for (int i = 0; i < count; i++) {
            JSONObject release = releaseList.get(i);
            appendRelease(sb, release);
        }

        return sb.toString();
    }

    /**
     * Sắp xếp thông tin phát hành (mới nhất trước)
     */
    private List<JSONObject> sortReleases(JSONArray releases) {
        List<JSONObject> releaseList = new ArrayList<>();
        for (int i = 0; i < releases.size(); i++) {
            releaseList.add(releases.getJSONObject(i));
        }

        releaseList.sort((a, b) -> {
            String dateA = getPublishDate(a);
            String dateB = getPublishDate(b);
            return dateB.compareTo(dateA); // Thứ tự giảm dần
        });

        return releaseList;
    }

    /**
     * Nhận ngày phát hành (tương thích GitHub và Gitee）
     */
    private String getPublishDate(JSONObject release) {
        // GitHub sử dụng published_at，Gitee sử dụng created_at
        String date = release.getStr("published_at");
        if (StrUtil.isBlank(date)) {
            date = release.getStr("created_at");
        }
        return StrUtil.isNotBlank(date) ? date : "";
    }

    /**
     * Thêm thông tin phát hành riêng lẻ
     */
    private void appendRelease(StringBuilder sb, JSONObject release) {
        String tagName = release.getStr("tag_name");
        String name = release.getStr("name");
        String body = release.getStr("body");
        String date = getPublishDate(release);

        String displayName = StrUtil.isNotBlank(name) ? name : tagName;
        String displayDate = date.length() >= 10
                ? date.substring(0, 10)
                : I18nUtil.getMessage(MessageKeys.CHANGELOG_UNKNOWN_DATE);
        sb.append(displayName).append("  ").append(displayDate).append("\n");

        if (StrUtil.isNotBlank(body)) {
            sb.append(body.trim()).append("\n\n\n");
        } else {
            String noNotes = "- " + I18nUtil.getMessage(MessageKeys.CHANGELOG_NO_NOTES);
            sb.append(noNotes).append("\n\n\n");
        }
    }
}
