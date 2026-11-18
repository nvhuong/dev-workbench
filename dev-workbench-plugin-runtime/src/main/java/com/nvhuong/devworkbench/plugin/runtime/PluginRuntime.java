package com.nvhuong.devworkbench.plugin.runtime;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Thời gian chạy plugin đơn giản: từ local plugins Plugin tải thư mục jar。
 * <p>
 * Đây là lối vào cốt lõi của hệ thống plug-in và có trách nhiệm chính 4 cá nhân:
 * 1. Quét thư mục plugin và phân tích nó descriptor
 * 2. Quyết định nên tải plugin nào (đã bật, tương thích, phiên bản trước)
 * 3. Tạo trình nạp lớp và gọi vòng đời của plugin
 * 4. Bảo trì bị vô hiệu hóa/Để được gỡ cài đặt và trạng thái liên tục khác
 * </p>
 */
@Slf4j
public final class PluginRuntime {

    private static final String RUNTIME_PROPERTIES_RESOURCE = "/META-INF/dev-workbench/runtime.properties";
    private static final String PLATFORM_VERSION_KEY = "plugin.platform.version";
    private static final PluginRegistry REGISTRY = new PluginRegistry();
    // Các phiên bản trình cắm, trình nạp lớp và thông tin tệp đã được tải thành công trong quy trình hiện tại
    private static final List<DevWorkbenchPlugin> LOADED_PLUGINS = new ArrayList<>();
    private static final List<URLClassLoader> PLUGIN_CLASSLOADERS = new ArrayList<>();
    private static final List<PluginFileInfo> LOADED_PLUGIN_FILES = new ArrayList<>();
    private static final Map<Path, String> LOAD_FAILURES = new LinkedHashMap<>();
    @Getter
    private static volatile boolean initialized = false;
    private static volatile String cachedAppVersion;
    private static volatile String cachedPlatformVersion;

