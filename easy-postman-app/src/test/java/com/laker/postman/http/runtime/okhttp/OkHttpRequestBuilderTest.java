package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;


import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

import static org.testng.Assert.*;

/**
 * OkHttpRequestBuilder Kiểm tra đơn vị
 */
public class OkHttpRequestBuilderTest {

    private PreparedRequest request;

    @BeforeMethod
    public void setUp() {
        request = new PreparedRequest();
        request.url = "https://api.example.com/test";
        request.method = "POST";
        request.headersList = new ArrayList<>();
    }

    // ==================== Kiểm tra yêu cầu cơ bản ====================

    @Test(description = "Bài kiểm tra GET Yêu cầu xây dựng")
    public void testBuildGetRequest() {
        request.method = "GET";
        request.headersList.add(new HttpHeader(true, "Accept", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.url().toString(), request.url);
        assertEquals(okRequest.method(), "GET");
        assertNull(okRequest.body(), "GET The request should not have body");
        assertEquals(okRequest.header("Accept"), "application/json");
    }

    @Test(description = "Bài kiểm tra POST Yêu cầu xây dựng")
    public void testBuildPostRequest() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"name\":\"test\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());

        String bodyContent = readRequestBody(okRequest.body());
        assertEquals(bodyContent, "{\"name\":\"test\"}");
    }

    @Test(description = "Bài kiểm tra PUT Yêu cầu xây dựng")
    public void testBuildPutRequest() {
        request.method = "PUT";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"id\":1,\"name\":\"updated\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "PUT");
        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra DELETE Yêu cầu xây dựng")
    public void testBuildDeleteRequest() {
        request.method = "DELETE";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "DELETE");
        assertNotNull(okRequest.body(), "DELETE Yêu cầu phải có sẵn body");
    }

