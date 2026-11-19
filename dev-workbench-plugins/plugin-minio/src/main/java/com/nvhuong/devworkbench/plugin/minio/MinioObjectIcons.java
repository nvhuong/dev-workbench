package com.nvhuong.devworkbench.plugin.minio;

import com.nvhuong.devworkbench.util.IconUtil;
import lombok.experimental.UtilityClass;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Type icons for objects in the MinIO object table, chosen from the object name's extension. */
@UtilityClass
class MinioObjectIcons {
    static final String FILE = "icons/file.svg";
    static final String CODE = "icons/file-code.svg";
    static final String TEXT = "icons/object-text.svg";
    static final String IMAGE = "icons/object-image.svg";
    static final String ARCHIVE = "icons/object-archive.svg";

    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "tif", "tiff", "heic", "avif");
    private static final Set<String> ARCHIVES = Set.of("zip", "gz", "tgz", "tar", "bz2", "xz", "7z", "rar", "jar", "war", "zst");
    private static final Set<String> CODES = Set.of("json", "xml", "yaml", "yml", "js", "ts", "html", "htm", "css", "java",
            "py", "sql", "sh", "go", "kt", "properties", "toml", "ini");
    private static final Set<String> TEXTS = Set.of("txt", "md", "csv", "tsv", "log");
    /** Plugin-owned icons live in this plugin's jar; the others are shared from dev-workbench-ui. */
    private static final Set<String> PLUGIN_ICONS = Set.of(IMAGE, ARCHIVE, TEXT);
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    static String iconPath(String objectName) {
        String name = objectName == null ? "" : objectName;
        int slash = name.lastIndexOf('/');
        String fileName = name.substring(slash + 1).toLowerCase(Locale.ROOT);
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) return FILE;
        String extension = fileName.substring(dot + 1);
        if (IMAGES.contains(extension)) return IMAGE;
        if (ARCHIVES.contains(extension)) return ARCHIVE;
        if (CODES.contains(extension)) return CODE;
        if (TEXTS.contains(extension)) return TEXT;
        return FILE;
    }

    static Icon icon(String path) {
        return CACHE.computeIfAbsent(path, key -> IconUtil.createThemed(key, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL,
                PLUGIN_ICONS.contains(key) ? MinioObjectIcons.class.getClassLoader() : null));
    }

    /** Renders the "Object" column as icon + object name. */
    static final class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                label.putClientProperty("html.disable", Boolean.TRUE);
                label.setIcon(value == null ? null : icon(iconPath(String.valueOf(value))));
                label.setIconTextGap(6);
            }
            return component;
        }
    }
}
