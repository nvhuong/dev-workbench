package com.laker.postman.panel.collections.editor.request.sub;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.http.runtime.file.ResponseFiles;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.util.FileChooserUtil;
import com.laker.postman.util.FileExtensionUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Tải xuống nội dung phản hồi: Chọn vị trí lưu, viết nội dung gốc ở chế độ nền và thông báo cho người gọi để cập nhật trạng thái nút trong quá trình lưu.
 */
@Slf4j
final class ResponseBodyDownloader {

    private final Consumer<Boolean> savingStateListener;
    private boolean saving;

    /**
     * @param savingStateListener Quá trình lưu bắt đầu/Gọi lại khi hoàn tất (EDT），để vô hiệu hóa/Tiếp tục nút tải xuống
     */
    ResponseBodyDownloader(Consumer<Boolean> savingStateListener) {
        this.savingStateListener = savingStateListener;
    }

    boolean isSaving() {
        return saving;
    }

    /**
     * Một hộp thoại lưu sẽ bật lên. Sau khi xác nhận, nó sẽ được viết ở chế độ nền; sau khi viết thành công, nó sẽ được viết ở chế độ nền EDT gọi lại {@code onSaved}，Một thông báo lỗi bật lên khi thất bại.
     *
     * @param source          Khi được gọi (EDT）Nguồn tải xuống bị chặn và nội dung sai sẽ không được ghi ngay cả khi phản hồi được chuyển trong khi ghi
     * @param defaultFileName Lưu tên tệp mặc định của hộp thoại
     */
    void download(Source source, String defaultFileName, Consumer<File> onSaved) {
        if (saving) {
            return;
        }
        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser("response.body.save",
                I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_SAVE_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File(defaultFileName));
        int userSelection = fileChooser.showSaveDialog(UiSingletonFactory.getInstance(MainFrame.class));
        if (userSelection != SystemFileChooser.APPROVE_OPTION) {
            return;
        }
        File destFile = fileChooser.getSelectedFile();
        setSaving(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                source.writeTo(destFile);
                return null;
            }

            @Override
            protected void done() {
                setSaving(false);
                try {
                    get();
                    onSaved.accept(destFile);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    log.warn("Failed to save response body to {}", destFile, cause);
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_SAVE_FAILED,
                            cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()));
                }
            }
        }.execute();
    }

    static void notifySaved(File savedFile) {
        NotificationCenter.showSuccess(
                I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_SAVED_TO, savedFile.getAbsolutePath()));
    }

    /**
     * Tên tệp tải xuống mặc định: Ưu tiên tên tệp được phân tích cú pháp từ phản hồi (Content-Disposition / URL / nhận dạng nội dung), nếu không hãy nhấn Content-Type phát ra.
     */
    static String defaultFileName(HttpResponse response) {
        if (response != null && response.fileName != null && !response.fileName.isBlank()) {
            return response.fileName;
        }
        String extension = FileExtensionUtil.guessExtension(ResponseFiles.contentType(response));
        return FileExtensionUtil.generateSmartFileName(extension == null ? ".txt" : extension);
    }

    private void setSaving(boolean saving) {
        this.saving = saving;
        if (savingStateListener != null) {
            savingStateListener.accept(saving);
        }
    }

    /**
     * Tải xuống ảnh chụp nguồn. Ưu tiên viết: .
     * <ol>
     *     <li>Các tệp tạm thời (lớn hoặc nhị phân) được sao chép không thay đổi</li>
     *     <li>Byte phản hồi thô (phản hồi tệp, tệp đính kèm hoặc không UTF-8/Văn bản không thể được giải mã một cách dễ dàng)</li>
     *     <li>Nội dung phản hồi nhấn văn bản Content-Type Bộ ký tự được khai báo (default UTF-8）Viết, không định dạng/Hiệu ứng của nội dung soạn thảo được sắp xếp</li>
     *     <li>Dự phòng để lưu văn bản soạn thảo khi không có đối tượng phản hồi nào tồn tại</li>
     * </ol>
     */
    record Source(File file, byte[] bytes, String text) {

        static Source of(HttpResponse response, String editorText) {
            if (response != null && response.filePath != null && !response.filePath.isEmpty()) {
                return new Source(new File(response.filePath), null, null);
            }
            if (response != null && response.rawBodyBytes != null) {
                return new Source(null, response.rawBodyBytes, null);
            }
            if (response != null && response.body != null) {
                return new Source(null, ResponseFiles.encodeBody(response), null);
            }
            return new Source(null, null, editorText == null ? "" : editorText);
        }

        void writeTo(File destFile) throws IOException {
            if (file != null) {
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else if (bytes != null) {
                Files.write(destFile.toPath(), bytes);
            } else {
                Files.writeString(destFile.toPath(), text, StandardCharsets.UTF_8);
            }
        }
    }
}
