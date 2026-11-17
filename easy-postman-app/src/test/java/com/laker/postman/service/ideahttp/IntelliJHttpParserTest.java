package com.laker.postman.service.ideahttp;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.RequestAuthTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.collection.model.CollectionParseResult;
import org.testng.annotations.Test;

import java.util.Base64;

import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static org.testng.Assert.*;

/**
 * HttpFileParser Lớp kiểm tra đơn vị
 * Bài kiểm tra HTTP Các chức năng khác nhau của trình phân tích cú pháp tập tin
 */
public class IntelliJHttpParserTest {

    @Test(description = "Kiểm tra phân tích nội dung trống")
    public void testParseEmptyContent() {
        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(null, "test.http");
        assertNull(result, "null Nội dung nên được trả lại null");

        result = IntelliJHttpParser.parseHttpFile("", "test.http");
        assertNull(result, "Một chuỗi trống sẽ được trả về null");

        result = IntelliJHttpParser.parseHttpFile("   ", "test.http");
        assertNull(result, "Một chuỗi trống sẽ được trả về null");
    }

    @Test(description = "Kiểm tra phân tích cú pháp rất đơn giản GET yêu cầu")
    public void testParseSimpleGetRequest() {
        String content = "GET https://api.example.com/users";
        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");

        assertNotNull(result, "nên phân tích thành công");
        assertEquals(result.getChildren().size(), 1, "Nên có một yêu cầu");

        HttpRequestItem request = result.getChildren().get(0).asRequest();
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
        assertNotNull(request.getName(), "Cần có một tên yêu cầu");
    }

    @Test(description = "Kiểm tra các yêu cầu phân tích cú pháp bằng tên")
    public void testParseRequestWithName() {
        String content = """
                ### Lấy danh sách người dùng
                GET https://api.example.com/users
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem request = result.getChildren().get(0).asRequest();
        assertEquals(request.getName(), "Lấy danh sách người dùng");
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
    }

    @Test(description = "Kiểm tra phân tích cú pháp nhiều yêu cầu")
    public void testParseMultipleRequests() {
        String content = """
                ### Lấy danh sách người dùng
                GET https://api.example.com/users

                ### Tạo người dùng
                POST https://api.example.com/users
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 2, "Nên có hai yêu cầu");

        // Xác minh yêu cầu đầu tiên
        HttpRequestItem request1 = result.getChildren().get(0).asRequest();
        assertEquals(request1.getName(), "Lấy danh sách người dùng");
        assertEquals(request1.getMethod(), "GET");

