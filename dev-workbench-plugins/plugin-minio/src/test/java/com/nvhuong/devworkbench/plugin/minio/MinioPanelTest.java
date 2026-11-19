package com.nvhuong.devworkbench.plugin.minio;

import com.nvhuong.devworkbench.util.I18nUtil;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertTrue;

public class MinioPanelTest {
    @Test
    public void buildsStorageBrowserWithoutConnecting() throws Exception {
        MinioPanel[] panel = new MinioPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new MinioPanel());
        assertTrue(panel[0].getComponentCount() > 0);
    }

    /**
     * The access key label used to land in a growing column, squeezing the access key field into a narrow box at the
     * far right; the bucket title was also cut to "Buc..." by its action buttons.
     */
    @Test
    public void connectionFieldsAndTitlesAreFullyVisible() throws Exception {
        Locale original = I18nUtil.currentLocale();
        try {
            for (String language : new String[]{"vi", "en"}) {
                for (int width : new int[]{1200, 900}) {
                    List<String> problems = new ArrayList<>();
                    SwingUtilities.invokeAndWait(() -> {
                        I18nUtil.setLocale(Locale.forLanguageTag(language));
                        MinioPanel panel = new MinioPanel();
                        panel.setSize(width, 720);
                        layoutRecursively(panel);
                        JComponent endpoint = field(panel, "endpoint");
                        JComponent accessKey = field(panel, "accessKey");
                        JComponent secretKey = field(panel, "secretKey");
                        if (accessKey.getWidth() < endpoint.getWidth() * 0.6) {
                            problems.add("access key field " + accessKey.getWidth() + "px vs endpoint " + endpoint.getWidth() + "px");
                        }
                        if (Math.abs(secretKey.getX() - endpoint.getX()) > 1) {
                            problems.add("secret key not aligned with endpoint");
                        }
                        collectLabels(panel).forEach(label -> {
                            if (label.getWidth() + 1 < label.getPreferredSize().width) {
                                problems.add("label '" + label.getText() + "' cut to " + label.getWidth()
                                        + "px of " + label.getPreferredSize().width + "px");
                            }
                        });
                    });
                    assertTrue(problems.isEmpty(), language + "@" + width + ": " + problems);
                }
            }
        } finally {
            I18nUtil.setLocale(original);
        }
    }

    private static List<JLabel> collectLabels(Container container) {
        List<JLabel> labels = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && label.isVisible() && label.getText() != null && !label.getText().isBlank()) {
                labels.add(label);
            } else if (child instanceof Container nested && !(child instanceof javax.swing.JTable)
                    && !(child instanceof javax.swing.JTree) && !(child instanceof javax.swing.JComboBox)) {
                labels.addAll(collectLabels(nested));
            }
        }
        return labels;
    }

    private static JComponent field(MinioPanel panel, String name) {
        try {
            Field field = MinioPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return (JComponent) field.get(panel);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
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
