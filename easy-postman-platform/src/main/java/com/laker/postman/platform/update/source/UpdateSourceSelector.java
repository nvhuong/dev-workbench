package com.laker.postman.platform.update.source;

import com.laker.postman.platform.update.UpdateSettingsProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cập nhật bộ chọn nguồn - Chịu trách nhiệm lựa chọn nguồn cập nhật tốt nhất dựa trên cấu hình hoặc phát hiện tự động
 *
 * <p>Ba chế độ được hỗ trợ:</p>
 * <ul>
 *   <li>github - Sử dụng cố định GitHub</li>
 *   <li>gitee - Sử dụng cố định Gitee</li>
 *   <li>auto - Tự động chọn nguồn nhanh nhất</li>
 * </ul>
 */
@Slf4j
public class UpdateSourceSelector {

    private final UpdateSettingsProvider settingsProvider;

    /** GitHub Cập nhật nguồn */
    private final UpdateSource githubSource;

    /** Gitee Cập nhật nguồn */
    private final UpdateSource giteeSource;

    /** Nguồn tốt nhất cho bộ nhớ đệm */
    private volatile UpdateSource cachedBestSource = null;

    /** Thời gian chờ tự động phát hiện (giây) */
    private static final int AUTO_DETECT_TIMEOUT_SECONDS = 6;

    /** Ngưỡng phản hồi nhanh (mili giây) */
    private static final long FAST_RESPONSE_THRESHOLD_MS = 3000;

    public UpdateSourceSelector() {
        this(UpdateSettingsProvider.defaults());
    }

    public UpdateSourceSelector(UpdateSettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider == null ? UpdateSettingsProvider.defaults() : settingsProvider;
        this.githubSource = new GitHubUpdateSource();
        this.giteeSource = new GiteeUpdateSource();
    }

    /**
     * Chọn nguồn cập nhật tốt nhất
     *
     * @return Nguồn cập nhật đã chọn
     */
    public UpdateSource selectBestSource() {
        String preference = settingsProvider.getUpdateSourcePreference();

        // Người dùng chỉ định GitHub
        if ("github".equalsIgnoreCase(preference)) {
            log.info("Using user-preferred source: GitHub");
            cachedBestSource = githubSource;
            return githubSource;
        }

        // Người dùng chỉ định Gitee
        if ("gitee".equalsIgnoreCase(preference)) {
            log.info("Using user-preferred source: Gitee");
            cachedBestSource = giteeSource;
            return giteeSource;
        }

        // auto Chế độ: Chọn tự động
        return selectSourceAutomatically();
    }

    /**
     * Tự động chọn nguồn cập nhật nhanh nhất
     */
    private UpdateSource selectSourceAutomatically() {
        // Nếu có bộ đệm, hãy sử dụng nó trực tiếp
        if (cachedBestSource != null) {
            log.info("Using cached best source: {}", cachedBestSource.getName());
            return cachedBestSource;
        }

        log.info("Auto-detecting best update source...");

        // Kiểm tra song song cả hai nguồn
        CompletableFuture<Long> githubTest = CompletableFuture.supplyAsync(githubSource::testConnectionSpeed);
        CompletableFuture<Long> giteeTest = CompletableFuture.supplyAsync(giteeSource::testConnectionSpeed);

        try {
            // Chờ cho quá trình kiểm tra hoàn tất
            CompletableFuture.allOf(githubTest, giteeTest).get(AUTO_DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            long githubTime = githubTest.getNow(Long.MAX_VALUE);
            long giteeTime = giteeTest.getNow(Long.MAX_VALUE);

            // Chọn một nguồn nhanh hơn
            if (githubTime < giteeTime && githubTime < FAST_RESPONSE_THRESHOLD_MS) {
                log.info("GitHub is faster ({} ms vs {} ms), using GitHub", githubTime, giteeTime);
                cachedBestSource = githubSource;
                return githubSource;
            } else if (giteeTime < Long.MAX_VALUE) {
                log.info("Gitee is faster or preferred ({} ms vs {} ms), using Gitee", giteeTime, githubTime);
                cachedBestSource = giteeSource;
                return giteeSource;
            }
        } catch (Exception e) {
            log.debug("Source detection error: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        // Được sử dụng theo mặc định Gitee（Hầu hết người dùng trong nước)
        log.info("Using default source: Gitee");
        cachedBestSource = giteeSource;
        return giteeSource;
    }

    /**
     * Nhận nguồn dự phòng (dùng khi nguồn chính bị lỗi)
     *
     * @param primarySource nguồn chính
     * @return Nguồn thay thế
     */
    public UpdateSource getFallbackSource(UpdateSource primarySource) {
        if (primarySource == githubSource) {
            return giteeSource;
        }
        return githubSource;
    }

    /**
     * Nhận nguồn thay thế dựa trên tên nguồn trong thông tin phát hành.
     * Không cần phỏng đoán đối với các nguồn không xác định để tránh gửi yêu cầu tải xuống tới máy nhân bản sai.
     */
    public UpdateSource getFallbackSource(String primarySourceName) {
        if (primarySourceName == null) {
            return null;
        }
        if (githubSource.getName().equalsIgnoreCase(primarySourceName)) {
            return giteeSource;
        }
        if (giteeSource.getName().equalsIgnoreCase(primarySourceName)) {
            return githubSource;
        }
        return null;
    }

    /**
     * Nguồn tốt nhất để xóa bộ nhớ cache
     */
    public void clearCache() {
        cachedBestSource = null;
    }
}
