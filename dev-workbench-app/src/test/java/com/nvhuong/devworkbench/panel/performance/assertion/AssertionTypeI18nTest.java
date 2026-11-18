package com.nvhuong.devworkbench.panel.performance.assertion;

import com.nvhuong.devworkbench.util.MessageKeys;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;

public class AssertionTypeI18nTest {

    @Test
    public void shouldHaveEnglishAndChineseLabelsForAllTypes() {
        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.forLanguageTag("vi"));

        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE), "Response Code");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS), "Contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH), "JSONPath");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE), "Mã phản hồi");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS), "Chứa nội dung");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH), "JSONPath");
    }
}
