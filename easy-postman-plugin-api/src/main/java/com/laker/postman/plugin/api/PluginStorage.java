package com.laker.postman.plugin.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Plug-in không gian lưu giữ riêng tư.
 * <p>
 * Các plug-in chỉ có thể sử dụng các đường dẫn tương đối để đọc và ghi các thư mục dữ liệu của riêng chúng và không được dựa trực tiếp vào máy chủ app tập tin cài đặt hoặc đường dẫn nội bộ.
 * </p>
 */
public interface PluginStorage {

    Optional<String> readString(String relativePath) throws IOException;

    void writeString(String relativePath, String content) throws IOException;

    void delete(String relativePath) throws IOException;

    Path dataDirectory();

    static PluginStorage noop() {
        return NoopPluginStorage.INSTANCE;
    }

    final class NoopPluginStorage implements PluginStorage {
        private static final NoopPluginStorage INSTANCE = new NoopPluginStorage();

        private NoopPluginStorage() {
        }

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.empty();
        }

        @Override
        public void writeString(String relativePath, String content) {
            // no-op for compatibility with tests or standalone plugin panel construction
        }

        @Override
        public void delete(String relativePath) {
            // no-op for compatibility with tests or standalone plugin panel construction
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
