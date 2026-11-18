package com.nvhuong.devworkbench.http.runtime.okhttp;

import com.nvhuong.devworkbench.http.runtime.error.DownloadCancelledException;
import com.nvhuong.devworkbench.http.runtime.interaction.DownloadProgressSink;
import com.nvhuong.devworkbench.http.runtime.interaction.DownloadProgressSinkFactory;
import com.nvhuong.devworkbench.http.runtime.interaction.ResponseSizeLimitWarning;
import com.nvhuong.devworkbench.http.runtime.interaction.ResponseSizeLimitWarningSink;
import com.nvhuong.devworkbench.http.runtime.config.HttpRuntimeSettingsProvider;
import com.nvhuong.devworkbench.http.runtime.file.ResponseFileNames;
import com.nvhuong.devworkbench.http.runtime.file.ResponseFileTypeDetector;
import com.nvhuong.devworkbench.http.runtime.file.ResponseFiles;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.sse.SseEventReader;
import com.nvhuong.devworkbench.http.runtime.sse.SseResponseCallback;
import com.nvhuong.devworkbench.util.FileExtensionUtil;
import com.nvhuong.devworkbench.util.HttpHeaderConstants;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * OkHttp Lớp công cụ xử lý phản hồi
 * <p>
 * Chịu trách nhiệm xử lý HTTP Phản hồi, bao gồm: .
 * - Phân biệt giữa phản hồi văn bản và nhị phân
 * - Xử lý tải tập tin lớn
 * - Phân tích tên tập tin
 * - Phát hiện bộ ký tự
 * - Quản lý tập tin tạm thời
 * </p>
 */
@Slf4j
public class OkHttpResponseHandler {

