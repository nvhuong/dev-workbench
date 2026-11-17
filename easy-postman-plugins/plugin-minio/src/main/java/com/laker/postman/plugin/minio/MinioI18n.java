package com.laker.postman.plugin.minio;

import com.laker.postman.util.I18nUtil;

final class MinioI18n {
    static final String BUNDLE = "minio-messages";
    private MinioI18n() { }
    static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE, MinioI18n.class.getClassLoader(), "minio." + key, args);
    }
}
