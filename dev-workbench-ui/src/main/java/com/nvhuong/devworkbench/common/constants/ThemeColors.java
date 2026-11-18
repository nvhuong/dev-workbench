package com.nvhuong.devworkbench.common.constants;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * DevWorkbench semantic color tokens backed by FlatLaf UIDefaults.
 * <p>
 * Keep reusable UI code reading colors from this class instead of branching on
 * light/dark mode. Theme properties define the real values.
 */
@UtilityClass
public class ThemeColors {

    public static final String PRIMARY = "DevWorkbench.color.primary";
    public static final String PRIMARY_DARK = "DevWorkbench.color.primary.dark";
    public static final String PRIMARY_DARKER = "DevWorkbench.color.primary.darker";
    public static final String PRIMARY_LIGHT = "DevWorkbench.color.primary.light";
    public static final String PRIMARY_LIGHTER = "DevWorkbench.color.primary.lighter";
    public static final String SECONDARY = "DevWorkbench.color.secondary";
    public static final String SECONDARY_DARK = "DevWorkbench.color.secondary.dark";
    public static final String SECONDARY_LIGHT = "DevWorkbench.color.secondary.light";
    public static final String SECONDARY_LIGHTER = "DevWorkbench.color.secondary.lighter";
    public static final String ACCENT = "DevWorkbench.color.accent";
    public static final String ACCENT_LIGHT = "DevWorkbench.color.accent.light";
    public static final String SUCCESS = "DevWorkbench.color.success";
    public static final String SUCCESS_DARK = "DevWorkbench.color.success.dark";
    public static final String ERROR = "DevWorkbench.color.error";
    public static final String ERROR_DARK = "DevWorkbench.color.error.dark";
    public static final String ERROR_DARKER = "DevWorkbench.color.error.darker";
    public static final String WARNING = "DevWorkbench.color.warning";
    public static final String WARNING_DARK = "DevWorkbench.color.warning.dark";
    public static final String WARNING_DARKER = "DevWorkbench.color.warning.darker";
    public static final String INFO = "DevWorkbench.color.info";
    public static final String NEUTRAL = "DevWorkbench.color.neutral";
    public static final String NEUTRAL_DARK = "DevWorkbench.color.neutral.dark";
    public static final String NEUTRAL_DARKER = "DevWorkbench.color.neutral.darker";
    public static final String GIT_COMMIT = "DevWorkbench.color.git.commit";
    public static final String GIT_PUSH = "DevWorkbench.color.git.push";
    public static final String GIT_PULL = "DevWorkbench.color.git.pull";
    public static final String VARIABLE_CONTEXT = "DevWorkbench.color.variable.context";
    public static final String VARIABLE_ITERATION_DATA = "DevWorkbench.color.variable.iterationData";
    public static final String VARIABLE_GROUP = "DevWorkbench.color.variable.group";
    public static final String VARIABLE_ENVIRONMENT = "DevWorkbench.color.variable.environment";
    public static final String VARIABLE_GLOBAL = "DevWorkbench.color.variable.global";
    public static final String VARIABLE_BUILT_IN = "DevWorkbench.color.variable.builtIn";
    public static final String HTTP_METHOD_GET = "DevWorkbench.http.method.get";
    public static final String HTTP_METHOD_POST = "DevWorkbench.http.method.post";
    public static final String HTTP_METHOD_PUT = "DevWorkbench.http.method.put";
    public static final String HTTP_METHOD_PATCH = "DevWorkbench.http.method.patch";
    public static final String HTTP_METHOD_DELETE = "DevWorkbench.http.method.delete";
    public static final String HTTP_METHOD_DEFAULT = "DevWorkbench.http.method.default";
    public static final String HTTP_PROTOCOL_WS = "DevWorkbench.http.protocol.ws";
    public static final String HTTP_PROTOCOL_SSE = "DevWorkbench.http.protocol.sse";

