package com.laker.postman.service.update;

import cn.hutool.json.JSONArray;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.panel.update.AutoUpdateNotification;
import com.laker.postman.panel.update.ModernProgressDialog;
import com.laker.postman.panel.update.ModernUpdateDialog;
import com.laker.postman.panel.update.NoAssetDialog;
import com.laker.postman.platform.update.asset.FallbackDownloadUrlResolver;
import com.laker.postman.platform.update.asset.PlatformDownloadUrlResolver;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.platform.update.source.UpdateSourceSelector;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;

/**
 * đổi mới UI Bộ điều khiển, chịu trách nhiệm cập nhật thông báo, hộp thoại và giao diện tiến trình tải xuống.
 */
@Slf4j
public class UpdateUiController {

    private final UpdateDownloader downloader;
    private final UpdateSourceSelector sourceSelector;
    private final PlatformDownloadUrlResolver downloadUrlResolver;
    private final FallbackDownloadUrlResolver fallbackDownloadUrlResolver;

    public UpdateUiController() {
        this.downloader = new UpdateDownloader();
        this.sourceSelector = new UpdateSourceSelector(SettingManager::getUpdateSourcePreference);
        this.downloadUrlResolver = new PlatformDownloadUrlResolver();
        this.fallbackDownloadUrlResolver = new FallbackDownloadUrlResolver(sourceSelector);
    }

    /**
     * Hiển thị thông báo cập nhật nền (cửa sổ bật lên ở góc dưới bên phải)
     */
    public void showUpdateNotification(UpdateInfo updateInfo) {
        showUpdateNotification(updateInfo, () -> {
        });
    }

    public void showUpdateNotification(UpdateInfo updateInfo, Runnable onIgnoreVersion) {
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        AutoUpdateNotification.show(
                mainFrame,
                updateInfo,
                selectedUpdate -> showUpdateDialog(selectedUpdate, onIgnoreVersion)
        );
    }

    /**
     * Hiển thị hộp thoại cập nhật (có nhật ký cập nhật)- Sử dụng hộp thoại hiện đại
     */
    public void showUpdateDialog(UpdateInfo updateInfo) {
        showUpdateDialog(updateInfo, () -> {
        });
    }

