package com.nvhuong.devworkbench.panel.toolbox;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class JwtDecoderTest {
    @Test
    public void decodesBase64UrlHeaderAndPayloadAsFormattedJson() {
        String token = segment("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "."
                + segment("{\"sub\":\"123\",\"admin\":true}") + ".signature";

        String decoded = JwtDecoder.decode(token, "HEADER", "PAYLOAD", "Signature is not verified.");

        assertTrue(decoded.contains("\"alg\"") && decoded.contains("HS256"));
        assertTrue(decoded.contains("\"admin\"") && decoded.contains("true"));
        assertTrue(decoded.endsWith("Signature is not verified."));
        assertTrue(!decoded.contains("signature"));
    }

    @Test
    public void acceptsUnsignedJwtWithEmptySignature() {
        String token = segment("{\"alg\":\"none\"}") + "." + segment("{\"sub\":\"guest\"}") + ".";
        assertTrue(JwtDecoder.decode(token, "HEADER", "PAYLOAD", "Not verified.").contains("guest"));
    }

    @Test
    public void rejectsMalformedJwtWithoutEchoingToken() {
        IllegalArgumentException error = expectThrows(IllegalArgumentException.class,
                () -> JwtDecoder.decode("not.a.jwt.token", "HEADER", "PAYLOAD", "Not verified."));
        assertTrue(!error.getMessage().contains("not.a.jwt.token"));
    }

    private String segment(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
