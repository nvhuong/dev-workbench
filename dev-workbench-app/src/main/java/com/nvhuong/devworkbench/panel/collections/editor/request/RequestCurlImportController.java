package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.curl.CurlImportUtil;
import com.nvhuong.devworkbench.service.curl.CurlParser;
import com.nvhuong.devworkbench.service.curl.CurlRequest;
import com.nvhuong.devworkbench.util.AsyncClipboardUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * cURL Nhập bộ điều khiển nhập.
 * <p>
 * URL Ô nhập liệu chỉ có nhiệm vụ mang văn bản và nhận dạng nó cURL、Các yêu cầu phân tích cú pháp và dọn dẹp clipboard tập trung ở đây.
 */
@RequiredArgsConstructor
final class RequestCurlImportController {
    private final JTextField urlField;
    private final Consumer<HttpRequestItem> requestImporter;

    void detectAndImport(boolean loadingData) {
        if (loadingData) {
            return;
        }

        String text = urlField.getText();
        if (text != null && text.trim().toLowerCase().startsWith("curl")) {
            SwingUtilities.invokeLater(() -> importCurl(text.trim()));
        }
    }

    private void importCurl(String text) {
        try {
            CurlRequest curlRequest = CurlParser.parse(text);
            if (curlRequest == null || curlRequest.url == null) {
                return;
            }

            HttpRequestItem item = CurlImportUtil.fromCurlRequest(curlRequest);
            if (item == null) {
                return;
            }

            requestImporter.accept(item);
            AsyncClipboardUtil.clearStringAsync();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.PARSE_CURL_SUCCESS));
        } catch (Exception ignored) {
            // Người dùng có thể vẫn đang gõ giữa chừng cURL，Giữ im lặng để tránh thường xuyên mắc lỗi trong quá trình nhập liệu.
        }
    }
}