    private PluginRuntime() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (PluginRuntime.class) {
            if (initialized) {
                return;
            }
            Path managedPluginDir = getManagedPluginDir();
            Path pluginPackageDir = getPluginPackageDir();
            Set<Path> pluginDirs = PluginScanner.resolvePluginDirs(managedPluginDir);
            log.info("Plugin runtime initializing. managedPluginDir={}, pluginPackageDir={}, scanDirs={}",
                    managedPluginDir, pluginPackageDir, pluginDirs);

            // Khi bắt đầu, các mục còn lại sau lần thoát cuối cùng sẽ được xử lý trước“Sẽ được gỡ cài đặt”Các tệp trình cắm, .
            // Bằng cách này, kết quả quét tiếp theo sẽ ở trạng thái hiện tại sạch sẽ.
            LOAD_FAILURES.clear();
            cleanupPendingUninstallPlugins();

            // Làm điều đó ở đây đầu tiên“Chọn plugin”Thay vì tải trực tiếp bất cứ thứ gì bạn quét.
            // Việc vô hiệu hóa, chờ gỡ cài đặt, tính tương thích và đồng bộ hóa sẽ được xử lý thống nhất trong thời gian chạy id Chọn tốt nhất trong số nhiều phiên bản.
            List<PluginFileInfo> loadCandidates = resolveLoadCandidates(pluginDirs);
            if (loadCandidates.isEmpty()) {
                log.info("No plugin candidates selected for loading.");
            } else {
                log.info("Plugin candidates selected for loading ({}): {}",
                        loadCandidates.size(),
                        formatPluginList(loadCandidates));
            }

            for (PluginFileInfo pluginFile : loadCandidates) {
                PluginLoader.loadPluginJar(
                        pluginFile.jarPath(),
                        pluginFile.descriptor(),
                        REGISTRY,
                        LOADED_PLUGINS,
                        PLUGIN_CLASSLOADERS,
                        LOADED_PLUGIN_FILES,
                        PluginRuntime.class.getClassLoader()
                ).ifPresent(failureMessage ->
                        LOAD_FAILURES.put(normalizePath(pluginFile.jarPath()), failureMessage));
            }
            // onLoad Chịu trách nhiệm đăng ký điểm mở rộng.onStart có nghĩa là“Tất cả các plug-in đã được đăng ký và có thể bắt đầu chạy”。
            PluginLoader.startPlugins(LOADED_PLUGINS);
            if (LOADED_PLUGIN_FILES.isEmpty()) {
                log.info("Plugin runtime initialized with 0 loaded plugins.");
            } else {
                log.info("Plugin runtime initialized. loadedPlugins={}", formatPluginList(LOADED_PLUGIN_FILES));
            }
            initialized = true;
        }
    }

    public static PluginRegistry getRegistry() {
        return REGISTRY;
    }

    public static List<PluginFileInfo> getInstalledPlugins() {
        List<PluginFileInfo> installed = new ArrayList<>();
        Set<String> disabledPluginIds = PluginStateStore.getDisabledPluginIds();
        Set<String> pendingUninstallPluginIds = PluginStateStore.getPendingUninstallPluginIds();
        for (Path pluginDir : PluginScanner.resolvePluginDirs(getManagedPluginDir())) {
            installed.addAll(PluginScanner.listPluginsFromDirectory(
                    pluginDir,
                    disabledPluginIds,
                    pendingUninstallPluginIds,
                    PluginRuntime::isLoaded
            ));
        }
        return applyLoadFailures(installed);
    }

    public static String getCurrentAppVersion() {
        String version = cachedAppVersion;
        if (version != null) {
            return version;
        }
        synchronized (PluginRuntime.class) {
            if (cachedAppVersion != null) {
                return cachedAppVersion;
            }
            cachedAppVersion = RuntimeVersionResolver.resolveCurrentAppVersion(PluginRuntime.class);
            return cachedAppVersion;
        }
    }

    public static String getCurrentPluginPlatformVersion() {
        String version = cachedPlatformVersion;
        if (version != null) {
            return version;
        }
        synchronized (PluginRuntime.class) {
            if (cachedPlatformVersion != null) {
                return cachedPlatformVersion;
            }
            cachedPlatformVersion = RuntimeVersionResolver.resolveCurrentPlatformVersion(
                    PluginRuntime.class,
                    RUNTIME_PROPERTIES_RESOURCE,
                    PLATFORM_VERSION_KEY
            );
            return cachedPlatformVersion;
        }
    }

    public static Path getManagedPluginDir() {
        return PluginRuntimePaths.managedPluginDir();
    }

    public static Path getPluginPackageDir() {
        return PluginRuntimePaths.pluginPackageDir();
    }

    public static PluginDescriptor inspectPluginJar(Path jarPath) throws IOException {
        return PluginScanner.readDescriptor(jarPath);
    }

    public static PluginCompatibility evaluateCompatibility(PluginDescriptor descriptor) {
        if (descriptor == null) {
            return new PluginCompatibility(false, false, false,
                    getCurrentAppVersion(), getCurrentPluginPlatformVersion(),
                    "", "", "", "");
        }
        return evaluateCompatibility(
                descriptor.minAppVersion(),
                descriptor.maxAppVersion(),
                descriptor.minPlatformVersion(),
                descriptor.maxPlatformVersion()
        );
    }

    public static PluginCompatibility evaluateCompatibility(String minAppVersion,
                                                            String maxAppVersion,
                                                            String minPlatformVersion,
                                                            String maxPlatformVersion) {
        return evaluateCompatibility(
                new RuntimeVersionInfo(getCurrentAppVersion(), getCurrentPluginPlatformVersion()),
                minAppVersion,
                maxAppVersion,
                minPlatformVersion,
                maxPlatformVersion
        );
    }

    private static PluginCompatibility evaluateCompatibility(RuntimeVersionInfo runtimeVersionInfo,
                                                             String minAppVersion,
                                                             String maxAppVersion,
                                                             String minPlatformVersion,
                                                             String maxPlatformVersion) {
        String currentAppVersion = runtimeVersionInfo.currentAppVersion();
        String currentPlatformVersion = runtimeVersionInfo.currentPlatformVersion();

        // app Phiên bản được xác định“Liệu phân phối máy chủ này có cho phép cài đặt hay không”，platform Phiên bản được xác định“Bộ plug-in này SPI Cho dù nó vẫn có thể được lắp ráp”。
        boolean appVersionCompatible = isVersionInRange(currentAppVersion, minAppVersion, maxAppVersion);
        boolean platformVersionCompatible = isVersionInRange(currentPlatformVersion, minPlatformVersion, maxPlatformVersion);
        return new PluginCompatibility(
                appVersionCompatible && platformVersionCompatible,
                appVersionCompatible,
                platformVersionCompatible,
                currentAppVersion,
                currentPlatformVersion,
                minAppVersion == null ? "" : minAppVersion,
                maxAppVersion == null ? "" : maxAppVersion,
                minPlatformVersion == null ? "" : minPlatformVersion,
                maxPlatformVersion == null ? "" : maxPlatformVersion
        );
    }

    public static void shutdown() {
        synchronized (PluginRuntime.class) {
            try {
                PluginLoader.stopPlugins(LOADED_PLUGINS);
                PluginLoader.closeClassLoaders(PLUGIN_CLASSLOADERS);
                // shutdown Dọn dẹp từng giai đoạn các plugin sẽ được gỡ cài đặt để ngăn chúng được tải jar Xóa không thành công trong thời gian chạy
                cleanupPendingUninstallPlugins();
            } finally {
                // Xóa hoàn toàn trạng thái trong quá trình để tránh IDE Bộ đệm bẩn xuất hiện khi khởi động lại
                LOADED_PLUGINS.clear();
                PLUGIN_CLASSLOADERS.clear();
                LOADED_PLUGIN_FILES.clear();
                LOAD_FAILURES.clear();
                REGISTRY.clear();
                initialized = false;
                cachedAppVersion = null;
                cachedPlatformVersion = null;
            }
        }
    }

    public static boolean isPluginEnabled(String pluginId) {
        return PluginStateStore.isPluginEnabled(pluginId);
    }

    public static void setPluginEnabled(String pluginId, boolean enabled) {
        PluginStateStore.setPluginEnabled(pluginId, enabled);
    }

    public static boolean isPluginPendingUninstall(String pluginId) {
        return PluginStateStore.isPluginPendingUninstall(pluginId);
    }

    public static void markPluginPendingUninstall(String pluginId) {
        PluginStateStore.markPluginPendingUninstall(pluginId);
    }

    public static void clearPendingUninstall(String pluginId) {
        PluginStateStore.clearPendingUninstall(pluginId);
    }

    public static List<PluginFileInfo> getManagedPluginFiles() {
        return applyLoadFailures(PluginScanner.listPluginsFromDirectory(
                getManagedPluginDir(),
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        ));
    }

    public static List<PluginFileInfo> getPluginPackageFiles() {
        return applyLoadFailures(PluginScanner.listPluginsFromDirectory(
                getPluginPackageDir(),
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        ));
    }

    private static List<PluginFileInfo> resolveLoadCandidates(Set<Path> pluginDirs) {
        return PluginCandidateResolver.resolveLoadCandidates(
                pluginDirs,
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        );
    }

    private static String formatPluginList(List<PluginFileInfo> pluginFiles) {
        List<String> parts = new ArrayList<>(pluginFiles.size());
        for (PluginFileInfo pluginFile : pluginFiles) {
            PluginDescriptor descriptor = pluginFile.descriptor();
            if (descriptor == null) {
                parts.add(String.valueOf(pluginFile.jarPath()));
                continue;
            }
            parts.add(descriptor.id() + ":" + descriptor.version() + "@" + pluginFile.jarPath());
        }
        return parts.toString();
    }

    private static boolean isLoaded(Path jarPath) {
        return LOADED_PLUGIN_FILES.stream().anyMatch(info -> info.jarPath().equals(jarPath));
    }

    private static List<PluginFileInfo> applyLoadFailures(List<PluginFileInfo> pluginFiles) {
        if (pluginFiles == null || pluginFiles.isEmpty() || LOAD_FAILURES.isEmpty()) {
            return pluginFiles;
        }
        List<PluginFileInfo> result = new ArrayList<>(pluginFiles.size());
        for (PluginFileInfo info : pluginFiles) {
            String failureMessage = LOAD_FAILURES.get(normalizePath(info.jarPath()));
            if (failureMessage == null || failureMessage.isBlank()) {
                result.add(info);
                continue;
            }
            result.add(new PluginFileInfo(
                    info.descriptor(),
                    info.jarPath(),
                    info.loaded(),
                    info.enabled(),
                    false,
                    failureMessage
            ));
        }
        return result;
    }

    private static Path normalizePath(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    static void cleanupPendingUninstallPlugins() {
        Set<String> pendingIds = new LinkedHashSet<>(PluginStateStore.getPendingUninstallPluginIds());
        if (pendingIds.isEmpty()) {
            return;
        }
        List<PluginFileInfo> managedPluginFiles = getManagedPluginFiles();

        // Chỉ làm sạch bản sao trong thư mục cài đặt được quản lý, không chạm vào nó packages gói plug-in cục bộ
        for (PluginFileInfo info : managedPluginFiles) {
            if (!pendingIds.contains(info.descriptor().id())) {
                continue;
            }
            try {
                Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete pending uninstall plugin file: {}", info.jarPath(), e);
            }
        }

        Set<String> remainingPending = new LinkedHashSet<>();
        for (PluginFileInfo info : getManagedPluginFiles()) {
            if (pendingIds.contains(info.descriptor().id())) {
                remainingPending.add(info.descriptor().id());
            }
        }
        PluginStateStore.replacePendingUninstallPluginIds(remainingPending);
        pendingIds.stream()
                .filter(pluginId -> !remainingPending.contains(pluginId))
                .forEach(pluginId -> PluginStateStore.setPluginEnabled(pluginId, true));
        if (!remainingPending.isEmpty()) {
            log.info("Pending uninstall plugin(s) still remain after cleanup: {}", remainingPending);
        }
    }

    public static void resetForTests() {
        synchronized (PluginRuntime.class) {
            LOADED_PLUGINS.clear();
            PLUGIN_CLASSLOADERS.clear();
            LOADED_PLUGIN_FILES.clear();
            LOAD_FAILURES.clear();
            REGISTRY.clear();
            initialized = false;
            cachedAppVersion = null;
            cachedPlatformVersion = null;
            PluginRuntimePaths.resetForTests();
        }
    }

    private static boolean isVersionInRange(String currentVersion, String minVersion, String maxVersion) {
        if (minVersion != null && !minVersion.isBlank()
                && PluginVersionComparator.compare(currentVersion, minVersion) < 0) {
            return false;
        }
        return maxVersion == null || maxVersion.isBlank()
                || PluginVersionComparator.compare(currentVersion, maxVersion) <= 0;
    }

    private record RuntimeVersionInfo(String currentAppVersion, String currentPlatformVersion) {
    }
}
