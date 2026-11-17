package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginUpdateMetadataContribution;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Sổ đăng ký mở rộng thời gian chạy.
 * <p>
 * Có thể hiểu là“Bảng tóm tắt các khả năng sau khi tải plug-in”：
 * plugin onLoad Đăng ký nó khi máy chủ đang chạy và máy chủ sẽ đọc nó từ đây khi nó đang chạy.
 * </p>
 */
@Slf4j
public class PluginRegistry {

    // alias -> Nhà máy, dành cho sự sáng tạo lười biếng pm.xxx loại kịch bản API đối tượng
    private final Map<String, ScriptApiRegistration> scriptApiFactories = new ConcurrentHashMap<>();
    // type -> service，Để lớp cầu nối máy chủ nhận được các dịch vụ theo loại
    private final Map<Class<?>, ServiceRegistration> services = new ConcurrentHashMap<>();
    // Toolbox / Tự động hoàn thành / Snippet Tất cả UI Điểm có thể mở rộng bên
    private final List<ToolboxContribution> toolboxContributions = new CopyOnWriteArrayList<>();
    private final List<PluginMenuContribution> menuContributions = new CopyOnWriteArrayList<>();
    private final List<PluginSettingsContribution> settingsContributions = new CopyOnWriteArrayList<>();
    private final List<PluginUpdateMetadataContribution> updateMetadataContributions = new CopyOnWriteArrayList<>();
    private final List<StatusBarActionContribution> statusBarActionContributions = new CopyOnWriteArrayList<>();
    private final List<ScriptCompletionContributor> scriptCompletionContributors = new CopyOnWriteArrayList<>();
    private final List<SnippetDefinition> snippetDefinitions = new CopyOnWriteArrayList<>();

    public void registerScriptApi(String alias, Supplier<Object> factory) {
        registerScriptApi(null, alias, factory);
    }

    void registerScriptApi(String pluginId, String alias, Supplier<Object> factory) {
        if (alias == null || alias.isBlank() || factory == null) {
            return;
        }
        ScriptApiRegistration previous = scriptApiFactories.put(alias, new ScriptApiRegistration(pluginId, factory));
        if (previous != null && previous.factory() != factory) {
            log.warn("Script API alias '{}' is already registered by plugin '{}'. Latest registration from plugin '{}' will replace it.",
                    alias, owner(previous.pluginId()), owner(pluginId));
        }
    }

    public Map<String, Object> createScriptApis() {
        Map<String, Object> apis = new LinkedHashMap<>();
        for (Map.Entry<String, ScriptApiRegistration> entry : scriptApiFactories.entrySet()) {
            try {
                // Quá trình khởi tạo này bị trì hoãn thay vì ngay lập tức khi plugin được tải new。
                // Ưu điểm là môi trường tập lệnh được tạo khi thực sự cần thiết, tải plug-in nhẹ hơn và bề mặt lỗi trong giai đoạn khởi động được giảm bớt.
                // Một plug-in nhất định API Khi quá trình khởi tạo không thành công, chỉ phần bổ trợ hiện tại bị bỏ qua để tránh làm hỏng toàn bộ môi trường tập lệnh
                Object api = entry.getValue().factory().get();
                if (api != null) {
                    apis.put(entry.getKey(), api);
                }
            } catch (Throwable t) {
                log.error("Failed to create script API for plugin alias: {}", entry.getKey(), t);
            }
        }
        return apis;
    }

    public <T> void registerService(Class<T> type, T service) {
        registerService(null, type, service);
    }

    <T> void registerService(String pluginId, Class<T> type, T service) {
        if (type == null || service == null) {
            return;
        }
        ServiceRegistration previous = services.put(type, new ServiceRegistration(pluginId, service));
        if (previous != null && previous.service() != service) {
            log.warn("Plugin service '{}' is already registered by plugin '{}'. Latest registration from plugin '{}' will replace it.",
                    type.getName(), owner(previous.pluginId()), owner(pluginId));
        }
    }

