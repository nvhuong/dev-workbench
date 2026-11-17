package com.laker.postman.panel.toolbox;

import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class DbPortalPanelTest extends AbstractSwingUiTest {
    @Test
    public void initializesJdbcWorkbenchWithoutOpeningAConnection() throws Exception {
        DbPortalPanel[] panel = new DbPortalPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new DbPortalPanel());

        assertNotNull(panel[0]);
        assertNotNull(findComponent(panel[0], JTree.class));
        assertNotNull(findComponent(panel[0], RSyntaxTextArea.class));
        assertTrue(panel[0].isVisible() || panel[0].getComponentCount() > 0);
    }

    private static <T extends Component> T findComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findComponent(child, type);
                if (match != null) return match;
            }
        }
        return null;
    }
}
