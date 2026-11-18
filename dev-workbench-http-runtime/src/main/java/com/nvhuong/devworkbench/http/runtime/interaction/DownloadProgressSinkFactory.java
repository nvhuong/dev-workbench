package com.nvhuong.devworkbench.http.runtime.interaction;

@FunctionalInterface
public interface DownloadProgressSinkFactory {
    DownloadProgressSinkFactory NOOP = () -> DownloadProgressSink.NOOP;

    DownloadProgressSink create();

    static DownloadProgressSinkFactory noop() {
        return NOOP;
    }
}
