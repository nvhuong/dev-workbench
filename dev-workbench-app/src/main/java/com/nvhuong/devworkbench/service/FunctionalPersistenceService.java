package com.nvhuong.devworkbench.service;

import com.nvhuong.devworkbench.functional.model.FunctionalConfigRow;
import com.nvhuong.devworkbench.functional.model.FunctionalConfigSnapshot;
import com.nvhuong.devworkbench.model.Workspace;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.common.constants.ConfigPathConstants;
import com.nvhuong.devworkbench.functional.model.FunctionalCsvDataState;
import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.ioc.PostConstruct;
import com.nvhuong.devworkbench.ioc.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Kiểm tra chức năng cấu hình dịch vụ kiên trì
 * Được sử dụng để lưu và tải cấu hình yêu cầu trong bảng kiểm tra chức năng
 */
@Slf4j
@Component
public class FunctionalPersistenceService {
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024; // 2MB
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "functional-config-save");
        thread.setDaemon(true);
        return thread;
    });

    public FunctionalPersistenceService() {
    }

    @PostConstruct
    public void init() {
        ensureDirExists();
    }

    private void ensureDirExists() {
        ensureDirExists(getConfigFilePath());
    }

    private void ensureDirExists(Path configPath) {
        try {
            Path configDir = configPath.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            log.error("Failed to create config directory: {}", e.getMessage());
        }
    }

    /**
     * Lưu cấu hình kiểm tra chức năng
     * Chỉ có yêu cầu được lưuIDTham chiếu, không lưu cấu hình đầy đủ, đảm bảo đồng bộ hóa với các yêu cầu trong bộ sưu tập
     */
    public void save(FunctionalConfigSnapshot snapshot) {
        Path configPath = getConfigFilePath();
        writeAndWait(() -> writeSnapshot(configPath, snapshot));
    }

    private void writeSnapshot(Path configPath, FunctionalConfigSnapshot snapshot) {
        try {
            ensureDirExists(configPath);
            FunctionalConfigSnapshot safeSnapshot = snapshot == null ? FunctionalConfigSnapshot.empty() : snapshot;
            JSONObject root = new JSONObject();
            root.set("version", "1.0");
            root.set("rows", serializeRows(safeSnapshot.getRows()));
            if (safeSnapshot.getCsvState() != null) {
                root.set("csvState", serializeCsvState(safeSnapshot.getCsvState()));
            }

            // Viết vào tập tin
            String jsonString = JSONUtil.toJsonPrettyStr(root);
            Files.writeString(configPath, jsonString, StandardCharsets.UTF_8);

            log.info("Successfully saved {} functional test configurations", safeSnapshot.getRows().size());
        } catch (IOException e) {
            log.error("Failed to save functional test config: {}", e.getMessage(), e);
        }
    }

    /**
     * Lưu cấu hình không đồng bộ
     */
    public void saveAsync(FunctionalConfigSnapshot snapshot) {
        // Đã sửa đường dẫn trước khi gửi tác vụ lưu để ngăn người dùng chuyển đổi workspace Sau đó ghi dữ liệu cũ vào dữ liệu mới workspace。
        Path configPath = getConfigFilePath();
        saveExecutor.execute(() -> writeSnapshot(configPath, snapshot));
    }

    /**
     * Tải cấu hình kiểm tra chức năng
     * vượt quaIDNhận cấu hình yêu cầu mới nhất từ bộ sưu tập, đảm bảo luôn đồng bộ hóa với bộ sưu tập
     */
    public FunctionalConfigSnapshot loadSnapshot() {
        List<FunctionalConfigRow> rows = new ArrayList<>();
        Path configPath = getConfigFilePath();
        File file = configPath.toFile();

        if (!file.exists()) {
            log.info("No functional test config file found, starting fresh");
            return FunctionalConfigSnapshot.empty();
        }

        try {
            // Kiểm tra kích thước tập tin
            long fileSizeInBytes = file.length();
            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("Config file is too large ({} bytes), deleting and starting fresh", fileSizeInBytes);
                deleteFile(file);
                return FunctionalConfigSnapshot.empty();
            }

            if (fileSizeInBytes == 0) {
                return FunctionalConfigSnapshot.empty();
            }

            // Đọc tập tin
            String jsonString = Files.readString(configPath, StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return FunctionalConfigSnapshot.empty();
            }

            JSONObject root = JSONUtil.parseObj(jsonString);
            JSONArray jsonArray = root.getJSONArray("rows");
            rows.addAll(deserializeRows(jsonArray));

            log.info("Successfully loaded {} functional test configurations", rows.size());
            return new FunctionalConfigSnapshot(rows, deserializeCsvState(root.getJSONObject("csvState")));

        } catch (Exception e) {
            log.error("Failed to load functional test config: {}", e.getMessage(), e);
            deleteFile(file);
        }

        return FunctionalConfigSnapshot.empty();
    }

    /**
     * Cấu hình rõ ràng
     */
    public void clear() {
        Path configPath = getConfigFilePath();
        writeAndWait(() -> deleteFile(configPath.toFile()));
    }

    private void writeAndWait(Runnable writeAction) {
        // Lần lưu và xóa cuối cùng cũng được lên lịch sau lần lưu tự động cũ để ngăn các ảnh chụp nhanh cũ ghi đè lên cấu hình mới nhất.
        Future<?> write = saveExecutor.submit(writeAction);
        try {
            write.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving functional config", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to save functional config", e.getCause());
        }
    }

    /**
     * Xóa tập tin cấu hình
     */
    private void deleteFile(File file) {
        try {
            if (file.exists() && !file.delete()) {
                log.warn("Failed to delete config file: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error deleting config file: {}", e.getMessage());
        }
    }

    private JSONArray serializeRows(List<FunctionalConfigRow> rows) {
        JSONArray jsonArray = new JSONArray();
        if (rows == null) {
            return jsonArray;
        }
        for (FunctionalConfigRow row : rows) {
            if (row == null || row.getRequestId() == null || row.getRequestId().isBlank()) {
                continue;
            }

            JSONObject jsonItem = new JSONObject();
            jsonItem.set("selected", row.isSelected());
            jsonItem.set("requestItemId", row.getRequestId());
            jsonArray.add(jsonItem);
        }
        return jsonArray;
    }

    private List<FunctionalConfigRow> deserializeRows(JSONArray jsonArray) {
        List<FunctionalConfigRow> rows = new ArrayList<>();
        if (jsonArray == null) {
            return rows;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JSONObject jsonItem = jsonArray.getJSONObject(i);
                String requestItemId = jsonItem.getStr("requestItemId");
                boolean selected = jsonItem.getBool("selected", true);
                if (requestItemId == null || requestItemId.isBlank()) {
                    continue;
                }
                rows.add(new FunctionalConfigRow(requestItemId, selected));
            } catch (Exception e) {
                log.warn("Failed to restore config item at index {}: {}", i, e.getMessage());
            }
        }
        return rows;
    }

    private JSONObject serializeCsvState(FunctionalCsvDataState csvState) {
        JSONObject json = new JSONObject();
        json.set("sourceName", csvState.getSourceName());

        JSONArray headers = new JSONArray();
        for (String header : csvState.getHeaders()) {
            headers.add(header);
        }
        json.set("headers", headers);

        JSONArray rows = new JSONArray();
        for (Map<String, String> row : csvState.getRows()) {
            JSONObject rowJson = new JSONObject();
            if (row != null) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    rowJson.set(entry.getKey(), entry.getValue());
                }
            }
            rows.add(rowJson);
        }
        json.set("rows", rows);
        return json;
    }

    private FunctionalCsvDataState deserializeCsvState(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONArray headersJson = json.getJSONArray("headers");
        JSONArray rowsJson = json.getJSONArray("rows");
        if (headersJson == null || rowsJson == null || rowsJson.isEmpty()) {
            return null;
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headersJson.size(); i++) {
            headers.add(headersJson.getStr(i));
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < rowsJson.size(); i++) {
            JSONObject rowJson = rowsJson.getJSONObject(i);
            Map<String, String> row = new LinkedHashMap<>();
            if (rowJson != null) {
                for (String header : headers) {
                    row.put(header, rowJson.getStr(header, ""));
                }
            }
            rows.add(row);
        }

        return new FunctionalCsvDataState(json.getStr("sourceName"), headers, rows);
    }

    protected Path getConfigFilePath() {
        Workspace workspace = getCurrentWorkspace();
        return Paths.get(ConfigPathConstants.getFunctionalConfigPath(workspace));
    }

    protected Workspace getCurrentWorkspace() {
        try {
            return WorkspaceService.getInstance().getCurrentWorkspace();
        } catch (Exception e) {
            log.debug("Failed to resolve current workspace for functional config path", e);
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
