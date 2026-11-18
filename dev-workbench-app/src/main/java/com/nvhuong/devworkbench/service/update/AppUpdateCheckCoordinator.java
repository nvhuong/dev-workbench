package com.nvhuong.devworkbench.service.update;

import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.platform.update.UpdateCenter;
import com.nvhuong.devworkbench.platform.update.VersionChecker;
import com.nvhuong.devworkbench.platform.update.model.UpdateInfo;
import com.nvhuong.devworkbench.platform.update.model.UpdatePolicy;
import com.nvhuong.devworkbench.platform.update.model.UpdateTarget;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import com.nvhuong.devworkbench.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Điều phối viên kiểm tra cập nhật ứng dụng.
 * Kiểm tra mỗi khi chương trình khởi động và quyết định có thực hiện kiểm tra hay không dựa trên tần suất đã định cấu hình và thời gian kiểm tra gần đây nhất.
 */
@Slf4j
@Component
public class AppUpdateCheckCoordinator {

    private final VersionChecker versionChecker;
    private final UpdateUiController uiController;
    private final UpdateCenter updateCenter;

    /**
     * Nhóm luồng dành riêng cho kiểm tra cập nhật (đơn luồng, đảm bảo kiểm tra cập nhật được thực hiện tuần tự)
     */
    private static final ScheduledExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "UpdateChecker");
        thread.setDaemon(true);
        return thread;
    });

    public AppUpdateCheckCoordinator() {
        this(new VersionChecker(SettingManager::getUpdateSourcePreference),
                new UpdateUiController(),
                AppUpdateCenter.get());
    }

    AppUpdateCheckCoordinator(UpdateUiController uiController, UpdateCenter updateCenter) {
        this(new VersionChecker(SettingManager::getUpdateSourcePreference), uiController, updateCenter);
    }

    AppUpdateCheckCoordinator(VersionChecker versionChecker,
                              UpdateUiController uiController,
                              UpdateCenter updateCenter) {
        this.versionChecker = Objects.requireNonNull(versionChecker, "versionChecker");
        this.uiController = Objects.requireNonNull(uiController, "uiController");
        this.updateCenter = Objects.requireNonNull(updateCenter, "updateCenter");
    }

    /**
     * Bắt đầu kiểm tra cập nhật tự động (được gọi khi ứng dụng khởi động)
     * Xác định xem bạn có cần kiểm tra các bản cập nhật hay không dựa trên thời gian kiểm tra gần đây nhất và tần suất được định cấu hình
     */
    public void startBackgroundCheck() {
        String currentVersion = SystemUtil.getCurrentVersion();
        log.info("Current application version: {}", currentVersion);
        String osInfo = SystemUtil.getOsInfo();
        log.info("Detected operating system: \n{} ", osInfo);

        UPDATE_EXECUTOR.execute(() -> UpdateInstallerFileStore.forCurrentUser().cleanupStale());

        UpdatePolicy policy = updateCenter.policy(UpdateTarget.APP);
        if (!policy.enabled()) {
            log.info("Auto-update check is disabled");
            return;
        }

        long lastCheckTime = updateCenter.state(UpdateTarget.APP).lastCheckTimeMillis();
        long currentTime = System.currentTimeMillis();

        log.info("Update check frequency: {}, last check time: {}",
                policy.frequency().getCode(), lastCheckTime > 0 ? new Date(lastCheckTime) : "never");

        // Xác định xem bạn có cần kiểm tra các bản cập nhật hay không
        if (updateCenter.shouldCheck(UpdateTarget.APP, currentTime)) {
            log.info("Scheduling startup update check...");
            // Sử dụng nhóm luồng để thực thi không đồng bộ nhằm tránh chặn luồng đang gọi
            UPDATE_EXECUTOR.submit(() -> {
                try {
                    performUpdateCheck();
                } catch (Exception e) {
                    log.error("Unexpected error in update check task", e);
                }
            });
        } else {
            log.info("Skipping update check - not yet time according to frequency settings");
        }
    }

    /**
     * Kiểm tra cập nhật theo cách thủ công (được kích hoạt từ menu)
     * LƯU Ý: Kiểm tra thủ công cũng được cập nhật"Lần kiểm tra cuối cùng"
     */
    public CompletableFuture<UpdateInfo> checkForUpdateManually() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Manual update check initiated");
            UpdateInfo updateInfo = versionChecker.checkForUpdate();
            // Kiểm tra thủ công cũng ghi lại thời gian
            updateCenter.recordCheck(UpdateTarget.APP, System.currentTimeMillis());
            log.info("Manual update check completed, timestamp recorded");
            return updateInfo;
        }, UPDATE_EXECUTOR); // Sử dụng nhóm luồng kiểm tra cập nhật thống nhất
    }

    /**
     * Logic cốt lõi thực hiện kiểm tra cập nhật
     */
    private void performUpdateCheck() {
        log.debug("Starting update check task...");
        try {
            UpdateInfo updateInfo = versionChecker.checkForUpdate();
            // Ghi lại thời gian kiểm tra
            updateCenter.recordCheck(UpdateTarget.APP, System.currentTimeMillis());
            log.info("Update check completed successfully, timestamp recorded");

            handleUpdateCheckResult(updateInfo, false);

        } catch (Exception e) {
            log.warn("Update check failed: {}", e.getMessage(), e);
            // Ghi lại thời gian kiểm tra ngay cả khi không tránh được việc phải thử lại thường xuyên
            updateCenter.recordCheck(UpdateTarget.APP, System.currentTimeMillis());
        }
    }

    /**
     * Kết quả kiểm tra cập nhật quá trình
     *
     * @param updateInfo Cập nhật thông tin
     * @param isManual   Cho dù đó là kiểm tra thủ công
     */
    public void handleUpdateCheckResult(UpdateInfo updateInfo, boolean isManual) {
        if (updateInfo == null) {
            log.warn("Ignoring empty update check result");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            String marker = notificationMarker(updateInfo);
            if (!shouldNotify(updateInfo, updateCenter.ignoredMarkers(UpdateTarget.APP), isManual)) {
                log.info("Skipping app update notification because marker was ignored: {}", marker);
                return;
            }
            switch (updateInfo.getStatus()) {
                case UPDATE_AVAILABLE -> {
                    log.info("Update available: {} -> {}",
                            updateInfo.getCurrentVersion(), updateInfo.getLatestVersion());

                    if (isManual) {
                        // Kiểm tra thủ công hiển thị hộp thoại trực tiếp
                        uiController.showUpdateDialog(updateInfo, () -> rememberIgnoredMarker(updateInfo));
                    } else {
                        // Kiểm tra lý lịch hiển thị thông báo
                        uiController.showUpdateNotification(updateInfo, () -> rememberIgnoredMarker(updateInfo));
                    }
                }
                case UPDATE_AVAILABLE_NO_ASSET -> {
                    log.info("Update available but no asset for current platform: {} -> {}",
                            updateInfo.getCurrentVersion(), updateInfo.getLatestVersion());
                    if (isManual) {
                        // Kiểm tra thủ công: hiển thị trực tiếp NoAsset hộp thoại
                        uiController.showNoAssetDialog(updateInfo);
                    } else {
                        // Kiểm tra lý lịch: hiển thị góc dưới bên phải trước toast，Bấm vào và hộp thoại sẽ bật lên
                        uiController.showNoAssetNotification(updateInfo);
                    }
                }
                case NO_UPDATE -> {
                    if (isManual) {
                        // Chỉ hiển thị khi được kiểm tra thủ công"Đã là phiên bản mới nhất"
                        showNoUpdateMessage(updateInfo);
                    }
                    log.debug("No update available: {}", updateInfo.getMessage());
                }
                case CHECK_FAILED -> {
                    if (isManual) {
                        // Thông báo lỗi chỉ được hiển thị khi kiểm tra thủ công
                        showCheckFailedMessage(updateInfo);
                    }
                    log.debug("Update check failed: {}", updateInfo.getMessage());
                }
            }
        });
    }

    static boolean shouldNotify(UpdateInfo updateInfo, Set<String> ignoredMarkers, boolean isManual) {
        if (updateInfo == null) {
            return false;
        }
        String marker = notificationMarker(updateInfo);
        return marker.isEmpty()
                || isManual
                || ignoredMarkers == null
                || !ignoredMarkers.contains(marker.trim());
    }

    static String notificationMarker(UpdateInfo updateInfo) {
        if (updateInfo == null
                || updateInfo.getLatestVersion() == null
                || updateInfo.getLatestVersion().isBlank()
                || !(updateInfo.isUpdateAvailable() || updateInfo.isUpdateAvailableNoAsset())) {
            return "";
        }
        return UpdateTarget.APP.getId() + "@" + updateInfo.getLatestVersion().trim() + "@" + updateInfo.getStatus();
    }

    private void rememberIgnoredMarker(UpdateInfo updateInfo) {
        updateCenter.rememberIgnoredMarker(UpdateTarget.APP, notificationMarker(updateInfo));
    }

    /**
     * Không có thông báo cập nhật được hiển thị
     */
    private void showNoUpdateMessage(UpdateInfo updateInfo) {
        NotificationCenter.showInfo(updateInfo.getMessage());
    }

    /**
     * Một thông báo kiểm tra lỗi được hiển thị
     */
    private void showCheckFailedMessage(UpdateInfo updateInfo) {
        NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.ERROR_UPDATE_FAILED, updateInfo.getMessage()));
    }

}
