package com.laker.postman.plugin.api;

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
