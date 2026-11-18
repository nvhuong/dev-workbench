package com.nvhuong.devworkbench.plugin.kafka;

import com.nvhuong.devworkbench.util.I18nUtil;

public final class KafkaI18n {

    public static final String BUNDLE_NAME = "kafka-messages";

    private KafkaI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, KafkaI18n.class.getClassLoader(), key, args);
    }
}
