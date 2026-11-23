package com.nvhuong.devworkbench.mcp.server;

import java.util.Map;
import java.util.Objects;

/**
 * An additional tool contributed by the host (for example from a plugin), served next to the built-in workspace
 * tools. Kept independent of the plugin API so this module only depends on the MCP SDK.
 */
public record McpExtraTool(String name,
                           String title,
                           String description,
                           Map<String, Object> inputSchema,
                           boolean readOnly,
                           Handler handler) {

    public McpExtraTool {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(handler, "handler");
        title = title == null ? name : title;
        description = description == null ? "" : description;
    }

    @FunctionalInterface
    public interface Handler {
        Map<String, Object> call(Map<String, Object> arguments) throws Exception;
    }
}
