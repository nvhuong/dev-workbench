package com.nvhuong.devworkbench.plugin.redis;

import org.testng.annotations.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Disconnect + connect (to another server) while the key list of the first server is still loading: the new
 * connection must load its own keys, and the old worker must neither query the new server nor fill its key list.
 */
public class RedisPanelReconnectTest {

    @Test(timeOut = 30_000)
    public void keyLoadOfPreviousConnectionDoesNotLeakIntoNewOne() throws Exception {
        FakeRedis first = new FakeRedis("old-key", true);
        FakeRedis second = new FakeRedis("new-key", false);
        RedisPanel[] panel = new RedisPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new RedisPanel();
            connectTo(panel[0], first);
            invoke(panel[0], "loadKeysAsync");
        });
        assertTrue(first.inScan.await(10, TimeUnit.SECONDS), "first scan never started");

        SwingUtilities.invokeAndWait(() -> {
            invoke(panel[0], "doDisconnect");
            connectTo(panel[0], second);
            invoke(panel[0], "loadKeysAsync");
        });
        @SuppressWarnings("unchecked")
        DefaultListModel<String> keys = (DefaultListModel<String>) getField(panel[0], "keyListModel");
        waitUntil(() -> onEdt(() -> keys.contains("new-key")), "new connection never loaded its keys");

        first.release.countDown();
        Thread.sleep(500);
        SwingUtilities.invokeAndWait(() -> { });

        assertTrue(onEdt(() -> keys.size() == 1 && "new-key".equals(keys.get(0))), "key list: " + keys);
        assertFalse(second.metadataQueries.contains("old-key"), "old worker queried the new server");
    }

    private static void connectTo(RedisPanel panel, FakeRedis client) {
        setField(panel, "jedis", client);
        invoke(panel, "updateConnectedState", boolean.class, true);
    }

    /** Fake server: SCAN returns one key (optionally after the test releases it); TYPE/TTL are recorded. */
    private static final class FakeRedis extends JedisPooled {
        final CountDownLatch inScan = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Set<String> metadataQueries = ConcurrentHashMap.newKeySet();
        private final String key;
        private final boolean blockScan;

        FakeRedis(String key, boolean blockScan) {
            super("127.0.0.1", 1); // pooled: no connection until a real command is sent
            this.key = key;
            this.blockScan = blockScan;
        }

        @Override
        public ScanResult<String> scan(String cursor, ScanParams params) {
            inScan.countDown();
            if (blockScan) {
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return new ScanResult<>(ScanParams.SCAN_POINTER_START, List.of(key));
        }

        @Override
        public String type(String k) {
            metadataQueries.add(k);
            return "string";
        }

        @Override
        public long ttl(String k) {
            metadataQueries.add(k);
            return -1;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    private static boolean onEdt(java.util.function.BooleanSupplier check) {
        boolean[] result = new boolean[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = check.getAsBoolean());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result[0];
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError(message);
            Thread.sleep(50);
        }
    }

    private static Object getField(Object target, String name) {
        try {
            Field field = RedisPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = RedisPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void invoke(Object target, String name, Object... args) {
        invoke(target, name, new Class<?>[0], args);
    }

    private static void invoke(Object target, String name, Class<?> type, Object arg) {
        invoke(target, name, new Class<?>[]{type}, new Object[]{arg});
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object[] args) {
        try {
            Method method = RedisPanel.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
