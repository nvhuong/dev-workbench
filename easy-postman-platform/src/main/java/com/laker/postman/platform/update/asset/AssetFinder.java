package com.laker.postman.platform.update.asset;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Trình tìm tài nguyên - Trích xuất các liên kết tải xuống thích hợp từ thông tin phát hành
 */
@Slf4j
public class AssetFinder {

    private static final String BROWSER_DOWNLOAD_URL = "browser_download_url";


    /**
     * Tìm tài nguyên dựa trên phần mở rộng
     *
     * @param assets    Danh sách tài nguyên
     * @param extension Phần mở rộng tệp (ví dụ:  ".dmg", ".exe"）
     * @return tải về URL，Trả lại không tìm thấy null
     */
    public String findByExtension(JSONArray assets, String extension) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(extension)) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found asset: {} -> {}", name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * Tìm tài nguyên dựa trên các mẫu
     *
     * @param assets  Danh sách tài nguyên
     * @param pattern Mẫu khớp (ví dụ "-portable.zip"）
     * @return tải về URL，Trả lại không tìm thấy null
     */
    public String findByPattern(JSONArray assets, String pattern) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.contains(pattern)) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found asset by pattern '{}': {} -> {}", pattern, name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * Tìm kiếm tài nguyên theo trình tự theo nhiều mẫu và trả về lượt tải xuống đầu tiên URL
     */
    public String findFirstByPatterns(JSONArray assets, String... patterns) {
        if (patterns == null) {
            return null;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String url = findByPattern(assets, pattern);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

}
