package com.laker.postman.platform.update.source;

/**
 * GitHub Cập nhật triển khai nguồn
 */
public class GitHubUpdateSource extends AbstractUpdateSource {

    private static final String API_URL = "https://api.github.com/repos/nvhuong/easy-postman/releases/latest";
    private static final String ALL_RELEASES_API_URL = "https://api.github.com/repos/nvhuong/easy-postman/releases";
    private static final String WEB_URL = "https://github.com/nvhuong/easy-postman/releases";
    private static final String NAME = "GitHub";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getApiUrl() {
        return API_URL;
    }

    @Override
    public String getAllReleasesApiUrl() {
        return ALL_RELEASES_API_URL;
    }

    @Override
    public String getWebUrl() {
        return WEB_URL;
    }
}

