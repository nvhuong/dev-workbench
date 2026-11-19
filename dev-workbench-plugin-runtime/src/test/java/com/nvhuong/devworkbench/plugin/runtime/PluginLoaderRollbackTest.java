package com.nvhuong.devworkbench.plugin.runtime;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginDescriptor;
import com.nvhuong.devworkbench.plugin.api.SnippetDefinition;
import com.nvhuong.devworkbench.util.I18nBundleRegistry;
import org.testng.annotations.Test;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PluginLoaderRollbackTest {

    /**
     * A plugin whose onLoad fails after registering contributions used to leave them in the registry while its class
     * loader was closed, so the host later called into classes of a closed loader.
     */
    @Test
    public void failedOnLoadLeavesNoRegistrations() throws Exception {
        PluginRegistry registry = new PluginRegistry();
        List<URLClassLoader> classLoaders = new ArrayList<>();
        List<DevWorkbenchPlugin> plugins = new ArrayList<>();

        Optional<String> failure = load(registry, plugins, classLoaders, "rollback-failing", FailingPlugin.class);

        assertTrue(failure.isPresent(), "load should report the onLoad failure");
        assertTrue(plugins.isEmpty());
        assertTrue(classLoaders.isEmpty());
        assertNull(registry.getService(RollbackService.class), "service registered before the failure must be removed");
        assertFalse(registry.createScriptApis().containsKey("rollbackApi"), "script API must be removed");
        assertTrue(registry.getSnippetDefinitions().isEmpty(), "snippet must be removed");
        assertTrue(I18nBundleRegistry.registeredBundles().stream()
                        .noneMatch(bundle -> "rollback-failing".equals(bundle.ownerId())),
                "i18n bundle of the failed plugin must be removed");
    }

    @Test
    public void successfulOnLoadKeepsRegistrations() throws Exception {
        PluginRegistry registry = new PluginRegistry();
        List<URLClassLoader> classLoaders = new ArrayList<>();
        List<DevWorkbenchPlugin> plugins = new ArrayList<>();

        Optional<String> failure = load(registry, plugins, classLoaders, "rollback-working", WorkingPlugin.class);

        assertTrue(failure.isEmpty(), failure.orElse(""));
        assertNotNull(registry.getService(RollbackService.class));
        assertTrue(registry.createScriptApis().containsKey("workingApi"));
        assertEquals(registry.getSnippetDefinitions().size(), 1);
        assertTrue(I18nBundleRegistry.registeredBundles().stream()
                .anyMatch(bundle -> "rollback-working".equals(bundle.ownerId())));
        PluginLoader.closeClassLoaders(classLoaders);
    }

    private static Optional<String> load(PluginRegistry registry,
                                         List<DevWorkbenchPlugin> plugins,
                                         List<URLClassLoader> classLoaders,
                                         String pluginId,
                                         Class<? extends DevWorkbenchPlugin> entryClass) throws Exception {
        Path jar = Files.createTempFile("rollback-plugin", ".jar");
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(jar))) {
            // empty jar: the entry class is resolved through the parent (test) class loader
        }
        jar.toFile().deleteOnExit();
        PluginDescriptor descriptor = new PluginDescriptor(pluginId, pluginId, "1.0.0", entryClass.getName());
        return PluginLoader.loadPluginJar(jar, descriptor, registry, plugins, classLoaders, new ArrayList<>(),
                PluginLoaderRollbackTest.class.getClassLoader());
    }

    public interface RollbackService {
    }

    public static final class FailingPlugin implements DevWorkbenchPlugin {
        @Override
        public void onLoad(PluginContext context) {
            context.registerI18nBundle("rollback-messages");
            context.registerService(RollbackService.class, new RollbackService() { });
            context.registerScriptApi("rollbackApi", Object::new);
            context.registerSnippet(new SnippetDefinition("test", "Rollback", "", "1"));
            throw new IllegalStateException("simulated onLoad failure");
        }
    }

    public static final class WorkingPlugin implements DevWorkbenchPlugin {
        @Override
        public void onLoad(PluginContext context) {
            context.registerI18nBundle("working-messages");
            context.registerService(RollbackService.class, new RollbackService() { });
            context.registerScriptApi("workingApi", Object::new);
            context.registerSnippet(new SnippetDefinition("test", "Working", "", "1"));
        }
    }
}
