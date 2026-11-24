package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.service.setting.ShortcutManager;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class RequestEditorEmptyStatePanelTest extends AbstractSwingUiTest {

    @Test
    public void hintButtonShouldRunActionFromKeyboard() {
        AtomicInteger invocations = new AtomicInteger();
        RequestEditorEmptyStatePanel panel = new RequestEditorEmptyStatePanel(invocations::incrementAndGet);
        JComponent hintButton = findFocusableLabel(panel);
        assertNotNull(hintButton);

        Object actionKey = hintButton.getInputMap(JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        hintButton.getActionMap().get(actionKey)
                .actionPerformed(new ActionEvent(hintButton, ActionEvent.ACTION_PERFORMED, null));

        assertEquals(invocations.get(), 1);
    }

    @Test
    public void refreshShortcutHintsShouldReflectUpdatedShortcuts() {
        ShortcutManager.ShortcutConfig saveConfig = ShortcutManager.getShortcut(ShortcutManager.SAVE_REQUEST);
        int previousKeyCode = saveConfig.getKeyCode();
        int previousModifiers = saveConfig.getModifiers();
        try {
            ShortcutManager.clearShortcut(ShortcutManager.SAVE_REQUEST);
            RequestEditorEmptyStatePanel panel = new RequestEditorEmptyStatePanel(() -> {
            });
            int labelsWithoutSave = countLabels(panel);

            ShortcutManager.setShortcut(ShortcutManager.SAVE_REQUEST, previousKeyCode == 0 ? 'S' : previousKeyCode,
                    previousModifiers);
            panel.refreshShortcutHints();

            assertEquals(countLabels(panel), labelsWithoutSave + 1);
        } finally {
            ShortcutManager.setShortcut(ShortcutManager.SAVE_REQUEST, previousKeyCode, previousModifiers);
        }
    }

    @Test
    public void textColorsShouldFollowThemeSwitch() throws Exception {
        LookAndFeel original = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            RequestEditorEmptyStatePanel panel = new RequestEditorEmptyStatePanel(() -> {
            });
            JLabel title = findLabel(panel, I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
            assertNotNull(title);
            Color lightTitle = title.getForeground();

            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            SwingUtilities.updateComponentTreeUI(panel);

            assertEquals(title.getForeground(), RequestEditorEmptyStateTheme.titleForeground());
            assertNotEquals(title.getForeground(), lightTitle, "title must not keep the light theme color");
            JComponent hint = findFocusableLabel(panel);
            assertEquals(hint.getForeground(), RequestEditorEmptyStateTheme.hintForeground());
        } finally {
            UIManager.setLookAndFeel(original);
        }
    }

    private static JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JComponent findFocusableLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label
                    && label.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) != null) {
                return label;
            }
            if (component instanceof Container child) {
                JComponent found = findFocusableLabel(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int countLabels(Container container) {
        int count = 0;
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                count++;
            }
            if (component instanceof Container child) {
                count += countLabels(child);
            }
        }
        return count;
    }
}
