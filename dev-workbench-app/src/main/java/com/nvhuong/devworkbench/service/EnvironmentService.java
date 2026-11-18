package com.nvhuong.devworkbench.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.util.SystemUtil;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dịch vụ quản lý biến môi trường chịu trách nhiệm về tính bền vững, tải và quản lý các biến môi trường
 */
@Slf4j
@UtilityClass
public class EnvironmentService {

    private static final Map<String, Environment> environments = Collections.synchronizedMap(new LinkedHashMap<>());

    @Getter
    private static Environment activeEnvironment = null;


    // Đường dẫn tệp dữ liệu hiện tại
    private static String currentDataFilePath;

    static {
        loadEnvironments();
    }

    /**
     * Lấy đường dẫn tệp dữ liệu hiện tại
     */
    public static String getDataFilePath() {
        if (currentDataFilePath != null) {
            return currentDataFilePath;
        }
        // Nếu không được đặt, sẽ trả về đường dẫn tệp môi trường của không gian làm việc hiện tại
        Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
        return SystemUtil.getEnvPathForWorkspace(currentWorkspace);
    }

    /**
     * Chuyển đổi đường dẫn tệp dữ liệu biến môi trường và tải lại
     */
    public static void setDataFilePath(String path) {
        if (path == null || path.isBlank()) return;
        currentDataFilePath = path;
        loadEnvironmentsFromPath(path);
    }

    /**
     * Tải các biến môi trường từ đường dẫn đã chỉ định
     */
    private static void loadEnvironmentsFromPath(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Tệp biến môi trường không tồn tại: {}, Một môi trường mặc định sẽ được tạo", filePath);
            createDefaultEnvironments();
            return;
        }

        try {
            environments.clear();
            JSONArray array = JSONUtil.readJSONArray(file, StandardCharsets.UTF_8);
            for (Object obj : array) {
                Environment env = JSONUtil.toBean((JSONObject) obj, Environment.class);
                environments.put(env.getId(), env);
                if (env.isActive()) {
                    activeEnvironment = env;
                }
            }

            if (activeEnvironment == null && !environments.isEmpty()) {
                // Nếu không có môi trường hoạt động, hãy kích hoạt môi trường đầu tiên
                Environment firstEnv = environments.values().iterator().next();
                firstEnv.setActive(true);
                activeEnvironment = firstEnv;
                saveEnvironments();
            }
        } catch (Exception e) {
            log.error("Không thể tải các biến môi trường: {}", filePath, e);
            createDefaultEnvironments();
        }
    }

    /**
     * Tải tất cả các biến môi trường
     */
    public static void loadEnvironments() {
        if (currentDataFilePath != null) {
            loadEnvironmentsFromPath(currentDataFilePath);
        } else {
            Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
            String filePath = SystemUtil.getEnvPathForWorkspace(currentWorkspace);
            loadEnvironmentsFromPath(filePath);
        }
    }

    /**
     * Tạo môi trường mặc định
     */
    private static void createDefaultEnvironments() {
        environments.clear();

        // Create Development Environment
        Environment devEnv = new Environment("Dev Env");
        devEnv.setId("dev-" + System.currentTimeMillis());
        devEnv.setActive(true);
        devEnv.addVariable("baseUrl", "https://so.gitee.com");
        devEnv.addVariable("apiKey", "dev-api-key-123");

        // Create Testing Environment
        Environment testEnv = new Environment("Test Env");
        testEnv.setId("test-" + System.currentTimeMillis());
        testEnv.addVariable("baseUrl", "https://so.csdn.net/so/search");
        testEnv.addVariable("apiKey", "test-api-key-456");

        environments.put(devEnv.getId(), devEnv);
        environments.put(testEnv.getId(), testEnv);
        activeEnvironment = devEnv;

        saveEnvironments();
    }

    /**
     * Lưu tất cả các biến môi trường
     */
    public static void saveEnvironments() {
        try {
            String filePath;
            if (currentDataFilePath != null) {
                filePath = currentDataFilePath;
            } else {
                Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
                filePath = SystemUtil.getEnvPathForWorkspace(currentWorkspace);
            }

            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<Environment> envList = new ArrayList<>(environments.values());
            String jsonStr = JSONUtil.toJsonPrettyStr(envList);

            FileUtil.writeString(jsonStr, file, StandardCharsets.UTF_8);
            log.debug("Các biến môi trường được lưu vào: {}", filePath);
        } catch (Exception e) {
            log.error("Không thể lưu các biến môi trường", e);
        }
    }

    /**
     * Thêm hoặc cập nhật môi trường
     */
    public static void saveEnvironment(Environment environment) {
        // Nếu môi trường không ID，Tự động tạo một
        if (environment.getId() == null) {
            environment.setId("env-" + IdUtil.simpleUUID());
        }
        environments.put(environment.getId(), environment);
        saveEnvironments();
    }

    public static boolean isManagedEnvironment(Environment environment) {
        if (environment == null || environment.getId() == null) {
            return false;
        }
        return environments.get(environment.getId()) == environment;
    }

    /**
     * Xóa môi trường
     */
    public static void deleteEnvironment(String id) {
        Environment env = environments.remove(id);
        if (env != null && env.isActive() && !environments.isEmpty()) {
            // Nếu môi trường hiện đang kích hoạt bị xóa thì môi trường đầu tiên sẽ được kích hoạt
            Environment firstEnv = environments.values().iterator().next();
            firstEnv.setActive(true);
            activeEnvironment = firstEnv;
        }
        // if environments is empty after removal, set activeEnvironment to null
        if (environments.isEmpty()) {
            activeEnvironment = null;
        }
        saveEnvironments();
    }

    /**
     * Nhận tất cả các môi trường
     */
    public static List<Environment> getAllEnvironments() {
        return new ArrayList<>(environments.values());
    }

    /**
     * Thiết lập môi trường kích hoạt
     */
    public static void setActiveEnvironment(String id) {
        if (activeEnvironment != null) {
            activeEnvironment.setActive(false);
        }

        Environment env = environments.get(id);
        if (env != null) {
            env.setActive(true);
            activeEnvironment = env;
            saveEnvironments();
        }
    }

    /**
     * Như đã chỉ địnhidSắp xếp lại thứ tựenvironmentsvà kiên trì
     */
    public static void saveEnvironmentOrder(List<String> idOrder) {
        synchronized (environments) {
            LinkedHashMap<String, Environment> newOrder = new LinkedHashMap<>();
            for (String id : idOrder) {
                Environment env = environments.get(id);
                if (env != null) {
                    newOrder.put(id, env);
                }
            }
            // Phần bổ sung không có ở đóidOrdermôi trường (để tránh thiếu sót)
            for (Map.Entry<String, Environment> entry : environments.entrySet()) {
                if (!newOrder.containsKey(entry.getKey())) {
                    newOrder.put(entry.getKey(), entry.getValue());
                }
            }
            environments.clear();
            environments.putAll(newOrder);
            saveEnvironments();
        }
    }
}
