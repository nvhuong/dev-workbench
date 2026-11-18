package com.nvhuong.devworkbench.http.runtime.transport;

import okhttp3.*;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;
import org.brotli.dec.BrotliInputStream;

import java.io.IOException;

import static com.nvhuong.devworkbench.util.HttpHeaderConstants.*;

/**
 * Trình chặn mạng: Tự động giải nén gzip、deflate、br Ba định dạng nén, giải nén trực tuyến, tương thích SSE/chunked/Phản ứng bình thường
 * Tham khảo nghiêm ngặt OkHttp BridgeInterceptor của promisesBody Logic, chỉ khi phản hồi tồn tại body Sau đó mới giải nén được
 * Xử lý đầu Vs BridgeInterceptor Hãy nhất quán, loại bỏ Content-Encoding/Content-Length，Đã đặt trước Content-Type
 */
public class CompressionDecompressNetworkInterceptor implements Interceptor {
    private boolean promisesBody(Response response) {
        if ("HEAD".equalsIgnoreCase(response.request().method())) {
            return false;
        }
        int responseCode = response.code();
        if ((responseCode < 100 || responseCode >= 200) && responseCode != 204 && responseCode != 304) {
            return true;
        }
        if (response.header(CONTENT_LENGTH) != null ||
                "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
            return true;
        }
        return false;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        String encoding = response.header(CONTENT_ENCODING);
        String contentLength = response.header(CONTENT_LENGTH);
        MediaType respContentType = response.body() != null ? response.body().contentType() : null;
        // Chỉ trong Content-Encoding tồn tại và promisesBody Xử lý trong thời gian thực để tránh tình trạng giải nén lặp lại và phản hồi phát trực tuyến bị treo
        if (encoding != null && response.body() != null && promisesBody(response)) {
            encoding = encoding.toLowerCase();
            BufferedSource decompressed;
            if ("gzip".equals(encoding)) {
                decompressed = Okio.buffer(new GzipSource(response.body().source()));
            } else if ("deflate".equals(encoding)) {
                decompressed = Okio.buffer(new AutoDetectDeflateSource(response.body().source()));
            } else if ("br".equals(encoding)) {
                BrotliInputStream brInputStream = new BrotliInputStream(response.body().byteStream());
                decompressed = Okio.buffer(Okio.source(brInputStream));
            } else {
                return response;
            }
            // Tham chiếu xử lý tiêu đề BridgeInterceptor，Đang giải nén trực tuyến, độ dài không xác định
            Response.Builder builder = response.newBuilder()
                    .removeHeader(CONTENT_ENCODING)
                    .removeHeader(CONTENT_LENGTH)
                    .header(DEV_WORKBENCH_CONTENT_ENCODING, encoding)
                    .body(ResponseBody.create(respContentType, -1, decompressed));

            // Nếu hóa ra là cóContent-Length，Lưu vàoDevWorkbench-Content-Length
            if (contentLength != null) {
                builder.header(DEV_WORKBENCH_CONTENT_LENGTH, contentLength);
            }

            return builder.build();
        }
        // Các cảnh khác (không có body、Phản hồi trực tuyến,HEAD/204/304v.v.), trả lại như cũ
        return response;
    }
}
