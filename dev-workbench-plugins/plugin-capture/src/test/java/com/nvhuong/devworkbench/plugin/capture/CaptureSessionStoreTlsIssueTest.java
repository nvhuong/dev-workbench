package com.nvhuong.devworkbench.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureSessionStoreTlsIssueTest {

    /** TLS issue de-duplication kept one entry per host:port for the whole session (unbounded in long captures). */
    @Test
    public void tlsIssueSuppressionMemoryIsBounded() {
        CaptureSessionStore store = new CaptureSessionStore();
        for (int i = 0; i < 20_000; i++) {
            store.recordTlsIssue("host-" + i + ".example.com", 443, "handshake failed");
        }
        assertTrue(store.trackedTlsIssueCount() <= CaptureSessionStore.MAX_TRACKED_TLS_ISSUES,
                "tracked " + store.trackedTlsIssueCount());

        // De-duplication still works for recently seen hosts.
        assertTrue(store.recordTlsIssue("repeat.example.com", 443, "first"));
        assertFalse(store.recordTlsIssue("REPEAT.example.com", 443, "again within the window"));
    }
}