    public static final String TEXT_PRIMARY = "DevWorkbench.text.primary";
    public static final String TEXT_SECONDARY = "DevWorkbench.text.secondary";
    public static final String TEXT_HINT = "DevWorkbench.text.hint";
    public static final String TEXT_DISABLED = "DevWorkbench.text.disabled";
    public static final String TEXT_INVERSE = "DevWorkbench.text.inverse";

    public static final String BACKGROUND = "DevWorkbench.background";
    public static final String SURFACE = "DevWorkbench.surface";
    public static final String WINDOW_CHROME_BACKGROUND = "DevWorkbench.window.chrome.background";
    public static final String DIALOG_CHROME_BACKGROUND = "DevWorkbench.dialog.chrome.background";
    public static final String INPUT_BACKGROUND = "DevWorkbench.input.background";
    public static final String TAB_BACKGROUND = "DevWorkbench.tab.background";
    public static final String TAB_SELECTED_BACKGROUND = "DevWorkbench.tab.selected.background";
    public static final String TAB_HOVER_BACKGROUND = "DevWorkbench.tab.hover.background";
    public static final String TAB_SEPARATOR = "DevWorkbench.tab.separator";
    public static final String HOVER_BACKGROUND = "DevWorkbench.hover.background";
    public static final String SELECTION_BACKGROUND = "DevWorkbench.selection.background";
    public static final String BUTTON_PRESSED_BACKGROUND = "DevWorkbench.button.pressed.background";
    public static final String BUTTON_DISABLED_BACKGROUND = "DevWorkbench.button.disabled.background";
    public static final String BORDER_LIGHT = "DevWorkbench.border.light";
    public static final String BORDER_MEDIUM = "DevWorkbench.border.medium";
    public static final String DIVIDER = "DevWorkbench.divider";
    public static final String EMPTY_CELL_BACKGROUND = "DevWorkbench.emptyCell.background";
    public static final String SCROLLBAR_TRACK = "DevWorkbench.scrollbar.track";
    public static final String SCROLLBAR_THUMB = "DevWorkbench.scrollbar.thumb";
    public static final String SCROLLBAR_THUMB_HOVER = "DevWorkbench.scrollbar.thumbHover";
    public static final String WARNING_BACKGROUND = "DevWorkbench.warning.background";
    public static final String WARNING_BORDER = "DevWorkbench.warning.border";
    public static final String SEARCH_HIGHLIGHT_BACKGROUND = "DevWorkbench.search.highlight.background";
    public static final String SEARCH_CURRENT_HIGHLIGHT_BACKGROUND = "DevWorkbench.search.currentHighlight.background";
    public static final String SPLASH_GRADIENT_START = "DevWorkbench.splash.gradient.start";
    public static final String SPLASH_GRADIENT_END = "DevWorkbench.splash.gradient.end";

    public static final String CONSOLE_TEXT_AREA_BACKGROUND = "DevWorkbench.console.textArea.background";
    public static final String CONSOLE_TEXT = "DevWorkbench.console.text";
    public static final String CONSOLE_INFO = "DevWorkbench.console.info";
    public static final String CONSOLE_DEBUG = "DevWorkbench.console.debug";
    public static final String CONSOLE_WARN = "DevWorkbench.console.warn";
    public static final String CONSOLE_ERROR = "DevWorkbench.console.error";
    public static final String CONSOLE_CLASS_NAME = "DevWorkbench.console.className";
    public static final String CONSOLE_METHOD_NAME = "DevWorkbench.console.methodName";
    public static final String CONSOLE_TOOLBAR_BACKGROUND = "DevWorkbench.console.toolbar.background";
    public static final String CONSOLE_TOOLBAR_BORDER = "DevWorkbench.console.toolbar.border";
    public static final String CONSOLE_SELECTION_BACKGROUND = "DevWorkbench.console.selection.background";

