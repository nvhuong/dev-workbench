package com.laker.postman.plugin.host;

import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginUpdateMetadataContribution;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.plugin.runtime.PluginRuntime;

import java.util.List;
import java.util.Map;

/**
 * app Một điểm vào thống nhất để truy cập lớp vào các khả năng thời gian chạy của trình cắm thêm.
 * <p>
 * UI、Bối cảnh tập lệnh và việc điều chỉnh dịch vụ máy chủ đều được đóng góp bởi các phần bổ trợ ở đây để tránh sự ghép nối trực tiếp ở mọi nơi PluginRuntime。
 * Nếu các điều chỉnh được thực hiện trong thời gian chạy sau đó thì điều này có thể được sử dụng làm điểm kết thúc duy nhất.
 * </p>
 */
public final class PluginAccess {

    private PluginAccess() {
    }

    public static Map<String, Object> createScriptApis() {
        return PluginRuntime.getRegistry().createScriptApis();
    }

    public static List<ToolboxContribution> getToolboxContributions() {
        return PluginRuntime.getRegistry().getToolboxContributions();
    }

    public static List<PluginMenuContribution> getMenuContributions() {
        return PluginRuntime.getRegistry().getMenuContributions();
    }

    public static List<StatusBarActionContribution> getStatusBarActionContributions() {
        return PluginRuntime.getRegistry().getStatusBarActionContributions();
    }

    public static List<PluginSettingsContribution> getSettingsContributions() {
        return PluginRuntime.getRegistry().getSettingsContributions();
    }

    public static List<PluginUpdateMetadataContribution> getUpdateMetadataContributions() {
        return PluginRuntime.getRegistry().getUpdateMetadataContributions();
    }

    public static List<ScriptCompletionContributor> getScriptCompletionContributors() {
        return PluginRuntime.getRegistry().getScriptCompletionContributors();
    }

    public static List<SnippetDefinition> getSnippetDefinitions() {
        return PluginRuntime.getRegistry().getSnippetDefinitions();
    }

    public static <T> T getService(Class<T> type) {
        return PluginRuntime.getRegistry().getService(type);
    }
}
