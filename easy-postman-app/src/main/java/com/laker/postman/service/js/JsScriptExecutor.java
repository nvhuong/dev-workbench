package com.laker.postman.service.js;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSTrình thực thi tập lệnh, sử dụngGraalVMcủaPolyglot APIthực hiệnJavaScriptkịch bản.
 * Cung cấp lối vào thực thi tập lệnh thống nhất và hỗ trợ chèn biến.polyfill、Lệnh gọi lại đầu ra và xử lý lỗi.
 * <p>
 * sử dụng Context Pool Tái sử dụng Context Đối tượng để tránh sự cố tràn bộ nhớ trong các tình huống có tính tương tranh cao.
 * </p>
 */
@Slf4j
@UtilityClass
public class JsScriptExecutor {

    /**
     * bình thường collection/function Nhóm chia sẻ tập lệnh. Được sử dụng để kiểm tra căng thẳng PooledScriptExecutor Nhóm độc lập ở cấp độ chạy.
     */
    static final int DEFAULT_SHARED_CONTEXT_POOL_SIZE = Math.max(
            4,
            Math.min(16, Runtime.getRuntime().availableProcessors())
    );
    private static final int DEFAULT_SHARED_CONTEXT_ACQUIRE_TIMEOUT_MS = 1_000;
    private static volatile JsContextPool contextPool;
    private static volatile int contextPoolSize;
    private static volatile int contextAcquireTimeoutMs; // Lấy Context khoảng thời gian chờ
    private static final Object CONTEXT_POOL_LOCK = new Object();
    private static final int SCRIPT_SOURCE_CACHE_MAX_SIZE = 512;
    private static final Map<String, Source> SCRIPT_SOURCE_CACHE = new LinkedHashMap<>(
            SCRIPT_SOURCE_CACHE_MAX_SIZE,
            0.75f,
            true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Source> eldest) {
            return size() > SCRIPT_SOURCE_CACHE_MAX_SIZE;
        }
    };

    /**
     * ThreadLocal Lưu trữ tập lệnh gốc hiện đang được thực thi để báo cáo lỗi
     */
    private static final ThreadLocal<String> CURRENT_SCRIPT = new ThreadLocal<>();

    @FunctionalInterface
    public interface ScriptExecutor {
        void execute(ScriptExecutionContext context) throws ScriptExecutionException;
    }

    public static final class PooledScriptExecutor implements ScriptExecutor, AutoCloseable {
        private final JsContextPool pool;
        private final int acquireTimeoutMs;
        private final Map<String, Source> scriptSourceCache = new LinkedHashMap<>(
                SCRIPT_SOURCE_CACHE_MAX_SIZE,
                0.75f,
                true
        ) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Source> eldest) {
                return size() > SCRIPT_SOURCE_CACHE_MAX_SIZE;
            }
        };

        public PooledScriptExecutor(int poolSize, int acquireTimeoutMs) {
            this.pool = new JsContextPool(Math.max(1, poolSize));
            this.acquireTimeoutMs = Math.max(1, acquireTimeoutMs);
        }

        @Override
        public void execute(ScriptExecutionContext context) throws ScriptExecutionException {
            executeScript(context, pool, acquireTimeoutMs, scriptSourceCache);
        }

        @Override
        public void close() {
            pool.shutdown();
            synchronized (scriptSourceCache) {
                scriptSourceCache.clear();
            }
        }
    }

    static {
        // Đăng ký shutdown hook，Đóng nhóm khi ứng dụng thoát
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down JS Context Pool...");
            JsContextPool pool = contextPool;
            if (pool != null) {
                pool.shutdown();
            }
        }));
    }

    public static void reconfigureContextPoolFromSettings() {
        int resolvedPoolSize = DEFAULT_SHARED_CONTEXT_POOL_SIZE;
        int resolvedAcquireTimeoutMs = DEFAULT_SHARED_CONTEXT_ACQUIRE_TIMEOUT_MS;

        synchronized (CONTEXT_POOL_LOCK) {
            contextAcquireTimeoutMs = resolvedAcquireTimeoutMs;
            if (contextPool == null || contextPoolSize != resolvedPoolSize) {
                JsContextPool oldPool = contextPool;
                contextPool = new JsContextPool(resolvedPoolSize);
                contextPoolSize = resolvedPoolSize;
                log.info("Initialized shared JS Context Pool with size: {}, acquire timeout: {}ms",
                        resolvedPoolSize, resolvedAcquireTimeoutMs);
                if (oldPool != null) {
                    oldPool.retire();
                }
            } else {
                log.info("Updated shared JS Context Pool acquire timeout: {}ms", resolvedAcquireTimeoutMs);
            }
        }
    }

    /**
     * thực hiệnJSScript (sử dụng đối tượng bối cảnh)
     *
     * @param context Bối cảnh thực thi tập lệnh
     * @throws ScriptExecutionException Ngoại lệ thực thi tập lệnh
     */
    public static void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        executeScript(context, null, 0, SCRIPT_SOURCE_CACHE);
    }

    private static void executeScript(ScriptExecutionContext context,
                                      JsContextPool pool,
                                      int acquireTimeoutMs,
                                      Map<String, Source> scriptSourceCache) throws ScriptExecutionException {
        if (context == null || context.getScript() == null || context.getScript().isBlank()) {
            log.debug("Script is empty, skipping execution");
            return;
        }

        try {
            executeScript(
                    context.getScript(),
                    context.getBindings(),
                    context.getOutputCallback(),
                    pool,
                    acquireTimeoutMs,
                    scriptSourceCache
            );
            log.debug("Script executed successfully: {}", context.getScriptType().getDisplayName());
        } catch (Exception e) {
            String errorMsg = String.format("%s execution failed: %s",
                    context.getScriptType().getDisplayName(), e.getMessage());
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e, context.getScriptType());
        }
    }

    /**
     * thực hiệnJSTập lệnh tự động chèn tất cả các biến.polyfill，Và hỗ trợ gọi lại đầu ra.
     * <p>
     * sử dụng Context Pool Tái sử dụng Context Object, cải thiện hiệu suất và tránh tràn bộ nhớ.
     * sử dụng IIFE (Immediately Invoked Function Expression) Tập lệnh người dùng trình bao bọc, .
     * tránh let/const Các biến gây ô nhiễm phạm vi toàn cầu, tạo ra Context Có thể được tái sử dụng một cách an toàn.
     * </p>
     *
     * @param script         Nội dung kịch bản
     * @param bindings       Các biến cần được chèn (tên biến->đối tượng), chẳng hạn như pm
     * @param outputCallback Gọi lại đầu ra (tùy chọnnull）
     * @throws ScriptExecutionException Ngoại lệ thực thi tập lệnh
     */
    public static void executeScript(String script, Map<String, Object> bindings, OutputCallback outputCallback)
            throws ScriptExecutionException {
        executeScript(script, bindings, outputCallback, null, 0, SCRIPT_SOURCE_CACHE);
    }

    private static void executeScript(String script,
                                      Map<String, Object> bindings,
                                      OutputCallback outputCallback,
                                      JsContextPool pool,
                                      int acquireTimeoutMs,
                                      Map<String, Source> scriptSourceCache)
            throws ScriptExecutionException {
        if (script == null || script.isBlank()) {
            return;
        }

        JsContextPool.PooledContext pooledContext = null;
        JsContextPool borrowedPool = null;

        try {
            // Lưu tập lệnh gốc vào  ThreadLocal，để báo cáo lỗi
            CURRENT_SCRIPT.set(script);

            // Lấy nó từ hồ bơi Context
            while (pooledContext == null) {
                borrowedPool = pool == null ? sharedContextPool() : pool;
                if (borrowedPool == null) {
                    throw new ScriptExecutionException("JS context pool is not initialized", null);
                }
                int resolvedAcquireTimeoutMs = pool == null ? contextAcquireTimeoutMs : acquireTimeoutMs;
                try {
                    pooledContext = borrowedPool.borrowContext(resolvedAcquireTimeoutMs);
                } catch (IllegalStateException e) {
                    if (pool == null && borrowedPool != contextPool && borrowedPool.isRetired()) {
                        log.debug("Retrying JS context borrow after old pool was retired");
                        continue;
                    }
                    throw e;
                }
            }
            Context context = pooledContext.getContext();

            // Thực hiện lệnh gọi lại đầu ra (nếu có)
            if (outputCallback != null) {
                injectConsoleLog(context, outputCallback);
            }

            // Chèn các biến (polyfill đã được chèn khi nhóm được tạo)
            injectBindings(context, bindings);

            context.eval(getCachedScriptSource(script, scriptSourceCache));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Script execution interrupted: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (PolyglotException e) {
            // GraalVMNgoại lệ cụ thể
            String errorMsg = formatPolyglotError(e);
            log.error("Script execution error: {}", errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during script execution: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } finally {
            // Dọn dẹp ThreadLocal
            CURRENT_SCRIPT.remove();

            // Trở về Context đến hồ bơi
            if (pooledContext != null) {
                borrowedPool.returnContext(pooledContext);
            }
        }
    }

    private static JsContextPool sharedContextPool() {
        JsContextPool pool = contextPool;
        if (pool == null) {
            reconfigureContextPoolFromSettings();
            pool = contextPool;
        }
        return pool;
    }

    /**
     * Phần bù số dòng bắt đầu của tập lệnh người dùng sau khi gói
     * IIFE Trình bao bọc thêm một dòng: (function() {
     * Vì vậy, tập lệnh người dùng bắt đầu từ s 2 dòng bắt đầu
     */
    private static final int USER_SCRIPT_LINE_OFFSET = 1;

    /**
     * sử dụng IIFE Tập lệnh bao bọc, tránh let/const Các biến gây ô nhiễm phạm vi toàn cầu
     * <p>
     * Nguyên tắc: Gói tập lệnh người dùng vào một hàm được thực thi ngay lập tức.
     * làm tất cả let/const/var Tất cả các biến được khai báo đều trở thành biến cục bộ.
     * </p>
     * <p>
     * Hình thức đóng gói:
     * KHÔNG. 1Được rồi: (function() {
     * KHÔNG. 2dòng bắt đầu: Kịch bản người dùng
     * Cuối cùng: })();
     * </p>
     *
     * @param script Kịch bản gốc
     * @return Kịch bản được gói gọn
     */
    private static String wrapScriptWithIIFE(String script) {
        return "(function() {\n" + script + "\n})();";
    }

    private static Source getCachedScriptSource(String script) {
        return getCachedScriptSource(script, SCRIPT_SOURCE_CACHE);
    }

    private static Source getCachedScriptSource(String script, Map<String, Source> scriptSourceCache) {
        Map<String, Source> resolvedCache = scriptSourceCache == null ? SCRIPT_SOURCE_CACHE : scriptSourceCache;
        synchronized (resolvedCache) {
            Source source = resolvedCache.get(script);
            if (source != null) {
                return source;
            }

            Source newSource = Source.newBuilder("js", wrapScriptWithIIFE(script), buildSourceName(script))
                    .cached(true)
                    .buildLiteral();
            resolvedCache.put(script, newSource);
            return newSource;
        }
    }

    private static String buildSourceName(String script) {
        return "easypostman-user-script-" + Integer.toUnsignedString(script.hashCode(), 16) + ".js";
    }

    /**
     * Tiêm tùy chỉnh console Phương pháp thực hiện (sử dụng Java gọi lại)
     * Hỗ trợ console.log, console.error, console.warn, console.info, console.debug
     */
    private static void injectConsoleLog(Context context, OutputCallback outputCallback) {
        if (outputCallback == null) {
            return;
        }

        // Hãy chắc chắn console Đối tượng tồn tại
        context.eval("js", """
                if (typeof console === 'undefined') {
                    globalThis.console = {};
                }
                """);

        // Tiêm tất cả theo đợt console phương pháp
        var consoleObj = context.getBindings("js").getMember("console");
        for (ConsoleType type : ConsoleType.values()) {
            consoleObj.putMember(type.getMethodName(), createConsoleFunc(outputCallback, type));
        }
    }

    /**
     * Tạo console phương pháp ProxyExecutable
     *
     * @param callback    Đầu ra gọi lại
     * @param consoleType Console Loại phương pháp
     * @return ProxyExecutable Ví dụ
     */
    private static ProxyExecutable createConsoleFunc(OutputCallback callback, ConsoleType consoleType) {
        return args -> {
            if (args.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(args[i].toString());
                }
                callback.onOutput(sb.toString(), consoleType);
            }
            return null;
        };
    }


    /**
     * Định dạngPolyglotThông tin ngoại lệ
     * <p>
     * LƯU Ý: Vì tập lệnh có thể Group/Request Hợp nhất, là tốt IIFE Bưu kiện,
     * Số dòng có thể không chính xác. Do đó, nó chủ yếu dựa vào việc hiển thị nội dung mã lỗi để giúp xác định vấn đề.
     */
    private static String formatPolyglotError(PolyglotException e) {
        if (e.isHostException()) {
            return "Host exception: " + e.getMessage();
        }
        if (e.isGuestException()) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());

            if (e.getSourceLocation() != null) {
                int reportedLine = e.getSourceLocation().getStartLine();

                // Cố gắng lấy dòng mã lỗi (dựa trên số dòng được gói)
                int adjustedLine = reportedLine - USER_SCRIPT_LINE_OFFSET;
                adjustedLine = Math.max(1, adjustedLine);
                String codeAtLine = getCodeLineFromScript(adjustedLine);

                // Hiển thị mã lỗi (đây là mã quan trọng nhất, người dùng có thể tìm kiếm và định vị trực tiếp)
                if (codeAtLine != null && !codeAtLine.trim().isEmpty()) {
                    sb.append("\n>>> ").append(codeAtLine.trim());
                }
            }
            return sb.toString();
        }
        return e.getMessage();
    }

    /**
     * Lấy mã ở số dòng được chỉ định từ tập lệnh gốc
     *
     * @param lineNumber Số dòng (1-based）
     * @return Nội dung của dòng mã, được trả về nếu không lấy được null
     */
    private static String getCodeLineFromScript(int lineNumber) {
        try {
            String script = CURRENT_SCRIPT.get();
            if (script == null) {
                return null;
            }

            String[] lines = script.split("\n");
            if (lineNumber > 0 && lineNumber <= lines.length) {
                return lines[lineNumber - 1];
            }
        } catch (Exception ex) {
            log.trace("Failed to get code line: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Chèn các biến vàoJSbối cảnh
     */
    private static void injectBindings(Context context, Map<String, Object> bindings) {
        if (bindings != null && !bindings.isEmpty()) {
            for (var entry : bindings.entrySet()) {
                try {
                    context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    log.trace("Injected binding: {}", entry.getKey());
                } catch (Exception e) {
                    log.warn("Failed to inject binding: {}, error: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    /**
     * Console Kiểu phương thức enum
     */
    @Getter
    public enum ConsoleType {
        LOG("log"),
        ERROR("error"),
        WARN("warn"),
        INFO("info"),
        DEBUG("debug");

        private final String methodName;

        ConsoleType(String methodName) {
            this.methodName = methodName;
        }

    }

    /**
     * Giao diện gọi lại đầu ra
     */
    public interface OutputCallback {
        void onOutput(String output);

        /**
         * Gọi lại đầu ra với loại nhật ký
         *
         * @param output      Nội dung đầu ra
         * @param consoleType Console Loại phương pháp
         */
        default void onOutput(String output, ConsoleType consoleType) {
            onOutput(output); // Dự phòng các phương thức chưa được gõ theo mặc định
        }
    }
}
