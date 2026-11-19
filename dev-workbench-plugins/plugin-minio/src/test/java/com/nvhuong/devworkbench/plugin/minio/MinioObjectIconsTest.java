package com.nvhuong.devworkbench.plugin.minio;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class MinioObjectIconsTest {

    @Test
    public void objectsGetAnIconMatchingTheirType() {
        assertEquals(MinioObjectIcons.iconPath("reports/2026/photo.PNG"), MinioObjectIcons.IMAGE);
        assertEquals(MinioObjectIcons.iconPath("backup.tar.gz"), MinioObjectIcons.ARCHIVE);
        assertEquals(MinioObjectIcons.iconPath("config/app.json"), MinioObjectIcons.CODE);
        assertEquals(MinioObjectIcons.iconPath("notes/readme.md"), MinioObjectIcons.TEXT);
        assertEquals(MinioObjectIcons.iconPath("blob"), MinioObjectIcons.FILE);
        assertEquals(MinioObjectIcons.iconPath(".hidden"), MinioObjectIcons.FILE);
        for (String path : new String[]{MinioObjectIcons.IMAGE, MinioObjectIcons.ARCHIVE, MinioObjectIcons.CODE,
                MinioObjectIcons.TEXT, MinioObjectIcons.FILE}) {
            Icon icon = MinioObjectIcons.icon(path);
            assertTrue(icon instanceof FlatSVGIcon svg && svg.hasFound(), "icon resource missing: " + path);
        }
    }

    @Test
    public void objectColumnShowsTheIcon() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MinioPanel panel = new MinioPanel();
            JTable objects = field(panel, "objects");
            DefaultTableModel model = field(panel, "objectModel");
            model.addRow(new Object[]{"images/logo.svg", "1 KB", ""});
            Component cell = objects.getCellRenderer(0, 0).getTableCellRendererComponent(objects,
                    model.getValueAt(0, 0), false, false, 0, 0);
            assertTrue(cell instanceof JLabel, String.valueOf(cell));
            assertNotNull(((JLabel) cell).getIcon(), "object cell has no icon");
            assertEquals(((JLabel) cell).getText(), "images/logo.svg");
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) {
        try {
            java.lang.reflect.Field field = MinioPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
