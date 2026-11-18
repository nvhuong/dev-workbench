package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.util.I18nUtil;

final class RemoteI18n {
    static final String BUNDLE = "remote-messages";
    private RemoteI18n() { }
    static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE, RemoteI18n.class.getClassLoader(), "remote." + key, args);
    }
}
