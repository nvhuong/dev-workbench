package com.nvhuong.devworkbench.common.themes;

import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Registry of application themes. Keep theme metadata here instead of scattering light/dark strings.
 */
@UtilityClass
public class ThemeRegistry {

    public static final String LIGHT_THEME_ID = "light";
    public static final String DARK_THEME_ID = "dark";
    public static final String DEFAULT_THEME_ID = LIGHT_THEME_ID;

    private static final String FLAT_LAF_DEFAULTS_SOURCE = "com.nvhuong.devworkbench.common.themes";

    private static final List<ThemeDescriptor> THEMES = List.of(
            new ThemeDescriptor(
                    LIGHT_THEME_ID,
                    MessageKeys.MENU_THEME_LIGHT,
                    false,
                    DevWorkbenchLightLaf::setup,
                    "/themes/devworkbench-light.xml",
                    "/org/fife/ui/rsyntaxtextarea/themes/vs.xml",
                    FLAT_LAF_DEFAULTS_SOURCE
            ),
            new ThemeDescriptor(
                    DARK_THEME_ID,
                    MessageKeys.MENU_THEME_DARK,
                    true,
                    DevWorkbenchDarkLaf::setup,
                    "/themes/devworkbench-dark.xml",
                    "/org/fife/ui/rsyntaxtextarea/themes/dark.xml",
                    FLAT_LAF_DEFAULTS_SOURCE
            )
    );

    public static List<ThemeDescriptor> all() {
        return THEMES;
    }

    public static ThemeDescriptor defaultTheme() {
        return THEMES.get(0);
    }

    public static Optional<ThemeDescriptor> find(String id) {
        String normalizedId = normalizeId(id);
        return THEMES.stream()
                .filter(theme -> theme.id().equals(normalizedId))
                .findFirst();
    }

    public static ThemeDescriptor getOrDefault(String id) {
        return find(id).orElse(defaultTheme());
    }

    public static String normalizeId(String id) {
        return id == null ? DEFAULT_THEME_ID : id.trim().toLowerCase(Locale.ROOT);
    }
}
