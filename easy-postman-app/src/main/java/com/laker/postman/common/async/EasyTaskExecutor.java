package com.laker.postman.common.async;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Người thực thi nhiệm vụ không đồng bộ
 * <p>
 * Được sử dụng để thực hiện các thao tác tốn thời gian trên một luồng nền (ví dụ:IO、yêu cầu mạng, v.v.) rồi vàoEDTĐã cập nhật trong chủ đềUI。
 * Tránh vàoEDTViệc thực hiện các thao tác tốn thời gian trên màn hình khiến giao diện bị đơ.
 * </p>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Ví dụ1：Nhiệm vụ nền đơn giản + UIđổi mới
 * AsyncTaskExecutor.execute(
 *     () -> {
 *         // Chủ đề nền: thực hiện tốn thời gianIOhoạt động
 *         return loadDataFromFile();
 *     },
 *     result -> {
 *         // EDTChủ đề: đã cập nhật kết quả tảiUI
 *         updateUIWithData(result);
 *     },
 *     error -> {
 *         // EDTChủ đề: xử lý lỗi
 *         showErrorMessage(error.getMessage());
 *     }
 * );
 *
 * // Ví dụ2：Chỉ các tác vụ nền được thực thi và không yêu cầu giá trị trả về
 * AsyncTaskExecutor.execute(
 *     () -> saveDataToFile(data),
 *     () -> showSuccessMessage(),
 *     error -> showErrorMessage(error.getMessage())
 * );
 *
 * // Ví dụ3：Sử dụng nhóm chủ đề tùy chỉnh và tên chủ đề
 * AsyncTaskExecutor.builder()
 *     .threadName("CustomTask")
 *     .backgroundTask(() -> heavyComputation())
 *     .onSuccess(result -> updateUI(result))
 *     .onError(error -> handleError(error))
 *     .execute();
 * }</pre>
 *
 * @author Laker
 */
@Slf4j
@UtilityClass
public class EasyTaskExecutor {

    private static final String DEFAULT_THREAD_NAME = "EasyTask";
    private static final String ERROR_MESSAGE = "Error executing async task";