        // Xác minh yêu cầu thứ hai
        HttpRequestItem request2 = result.getChildren().get(1).asRequest();
        assertEquals(request2.getName(), "Tạo người dùng");
        assertEquals(request2.getMethod(), "POST");
    }

    @Test(description = "Kiểm tra phân tích cú pháp POST Yêu cầu mang theo JSON body")
    public void testParsePostRequestWithJsonBody() {
        String content = """
                ### Tạo người dùng
                POST https://api.example.com/users
                Content-Type: application/json

                {
                  "name": "John Doe",
                  "email": "john@example.com"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getUrl(), "https://api.example.com/users");
        assertEquals(request.getBodyType(), "raw");
        assertTrue(request.getBody().contains("John Doe"));
        assertTrue(request.getBody().contains("john@example.com"));

        // Xác minh Content-Type cái đầu
        assertNotNull(request.getHeadersList());
        boolean hasContentType = request.getHeadersList().stream()
                .anyMatch(h -> "Content-Type".equals(h.getKey()) &&
                        h.getValue().contains("application/json"));
        assertTrue(hasContentType, "Nên có Content-Type cái đầu");
    }

    @Test(description = "Kiểm tra các yêu cầu phân tích cú pháp với nhiều tiêu đề yêu cầu")
    public void testParseRequestWithMultipleHeaders() {
        String content = """
                ### API yêu cầu
                GET https://api.example.com/data
                Accept: application/json
                User-Agent: EasyPostman/1.0
                X-Custom-Header: custom-value
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertNotNull(request.getHeadersList());
        assertEquals(request.getHeadersList().size(), 3, "Nên có 3 tiêu đề yêu cầu");

        // Xác minh tiêu đề yêu cầu
        assertTrue(hasHeader(request, "Accept", "application/json"));
        assertTrue(hasHeader(request, "User-Agent", "EasyPostman/1.0"));
        assertTrue(hasHeader(request, "X-Custom-Header", "custom-value"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp Basic Chứng nhận")
    public void testParseBasicAuth() {
        String content = """
                ### Một yêu cầu yêu cầu xác thực
                GET https://api.example.com/protected
                Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BASIC);

        // Xác minh Base64 giải mã
        String decoded = new String(Base64.getDecoder().decode("dXNlcm5hbWU6cGFzc3dvcmQ="));
        String[] parts = decoded.split(":", 2);
        assertEquals(request.getAuthUsername(), parts[0]);
        assertEquals(request.getAuthPassword(), parts[1]);
    }

    @Test(description = "Kiểm tra phân tích cú pháp Basic Xác thực (định dạng thay đổi)")
    public void testParseBasicAuthWithVariables() {
        String content = """
                ### Một yêu cầu yêu cầu xác thực
                GET https://api.example.com/protected
                Authorization: Basic {{username}} {{password}}
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(request.getAuthUsername(), "{{username}}");
        assertEquals(request.getAuthPassword(), "{{password}}");
    }

    @Test(description = "Kiểm tra phân tích cú pháp Bearer Chứng nhận")
    public void testParseBearerAuth() {
        String content = """
                ### Cần thiết Bearer Token lời yêu cầu
                GET https://api.example.com/protected
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(request.getAuthToken(), "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test(description = "Kiểm tra phân tích cú pháp urlencoded body")
    public void testParseUrlencodedBody() {
        String content = """
                ### Gửi biểu mẫu
                POST https://api.example.com/submit
                Content-Type: application/x-www-form-urlencoded

                name=John+Doe&email=john%40example.com&age=30
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "x-www-form-urlencoded");
        assertNotNull(request.getUrlencodedList());
        assertEquals(request.getUrlencodedList().size(), 3, "Nên có 3 cá nhân urlencoded lĩnh vực");

        // Xác thực các giá trị trường
        assertTrue(hasUrlencodedField(request, "name", "John+Doe"));
        assertTrue(hasUrlencodedField(request, "email", "john%40example.com"));
        assertTrue(hasUrlencodedField(request, "age", "30"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp multipart/form-data body")
    public void testParseMultipartFormData() {
        String content = """
                ### Send a form with text and file fields
                POST https://examples.http-client.intellij.net/post
                Content-Type: multipart/form-data; boundary=WebAppBoundary

                --WebAppBoundary
                Content-Disposition: form-data; name="element-name"
                Content-Type: text/plain

                Name
                --WebAppBoundary
                Content-Disposition: form-data; name="data"; filename="data.json"
                Content-Type: application/json

                < ./request-form-data.json
                --WebAppBoundary--
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "form-data");
        assertNotNull(request.getFormDataList());
        assertEquals(request.getFormDataList().size(), 2, "Nên có 2 cá nhân form-data lĩnh vực");

        // Xác thực các trường văn bản
        boolean hasTextField = request.getFormDataList().stream()
                .anyMatch(f -> "element-name".equals(f.getKey()) &&
                        "Name".equals(f.getValue()) &&
                        f.isText());
        assertTrue(hasTextField, "Nó nên được đặt tên element-name trường văn bản");

        // Xác thực các trường tệp
        boolean hasFileField = request.getFormDataList().stream()
                .anyMatch(f -> "data".equals(f.getKey()) &&
                        f.getValue().contains("request-form-data.json") &&
                        !f.isText());
        assertTrue(hasFileField, "Nó nên được đặt tên data các trường tập tin");
    }

    @Test(description = "Kiểm tra phân tích cú pháp khác nhau HTTP phương pháp")
    public void testParseVariousHttpMethods() {
        String content = """
                ### GET yêu cầu
                GET https://api.example.com/get

                ### POST yêu cầu
                POST https://api.example.com/post

                ### PUT yêu cầu
                PUT https://api.example.com/put

                ### DELETE yêu cầu
                DELETE https://api.example.com/delete

                ### PATCH yêu cầu
                PATCH https://api.example.com/patch

                ### HEAD yêu cầu
                HEAD https://api.example.com/head

                ### OPTIONS yêu cầu
                OPTIONS https://api.example.com/options
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 7, "Nên có 7 lời yêu cầu");

        assertEquals(getRequestFromResult(result, 0).getMethod(), "GET");
        assertEquals(getRequestFromResult(result, 1).getMethod(), "POST");
        assertEquals(getRequestFromResult(result, 2).getMethod(), "PUT");
        assertEquals(getRequestFromResult(result, 3).getMethod(), "DELETE");
        assertEquals(getRequestFromResult(result, 4).getMethod(), "PATCH");
        assertEquals(getRequestFromResult(result, 5).getMethod(), "HEAD");
        assertEquals(getRequestFromResult(result, 6).getMethod(), "OPTIONS");
    }

    @Test(description = "Kiểm tra phân tích cú pháp POST Yêu cầu nhiều dòng body")
    public void testParsePostRequestWithMultilineBody() {
        String content = """
                ### Tạo các đối tượng phức tạp
                POST https://api.example.com/complex
                Content-Type: application/json

                {
                  "name": "John",
                  "address": {
                    "street": "123 Main St",
                    "city": "New York"
                  },
                  "tags": ["tag1", "tag2"]
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "raw");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"name\": \"John\""));
        assertTrue(request.getBody().contains("\"street\": \"123 Main St\""));
        assertTrue(request.getBody().contains("\"tags\":"));
    }

    @Test(description = "Kiểm tra các yêu cầu phân tích cú pháp không có tên (sử dụng  URL như tên)")
    public void testParseRequestWithoutName() {
        String content = """
                ###
                GET https://api.example.com/users/profile
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        // Nếu không có tên thì nên sử dụng URL phần đường dẫn như tên
        assertNotNull(request.getName());
        assertFalse(request.getName().isEmpty());
    }

    @Test(description = "Kiểm tra phân tích cú pháp SSE yêu cầu")
    public void testParseSSERequest() {
        String content = """
                ### SSE Truyền phát các yêu cầu
                GET https://api.example.com/events
                Accept: text/event-stream
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getProtocol(), RequestItemProtocolEnum.HTTP);
        assertTrue(hasHeader(request, "Accept", "text/event-stream"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp WebSocket yêu cầu")
    public void testParseWebSocketRequest() {
        String content = """
                ### WebSocket kết nối
                GET wss://echo-websocket.hoppscotch.io/
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getProtocol(), RequestItemProtocolEnum.WEBSOCKET);
        assertEquals(request.getUrl(), "wss://echo-websocket.hoppscotch.io/");
    }

    @Test(description = "Kiểm tra các yêu cầu phân tích cú pháp có chứa các dòng trống")
    public void testParseRequestWithEmptyLines() {
        String content = """
                ### Yêu cầu có dòng trống
                POST https://api.example.com/data
                Content-Type: application/json


                {
                  "data": "value"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"data\": \"value\""));
    }

    @Test(description = "Kiểm tra phân tích cú pháp một yêu cầu duy nhất mà không có dấu phân cách")
    public void testParseSingleRequestWithoutSeparator() {
        String content = "GET https://api.example.com/users";
        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
    }

    @Test(description = "Kiểm tra phân tích cú pháp với các tham số truy vấn URL")
    public void testParseRequestWithQueryParams() {
        String content = """
                ### Yêu cầu tìm kiếm
                GET https://api.example.com/search?q=test&limit=10&page=1
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getUrl(), "https://api.example.com/search?q=test&limit=10&page=1");
    }

    @Test(description = "Kiểm tra các nút nhóm phân tích cú pháp")
    public void testParseGroupNode() {
        String content = """
                ### Yêu cầu đầu tiên
                GET https://api.example.com/one

                ### Yêu cầu thứ hai
                POST https://api.example.com/two
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        // Xác minh thông tin nhóm
        RequestGroup group = result.getGroup();
        assertNotNull(group);
        assertNotNull(group.getName());
        assertEquals(result.getChildren().size(), 2);
    }

    @Test(description = "Kiểm tra phân tích cú pháp yêu cầu không hợp lệ (không URL）")
    public void testParseInvalidRequestWithoutUrl() {
        String content = """
                ### Yêu cầu không hợp lệ
                GET
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        // Không hợp lệ URL Yêu cầu không nên được thêm vào kết quả
        assertNull(result, "Không hợp lệ URL Yêu cầu nên được trả lại null");
    }

    @Test(description = "Kiểm tra phân tích giá trị tiêu đề yêu cầu bằng các ký tự đặc biệt")
    public void testParseHeaderWithSpecialCharacters() {
        String content = """
                ### Kiểm tra ký tự đặc biệt
                GET https://api.example.com/test
                X-Custom: value:with:colons
                X-Another: value with spaces
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertTrue(hasHeader(request, "X-Custom", "value:with:colons"));
        assertTrue(hasHeader(request, "X-Another", "value with spaces"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp DELETE Yêu cầu mang theo body")
    public void testParseDeleteRequestWithBody() {
        String content = """
                ### Xóa tài nguyên
                DELETE https://api.example.com/resource/123
                Content-Type: application/json

                {
                  "reason": "No longer needed"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "DELETE");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"reason\""));
    }

    @Test(description = "Kiểm tra phân tích cú pháp PUT yêu cầu")
    public void testParsePutRequest() {
        String content = """
                ### Cập nhật tài nguyên
                PUT https://api.example.com/resource/123
                Content-Type: application/json

                {
                  "name": "Updated Name"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "PUT");
        assertEquals(request.getBodyType(), "raw");
        assertTrue(request.getBody().contains("Updated Name"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp urlencoded body Với giá trị null")
    public void testParseUrlencodedWithEmptyValues() {
        String content = """
                ### Gửi biểu mẫu
                POST https://api.example.com/form
                Content-Type: application/x-www-form-urlencoded

                name=John&email=&age=30&empty=
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getBodyType(), "x-www-form-urlencoded");
        assertTrue(hasUrlencodedField(request, "name", "John"));
        assertTrue(hasUrlencodedField(request, "email", ""));
        assertTrue(hasUrlencodedField(request, "age", "30"));
        assertTrue(hasUrlencodedField(request, "empty", ""));
    }

    @Test(description = "Kiểm tra và phân tích các tình huống phức tạp: nhiều yêu cầu với nhiều cấu hình khác nhau")
    public void testParseComplexScenario() {
        String content = """
                ### Nhận người dùng
                GET https://api.example.com/users
                Accept: application/json
                Authorization: Bearer token123

                ### Tạo người dùng
                POST https://api.example.com/users
                Content-Type: application/json
                Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=

                {
                  "name": "John",
                  "email": "john@example.com"
                }

                ### Gửi biểu mẫu
                POST https://api.example.com/form
                Content-Type: application/x-www-form-urlencoded

                name=John&email=john@example.com
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 3, "Nên có 3 lời yêu cầu");

        // Xác minh yêu cầu đầu tiên (GET with Bearer）
        HttpRequestItem req1 = getRequestFromResult(result, 0);
        assertEquals(req1.getMethod(), "GET");
        assertEquals(req1.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(req1.getAuthToken(), "token123");

        // Xác minh yêu cầu thứ hai (POST with JSON and Basic Auth）
        HttpRequestItem req2 = getRequestFromResult(result, 1);
        assertEquals(req2.getMethod(), "POST");
        assertEquals(req2.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(req2.getBodyType(), "raw");
        assertTrue(req2.getBody().contains("John"));

        // Xác minh yêu cầu thứ ba (POST with urlencoded）
        HttpRequestItem req3 = getRequestFromResult(result, 2);
        assertEquals(req3.getMethod(), "POST");
        assertEquals(req3.getBodyType(), "x-www-form-urlencoded");
        assertTrue(hasUrlencodedField(req3, "name", "John"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp các yêu cầu có chú thích (# đầu)")
    public void testParseRequestWithComments() {
        String content = """
                ### Được chứng nhận Token
                # @no-cookie-jar
                # @no-log
                # @name GetAuthToken
                POST https://api.example.com/auth/token
                Content-Type: application/json

                {
                  "username": "testuser",
                  "password": "testpass"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getName(), "Được chứng nhận Token");
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getUrl(), "https://api.example.com/auth/token");
    }

    @Test(description = "Kiểm tra phân tích cú pháp nhiều dòng urlencoded body")
    public void testParseMultilineUrlencodedBody() {
        String content = """
                ### Gửi biểu mẫu nhiều dòng
                POST https://api.example.com/form
                Content-Type: application/x-www-form-urlencoded

                grant_type = password &
                username = testuser &
                password = testpass &
                scope = read write
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "x-www-form-urlencoded");
        assertNotNull(request.getUrlencodedList());

        // Xác thực các trường
        assertTrue(hasUrlencodedField(request, "grant_type", "password"));
        assertTrue(hasUrlencodedField(request, "username", "testuser"));
        assertTrue(hasUrlencodedField(request, "password", "testpass"));
        assertTrue(hasUrlencodedField(request, "scope", "read write"));
    }

    @Test(description = "Kiểm tra các yêu cầu phân tích cú pháp bằng các tập lệnh xử lý phản hồi")
    public void testParseRequestWithResponseScript() {
        String content = """
                ### Lấy Token
                POST https://api.example.com/token
                Content-Type: application/json

                {
                  "username": "test"
                }

                > {% client.global.set("token", response.body.token); %}

                ### sử dụng Token
                GET https://api.example.com/data
                Authorization: Bearer {{token}}
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 2, "nên được phân tích cú pháp 2 lời yêu cầu");

        HttpRequestItem req1 = getRequestFromResult(result, 0);
        assertEquals(req1.getName(), "Lấy Token");
        assertEquals(req1.getMethod(), "POST");

        // Tập lệnh phản hồi xác thực được chuyển đổi thành Postman định dạng
        assertNotNull(req1.getPostscript(), "Nên có phần tái bút");
        assertFalse(req1.getPostscript().isEmpty(), "Phần tái bút không được để trống");
        assertTrue(req1.getPostscript().contains("pm.environment.set"), "nên chuyển đổi thành  pm.environment.set");
        assertTrue(req1.getPostscript().contains("pm.response.json()"), "nên chuyển đổi thành  pm.response.json()");
        assertFalse(req1.getPostscript().contains("client.global.set"), "không nên được đưa vào client.global.set");

        HttpRequestItem req2 = getRequestFromResult(result, 1);
        assertEquals(req2.getName(), "sử dụng Token");
        assertEquals(req2.getMethod(), "GET");
    }

    @Test(description = "Kiểm tra các kịch bản khác nhau để chuyển đổi tập lệnh")
    public void testScriptConversion() {
        String content = """
                ### Kiểm tra các cú pháp tập lệnh khác nhau
                POST https://api.example.com/test

                > {%
                client.global.set("var1", response.body.value1);
                client.global.set("var2", response.body.data.value2);
                client.global.get("existingVar");
                client.test("Status is 200", function() {
                    return response.status === 200;
                });
                %}
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem req = getRequestFromResult(result, 0);
        String postscript = req.getPostscript();

        // In tập lệnh để gỡ lỗi
        System.out.println("Kịch bản đã chuyển đổi:");
        System.out.println(postscript);
        System.out.println("---");

        assertNotNull(postscript, "Nên có phần tái bút");
        assertTrue(postscript.contains("pm.environment.set(\"var1\""), "nên được chuyển đổi client.global.set");
        assertTrue(postscript.contains("pm.response.json().value1"), "nên được chuyển đổi response.body.value1");
        assertTrue(postscript.contains("pm.response.json().data.value2"), "nên được chuyển đổi response.body.data.value2");
        assertTrue(postscript.contains("pm.environment.get(\"existingVar\")"), "nên được chuyển đổi client.global.get");
        assertTrue(postscript.contains("pm.test(\"Status is 200\""), "nên được chuyển đổi client.test");
        assertTrue(postscript.contains("pm.response.code"), "nên được chuyển đổi response.status");
    }

    @Test(description = "Kiểm tra và phân tích toàn bộ quy trình chứng nhận (trường hợp thực tế)")
    public void testParseRealWorldAuthFlow() {
        String content = """
                ### STEP 1: Lấy Bearer Token
                # @no-cookie-jar
                # @no-log
                # @name GetBearerToken
                POST https://{{auth_server}}/api/v1/auth/bearer-token
                Content-Type: application/json

                {
                  "username": "{{username}}",
                  "password": "{{password}}"
                }

                > {% client.global.set("bearerToken", response.body.token); %}

                ### STEP 2: phát ra Access Token
                # @no-cookie-jar
                # @no-log
                # @name GenerateAccessToken
                POST https://{{auth_server}}/api/v1/oauth/token
                Content-Type: application/x-www-form-urlencoded

                grant_type = {{grant_type}} &
                assertion = {{bearerToken}} &
                scope = {{scope}} &
                include_refresh_token = true &
                client_id = {{client_id}} &
                client_secret = {{client_secret}}

                > {% client.global.set("accessToken", response.body.access_token); %}

                ### STEP 3: Xác minh Access Token
                # @no-cookie-jar
                # @no-log
                # @name ValidateAccessToken
                POST https://{{auth_server}}/api/v1/oauth/validate
                Authorization: Basic {{client_id}} {{client_secret}}
                Content-Type: application/x-www-form-urlencoded

                access_token = {{accessToken}}
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result, "nên phân tích thành công");
        assertEquals(result.getChildren().size(), 3, "Nên có 3 lời yêu cầu");

        // Xác minh STEP 1
        HttpRequestItem req1 = getRequestFromResult(result, 0);
        assertEquals(req1.getName(), "STEP 1: Lấy Bearer Token");
        assertEquals(req1.getMethod(), "POST");
        assertTrue(req1.getUrl().contains("bearer-token"));
        assertEquals(req1.getBodyType(), "raw");
        assertTrue(req1.getBody().contains("{{username}}"));

        // Xác minh STEP 1 kịch bản phản hồi
        assertNotNull(req1.getPostscript(), "STEP 1 Nên có phần tái bút");
        assertTrue(req1.getPostscript().contains("pm.environment.set(\"bearerToken\""), "nên được thiết lập bearerToken");
        assertTrue(req1.getPostscript().contains("pm.response.json().token"), "Nên thu được từ phản hồi token");

        // Xác minh STEP 2
        HttpRequestItem req2 = getRequestFromResult(result, 1);
        assertEquals(req2.getName(), "STEP 2: phát ra Access Token");
        assertEquals(req2.getMethod(), "POST");
        assertEquals(req2.getBodyType(), "x-www-form-urlencoded");
        assertTrue(hasUrlencodedField(req2, "grant_type", "{{grant_type}}"));
        assertTrue(hasUrlencodedField(req2, "assertion", "{{bearerToken}}"));
        assertTrue(hasUrlencodedField(req2, "scope", "{{scope}}"));

        // Xác minh STEP 2 kịch bản phản hồi
        assertNotNull(req2.getPostscript(), "STEP 2 Nên có phần tái bút");
        assertTrue(req2.getPostscript().contains("pm.environment.set(\"accessToken\""), "nên được thiết lập accessToken");
        assertTrue(req2.getPostscript().contains("pm.response.json().access_token"), "Nên thu được từ phản hồi access_token");

        // Xác minh STEP 3
        HttpRequestItem req3 = getRequestFromResult(result, 2);
        assertEquals(req3.getName(), "STEP 3: Xác minh Access Token");
        assertEquals(req3.getMethod(), "POST");
        assertEquals(req3.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(req3.getAuthUsername(), "{{client_id}}");
        assertEquals(req3.getAuthPassword(), "{{client_secret}}");
        assertEquals(req3.getBodyType(), "x-www-form-urlencoded");
        assertTrue(hasUrlencodedField(req3, "access_token", "{{accessToken}}"));
    }

    // Phương pháp phụ trợ: Thu được từ kết quả phân tích cú pháp HttpRequestItem
    private HttpRequestItem getRequestFromResult(CollectionParseResult result, int index) {
        return result.getChildren().get(index).asRequest();
    }

    // Phương thức trợ giúp: Kiểm tra xem yêu cầu có tiêu đề yêu cầu được chỉ định hay không
    private boolean hasHeader(HttpRequestItem request, String key, String value) {
        if (request.getHeadersList() == null) {
            return false;
        }
        return request.getHeadersList().stream()
                .anyMatch(h -> key.equals(h.getKey()) && value.equals(h.getValue()));
    }

    // Phương thức trợ giúp: Kiểm tra xem yêu cầu có được chỉ định hay không urlencoded lĩnh vực
    private boolean hasUrlencodedField(HttpRequestItem request, String key, String value) {
        if (request.getUrlencodedList() == null) {
            return false;
        }
        return request.getUrlencodedList().stream()
                .anyMatch(f -> key.equals(f.getKey()) && value.equals(f.getValue()));
    }

    // Phương thức trợ giúp: Kiểm tra xem nhóm có biến được chỉ định hay không
    private boolean hasVariable(RequestGroup group, String key, String value) {
        if (group.getVariables() == null) {
            return false;
        }
        return group.getVariables().stream()
                .anyMatch(v -> key.equals(v.getKey()) && value.equals(v.getValue()));
    }

    @Test(description = "Kiểm tra các định nghĩa biến môi trường phân tích cú pháp (@ đầu)")
    public void testParseEnvironmentVariables() {
        String content = """
                ##############################################################################################################################################
                ### CI Service Integration
                ##############################################################################################################################################
                @ciUserId=test-user-uuid-12345
                @orgId=test-org-uuid-67890
                @displayName=Test User
                @userEmail=test@example.com

                ### get user by uuid
                GET {{ciUrl}}/identity/scim/v1/Users?startIndex=0&filter=id eq "{{ciUserId}}"
                Content-Type: application/json
                Authorization: Bearer {{accessToken}}
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "ci-service.http");
        assertNotNull(result, "nên phân tích thành công");

        // Xác thực các biến cấp độ nhóm
        RequestGroup group = result.getGroup();
        assertNotNull(group, "Nên có các đối tượng được nhóm lại");
        assertNotNull(group.getVariables(), "Nhóm nên có danh sách các biến");
        assertFalse(group.getVariables().isEmpty(), "Danh sách biến không được để trống");
        assertEquals(group.getVariables().size(), 4, "Nên có4các biến");

        // Xác thực các giá trị biến
        assertTrue(hasVariable(group, "ciUserId", "test-user-uuid-12345"));
        assertTrue(hasVariable(group, "orgId", "test-org-uuid-67890"));
        assertTrue(hasVariable(group, "displayName", "Test User"));
        assertTrue(hasVariable(group, "userEmail", "test@example.com"));

        // Xác minh rằng yêu cầu đã được phân tích cú pháp chính xác
        assertEquals(result.getChildren().size(), 1, "Nên có một yêu cầu");
        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getName(), "get user by uuid");
        assertTrue(request.getUrl().contains("{{ciUserId}}"), "URL Nên chứa các tham chiếu biến");
    }

    @Test(description = "Kiểm tra phân tích cú pháp nhiều yêu cầu và biến môi trường")
    public void testParseMultipleRequestsWithVariables() {
        String content = """
                @ciUrl=https://api.example.com
                @accessToken=test-token-123

                ### get user by email
                GET {{ciUrl}}/users?email={{userEmail}}
                Authorization: Bearer {{accessToken}}

                ### create user
                POST {{ciUrl}}/users
                Content-Type: application/json
                Authorization: Bearer {{accessToken}}

                {
                  "email": "{{userEmail}}",
                  "name": "{{displayName}}"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "test.http");
        assertNotNull(result);

        // Xác thực các biến cấp độ nhóm
        RequestGroup group = result.getGroup();
        assertNotNull(group.getVariables());
        assertEquals(group.getVariables().size(), 2, "Nên có2các biến");
        assertTrue(hasVariable(group, "ciUrl", "https://api.example.com"));
        assertTrue(hasVariable(group, "accessToken", "test-token-123"));

        // Xác minh cả hai yêu cầu
        assertEquals(result.getChildren().size(), 2);
    }

    @Test(description = "Kiểm tra phân tích cú pháp JetBrains chính thức # @name、pre-request và nội tuyến response handler")
    public void testParseJetBrainsOfficialScriptSyntax() {
        String content = """
                # @name CreatePet
                < {% request.variables.set("petName", "Bella") %}
                POST https://example.org/pets/{{petName}} HTTP/2 > {% client.global.set("createdPet", response.body.id); %}
                Content-Type: application/json

                {
                  "name": "{{petName}}"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "pets.http");
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getName(), "CreatePet");
        assertEquals(request.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_2);
        assertEquals(request.getPrescript(), "pm.variables.set(\"petName\", \"Bella\")");
        assertTrue(request.getPostscript().contains("pm.environment.set(\"createdPet\""));
        assertTrue(request.getPostscript().contains("pm.response.json().id"));
        assertTrue(request.getUrl().contains("{{petName}}"));
    }

    @Test(description = "Kiểm tra phân tích cú pháp nhiều dòng pre-request kịch bản")
    public void testParseMultilinePreRequestScript() {
        String content = """
                # @name Login
                < {%
                request.variables.set("user", "demo");
                client.global.set("token", "abc");
                %}
                POST https://example.org/login HTTP/1.1
                Content-Type: application/json

                {
                  "user": "{{user}}"
                }
                """;

        CollectionParseResult result = IntelliJHttpParser.parseHttpFile(content, "login.http");
        assertNotNull(result);

        HttpRequestItem request = getRequestFromResult(result, 0);
        assertEquals(request.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        assertTrue(request.getPrescript().contains("pm.variables.set(\"user\", \"demo\")"));
        assertTrue(request.getPrescript().contains("pm.environment.set(\"token\", \"abc\")"));
    }

}
