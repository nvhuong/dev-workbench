package com.laker.postman.http.runtime.error;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import java.io.IOException;

/**
 * Việc tải xuống bị hủy một cách bất thường
 * Ngoại lệ này được đưa ra khi người dùng chủ động hủy tải xuống, cho biết rằng đây là hành vi bình thường của người dùng chứ không phải lỗi
 */
public class DownloadCancelledException extends IOException {
    @Getter
    private final long receivedBytes;

    public DownloadCancelledException() {
        this(0L);
    }

    public DownloadCancelledException(long receivedBytes) {
        super(I18nUtil.getMessage(MessageKeys.DOWNLOAD_CANCELLED));
        this.receivedBytes = Math.max(0L, receivedBytes);
    }
}
