package com.nvhuong.devworkbench.util;

import com.nvhuong.devworkbench.common.constants.ConfigPathConstants;
import com.nvhuong.devworkbench.model.Workspace;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@UtilityClass
public class SystemUtil {
    /**
     * Thông tin hệ thống
     */
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String PATH_SEPARATOR = File.pathSeparator;

    /**
     * Đường dẫn thư mục dữ liệu được lưu trong bộ nhớ cache
     */
    private static String cachedDataPath = null;


    /**
     * Lấy thư mục chứa chương trình
     */
    private static String getApplicationDirectory() {
        return AppRuntimeLayout.applicationRootDirectory(SystemUtil.class).toString() + File.separator;
    }

    private static boolean isPortableMode() {
        return AppRuntimeLayout.isPortableMode(SystemUtil.class);
    }

    /**
     * Lấy thư mục gốc lưu trữ dữ liệu
     * Portable Mode: Thư mục chứa chương trình/data/
     * Chế độ bình thường: thư mục chính của người dùng/DevWorkbench/
     */
    public static String getDevWorkbenchPath() {
        if (cachedDataPath != null) {
            return cachedDataPath;
        }

        String dataPath;
        String override = System.getProperty("devWorkbench.data.dir");
        if (override != null && !override.isBlank()) {
            dataPath = Paths.get(override.trim()).toAbsolutePath().normalize().toString() + File.separator;
        } else {
            dataPath = AppRuntimeLayout.dataRootDirectory(SystemUtil.class).toString() + File.separator;
            if (isPortableMode()) {
                log.info("Chạy tiếp Portable Lược đồ, thư mục dữ liệu: {}", dataPath);
            }
        }

        // Hãy chắc chắn rằng thư mục tồn tại
        try {
            File dataDir = new File(dataPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                log.info("Tạo một thư mục dữ liệu: {}", dataPath);
            }
        } catch (Exception e) {
            log.error("Không tạo được thư mục dữ liệu: {}", dataPath, e);
        }

        cachedDataPath = dataPath;
        return dataPath;
    }

    /**
     * Chỉ nhằm mục đích thử nghiệm, xóa bộ nhớ đệm tĩnh.
     */
    public static void resetForTests() {
        cachedDataPath = null;
    }

    /**
     * Lấy số phiên bản hiện tại: Ưu tiên từ MANIFEST.MF Implementation-Version，Nếu không, hãy cố gắng đọc pom.xml
     */
    public static String getCurrentVersion() {
        String version = null;
        try {
            version = SystemUtil.class.getPackage().getImplementationVersion();
        } catch (Exception ignored) {
            // Bỏ qua ngoại lệ, có thể không có MANIFEST.MF tập tin
        }
        if (version != null && !version.isBlank()) {
            return "v" + version;
        }
        try {
            version = resolveVersionFromPom(Paths.get("pom.xml"));
            if (version != null && !version.isBlank()) {
                return "v" + version;
            }
        } catch (Exception ignored) {
        }
        return "môi trường phát triển";
    }

    private static String resolveVersionFromPom(Path pom) {
        Path current = pom;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            if (Files.exists(current)) {
                String xml = readPom(current);
                String version = readXmlTag(xml, "version");
                String resolved = resolveVersionToken(version, xml, current);
                if (resolved != null && !resolved.contains("${")) {
                    return resolved;
                }
                String revision = readXmlTag(xml, "revision");
                if (revision != null && !revision.isBlank()) {
                    return revision.trim();
                }
                String relativePath = readXmlTag(xml, "relativePath");
                if (relativePath != null && !relativePath.isBlank()) {
                    current = current.getParent().resolve(relativePath.trim()).normalize();
                    continue;
                }
            }
            current = current.getParent() == null ? null : current.getParent().getParent() == null ? null : current.getParent().getParent().resolve("pom.xml");
        }
        return null;
    }

    private static String resolveVersionToken(String version, String xml, Path currentPom) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return trimmed;
        }
        String propertyName = trimmed.substring(2, trimmed.length() - 1);
        String propertyValue = readXmlTag(xml, propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        if ("revision".equals(propertyName)) {
            Path parentPom = currentPom.getParent() == null ? null : currentPom.getParent().getParent();
            if (parentPom != null) {
                return resolveVersionFromPom(parentPom.resolve("pom.xml"));
            }
        }
        return trimmed;
    }

    private static String readPom(Path pom) {
        try {
            return Files.readString(pom);
        } catch (Exception e) {
            return "";
        }
    }

    private static String readXmlTag(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        if (start < 0) {
            return null;
        }
        int contentStart = start + openTag.length();
        int end = xml.indexOf(closeTag, contentStart);
        if (end <= contentStart) {
            return null;
        }
        return xml.substring(contentStart, end).trim();
    }

    public static String getEnvPathForWorkspace(Workspace ws) {
        return ConfigPathConstants.getEnvironmentsPath(ws);
    }

    public static String getCollectionPathForWorkspace(Workspace ws) {
        return ConfigPathConstants.getCollectionsPath(ws);
    }

    /**
     * Nhận thông tin hệ điều hành
     */
    public static String getOsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS Name:        ").append(OS_NAME).append('\n');
        sb.append("OS Arch:        ").append(OS_ARCH).append('\n');
        sb.append("OS Version:     ").append(OS_VERSION).append('\n');
        sb.append("Path Separator: ").append(PATH_SEPARATOR).append('\n');
        sb.append("File Separator: ").append(FILE_SEPARATOR).append('\n');
        sb.append("Line Separator: ").append(LINE_SEPARATOR);
        return sb.toString();
    }

}
