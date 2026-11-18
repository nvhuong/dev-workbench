package com.nvhuong.devworkbench.plugin.capture;

import com.nvhuong.devworkbench.util.I18nUtil;

public final class CaptureI18n {

    public static final String BUNDLE_NAME = "capture-messages";

    private CaptureI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, CaptureI18n.class.getClassLoader(), key, args);
    }
}
