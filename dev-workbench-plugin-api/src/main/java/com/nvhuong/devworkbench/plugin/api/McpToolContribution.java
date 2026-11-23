package com.nvhuong.devworkbench.plugin.api;

import java.util.Map;
import java.util.Objects;

/**
 * A tool a plugin exposes through the DevWorkbench MCP server ({@code mcp serve}), so AI clients can use the plugin's
 * capability headlessly.
 *
 * @param name        unique snake_case tool name, prefixed with the plugin area (e.g. {@code sql_query})
 * @param title       short human readable title
 * @param description what the tool does and how to use its arguments
 * @param inputSchema JSON Schema (draft 2020-12) of the arguments object
 * @param readOnly    {@code true} when the tool never changes external state; write tools are only exposed when the
 *                    MCP server runs with {@code --allow-writes}
 * @param handler     runs the tool; the returned map is serialized as the tool result
 */
public record McpToolContribution(String name,
                                  String title,
                                  String description,
                                  Map<String, Object> inputSchema,
                                  boolean readOnly,
                                  Handler handler) {

    public McpToolContribution {
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
