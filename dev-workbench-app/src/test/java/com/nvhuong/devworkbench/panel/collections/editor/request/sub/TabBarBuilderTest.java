package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TabBarBuilderTest {

    @Test(description = "WebSocket Vùng phản hồi sẽ hiển thị nhãn nhật ký mạng để xem các yêu cầu bắt tay")
    public void shouldIncludeNetworkLogTabForWebSocketResponses() {
        TabBarBuilder.TabConfig config = TabBarBuilder.createWebSocketTabs();

        assertEquals(config.tabNames.length, 3);
        assertFalse(java.util.Arrays.asList(config.tabNames)
                .contains(I18nUtil.getMessage(MessageKeys.TAB_TIMING)));
        assertTrue(java.util.Arrays.asList(config.tabNames)
                .contains(I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG)));
    }
}
