package com.laker.postman.common;

import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DebouncedSaveSupportTest {

    @Test(description = "Việc lưu được kích hoạt liên tục phải được kết hợp thành một lần thực thi")
    public void shouldCoalesceRepeatedSaveRequests() throws Exception {
        AtomicInteger saveCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        DebouncedSaveSupport support = new DebouncedSaveSupport(50, () -> {
            saveCount.incrementAndGet();
            latch.countDown();
        });

        SwingUtilities.invokeAndWait(() -> {
            support.requestSave();
            support.requestSave();
            support.requestSave();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertEquals(saveCount.get(), 1);
    }

}
