package com.nvhuong.devworkbench.plugin.api;

/**
 * Định nghĩa đoạn mã.
 */
public record SnippetDefinition(
        String categoryId,
        String title,
        String description,
        String code
) {
}
