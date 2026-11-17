package com.laker.postman.service.curl;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.TransportAuth;
import com.laker.postman.request.util.HttpUrlUtil;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * CurlParser Lớp kiểm tra đơn vị
 * Bài kiểm tra cURL Các tính năng khác nhau của trình phân tích cú pháp lệnh (chỉ được hỗ trợ Bash định dạng)
 */
public class CurlParserTest {

    @Test(description = "Phân tích cú pháp rất đơn giảnGETyêu cầu")
    public void testParseSimpleGetRequest() {
        String curl = "curl https://api.example.com/users";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
        assertTrue(result.headersList == null || result.headersList.isEmpty());
        assertNull(result.body);
        assertTrue(result.paramsList == null || result.paramsList.isEmpty());
        assertFalse(result.followRedirects);
    }

    @Test(description = "Phân tích cú pháp với các tham số truy vấnGETyêu cầu")
    public void testParseGetRequestWithQueryParams() {
        String curl = "curl 'https://api.example.com/users?page=1&limit=10&sort=name'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users?page=1&limit=10&sort=name");
        assertEquals(result.method, "GET");
        assertEquals(findParamValue(result.paramsList, "page"), "1");
        assertEquals(findParamValue(result.paramsList, "limit"), "10");
        assertEquals(findParamValue(result.paramsList, "sort"), "name");
    }

    @Test(description = "phân tíchPOSTYêu cầu mang theoJSONdữ liệu")
    public void testParsePostRequestWithJsonData() {
        String curl = "curl -X POST https://api.example.com/users " +
                "-H 'Content-Type: application/json' " +
                "-d '{\"name\":\"John\",\"email\":\"john@example.com\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\",\"email\":\"john@example.com\"}");
    }

    @Test(description = "Phân tích yêu cầu với nhiều tiêu đề yêu cầu")
    public void testParseRequestWithMultipleHeaders() {
        String curl = "curl -X GET https://api.example.com/users " +
                "-H 'Authorization: Bearer token123' " +
                "-H 'Accept: application/json' " +
                "-H 'User-Agent: MyApp/1.0'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
        assertEquals(findHeaderValue(result.headersList, "Authorization"), "Bearer token123");
        assertEquals(findHeaderValue(result.headersList, "Accept"), "application/json");
        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "MyApp/1.0");
    }

    @Test(description = "Vùng phân tíchCookielời yêu cầu")
    public void testParseRequestWithCookie() {
        String curl = "curl https://api.example.com/users -b 'session=abc123; lang=en'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(findHeaderValue(result.headersList, "Cookie"), "session=abc123; lang=en");
    }

    @Test(description = "Phân tích yêu cầu bằng cờ chuyển hướng")
    public void testParseRequestWithRedirectFlag() {
        String curl = "curl -L https://api.example.com/users";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertTrue(result.followRedirects);

        // Kiểm tra định dạng dài
        curl = "curl --location https://api.example.com/users";
        result = CurlParser.parse(curl);
        assertTrue(result.followRedirects);
    }

    @Test(description = "Phân tích các yêu cầu dữ liệu biểu mẫu")
    public void testParseFormDataRequest() {
        String curl = "curl -X POST https://api.example.com/upload " +
                "-F 'name=John' " +
                "-F 'email=john@example.com' " +
                "-F 'file=@/path/to/file.txt'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/upload");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "multipart/form-data");
        assertEquals(findFormDataValue(result.formDataList, "name"), "John");
        assertEquals(findFormDataValue(result.formDataList, "email"), "john@example.com");
        assertEquals(findFormDataFileValue(result.formDataList, "file"), "/path/to/file.txt");
    }

    @Test(description = "Phân tích dữ liệu với các ký tự thoát")
    public void testParseDataWithEscapeCharacters() {
        String curl = "curl -X POST https://api.example.com/data " +
                "-d \"{\\\"message\\\":\\\"Hello\\nWorld\\t!\\\",\\\"path\\\":\\\"/usr/local\\\"}\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertTrue(result.body.contains("Hello\\nWorld\\t!"));
        assertFalse(result.body.contains("Hello\nWorld\t!"));
        assertTrue(result.body.contains("/usr/local"));
    }

    @Test(description = "Phân tích nhiều dòngcURLlệnh(Bashđịnh dạng)")
    public void testParseMultiLineCurlCommand() {
        String curl = """
                curl -X POST \\
                  https://api.example.com/users \\
                  -H 'Content-Type: application/json' \\
                  -d '{"name":"John"}'
                """;
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\"}");
    }

    @Test(description = "lô cURL Văn bản nên được chia nhỏ và phân tích từng phần")
    public void testParseMultipleCurlCommands() {
        String curls = """
                curl 'https://api.example.com/orders/123' \\
                  -H 'Accept: application/json' \\
                  -H 'Cookie: session=abc; lang=zh-CN' \\
                  --insecure ;
                curl 'https://api.example.com/orders/search' \\
                  -H 'Content-Type: application/json' \\
                  --data-raw '{"orderNo":"","pageNum":1,"pageSize":20}' \\
                  --insecure ;
                curl 'https://api.example.com/orders/stats?date=&status=' \\
                  -H 'Accept: application/json' \\
                  --insecure
                """;

        java.util.List<CurlRequest> results = CurlParser.parseMany(curls);

        assertEquals(results.size(), 3);
        assertEquals(results.get(0).url, "https://api.example.com/orders/123");
        assertEquals(results.get(0).method, "GET");
        assertEquals(findHeaderValue(results.get(0).headersList, "Cookie"), "session=abc; lang=zh-CN");
        assertNull(results.get(0).body);

        assertEquals(results.get(1).url, "https://api.example.com/orders/search");
        assertEquals(results.get(1).method, "POST");
        assertEquals(findHeaderValue(results.get(1).headersList, "Content-Type"), "application/json");
        assertEquals(results.get(1).body, "{\"orderNo\":\"\",\"pageNum\":1,\"pageSize\":20}");

        assertEquals(results.get(2).url, "https://api.example.com/orders/stats?date=&status=");
        assertEquals(results.get(2).method, "GET");
    }

    @Test(description = "Một mục phân tích cú pháp gặp phải các đợt cURL Khi nói đến văn bản, chỉ có văn bản đầu tiên được phân tích cú pháp")
    public void testParseSingleCommandFromMultipleCurlText() {
        String curls = """
                curl 'https://api.example.com/first' -H 'X-First: true' ;
                curl 'https://api.example.com/second' -H 'X-Second: true'
                """;

        CurlRequest result = CurlParser.parse(curls);

        assertEquals(result.url, "https://api.example.com/first");
        assertEquals(findHeaderValue(result.headersList, "X-First"), "true");
        assertNull(findHeaderValue(result.headersList, "X-Second"));
    }

    @Test(description = "lô cURL Việc phân tách không thể bị xáo trộn bởi dấu chấm phẩy trong dấu ngoặc kép")
    public void testSplitMultipleCurlCommandsIgnoresQuotedSemicolon() {
        String curls = """
                curl 'https://api.example.com/a' -H 'Cookie: a=1; b=2'
                curl 'https://api.example.com/b' --data-raw '{"note":"a;b"}'
                """;

        java.util.List<String> commands = CurlParser.splitCommands(curls);

        assertEquals(commands.size(), 2);
        assertTrue(commands.get(0).contains("Cookie: a=1; b=2"));
        assertTrue(commands.get(1).contains("{\"note\":\"a;b\"}"));
    }

    @Test(description = "Vùng phân tíchURLyêu cầu tham số")
    public void testParseRequestWithUrlParameter() {
        String curl = "curl --url https://api.example.com/users -X GET";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
    }

    @Test(description = "Các tùy chọn dài nên được hỗ trợ --opt=value hình thức")
    public void testParseLongOptionsWithInlineValue() {
        String curl = "curl --url=https://api.example.com/users " +
                "--request=POST " +
                "--header='Accept: application/json' " +
                "--data-raw='{\"ok\":true}'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Accept"), "application/json");
        assertEquals(result.body, "{\"ok\":true}");
    }

    @Test(description = "Các bảng siêu dữ liệu nên sử dụng các tham số không hỗ trợ các tùy chọn để tránh nhầm địa chỉ proxy với một yêu cầu URL")
    public void testOptionMetadataConsumesIgnoredOptionValues() {
        String curl = "curl https://api.example.com/users " +
                "--proxy http://proxy.example.com:8080 " +
                "--connect-timeout 10 " +
                "--resolve api.example.com:443:127.0.0.1";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
    }

    @Test(description = "Tổng hợp tùy chọn ngắn nên được nhấn curl Semantic expansion")
    public void testParseShortOptionCluster() {
        String curl = "curl -LkXPOST https://api.example.com/users";

        CurlRequest result = CurlParser.parse(curl);

        assertTrue(result.followRedirects);
        assertEquals(result.method, "POST");
        assertEquals(result.url, "https://api.example.com/users");
    }

    @Test(description = "Phân tích các định dạng tham số dữ liệu khác nhau")
    public void testParseDataParameterVariants() {
        // Bài kiểm tra -d
        String curl1 = "curl -d 'test data' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "POST");
        assertEquals(result1.body, "test data");

        // Bài kiểm tra --data
        String curl2 = "curl --data 'test data' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "POST");
        assertEquals(result2.body, "test data");

        // Bài kiểm tra --data-raw
        String curl3 = "curl --data-raw 'test data' https://api.example.com";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertEquals(result3.method, "POST");
        assertEquals(result3.body, "test data");

        // Bài kiểm tra --data-binary
        String curl4 = "curl --data-binary 'test data' https://api.example.com";
        CurlRequest result4 = CurlParser.parse(curl4);
        assertEquals(result4.method, "POST");
        assertEquals(result4.body, "test data");
    }

    @Test(description = "Phân tích các lệnh phức tạp bằng dấu ngoặc kép")
    public void testParseComplexQuotedCommand() {
        String curl = "curl -X POST 'https://api.example.com/users' " +
                "-H \"Content-Type: application/json\" " +
                "-H 'Authorization: Bearer token' " +
                "-d $'{\"data\": \"test\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(findHeaderValue(result.headersList, "Authorization"), "Bearer token");
        assertEquals(result.body, "{\"data\": \"test\"}");
    }

    @Test(description = "Kiểm tra suy luận phương pháp mặc định")
    public void testDefaultMethodInference() {
        // Các yêu cầu không có dữ liệu và phương thức sẽ mặc định là GET
        String curl1 = "curl https://api.example.com/users";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "GET");

        // Các yêu cầu có dữ liệu nhưng không có phương thức nào được chỉ định sẽ mặc định là POST
        String curl2 = "curl https://api.example.com/users -d 'data'";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "POST");

        // Các yêu cầu có dữ liệu biểu mẫu nhưng không có phương thức nào được chỉ định sẽ mặc định là POST
        String curl3 = "curl https://api.example.com/users -F 'name=test'";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertEquals(result3.method, "POST");
    }

    @Test(description = "Kiểm tra các giá trị null và các trường hợp cạnh")
    public void testNullAndEdgeCases() {
        // Chuỗi trống
        CurlRequest result1 = CurlParser.parse("");
        assertEquals(result1.method, "GET");
        assertNull(result1.url);

        // ChỉcurlĐặt hàng
        CurlRequest result2 = CurlParser.parse("curl");
        assertEquals(result2.method, "GET");
        assertNull(result2.url);

        // nullNhập
        CurlRequest result3 = CurlParser.parse(null);
        assertEquals(result3.method, "GET");
        assertNull(result3.url);
    }

    @Test(description = "Bài kiểm traPreparedRequestthay đổicURLChức năng")
    public void testToCurlMethod() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.headersList = new java.util.ArrayList<>();
        preparedRequest.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        preparedRequest.headersList.add(new HttpHeader(true, "Authorization", "Bearer token"));
        preparedRequest.body = "{\"name\":\"John\"}";

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.startsWith("curl"));
        assertTrue(curlCommand.contains("-X POST"));
        assertTrue(curlCommand.contains("'https://api.example.com/users'"));
        assertTrue(curlCommand.contains("-H 'Content-Type: application/json'"));
        assertTrue(curlCommand.contains("-H 'Authorization: Bearer token'"));
        assertTrue(curlCommand.contains("--data"));
        assertTrue(curlCommand.contains("'{\"name\":\"John\"}'"));
    }

    @Test(description = "Bài kiểm traPreparedRequestthay đổicURL - Dữ liệu biểu mẫu")
    public void testToCurlWithFormData() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/upload";
        preparedRequest.formDataList = new java.util.ArrayList<>();
        preparedRequest.formDataList.add(new HttpFormData(true, "name", HttpFormData.TYPE_TEXT, "John"));
        preparedRequest.formDataList.add(new HttpFormData(true, "email", HttpFormData.TYPE_TEXT, "john@example.com"));
        preparedRequest.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "/path/to/file.txt"));

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.contains("-F 'name=John'"));
        assertTrue(curlCommand.contains("-F 'email=john@example.com'"));
        assertTrue(curlCommand.contains("-F 'file=@/path/to/file.txt'"));
    }

    @Test(description = "Bài kiểm traPreparedRequestthay đổicURL - DigestChứng nhận")
    public void testToCurlWithDigestAuth() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/digest";
        preparedRequest.transportAuth = new TransportAuth(AuthType.DIGEST.getConstant(), "digest-user", "digest pass");

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.contains(" --digest"));
        assertTrue(curlCommand.contains(" --user 'digest-user:digest pass'"));
    }

    @Test(description = "Bài kiểm traDigestChứng nhậncURLNó có thể được nhập lại sau khi xuất khẩu")
    public void testDigestCurlRoundTripImport() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/digest";
        preparedRequest.transportAuth = new TransportAuth(AuthType.DIGEST.getConstant(), "digest-user", "digest pass");

        String curlCommand = CurlParser.toCurl(preparedRequest);
        HttpRequestItem item = CurlImportUtil.fromCurl(curlCommand);

        assertNotNull(item);
        assertEquals(item.getAuthType(), AuthType.DIGEST.getConstant());
        assertEquals(item.getAuthUsername(), "digest-user");
        assertEquals(item.getAuthPassword(), "digest pass");
    }

    @Test(description = "Bài kiểm traPreparedRequestthay đổicURL - GETyêu cầu")
    public void testToCurlGetRequest() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.headersList = new java.util.ArrayList<>();
        preparedRequest.headersList.add(new HttpHeader(true, "Accept", "application/json"));

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.startsWith("curl"));
        assertFalse(curlCommand.contains("-X GET")); // GETCác phương thức không nên được chỉ định rõ ràng
        assertTrue(curlCommand.contains("'https://api.example.com/users'"));
        assertTrue(curlCommand.contains("-H 'Accept: application/json'"));
    }

    @Test(description = "Bài kiểm traBash ShellThoát tham số")
    public void testBashShellArgumentEscaping() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.body = "data with 'single quotes' and \"double quotes\"";

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertFalse(curlCommand.contains("--data \""));
        assertTrue(curlCommand.contains("'\\''single quotes'\\''"));

        CurlRequest parsed = CurlParser.parse(curlCommand);
        assertEquals(parsed.body, preparedRequest.body);
    }

    @Test(description = "Xuất khẩu cURL thời gian URL và tiêu đề yêu cầu nên được sử dụng shell Báo giá an toàn")
    public void testToCurlShellQuotesUrlAndHeaders() {
        String userAgentClientHint = "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\"";
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://example.com/search?q=\"quoted\"&lang=zh-CN";
        preparedRequest.headersList = new java.util.ArrayList<>();
        preparedRequest.headersList.add(new HttpHeader(true, "sec-ch-ua", userAgentClientHint));

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.contains("'https://example.com/search?q=\"quoted\"&lang=zh-CN'"));
        assertTrue(curlCommand.contains("-H 'sec-ch-ua: " + userAgentClientHint + "'"));

        CurlRequest parsed = CurlParser.parse(curlCommand);
        assertEquals(parsed.url, preparedRequest.url);
        assertEquals(findHeaderValue(parsed.headersList, "sec-ch-ua"), userAgentClientHint);
    }

    @Test(description = "Xuất khẩu cURL chứa dấu ngoặc đơn body Không nên quay lại dấu ngoặc kép")
    public void testToCurlEscapesSingleQuotesWithoutDoubleQuoteFallback() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://example.com/api";
        preparedRequest.body = "prefix 'quoted' $(uname) $HOME `id` \\ tail";

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertFalse(curlCommand.contains("--data \""));
        assertTrue(curlCommand.contains("'\\''quoted'\\''"));

        CurlRequest parsed = CurlParser.parse(curlCommand);
        assertEquals(parsed.body, preparedRequest.body);
    }

    @Test(description = "Sao chép gửi thực tế cURL Ảnh chụp nhanh phải được yêu cầu bằng cách sử dụng lớp mạng")
    public void testToActualCurlUsesSentSnapshot() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/final";
        preparedRequest.headersList = new java.util.ArrayList<>();
        preparedRequest.headersList.add(new HttpHeader(true, "Authorization", "Bearer configured"));
        preparedRequest.body = "{\"configured\":true}";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Host", "api.example.com"));
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Authorization", "Bearer actual"));
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Cookie", "session=jar; theme=dark"));
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Content-Length", "17"));
        preparedRequest.sentRequestBody = "{\"actual\":true}";
        preparedRequest.sentRequestBodyReplayable = true;

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertTrue(curlCommand.contains("-H 'Authorization: Bearer actual'"));
        assertTrue(curlCommand.contains("-H 'Cookie: session=jar; theme=dark'"));
        assertTrue(curlCommand.contains("-H 'Content-Length: 17'"));
        assertTrue(curlCommand.contains("'{\"actual\":true}'"));
        assertFalse(curlCommand.contains("Bearer configured"));
        assertFalse(curlCommand.contains("{\"configured\":true}"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Các phiên bản giao thức lớp mạng nên được giữ nguyên")
    public void testToActualCurlIncludesNegotiatedProtocol() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/h2";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Host", "api.example.com"));
        preparedRequest.exchangeEventInfo = new HttpEventInfo();
        preparedRequest.exchangeEventInfo.setProtocol("h2");

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertTrue(curlCommand.contains(" --http2 "));
        assertFalse(curlCommand.contains("-X GET"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Lớp mạng nên được sử dụng đầu tiên URL và các phương pháp")
    public void testToActualCurlUsesSentUrlAndMethod() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/start";
        preparedRequest.sentMethod = "GET";
        preparedRequest.sentUrl = "https://api.example.com/target";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Host", "api.example.com"));

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertTrue(curlCommand.contains("'https://api.example.com/target'"));
        assertFalse(curlCommand.contains("https://api.example.com/start"));
        assertFalse(curlCommand.contains("-X POST"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Vào GET Ảnh chụp nhanh nội dung yêu cầu phải được giữ lại rõ ràng khi có sẵn GET phương pháp")
    public void testToActualCurlPreservesGetMethodWhenBodySnapshotExists() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/search";
        preparedRequest.sentMethod = "GET";
        preparedRequest.sentUrl = "https://api.example.com/search";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Host", "api.example.com"));
        preparedRequest.sentRequestBody = "{\"query\":\"hello\"}";
        preparedRequest.sentRequestBodyReplayable = true;

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertTrue(curlCommand.contains(" -X GET "));
        assertTrue(curlCommand.contains("--data-raw '{\"query\":\"hello\"}'"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Nội dung yêu cầu chẩn đoán bị cắt ngắn không được coi là thực tế payload")
    public void testToActualCurlOmitsTruncatedBodyPreview() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/upload";
        preparedRequest.sentMethod = "POST";
        preparedRequest.sentUrl = "https://api.example.com/upload";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        preparedRequest.sentRequestBody = "{\"partial\":true}\n\n[Truncated request body: 1048576 bytes, showing first 64KB]";
        preparedRequest.sentRequestBodyReplayable = false;

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertFalse(curlCommand.contains("--data-raw"));
        assertFalse(curlCommand.contains("Truncated request body"));
        assertTrue(curlCommand.startsWith("# Request body omitted:"));
        assertTrue(curlCommand.contains("-X POST"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Nội dung yêu cầu không được giữ lại khi nội dung yêu cầu không thể tái tạo bị bỏ qua framing cái đầu")
    public void testToActualCurlOmitsBodyFramingHeadersWhenBodyIsNotReplayable() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/upload";
        preparedRequest.sentMethod = "POST";
        preparedRequest.sentUrl = "https://api.example.com/upload";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Content-Type", "application/octet-stream"));
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Content-Length", "1048576"));
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Transfer-Encoding", "chunked"));
        preparedRequest.sentRequestBody = "[binary/octet-stream]";
        preparedRequest.sentRequestBodyReplayable = false;

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertFalse(curlCommand.contains("--data-raw"));
        assertFalse(curlCommand.contains("Content-Length"));
        assertFalse(curlCommand.contains("Transfer-Encoding"));
        assertTrue(curlCommand.contains("-H 'Content-Type: application/octet-stream'"));
        assertTrue(curlCommand.startsWith("# Request body omitted:"));
    }

    @Test(description = "Sao chép gửi thực tế cURL Không nên sử dụng multipart Văn bản giữ chỗ được coi là có thật payload")
    public void testToActualCurlOmitsMultipartPlaceholderBodyPreview() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/form";
        preparedRequest.sentMethod = "POST";
        preparedRequest.sentUrl = "https://api.example.com/form";
        preparedRequest.sentHeadersList = new java.util.ArrayList<>();
        preparedRequest.sentHeadersList.add(new HttpHeader(true, "Content-Type", "multipart/form-data; boundary=abc"));
        preparedRequest.sentRequestBody = "[multipart/form-data] (see form files)";
        preparedRequest.sentRequestBodyReplayable = false;

        String curlCommand = CurlParser.toActualCurl(preparedRequest);

        assertFalse(curlCommand.contains("--data-raw"));
        assertFalse(curlCommand.contains("[multipart/form-data]"));
        assertTrue(curlCommand.startsWith("# Request body omitted:"));
    }

    @DataProvider(name = "getRequestFormats")
    public Object[][] getRequestFormats() {
        return new Object[][]{
                {"curl https://example.com"},
                {"curl -X GET https://example.com"},
                {"curl --request GET https://example.com"},
                {"curl https://example.com -X GET"}
        };
    }

    @Test(description = "Kiểm tra các định dạng khác nhauGETyêu cầu", dataProvider = "getRequestFormats")
    public void testVariousGetRequestFormats(String curlCommand) {
        CurlRequest result = CurlParser.parse(curlCommand);
        assertEquals(result.url, "https://example.com");
        assertEquals(result.method, "GET");
    }

    @Test(description = "Kiểm tra logic phân tách tiêu đề yêu cầu")
    public void testHeaderSplitting() {
        String curl = "curl https://api.example.com -H 'Content-Type:application/json' " +
                "-H 'Authorization: Bearer token:with:colons'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(findHeaderValue(result.headersList, "Authorization"), "Bearer token:with:colons");
    }

    @Test(description = "Kiểm tra các trường hợp phân tích cú pháp tham số truy vấn")
    public void testQueryParamEdgeCases() {
        // Tham số không có giá trị
        String curl1 = "curl 'https://api.example.com?flag&key=value'";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(findParamValue(result1.paramsList, "flag"), "");
        assertEquals(findParamValue(result1.paramsList, "key"), "value");

        // Tham số rỗng
        String curl2 = "curl 'https://api.example.com?empty=&key=value'";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(findParamValue(result2.paramsList, "empty"), "");
        assertEquals(findParamValue(result2.paramsList, "key"), "value");
    }

    @Test(description = "Kiểm tra phân tích cú pháp trường biểu mẫu phức tạp")
    public void testComplexFormFieldParsing() {
        String curl = "curl -X POST https://api.example.com/upload " +
                "-F 'field_without_value' " +
                "-F 'normal_field=value' " +
                "-F 'file_field=@/path/file.txt'";
        CurlRequest result = CurlParser.parse(curl);

        // Các trường không có dấu bằng sẽ bị bỏ qua
        assertNull(findFormDataValue(result.formDataList, "field_without_value"));
        assertEquals(findFormDataValue(result.formDataList, "normal_field"), "value");
        assertEquals(findFormDataFileValue(result.formDataList, "file_field"), "/path/file.txt");
    }

    @Test(description = "Bài kiểm traURLvà phương pháp ở những vị trí khác nhau")
    public void testUrlAndMethodPositions() {
        // URLĐầu tiên, phương pháp này đến sau cùng
        String curl1 = "curl https://api.example.com -X POST";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.url, "https://api.example.com");
        assertEquals(result1.method, "POST");

        // Phương pháp đầu tiên,URLSau
        String curl2 = "curl -X POST https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.url, "https://api.example.com");
        assertEquals(result2.method, "POST");
    }

    @Test(description = "Bài kiểm traCookieCác định dạng khác nhau cho các tham số")
    public void testCookieParameterFormats() {
        // Hình thức ngắn
        String curl1 = "curl -b 'session=123' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(findHeaderValue(result1.headersList, "Cookie"), "session=123");

        // dạng dài
        String curl2 = "curl --cookie 'session=123' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(findHeaderValue(result2.headersList, "Cookie"), "session=123");
    }

    @Test(description = "Kiểm tra các định dạng khác nhau của tham số tiêu đề yêu cầu")
    public void testHeaderParameterFormats() {
        // Hình thức ngắn
        String curl1 = "curl -H 'Content-Type: application/json' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(findHeaderValue(result1.headersList, "Content-Type"), "application/json");

        // dạng dài
        String curl2 = "curl --header 'Content-Type: application/json' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(findHeaderValue(result2.headersList, "Content-Type"), "application/json");
    }

    @Test(description = "Bài kiểm tra -H Định dạng dấu chấm phẩy nhập tiêu đề rỗng")
    public void testHeaderSemicolonImportsEmptyHeaderValue() {
        String curl = "curl -H 'X-Empty;' https://api.example.com";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(findHeaderValue(result.headersList, "X-Empty"), "");
    }

    @Test(description = "Bài kiểm tra -A/--user-agent Nhập dưới dạng User-Agent cái đầu")
    public void testParseUserAgentOptionAsHeader() {
        String curl = "curl -A 'Agent/1.0' --user-agent 'Agent/2.0' https://api.example.com";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "Agent/2.0");
    }

    @Test(description = "Bài kiểm tra Windows CMD caret Đã trốn thoát URL và tiêu đề yêu cầu")
    public void testParseWindowsCmdCaretEscapedUrlAndHeader() {
        String curl = "curl \"https://example.com/api?first=1^&second=2\" " +
                "-H \"X-Token: a^&b\"";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://example.com/api?first=1&second=2");
        assertEquals(findParamValue(result.paramsList, "first"), "1");
        assertEquals(findParamValue(result.paramsList, "second"), "2");
        assertEquals(findHeaderValue(result.headersList, "X-Token"), "a&b");
    }

    @Test(description = "Bài kiểm traHTTPChuyển đổi trường hợp phương thức")
    public void testHttpMethodCaseConversion() {
        String curl = "curl -X post https://api.example.com";
        CurlRequest result = CurlParser.parse(curl);
        assertEquals(result.method, "POST"); // Nên chuyển thành chữ hoa
    }

    @Test(description = "Bài kiểm traBash $'...'Xử lý ký tự thoát định dạng")
    public void testBashDollarQuoteEscapeCharacterHandling() {
        // sử dụng $'...' định dạng để hỗ trợ các ký tự thoát (ANSI-C quoting）
        String curl = "curl -d $'line1\\nline2\\tindented\\\\backslash\\\"quote' https://api.example.com";
        CurlRequest result = CurlParser.parse(curl);

        String expectedBody = "line1\nline2\tindented\\backslash\"quote";
        assertEquals(result.body, expectedBody);
    }

    @Test(description = "Bài kiểm traPreparedRequestvìnulltình huống")
    public void testToCurlWithNullPreparedRequest() {
        // Kiểm tra khác nhaunullTình trạng
        PreparedRequest preparedRequest = new PreparedRequest();

        String curlCommand = CurlParser.toCurl(preparedRequest);
        assertEquals(curlCommand, "curl");

        // Phần kiểm tranull
        preparedRequest.url = "https://api.example.com";
        curlCommand = CurlParser.toCurl(preparedRequest);
        assertTrue(curlCommand.contains("'https://api.example.com'"));
    }

    @Test(description = "Kiểm tra các kịch bản tổng hợp - Hoàn thànhBash cURLĐặt hàng")
    public void testCompleteScenario() {
        String curl = "curl -L -X POST 'https://api.example.com/users?active=true' " +
                "-H 'Content-Type: application/json' " +
                "-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9' " +
                "-H 'User-Agent: EasyPostman/2.1.0' " +
                "-b 'session=abc123; csrf_token=xyz789' " +
                "-d '{\"name\":\"Người dùng thử nghiệm\",\"age\":25,\"active\":true}'";

        CurlRequest result = CurlParser.parse(curl);

        // Xác minh các thuộc tính cơ bản
        assertEquals(result.url, "https://api.example.com/users?active=true");
        assertEquals(result.method, "POST");
        assertTrue(result.followRedirects);

        // Xác minh tiêu đề yêu cầu
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(findHeaderValue(result.headersList, "Authorization"), "Bearer eyJhbGciOiJIUzI1NiJ9");
        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "EasyPostman/2.1.0");
        assertEquals(findHeaderValue(result.headersList, "Cookie"), "session=abc123; csrf_token=xyz789");

        // Xác minh nội dung yêu cầu
        assertEquals(result.body, "{\"name\":\"Người dùng thử nghiệm\",\"age\":25,\"active\":true}");

        // Xác thực các tham số truy vấn
        assertEquals(findParamValue(result.paramsList, "active"), "true");
    }

    @Test(description = "Kiểm tra tính nhất quán của chuyển đổi khứ hồi")
    public void testRoundTripConsistency() {
        // Tạo mộtPreparedRequest
        PreparedRequest original = new PreparedRequest();
        original.method = "PUT";
        original.url = "https://api.example.com/users/123";
        original.headersList = new java.util.ArrayList<>();
        original.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        original.headersList.add(new HttpHeader(true, "Accept", "application/json"));
        original.body = "{\"name\":\"Updated Name\"}";

        // Chuyển đổi sang cURLĐặt hàng
        String curlCommand = CurlParser.toCurl(original);

        // Sau đó phân tích lạiCurlRequest
        CurlRequest parsed = CurlParser.parse(curlCommand);

        // Xác minh tính nhất quán của chuyển đổi khứ hồi
        assertEquals(parsed.method, original.method);
        assertEquals(parsed.url, original.url);
        assertEquals(findHeaderValue(parsed.headersList, "Content-Type"), findHeaderValue(original.headersList, "Content-Type"));
        assertEquals(findHeaderValue(parsed.headersList, "Accept"), findHeaderValue(original.headersList, "Accept"));
        assertEquals(parsed.body, original.body);
    }

    @Test(description = "phân tíchWebSocketĐã kết nốicurlĐặt hàng")
    public void testParseWebSocketCurlCommand() {
        String curl = "curl 'wss://echo-websocket.hoppscotch.io/' " +
                "-H 'Upgrade: websocket' " +
                "-H 'Origin: https://hoppscotch.io' " +
                "-H 'Cache-Control: no-cache' " +
                "-H 'Accept-Language: zh-CN,zh;q=0.9' " +
                "-H 'Pragma: no-cache' " +
                "-H 'Connection: Upgrade' " +
                "-H 'Sec-WebSocket-Key: ISD3FRCDNdAPiU+0dTuSXg==' " +
                "-H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits'";

        CurlRequest result = CurlParser.parse(curl);

        // Xác minhURLvà thông tin cơ bản được phân tích cú pháp chính xác
        assertEquals(result.url, "wss://echo-websocket.hoppscotch.io/");
        assertEquals(result.method, "GET"); // WebSocketBắt tay được sử dụngGETphương pháp

        // Xác thực bị hạn chếWebSocketTiêu đề đã được lọc ra
        assertNull(findHeaderValue(result.headersList, "Upgrade"));
        assertNull(findHeaderValue(result.headersList, "Connection"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Key"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Version"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Extensions"));

        // Xác minh rằng các tiêu đề không bị hạn chế được bảo tồn đúng cách
        assertEquals(findHeaderValue(result.headersList, "Origin"), "https://hoppscotch.io");
        assertEquals(findHeaderValue(result.headersList, "Cache-Control"), "no-cache");
        assertEquals(findHeaderValue(result.headersList, "Accept-Language"), "zh-CN,zh;q=0.9");
        assertEquals(findHeaderValue(result.headersList, "Pragma"), "no-cache");
        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36");
    }

    @Test(description = "Bài kiểm traWebSocketLọc tiêu đề hạn chế cho các yêu cầu")
    public void testWebSocketRestrictedHeaderFiltering() {
        String curl = "curl 'wss://echo-websocket.hoppscotch.io/' " +
                "-H 'Upgrade: websocket' " +
                "-H 'Origin: https://hoppscotch.io' " +
                "-H 'Connection: Upgrade' " +
                "-H 'Sec-WebSocket-Key: ISD3FRCDNdAPiU+0dTuSXg==' " +
                "-H 'User-Agent: Mozilla/5.0' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Sec-WebSocket-Extensions: permessage-deflate' " +
                "-H 'Accept-Language: zh-CN,zh;q=0.9'";

        CurlRequest result = CurlParser.parse(curl);

        // Xác minhURLvà thông tin cơ bản được phân tích cú pháp chính xác
        assertEquals(result.url, "wss://echo-websocket.hoppscotch.io/");
        assertEquals(result.method, "GET");

        // Xác thực bị hạn chếWebSocketTiêu đề đã được lọc ra
        assertNull(findHeaderValue(result.headersList, "Upgrade"));
        assertNull(findHeaderValue(result.headersList, "Connection"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Key"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Version"));
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Extensions"));

        // Xác minh rằng các tiêu đề không bị hạn chế được bảo tồn đúng cách
        assertEquals(findHeaderValue(result.headersList, "Origin"), "https://hoppscotch.io");
        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "Mozilla/5.0");
        assertEquals(findHeaderValue(result.headersList, "Accept-Language"), "zh-CN,zh;q=0.9");
    }

    @Test(description = "Bài kiểm tra ở mức trung bìnhHTTPRequest not acceptedWebSocketHiệu ứng lọc tiêu đề")
    public void testHttpRequestWithWebSocketHeaders() {
        String curl = "curl 'https://api.example.com/test' " +
                "-H 'Sec-WebSocket-Key: test-key' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Connection: keep-alive' " +
                "-H 'User-Agent: TestAgent'";

        CurlRequest result = CurlParser.parse(curl);

        // Việc xác minh là bình thườngHTTPYêu cầu quyền đặt các tiêu đề này
        assertEquals(result.url, "https://api.example.com/test");
        assertEquals(result.method, "GET");
        assertEquals(findHeaderValue(result.headersList, "Sec-WebSocket-Key"), "test-key");
        assertEquals(findHeaderValue(result.headersList, "Sec-WebSocket-Version"), "13");
        assertEquals(findHeaderValue(result.headersList, "Connection"), "keep-alive");
        assertEquals(findHeaderValue(result.headersList, "User-Agent"), "TestAgent");
    }

    @Test(description = "Phân tích phức tạpBashNhiều dòngcurlVí dụ")
    public void testParseComplexBashCurlExample() {
        var curl = """
                curl 'http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList' \\
                  -H 'Accept: */*' \\
                  -H 'Accept-Language: zh-CN,zh;q=0.9' \\
                  -H 'Authorization: bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9' \\
                  -H 'Cache-Control: no-cache' \\
                  -H 'Connection: keep-alive' \\
                  -H 'Content-Type: application/json' \\
                  -H 'Origin: http://127.0.0.1:8801' \\
                  -H 'Pragma: no-cache' \\
                  -H 'Referer: http://127.0.0.1:8801/doc.html' \\
                  -H 'Request-Origion: Knife4j' \\
                  -H 'Sec-Fetch-Dest: empty' \\
                  -H 'Sec-Fetch-Mode: cors' \\
                  -H 'Sec-Fetch-Site: same-origin' \\
                  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36' \\
                  -H 'knife4j-gateway-code: ROOT' \\
                  -H 'sec-ch-ua: "Google Chrome";v="141", "Not?A_Brand";v="8", "Chromium";v="141"' \\
                  -H 'sec-ch-ua-mobile: ?0' \\
                  -H 'sec-ch-ua-platform: "Windows"' \\
                  --data-raw $'{\\n  "id": "",\\n  "memberId": "",\\n  "pageNum": 1,\\n  "pageSize": 10\\n}'
                """;

        var req = CurlParser.parse(curl);

        assertNotNull(req);
        assertEquals(req.url, "http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList");
        assertEquals(req.method, "POST");
        assertEquals(findHeaderValue(req.headersList, "Content-Type"), "application/json");
        assertNotNull(findHeaderValue(req.headersList, "Authorization"));
        assertEquals(findHeaderValue(req.headersList, "Request-Origion"), "Knife4j");

        assertNotNull(req.body);
        assertTrue(req.body.contains("\"pageNum\": 1"));
        assertTrue(req.body.contains("\"pageSize\": 10"));
        assertTrue(req.body.contains("\"id\": \"\""));
        assertTrue(req.body.contains("\"memberId\": \"\""));
    }

    @Test(description = "Bài kiểm traBash $'...'Định dạng sự thậtcURLĐặt hàng")
    public void testParseRealBashCurlWithDollarQuote() {
        String curl = "curl 'http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList' \\\n" +
                "  -H 'Accept: */*' \\\n" +
                "  -H 'Accept-Language: zh-CN,zh;q=0.9' \\\n" +
                "  -H 'Authorization: bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9' \\\n" +
                "  -H 'Cache-Control: no-cache' \\\n" +
                "  -H 'Connection: keep-alive' \\\n" +
                "  -H 'Content-Type: application/json' \\\n" +
                "  -H 'Origin: http://127.0.0.1:8801' \\\n" +
                "  -H 'Pragma: no-cache' \\\n" +
                "  -H 'Referer: http://127.0.0.1:8801/doc.html' \\\n" +
                "  -H 'Request-Origion: Knife4j' \\\n" +
                "  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' \\\n" +
                "  --data-raw $'{\\n  \"id\": \"\",\\n  \"memberId\": \"\",\\n  \"pageNum\": 1,\\n  \"pageSize\": 10\\n}'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList");
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Accept"), "*/*");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(findHeaderValue(result.headersList, "Authorization"), "bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");

        assertNotNull(result.body);
        assertTrue(result.body.contains("\n"), "Body should contain newline characters");
        assertTrue(result.body.contains("\"id\":"), "Body should contain id field");
        assertTrue(result.body.contains("\"memberId\":"), "Body should contain memberId field");
        assertTrue(result.body.contains("\"pageNum\": 1"), "Body should contain pageNum field");
        assertTrue(result.body.contains("\"pageSize\": 10"), "Body should contain pageSize field");
        assertFalse(result.body.contains("\\n"), "Body should not contain literal \\n");
    }

    @Test(description = "Kiểm tra thực tếBash curlĐặt hàng")
    public void testActualUserBashCurl() {
        String curl = "curl 'https://www.doubao.com/samantha/chat/completion?aid=497858' \\\n" +
                "  -H 'content-type: application/json' \\\n" +
                "  --data-raw '{\"messages\":[{\"content\":\"{\\\"text\\\":\\\"1\\\"}\",\"content_type\":2001}],\"conversation_id\":\"24642138855619074\"}'";
        CurlRequest result = CurlParser.parse(curl);
        assertEquals(result.body, "{\"messages\":[{\"content\":\"{\\\"text\\\":\\\"1\\\"}\",\"content_type\":2001}],\"conversation_id\":\"24642138855619074\"}");
    }

    @Test(description = "Nhập khẩu cURL Không nén dấu ngoặc kép trong dấu ngoặc kép raw body trống")
    public void testParsePreservesQuotedRawBodyWhitespace() {
        String body = "line  one\nline\tthree";
        String curl = "curl -X POST 'https://example.com/text' " +
                "-H 'Content-Type: text/plain' " +
                "--data-raw '" + body + "'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.body, body);
    }


    @Test(description = "Bài kiểm tra -G Tùy chọn:GET Request with data-urlencode")
    public void testForceGetWithDataUrlencode() {
        // Kiểm tra cái đầu tiên bạn cung cấp curl Đặt hàng
        String curl = "curl -G 'http://localhost:8086/query?db=mydb' " +
                "--data-urlencode 'q=SELECT * FROM my_measurement LIMIT 5'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "GET", "sử dụng -G Các tùy chọn nên là bắt buộc GET phương pháp");
        assertEquals(result.url, "http://localhost:8086/query?db=mydb");
        assertEquals(findParamValue(result.paramsList, "q"), "SELECT * FROM my_measurement LIMIT 5",
                "sử dụng -G giờ,data Các tham số nên được thêm vào các tham số truy vấn");
        assertNull(result.body, "sử dụng -G , không nên có nội dung yêu cầu");
    }

    @Test(description = "Bài kiểm tra -XPOST Và data-urlencode")
    public void testPostWithDataUrlencode() {
        // Kiểm tra cái thứ hai bạn cung cấp curl Đặt hàng
        String curl = "curl -XPOST 'http://localhost:8086/query?db=mydb' " +
                "--data-urlencode 'q=SELECT * FROM my_measurement LIMIT 5'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST", "sử dụng -XPOST nên sử dụng khi nào POST phương pháp");
        assertEquals(result.url, "http://localhost:8086/query?db=mydb");
        // Tham số truy vấn chỉ được chứa  URL các thông số trong
        assertEquals(findParamValue(result.paramsList, "db"), "mydb");
        assertNull(findParamValue(result.paramsList, "q"), "Không được sử dụng -G giờ,data Tham số không được có trong tham số truy vấn");
        // Nó nên được thiết lập tự động Content-Type
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded",
                "sử dụng data-urlencode nên được thiết lập tự động Content-Type");
        // data-urlencode Dữ liệu nên được phân tích thành urlencoded list ở giữa
        assertEquals(findUrlencodedValue(result.urlencodedList, "q"), "SELECT * FROM my_measurement LIMIT 5",
                "sử dụng data-urlencode , dữ liệu cần được phân tích cú pháp urlencoded list ở giữa");
        // body Nó nên như vậy null，Bởi vì dữ liệu ở đó urlencoded list ở giữa
        assertNull(result.body, "khi sử dụng urlencoded giờ,body Nó nên như vậy null");
    }

    @Test(description = "Bài kiểm tra data-urlencode trong một tham số duy nhất & Vâng value nội dung")
    public void testDataUrlencodeTreatsAmpersandAsValueContentForPost() {
        String curl = "curl -XPOST 'https://example.com/search' " +
                "--data-urlencode 'q=a&b' " +
                "--data-urlencode 'limit=10'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded");
        assertEquals(result.urlencodedList.size(), 2);
        assertEquals(findUrlencodedValue(result.urlencodedList, "q"), "a&b");
        assertEquals(findUrlencodedValue(result.urlencodedList, "limit"), "10");
        assertNull(findUrlencodedValue(result.urlencodedList, "b"));
        assertNull(result.body);
    }

    @Test(description = "Bài kiểm tra -G Thấp hơn phổ biến -d Nhấn & tháo dỡ,data-urlencode Đừng nhấn & tháo dỡ")
    public void testForceGetSplitsDataAmpersandButKeepsDataUrlencodeAmpersand() {
        String curl = "curl -G 'https://example.com/search' " +
                "-d 'page=1&size=20' " +
                "--data-urlencode 'q=a&b'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "GET");
        assertEquals(findParamValue(result.paramsList, "page"), "1");
        assertEquals(findParamValue(result.paramsList, "size"), "20");
        assertEquals(findParamValue(result.paramsList, "q"), "a&b");
        assertNull(findParamValue(result.paramsList, "b"));
        assertNull(result.body);
    }

    @Test(description = "Bài kiểm tra POST với nhiều data-urlencode các thông số")
    public void testPostWithMultipleDataUrlencode() {
        String curl = "curl -XPOST 'http://localhost:8086/query' " +
                "--data-urlencode 'db=mydb' " +
                "--data-urlencode 'q=SELECT * FROM my_measurement'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded");
        assertEquals(findUrlencodedValue(result.urlencodedList, "db"), "mydb",
                "Nhiều data-urlencode Tất cả các tham số nên được phân tích cú pháp urlencoded list ở giữa");
        assertEquals(findUrlencodedValue(result.urlencodedList, "q"), "SELECT * FROM my_measurement",
                "Nhiều data-urlencode Tất cả các tham số nên được phân tích cú pháp urlencoded list ở giữa");
        assertNull(result.body, "khi sử dụng urlencoded giờ,body Nó nên như vậy null");
    }

    @Test(description = "Bài kiểm tra -G Tùy chọn với nhiều data các thông số")
    public void testForceGetWithMultipleDataParams() {
        String curl = "curl -G 'http://localhost:8086/query' " +
                "--data-urlencode 'db=mydb' " +
                "--data-urlencode 'q=SELECT * FROM my_measurement'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "GET");
        assertEquals(findParamValue(result.paramsList, "db"), "mydb");
        assertEquals(findParamValue(result.paramsList, "q"), "SELECT * FROM my_measurement");
        assertNull(result.body);
    }

    @Test(description = "Bài kiểm tra --get Tùy chọn định dạng dài")
    public void testGetLongFormatOption() {
        String curl = "curl --get 'http://localhost:8086/query' " +
                "-d 'key=value' " +
                "-d 'param=data'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "GET");
        assertEquals(findParamValue(result.paramsList, "key"), "value");
        assertEquals(findParamValue(result.paramsList, "param"), "data");
        assertNull(result.body);
    }

    @Test(description = "Thử nghiệm thì không -G của sự bình thường POST yêu cầu")
    public void testPostWithoutForceGet() {
        String curl = "curl -X POST 'http://localhost:8086/query' " +
                "-d 'key=value' " +
                "-d 'param=data'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded",
                "sử dụng -d nên được thiết lập tự động Content-Type vì application/x-www-form-urlencoded");
        // Nhiều -d Các thông số cần được giải quyết urlencoded list ở giữa
        assertEquals(findUrlencodedValue(result.urlencodedList, "key"), "value");
        assertEquals(findUrlencodedValue(result.urlencodedList, "param"), "data");
        assertNull(result.body, "khi sử dụng urlencoded giờ,body Nó nên như vậy null");
        assertTrue(result.paramsList == null || result.paramsList.isEmpty());
    }

    @Test(description = "Kiểm tra không có dấu bằng data các thông số")
    public void testDataParamWithoutEquals() {
        String curl = "curl -X POST 'http://localhost:8086/query' " +
                "-d 'rawdata'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(result.body, "rawdata", "Không có dấu bằng data Các tham số nên được coi là thô body");
    }

    @Test(description = "Kiểm tra hỗn hợp JSON Content-Type và data các thông số")
    public void testJsonContentTypeWithDataParams() {
        String curl = "curl -X POST 'http://localhost:8086/api' " +
                "-H 'Content-Type: application/json' " +
                "-d '{\"key\":\"value\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/json");
        assertEquals(result.body, "{\"key\":\"value\"}", "JSON body Nó nên được để nguyên như vậy");
        assertTrue(result.urlencodedList == null || result.urlencodedList.isEmpty(), "JSON Định dạng không được phân tích cú pháp dưới dạng urlencoded");
    }

    @Test(description = "Bài kiểm tra -G với các tham số không có giá trị")
    public void testForceGetWithEmptyValueParam() {
        String curl = "curl -G 'http://localhost:8086/query' " +
                "-d 'flag'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "GET");
        assertEquals(findParamValue(result.paramsList, "flag"), "", "Các tham số không có giá trị phải được đặt thành chuỗi trống");
    }

    @Test(description = "Bài kiểm tra ws:// hiệp định WebSocket URL")
    public void testWebSocketWithWsProtocol() {
        String curl = "curl 'ws://localhost:8080/socket' " +
                "-H 'Sec-WebSocket-Key: test123' " +
                "-H 'Custom-Header: value'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "ws://localhost:8080/socket");
        assertNull(findHeaderValue(result.headersList, "Sec-WebSocket-Key"), "ws:// Các giao thức cũng nên được lọc WebSocket cái đầu");
        assertEquals(findHeaderValue(result.headersList, "Custom-Header"), "value", "Tiêu đề tùy chỉnh nên được giữ lại");
    }

    @Test(description = "Bài kiểm tra Host Đầu đang ở trong WebSocket được lọc")
    public void testWebSocketHostHeaderFiltering() {
        String curl = "curl 'wss://example.com/socket' " +
                "-H 'Host: example.com' " +
                "-H 'Custom-Header: value'";
        CurlRequest result = CurlParser.parse(curl);

        assertNull(findHeaderValue(result.headersList, "Host"), "Host Cái đầu phải ở đó WebSocket được lọc");
        assertEquals(findHeaderValue(result.headersList, "Custom-Header"), "value");
    }

    @Test(description = "WebSocket Không nên dựa vào việc lọc tiêu đề bị hạn chế URL thứ tự xuất hiện")
    public void testWebSocketRestrictedHeaderFilteringAfterUrlIsKnown() {
        String curl = "curl -H 'Upgrade: websocket' " +
                "-H 'Connection: Upgrade' " +
                "-H 'sec-websocket-key: abc123' " +
                "-H 'Custom-Header: value' " +
                "'wss://example.com/socket'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "wss://example.com/socket");
        assertNull(findHeaderValue(result.headersList, "Upgrade"));
        assertNull(findHeaderValue(result.headersList, "Connection"));
        assertNull(findHeaderValue(result.headersList, "sec-websocket-key"));
        assertEquals(findHeaderValue(result.headersList, "Custom-Header"), "value");
    }

    @Test(description = "Kiểm tra dấu ngoặc kép bao gồm dấu ngoặc đơn")
    public void testDoubleQuoteWithSingleQuote() {
        String curl = "curl -d \"data with 'single' quotes\" https://example.com";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.body, "data with 'single' quotes");
    }

    @Test(description = "Kiểm tra nhiều URL Tham số (lấy cái cuối cùng)")
    public void testMultipleUrlParameters() {
        String curl = "curl https://first.com --url https://second.com";
        CurlRequest result = CurlParser.parse(curl);

        // Cái cuối cùng nên được sử dụng URL
        assertEquals(result.url, "https://second.com");
    }

    @Test(description = "Bài kiểm tra PreparedRequest thay đổi cURL Xử lý các giá trị null")
    public void testToCurlWithEmptyBody() {
        PreparedRequest req = new PreparedRequest();
        req.method = "GET";
        req.url = "https://example.com";
        req.body = "";

        String curl = CurlParser.toCurl(req);

        assertFalse(curl.contains("--data"), "vô giá trị body Không nên tạo ra --data các thông số");
    }

    @Test(description = "Ký tự thoát thử nghiệm không hoạt động trong dấu ngoặc đơn")
    public void testEscapeInSingleQuote() {
        String curl = "curl -d 'test\\ndata' https://example.com";
        CurlRequest result = CurlParser.parse(curl);

        // Không nên xử lý các ký tự thoát trong dấu nháy đơn
        assertEquals(result.body, "test\\ndata");
    }

    @Test(description = "Dải thử nghiệm boundary của multipart/form-data")
    public void testMultipartFormDataWithBoundary() {
        String curl = "curl -X POST 'http://example.com/upload' " +
                "-H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundary' " +
                "-d '------WebKitFormBoundary\r\n" +
                "Content-Disposition: form-data; name=\"field1\"\r\n\r\n" +
                "value1\r\n" +
                "------WebKitFormBoundary--'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "multipart/form-data; boundary=----WebKitFormBoundary");
        // Bởi vì parseMultipartFormData sẽ được gọi
        assertNotNull(result.body);
    }

    @Test(description = "Bài kiểm tra -XPOST Dạng viết liên tục")
    public void testDashXMethodCombinedFormat() {
        String curl1 = "curl -XPOST 'http://example.com/api'";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "POST", "-XPOST nên được phân tích cú pháp chính xác POST");

        String curl2 = "curl -XGET 'http://example.com/api'";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "GET", "-XGET nên được phân tích cú pháp chính xác GET");

        String curl3 = "curl -XPUT 'http://example.com/api'";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertEquals(result3.method, "PUT", "-XPUT nên được phân tích cú pháp chính xác PUT");

        String curl4 = "curl -XDELETE 'http://example.com/api'";
        CurlRequest result4 = CurlParser.parse(curl4);
        assertEquals(result4.method, "DELETE", "-XDELETE nên được phân tích cú pháp chính xác DELETE");
    }

    @Test(description = "Kiểm tra nhiều URL Các tham số (cái cuối cùng nên được sử dụng)")
    public void testMultipleUrlsUsesLast() {
        String curl = "curl https://first.com https://second.com";
        CurlRequest result = CurlParser.parse(curl);

        // curl Hành vi thực tế là sử dụng cái cuối cùng URL
        assertEquals(result.url, "https://second.com", "Cái cuối cùng xuất hiện nên được sử dụng URL（tuân thủ curl hành vi thực tế)");
    }

    @Test(description = "Bài kiểm tra toCurl Hỗ trợ urlencoded lĩnh vực")
    public void testToCurlWithUrlencoded() {
        PreparedRequest req = new PreparedRequest();
        req.method = "POST";
        req.url = "https://example.com/api";
        req.urlencodedList = new java.util.ArrayList<>();
        req.urlencodedList.add(new com.laker.postman.request.model.HttpFormUrlencoded(true, "username", "admin"));
        req.urlencodedList.add(new com.laker.postman.request.model.HttpFormUrlencoded(true, "password", "secret123"));

        String curl = CurlParser.toCurl(req);

        assertTrue(curl.contains("--data-urlencode 'username=admin'"),
                "nên được bao gồm urlencoded các thông số");
        assertTrue(curl.contains("--data-urlencode 'password=secret123'"),
                "nên được bao gồm urlencoded các thông số");
    }

    @Test(description = "Kiểm tra các ký tự thoát \\a \\v \\0 Vào $'...' ở định dạng")
    public void testAdditionalEscapeCharacters() {
        String curl = "curl -d $'line1\\aline2\\vline3' https://example.com";
        CurlRequest result = CurlParser.parse(curl);

        assertTrue(result.body.contains("\u0007"), "nên được bao gồm alert nhân vật");
        assertTrue(result.body.contains("\u000B"), "nên được bao gồm vertical tab nhân vật");
    }

    @Test(description = "Kiểm tra chuyển đổi khứ hồi: .urlencoded lĩnh vực")
    public void testRoundTripWithUrlencoded() {
        PreparedRequest original = new PreparedRequest();
        original.method = "POST";
        original.url = "https://example.com/api";
        original.urlencodedList = new java.util.ArrayList<>();
        original.urlencodedList.add(new com.laker.postman.request.model.HttpFormUrlencoded(true, "key1", "value1"));
        original.urlencodedList.add(new com.laker.postman.request.model.HttpFormUrlencoded(true, "key2", "value2"));

        String curlCommand = CurlParser.toCurl(original);
        CurlRequest parsed = CurlParser.parse(curlCommand);

        assertEquals(parsed.method, original.method);
        assertEquals(parsed.url, original.url);
        assertEquals(findUrlencodedValue(parsed.urlencodedList, "key1"), "value1");
        assertEquals(findUrlencodedValue(parsed.urlencodedList, "key2"), "value2");
    }

    @Test(description = "Kiểm tra giá trị null và null điều kiện biên")
    public void testEdgeCasesWithNullValues() {
        // Bài kiểm tra không có tham số -H
        String curl1 = "curl https://example.com -H";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.url, "https://example.com");

        // Bài kiểm tra không có tham số -d
        String curl2 = "curl https://example.com -d";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.url, "https://example.com");
    }

    @Test(description = "Bài kiểm tra URL Xử lý các ký tự đặc biệt trong ")
    public void testUrlWithSpecialCharacters() {
        String curl = "curl 'https://example.com/api?name=John%20Doe&age=30&tags=a,b,c'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://example.com/api?name=John%20Doe&age=30&tags=a,b,c");
        assertEquals(findParamValue(result.paramsList, "name"), "John Doe");
        assertEquals(findParamValue(result.paramsList, "age"), "30");
        assertEquals(findParamValue(result.paramsList, "tags"), "a,b,c");
    }

    @Test(description = "Kiểm tra trường hợp hỗn hợp HTTP phương pháp")
    public void testMixedCaseHttpMethods() {
        String curl = "curl -X pOsT https://example.com";
        CurlRequest result = CurlParser.parse(curl);
        assertEquals(result.method, "POST", "HTTP Các phương thức nên được chuyển đổi thành chữ hoa");
    }

    @Test(description = "Bài kiểm tra followRedirects Các định dạng khác nhau của logo")
    public void testFollowRedirectsVariations() {
        // Hình thức ngắn
        String curl1 = "curl -L https://example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertTrue(result1.followRedirects);

        // dạng dài
        String curl2 = "curl --location https://example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertTrue(result2.followRedirects);

        // Không có cờ chuyển hướng
        String curl3 = "curl https://example.com";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertFalse(result3.followRedirects);
    }

    @Test(description = "Kiểm tra phân tích tham số truy vấn phức tạp")
    public void testComplexQueryParameterParsing() {
        String curl = "curl 'https://api.example.com/search?q=java+programming&sort=date&filter=&limit=10'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(findParamValue(result.paramsList, "q"), "java+programming");
        assertEquals(findParamValue(result.paramsList, "sort"), "date");
        assertEquals(findParamValue(result.paramsList, "filter"), "");
        assertEquals(findParamValue(result.paramsList, "limit"), "10");
    }

    @Test(description = "phân tích cURL URL in được sử dụng để đóng glob Dấu ngoặc vuông thoát")
    public void testParseUrlWithCurlEscapedGlobBrackets() {
        String curl = "curl 'https://www.google.com/httpservice/web/AimThreadsService/ListThreads?aep=48&reqpld=\\[null,null,1\\]&msc=gwsclient'";

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(
                result.url,
                "https://www.google.com/httpservice/web/AimThreadsService/ListThreads?aep=48&reqpld=[null,null,1]&msc=gwsclient"
        );
        assertEquals(findParamValue(result.paramsList, "reqpld"), "[null,null,1]");
    }

    @Test(description = "Đã thử nghiệm bằng cách sử dụng cả hai body và urlencoded（body ưu tiên)")
    public void testBodyAndUrlencodedConflict() {
        PreparedRequest req = new PreparedRequest();
        req.method = "POST";
        req.url = "https://example.com/api";
        req.body = "{\"raw\":\"json\"}";
        req.urlencodedList = new java.util.ArrayList<>();
        req.urlencodedList.add(new com.laker.postman.request.model.HttpFormUrlencoded(true, "key", "value"));

        String curl = CurlParser.toCurl(req);

        // body nên được ưu tiên
        assertTrue(curl.contains("--data"));
        assertTrue(curl.contains("{\"raw\":\"json\"}"));
    }

    // ==================== Các phương pháp trợ giúp ====================

    /**
     * Từ headersList Tìm  key của value
     */
    private String findHeaderValue(java.util.List<HttpHeader> list, String key) {
        if (list == null) return null;
        for (HttpHeader item : list) {
            if (item.getKey() != null && item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    /**
     * Từ paramsList Tìm  key của value
     */
    private String findParamValue(java.util.List<HttpParam> list, String key) {
        if (list == null) return null;
        for (HttpParam item : list) {
            if (item.getKey() != null && item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    /**
     * Từ formDataList Tìm  key của Text loại value
     */
    private String findFormDataValue(java.util.List<HttpFormData> list, String key) {
        if (list == null) return null;
        for (HttpFormData item : list) {
            if (item.getKey() != null && item.getKey().equals(key) && HttpFormData.TYPE_TEXT.equals(item.getType())) {
                return item.getValue();
            }
        }
        return null;
    }

    /**
     * Từ formDataList Tìm  key của File loại value
     */
    private String findFormDataFileValue(java.util.List<HttpFormData> list, String key) {
        if (list == null) return null;
        for (HttpFormData item : list) {
            if (item.getKey() != null && item.getKey().equals(key) && HttpFormData.TYPE_FILE.equals(item.getType())) {
                return item.getValue();
            }
        }
        return null;
    }

    /**
     * Từ urlencodedList Tìm  key của value
     */
    private String findUrlencodedValue(java.util.List<com.laker.postman.request.model.HttpFormUrlencoded> list, String key) {
        if (list == null) return null;
        for (com.laker.postman.request.model.HttpFormUrlencoded item : list) {
            if (item.getKey() != null && item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    @Test(description = "Bài kiểm tra --data-raw Hợp tác application/x-www-form-urlencoded Phân tích hoàn tất urlencoded chuỗi")
    public void testDataRawWithUrlencodedContentType() {
        String curl = """
                curl 'https://xxxxx/xxxx.json' \\
                  -H 'Content-Type: application/x-www-form-urlencoded' \\
                  --data-raw 'page_no=1&page_size=10&province_code=13&start_date=&end_date=&status=4&start_card_date=&end_card_date=&card_no=&loss_type=&base_line=20251201'
                """;

        CurlRequest result = CurlParser.parse(curl);

        assertNotNull(result, "nên phân tích thành công");
        assertEquals(result.method, "POST", "nên được tự động đặt thành  POST phương pháp");
        assertEquals(result.url, "https://xxxxx/xxxx.json");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded");

        // Xác minh urlencoded Các trường được phân tích cú pháp chính xác
        assertNotNull(result.urlencodedList, "Nên có urlencodedList");
        assertEquals(result.urlencodedList.size(), 11, "Nên có 11 cá nhân urlencoded lĩnh vực");

        // Xác thực các lĩnh vực khác nhau
        assertEquals(findUrlencodedValue(result.urlencodedList, "page_no"), "1");
        assertEquals(findUrlencodedValue(result.urlencodedList, "page_size"), "10");
        assertEquals(findUrlencodedValue(result.urlencodedList, "province_code"), "13");
        assertEquals(findUrlencodedValue(result.urlencodedList, "start_date"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "end_date"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "status"), "4");
        assertEquals(findUrlencodedValue(result.urlencodedList, "start_card_date"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "end_card_date"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "card_no"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "loss_type"), "");
        assertEquals(findUrlencodedValue(result.urlencodedList, "base_line"), "20251201");

        // Xác minh body Nó nên như vậy null（Bởi vì dữ liệu ở đó urlencodedList giữa)
        assertNull(result.body, "khi sử dụng urlencoded giờ,body Nó nên như vậy null");
    }

    @Test(description = "Bài kiểm tra --data-raw Hợp tác application/x-www-form-urlencoded Phân tích nhiều data các thông số")
    public void testMultipleDataRawWithUrlencodedContentType() {
        String curl = """
                curl 'https://api.example.com/submit' \\
                  -H 'Content-Type: application/x-www-form-urlencoded' \\
                  --data-raw 'name=John' \\
                  --data-raw 'email=john@example.com' \\
                  --data-raw 'age=30'
                """;

        CurlRequest result = CurlParser.parse(curl);

        assertNotNull(result);
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded");

        // Xác minh urlencoded Các trường được phân tích cú pháp chính xác
        assertNotNull(result.urlencodedList);
        assertEquals(result.urlencodedList.size(), 3);

        assertEquals(findUrlencodedValue(result.urlencodedList, "name"), "John");
        assertEquals(findUrlencodedValue(result.urlencodedList, "email"), "john@example.com");
        assertEquals(findUrlencodedValue(result.urlencodedList, "age"), "30");

        assertNull(result.body);
    }

    @Test(description = "Bài kiểm tra --data-raw Hợp tác application/x-www-form-urlencoded Phân tích các giá trị có chứa các ký tự đặc biệt")
    public void testDataRawWithUrlencodedSpecialCharacters() {
        String curl = """
                curl 'https://api.example.com/submit' \\
                  -H 'Content-Type: application/x-www-form-urlencoded' \\
                  --data-raw 'key1=value%20with%20spaces&key2=value%3Dwith%3Dequals&key3=normal'
                """;

        CurlRequest result = CurlParser.parse(curl);

        assertNotNull(result);
        assertEquals(result.method, "POST");
        assertEquals(findHeaderValue(result.headersList, "Content-Type"), "application/x-www-form-urlencoded");

        assertNotNull(result.urlencodedList);
        assertEquals(result.urlencodedList.size(), 3);

        assertEquals(findUrlencodedValue(result.urlencodedList, "key1"), "value with spaces");
        assertEquals(findUrlencodedValue(result.urlencodedList, "key2"), "value=with=equals");
        assertEquals(findUrlencodedValue(result.urlencodedList, "key3"), "normal");

        assertNull(result.body);
    }

    @Test(description = "Bài kiểm tra curl Tiếng Trung khi nhập khẩu query và urlencoded Các thông số được giải mã")
    public void testDecodeChineseParametersFromCurlImport() {
        String curl = """
                curl 'https://example.com/api?name=%E5%BC%A0%E4%B8%89&city=%E5%8C%97%E4%BA%AC' \\
                  -H 'Content-Type: application/x-www-form-urlencoded' \\
                  --data-raw 'keyword=%E4%B8%AD%E6%96%87%E5%85%B3%E9%94%AE%E8%AF%8D&note=%E6%B5%8B%E8%AF%95'
                """;

        CurlRequest result = CurlParser.parse(curl);

        assertEquals(findParamValue(result.paramsList, "name"), "Trương San");
        assertEquals(findParamValue(result.paramsList, "city"), "Bắc Kinh");
        assertEquals(findUrlencodedValue(result.urlencodedList, "keyword"), "Từ khóa tiếng Trung");
        assertEquals(findUrlencodedValue(result.urlencodedList, "note"), "Bài kiểm tra");
    }

    @Test(description = "Khi kiểm tra đối tượng yêu cầu nhập URL query Hiển thị dưới dạng văn bản có thể đọc được")
    public void testCurlImportUtilDecodesUrlQueryForDisplay() {
        String curl = "curl 'https://example.com/api?keyword=%E4%B8%AD%E6%96%87&name=%E5%BC%A0%E4%B8%89'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getUrl(), "https://example.com/api?keyword=Tiếng Trung Quốc&name=Trương San");
        assertEquals(item.getParamsList().size(), 2);
        assertEquals(item.getParamsList().get(0).getValue(), "Tiếng Trung Quốc");
        assertEquals(item.getParamsList().get(1).getValue(), "Trương San");
        assertEquals(item.getBody(), "");
    }

    @Test(description = "Nhập khẩu cURL kín đáo query value Lồng vào URL của reserved percent-encoding")
    public void testCurlImportUtilPreservesReservedEncodingInsideQueryValues() {
        String originalUrl = "https://www.google.com/async/folif?" +
                "async=_fmt:adl,_snbasecss:https%3A%2F%2Fwww.gstatic.com%2F_%2Fmss%2Fsearch-next%2F_%2Fss%2Fk%3Dsearch-next.aim%2Fam%3DgBA%2Fd%3D1,_xsrf:token%3A123" +
                "&keyword=%E4%B8%AD%E6%96%87";
        String curl = "curl '" + originalUrl + "'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertTrue(item.getUrl().contains("keyword=Tiếng Trung Quốc"), "Tiếng Trung bình thường query Màn hình vẫn có thể đọc được");
        assertTrue(item.getUrl().contains("https%3A%2F%2Fwww.gstatic.com%2F_"),
                "Lồng nhau URL của :// không nên như vậy display decode trở nên raw reserved nhân vật");
        assertTrue(item.getUrl().contains("%2Fk%3Dsearch-next.aim%2Fam%3DgBA%2Fd%3D1"),
                "Đường dẫn lồng nhau và dấu phân cách giá trị phải giữ nguyên percent-encoding");
        assertEquals(HttpUrlUtil.buildEncodedUrl(item.getUrl(), item.getParamsList()), originalUrl);
    }

    @Test(description = "Nhập khẩu raw body cURL nên được thiết lập khi nào bodyType")
    public void testCurlImportUtilSetsRawBodyType() {
        String curl = "curl -X POST 'https://example.com/text' --data-raw 'plain body'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getBody(), "plain body");
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_RAW);
    }

    @Test(description = "Nhập khẩu --data-binary @file cURL nên được thiết lập khi nào binary bodyType")
    public void testCurlImportUtilSetsBinaryBodyTypeForDataBinaryFile() {
        String curl = "curl -X PUT 'https://example.com/upload' --data-binary '@/tmp/upload.bin'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getBody(), "/tmp/upload.bin");
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_BINARY);
    }

    @Test(description = "Nhập được phân tích cú pháp form-data của multipart cURL thời gian bodyType không nên như vậy raw body che phủ")
    public void testCurlImportUtilKeepsFormDataBodyTypeWhenMultipartBodyIsParsed() {
        String curl = "curl -X POST 'http://example.com/upload' " +
                "-H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundary' " +
                "-d '------WebKitFormBoundary\r\n" +
                "Content-Disposition: form-data; name=\"field1\"\r\n\r\n" +
                "value1\r\n" +
                "------WebKitFormBoundary--'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_FORM_DATA);
        assertEquals(item.getFormDataList().size(), 1);
    }

    @Test(description = "Băng nhập khẩu -L của cURL Cài đặt chuyển hướng tiếp theo nên được giữ lại")
    public void testCurlImportUtilPreservesFollowRedirectsFlag() {
        HttpRequestItem item = CurlImportUtil.fromCurl("curl -L 'https://example.com/redirect'");

        assertNotNull(item);
        assertEquals(item.getFollowRedirects(), Boolean.TRUE);
    }

    @Test(description = "Bài kiểm tra curl Dành riêng khi nhập đối tượng yêu cầu JSON body Nội dung gốc")
    public void testCurlImportUtilPreservesJsonBodyFromCurl() {
        String curl = "curl -X POST 'https://example.com/api' " +
                "-H 'Content-Type: application/json' " +
                "--data-raw '{\"name\":\"Trương San\",\"roles\":[\"admin\",\"user\"]}'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getBody(), "{\"name\":\"Trương San\",\"roles\":[\"admin\",\"user\"]}");
    }

    @Test(description = "Bài kiểm tra ChatGPT SSE cURL Được giữ lại sau khi nhập khẩu cookie、SSE thỏa thuận và raw JSON body")
    public void testChatGptSseCurlImportPreservesSendShape() {
        String body = "{\"action\":\"next\",\"messages\":[{\"content\":{\"parts\":[\"1\"]}}]}";
        String curl = "curl 'https://chatgpt.com/backend-api/f/conversation' " +
                "-H 'Content-Type: application/json' " +
                "-H 'accept: text/event-stream' " +
                "-H 'Authorization: Bearer token' " +
                "-H 'OpenAI-Sentinel-Chat-Requirements-Token: challenge-token' " +
                "-b 'cf_clearance=clearance; __Secure-oai-is=ois; __cf_bm=bm' " +
                "--data-raw '" + body + "'";

        HttpRequestItem item = CurlImportUtil.fromCurl(curl);

        assertNotNull(item);
        assertEquals(item.getProtocol(), RequestItemProtocolEnum.HTTP);
        assertEquals(item.getMethod(), "POST");
        assertEquals(item.getBody(), body);
        assertEquals(findHeaderValue(item.getHeadersList(), "accept"), "text/event-stream");
        assertEquals(findHeaderValue(item.getHeadersList(), "Cookie"),
                "cf_clearance=clearance; __Secure-oai-is=ois; __cf_bm=bm");

        PreparedRequest preparedRequest = PreparedRequestFactory.buildWithoutInheritance(item);

        assertEquals(preparedRequest.body, body);
        assertEquals(findHeaderValue(preparedRequest.headersList, "Content-Type"), "application/json");
        assertEquals(findHeaderValue(preparedRequest.headersList, "Cookie"),
                "cf_clearance=clearance; __Secure-oai-is=ois; __cf_bm=bm");
    }
}
