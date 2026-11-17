package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.file.ResponseFileNames;
import com.laker.postman.http.runtime.file.ResponseFiles;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.util.FileExtensionUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.io.File;
import java.util.Locale;

/**
 * Hiển thị dữ liệu để biết tổng quan về tệp và“Liệu phản hồi có được hiển thị dưới dạng tổng quan về tệp hay không”quy tắc quyết định.
 *
 * @param fileName            Tải xuống tên tập tin
 * @param fileType            Hiển thị loại tệp (ví dụ XLSX）
 * @param bodySize            Số byte nội dung phản hồi
 * @param declaredContentType Được khai báo bởi máy chủ Content-Type，Có thể trống
 * @param detectedFileType    Loại được xác định dựa trên nội dung; khi nó không trống, dấu nhắc không khớp với kiểu đã khai báo
 * @param canViewAsText       Nó có được cung cấp không?“Xem bằng văn bản”（Chỉ có các tệp đính kèm văn bản như  CSV）
 * @param contentAvailable    Liệu nội dung tệp có thể tải xuống được hay không; ví dụ/Lịch sử chỉ lưu thông tin tập tin false
 */
record FileResponseModel(String fileName,
                         String fileType,
                         long bodySize,
                         String declaredContentType,
                         String detectedFileType,
                         boolean canViewAsText,
                         boolean contentAvailable) {

    /**
     * A file, attachment statement, or binary was identified Content-Type，Và khi có nội dung có thể tải xuống (hoặc nội dung đã bị bỏ qua rõ ràng), tổng quan về tệp sẽ được hiển thị.
     */
    static boolean shouldShow(HttpResponse response) {
        if (response == null) {
            return false;
        }
        if (response.fileContentOmitted) {
            return true;
        }
        if (!hasDownloadablePayload(response)) {
            return false;
        }
        return response.detectedFileExtension != null
                || response.fileAttachment
                || FileExtensionUtil.isBinaryType(ResponseFiles.contentType(response));
    }

    static FileResponseModel from(HttpResponse response) {
        String contentType = ResponseFiles.contentType(response);
        String fileType = resolveFileType(response, contentType);
        boolean detectedBinaryFile = response.detectedFileExtension != null;
        return new FileResponseModel(
                response.fileName,
                fileType,
                response.bodySize,
                contentType,
                detectedBinaryFile ? fileType : null,
                // The recognized binary format is only garbled when viewed as text, only for text attachments (eg CSV）cung cấp
                response.rawBodyBytes != null && !detectedBinaryFile,
                !response.fileContentOmitted
        );
    }

    private static boolean hasDownloadablePayload(HttpResponse response) {
        if (response.rawBodyBytes != null) {
            return true;
        }
        if (response.filePath == null || response.filePath.isBlank()) {
            return false;
        }
        File responseFile = new File(response.filePath);
        return responseFile.isFile() && responseFile.canRead();
    }

    private static String resolveFileType(HttpResponse response, String contentType) {
        String extension = response.detectedFileExtension;
        if (extension == null || extension.isBlank()) {
            extension = FileExtensionUtil.guessExtension(contentType);
        }
        if (extension == null || extension.isBlank()) {
            extension = ResponseFileNames.extensionOf(response.fileName);
        }
        if (extension == null || extension.isBlank()) {
            return I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_GENERIC_TYPE);
        }
        return (extension.startsWith(".") ? extension.substring(1) : extension).toUpperCase(Locale.ROOT);
    }
}
