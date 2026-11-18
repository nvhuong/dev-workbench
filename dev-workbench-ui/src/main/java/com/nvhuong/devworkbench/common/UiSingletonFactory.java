package com.nvhuong.devworkbench.common;

import com.nvhuong.devworkbench.common.exception.GetInstanceException;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UI Singleton Factory, chỉ dùng để tạo và quản lý cửa sổ chính, bảng điều khiển singleton và thanh menu singleton.
 * <p>
 * kinh doanh Service/Repository nên vượt qua IOC Container lấy được thì không nên đưa vào nhà máy này.
 */
@Slf4j
public class UiSingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();
    private static final Set<Class<?>> CREATING_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object CREATION_LOCK = new Object();

    private UiSingletonFactory() {
        // Nhà xây dựng riêng để ngăn chặn việc khởi tạo
    }

    /**
     * Nhận một cá thể đơn được xây dựng không có tham số
     * Unity được tạo ra lần đầu tiên EDT Xong rồi, tránh ra Swing Các thành phần được khởi tạo trên một luồng nền.
     * nếu UI Nếu sự phụ thuộc vòng tròn xảy ra trong giai đoạn khởi tạo đơn lẻ, nó sẽ trực tiếp bị lỗi và nhắc bạn điều chỉnh thiết kế để không để lộ các đối tượng bán thành phẩm.
     */
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (!isSupportedUiSingleton(clazz)) {
            throw new IllegalArgumentException(
                    "UiSingletonFactory can only create UI singletons. Use BeanFactory for services: "
                            + clazz.getName());
        }
        Object existing = INSTANCE_MAP.get(clazz);
        if (clazz.isInstance(existing)) {
            return clazz.cast(existing);
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return getOrCreateOnEdt(clazz);
        }

        return createOnEdtAndWait(clazz);
    }

    private static <T> T createOnEdtAndWait(Class<T> clazz) {
        AtomicReference<T> instanceRef = new AtomicReference<>();
        AtomicReference<RuntimeException> runtimeExceptionRef = new AtomicReference<>();
        AtomicReference<Error> errorRef = new AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    instanceRef.set(getOrCreateOnEdt(clazz));
                } catch (RuntimeException e) {
                    runtimeExceptionRef.set(e);
                } catch (Error e) {
                    errorRef.set(e);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GetInstanceException("Không tạo được singleton: " + clazz.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new GetInstanceException("Không tạo được singleton: " + clazz.getName(), cause);
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        if (runtimeExceptionRef.get() != null) {
            throw runtimeExceptionRef.get();
        }
        return instanceRef.get();
    }

    private static <T> T getOrCreateOnEdt(Class<T> clazz) {
        synchronized (CREATION_LOCK) {
            Object existing = INSTANCE_MAP.get(clazz);
            if (clazz.isInstance(existing)) {
                return clazz.cast(existing);
            }
            if (CREATING_CLASSES.contains(clazz)) {
                throw new GetInstanceException(
                        "Circular UI singleton creation detected: " + clazz.getName(),
                        new IllegalStateException("Circular UI singleton creation: " + clazz.getName()));
            }
            CREATING_CLASSES.add(clazz);
            try {
                return createInstance(clazz);
            } finally {
                CREATING_CLASSES.remove(clazz);
            }
        }
    }

    private static <T> T createInstance(Class<T> clazz) {
        boolean isUiSingletonPanel = UiSingletonPanel.class.isAssignableFrom(clazz);
        boolean isUiSingletonMenuBar = UiSingletonMenuBar.class.isAssignableFrom(clazz);
        try {
            // Đối vớiUiSingletonPanelNhập, đặt cờ tạo
            if (isUiSingletonPanel) {
                UiSingletonPanel.setFactoryCreationAllowed(true);
            }

            if (isUiSingletonMenuBar) {
                UiSingletonMenuBar.setFactoryCreationAllowed(true);
            }

            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            if (instance instanceof UiSingletonPanel panel) {
                panel.initializeSingletonUi();
            } else if (instance instanceof UiSingletonMenuBar menuBar) {
                menuBar.initializeSingletonUi();
            }
            INSTANCE_MAP.put(clazz, instance);
            return instance;
        } catch (GetInstanceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không tạo được singleton: {}", clazz.getName(), e);
            throw new GetInstanceException("Không tạo được singleton: " + clazz.getName(), e);
        } finally {
            if (UiSingletonPanel.class.isAssignableFrom(clazz)) {
                UiSingletonPanel.setFactoryCreationAllowed(false);
            }
            if (UiSingletonMenuBar.class.isAssignableFrom(clazz)) {
                UiSingletonMenuBar.setFactoryCreationAllowed(false);
            }
        }
    }

    private static boolean isSupportedUiSingleton(Class<?> clazz) {
        return UiSingletonPanel.class.isAssignableFrom(clazz)
                || UiSingletonMenuBar.class.isAssignableFrom(clazz)
                || JFrame.class.isAssignableFrom(clazz);
    }

    public static <T> Optional<T> getExistingInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }

        Object instance = INSTANCE_MAP.get(clazz);
        if (clazz.isInstance(instance)) {
            return Optional.of(clazz.cast(instance));
        }
        return Optional.empty();
    }
}
