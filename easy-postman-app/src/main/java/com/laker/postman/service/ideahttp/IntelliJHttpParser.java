package com.laker.postman.service.ideahttp;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.RequestAuthTypes;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpRequestItem;

import com.laker.postman.collection.importer.AuthParserUtil;
import com.laker.postman.collection.model.CollectionParseResult;
import com.laker.postman.http.request.HttpRequestProtocol;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_FORM_DATA;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_NONE;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_RAW;

/**
 * IntelliJ IDEA HTTP Client Trình phân tích tệp
 * Chịu trách nhiệm phân tích .http Định dạng tệp (IntelliJ IDEA HTTP Client / REST Client format), được chuyển đổi thành cấu trúc dữ liệu nội bộ
 * Lớp này chỉ chịu trách nhiệm phân tích cú pháp và không liên quan UI lớp TreeNode Lắp ráp
 */
@Slf4j
@UtilityClass
public class IntelliJHttpParser {
    // Dấu phân cách yêu cầu khớp: .### Ghi chú ở đầu
    private static final Pattern REQUEST_SEPARATOR_PATTERN = Pattern.compile("^###\\s*(.+)$");
    // cuộc thi đấu HTTP phương pháp + URL + Không bắt buộc HTTP phiên bản, hỗ trợ các phương pháp tùy chỉnh
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile(
            "^([A-Z][A-Z0-9_-]*)\\s+(.+?)(?:\\s+(HTTP/1\\.1|HTTP/2(?:\\s*\\(Prior Knowledge\\))?))?$"
    );
    // Tiêu đề trận đấu: .Key: Value
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.+)$");
    // Khớp với nhận xét một dòng: .# Bắt đầu
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^#\\s*(.*)$");
    // Chú thích tên yêu cầu khớp: .# @name RequestName
    private static final Pattern REQUEST_NAME_PATTERN = Pattern.compile("^#\\s*@name\\s*(?:=\\s*)?(.+)$");
    // So khớp định nghĩa biến: .@tên biến=giá trị
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^@([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.*)$");
    // Khớp với dòng nội tuyến ở cuối dòng yêu cầu response handler
    private static final Pattern INLINE_RESPONSE_HANDLER_PATTERN = Pattern.compile("^(.*?)\\s+>\\s*\\{%\\s*(.*?)\\s*%}\\s*$");

    /**
     * phân tích HTTP Tệp, trả về kết quả phân tích cú pháp chứa thông tin nhóm và danh sách yêu cầu
     *
     * @param content  HTTP Nội dung tập tin
     * @param filename Tên tệp (tùy chọn), được sử dụng để tạo tên nhóm thân thiện hơn với người dùng
     * @return CollectionParseResult，Trả về nếu phân tích cú pháp không thành công null
     */
    public static CollectionParseResult parseHttpFile(String content, String filename) {
        try {
            if (content == null || content.trim().isEmpty()) {
                log.error("HTTP file content is empty");
                return null;
            }

            // Tạo nhóm - Tên tệp được sử dụng đầu tiên, nếu không thì tên nhóm được quốc tế hóa và dấu thời gian được định dạng sẽ được sử dụng
            String groupName;
            if (filename != null && !filename.trim().isEmpty()) {
                // Xóa phần mở rộng tập tin
                groupName = filename.replaceAll("\\.http$", "").trim();
            } else {
                // Sử dụng tên nhóm được quốc tế hóa mặc định
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                groupName = I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_DEFAULT_GROUP, timestamp);
            }

            RequestGroup collectionGroup = new RequestGroup(groupName);

            // Chia theo hàng
            String[] lines = content.split("\n");

            // Phân tích các biến môi trường và yêu cầu
            ParseContext context = parseHttpContent(lines);

            List<HttpRequestItem> validRequests = new ArrayList<>();
            for (HttpRequestItem request : context.requests) {
                if (request != null && request.getUrl() != null && !request.getUrl().isEmpty()) {
                    validRequests.add(request);
                }
            }

            if (validRequests.isEmpty()) {
                log.warn("No valid requests found in HTTP file");
                return null;
            }

            // Nếu có các biến được phân tích cú pháp, hãy thêm chúng vào RequestGroup của variables trường (một biến ở cấp độ nhóm)
            if (!context.variables.isEmpty()) {
                for (Map.Entry<String, String> entry : context.variables.entrySet()) {
                    Variable variable = new Variable(true, entry.getKey(), entry.getValue());
                    collectionGroup.getVariables().add(variable);
                }
            }

            // Tạo kết quả phân tích cú pháp
            return CollectionParseResult.createFlat(collectionGroup, validRequests);

        } catch (Exception e) {
            log.error("Failed to parse HTTP file", e);
            return null;
        }
    }

    /**
     * Phân tích ngữ cảnh, bao gồm danh sách yêu cầu và ánh xạ biến
     */
    private static class ParseContext {
        List<HttpRequestItem> requests = new ArrayList<>();
        Map<String, String> variables = new LinkedHashMap<>();
    }

    /**
     * phân tích HTTP Nội dung tập tin, trích xuất các biến và yêu cầu
     */
    private static ParseContext parseHttpContent(String[] lines) {
        ParseContext context = new ParseContext();
        context.requests = parseHttpRequests(lines, context.variables);
        return context;
    }

    /**
     * phân tích HTTP Danh sách yêu cầu
     *
     * @param lines     Mảng các dòng tập tin
     * @param variables Được sử dụng để lưu trữ các định nghĩa biến được phân tích cú pháp
     * @return Danh sách yêu cầu
     */
    private static List<HttpRequestItem> parseHttpRequests(String[] lines, Map<String, String> variables) {
        List<HttpRequestItem> requests = new ArrayList<>();
        HttpRequestItem currentRequest = null;
        StringBuilder bodyBuilder = null;
        StringBuilder responseScriptBuilder = null;
        StringBuilder preRequestScriptBuilder = new StringBuilder();
        boolean inBody = false;
        boolean inResponseScript = false;
        boolean inPreRequestScript = false;
        String contentType = null;
        String boundary = null;
        String pendingRequestName = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // Bỏ qua các dòng trống
            if (trimmedLine.isEmpty()) {
                if (inBody && bodyBuilder != null) {
                    // Có thể có dòng trống body một phần của
                    bodyBuilder.append("\n");
                }
                continue;
            }

            if (inPreRequestScript) {
                int endIndex = trimmedLine.indexOf("%}");
                if (endIndex >= 0) {
                    String scriptLine = trimmedLine.substring(0, endIndex).trim();
                    if (!scriptLine.isEmpty()) {
                        appendScriptLine(preRequestScriptBuilder, convertPreRequestScript(scriptLine));
                    }
                    inPreRequestScript = false;
                    trimmedLine = trimmedLine.substring(endIndex + 2).trim();
                    if (trimmedLine.isEmpty()) {
                        continue;
                    }
                } else {
                    appendScriptLine(preRequestScriptBuilder, convertPreRequestScript(trimmedLine));
                    continue;
                }
            }

            // Kiểm tra xem đó có phải là định nghĩa biến (@ đầu)
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(trimmedLine);
            if (variableMatcher.matches()) {
                String varName = variableMatcher.group(1).trim();
                String varValue = variableMatcher.group(2).trim();
                variables.put(varName, varValue);
                continue;
            }

            // JetBrains Hỗ trợ chính thức # @name Chỉ định tên yêu cầu
            Matcher requestNameMatcher = REQUEST_NAME_PATTERN.matcher(trimmedLine);
            if (requestNameMatcher.matches()) {
                pendingRequestName = requestNameMatcher.group(1).trim();
                continue;
            }

            ScriptLineResult scriptLineResult = parseScriptLine(trimmedLine);
            if (!scriptLineResult.preScript.isEmpty()) {
                appendScriptLine(preRequestScriptBuilder, convertPreRequestScript(scriptLineResult.preScript));
            }
            if (!scriptLineResult.postScript.isEmpty()) {
                if (responseScriptBuilder == null) {
                    responseScriptBuilder = new StringBuilder();
                }
                appendScriptLine(responseScriptBuilder, convertResponseScriptToPostman(scriptLineResult.postScript));
            }
            if (scriptLineResult.startsMultilinePreScript) {
                inPreRequestScript = true;
            }
            trimmedLine = scriptLineResult.remainingLine;
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Bỏ qua các nhận xét một dòng (# Bắt đầu, nhưng không ###）
            if (COMMENT_PATTERN.matcher(trimmedLine).matches() && !trimmedLine.startsWith("###")) {
                continue;
            }

            // Kiểm tra tập lệnh xử lý phản hồi bắt đầu (> {% ... %} định dạng)
            if (trimmedLine.startsWith(">") && trimmedLine.contains("{%")) {
                // Nếu vào body Tập lệnh phản hồi gặp phải trong , mô tả body Đã kết thúc
                if (inBody) {
                    inBody = false;
                }
                // Khởi tạo tập lệnh phản hồi builder
                if (responseScriptBuilder == null) {
                    responseScriptBuilder = new StringBuilder();
                } else if (!responseScriptBuilder.isEmpty()) {
                    responseScriptBuilder.append("\n");
                }
                // Trích xuất nội dung tập lệnh (remove > {% và %}）
                String scriptLine = trimmedLine.substring(1).trim(); // Xóa >
                if (scriptLine.startsWith("{%") && scriptLine.endsWith("%}")) {
                    // Kịch bản một dòng
                    scriptLine = scriptLine.substring(2, scriptLine.length() - 2).trim();
                    responseScriptBuilder.append(scriptLine);
                } else if (scriptLine.startsWith("{%")) {
                    // Kịch bản nhiều dòng bắt đầu
                    scriptLine = scriptLine.substring(2).trim();
                    responseScriptBuilder.append(scriptLine);
                    inResponseScript = true;
                } else {
                    // Định dạng không xác định, bỏ qua
                }
                continue;
            }

            // Nếu ở chế độ tập lệnh phản hồi, hãy tiếp tục thu thập tập lệnh nhiều dòng
            if (inResponseScript) {
                if (trimmedLine.endsWith("%}")) {
                    // Kịch bản kết thúc
                    String scriptLine = trimmedLine;
                    if (scriptLine.endsWith("%}")) {
                        scriptLine = scriptLine.substring(0, scriptLine.length() - 2).trim();
                    }
                    if (!scriptLine.isEmpty()) {
                        responseScriptBuilder.append("\n").append(scriptLine);
                    }
                    inResponseScript = false;
                } else {
                    // Tiếp tục thu thập nội dung kịch bản
                    responseScriptBuilder.append("\n").append(trimmedLine);
                }
                continue;
            }

            // Kiểm tra xem đó có phải là dấu phân cách yêu cầu (### Lưu ý ở đầu)
            Matcher separatorMatcher = REQUEST_SEPARATOR_PATTERN.matcher(trimmedLine);
            if (separatorMatcher.matches()) {
                // Lưu yêu cầu trước đó
                if (currentRequest != null) {
                    finishRequest(currentRequest, bodyBuilder, contentType, boundary);
                    // Xử lý các kịch bản phản hồi
                    if (responseScriptBuilder != null && !responseScriptBuilder.isEmpty()) {
                        String convertedScript = convertResponseScriptToPostman(responseScriptBuilder.toString());
                        currentRequest.setPostscript(convertedScript);
                    }
                    requests.add(currentRequest);
                }
                // Bắt đầu một yêu cầu mới
                String requestName = separatorMatcher.group(1).trim();
                currentRequest = new HttpRequestItem();
                currentRequest.setId(UUID.randomUUID().toString());
                currentRequest.setName(requestName.isEmpty() ? I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_UNNAMED_REQUEST) : requestName);
                pendingRequestName = applyPendingRequestNameIfNeeded(currentRequest, pendingRequestName);
                bodyBuilder = new StringBuilder();
                responseScriptBuilder = new StringBuilder();
                inBody = false;
                contentType = null;
                boundary = null;
                continue;
            }

            // Kiểm tra xem có HTTP Hàng phương thức (tự động tạo một hàng nếu không có yêu cầu hiện tại)
            Matcher methodMatcher = HTTP_METHOD_PATTERN.matcher(trimmedLine);
            if (methodMatcher.matches()) {
                // Nếu chưa có yêu cầu hiện tại, hãy tạo một yêu cầu trước (không có hỗ trợ nào ### dấu phân cách)
                if (currentRequest == null) {
                    currentRequest = new HttpRequestItem();
                    currentRequest.setId(UUID.randomUUID().toString());
                    currentRequest.setName(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_UNNAMED_REQUEST));
                    if (bodyBuilder == null) {
                        bodyBuilder = new StringBuilder();
                    }
                    if (responseScriptBuilder == null) {
                        responseScriptBuilder = new StringBuilder();
                    }
                    inBody = false;
                    inResponseScript = false;
                    contentType = null;
                }
                String method = methodMatcher.group(1).toUpperCase();
                String url = methodMatcher.group(2).trim();
                String httpVersion = methodMatcher.group(3);
                pendingRequestName = applyPendingRequestNameIfNeeded(currentRequest, pendingRequestName);
                currentRequest.setMethod(method);
                currentRequest.setUrl(url);
                applyHttpVersion(currentRequest, httpVersion);
                if (!preRequestScriptBuilder.isEmpty()) {
                    currentRequest.setPrescript(preRequestScriptBuilder.toString());
                    preRequestScriptBuilder = new StringBuilder();
                }
                continue;
            }

            // Nếu không có yêu cầu hiện tại, hãy bỏ qua nó
            if (currentRequest == null) {
                continue;
            }

            // Kiểm tra xem có Content-Type Đầu (có thể ảnh hưởng body phân tích)
            // Chỉ khi không được nhập body Chỉ khi đó nó mới được xử lý như một tiêu đề yêu cầu
            if (trimmedLine.toLowerCase().startsWith("content-type:") && !inBody) {
                Matcher contentTypeMatcher = HEADER_PATTERN.matcher(trimmedLine);
                if (contentTypeMatcher.matches()) {
                    String originalContentType = contentTypeMatcher.group(2).trim();
                    contentType = originalContentType.toLowerCase();

                    // Kiểm tra xem có multipart/form-data và trích xuất boundary
                    // Lưu ý:boundary Nó phân biệt chữ hoa chữ thường nên được trích xuất từ giá trị ban đầu
                    if (contentType.contains("multipart/form-data")) {
                        // Được tìm thấy bằng cách sử dụng kết hợp không phân biệt chữ hoa chữ thường boundary các thông số
                        Pattern boundaryPattern = Pattern.compile("boundary=([^;\\s]+)", Pattern.CASE_INSENSITIVE);
                        Matcher boundaryMatcher = boundaryPattern.matcher(originalContentType);
                        if (boundaryMatcher.find()) {
                            boundary = boundaryMatcher.group(1).trim();
                            // Loại bỏ các trích dẫn có thể
                            if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                                boundary = boundary.substring(1, boundary.length() - 1);
                            }
                        }
                    }

                    if (currentRequest.getHeadersList() == null) {
                        currentRequest.setHeadersList(new ArrayList<>());
                    }
                    currentRequest.getHeadersList().add(new HttpHeader(true, "Content-Type", originalContentType));
                    continue;
                }
            }

            // Kiểm tra xem đó có phải là hàng tiêu đề không
            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmedLine);
            if (headerMatcher.matches() && !inBody) {
                String headerName = headerMatcher.group(1).trim();
                String headerValue = headerMatcher.group(2).trim();
                // xử lý Authorization cái đầu
                if ("Authorization".equalsIgnoreCase(headerName)) {
                    parseAuthorization(currentRequest, headerValue);
                } else {
                    // Thêm vào danh sách tiêu đề
                    if (currentRequest.getHeadersList() == null) {
                        currentRequest.setHeadersList(new ArrayList<>());
                    }
                    currentRequest.getHeadersList().add(new HttpHeader(true, headerName, headerValue));
                }
                continue;
            }

            // Các tình huống khác được cho là như vậy body nội dung
            if (currentRequest.getMethod() != null &&
                    ("POST".equals(currentRequest.getMethod()) ||
                            "PUT".equals(currentRequest.getMethod()) ||
                            "PATCH".equals(currentRequest.getMethod()) ||
                            "DELETE".equals(currentRequest.getMethod()))) {
                inBody = true;
                if (bodyBuilder != null && !bodyBuilder.isEmpty()) {
                    bodyBuilder.append("\n");
                }
                if (bodyBuilder == null) {
                    bodyBuilder = new StringBuilder();
                }
                bodyBuilder.append(line);
            }
        }

        // Lưu yêu cầu cuối cùng
        if (currentRequest != null) {
            finishRequest(currentRequest, bodyBuilder, contentType, boundary);
            // Xử lý các kịch bản phản hồi
            if (responseScriptBuilder != null && !responseScriptBuilder.isEmpty()) {
                String convertedScript = convertResponseScriptToPostman(responseScriptBuilder.toString());
                currentRequest.setPostscript(convertedScript);
            }
            requests.add(currentRequest);
        }

        return requests;
    }

    private static void applyHttpVersion(HttpRequestItem request, String httpVersion) {
        if (httpVersion == null || httpVersion.isBlank()) {
            return;
        }
        if (httpVersion.startsWith("HTTP/1.1")) {
            request.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        } else if (httpVersion.startsWith("HTTP/2")) {
            request.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_2);
        }
    }

    private static void appendScriptLine(StringBuilder builder, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
        builder.append(line.trim());
    }

    private static String convertPreRequestScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            return "";
        }

        String converted = script.trim();
        converted = converted.replace("request.variables.set", "pm.variables.set");
        converted = converted.replace("request.variables.get", "pm.variables.get");
        converted = converted.replace("client.global.set", "pm.environment.set");
        converted = converted.replace("client.global.get", "pm.environment.get");
        return converted;
    }

    private static String applyPendingRequestNameIfNeeded(HttpRequestItem request, String pendingRequestName) {
        if (request == null || pendingRequestName == null || pendingRequestName.isEmpty()) {
            return pendingRequestName;
        }
        String currentName = request.getName();
        String unnamedRequest = I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_UNNAMED_REQUEST);
        if (currentName == null || currentName.isEmpty() || unnamedRequest.equals(currentName)) {
            request.setName(pendingRequestName);
        }
        return null;
    }

    private static ScriptLineResult parseScriptLine(String trimmedLine) {
        ScriptLineResult result = new ScriptLineResult();
        result.remainingLine = trimmedLine;

        if (trimmedLine.startsWith("<")) {
            String afterMarker = trimmedLine.substring(1).trim();
            if (afterMarker.startsWith("{%")) {
                int endIndex = afterMarker.indexOf("%}");
                if (endIndex >= 0) {
                    result.preScript = afterMarker.substring(2, endIndex).trim();
                    result.remainingLine = afterMarker.substring(endIndex + 2).trim();
                } else {
                    result.preScript = afterMarker.substring(2).trim();
                    result.remainingLine = "";
                    result.startsMultilinePreScript = true;
                }
            }
        }

        String lineToCheck = result.remainingLine;
        Matcher inlineHandlerMatcher = INLINE_RESPONSE_HANDLER_PATTERN.matcher(lineToCheck);
        if (inlineHandlerMatcher.matches()) {
            result.remainingLine = inlineHandlerMatcher.group(1).trim();
            result.postScript = inlineHandlerMatcher.group(2).trim();
            return result;
        }

        if (lineToCheck.startsWith(">")) {
            String afterMarker = lineToCheck.substring(1).trim();
            if (afterMarker.startsWith("{%")) {
                int endIndex = afterMarker.indexOf("%}");
                if (endIndex >= 0) {
                    result.postScript = afterMarker.substring(2, endIndex).trim();
                    result.remainingLine = afterMarker.substring(endIndex + 2).trim();
                }
            }
        }

        return result;
    }

    private static class ScriptLineResult {
        String preScript = "";
        String postScript = "";
        String remainingLine = "";
        boolean startsMultilinePreScript = false;
    }

    /**
     * Hoàn tất việc phân tích và thiết lập yêu cầu body và các thuộc tính khác
     */
    private static void finishRequest(HttpRequestItem request, StringBuilder bodyBuilder, String contentType, String boundary) {
        // Đặt loại giao thức
        if (HttpRequestProtocol.isWebSocketUrl(request.getUrl())) {
            request.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        } else {
            request.setProtocol(RequestItemProtocolEnum.HTTP);
        }

        // xử lý body
        if (bodyBuilder != null && !bodyBuilder.isEmpty()) {
            String body = bodyBuilder.toString().trim();
            if (!body.isEmpty()) {
                // theo Content-Type xử lý body
                if (contentType != null && contentType.contains("multipart/form-data")) {
                    // phân tích multipart/form-data định dạng
                    parseMultipartFormData(request, body, boundary);
                } else if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                    // phân tích urlencoded định dạng
                    parseUrlencodedBody(request, body);
                } else {
                    // Mặc định là  raw body
                    request.setBody(body);
                    request.setBodyType(BODY_TYPE_RAW);
                }
            } else {
                // body trống rỗng
                request.setBodyType(BODY_TYPE_NONE);
            }
        } else {
            // Không body
            request.setBodyType(BODY_TYPE_NONE);
        }

        // Nếu không có tên nào được đặt, nó sẽ được sử dụng URL
        String unnamedRequest = I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_UNNAMED_REQUEST);
        if (request.getName() == null || request.getName().isEmpty() || unnamedRequest.equals(request.getName())) {
            try {
                java.net.URI uri = new java.net.URI(request.getUrl());
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    String[] parts = path.split("/");
                    String name = parts[parts.length - 1];
                    if (name.isEmpty() && parts.length > 1) {
                        name = parts[parts.length - 2];
                    }
                    request.setName(name.isEmpty() ? request.getUrl() : name);
                } else {
                    request.setName(request.getUrl());
                }
            } catch (Exception e) {
                request.setName(request.getUrl());
            }
        }
    }

    /**
     * Sẽ IntelliJ HTTP Client Tập lệnh phản hồi được chuyển đổi thành  Postman kịch bản phong cách
     * Ví dụ:client.global.set("token", response.body.token) -> pm.environment.set("token", pm.response.json().token)
     */
    private static String convertResponseScriptToPostman(String httpClientScript) {
        if (httpClientScript == null || httpClientScript.trim().isEmpty()) {
            return "";
        }

        StringBuilder postmanScript = new StringBuilder();
        String[] lines = httpClientScript.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String convertedLine = line;

            // Chuyển thành client.global.set vì pm.environment.set
            if (line.contains("client.global.set")) {
                convertedLine = line.replace("client.global.set", "pm.environment.set");

                // Chuyển thành response.body vì pm.response.json()
                // Ví dụ:response.body.token -> pm.response.json().token
                convertedLine = convertedLine.replaceAll("response\\.body\\.(\\w+)", "pm.response.json().$1");

                // Nếu chỉ response.body（không có quyền truy cập thuộc tính), được chuyển đổi thành  pm.response.json()
                convertedLine = convertedLine.replace("response.body", "pm.response.json()");
            }

            // Chuyển thành client.global.get vì pm.environment.get
            if (line.contains("client.global.get")) {
                convertedLine = convertedLine.replace("client.global.get", "pm.environment.get");
            }

            // Chuyển thành client.test vì pm.test
            if (line.contains("client.test")) {
                convertedLine = convertedLine.replace("client.test", "pm.test");
            }

            // Chuyển thành response.status vì pm.response.code
            if (line.contains("response.status")) {
                convertedLine = convertedLine.replace("response.status", "pm.response.code");
            }

            // Chuyển thành response.headers vì pm.response.headers
            if (line.contains("response.headers")) {
                convertedLine = convertedLine.replace("response.headers", "pm.response.headers");
            }

            if (!postmanScript.isEmpty()) {
                postmanScript.append("\n");
            }
            postmanScript.append(convertedLine);
        }

        return postmanScript.toString();
    }

    /**
     * phân tích application/x-www-form-urlencoded định dạng body
     */
    private static void parseUrlencodedBody(HttpRequestItem request, String body) {
        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();

        // Xử lý các định dạng nhiều dòng (mỗi dòng có thể bắt đầu bằng  & kết thúc)
        // Đầu tiên hãy xóa tất cả các dòng mới và khoảng trắng thừa, sau đó nhấn  & sự phân đoạn
        String normalizedBody = body.replaceAll("\\s*&\\s*\\n\\s*", "&")  // xử lý & Ngắt dòng sau
                .replaceAll("\\n\\s*", "&")           // Xử lý các dòng mới (được coi là  & tách ra)
                .replaceAll("&+", "&");              // Loại bỏ các bản sao &

        String[] pairs = normalizedBody.split("&");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) {
                continue;
            }

            // xử lý key = value hoặc key=value định dạng
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex > 0) {
                String name = pair.substring(0, equalsIndex).trim();
                String value = pair.substring(equalsIndex + 1).trim();
                if (!name.isEmpty()) {
                    urlencodedList.add(new HttpFormUrlencoded(true, name, value));
                }
            } else if (!pair.contains("=")) {
                // Nếu không có dấu bằng thì nó được coi là riêng biệt key
                urlencodedList.add(new HttpFormUrlencoded(true, pair, ""));
            }
        }
        request.setUrlencodedList(urlencodedList);
        request.setBodyType(BODY_TYPE_FORM_URLENCODED);
    }

    /**
     * phân tích multipart/form-data định dạng body
     */
    private static void parseMultipartFormData(HttpRequestItem request, String body, String boundary) {
        if (boundary == null || boundary.isEmpty()) {
            log.warn("multipart/form-data thiếu boundary，Không thể phân tích cú pháp");
            // Đã hạ cấp xuống raw body
            request.setBody(body);
            request.setBodyType(BODY_TYPE_RAW);
            return;
        }

        List<HttpFormData> formDataList = new ArrayList<>();

        // Thêm vào -- tiền tố tới boundary（nếu bạn chưa có)
        String fullBoundary = boundary.startsWith("--") ? boundary : "--" + boundary;

        // Sử dụng biểu thức chính quy để phân chia và có thể có các ký tự dòng mới ở phía trước ranh giới khớp
        // Chế độ phân chia: ký tự dòng mới tùy chọn + boundary
        String splitPattern = "(?:\\r?\\n)?" + Pattern.quote(fullBoundary);
        String[] parts = body.split(splitPattern);


        for (String part : parts) {
            part = part.trim();
            // Bỏ qua trống part và dấu hiệu kết thúc
            if (part.isEmpty() || part.equals("--")) {
                continue;
            }

            try {
                // Sẽ part Chia theo hàng
                String[] lines = part.split("\\r?\\n");

                String fieldName = null;
                String fileName = null;
                StringBuilder contentBuilder = new StringBuilder();
                boolean foundDoubleNewline = false;

                // phân tích Content-Disposition cái đầu
                Pattern dispositionPattern = Pattern.compile(
                        "Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"(?:;\\s*filename=\"([^\"]+)\")?",
                        Pattern.CASE_INSENSITIVE
                );

                int lineIndex = 0;
                // Xử lý tất cả các tiêu đề trước (Content-Disposition, Content-TypeChờ đợi)
                for (; lineIndex < lines.length; lineIndex++) {
                    String line = lines[lineIndex];

                    // Kiểm tra xem đó có phải là dòng trống không (cho biết phần cuối của tiêu đề)
                    if (line.trim().isEmpty()) {
                        foundDoubleNewline = true;
                        lineIndex++; // Bỏ qua các dòng trống
                        break;
                    }

                    // phân tích Content-Disposition
                    Matcher dispositionMatcher = dispositionPattern.matcher(line);
                    if (dispositionMatcher.find()) {
                        fieldName = dispositionMatcher.group(1);
                        fileName = dispositionMatcher.group(2);
                    }

                    // Các tiêu đề khác (ví dụ: Content-Type）Chúng tôi bỏ qua nó bây giờ và chỉ chú ý Content-Disposition
                }

                // Thu thập nội dung (tất cả các dòng sau dòng trống)
                if (foundDoubleNewline && fieldName != null) {
                    for (; lineIndex < lines.length; lineIndex++) {
                        if (!contentBuilder.isEmpty()) {
                            contentBuilder.append("\n");
                        }
                        contentBuilder.append(lines[lineIndex]);
                    }

                    String value = contentBuilder.toString().trim();

                    // Kiểm tra xem đó có phải là tệp tham chiếu (< ./filename định dạng)
                    if (value.startsWith("<")) {
                        value = value.substring(1).trim();
                    }

                    // Xác định xem đó là một tập tin hay một trường văn bản
                    if (fileName != null && !fileName.isEmpty()) {
                        // Trường tệp
                        formDataList.add(new HttpFormData(true, fieldName, HttpFormData.TYPE_FILE, value.isEmpty() ? fileName : value));
                    } else {
                        // Trường văn bản
                        formDataList.add(new HttpFormData(true, fieldName, HttpFormData.TYPE_TEXT, value));
                    }
                }
            } catch (Exception e) {
                log.warn("phân tích multipart part thất bại: " + e.getMessage(), e);
            }
        }

        if (formDataList.isEmpty()) {
            log.warn("multipart/form-data Kết quả phân tích cú pháp trống, bị hạ cấp xuống raw body");
            request.setBody(body);
            request.setBodyType(BODY_TYPE_RAW);
        } else {
            request.setFormDataList(formDataList);
            request.setBodyType(BODY_TYPE_FORM_DATA);
        }
    }

    /**
     * phân tích Authorization cái đầu
     */
    private static void parseAuthorization(HttpRequestItem request, String authValue) {
        if (authValue.startsWith("Basic ")) {
            request.setAuthType(AUTH_TYPE_BASIC);
            AuthParserUtil.BasicAuthCredentials credentials = AuthParserUtil.parseBasicAuthHeader(authValue);
            if (credentials != null) {
                request.setAuthUsername(credentials.getUsername());
                request.setAuthPassword(credentials.getPassword());
            }
        } else if (authValue.startsWith("Bearer ")) {
            request.setAuthType(AUTH_TYPE_BEARER);
            request.setAuthToken(authValue.substring(7).trim());
        }
    }

}
