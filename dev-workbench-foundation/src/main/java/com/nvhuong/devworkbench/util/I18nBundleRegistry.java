package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class I18nBundleRegistry {

    private static final ConcurrentMap<BundleIdentity, I18nBundleDescriptor> BUNDLES = new ConcurrentHashMap<>();

    public static void registerBundle(String ownerId, String bundleName, ClassLoader classLoader) {
        ClassLoader resolvedClassLoader = classLoader != null
                ? classLoader
                : I18nBundleRegistry.class.getClassLoader();
        I18nBundleDescriptor descriptor = new I18nBundleDescriptor(ownerId, bundleName, resolvedClassLoader);
        BUNDLES.putIfAbsent(new BundleIdentity(descriptor.bundleName(), descriptor.classLoader()), descriptor);
    }

    /** Removes bundles an owner registered through the given class loader (e.g. a plugin whose load failed). */
    public static void unregisterBundles(String ownerId, ClassLoader classLoader) {
        BUNDLES.values().removeIf(descriptor -> descriptor.ownerId().equals(ownerId)
                && descriptor.classLoader() == classLoader);
    }

    public static List<I18nBundleDescriptor> registeredBundles() {
        return List.copyOf(BUNDLES.values());
    }

    private record BundleIdentity(String bundleName, ClassLoader classLoader) {
    }
}