    /**
     * Nhóm luồng mặc định: được sử dụng để thực hiện các tác vụ không đồng bộ
     * Số lượng chủ đề cốt lõi làCPUSố lượng lõi và số lượng luồng tối đa làCPUSố lượng lõi2lần
     */
    private static final ExecutorService DEFAULT_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private int count = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, DEFAULT_THREAD_NAME + "-" + (++count));
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * Thực hiện một tác vụ không đồng bộ (không có giá trị trả về)
     *
     * @param backgroundTask Tác vụ nền (được thực thi trên các luồng nền)
     * @param onSuccess      gọi lại thành công(inEDTthực thi luồng)
     * @param onError        Lỗi gọi lại (trong EDTthực thi luồng)
     */
    public static void execute(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError
    ) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_THREAD_NAME);
    }

    /**
     * Thực thi các tác vụ không đồng bộ (không có giá trị trả về, có tên luồng)
     *
     * @param backgroundTask Tác vụ nền (được thực thi trên các luồng nền)
     * @param onSuccess      gọi lại thành công(inEDTthực thi luồng)
     * @param onError        Lỗi gọi lại (trong EDTthực thi luồng)
     * @param threadName     Tên chủ đề
     */
    public static void execute(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError,
            String threadName
    ) {
        new Thread(() -> {
            try {
                backgroundTask.run();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(onSuccess);
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        }, threadName).start();
    }

    /**
     * Thực hiện các tác vụ không đồng bộ (có giá trị trả về)
     *
     * @param backgroundTask Tác vụ nền (được thực thi trên các luồng nền)
     * @param onSuccess      gọi lại thành công(inEDTthực thi luồng)
     * @param onError        Lỗi gọi lại (trong EDTthực thi luồng)
     * @param <T>            Kiểu giá trị trả về
     */
    public static <T> void execute(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_THREAD_NAME);
    }

    /**
     * Thực thi các tác vụ không đồng bộ (với giá trị trả về và tên luồng)
     *
     * @param backgroundTask Tác vụ nền (được thực thi trên các luồng nền)
     * @param onSuccess      gọi lại thành công(inEDTthực thi luồng)
     * @param onError        Lỗi gọi lại (trong EDTthực thi luồng)
     * @param threadName     Tên chủ đề
     * @param <T>            Kiểu giá trị trả về
     */
    public static <T> void execute(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            String threadName
    ) {
        new Thread(() -> {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        }, threadName).start();
    }

    /**
     * Sử dụng nhóm luồng để thực thi các tác vụ không đồng bộ (không có giá trị trả về)
     *
     * @param backgroundTask Nhiệm vụ nền
     * @param onSuccess      Gọi lại thành công
     * @param onError        Lỗi gọi lại
     */
    public static void executeWithPool(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError
    ) {
        DEFAULT_EXECUTOR.execute(() -> {
            try {
                backgroundTask.run();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(onSuccess);
                }
            } catch (Exception e) {
                log.error("Error executing async task in pool", e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        });
    }

    /**
     * Sử dụng nhóm luồng để thực thi các tác vụ không đồng bộ (có giá trị trả về)
     *
     * @param backgroundTask Nhiệm vụ nền
     * @param onSuccess      Gọi lại thành công
     * @param onError        Lỗi gọi lại
     * @param <T>            Kiểu giá trị trả về
     * @return FutureĐối tượng có thể được sử dụng để hủy nhiệm vụ hoặc nhận kết quả
     */
    public static <T> Future<T> executeWithPool(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        return DEFAULT_EXECUTOR.submit(() -> {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
                return result;
            } catch (Exception e) {
                log.error("Error executing async task in pool", e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
                throw e;
            }
        });
    }

    /**
     * Tạo một trình tạo tác vụ không đồng bộ
     *
     * @param <T> Kiểu giá trị trả về
     * @return Ví dụ về người xây dựng
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Đóng nhóm luồng mặc định (được gọi khi ứng dụng thoát)
     */
    public static void shutdown() {
        DEFAULT_EXECUTOR.shutdown();
        try {
            if (!DEFAULT_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                DEFAULT_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            DEFAULT_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Trình tạo tác vụ không đồng bộ
     * <p>
     * Cung cấp một cách linh hoạt hơn để xây dựng các tác vụ không đồng bộ
     * </p>
     *
     * @param <T> Kiểu giá trị trả về
     */
    public static class Builder<T> {
        private Callable<T> backgroundTask;
        private Consumer<T> onSuccess;
        private Consumer<Exception> onError;
        private Runnable onFinally;
        private String threadName = "AsyncTask";
        private boolean useThreadPool = false;

        /**
         * Thiết lập các tác vụ nền
         */
        public Builder<T> backgroundTask(Callable<T> task) {
            this.backgroundTask = task;
            return this;
        }

        /**
         * Thiết lập tác vụ nền (không có giá trị trả về)
         */
        public Builder<T> backgroundTask(Runnable task) {
            this.backgroundTask = () -> {
                task.run();
                return null;
            };
            return this;
        }

        /**
         * Đặt gọi lại thành công
         */
        public Builder<T> onSuccess(Consumer<T> callback) {
            this.onSuccess = callback;
            return this;
        }

        /**
         * Đặt gọi lại thành công (không có tham số)
         */
        public Builder<T> onSuccess(Runnable callback) {
            this.onSuccess = result -> callback.run();
            return this;
        }

        /**
         * Đặt gọi lại lỗi
         */
        public Builder<T> onError(Consumer<Exception> callback) {
            this.onError = callback;
            return this;
        }

        /**
         * Đặt lệnh gọi lại cuối cùng (được thực hiện bất kể thành công hay thất bại)
         */
        public Builder<T> onFinally(Runnable callback) {
            this.onFinally = callback;
            return this;
        }

        /**
         * Đặt tên chủ đề
         */
        public Builder<T> threadName(String name) {
            this.threadName = name;
            return this;
        }

        /**
         * Thực thi bằng cách sử dụng nhóm luồng
         */
        public Builder<T> useThreadPool() {
            this.useThreadPool = true;
            return this;
        }

        /**
         * Thực hiện nhiệm vụ
         */
        public void execute() {
            if (backgroundTask == null) {
                throw new IllegalStateException("Background task is required");
            }

            if (useThreadPool) {
                DEFAULT_EXECUTOR.execute(() -> executeTask());
            } else {
                new Thread(this::executeTask, threadName).start();
            }
        }

        /**
         * Thực hiện nhiệm vụ và trở vềFuture（Chỉ hợp lệ khi sử dụng nhóm luồng)
         */
        public Future<T> submit() {
            if (backgroundTask == null) {
                throw new IllegalStateException("Background task is required");
            }
            return DEFAULT_EXECUTOR.submit(this::executeTaskWithResult);
        }

        private void executeTask() {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            } finally {
                if (onFinally != null) {
                    SwingUtilities.invokeLater(onFinally);
                }
            }
        }

        private T executeTaskWithResult() throws Exception {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
                return result;
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
                throw e;
            } finally {
                if (onFinally != null) {
                    SwingUtilities.invokeLater(onFinally);
                }
            }
        }
    }
}

