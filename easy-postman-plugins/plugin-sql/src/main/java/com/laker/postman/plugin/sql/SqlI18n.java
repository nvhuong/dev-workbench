package com.laker.postman.plugin.sql;

import com.laker.postman.util.I18nUtil;

final class SqlI18n {
    static final String BUNDLE = "sql-messages";
    private SqlI18n() { }
    static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE, SqlI18n.class.getClassLoader(), "sql." + key, args);
    }
}
