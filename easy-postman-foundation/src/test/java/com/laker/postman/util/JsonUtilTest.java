package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class JsonUtilTest {

    @Test(description = "JSON Định dạng nên được giữ nguyên JSON5/JSONC Ghi chú phong cách")
    public void prettyPrintShouldPreserveJsonComments() {
        String formatted = JsonUtil.toJsonPrettyStr("""
                {
                  // request metadata
                  "url": "https://example.com/api", // keep inline
                  "nested": {
                    "enabled": true // keep tail
                  }
                }""");

        assertEquals(formatted, """
                {
                    // request metadata
                    "url": "https://example.com/api", // keep inline
                    "nested": {
                        "enabled": true // keep tail
                    }
                }""");
    }

    @Test(description = "URL Dấu gạch chéo kép trong chuỗi không nên nhầm lẫn với nhận xét")
    public void prettyPrintShouldNotTreatUrlStringAsComment() {
        String formatted = JsonUtil.toJsonPrettyStr("""
                {"url":"https://example.com/a//b"}""");

        assertEquals(formatted, """
                {
                    "url": "https://example.com/a//b"
                }""");
    }

    @Test(description = "JSON So sánh cấu trúc nên bỏ qua thứ tự trường đối tượng nhưng giữ nguyên thứ tự mảng")
    public void structurallyEqualShouldCompareParsedJson() {
        assertTrue(JsonUtil.isStructurallyEqual("{\"a\":1,\"b\":[2,3]}", "{\"b\":[2,3],\"a\":1}"));
        assertFalse(JsonUtil.isStructurallyEqual("[1,2]", "[2,1]"));
        assertFalse(JsonUtil.isStructurallyEqual("plain text", "plain text"));
    }
}
