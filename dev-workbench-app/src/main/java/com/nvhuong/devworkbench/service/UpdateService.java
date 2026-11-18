package com.nvhuong.devworkbench.service;

import com.nvhuong.devworkbench.ioc.Autowired;
import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.ioc.PreDestroy;
import com.nvhuong.devworkbench.platform.update.model.UpdateInfo;
import com.nvhuong.devworkbench.service.update.AppUpdateCheckCoordinator;
import com.nvhuong.devworkbench.service.update.plugin.PluginUpdateCheckResult;
import com.nvhuong.devworkbench.service.update.plugin.PluginUpdateCheckCoordinator;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Dịch vụ cập nhật phiên bản
 */
@Slf4j
@Component
public class UpdateService {

    @Autowired
    private AppUpdateCheckCoordinator appUpdateCheckCoordinator;

    @Autowired
    private PluginUpdateCheckCoordinator pluginUpdateCheckCoordinator;

    /**
     * Kiểm tra các bản cập nhật không đồng bộ khi khởi động
     */
    public void checkUpdateOnStartup() {
        // Bắt đầu kiểm tra cập nhật nền
        appUpdateCheckCoordinator.startBackgroundCheck();
        pluginUpdateCheckCoordinator.startBackgroundCheck();
    }

    /**
     * Kiểm tra cập nhật theo cách thủ công (đối với các lệnh gọi menu)
     */
    public void checkUpdateManually() {
        CompletableFuture<PluginUpdateCheckResult> pluginUpdateFuture = pluginUpdateCheckCoordinator.checkForUpdateManually();
        appUpdateCheckCoordinator.checkForUpdateManually()
                .thenCombine(pluginUpdateFuture, (updateInfo, pluginUpdateResult) -> {
                    handleManualCheckResult(updateInfo, pluginUpdateResult);
                    return null;
                })
                .exceptionally(throwable -> {
                    log.error("Manual update check failed", throwable);
                    return null;
                });
    }

    private void handleManualCheckResult(UpdateInfo updateInfo, PluginUpdateCheckResult pluginUpdateResult) {
        boolean appNoUpdate = updateInfo != null && updateInfo.getStatus() == UpdateInfo.Status.NO_UPDATE;
        boolean pluginCheckSucceeded = pluginUpdateResult != null && !pluginUpdateResult.failed();
        boolean pluginHasUpdates = pluginUpdateResult != null && pluginUpdateResult.hasUpdates();

        if (appNoUpdate && pluginCheckSucceeded && !pluginHasUpdates) {
            NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.UPDATE_ALREADY_LATEST_WITH_PLUGINS));
            return;
        }

        if (!(appNoUpdate && pluginHasUpdates)) {
            appUpdateCheckCoordinator.handleUpdateCheckResult(updateInfo, true);
        }
        pluginUpdateCheckCoordinator.handleManualCheckResult(pluginUpdateResult);
    }

}
