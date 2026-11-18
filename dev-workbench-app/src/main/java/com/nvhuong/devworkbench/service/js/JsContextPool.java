package com.nvhuong.devworkbench.service.js;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JS Context Nhóm đối tượng
 * <p>
 * thông qua việc tái sử dụng GraalVM Context Các đối tượng để cải thiện hiệu suất và tránh việc tạo và phá hủy thường xuyên Context。
 * sử dụng IIFE (Immediately Invoked Function Expression) Gói các tập lệnh người dùng để đạt được sự cô lập có thể thay đổi.
 * tránh let/const Các biến gây ô nhiễm phạm vi toàn cầu, tạo ra Context Có thể được tái sử dụng một cách an toàn.
 * </p>
 *
 * <h3>Nguyên tắc:</h3>
 * <pre>
 * Mô tả người dùng: .
 *   let env = "test";
 *   console.log(env);
 *
 * Tự động đóng gói dưới dạng: .
 *   (function() {
 *     let env = "test";  // Các biến cục bộ không gây ô nhiễm phạm vi toàn cầu
 *     console.log(env);
 *   })();
 * </pre>
 *
 * @author nvhuong
 */
@Slf4j
public class JsContextPool {

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private final BlockingQueue<PooledContext> pool;
    private final int maxSize;
    private final AtomicInteger currentSize = new AtomicInteger(0);
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger totalReused = new AtomicInteger(0);
    private final AtomicInteger waitingBorrowCount = new AtomicInteger(0);
    private volatile boolean retired = false;
    private volatile boolean closed = false;

    /**
     * Đóng gói Context Đối tượng, với số liệu thống kê
     */
    public static class PooledContext {
        final Context context;
        final long createdTime;
        final AtomicInteger useCount = new AtomicInteger(0);

        PooledContext(Context context) {
            this.context = context;
            this.createdTime = System.currentTimeMillis();
        }

        public Context getContext() {
            useCount.incrementAndGet();
            return context;
        }

        public void close() {
            try {
                context.close();
            } catch (Exception e) {
                log.warn("Failed to close context: {}", e.getMessage());
            }
        }
    }

    /**
     * Tạo Context hồ bơi
     *
     * @param maxSize Kích thước nhóm tối đa (cài đặt được đề xuất là  CPU Số lượng lõi 2-4 lần)
     */
    public JsContextPool(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>(maxSize);
        log.info("Created JsContextPool with max size: {}", maxSize);
    }

