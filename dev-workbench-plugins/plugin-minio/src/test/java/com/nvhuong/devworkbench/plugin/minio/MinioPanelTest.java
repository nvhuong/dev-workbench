package com.nvhuong.devworkbench.plugin.minio;

import org.testng.annotations.Test;
import javax.swing.SwingUtilities;
import static org.testng.Assert.assertTrue;

public class MinioPanelTest {
    @Test
    public void buildsStorageBrowserWithoutConnecting() throws Exception {
        MinioPanel[] panel = new MinioPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new MinioPanel());
        assertTrue(panel[0].getComponentCount() > 0);
    }
}
