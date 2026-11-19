package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import com.nvhuong.devworkbench.util.I18nUtil;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertTrue;

public class RemotePanelTest {
    @Test public void buildsRemoteWorkspaceWithoutConnecting() throws Exception {
        RemotePanel[] panel = new RemotePanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new RemotePanel(PluginStorage.noop()));
        assertTrue(panel[0].getComponentCount() > 0);
    }

    /**
     * The connection form used to be forced to 310px while its labels (notably the Vietnamese ones) needed far more,
     * so the input fields were pushed under the session area and hidden.
     */
    @Test public void connectionFormControlsStayInsideSidebarAcrossLocales() throws Exception {
        Locale original = I18nUtil.currentLocale();
        try {
            for (String language : new String[]{"vi", "en"}) {
                List<String> problems = new ArrayList<>();
                SwingUtilities.invokeAndWait(() -> {
                    I18nUtil.setLocale(Locale.forLanguageTag(language));
                    RemotePanel panel = new RemotePanel(PluginStorage.noop());
                    panel.setSize(1200, 720);
                    layoutRecursively(panel);
                    JPanel found = null;
                    for (Component child : panel.getComponents()) {
                        if (child instanceof JPanel candidate && !(child instanceof JTabbedPane)) {
                            found = candidate;
                        }
                    }
                    assertTrue(found != null, "connection form not found");
                    JPanel sidebar = found;
                    Rectangle visible = new Rectangle(0, 0, sidebar.getWidth(), sidebar.getHeight());
                    collectControls(sidebar).forEach(control -> {
                        Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), sidebar);
                        if (!visible.contains(bounds)) {
                            problems.add(control.getClass().getSimpleName() + " " + bounds + " outside " + visible);
                        } else if (bounds.width < control.getMinimumSize().width && control.getMinimumSize().width < 80) {
                            problems.add(control.getClass().getSimpleName() + " squeezed to " + bounds.width + "px");
                        }
                    });
                });
                assertTrue(problems.isEmpty(), language + ": " + problems);
            }
        } finally {
            I18nUtil.setLocale(original);
        }
    }

    private static List<JComponent> collectControls(Container container) {
        List<JComponent> controls = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof javax.swing.text.JTextComponent || child instanceof javax.swing.AbstractButton
                    || child instanceof javax.swing.JComboBox || child instanceof javax.swing.JSpinner) {
                controls.add((JComponent) child);
            } else if (child instanceof JPanel nested) {
                controls.addAll(collectControls(nested));
            }
        }
        return controls;
    }

    private static void layoutRecursively(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