    /**
     * Lấy Context（với thời gian chờ)
     *
     * @param timeoutMs Thời gian chờ (mili giây)
     * @return Context đối tượng
     * @throws InterruptedException Nếu việc chờ đợi bị gián đoạn
     */
    public PooledContext borrowContext(long timeoutMs) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("Context pool is closed");
        }

        long effectiveTimeoutMs = Math.max(1, timeoutMs);
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeoutMs);

        while (true) {
            // 1. Cố gắng lấy (tái sử dụng) nhanh chóng từ hồ bơi
            PooledContext pooled = pool.poll();
            if (pooled != null) {
                totalReused.incrementAndGet();
                log.debug("Reused context from pool, reuse count: {}", totalReused.get());
                return pooled;
            }

            // 2. Nhóm trống, kiểm tra xem có thể tạo nhóm mới không Context。CAS Thử lại khi thất bại để tránh xảy ra đồng thời đột ngột maxSize Chỉ cần vào và chờ đợi.
            int current = currentSize.get();
            if (!retired && current < maxSize) {
                if (currentSize.compareAndSet(current, current + 1)) {
                    try {
                        pooled = createNewContext();
                        totalCreated.incrementAndGet();
                        log.debug("Created new context #{}, pool size: {}/{}",
                                totalCreated.get(), currentSize.get(), maxSize);
                        return pooled;
                    } catch (Exception e) {
                        currentSize.decrementAndGet();
                        throw e;
                    }
                }
                continue;
            }

            // 3. Đã đạt đến giới hạn số lượng tối đa và chặn chờ thời gian nhàn rỗi Context
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new IllegalStateException(
                        String.format("Failed to acquire context within timeout: %dms. Pool size: %d/%d",
                                effectiveTimeoutMs, currentSize.get(), maxSize));
            }
            log.debug("Context pool exhausted ({}), waiting for available context...", current);
            waitingBorrowCount.incrementAndGet();
            try {
                pooled = pool.poll();
                if (pooled != null) {
                    totalReused.incrementAndGet();
                    return pooled;
                }
                if (closed) {
                    throw new IllegalStateException("Context pool is closed");
                }
                if (retired && currentSize.get() <= 0) {
                    throw new IllegalStateException("Context pool is retired");
                }
                pooled = pool.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (pooled != null) {
                    totalReused.incrementAndGet();
                    return pooled;
                }
            } finally {
                waitingBorrowCount.decrementAndGet();
                closeRetiredIdleContextsIfUnused();
            }
            throw new IllegalStateException(
                    String.format("Failed to acquire context within timeout: %dms. Pool size: %d/%d",
                            effectiveTimeoutMs, currentSize.get(), maxSize));
        }
    }

    /**
     * Trở về Context vào hồ bơi (tái sử dụng)
     *
     * @param pooled Context đối tượng
     */
    public void returnContext(PooledContext pooled) {
        if (pooled == null) {
            return;
        }
        if (closed) {
            pooled.close();
            decrementCurrentSize();
            return;
        }

        try {
            // Dọn dẹp trạng thái chung mới được thêm vào trong quá trình thực thi tập lệnh để tránh sử dụng lại Context khi gây ô nhiễm trong lần thực hiện tiếp theo.
            cleanupGlobalVariables(pooled.context);

            // Quay trở lại hồ bơi để tái sử dụng. Pool ngừng hoạt động chỉ phục vụ những người vay đang chờ đợi ở pool cũ, sau đó sẽ dần đóng cửa.
            if (retired) {
                if (waitingBorrowCount.get() > 0 && pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                    return;
                }
                pooled.close();
                decrementCurrentSize();
                log.debug("Retired pool has no waiters, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            } else if (!pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                // Bể bơi đầy và đóng cửa Context
                pooled.close();
                decrementCurrentSize();
                log.debug("Pool is full, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            }
        } catch (Exception e) {
            log.warn("Failed to return context to pool: {}", e.getMessage());
            // Trả lại không thành công, đã đóng Context
            pooled.close();
            decrementCurrentSize();
        }
    }

    private void decrementCurrentSize() {
        currentSize.updateAndGet(value -> Math.max(0, value - 1));
    }

    /**
     * Nhóm đã ngừng hoạt động: Nó không còn được sử dụng làm nhóm chia sẻ mới nữa, nhưng các chủ đề đã chờ trong nhóm này được phép trả lại Context。
     */
    void retire() {
        retired = true;
        closeRetiredIdleContextsIfUnused();
    }

    boolean isRetired() {
        return retired;
    }

    int getWaitingBorrowCountForTests() {
        return waitingBorrowCount.get();
    }

    private void closeRetiredIdleContextsIfUnused() {
        if (!retired || closed || waitingBorrowCount.get() > 0) {
            return;
        }
        PooledContext idle;
        while ((idle = pool.poll()) != null) {
            idle.close();
            decrementCurrentSize();
        }
    }

    /**
     * Dọn dẹp các biến toàn cục được thêm vào trong quá trình thực thi tập lệnh.
     * <p>
     * LƯU Ý: Do việc sử dụng tập lệnh của người dùng IIFE Bưu kiện,let/const Các biến đều là biến cục bộ.
     * Điều này chủ yếu liên quan đến văn bản rõ ràng globalThis Các biến và các biến chúng tôi đã chèn (pm）。
     * </p>
     */
    private void cleanupGlobalVariables(Context context) {
        try {
            context.eval("js", """
                    (function() {
                        const baseline = globalThis.__epBaselineGlobals;
                        if (baseline) {
                            Object.getOwnPropertyNames(globalThis).forEach(name => {
                                if (name === '__epBaselineGlobals') {
                                    return;
                                }
                                if (!baseline[name]) {
                                    try {
                                        delete globalThis[name];
                                    } catch (e) {
                                        // Xóa thất bại được bỏ qua
                                    }
                                }
                            });
                        }

                        const injectedVars = ['pm'];
                        injectedVars.forEach(varName => {
                            try {
                                delete globalThis[varName];
                            } catch (e) {
                                // Xóa thất bại được bỏ qua
                            }
                        });

                        if (globalThis.__epRequireCache) {
                            Object.keys(globalThis.__epRequireCache).forEach(moduleId => {
                                if (String(moduleId).indexOf('builtin:') !== 0) {
                                    delete globalThis.__epRequireCache[moduleId];
                                }
                            });
                        }
                    })();
                    """);
        } catch (Exception e) {
            log.warn("Failed to cleanup global variables: {}", e.getMessage());
        }
    }

    /**
     * Tạo cái mới Context đối tượng
     */
    private PooledContext createNewContext() {
        try {
            Context context = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> false)
                    .allowNativeAccess(false)
                    .out(OutputStream.nullOutputStream())
                    .err(OutputStream.nullOutputStream())
                    .engine(ENGINE)
                    .build();

            // Đã tải trước polyfill và các thư viện tích hợp
            JsPolyfillInjector.injectAll(context);
            recordBaselineGlobals(context);

            return new PooledContext(context);
        } catch (Exception e) {
            log.error("Failed to create new context", e);
            throw new RuntimeException("Failed to create JS context", e);
        }
    }

    private void recordBaselineGlobals(Context context) {
        context.eval("js", """
                (function() {
                    const baseline = Object.create(null);
                    Object.getOwnPropertyNames(globalThis).forEach(name => {
                        baseline[name] = true;
                    });
                    Object.defineProperty(globalThis, '__epBaselineGlobals', {
                        value: baseline,
                        writable: false,
                        configurable: false,
                        enumerable: false
                    });
                })();
                """);
    }

    /**
     * Đóng hồ bơi và giải phóng mọi thứ Context
     */
    public void shutdown() {
        closed = true;
        PooledContext pooled;
        int closedIdleCount = 0;
        while ((pooled = pool.poll()) != null) {
            pooled.close();
            decrementCurrentSize();
            closedIdleCount++;
        }
        double reuseRatio = totalCreated.get() > 0 ? (totalReused.get() * 100.0 / totalCreated.get()) : 0;
        log.info("JsContextPool shutdown. Total created: {}, Total reused: {}, Closed idle: {}, Active borrowed: {}, Reuse ratio: {}%",
                totalCreated.get(), totalReused.get(), closedIdleCount, currentSize.get(), String.format("%.2f", reuseRatio));
    }

    /**
     * Nhận số liệu thống kê
     */
    public String getStats() {
        return String.format("Pool[size=%d/%d, created=%d, reused=%d, ratio=%.2f%%]",
                currentSize.get(), maxSize, totalCreated.get(), totalReused.get(),
                totalCreated.get() > 0 ? (totalReused.get() * 100.0 / totalCreated.get()) : 0);
    }
}
