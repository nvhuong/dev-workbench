package com.nvhuong.devworkbench.plugin.api;

/**
 * Lối thoát trung lập cho các plugin gửi các bản hoàn thành tập lệnh tới máy chủ.
 */
public interface ScriptCompletionSink {

    void add(ScriptCompletionItem item);

    default void basic(String inputText, String shortDescription) {
        if (inputText == null || inputText.isBlank()) {
            return;
        }
        add(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                inputText,
                inputText,
                shortDescription
        ));
    }

    default void shorthand(String inputText, String replacementText, String shortDescription) {
        if (inputText == null || inputText.isBlank()) {
            return;
        }
        add(new ScriptCompletionItem(
                ScriptCompletionKind.SHORTHAND,
                inputText,
                replacementText == null ? "" : replacementText,
                shortDescription
        ));
    }
}
