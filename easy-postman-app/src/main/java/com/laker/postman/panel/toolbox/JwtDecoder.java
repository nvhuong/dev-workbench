package com.laker.postman.panel.toolbox;

import com.laker.postman.util.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Decodes JWT header and payload without validating the signature. */
final class JwtDecoder {
    private JwtDecoder() {
    }

    static String decode(String token, String headerLabel, String payloadLabel, String signatureNote) {
        String[] parts = token.trim().split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("JWT must contain a Base64URL header, payload, and signature segment.");
        }
        return headerLabel + "\n" + pretty(decodeSegment(parts[0]))
                + "\n\n" + payloadLabel + "\n" + pretty(decodeSegment(parts[1]))
                + "\n\n" + signatureNote;
    }

    private static String decodeSegment(String segment) {
        try {
            return new String(Base64.getUrlDecoder().decode(segment), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT contains an invalid Base64URL segment.");
        }
    }

    private static String pretty(String value) {
        try {
            return JsonUtil.toJsonPrettyStr(value);
        } catch (Exception ignored) {
            return value;
        }
    }
}
