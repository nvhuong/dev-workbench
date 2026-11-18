package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.common.animation.ComponentSnapshotTransition;
import com.nvhuong.devworkbench.common.component.placeholder.RequestEditorPlaceholderPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;

/**
 * Yêu cầu trình soạn thảo khởi tạo bộ điều khiển một cách lười biếng.
 * <p>
 * Khởi động phục hồi rất nhiều Tab Trang giữ chỗ nhẹ được hiển thị đầu tiên và trình chỉnh sửa thực được tạo sau khi người dùng kích hoạt nó để tránh bảng điều khiển chính chịu trách nhiệm theo dõi và chuyển đổi chi tiết.
 */
@RequiredArgsConstructor
final class RequestEditorDeferredInitializationController {
    private final JPanel owner;
    private final BooleanSupplier placeholderActive;
    private final Runnable activationAction;
    private final ComponentSnapshotTransition transition;

    RequestEditorDeferredInitializationController(JPanel owner,
                                                  BooleanSupplier placeholderActive,
                                                  Runnable activationAction) {
        this(owner, placeholderActive, activationAction, new ComponentSnapshotTransition(owner));
    }

    void installPlaceholder() {
        owner.removeAll();
        JComponent placeholder = new RequestEditorPlaceholderPanel();
        attachActivationListeners(placeholder);
        owner.add(placeholder, BorderLayout.CENTER);
    }

    ComponentSnapshotTransition.CapturedSnapshot capturePlaceholderSnapshot() {
        if (!placeholderActive.getAsBoolean()) {
            return null;
        }
        Component currentContent = owner.getComponentCount() > 0 ? owner.getComponent(0) : null;
        if (!(currentContent instanceof JComponent placeholder) || !placeholder.isShowing()) {
            return null;
        }
        return transition.captureSnapshot(placeholder);
    }

    void startTransition(ComponentSnapshotTransition.CapturedSnapshot placeholderSnapshot) {
        if (placeholderSnapshot != null) {
            transition.start(placeholderSnapshot);
        }
    }

    private void attachActivationListeners(Component component) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activateIfNeeded();
            }
        };
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activateIfNeeded();
            }
        };
        attachActivationListeners(component, mouseAdapter, focusAdapter);
    }

    private void attachActivationListeners(Component component, MouseAdapter mouseAdapter, FocusAdapter focusAdapter) {
        component.addMouseListener(mouseAdapter);
        component.addFocusListener(focusAdapter);
        if (component instanceof JComponent jComponent) {
            jComponent.setFocusable(true);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachActivationListeners(child, mouseAdapter, focusAdapter);
            }
        }
    }

    private void activateIfNeeded() {
        if (placeholderActive.getAsBoolean()) {
            activationAction.run();
        }
    }
}
