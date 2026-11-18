package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.SearchTextField;
import com.nvhuong.devworkbench.common.component.ToolWindowSidebarToolbar;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ToolboxPanelLayoutTest extends AbstractSwingUiTest {

    @Test
    public void toolboxSidebarShouldDefaultNarrowerThanGenericSidePanels() throws Exception {
        ToolboxPanel[] panelHolder = new ToolboxPanel[1];
        SwingUtilities.invokeAndWait(() -> panelHolder[0] = createInitializedToolboxPanel());

        JSplitPane splitPane = findSplitPane(panelHolder[0]);

        assertNotNull(splitPane);
        assertEquals(splitPane.getDividerLocation(), 240);
        assertTrue(splitPane.getDividerLocation() < AppToolWindowChrome.DEFAULT_SIDE_WIDTH);
    }

    @Test
    public void toolboxSearchFieldShouldUseFiniteCompactSidebarWidth() throws Exception {
        ToolboxPanel[] panelHolder = new ToolboxPanel[1];
        SwingUtilities.invokeAndWait(() -> panelHolder[0] = createInitializedToolboxPanel());

        SearchTextField searchField = findComponent(panelHolder[0], SearchTextField.class);

        assertNotNull(searchField);
        assertEquals(searchField.getPreferredSize().width, 220);
        assertEquals(searchField.getPreferredSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(searchField.getMaximumSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertTrue(searchField.getPreferredSize().width < Integer.MAX_VALUE);
    }

    private static ToolboxPanel createInitializedToolboxPanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            ToolboxPanel panel = new ToolboxPanel();
            panel.initializeSingletonUi();
            return panel;
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static JSplitPane findSplitPane(Component component) {
        if (component instanceof JSplitPane splitPane) {
            return splitPane;
        }
        return findComponent(component, JSplitPane.class);
    }

    private static <T extends Component> T findComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findComponent(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
