package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RequestBodyPanelFormatTest {

    @Test(description = "JSON Định dạng không đưa ra ngoại lệ khi gặp nhiều đối tượng cấp cao nhất")
    public void jsonFormatReturnsEmptyWhenInputHasTrailingTokens() {
        Optional<String> formatted = RequestBodyPanel.formatBodyTextForDisplay(
                RequestBodyPanel.RAW_TYPE_JSON, "{\"a\":1}{\"b\":2}");

        assertTrue(formatted.isEmpty());
    }

    @Test(description = "JSON Định dạng trả về văn bản được định dạng cho một đối tượng hợp lệ")
    public void jsonFormatReturnsPrettyTextForValidObject() {
        Optional<String> formatted = RequestBodyPanel.formatBodyTextForDisplay(
                RequestBodyPanel.RAW_TYPE_JSON, "{\"a\":1}");

        assertTrue(formatted.isPresent());
        assertEquals(formatted.get(), """
                {
                    "a": 1
                }""");
    }

    @Test(description = "JSON Việc nén trả về một dòng văn bản cho các đối tượng hợp lệ")
    public void jsonCompressReturnsCompactTextForValidObject() {
        Optional<String> compressed = RequestBodyPanel.compressBodyTextForDisplay(
                RequestBodyPanel.RAW_TYPE_JSON, """
                        {
                            "url": "https://chatgpt.com/backend-api/f/conversation",
                            "chatId": 1
                        }""");

        assertTrue(compressed.isPresent());
        assertEquals(compressed.get(), "{\"url\":\"https://chatgpt.com/backend-api/f/conversation\",\"chatId\":1}");
    }
}
