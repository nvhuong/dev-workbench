package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import org.testng.annotations.Test;
import javax.swing.SwingUtilities;
import static org.testng.Assert.assertTrue;

public class RemotePanelTest {
    @Test public void buildsRemoteWorkspaceWithoutConnecting() throws Exception {
        RemotePanel[] panel = new RemotePanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new RemotePanel(PluginStorage.noop()));
        assertTrue(panel[0].getComponentCount() > 0);
    }
}