    public void showUpdateDialog(UpdateInfo updateInfo, Runnable onIgnoreVersion) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);

            // Sử dụng hộp thoại cập nhật hiện đại
            int choice = ModernUpdateDialog.showUpdateDialog(mainFrame, updateInfo);

            // Xử lý các lựa chọn của người dùng: 0=Tải xuống theo cách thủ công, 1=Cập nhật tự động, 2=Nhắc lại sau, 3=Bỏ qua phiên bản này, -1=Đóng hộp thoại
            switch (choice) {
                case 0 -> openManualDownloadPage();
                case 1 -> showUpdateTypeSelectionAndStart(updateInfo);
                case 3 -> runIgnoreVersion(onIgnoreVersion);
                case 2, -1 -> { /* Người dùng sẽ được nhắc nhở sau hoặc đóng hộp thoại */ }
                default -> log.debug("Unknown dialog choice: {}", choice);
            }
        });
    }

    private void runIgnoreVersion(Runnable onIgnoreVersion) {
        if (onIgnoreVersion != null) {
            onIgnoreVersion.run();
        }
    }

    /**
     * Một hộp thoại nhắc nhở hiển thị cho biết có phiên bản mới nhưng chưa có gói cài đặt (bật lên trực tiếp khi kiểm tra thủ công)
     */
    public void showNoAssetDialog(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
            boolean goToGitHub = NoAssetDialog.show(mainFrame, updateInfo);
            if (goToGitHub) {
                openGitHubReleasePage(updateInfo.getLatestVersion());
            }
        });
    }

    /**
     * Khi kiểm tra lý lịch tìm thấy phiên bản mới nhưng không có gói cài đặt thì góc dưới bên phải sẽ hiển thị đầu tiên toast；
     * Người dùng bấm vào "Đi GitHub」Chơi lại sau NoAssetDialog
     */
    public void showNoAssetNotification(UpdateInfo updateInfo) {
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        AutoUpdateNotification.showNoAsset(mainFrame, updateInfo, this::showNoAssetDialog);
    }

    /**
     * Mở GitHub Trang phát hành (phiên bản được chỉ định)
     */
    private void openGitHubReleasePage(String version) {
        try {
            String url = "https://github.com/nvhuong/easy-postman/releases/tag/" + version;
            log.info("Opening GitHub release page: {}", url);
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            log.error("Failed to open GitHub release page", ex);
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()));
        }
    }

    /**
     * Mở trang tải xuống thủ công - Tự động chọn nguồn cập nhật tốt nhất
     */
    private void openManualDownloadPage() {
        try {
            // Chọn nguồn cập nhật tốt nhất
            UpdateSource source = sourceSelector.selectBestSource();
            String releaseUrl = source.getWebUrl();

            log.info("Opening manual download page from {}: {}", source.getName(), releaseUrl);
            Desktop.getDesktop().browse(new URI(releaseUrl));
        } catch (Exception ex) {
            log.error("Failed to open manual download page", ex);
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()));
        }
    }

    /**
     * Bắt đầu nâng cấp hoàn toàn im lặng
     */
    private void showUpdateTypeSelectionAndStart(UpdateInfo updateInfo) {

        // Kiểm tra các gói cài đặt có sẵn
        JSONArray assets = updateInfo.getReleaseInfo() != null ?
                updateInfo.getReleaseInfo().getJSONArray("assets") : null;

        if (assets == null || assets.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        // Nhận liên kết tải xuống gói cài đặt dành riêng cho nền tảng
        String installerUrl = downloadUrlResolver.resolveDownloadUrl(assets);

        if (installerUrl == null) {
            // Gói cài đặt tạm thời không khả dụng và hộp thoại hiện đại được sử dụng để hướng dẫn người dùng GitHub Tải xuống theo cách thủ công
            log.warn("No installer URL found for current platform, showing NoAssetDialog");
            boolean goToGitHub = NoAssetDialog.show(
                    UiSingletonFactory.getInstance(MainFrame.class), updateInfo);
            if (goToGitHub) {
                openGitHubReleasePage(updateInfo.getLatestVersion());
            }
            return;
        }

        // Bắt đầu cập nhật đầy đủ trực tiếp
        startAutomaticUpdate(updateInfo, installerUrl);
    }

    /**
     * Bắt đầu cập nhật tự động - Sử dụng hộp thoại tiến trình hiện đại
     *
     * <p>Chuỗi khóa:
     * <ol>
     *   <li>Trước tiên hãy bắt đầu tải xuống trong chuỗi nền (downloader.downloadAsync），Tại thời điểm này, cuộc gọi lại tiến trình đã sẵn sàng</li>
     *   <li>Đăng ký cancelListener（Vào show() bị chặn EDT hoàn thành trước đó)</li>
     *   <li>Cuộc gọi cuối cùng progressDialog.show()，Nó sẽ ở đó EDT thượng đẳng modal-block，
     *       cho đến khi hide() Nó chỉ được phát hành sau khi được gọi bằng lệnh gọi lại tải xuống</li>
     * </ol>
     * Bằng cách này, tiến trình được gọi trở lại invokeLater Nó sẽ chảy vào bình thường EDT Xếp hàng, thanh tiến trình sẽ cuộn.
     * </p>
     *
     * @param updateInfo  Cập nhật thông tin phát hành
     * @param downloadUrl Liên kết tải xuống
     */
    private void startAutomaticUpdate(UpdateInfo updateInfo, String downloadUrl) {
        if (downloadUrl == null) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        ModernProgressDialog progressDialog = new ModernProgressDialog(
                UiSingletonFactory.getInstance(MainFrame.class));

        // 1. Đầu tiên hãy đăng ký lệnh gọi lại hủy (in show() bị chặn EDT Trước)
        progressDialog.setOnCancelListener(downloader::cancel);

        // 2. Trước tiên hãy bắt đầu tải xuống trong một chuỗi nền
        String primarySourceName = updateInfo.getReleaseInfo() == null ? null
                : updateInfo.getReleaseInfo().getStr(UpdateSource.SOURCE_METADATA_KEY);
        try {
            downloader.downloadAsync(
                    downloadUrl,
                    () -> fallbackDownloadUrlResolver.resolve(updateInfo.getLatestVersion(), primarySourceName),
                    new UpdateDownloader.DownloadProgressCallback() {
                        @Override
                        public void onProgress(int percentage, long downloaded, long total, double speed) {
                            // show() bị chặn EDT，invokeLater Xếp hàng đợi EDT Nó sẽ được thực thi khi nó trống
                            progressDialog.updateProgress(percentage, downloaded, total, speed);
                        }

                        @Override
                        public void onCompleted(File downloadedFile) {
                            progressDialog.hide(); // Nâng lên modal bị chặn
                            SwingUtilities.invokeLater(() -> showInstallPrompt(downloadedFile));
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressDialog.hide(); // Nâng lên modal bị chặn
                            SwingUtilities.invokeLater(() -> NotificationCenter.showError(errorMessage));
                        }

                        @Override
                        public void onCancelled() {
                            // triggerCancel() Đã điều chỉnh rồi hideNow()，Chỉ cần một thông báo ở đây
                            SwingUtilities.invokeLater(() -> NotificationCenter.showInfo(
                                    I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_CANCELLED)));
                        }
                    });
        } catch (UpdateDownloader.DownloadInProgressException ex) {
            progressDialog.hide();
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_IN_PROGRESS));
            return;
        }

        // 3. Cuối cùng show() — modal bị chặn EDT，cho đến khi hide() Chỉ trả về sau khi được gọi
        //    Bản thân phương pháp này đã có sẵn EDT trên (bởi showUpdateTypeSelectionAndStart -> SwingUtilities.invokeLater đảm bảo)
        progressDialog.show();
    }

    /**
     * Hiển thị lời nhắc cài đặt và cài đặt âm thầm
     *
     * @param installerFile Đã tải xuống tập tin gói cài đặt
     */
    private void showInstallPrompt(File installerFile) {
        // Cập nhật gói cài đặt đầy đủ: cài đặt im lặng
        String message = I18nUtil.getMessage(MessageKeys.UPDATE_INSTALLER_INSTALL_PROMPT);

        int choice = JOptionPane.showConfirmDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            downloader.discardDownloadedInstaller(installerFile);
            return;
        }

        downloader.installUpdate(installerFile, success -> {
            if (!success) {
                downloader.discardDownloadedInstaller(installerFile);
                SwingUtilities.invokeLater(() -> NotificationCenter.showError(
                        I18nUtil.getMessage(MessageKeys.UPDATE_INSTALL_FAILED)));
            }
        });
    }
}