    public <T> T getService(Class<T> type) {
        ServiceRegistration registration = services.get(type);
        if (registration == null) {
            return null;
        }
        return type.cast(registration.service());
    }

    String getScriptApiOwner(String alias) {
        ScriptApiRegistration registration = scriptApiFactories.get(alias);
        return registration == null ? null : registration.pluginId();
    }

    String getServiceOwner(Class<?> type) {
        ServiceRegistration registration = services.get(type);
        return registration == null ? null : registration.pluginId();
    }

    public void registerToolboxContribution(ToolboxContribution contribution) {
        if (contribution != null) {
            toolboxContributions.add(contribution);
        }
    }

    public List<ToolboxContribution> getToolboxContributions() {
        return new ArrayList<>(toolboxContributions);
    }

    public void registerMenuContribution(PluginMenuContribution contribution) {
        registerMenuContribution(null, contribution);
    }

    void registerMenuContribution(String pluginId, PluginMenuContribution contribution) {
        if (contribution != null) {
            menuContributions.add(contribution);
        }
    }

    public List<PluginMenuContribution> getMenuContributions() {
        return new ArrayList<>(menuContributions);
    }

    public void registerStatusBarActionContribution(StatusBarActionContribution contribution) {
        registerStatusBarActionContribution(null, contribution);
    }

    void registerStatusBarActionContribution(String pluginId, StatusBarActionContribution contribution) {
        if (contribution != null) {
            statusBarActionContributions.add(contribution);
        }
    }

    public List<StatusBarActionContribution> getStatusBarActionContributions() {
        return new ArrayList<>(statusBarActionContributions);
    }

    public void registerSettingsContribution(PluginSettingsContribution contribution) {
        registerSettingsContribution(null, contribution);
    }

    void registerSettingsContribution(String pluginId, PluginSettingsContribution contribution) {
        if (contribution != null) {
            settingsContributions.add(contribution);
        }
    }

    public List<PluginSettingsContribution> getSettingsContributions() {
        return new ArrayList<>(settingsContributions);
    }

    public void registerUpdateMetadataContribution(PluginUpdateMetadataContribution contribution) {
        registerUpdateMetadataContribution(null, contribution);
    }

    void registerUpdateMetadataContribution(String pluginId, PluginUpdateMetadataContribution contribution) {
        if (contribution != null) {
            updateMetadataContributions.add(contribution);
        }
    }

    public List<PluginUpdateMetadataContribution> getUpdateMetadataContributions() {
        return new ArrayList<>(updateMetadataContributions);
    }

    public void registerScriptCompletionContributor(ScriptCompletionContributor contributor) {
        if (contributor != null) {
            scriptCompletionContributors.add(contributor);
        }
    }

    public List<ScriptCompletionContributor> getScriptCompletionContributors() {
        return new ArrayList<>(scriptCompletionContributors);
    }

    public void registerSnippet(SnippetDefinition definition) {
        if (definition != null) {
            snippetDefinitions.add(definition);
        }
    }

    public List<SnippetDefinition> getSnippetDefinitions() {
        return new ArrayList<>(snippetDefinitions);
    }

    public void clear() {
        // Bị xóa khi ứng dụng thoát hoặc được đặt lại trong thời gian chạy để tránh giữ lại trạng thái plug-in cũ trong các phiên
        scriptApiFactories.clear();
        services.clear();
        toolboxContributions.clear();
        menuContributions.clear();
        settingsContributions.clear();
        updateMetadataContributions.clear();
        statusBarActionContributions.clear();
        scriptCompletionContributors.clear();
        snippetDefinitions.clear();
    }

    private static String owner(String pluginId) {
        return pluginId == null || pluginId.isBlank() ? "<unknown>" : pluginId;
    }

    private record ScriptApiRegistration(String pluginId, Supplier<Object> factory) {
    }

    private record ServiceRegistration(String pluginId, Object service) {
    }
}