    public static final String NOTIFICATION_BACKGROUND = "DevWorkbench.notification.background";
    public static final String NOTIFICATION_BORDER = "DevWorkbench.notification.border";
    public static final String NOTIFICATION_DIVIDER = "DevWorkbench.notification.divider";
    public static final String NOTIFICATION_BODY_FOREGROUND = "DevWorkbench.notification.bodyForeground";

    public static final List<String> REQUIRED_KEYS = List.of(
            PRIMARY,
            PRIMARY_DARK,
            PRIMARY_DARKER,
            PRIMARY_LIGHT,
            PRIMARY_LIGHTER,
            SECONDARY,
            SECONDARY_DARK,
            SECONDARY_LIGHT,
            SECONDARY_LIGHTER,
            ACCENT,
            ACCENT_LIGHT,
            SUCCESS,
            SUCCESS_DARK,
            ERROR,
            ERROR_DARK,
            ERROR_DARKER,
            WARNING,
            WARNING_DARK,
            WARNING_DARKER,
            INFO,
            NEUTRAL,
            NEUTRAL_DARK,
            NEUTRAL_DARKER,
            GIT_COMMIT,
            GIT_PUSH,
            GIT_PULL,
            VARIABLE_CONTEXT,
            VARIABLE_ITERATION_DATA,
            VARIABLE_GROUP,
            VARIABLE_ENVIRONMENT,
            VARIABLE_GLOBAL,
            VARIABLE_BUILT_IN,
            HTTP_METHOD_GET,
            HTTP_METHOD_POST,
            HTTP_METHOD_PUT,
            HTTP_METHOD_PATCH,
            HTTP_METHOD_DELETE,
            HTTP_METHOD_DEFAULT,
            HTTP_PROTOCOL_WS,
            HTTP_PROTOCOL_SSE,
            TEXT_PRIMARY,
            TEXT_SECONDARY,
            TEXT_HINT,
            TEXT_DISABLED,
            TEXT_INVERSE,
            BACKGROUND,
            SURFACE,
            WINDOW_CHROME_BACKGROUND,
            DIALOG_CHROME_BACKGROUND,
            INPUT_BACKGROUND,
            TAB_BACKGROUND,
            TAB_SELECTED_BACKGROUND,
            TAB_HOVER_BACKGROUND,
            TAB_SEPARATOR,
            HOVER_BACKGROUND,
            SELECTION_BACKGROUND,
            BUTTON_PRESSED_BACKGROUND,
            BUTTON_DISABLED_BACKGROUND,
            BORDER_LIGHT,
            BORDER_MEDIUM,
            DIVIDER,
            EMPTY_CELL_BACKGROUND,
            SCROLLBAR_TRACK,
            SCROLLBAR_THUMB,
            SCROLLBAR_THUMB_HOVER,
            WARNING_BACKGROUND,
            WARNING_BORDER,
            SEARCH_HIGHLIGHT_BACKGROUND,
            SEARCH_CURRENT_HIGHLIGHT_BACKGROUND,
            SPLASH_GRADIENT_START,
            SPLASH_GRADIENT_END,
            CONSOLE_TEXT_AREA_BACKGROUND,
            CONSOLE_TEXT,
            CONSOLE_INFO,
            CONSOLE_DEBUG,
            CONSOLE_WARN,
            CONSOLE_ERROR,
            CONSOLE_CLASS_NAME,
            CONSOLE_METHOD_NAME,
            CONSOLE_TOOLBAR_BACKGROUND,
            CONSOLE_TOOLBAR_BORDER,
            CONSOLE_SELECTION_BACKGROUND,
            NOTIFICATION_BACKGROUND,
            NOTIFICATION_BORDER,
            NOTIFICATION_DIVIDER,
            NOTIFICATION_BODY_FOREGROUND
    );

    public static Color color(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return explicit(color != null ? color : fallback);
    }

    public static Color explicit(Color color) {
        return color == null ? null : new Color(color.getRGB(), true);
    }
}
