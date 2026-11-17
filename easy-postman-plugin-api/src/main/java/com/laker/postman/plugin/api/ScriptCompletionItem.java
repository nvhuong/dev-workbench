package com.laker.postman.plugin.api;

/**
 * Các mục hoàn thành tập lệnh không bị ràng buộc với việc triển khai trình chỉnh sửa cụ thể.
 */
public record ScriptCompletionItem(
        ScriptCompletionKind kind,
        String inputText,
        String replacementText,
        String shortDescription
) {
}
