package com.nvhuong.devworkbench.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class JsonUtilTest {

    @Test(description = "Việc thoát và thoát phải giữ nguyên nội dung văn bản nhiều dòng")
    public void testEscapeAndUnescapeJsonStringContent_RoundTrip() {
        String original = "{\n  \"name\": \"demo\",\n  \"enabled\": true\n}";

        String escaped = JsonUtil.escapeJsonStringContent(original);
        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, original);
    }

    @Test(description = "Chống thoát không nên đặt chữ trong đường dẫn n và t Chuyển đổi không chính xác thành ký tự dòng mới hoặc tab")
    public void testUnescapeJsonStringContent_PathLikeText() {
        String escaped = "C:\\\\new\\\\temp\\\\tab";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "C:\\new\\temp\\tab");
    }

    @Test(description = "Tính năng chống thoát phải xử lý chính xác dấu gạch chéo ngược theo nghĩa đen cộng với các ký tự điều khiển")
    public void testUnescapeJsonStringContent_LiteralEscapedSequences() {
        String escaped = "\\\\n \\\\t \\\\r";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "\\n \\t \\r");
    }

    @Test(description = "Chống thoát hiểm nên được hỗ trợ unicode sự liên tiếp")
    public void testUnescapeJsonStringContent_Unicode() {
        String escaped = "\\u4f60\\u597d";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "Xin chào");
    }
}