    // Định nghĩa hằng số
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SSE_CONTENT_TYPE = "text/event-stream";

    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo
     */
    private static final String UNKNOWN_BINARY_EXTENSION = ".bin";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".gif", ".webp");

    private OkHttpResponseHandler() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Nhận cài đặt kích thước nội dung phản hồi tối đa (tính bằng byte)
     */
    private static int getMaxBodySize() {
        return HttpRuntimeSettingsProvider.get().getMaxBodySize();
    }

    /**
     * Nhận cài đặt kích thước tải xuống tối đa (tính bằng byte)
     */
    private static int getMaxDownloadSize() {
        return HttpRuntimeSettingsProvider.get().getMaxDownloadSize();
    }

    /**
     * xử lý HTTP Phản ứng, sẽ OkHttp Response Chuyển đổi sang nội bộ HttpResponse đối tượng
     *
     * @param okResponse OkHttp đối tượng phản hồi
     * @param response   Đối tượng phản hồi nội bộ (được sử dụng để điền dữ liệu)
     * @throws IOException Các ngoại lệ có thể được đưa ra khi đọc nội dung phản hồi
     */
    public static void handleResponse(Response okResponse, HttpResponse response, SseResponseCallback callback) throws IOException {
        handleResponse(okResponse, response, callback, PreparedRequest.ResponseBodyMode.FULL, 64 * 1024);
    }

    public static void handleResponse(Response okResponse,
                                      HttpResponse response,
                                      SseResponseCallback callback,
                                      PreparedRequest.ResponseBodyMode bodyMode,
                                      int previewLimitBytes) throws IOException {
        handleResponse(
                okResponse,
                response,
                callback,
                bodyMode,
                previewLimitBytes,
                DownloadProgressSinkFactory.noop(),
                ResponseSizeLimitWarningSink.noop()
        );
    }

    public static void handleResponse(Response okResponse,
                                      HttpResponse response,
                                      SseResponseCallback callback,
                                      PreparedRequest.ResponseBodyMode bodyMode,
                                      int previewLimitBytes,
                                      DownloadProgressSinkFactory downloadProgressSinkFactory,
                                      ResponseSizeLimitWarningSink responseSizeLimitWarningSink) throws IOException {
        handleResponse(okResponse, response, callback, bodyMode, previewLimitBytes,
                downloadProgressSinkFactory, responseSizeLimitWarningSink, null);
    }

    public static void handleResponse(Response okResponse,
                                      HttpResponse response,
                                      SseResponseCallback callback,
                                      PreparedRequest.ResponseBodyMode bodyMode,
                                      int previewLimitBytes,
                                      DownloadProgressSinkFactory downloadProgressSinkFactory,
                                      ResponseSizeLimitWarningSink responseSizeLimitWarningSink,
                                      Call call) throws IOException {
        try {
            readResponse(okResponse, response, callback, bodyMode, previewLimitBytes,
                    downloadProgressSinkFactory, responseSizeLimitWarningSink, call);
        } finally {
            if (okResponse.body() != null) {
                okResponse.close();
            }
        }
    }

    private static void readResponse(Response okResponse,
                                     HttpResponse response,
                                     SseResponseCallback callback,
                                     PreparedRequest.ResponseBodyMode bodyMode,
                                     int previewLimitBytes,
                                     DownloadProgressSinkFactory downloadProgressSinkFactory,
                                     ResponseSizeLimitWarningSink responseSizeLimitWarningSink,
                                     Call call) throws IOException {
        response.code = okResponse.code();
        response.headers = new LinkedHashMap<>();
        for (String name : okResponse.headers().names()) {
            // sử dụng headers(name) Nhận tất cả có cùng tên header giá trị (ví dụ: bội số Set-Cookie）
            List<String> values = okResponse.headers(name);
            if (!values.isEmpty()) {
                response.addHeader(name, values);
            }
        }
        response.headersSize = response.httpEventInfo != null ? response.httpEventInfo.getHeaderBytesReceived() : 0;
        response.threadName = Thread.currentThread().getName();
        response.protocol = okResponse.protocol().toString();
        String contentType = okResponse.header(CONTENT_TYPE_HEADER, "");
        int contentLengthHeader = parseContentLength(okResponse);

        if (isSSEContent(contentType)) {
            handleSseResponse(response);
            if (callback != null) {
                ResponseBody body = okResponse.body();
                callback.onOpen(response);
                boolean failed = false;
                if (body != null) {
                    var reader = new SseEventReader(body.source(), callback);
                    try {
                        while (reader.processNextEvent()) {
                            // Tiếp tục đọc cho đến hết dòng
                        }
                    } catch (RuntimeException ex) {
                        failed = true;
                        callback.onFailure(ex.getMessage(), response);
                    } catch (IOException ex) {
                        failed = true;
                        callback.onFailure(ex.getMessage(), response);
                    }
                }
                if (!failed) {
                    callback.onClosed(response);
                }
            }
        } else if (bodyMode != null && bodyMode != PreparedRequest.ResponseBodyMode.FULL) {
            handleLightweightResponse(okResponse, response, bodyMode, previewLimitBytes);
        } else if (FileExtensionUtil.isBinaryType(contentType)) {
            handleBinaryResponse(okResponse, response, downloadProgressSinkFactory, responseSizeLimitWarningSink, call);
        } else {
            handleTextResponse(okResponse, response, contentLengthHeader, downloadProgressSinkFactory,
                    responseSizeLimitWarningSink, call);
        }
    }

    private static void handleLightweightResponse(Response okResponse,
                                                  HttpResponse response,
                                                  PreparedRequest.ResponseBodyMode bodyMode,
                                                  int previewLimitBytes) throws IOException {
        ResponseBody body = okResponse.body();
        if (body == null) {
            response.body = "";
            response.bodySize = 0;
            response.filePath = null;
            return;
        }

        int maxDownloadSize = getMaxDownloadSize();
        if (maxDownloadSize > 0 && parseContentLength(okResponse) > maxDownloadSize) {
            body.close();
            markLightweightBodyTooLarge(response, maxDownloadSize);
            return;
        }

        int limit = Math.max(0, previewLimitBytes);
        boolean keepPreview = bodyMode == PreparedRequest.ResponseBodyMode.PREVIEW
                && limit > 0
                && !FileExtensionUtil.isBinaryType(okResponse.header(CONTENT_TYPE_HEADER, ""));
        ByteArrayOutputStream preview = keepPreview ? new ByteArrayOutputStream(Math.min(limit, 64 * 1024)) : null;
        byte[] buffer = new byte[64 * 1024];
        long totalBytes = 0;

        try (InputStream inputStream = body.byteStream()) {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                if (keepPreview && preview.size() < limit) {
                    int allowed = Math.min(len, limit - preview.size());
                    preview.write(buffer, 0, allowed);
                }
                totalBytes += len;
                if (maxDownloadSize > 0 && totalBytes > maxDownloadSize) {
                    markLightweightBodyTooLarge(response, maxDownloadSize);
                    return;
                }
            }
        } catch (IOException e) {
            if (isIncompleteResponseBodyError(e)) {
                log.error("Failed to read complete response body preview: {}", e.getMessage());
                response.body = I18nUtil.getMessage(MessageKeys.RESPONSE_INCOMPLETE, e.getMessage());
                response.bodyIncomplete = true;
                response.bodySize = totalBytes;
                response.filePath = null;
                return;
            }
            log.error("Error reading response body preview: {}", e.getMessage(), e);
            throw e;
        }

        response.bodySize = totalBytes;
        response.filePath = null;
        String extension = FileExtensionUtil.guessExtension(okResponse.header(CONTENT_TYPE_HEADER));
        response.fileName = FileExtensionUtil.generateSmartFileName(extension != null ? extension : ".txt");

        if (!keepPreview) {
            response.body = I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_SKIPPED_PERFORMANCE, totalBytes);
            return;
        }

        Charset charset = resolveCharset(body);
        response.body = new String(preview.toByteArray(), charset);
        // Keep the body a clean prefix: callers evaluate assertions/extractors on it before deciding how to display it.
        response.bodyPreviewTruncated = totalBytes > preview.size();
    }

    private static void markLightweightBodyTooLarge(HttpResponse response, int maxDownloadSize) {
        response.body = I18nUtil.getMessage(MessageKeys.TEXT_TOO_LARGE_BODY, maxDownloadSize / 1024 / 1024);
        response.bodySize = 0;
        response.filePath = null;
    }

    private static Charset resolveCharset(ResponseBody body) {
        MediaType mediaType = body.contentType();
        if (mediaType != null) {
            Charset detectedCharset = mediaType.charset();
            if (detectedCharset != null) {
                return detectedCharset;
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * xử lý SSE（Server-Sent Events）loại phản ứng
     * SSE là phản hồi trực tuyến và không đọc toàn bộ nội dung
     *
     * @param response Đối tượng phản hồi nội bộ
     */
    private static void handleSseResponse(HttpResponse response) {
        response.body = I18nUtil.getMessage(MessageKeys.SSE_STREAM_UNSUPPORTED);
        response.bodySize = 0;
        response.isSse = true;
        // Không đóng cửa ở đây, thống nhất ở đây handleResponse Đóng trong phương pháp
    }

    /**
     * phân tích Content-Length tiêu đề phản hồi
     *
     * @param contentLengthStr Content-Length Giá trị của tiêu đề phản hồi
     * @return Độ dài nội dung (byte), được trả về nếu không thể phân tích cú pháp -1
     */
    private static int parseContentLength(String contentLengthStr) {
        if (contentLengthStr != null) {
            try {
                return Integer.parseInt(contentLengthStr);
            } catch (NumberFormatException ignore) {
                // Bỏ qua lỗi phân tích cú pháp và quay lại-1Đại diện cho một chiều dài không xác định
            }
        }
        return -1;
    }

    /**
     * Đọc độ dài nội dung trong phản hồi: .
     * Đọc đầu tiên Content-Length，Nếu bằng CompressionDecompressNetworkInterceptor Hãy loại bỏ sau đó
     * Dự phòng cho DevWorkbench-Content-Length（Những gì được lưu trữ là kích thước ban đầu trước khi nén và có thể được sử dụng làm ước tính gần đúng)
     */
    private static int parseContentLength(Response okResponse) {
        String contentLength = okResponse.header(HttpHeaderConstants.CONTENT_LENGTH);
        if (contentLength == null) {
            contentLength = okResponse.header(HttpHeaderConstants.DEV_WORKBENCH_CONTENT_LENGTH);
        }
        return parseContentLength(contentLength);
    }

    /**
     * Xác định xem có phải như vậy không SSE（Server-Sent Events）loại phản ứng
     *
     * @param contentType Content-Type Giá trị của tiêu đề phản hồi
     * @return trong trường hợp SSE Gõ trả về true，Còn không thì quay lại false
     */
    private static boolean isSSEContent(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains(SSE_CONTENT_TYPE);
    }

    private static boolean isIncompleteResponseBodyError(IOException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof EOFException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("unexpected end of stream")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }


    /**
     * Lưu luồng đầu vào vào một tệp tạm thời, trả về đối tượng tệp và số byte được ghi
     * <p>
     * This method will display the download progress dialog box and allow the user to cancel the download.
     * Nếu quá trình tải xuống bị hủy hoặc xảy ra ngoại lệ, các tệp tạm thời sẽ tự động bị xóa.
     * </p>
     *
     * @param is                  luồng đầu vào
     * @param prefix              Tiền tố tên tệp tạm thời
     * @param suffix              Hậu tố tên tệp tạm thời (phần mở rộng)
     * @param contentLengthHeader Content-Length Giá trị của tiêu đề phản hồi (được sử dụng để hiển thị tiến trình)
     * @return FileAndSize Đối tượng chứa tệp tạm thời và số byte thực tế được ghi
     * @throws IOException Các ngoại lệ có thể được đưa ra khi đọc và ghi tệp
     */
    private static FileAndSize saveInputStreamToTempFile(InputStream is,
                                                         String prefix,
                                                         String suffix,
                                                         int contentLengthHeader,
                                                         DownloadProgressSinkFactory downloadProgressSinkFactory,
                                                         boolean streamingMedia,
                                                         String sourceUrl,
                                                         Call call) throws IOException {
        DownloadProgressSink progressSink = createDownloadProgressSink(downloadProgressSinkFactory);
        File tempFile = File.createTempFile(prefix, suffix);
        long totalBytes = 0;
        int maxDownloadSize = getMaxDownloadSize();
        byte[] buf = new byte[64 * 1024];
        boolean completed = false;
        try {
            progressSink.start(contentLengthHeader, streamingMedia, sourceUrl, call == null ? null : call::cancel);
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile), 64 * 1024)) {
                while (true) {
                    checkDownloadCancelled(progressSink, call);
                    int len = is.read(buf);
                    checkDownloadCancelled(progressSink, call);
                    if (len == -1) {
                        break;
                    }
                    long receivedBytes = totalBytes + len;
                    if (maxDownloadSize > 0 && receivedBytes > maxDownloadSize) {
                        throw new DownloadSizeLimitException(receivedBytes, maxDownloadSize);
                    }
                    bos.write(buf, 0, len);
                    totalBytes = receivedBytes;
                    progressSink.updateProgress(len);
                }
            }
            checkDownloadCancelled(progressSink, call);
            completed = true;
            return new FileAndSize(tempFile, totalBytes);
        } catch (IOException e) {
            // A cancelled socket read is a user action, not a network failure.
            if (e instanceof DownloadCancelledException || isDownloadCancelled(progressSink, call)) {
                throw new DownloadCancelledException(totalBytes);
            }
            throw e;
        } finally {
            if (!completed) {
                // The output stream is closed before deleting, including on Windows.
                deleteTempFile(tempFile);
            }
            progressSink.finish(completed, isDownloadCancelled(progressSink, call));
        }
    }

    private static void checkDownloadCancelled(DownloadProgressSink sink, Call call) throws DownloadCancelledException {
        if (isDownloadCancelled(sink, call)) {
            throw new DownloadCancelledException();
        }
    }

    private static boolean isDownloadCancelled(DownloadProgressSink sink, Call call) {
        return sink.isCancelled() || (call != null && call.isCanceled());
    }

    private static DownloadProgressSink createDownloadProgressSink(DownloadProgressSinkFactory factory) {
        DownloadProgressSinkFactory resolvedFactory = factory == null ? DownloadProgressSinkFactory.noop() : factory;
        DownloadProgressSink sink = resolvedFactory.create();
        return sink == null ? DownloadProgressSink.NOOP : sink;
    }

    /**
     * Xóa các tập tin tạm thời một cách an toàn
     *
     * @param tempFile Tập tin tạm thời để xóa
     */
    private static void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                // Được đánh dấu để loại bỏ khi thoát
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * Xử lý các phản hồi thuộc loại nhị phân (hình ảnh, .PDF、gói nén, v.v.)
     * <p>
     * Phản hồi nhị phân sẽ được lưu vào một tệp tạm thời và tên tệp sẽ được lấy theo mức độ ưu tiên sau: .
     * 1. Content-Disposition Tên tệp trong tiêu đề phản hồi
     * 2. URL Tên tập tin trong đường dẫn
     * 3. theo Content-Type Thế hệ thông minh
     * </p>
     *
     * @param okResponse OkHttp đối tượng phản hồi
     * @param response   Đối tượng phản hồi nội bộ
     * @throws IOException Các ngoại lệ có thể được đưa ra khi đọc nội dung phản hồi
     */
    private static void handleBinaryResponse(Response okResponse,
                                             HttpResponse response,
                                             DownloadProgressSinkFactory downloadProgressSinkFactory,
                                             ResponseSizeLimitWarningSink responseSizeLimitWarningSink,
                                             Call call) throws IOException {
        InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
        String ext = FileExtensionUtil.guessExtension(okResponse.header(CONTENT_TYPE_HEADER));
        response.fileName = resolveFileName(okResponse, response.headers, ext, (ResponseFileTypeDetector.Detection) null);
        String contentDisposition = ResponseFileNames.getContentDisposition(response.headers);
        response.fileAttachment = ResponseFileNames.isAttachment(contentDisposition);

        // Kiểm tra giới hạn kích thước tập tin
        int maxDownloadSize = getMaxDownloadSize();
        int contentLengthHeader = parseContentLength(okResponse);
        if (maxDownloadSize > 0 && contentLengthHeader > maxDownloadSize) {
            if (is != null) is.close();
            warnResponseTooLarge(responseSizeLimitWarningSink,
                    new ResponseSizeLimitWarning(ResponseSizeLimitWarning.Kind.BINARY, contentLengthHeader, maxDownloadSize));
            response.body = I18nUtil.getMessage(MessageKeys.BINARY_TOO_LARGE_BODY, maxDownloadSize / 1024 / 1024);
            response.bodySize = 0;
            response.filePath = null;
            return;
        }
        if (is != null) {
            try {
                FileAndSize fs = saveInputStreamToTempFile(
                        is,
                        "devWorkbench_download_",
                        ext,
                        contentLengthHeader,
                        downloadProgressSinkFactory,
                        isStreamingMediaResponse(okResponse, contentDisposition),
                        okResponse.request().url().toString(),
                        call
                );
                response.filePath = fs.file.getAbsolutePath();
                response.body = I18nUtil.getMessage(MessageKeys.BINARY_SAVED_TEMP_FILE);
                response.bodySize = fs.size;
                applyDetectedBinaryType(okResponse, response, fs.file, ext);
                // Liệu thẻ có phải là loại hình ảnh hay không, được cung cấp UI Sử dụng xem trước lớp
                String ct = okResponse.header(CONTENT_TYPE_HEADER, "");
                // Khi nội dung mâu thuẫn với nội dung khai báo thì kết quả nhận dạng sẽ được ưu tiên áp dụng và tránh khai báo là image/png của PDF Như một bản xem trước hình ảnh
                response.isImage = response.detectedFileExtension != null
                        ? IMAGE_EXTENSIONS.contains(response.detectedFileExtension)
                        : ct.toLowerCase(Locale.ROOT).startsWith("image/");
            } catch (DownloadSizeLimitException e) {
                warnResponseTooLarge(responseSizeLimitWarningSink,
                        new ResponseSizeLimitWarning(ResponseSizeLimitWarning.Kind.BINARY,
                                (int) Math.min(e.receivedBytes, Integer.MAX_VALUE), e.maxDownloadBytes));
                response.body = I18nUtil.getMessage(MessageKeys.BINARY_TOO_LARGE_BODY, e.maxDownloadBytes / 1024 / 1024);
                response.bodySize = 0;
                response.filePath = null;
            } catch (IOException e) {
                if (!isIncompleteResponseBodyError(e)) {
                    throw e;
                }
                // Xử lý tình trạng gián đoạn kết nối trong quá trình tải xuống
                log.error("Failed to download complete binary response: {}", e.getMessage());
                response.body = I18nUtil.getMessage(MessageKeys.RESPONSE_INCOMPLETE, e.getMessage());
                response.bodyIncomplete = true;
                response.bodySize = 0;
                response.filePath = null;
            }
        } else {
            response.body = I18nUtil.getMessage(MessageKeys.NO_RESPONSE_BODY);
            response.bodySize = 0;
        }
    }


    /**
     * Tất cả các phản hồi nhị phân đều kiểm tra tiêu đề tệp: khai báo chung (application/octet-stream）Kết quả nhận dạng được sử dụng khi;
     * Tuyên bố này cụ thể nhưng mâu thuẫn với nội dung (application/vnd.ms-excel Trên thực tế là có xlsx）Nội dung sẽ chiếm ưu thế và nhanh chóng;
     * Đồng ý hoặc tuyên bố cụ thể hơn (tuyên bố .xlsm、Tiêu đề tập tin chỉ có thể được xác định .xlsx）Tuyên bố sẽ được ưu tiên áp dụng. Kết quả nhận dạng cũng được sử dụng để hiệu chỉnh URL Hậu tố sai trong .
     */
    private static void applyDetectedBinaryType(Response okResponse, HttpResponse response, File file, String declaredExtension) {
        ResponseFileTypeDetector.Detection detection = ResponseFileTypeDetector.detect(file.toPath());
        if (detection == null) {
            return;
        }
        String detected = detection.extension();
        String contentType = okResponse.header(CONTENT_TYPE_HEADER, "");
        // application/zip Nó cũng chỉ chỉ ra rằng nó là một thùng chứa.docx/jar v.v. dựa trên ZIP Định dạng cụ thể không được tính là xung đột loại
        boolean genericDeclared = contentType.isBlank() || ".zip".equals(declaredExtension)
                || ResponseFiles.isGenericBinaryContentType(contentType);
        if (!genericDeclared && declaredExtension == null && ResponseFileTypeDetector.isGenericContainer(detected)) {
            // Cụ thể nhưng không có bản đồ mở rộng MIME（Chẳng hạn như APK của application/vnd.android.package-archive），Chỉ tin cậy vào xác nhận quyền sở hữu khi vùng chứa được nhận dạng
            return;
        }
        String naming = genericDeclared ? detected : ResponseFileTypeDetector.reconcile(declaredExtension, detection);
        // Chỉ khi nội dung không nhất quán với câu phát biểu thì mới nhắc:application/pdf + PDF、application/zip + ZIP/docx/jar Không có lời nhắc nào cả
        boolean consistent = naming.equalsIgnoreCase(String.valueOf(declaredExtension))
                || (".zip".equals(declaredExtension) && ResponseFileTypeDetector.reconcile(naming, ".zip").equals(naming));
        if (!consistent) {
            response.detectedFileExtension = detected;
        }
        response.fileName = resolveFileName(okResponse, response.headers, naming, detection);
    }

    private static boolean isStreamingMediaResponse(Response response, String contentDisposition) {
        String contentType = response.header(CONTENT_TYPE_HEADER, "");
        String mediaType = contentType.split(";", 2)[0].trim();
        String disposition = contentDisposition == null ? "" : contentDisposition.split(";", 2)[0].trim();
        // FLV may also be a finite file. This selects unknown-length transfer UX
        // without claiming the stream is live or changing the EOF completion rule.
        return "video/x-flv".equalsIgnoreCase(mediaType)
                && response.body() != null && response.body().contentLength() < 0
                && !"attachment".equalsIgnoreCase(disposition);
    }

    /**
     * Xử lý các phản hồi của loại văn bản (JSON、XML、HTML、văn bản thuần túy, v.v.)
     * <p>
     * Các chiến lược xử lý phản hồi văn bản:
     * 1. Nếu nội dung vượt quá giới hạn kích thước tải xuống tối đa, hãy hiển thị cảnh báo và từ chối tải
     * 2. Nếu nội dung vượt quá giới hạn kích thước hiển thị tối đa, nó sẽ được lưu vào một tệp tạm thời
     * 3. Nếu không nó sẽ được hiển thị trực tiếp trong trình chỉnh sửa
     * </p>
     *
     * @param okResponse          OkHttp đối tượng phản hồi
     * @param response            Đối tượng phản hồi nội bộ
     * @param contentLengthHeader Content-Length Giá trị của tiêu đề phản hồi
     * @throws IOException Các ngoại lệ có thể được đưa ra khi đọc nội dung phản hồi
     */
    private static void handleTextResponse(Response okResponse,
                                           HttpResponse response,
                                           int contentLengthHeader,
                                           DownloadProgressSinkFactory downloadProgressSinkFactory,
                                           ResponseSizeLimitWarningSink responseSizeLimitWarningSink,
                                           Call call) throws IOException {
        String ext = FileExtensionUtil.guessExtension(okResponse.header(CONTENT_TYPE_HEADER));
        int maxDownloadSize = getMaxDownloadSize();
        ResponseBody body = okResponse.body();

        // Kiểm tra xem kích thước tải xuống tối đa có vượt quá không
        if (maxDownloadSize > 0 && contentLengthHeader > maxDownloadSize) {
            if (body != null) body.close();
            warnResponseTooLarge(responseSizeLimitWarningSink,
                    new ResponseSizeLimitWarning(ResponseSizeLimitWarning.Kind.TEXT, contentLengthHeader, maxDownloadSize));
            response.body = I18nUtil.getMessage(MessageKeys.TEXT_TOO_LARGE_BODY, maxDownloadSize / 1024 / 1024);
            response.bodySize = 0;
            response.filePath = null;
            return;
        }
        if (body != null) {
            byte[] bytes;
            try {
                bytes = body.bytes();
            } catch (IOException e) {
                if (isIncompleteResponseBodyError(e)) {
                    log.error("Failed to read complete response body: {}", e.getMessage());
                    response.body = I18nUtil.getMessage(MessageKeys.RESPONSE_INCOMPLETE, e.getMessage());
                    response.bodyIncomplete = true;
                    response.bodySize = 0;
                    response.filePath = null;
                    return;
                }
                // Xử lý những người khác IO bất thường
                log.error("Error reading response body: {}", e.getMessage(), e);
                throw e;
            }
            if (maxDownloadSize > 0 && bytes.length > maxDownloadSize) {
                warnResponseTooLarge(responseSizeLimitWarningSink,
                        new ResponseSizeLimitWarning(ResponseSizeLimitWarning.Kind.TEXT, bytes.length, maxDownloadSize));
                response.body = I18nUtil.getMessage(MessageKeys.TEXT_TOO_LARGE_BODY, maxDownloadSize / 1024 / 1024);
                response.bodySize = 0;
                response.filePath = null;
                return;
            }
            response.bodySize = bytes.length;
            String extension = applyFileMetadata(okResponse, response, bytes, ext);
            if (bytes.length > getMaxBodySize()) { // Nếu nội dung giải nén vượt quá giá trị đã đặt, nó sẽ được lưu dưới dạng tệp tạm thời
                FileAndSize fs = saveInputStreamToTempFile(
                        new ByteArrayInputStream(bytes),
                        "devWorkbench_text_download_",
                        extension,
                        bytes.length,
                        downloadProgressSinkFactory,
                        false,
                        okResponse.request().url().toString(),
                        call
                );
                response.filePath = fs.file.getAbsolutePath();
                int maxBodySizeKB = getMaxBodySize() / 1024;
                response.body = I18nUtil.getMessage(MessageKeys.BODY_TOO_LARGE_SAVED, maxBodySizeKB);
            } else {
                decodeInMemoryBody(response, bytes, body.contentType());
                response.filePath = null;
            }
        } else {
            response.body = "";
            response.bodySize = 0;
            response.filePath = null;
        }
    }

    /**
     * Xác định định dạng tệp và khai báo tệp đính kèm dựa trên nội dung, điền thông tin meta tệp và trả về phần mở rộng để lưu.
     * Giao diện văn bản đơn giản giữ tên tệp thông minh; chỉ có các phản hồi tệp rõ ràng mới được tuân theo Content-Disposition / URL Tên tập tin được suy ra.
     */
    private static String applyFileMetadata(Response okResponse, HttpResponse response, byte[] bytes, String contentTypeExtension) {
        ResponseFileTypeDetector.Detection signature = ResponseFileTypeDetector.detect(bytes);
        String detectedExtension = signature == null ? null : signature.extension();
        if (detectedExtension == null
                && mayBeUnlabeledBinary(okResponse.header(CONTENT_TYPE_HEADER, ""), ResponseFiles.declaredCharset(response))
                && ResponseFileTypeDetector.looksBinary(bytes)) {
            // Loại không được khai báo (hoặc được khai báo là text/plain）nhưng chứa các byte điều khiển không xuất hiện trong văn bản: được coi là nhị phân không xác định
            detectedExtension = UNKNOWN_BINARY_EXTENSION;
        }
        response.detectedFileExtension = detectedExtension;
        response.fileAttachment = ResponseFileNames.isAttachment(
                ResponseFileNames.getContentDisposition(response.headers));
        String extension = detectedExtension != null
                ? detectedExtension
                : (contentTypeExtension != null ? contentTypeExtension : ".txt");
        response.fileName = detectedExtension != null || response.fileAttachment
                ? resolveFileName(okResponse, response.headers, extension, signature)
                : FileExtensionUtil.generateSmartFileName(extension);
        return extension;
    }

    /**
     * Giải mã nội dung phản hồi trong bộ nhớ; chỉ giữ các byte gốc nếu văn bản không thể được khôi phục một cách dễ dàng hoặc phản hồi là một tệp, tránh thông thường UTF-8 Bộ nhớ văn bản tăng gấp đôi.
     */
    /**
     * Phán quyết byte điều khiển chỉ áp dụng cho  ASCII Mã hóa tương thích;UTF-16/UTF-32 Bản thân văn bản chứa rất nhiều 0x00，Nó không thể được đánh giá dựa trên điều này.
     */
    private static boolean mayBeUnlabeledBinary(String contentType, Charset declaredCharset) {
        String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        boolean unlabeledOrPlainText = mediaType.isEmpty() || "text/plain".equalsIgnoreCase(mediaType);
        return unlabeledOrPlainText && isAsciiCompatible(declaredCharset);
    }

    private static boolean isAsciiCompatible(Charset charset) {
        try {
            byte[] encoded = "\t\n\r A~".getBytes(charset);
            return Arrays.equals(encoded, "\t\n\r A~".getBytes(StandardCharsets.US_ASCII));
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private static void decodeInMemoryBody(HttpResponse response, byte[] bytes, MediaType mediaType) {
        Charset charset = mediaType != null && mediaType.charset() != null
                ? mediaType.charset()
                : StandardCharsets.UTF_8;
        String strictText = decodeStrictly(bytes, charset);
        response.body = strictText != null ? strictText : new String(bytes, charset);
        boolean losslessUtf8Text = strictText != null && StandardCharsets.UTF_8.equals(charset);
        if (response.detectedFileExtension != null || response.fileAttachment || !losslessUtf8Text) {
            response.rawBodyBytes = bytes;
        }
    }

    private static String decodeStrictly(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String resolveFileName(Response okResponse, Map<String, List<String>> headers,
                                          String extension, ResponseFileTypeDetector.Detection detection) {
        return ResponseFileNames.resolve(
                ResponseFileNames.getContentDisposition(headers),
                okResponse.request().url().encodedPath(),
                extension,
                detection);
    }

    private static void warnResponseTooLarge(ResponseSizeLimitWarningSink sink, ResponseSizeLimitWarning warning) {
        ResponseSizeLimitWarningSink resolvedSink = sink == null ? ResponseSizeLimitWarningSink.noop() : sink;
        resolvedSink.warn(warning);
    }

    /**
     * Lớp trình bao bọc đơn giản cho các tệp và kích thước
     * Được sử dụng cho saveInputStreamToTempFile Giá trị trả về của phương thức
     */
    private static class FileAndSize {
        /**
         * Các tập tin tạm thời
         */
        final File file;
        /**
         * Số byte thực tế được ghi
         */
        final long size;

        /**
         * Người xây dựng
         *
         * @param file Các tập tin tạm thời
         * @param size Số byte thực tế được ghi
         */
        FileAndSize(File file, long size) {
            this.file = file;
            this.size = size;
        }
    }

    private static final class DownloadSizeLimitException extends IOException {
        private final long receivedBytes;
        private final int maxDownloadBytes;

        private DownloadSizeLimitException(long receivedBytes, int maxDownloadBytes) {
            super("Response exceeded the download size limit");
            this.receivedBytes = receivedBytes;
            this.maxDownloadBytes = maxDownloadBytes;
        }
    }
}
