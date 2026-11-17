package com.laker.postman.plugin.api;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * Định nghĩa phần mở rộng của bảng điều khiển hộp công cụ.
 */
public record ToolboxContribution(
        String id,
        String displayName,
        String iconPath,
        String groupId,
        String groupDisplayName,
        Supplier<JPanel> panelSupplier,
        ClassLoader iconClassLoader
) {

    public ToolboxContribution(
            String id,
            String displayName,
            String iconPath,
            String groupId,
            String groupDisplayName,
            Supplier<JPanel> panelSupplier
    ) {
        this(id, displayName, iconPath, groupId, groupDisplayName, panelSupplier, null);
    }
}
