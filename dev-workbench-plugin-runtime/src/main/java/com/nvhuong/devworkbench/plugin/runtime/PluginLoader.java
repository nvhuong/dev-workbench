package com.nvhuong.devworkbench.plugin.runtime;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginDescriptor;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import com.nvhuong.devworkbench.util.I18nBundleRegistry;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Trình tải plugin.
 * <p>
 * Chịu trách nhiệm tải lớp, khởi tạo, gọi vòng đời và kết nối các hành động đăng ký phần bổ trợ với sổ đăng ký thời gian chạy.
 * </p>
 */
@Slf4j
@UtilityClass
class PluginLoader {

    Optional<String> loadPluginJar(Path jarPath,
                                   PluginDescriptor descriptor,
                                   PluginRegistry registry,
                                   List<DevWorkbenchPlugin> loadedPlugins,
                                   List<URLClassLoader> pluginClassLoaders,
                                   List<PluginFileInfo> loadedPluginFiles,
                                   ClassLoader parentClassLoader) {
        URLClassLoader classLoader = null;
        PluginContextImpl context = null;
        try {
            log.info("Loading plugin: id={}, version={}, entryClass={}, jar={}",
                    descriptor.id(), descriptor.version(), descriptor.entryClass(), jarPath);
            classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, parentClassLoader);
            Class<?> entryClass = Class.forName(descriptor.entryClass(), true, classLoader);
            Object instance = entryClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof DevWorkbenchPlugin plugin)) {
                throw new IllegalStateException("Plugin entry class does not implement DevWorkbenchPlugin: " + descriptor.entryClass());
            }
            context = new PluginContextImpl(descriptor, registry, classLoader, PluginFileStorage.forPlugin(descriptor.id()));
            plugin.onLoad(context);
            // Contributions only become visible once onLoad completed; a plugin failing halfway must not leave
            // registrations pointing into a class loader that is closed below.
            context.commit();
            loadedPlugins.add(plugin);
            pluginClassLoaders.add(classLoader);
            loadedPluginFiles.add(new PluginFileInfo(descriptor, jarPath, true, true, true));
            log.info("Loaded plugin successfully: id={}, version={}, jar={}",
                    descriptor.id(), descriptor.version(), jarPath);
            return Optional.empty();
        } catch (Exception | LinkageError e) {
            log.error("Failed to load plugin jar: {}", jarPath, e);
            if (context != null) {
                context.rollback();
            }
            closeFailedClassLoader(classLoader);
            return Optional.of(buildLoadFailureMessage(e));
        }
    }

    private String buildLoadFailureMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return current.getClass().getSimpleName() + ": " + message;
    }

    private void closeFailedClassLoader(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException closeError) {
            log.warn("Failed to close failed plugin classloader", closeError);
        }
    }

    void startPlugins(List<DevWorkbenchPlugin> plugins) {
        for (DevWorkbenchPlugin plugin : plugins) {
            try {
                plugin.onStart();
                log.info("Started plugin: {}", plugin.getClass().getName());
            } catch (Exception e) {
                log.error("Failed to start plugin: {}", plugin.getClass().getName(), e);
            }
        }
    }

    void stopPlugins(List<DevWorkbenchPlugin> plugins) {
        for (DevWorkbenchPlugin plugin : plugins) {
            try {
                plugin.onStop();
            } catch (Exception e) {
                log.warn("Failed to stop plugin: {}", plugin.getClass().getName(), e);
            }
        }
    }

    void closeClassLoaders(List<URLClassLoader> classLoaders) {
        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close plugin classloader", e);
            }
        }
    }

    @RequiredArgsConstructor
    private static final class PluginContextImpl implements PluginContext {
        private final PluginDescriptor descriptor;
        private final PluginRegistry registry;
        private final ClassLoader classLoader;
        private final PluginStorage storage;
        /** Registry writes requested during onLoad, applied by {@link #commit()} only when onLoad succeeds. */
        private final List<Runnable> pendingRegistrations = new java.util.ArrayList<>();
        private boolean committed;

        private void deferOrApply(Runnable registration) {
            if (committed) {
                registration.run();
            } else {
                pendingRegistrations.add(registration);
            }
        }

        void commit() {
            committed = true;
            pendingRegistrations.forEach(Runnable::run);
            pendingRegistrations.clear();
        }

        void rollback() {
            pendingRegistrations.clear();
            I18nBundleRegistry.unregisterBundles(descriptor.id(), classLoader);
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public PluginStorage storage() {
            return storage;
        }

        @Override
        public void registerScriptApi(String alias, java.util.function.Supplier<Object> factory) {
            deferOrApply(() -> registry.registerScriptApi(descriptor.id(), alias, factory));
        }

        @Override
        public <T> void registerService(Class<T> type, T service) {
            deferOrApply(() -> registry.registerService(descriptor.id(), type, service));
        }

        @Override
        public <T> T getService(Class<T> type) {
            return registry.getService(type);
        }

        @Override
        public void registerToolboxContribution(com.nvhuong.devworkbench.plugin.api.ToolboxContribution contribution) {
            deferOrApply(() -> registry.registerToolboxContribution(contribution));
        }

        @Override
        public void registerSettingsContribution(com.nvhuong.devworkbench.plugin.api.PluginSettingsContribution contribution) {
            deferOrApply(() -> registry.registerSettingsContribution(
                    descriptor.id(),
                    contribution == null ? null : contribution.withTitleClassLoader(classLoader)
            ));
        }

        @Override
        public void registerMenuContribution(com.nvhuong.devworkbench.plugin.api.PluginMenuContribution contribution) {
            deferOrApply(() -> registry.registerMenuContribution(
                    descriptor.id(),
                    contribution == null ? null : contribution.withTitleClassLoader(classLoader)
            ));
        }

        @Override
        public void registerStatusBarActionContribution(
                com.nvhuong.devworkbench.plugin.api.StatusBarActionContribution contribution) {
            deferOrApply(() -> registry.registerStatusBarActionContribution(
                    descriptor.id(),
                    contribution == null ? null : contribution.withIconClassLoader(classLoader)
            ));
        }

        @Override
        public void registerUpdateMetadataContribution(
                com.nvhuong.devworkbench.plugin.api.PluginUpdateMetadataContribution contribution) {
            deferOrApply(() -> registry.registerUpdateMetadataContribution(descriptor.id(), contribution));
        }

        @Override
        public void registerScriptCompletionContributor(com.nvhuong.devworkbench.plugin.api.ScriptCompletionContributor contributor) {
            deferOrApply(() -> registry.registerScriptCompletionContributor(contributor));
        }

        @Override
        public void registerSnippet(com.nvhuong.devworkbench.plugin.api.SnippetDefinition definition) {
            deferOrApply(() -> registry.registerSnippet(definition));
        }

        @Override
        public void registerI18nBundle(String bundleName) {
            I18nBundleRegistry.registerBundle(descriptor.id(), bundleName, classLoader);
        }
    }
}
