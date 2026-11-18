package com.nvhuong.devworkbench.util;

import java.util.concurrent.CompletableFuture;

public final class ClipboardUtil {

    private ClipboardUtil() {
    }

    /**
     * Kiểm tra xem clipboard có ở đó không cURL lệnh, nếu có, trả về văn bản, nếu không thì trả về null。
     */
    public static CompletableFuture<String> getClipboardCurlTextAsync() {
        return AsyncClipboardUtil.readStringAsync().thenApply(ClipboardUtil::extractCurlText);
    }

    public static String extractCurlText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (startsWithCurlCommand(trimmed)) {
            return trimmed;
        }
        return null;
    }

    private static boolean startsWithCurlCommand(String text) {
        if (text.length() < 4 || !text.regionMatches(true, 0, "curl", 0, 4)) {
            return false;
        }
        return text.length() == 4 || Character.isWhitespace(text.charAt(4));
    }
}
