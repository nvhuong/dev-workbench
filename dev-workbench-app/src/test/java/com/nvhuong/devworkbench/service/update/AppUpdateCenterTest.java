package com.nvhuong.devworkbench.service.update;

import com.nvhuong.devworkbench.platform.update.UpdateCenter;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

public class AppUpdateCenterTest {

    @Test
    public void shouldExposeSharedUpdateCenter() {
        UpdateCenter first = AppUpdateCenter.get();
        UpdateCenter second = AppUpdateCenter.get();

        assertNotNull(first);
        assertSame(first, second);
    }
}
