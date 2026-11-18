package com.nvhuong.devworkbench.service;

import com.nvhuong.devworkbench.http.runtime.model.HttpEventInfo;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.HttpRouteAttempt;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.file.ResponseFiles;
import com.nvhuong.devworkbench.history.RequestHistoryItem;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.common.constants.ConfigPathConstants;
import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.ioc.PostConstruct;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Người quản lý kiên trì lịch sử
 */
@Slf4j
@Component
public class HistoryPersistenceService {
    private static final String HISTORY_FILE = ConfigPathConstants.REQUEST_HISTORY;

    // Giới hạn số lượng ký tự tối đa được lưu trong một nội dung phản hồi (10KB)
    private static final int MAX_BODY_SIZE = 10 * 1024;
    // Giới hạn số lượng ký tự tối đa được lưu trong một nội dung yêu cầu (10KB)
    private static final int MAX_REQUEST_BODY_SIZE = 10 * 1024;
    // Giới hạn kích thước tập tin (50MB)
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final List<RequestHistoryItem> historyItems = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        ensureHistoryDirExists();
        loadHistory();
    }

    private void ensureHistoryDirExists() {
        try {
            Path historyDir = Paths.get(SystemUtil.getDevWorkbenchPath());
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }
        } catch (IOException e) {
            log.error("Failed to create history directory: {}", e.getMessage());
        }
    }

    /**
     * Thêm lịch sử
     */
    public RequestHistoryItem addHistory(PreparedRequest request, HttpResponse response, long requestTime) {
        RequestHistoryItem item = new RequestHistoryItem(request, toHistoryResponse(response), requestTime);
        historyItems.add(0, item); // Thêm vào đầu

        // Giới hạn số lượng hồ sơ lịch sử
        int maxCount = SettingManager.getMaxHistoryCount();
        while (historyItems.size() > maxCount) {
            historyItems.remove(historyItems.size() - 1);
        }

        // Lưu không đồng bộ
        saveHistoryAsync();
        return item;
    }

    /**
     * Lịch sử chỉ cần hiển thị thông tin: nó không giữ byte gốc và phản hồi được nhận dạng dưới dạng tệp không giữ lại văn bản bị cắt xén, ngăn danh sách lịch sử chiếm bộ nhớ trong một thời gian dài.
     */
    static HttpResponse toHistoryResponse(HttpResponse response) {
        // Các tệp đính kèm văn bản không thể được khôi phục một cách dễ dàng từ văn bản chính (chẳng hạn như tệp bất hợp pháp UTF-8 của CSV）Đồng thời chỉ giữ lại thông tin tệp để tránh hiển thị lịch sử làm hỏng văn bản
        boolean fileResponse = ResponseFiles.isFileResponse(response)
                || (ResponseFiles.isTextAttachment(response) && !ResponseFiles.canRestoreBytesFromBody(response));
        if (response == null || (response.rawBodyBytes == null && !fileResponse)) {
            return response;
        }
        HttpResponse copy = new HttpResponse();
        copy.headers = response.headers;
        copy.body = fileResponse ? "" : response.body;
        copy.detectedFileExtension = response.detectedFileExtension;
        copy.fileAttachment = response.fileAttachment;
        copy.fileContentOmitted = fileResponse;
        copy.code = response.code;
        copy.threadName = response.threadName;
        copy.filePath = response.filePath;
        copy.fileName = response.fileName;
        copy.costMs = response.costMs;
        copy.endTime = response.endTime;
        copy.protocol = response.protocol;
        copy.idleConnectionCount = response.idleConnectionCount;
        copy.connectionCount = response.connectionCount;
        copy.httpEventInfo = response.httpEventInfo;
        copy.bodySize = response.bodySize;
        copy.headersSize = response.headersSize;
        copy.isSse = response.isSse;
        copy.isImage = response.isImage;
        return copy;
    }

    /**
     * Nhận tất cả lịch sử
     */
    public List<RequestHistoryItem> getHistory() {
        return new ArrayList<>(historyItems);
    }

    /**
     * Xóa lịch sử
     */
    public void clearHistory() {
        historyItems.clear();
        saveHistoryAsync();
    }

    /**
     * Xóa một bản ghi lịch sử duy nhất
     */
    public void removeHistory(RequestHistoryItem item) {
        if (item == null) {
            return;
        }
        if (historyItems.remove(item)) {
            saveHistoryAsync();
        }
    }

    /**
     * Đồng bộ hóa và lưu lịch sử
     */
    public void saveHistory() {
        try {
            // Chỉ có lịch sử giới hạn được lưu lại
            int maxCount = SettingManager.getMaxHistoryCount();
            JSONArray jsonArray = new JSONArray();

            int count = Math.min(historyItems.size(), maxCount);
            for (int i = 0; i < count; i++) {
                RequestHistoryItem item = historyItems.get(i);
                JSONObject jsonItem = convertToJson(item);
                jsonArray.add(jsonItem);
            }

            // Viết vào tập tin
            String jsonString = JSONUtil.toJsonStr(jsonArray);
            Files.writeString(Paths.get(HISTORY_FILE), jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to save history: {}", e.getMessage());
        }
    }

    /**
     * Lưu lịch sử không đồng bộ
     */
    private void saveHistoryAsync() {
        Thread saveThread = new Thread(this::saveHistory);
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * Tệp phản hồi tạm thời có thể không còn tồn tại sau khi khởi động lại hoặc dọn dẹp hệ thống và đường dẫn không hợp lệ sẽ không còn được khôi phục để tránh trỏ đến các tệp không tồn tại.
     */
    private static String existingFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        try {
            return Files.isRegularFile(Paths.get(filePath)) ? filePath : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void restoreFileInfo(HttpResponse response, JSONObject responseJson) {
        response.fileAttachment = responseJson.getBool("fileAttachment", false);
        if (responseJson.getBool("fileContentOmitted", false)) {
            ResponseFiles.markContentOmitted(response, responseJson.getStr("detectedFileExtension"), null);
            return;
        }
        // Tương thích với lịch sử cũ: Nội dung tệp được lưu dưới dạng văn bản và bị cắt xén, đồng thời chỉ hiển thị thông tin tệp sau khi xác định bằng tiêu đề tệp
        String legacyExtension = ResponseFiles.detectFromLegacyText(response.body);
        if (legacyExtension != null) {
            ResponseFiles.markContentOmitted(response, legacyExtension, null);
        }
    }

    /**
     * Cắt bớt những cái quá lớnbodynội dung
     */
    private String truncateBody(String body, int maxSize) {
        if (body == null) {
            return "";
        }
        if (body.length() <= maxSize) {
            return body;
        }
        // Cắt bớt và thêm thông tin gợi ý
        return body.substring(0, maxSize) + "\n\n... [Nội dung quá lớn và đã bị cắt bớt. Kích thước ban đầu: " + body.length() + " nhân vật] ...";
    }

    /**
     * Tải lịch sử - Sử dụng tính năng đọc trực tuyến để tránh tràn bộ nhớ
     */
    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            // Kiểm tra kích thước tệp và nếu vượt quá giới hạn, hãy xóa tệp và bắt đầu lại
            long fileSizeInBytes = file.length();

            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("History file is too large ({} bytes, max: {} bytes), deleting and starting fresh",
                        fileSizeInBytes, MAX_FILE_SIZE);
                deleteHistoryFile(file);
                return;
            }

            // Nếu tập tin trống, hãy quay lại trực tiếp
            if (fileSizeInBytes == 0) {
                return;
            }

            // Sử dụng tính năng đọc trực tuyến để tránh tải toàn bộ tệp vào bộ nhớ cùng một lúc
            String jsonString = loadFileContent(file);
            if (jsonString.trim().isEmpty()) {
                return;
            }

            JSONArray jsonArray = JSONUtil.parseArray(jsonString);
            historyItems.clear();

            // Giới hạn số lượng lịch sử được tải
            int maxCount = SettingManager.getMaxHistoryCount();
            int loadCount = Math.min(jsonArray.size(), maxCount);

            for (int i = 0; i < loadCount; i++) {
                try {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    RequestHistoryItem item = convertFromJson(jsonItem);
                    historyItems.add(item);
                } catch (Exception e) {
                    // Bỏ qua các mục lịch sử không thể phục hồi
                    log.warn("Failed to restore history item at index {}: {}", i, e.getMessage());
                }
            }

            log.info("Successfully loaded {} history items from file", historyItems.size());

        } catch (OutOfMemoryError e) {
            log.error("Out of memory while loading history file, deleting and starting fresh", e);
            // Hết bộ nhớ, hãy xóa tập tin và bắt đầu lại
            deleteHistoryFile(file);
            historyItems.clear();
        } catch (Exception e) {
            log.error("Failed to load history: {}", e.getMessage(), e);
            // Tải không thành công, các tập tin bị hỏng đã bị xóa
            deleteHistoryFile(file);
            historyItems.clear();
        }
    }

    /**
     * Sử dụng chế độ phát trực tuyến để đọc nội dung tệp, giúp tiết kiệm bộ nhớ
     */
    private String loadFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder((int) Math.min(file.length(), MAX_FILE_SIZE));
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192]; // 8KB buffer
            int charsRead;
            long totalCharsRead = 0;
            long maxChars = MAX_FILE_SIZE / 2; // Giới hạn số lượng ký tự được đọc để ngăn chặn các tệp quá khổ

            while ((charsRead = reader.read(buffer)) != -1) {
                totalCharsRead += charsRead;
                if (totalCharsRead > maxChars) {
                    log.warn("History file content too large, truncating at {} chars", totalCharsRead);
                    break;
                }
                content.append(buffer, 0, charsRead);
            }
        }
        return content.toString();
    }

    /**
     * Xóa các tập tin lịch sử
     */
    private void deleteHistoryFile(File file) {
        try {
            Files.delete(file.toPath());
            log.info("Deleted history file: {}", file.getPath());
        } catch (IOException e) {
            log.error("Failed to delete history file: {}", file.getPath(), e);
        }
    }

    /**
     * Sẽ RequestHistoryItem Chuyển đổi sang  JSON đối tượng
     */
    private JSONObject convertToJson(RequestHistoryItem item) {
        PreparedRequest request = item.getRequest();
        HttpResponse response = item.getResponse();
        JSONObject jsonItem = new JSONObject();

        // Thông tin cơ bản
        jsonItem.set("method", item.getMethod());
        jsonItem.set("url", item.getUrl());
        jsonItem.set("responseCode", item.getResponseCode());
        jsonItem.set("requestTime", item.getRequestTime()); // Thêm thời gian yêu cầu

        // Yêu cầu thông tin
        JSONObject requestJson = new JSONObject();
        requestJson.set("method", request.method);
        requestJson.set("url", request.url);
        // Request body - Bảo toàn nội dung đầy đủ và lịch sử hỗ trợ khôi phục chính xác khi mở lại dưới dạng yêu cầu có thể chỉnh sửa
        String requestBody = "";
        if (request.sentRequestBody != null && !request.sentRequestBody.isEmpty()) {
            requestBody = request.sentRequestBody;
        } else if (request.body != null) {
            requestBody = request.body;
        }
        requestBody = truncateBody(requestBody, MAX_REQUEST_BODY_SIZE);
        requestJson.set("body", requestBody);
        if (!Objects.equals(request.body, requestBody)) {
            requestJson.set("originalBody", truncateBody(request.body, MAX_REQUEST_BODY_SIZE));
        }
        requestJson.set("bodyType", request.bodyType);
        requestJson.set("id", request.id);
        requestJson.set("followRedirects", request.followRedirects);
        requestJson.set("isMultipart", request.isMultipart);
        requestJson.set("cookieJarEnabled", request.cookieJarEnabled);
        requestJson.set("sslVerificationEnabled", request.sslVerificationEnabled);
        requestJson.set("httpVersion", request.httpVersion);
        requestJson.set("requestTimeoutMs", request.requestTimeoutMs);
        requestJson.set("webSocketPingIntervalMs", request.webSocketPingIntervalMs);
        requestJson.set("collectBasicInfo", request.collectBasicInfo);
        requestJson.set("collectEventInfo", request.collectEventInfo);
        requestJson.set("enableNetworkLog", request.enableNetworkLog);
        requestJson.set("prescript", request.prescript);
        requestJson.set("postscript", request.postscript);

        // Tiêu đề yêu cầu - Lưu những cái đã gửi thực tế trướcsentHeadersList
        JSONObject requestHeaders = new JSONObject();
        if (request.sentHeadersList != null && !request.sentHeadersList.isEmpty()) {
            for (HttpHeader header : request.sentHeadersList) {
                if (header != null && header.getKey() != null && !header.getKey().isBlank()) {
                    requestHeaders.set(header.getKey(), header.getValue());
                }
            }
        }
        requestJson.set("headers", requestHeaders);
        requestJson.set("headersList", convertHeadersListToJson(request.headersList));
        requestJson.set("paramsList", convertParamsListToJson(request.paramsList));
        requestJson.set("formDataList", convertFormDataListToJson(request.formDataList));
        requestJson.set("urlencodedList", convertUrlencodedListToJson(request.urlencodedList));


        jsonItem.set("request", requestJson);

        // Thông tin phản hồi
        JSONObject responseJson = new JSONObject();
        responseJson.set("code", response.code);
        // Giới hạn kích thước cơ thể phản ứng
        boolean fileResponse = ResponseFiles.isFileResponse(response);
        String responseBody = response.body != null && !fileResponse ? response.body : "";
        responseBody = truncateBody(responseBody, MAX_BODY_SIZE);
        responseJson.set("body", responseBody);
        if (fileResponse) {
            responseJson.set("fileContentOmitted", true);
            responseJson.set("detectedFileExtension", response.detectedFileExtension);
        }
        responseJson.set("fileAttachment", response.fileAttachment);
        responseJson.set("costMs", response.costMs);
        responseJson.set("threadName", response.threadName);
        responseJson.set("filePath", response.filePath);
        responseJson.set("fileName", response.fileName);
        responseJson.set("protocol", response.protocol);
        responseJson.set("idleConnectionCount", response.idleConnectionCount);
        responseJson.set("connectionCount", response.connectionCount);
        responseJson.set("bodySize", response.bodySize);
        responseJson.set("headersSize", response.headersSize);
        responseJson.set("isSse", response.isSse);

        // tiêu đề phản hồi
        JSONObject responseHeaders = new JSONObject();
        if (response.headers != null) {
            for (java.util.Map.Entry<String, java.util.List<String>> entry : response.headers.entrySet()) {
                String key = entry.getKey();
                java.util.List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    responseHeaders.set(key, String.join(", ", values));
                }
            }
        }
        responseJson.set("headers", responseHeaders);

        // thông tin sự kiện
        if (response.httpEventInfo != null) {
            JSONObject eventInfo = new JSONObject();
            eventInfo.set("localAddress", response.httpEventInfo.getLocalAddress());
            eventInfo.set("remoteAddress", response.httpEventInfo.getRemoteAddress());
            eventInfo.set("queueStart", response.httpEventInfo.getQueueStart());
            eventInfo.set("callStart", response.httpEventInfo.getCallStart());
            eventInfo.set("dispatcherQueueStart", response.httpEventInfo.getDispatcherQueueStart());
            eventInfo.set("dispatcherQueueEnd", response.httpEventInfo.getDispatcherQueueEnd());
            eventInfo.set("proxySelectStart", response.httpEventInfo.getProxySelectStart());
            eventInfo.set("proxySelectEnd", response.httpEventInfo.getProxySelectEnd());
            eventInfo.set("dnsStart", response.httpEventInfo.getDnsStart());
            eventInfo.set("dnsEnd", response.httpEventInfo.getDnsEnd());
            eventInfo.set("dnsHost", response.httpEventInfo.getDnsHost());
            eventInfo.set("dnsAddresses", response.httpEventInfo.getDnsAddresses());
            eventInfo.set("dnsError", response.httpEventInfo.getDnsError());
            eventInfo.set("connectStart", response.httpEventInfo.getConnectStart());
            eventInfo.set("secureConnectStart", response.httpEventInfo.getSecureConnectStart());
            eventInfo.set("secureConnectEnd", response.httpEventInfo.getSecureConnectEnd());
            eventInfo.set("connectEnd", response.httpEventInfo.getConnectEnd());
            eventInfo.set("connectionAcquired", response.httpEventInfo.getConnectionAcquired());
            eventInfo.set("requestHeadersStart", response.httpEventInfo.getRequestHeadersStart());
            eventInfo.set("requestHeadersEnd", response.httpEventInfo.getRequestHeadersEnd());
            eventInfo.set("requestBodyStart", response.httpEventInfo.getRequestBodyStart());
            eventInfo.set("requestBodyEnd", response.httpEventInfo.getRequestBodyEnd());
            eventInfo.set("responseHeadersStart", response.httpEventInfo.getResponseHeadersStart());
            eventInfo.set("responseHeadersEnd", response.httpEventInfo.getResponseHeadersEnd());
            eventInfo.set("responseBodyStart", response.httpEventInfo.getResponseBodyStart());
            eventInfo.set("responseBodyEnd", response.httpEventInfo.getResponseBodyEnd());
            eventInfo.set("connectionReleased", response.httpEventInfo.getConnectionReleased());
            eventInfo.set("callEnd", response.httpEventInfo.getCallEnd());
            eventInfo.set("callFailed", response.httpEventInfo.getCallFailed());
            eventInfo.set("canceled", response.httpEventInfo.getCanceled());
            eventInfo.set("queueingCost", response.httpEventInfo.getQueueingCost());
            eventInfo.set("stalledCost", response.httpEventInfo.getStalledCost());
            eventInfo.set("retryDecisionCount", response.httpEventInfo.getRetryDecisionCount());
            eventInfo.set("retryCount", response.httpEventInfo.getRetryCount());
            eventInfo.set("followUpDecisionCount", response.httpEventInfo.getFollowUpDecisionCount());
            eventInfo.set("followUpCount", response.httpEventInfo.getFollowUpCount());
            eventInfo.set("protocol", response.httpEventInfo.getProtocol());
            JSONArray routeAttempts = new JSONArray();
            for (HttpRouteAttempt attempt : response.httpEventInfo.getRouteAttempts()) {
                JSONObject routeAttempt = new JSONObject();
                routeAttempt.set("address", attempt.address());
                routeAttempt.set("addressFamily", attempt.addressFamily());
                routeAttempt.set("startTime", attempt.startTime());
                routeAttempt.set("endTime", attempt.endTime());
                routeAttempt.set("durationMs", attempt.durationMs());
                routeAttempt.set("connected", attempt.connected());
                routeAttempt.set("canceled", attempt.canceled());
                routeAttempt.set("protocol", attempt.protocol());
                routeAttempt.set("error", attempt.error());
                routeAttempts.add(routeAttempt);
            }
            eventInfo.set("routeAttempts", routeAttempts);
            eventInfo.set("tlsVersion", response.httpEventInfo.getTlsVersion());
            eventInfo.set("errorMessage", response.httpEventInfo.getErrorMessage());
            eventInfo.set("recoveredErrorMessage", response.httpEventInfo.getRecoveredErrorMessage());
            eventInfo.set("threadName", response.httpEventInfo.getThreadName());
            responseJson.set("httpEventInfo", eventInfo);
        }

        jsonItem.set("response", responseJson);

        return jsonItem;
    }

    /**
     * Từ JSON Đối tượng được chuyển đổi thành RequestHistoryItem
     */
    private RequestHistoryItem convertFromJson(JSONObject jsonItem) {
        // tái thiết PreparedRequest
        PreparedRequest request = new PreparedRequest();
        JSONObject requestJson = jsonItem.getJSONObject("request");
        request.method = requestJson.getStr("method");
        request.url = requestJson.getStr("url");
        request.body = requestJson.getStr("originalBody", requestJson.getStr("body"));
        request.bodyType = requestJson.getStr("bodyType");
        request.sentRequestBody = requestJson.getStr("body");
        request.id = requestJson.getStr("id");
        request.followRedirects = requestJson.getBool("followRedirects", true);
        request.isMultipart = requestJson.getBool("isMultipart", false);
        request.cookieJarEnabled = requestJson.getBool("cookieJarEnabled", true);
        request.sslVerificationEnabled = requestJson.getBool("sslVerificationEnabled", false);
        request.httpVersion = requestJson.getStr("httpVersion");
        request.requestTimeoutMs = requestJson.getInt("requestTimeoutMs", 0);
        if (requestJson.containsKey("webSocketPingIntervalMs")) {
            request.webSocketPingIntervalMs = requestJson.getInt("webSocketPingIntervalMs");
        }
        request.collectBasicInfo = requestJson.getBool("collectBasicInfo", true);
        request.collectEventInfo = requestJson.getBool("collectEventInfo", true);
        request.enableNetworkLog = requestJson.getBool("enableNetworkLog", false);
        request.prescript = requestJson.getStr("prescript");
        request.postscript = requestJson.getStr("postscript");
        request.headersList = convertHeadersListFromJson(requestJson.getJSONArray("headersList"));
        request.paramsList = convertParamsListFromJson(requestJson.getJSONArray("paramsList"));
        request.formDataList = convertFormDataListFromJson(requestJson.getJSONArray("formDataList"));
        request.urlencodedList = convertUrlencodedListFromJson(requestJson.getJSONArray("urlencodedList"));

        // Xây dựng lại tiêu đề yêu cầu - Xây dựng sentHeadersList vì HttpHtmlRenderer sử dụng
        JSONObject requestHeaders = requestJson.getJSONObject("headers");
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            List<HttpHeader> sentHeaders = new ArrayList<>();
            for (String key : requestHeaders.keySet()) {
                if (key != null && !key.isBlank()) {
                    sentHeaders.add(new HttpHeader(true, key, requestHeaders.getStr(key)));
                }
            }
            request.sentHeadersList = sentHeaders;
        } else if (request.headersList != null && !request.headersList.isEmpty()) {
            List<HttpHeader> sentHeaders = new ArrayList<>();
            for (HttpHeader header : request.headersList) {
                if (header == null || !header.isEnabled() || header.getKey() == null || header.getKey().isBlank()) {
                    continue;
                }
                sentHeaders.add(new HttpHeader(true, header.getKey(), header.getValue(), header.getDescription()));
            }
            request.sentHeadersList = sentHeaders;
        }

        // tái thiết HttpResponse
        HttpResponse response = new HttpResponse();
        JSONObject responseJson = jsonItem.getJSONObject("response");
        response.code = responseJson.getInt("code");
        response.body = responseJson.getStr("body");
        response.costMs = responseJson.getLong("costMs", 0L);
        response.threadName = responseJson.getStr("threadName");
        response.filePath = existingFilePath(responseJson.getStr("filePath"));
        response.fileName = responseJson.getStr("fileName");
        response.protocol = responseJson.getStr("protocol");
        response.idleConnectionCount = responseJson.getInt("idleConnectionCount", 0);
        response.connectionCount = responseJson.getInt("connectionCount", 0);
        response.bodySize = responseJson.getInt("bodySize", 0);
        response.headersSize = responseJson.getInt("headersSize", 0);
        response.isSse = responseJson.getBool("isSse", false);
        restoreFileInfo(response, responseJson);

        // Xây dựng lại tiêu đề phản hồi
        response.headers = new LinkedHashMap<>();
        JSONObject responseHeaders = responseJson.getJSONObject("headers");
        if (responseHeaders != null) {
            for (String key : responseHeaders.keySet()) {
                String value = responseHeaders.getStr(key);
                List<String> valueList = new ArrayList<>();
                // Nếu giá trị chứa dấu phẩy, hãy chia thành nhiều giá trị
                String[] values = value.split(", ");
                for (String v : values) {
                    valueList.add(v.trim());
                }
                response.headers.put(key, valueList);
            }
        }

        // Xây dựng lại thông tin sự kiện
        JSONObject eventInfoJson = responseJson.getJSONObject("httpEventInfo");
        if (eventInfoJson != null) {
            response.httpEventInfo = new HttpEventInfo();
            response.httpEventInfo.setLocalAddress(eventInfoJson.getStr("localAddress"));
            response.httpEventInfo.setRemoteAddress(eventInfoJson.getStr("remoteAddress"));
            response.httpEventInfo.setQueueStart(eventInfoJson.getLong("queueStart", 0L));
            response.httpEventInfo.setCallStart(eventInfoJson.getLong("callStart", 0L));
            response.httpEventInfo.setDispatcherQueueStart(eventInfoJson.getLong("dispatcherQueueStart", 0L));
            response.httpEventInfo.setDispatcherQueueEnd(eventInfoJson.getLong("dispatcherQueueEnd", 0L));
            response.httpEventInfo.setProxySelectStart(eventInfoJson.getLong("proxySelectStart", 0L));
            response.httpEventInfo.setProxySelectEnd(eventInfoJson.getLong("proxySelectEnd", 0L));
            response.httpEventInfo.setDnsStart(eventInfoJson.getLong("dnsStart", 0L));
            response.httpEventInfo.setDnsEnd(eventInfoJson.getLong("dnsEnd", 0L));
            response.httpEventInfo.setDnsHost(eventInfoJson.getStr("dnsHost"));
            response.httpEventInfo.replaceDnsAddresses(readStringList(eventInfoJson.getJSONArray("dnsAddresses")));
            response.httpEventInfo.setDnsError(eventInfoJson.getStr("dnsError"));
            response.httpEventInfo.setConnectStart(eventInfoJson.getLong("connectStart", 0L));
            response.httpEventInfo.setSecureConnectStart(eventInfoJson.getLong("secureConnectStart", 0L));
            response.httpEventInfo.setSecureConnectEnd(eventInfoJson.getLong("secureConnectEnd", 0L));
            response.httpEventInfo.setConnectEnd(eventInfoJson.getLong("connectEnd", 0L));
            response.httpEventInfo.setConnectionAcquired(eventInfoJson.getLong("connectionAcquired", 0L));
            response.httpEventInfo.setRequestHeadersStart(eventInfoJson.getLong("requestHeadersStart", 0L));
            response.httpEventInfo.setRequestHeadersEnd(eventInfoJson.getLong("requestHeadersEnd", 0L));
            response.httpEventInfo.setRequestBodyStart(eventInfoJson.getLong("requestBodyStart", 0L));
            response.httpEventInfo.setRequestBodyEnd(eventInfoJson.getLong("requestBodyEnd", 0L));
            response.httpEventInfo.setResponseHeadersStart(eventInfoJson.getLong("responseHeadersStart", 0L));
            response.httpEventInfo.setResponseHeadersEnd(eventInfoJson.getLong("responseHeadersEnd", 0L));
            response.httpEventInfo.setResponseBodyStart(eventInfoJson.getLong("responseBodyStart", 0L));
            response.httpEventInfo.setResponseBodyEnd(eventInfoJson.getLong("responseBodyEnd", 0L));
            response.httpEventInfo.setConnectionReleased(eventInfoJson.getLong("connectionReleased", 0L));
            response.httpEventInfo.setCallEnd(eventInfoJson.getLong("callEnd", 0L));
            response.httpEventInfo.setCallFailed(eventInfoJson.getLong("callFailed", 0L));
            response.httpEventInfo.setCanceled(eventInfoJson.getLong("canceled", 0L));
            response.httpEventInfo.setQueueingCost(eventInfoJson.getLong("queueingCost", 0L));
            response.httpEventInfo.setStalledCost(eventInfoJson.getLong("stalledCost", 0L));
            response.httpEventInfo.setRetryDecisionCount(eventInfoJson.getInt("retryDecisionCount", 0));
            response.httpEventInfo.setRetryCount(eventInfoJson.getInt("retryCount", 0));
            response.httpEventInfo.setFollowUpDecisionCount(eventInfoJson.getInt("followUpDecisionCount", 0));
            response.httpEventInfo.setFollowUpCount(eventInfoJson.getInt("followUpCount", 0));

            JSONArray routeAttempts = eventInfoJson.getJSONArray("routeAttempts");
            if (routeAttempts != null) {
                for (int i = 0; i < routeAttempts.size(); i++) {
                    JSONObject routeAttempt = routeAttempts.getJSONObject(i);
                    response.httpEventInfo.addRouteAttempt(new HttpRouteAttempt(
                            routeAttempt.getStr("address"),
                            routeAttempt.getStr("addressFamily"),
                            routeAttempt.getLong("startTime", 0L),
                            routeAttempt.getLong("endTime", 0L),
                            routeAttempt.getLong("durationMs",
                                    Math.max(0L, routeAttempt.getLong("endTime", 0L)
                                            - routeAttempt.getLong("startTime", 0L))),
                            routeAttempt.getBool("connected", false),
                            routeAttempt.getBool("canceled", false),
                            routeAttempt.getStr("protocol"),
                            routeAttempt.getStr("error")
                    ));
                }
            }

            String protocolStr = eventInfoJson.getStr("protocol");
            if (protocolStr != null && !protocolStr.isEmpty()) {
                response.httpEventInfo.setProtocol(protocolStr);
            }

            response.httpEventInfo.setTlsVersion(eventInfoJson.getStr("tlsVersion"));
            response.httpEventInfo.setErrorMessage(eventInfoJson.getStr("errorMessage"));
            response.httpEventInfo.setRecoveredErrorMessage(eventInfoJson.getStr("recoveredErrorMessage"));
            response.httpEventInfo.setThreadName(eventInfoJson.getStr("threadName"));
        }

        // Đọc thời gian yêu cầu
        long requestTime = jsonItem.getLong("requestTime", System.currentTimeMillis());

        return new RequestHistoryItem(request, response, requestTime);
    }

    private JSONArray convertHeadersListToJson(List<HttpHeader> headersList) {
        JSONArray jsonArray = new JSONArray();
        if (headersList == null) {
            return jsonArray;
        }
        for (HttpHeader header : headersList) {
            if (header == null) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("enabled", header.isEnabled());
            jsonObject.set("key", header.getKey());
            jsonObject.set("value", header.getValue());
            jsonObject.set("description", header.getDescription());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private List<HttpHeader> convertHeadersListFromJson(JSONArray jsonArray) {
        List<HttpHeader> headersList = new ArrayList<>();
        if (jsonArray == null) {
            return headersList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            headersList.add(new HttpHeader(
                    jsonObject.getBool("enabled", true),
                    jsonObject.getStr("key", ""),
                    jsonObject.getStr("value", ""),
                    jsonObject.getStr("description", "")
            ));
        }
        return headersList;
    }

    private JSONArray convertParamsListToJson(List<HttpParam> paramsList) {
        JSONArray jsonArray = new JSONArray();
        if (paramsList == null) {
            return jsonArray;
        }
        for (HttpParam param : paramsList) {
            if (param == null) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("enabled", param.isEnabled());
            jsonObject.set("key", param.getKey());
            jsonObject.set("value", param.getValue());
            jsonObject.set("description", param.getDescription());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private List<HttpParam> convertParamsListFromJson(JSONArray jsonArray) {
        List<HttpParam> paramsList = new ArrayList<>();
        if (jsonArray == null) {
            return paramsList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            paramsList.add(new HttpParam(
                    jsonObject.getBool("enabled", true),
                    jsonObject.getStr("key", ""),
                    jsonObject.getStr("value", ""),
                    jsonObject.getStr("description", "")
            ));
        }
        return paramsList;
    }

    private JSONArray convertFormDataListToJson(List<HttpFormData> formDataList) {
        JSONArray jsonArray = new JSONArray();
        if (formDataList == null) {
            return jsonArray;
        }
        for (HttpFormData formData : formDataList) {
            if (formData == null) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("enabled", formData.isEnabled());
            jsonObject.set("key", formData.getKey());
            jsonObject.set("type", formData.getType());
            jsonObject.set("value", formData.getValue());
            jsonObject.set("description", formData.getDescription());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private List<HttpFormData> convertFormDataListFromJson(JSONArray jsonArray) {
        List<HttpFormData> formDataList = new ArrayList<>();
        if (jsonArray == null) {
            return formDataList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            formDataList.add(new HttpFormData(
                    jsonObject.getBool("enabled", true),
                    jsonObject.getStr("key", ""),
                    jsonObject.getStr("type", HttpFormData.TYPE_TEXT),
                    jsonObject.getStr("value", ""),
                    jsonObject.getStr("description", "")
            ));
        }
        return formDataList;
    }

    private JSONArray convertUrlencodedListToJson(List<HttpFormUrlencoded> urlencodedList) {
        JSONArray jsonArray = new JSONArray();
        if (urlencodedList == null) {
            return jsonArray;
        }
        for (HttpFormUrlencoded urlencoded : urlencodedList) {
            if (urlencoded == null) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("enabled", urlencoded.isEnabled());
            jsonObject.set("key", urlencoded.getKey());
            jsonObject.set("value", urlencoded.getValue());
            jsonObject.set("description", urlencoded.getDescription());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private List<HttpFormUrlencoded> convertUrlencodedListFromJson(JSONArray jsonArray) {
        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
        if (jsonArray == null) {
            return urlencodedList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            urlencodedList.add(new HttpFormUrlencoded(
                    jsonObject.getBool("enabled", true),
                    jsonObject.getStr("key", ""),
                    jsonObject.getStr("value", ""),
                    jsonObject.getStr("description", "")
            ));
        }
        return urlencodedList;
    }

    private List<String> readStringList(JSONArray jsonArray) {
        List<String> values = new ArrayList<>();
        if (jsonArray == null) {
            return values;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            String value = jsonArray.getStr(i);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }
}
