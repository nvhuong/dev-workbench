package com.nvhuong.devworkbench.plugin.api;

/**
 * Nối các mục hoàn thành vào trình chỉnh sửa tập lệnh.
 */
@FunctionalInterface
public interface ScriptCompletionContributor {

    void contribute(ScriptCompletionSink sink);
}