    @Test(description = "Bài kiểm tra HEAD Yêu cầu xây dựng")
    public void testBuildHeadRequest() {
        request.method = "HEAD";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "HEAD");
        assertNull(okRequest.body(), "HEAD The request should not have body");
    }

    // ==================== JSON5 Kiểm tra loại bỏ bình luận ====================

    @Test(description = "Bài kiểm tra JSON5 Đã xóa nhận xét một dòng")
    public void testJson5SingleLineComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  // Đây là tên người dùng
                  "username": "test",
                  // Đây là mật khẩu
                  "password": "123456"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "Nhận xét một dòng nên được loại bỏ");
        assertFalse(bodyContent.contains("Đây là tên người dùng"), "Nội dung bình luận nên được loại bỏ");
        assertTrue(bodyContent.contains("username"), "Các trường nên được giữ lại");
        assertTrue(bodyContent.contains("test"), "Giá trị nên được giữ lại");
    }

    @Test(description = "Bài kiểm tra JSON5 Xóa bình luận nhiều dòng")
    public void testJson5MultiLineComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  /* Đây là một
                     Bình luận nhiều dòng */
                  "name": "John",
                  /* Thông tin về tuổi */
                  "age": 30
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("/*"), "Những phần bắt đầu bình luận nhiều dòng nên được loại bỏ");
        assertFalse(bodyContent.contains("*/"), "Các đầu cuối nhận xét nhiều dòng nên được loại bỏ");
        assertFalse(bodyContent.contains("Bình luận nhiều dòng"), "Nội dung bình luận nên được loại bỏ");
        assertTrue(bodyContent.contains("name"), "Các trường nên được giữ lại");
        assertTrue(bodyContent.contains("John"), "Giá trị nên được giữ lại");
    }

    @Test(description = "Bài kiểm tra JSON5 Loại bỏ chú thích hỗn hợp")
    public void testJson5MixedComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  // Thông tin người dùng
                  "user": {
                    /* Tên */
                    "name": "Alice",
                    // Email
                    "email": "alice@example.com"
                  },
                  /* Thiết lập các mục
                     Chứa nhiều trường */
                  "settings": {
                    "theme": "dark" // Cài đặt chủ đề
                  }
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "Nhận xét một dòng nên được loại bỏ");
        assertFalse(bodyContent.contains("/*"), "Bình luận nhiều dòng nên được loại bỏ");
        assertTrue(bodyContent.contains("user"), "Các trường nên được giữ lại");
        assertTrue(bodyContent.contains("Alice"), "Giá trị nên được giữ lại");
        assertTrue(bodyContent.contains("settings"), "Các trường nên được giữ lại");
    }

    @Test(description = "Kiểm tra Không có Content-Type Nhấn theo mặc định JSON Xử lý và xóa nhận xét")
    public void testDefaultJsonWithComments() throws IOException {
        // Chưa được thiết lập Content-Type，nên mặc định là  JSON
        request.body = """
                {
                  // Trường thử nghiệm
                  "test": "value"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "Mặc định JSON Định dạng nên được loại bỏ các bình luận");
        assertTrue(bodyContent.contains("test"), "Các trường nên được giữ lại");
        assertTrue(bodyContent.contains("value"), "Giá trị nên được giữ lại");
        assertNotNull(okRequest.body());
        assertEquals(Objects.requireNonNull(okRequest.body().contentType()).toString(), "application/json; charset=utf-8");
    }

    @Test(description = "Bài kiểm tra ở mức trung bình JSON Không bị ảnh hưởng")
    public void testNormalJsonNotAffected() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  "name": "Bob",
                  "age": 25,
                  "active": true
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertTrue(bodyContent.contains("name"), "Các trường nên được giữ lại");
        assertTrue(bodyContent.contains("Bob"), "Giá trị nên được giữ lại");
        assertTrue(bodyContent.contains("25"), "Giá trị nên được giữ lại");
    }

    @Test(description = "Kiểm tra thì không JSON Content-Type Bình luận không được xử lý")
    public void testNonJsonContentTypeNotProcessed() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "text/plain"));
        request.body = "// This is not JSON\nSome text content";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertEquals(bodyContent, request.body, "Không JSON Nội dung nên giữ nguyên như cũ");
        assertTrue(bodyContent.contains("//"), "Không JSON Nội dung bình luận không nên bị loại bỏ");
    }

    @Test(description = "Bài kiểm tra JSON Content-Type Các biến thể")
    public void testJsonContentTypeVariants() throws IOException {
        String[] jsonContentTypes = {
                "application/json",
                "application/json; charset=utf-8",
                "application/json;charset=UTF-8",
                "Application/JSON",
                "APPLICATION/JSON"
        };

        for (String contentType : jsonContentTypes) {
            PreparedRequest req = new PreparedRequest();
            req.url = "https://api.example.com/test";
            req.method = "POST";
            req.headersList = new ArrayList<>();
            req.headersList.add(new HttpHeader(true, "Content-Type", contentType));
            req.body = "{ // comment\n\"test\": \"value\" }";

            Request okRequest = OkHttpRequestBuilder.buildRequest(req);
            String bodyContent = readRequestBody(okRequest.body());

            assertFalse(bodyContent.contains("//"),
                    "Content-Type: " + contentType + " Bình luận nên được loại bỏ");
        }
    }

    // ==================== Headers Bài kiểm tra ====================

    @Test(description = "Kiểm tra việc thêm nhiều Headers")
    public void testMultipleHeaders() {
        request.headersList.add(new HttpHeader(true, "Accept", "application/json"));
        request.headersList.add(new HttpHeader(true, "Authorization", "Bearer token123"));
        request.headersList.add(new HttpHeader(true, "X-Custom-Header", "custom-value"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.header("Accept"), "application/json");
        assertEquals(okRequest.header("Authorization"), "Bearer token123");
        assertEquals(okRequest.header("X-Custom-Header"), "custom-value");
    }

    @Test(description = "Bài kiểm tra null Header giá trị")
    public void testNullHeaderValue() {
        request.headersList.add(new HttpHeader(true, "X-Test", null));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        // OkHttp returns empty string for non-existent headers, and null values are skipped
        String headerValue = okRequest.header("X-Test");
        assertTrue(headerValue == null || headerValue.isEmpty(), "null Đáng giá header Nên bỏ qua hoặc trả về một chuỗi trống");
    }

    @Test(description = "Việc kiểm tra là bất hợp pháp Header Tên bị bỏ qua")
    public void testInvalidHeaderNames() {
        request.headersList.add(new HttpHeader(true, "Valid-Header", "value1"));
        request.headersList.add(new HttpHeader(true, "Invalid:Header", "value2")); // Chứa dấu hai chấm, bất hợp pháp
        request.headersList.add(new HttpHeader(true, "", "value3")); // Tên trống, bất hợp pháp
        request.headersList.add(new HttpHeader(true, null, "value4")); // null Tên, bất hợp pháp

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.header("Valid-Header"), "value1");
        assertNull(okRequest.header("Invalid:Header"), "bất hợp pháp header nên được bỏ qua");
    }

    // ==================== vô giá trị Body Bài kiểm tra ====================

    @Test(description = "Kiểm tra trống rỗng Body")
    public void testEmptyBody() throws IOException {
        request.body = "";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest.body());
        assertEquals(readRequestBody(okRequest.body()), "", "vô giá trị body Phải là một chuỗi trống");
    }

    @Test(description = "Bài kiểm tra null Body")
    public void testNullBody() throws IOException {
        request.body = null;
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest.body());
        assertEquals(readRequestBody(okRequest.body()), "", "null body Phải là một chuỗi trống");
    }

    // ==================== Multipart Form Data Bài kiểm tra ====================

    @Test(description = "Xây dựng thử nghiệm Multipart yêu cầu")
    public void testBuildMultipartRequest() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());
        assertTrue(Objects.requireNonNull(okRequest.body().contentType()).toString().contains("multipart/form-data"));
    }

    @Test(description = "Bài kiểm tra Multipart Trường giá trị null")
    public void testMultipartWithNullValue() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, null));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra Multipart Các phím trống được bỏ qua")
    public void testMultipartWithEmptyKey() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra Multipart Tải lên tập tin")
    public void testMultipartWithFiles() throws IOException {
        // Tạo tập tin thử nghiệm tạm thời
        File tempFile = Files.createTempFile("test", ".txt").toFile();
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "test content");

        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, tempFile.getAbsolutePath()));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
        assertTrue(Objects.requireNonNull(okRequest.body().contentType()).toString().contains("multipart/form-data"));
    }

    @Test(description = "Bài kiểm tra Multipart Các tập tin không tồn tại sẽ bị bỏ qua")
    public void testMultipartWithNonExistentFile() {
        // Hãy đảm bảo thêm trường biểu mẫu hợp lệ multipart Có ít nhất một phần
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "/path/to/nonexistent/file.txt"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
        // Các tệp không tồn tại sẽ bị bỏ qua nhưng dữ liệu biểu mẫu sẽ tồn tại
    }

    // ==================== URL Encoded Form Bài kiểm tra ====================

    @Test(description = "Xây dựng thử nghiệm URL Encoded Form yêu cầu")
    public void testBuildFormRequest() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "username", "test"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "password", "123456"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());
        assertEquals(okRequest.header("Content-Type"),
                "application/x-www-form-urlencoded; charset=UTF-8");
    }

    @Test(description = "Bài kiểm tra Form Yêu cầu vẫn do người dùng chỉ định Content-Type")
    public void testFormRequestWithCustomContentType() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field", "value"));
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/x-www-form-urlencoded; charset=ISO-8859-1"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertEquals(okRequest.header("Content-Type"),
                "application/x-www-form-urlencoded; charset=ISO-8859-1",
                "Nên được giữ lại theo chỉ định của người dùng Content-Type");
    }

    @Test(description = "Bài kiểm tra Form yêu cầu null xử lý giá trị")
    public void testFormRequestWithNullValue() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field1", null));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field2", "value2"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra Form Yêu cầu về các phím trống sẽ bị bỏ qua")
    public void testFormRequestWithEmptyKey() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "", "value1"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field2", "value2"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest.body());
    }

    // ==================== Kiểm tra trường hợp ranh giới ====================

    @Test(description = "Thử nghiệm thì không headers lời yêu cầu")
    public void testRequestWithoutHeaders() {
        request.headersList = null;
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        // Nên sử dụng mặc định JSON Content-Type
        assertNotNull(okRequest.body());
        assertEquals(Objects.requireNonNull(okRequest.body().contentType()).toString(), "application/json; charset=utf-8");
    }

    @Test(description = "Kiểm tra trống rỗng headers List")
    public void testRequestWithEmptyHeaders() {
        request.headersList = new ArrayList<>();
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
    }

    @Test(description = "Tên phương pháp thử nghiệm có phân biệt chữ hoa chữ thường")
    public void testMethodCaseInsensitive() {
        request.method = "post";
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "POST", "Tên phương thức nên được chuyển đổi thành chữ hoa");
    }

    @Test(description = "Bài kiểm tra PATCH phương pháp")
    public void testPatchMethod() {
        request.method = "PATCH";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"status\":\"updated\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "PATCH");
        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra OPTIONS phương pháp")
    public void testOptionsMethod() {
        request.method = "OPTIONS";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "OPTIONS");
        assertNotNull(okRequest.body());
    }

    @Test(description = "Bài kiểm tra không hợp lệ JSON Sẽ không gây ra sự cố")
    public void testInvalidJsonWithComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{ // comment\n invalid json here }";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertNotNull(okRequest.body());
        // nếu JSON Nội dung gốc không hợp lệ nên được giữ lại
        String bodyContent = readRequestBody(okRequest.body());
        assertNotNull(bodyContent);
    }

    @Test(description = "Kiểm tra các ký tự đặc biệt JSON")
    public void testJsonWithSpecialCharacters() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  "text": "Hello\\nWorld\\t!",
                  "unicode": "Bài kiểm tra tiếng Trung",
                  "emoji": "😀"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        String bodyContent = readRequestBody(okRequest.body());
        assertTrue(bodyContent.contains("Bài kiểm tra tiếng Trung"));
    }

    // ==================== Các phương pháp trợ giúp ====================

    /**
     * Đọc RequestBody nội dung
     */
    private String readRequestBody(RequestBody body) throws IOException {
        if (body == null) {
            return "";
        }
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }
}

