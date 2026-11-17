package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ResponseTooltipBuilderThemeTest extends AbstractSwingUiTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SUCCESS);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void compressionBadgeBackgroundShouldUseSemanticSuccessToken() throws Exception {
        Color success = new Color(31, 132, 33);
        UIManager.put(ThemeColors.SUCCESS, success);
        ResponseSizeCalculator.SizeInfo sizeInfo = new ResponseSizeCalculator.SizeInfo(
                "10 KB", success, success, true, 50, 1024, "gzip");

        JPanel wrapper = invokeCompressionBadge(sizeInfo);
        JPanel badge = (JPanel) wrapper.getComponent(0);

        assertEquals(badge.getBackground(), ModernColors.withAlpha(success, 24));
    }

    @Test
    public void sizeTooltipShouldUseCurrentLocaleLabels() {
        Locale previousLocale = I18nUtil.currentLocale();
        try {
            I18nUtil.setLocale(Locale.forLanguageTag("vi"));
            HttpEventInfo info = new HttpEventInfo();
            info.setHeaderBytesReceived(181);
            info.setBodyBytesReceived(492);
            info.setHeaderBytesSent(119);

            ResponseSizeCalculator.SizeInfo sizeInfo = ResponseSizeCalculator.calculate(492, info, null);
            JPanel tooltip = ResponseTooltipBuilder.buildSizeTooltipPanel(492, info, sizeInfo);

            List<String> labels = collectLabels(tooltip);
            assertTrue(labels.contains("phản hồi"));
            assertTrue(labels.contains("tiêu đề phản hồi"));
            assertTrue(labels.contains("cơ quan phản ứng"));
            assertTrue(labels.contains("yêu cầu"));
            assertTrue(labels.contains("Tiêu đề yêu cầu"));
            assertTrue(labels.contains("Request body"));
        } finally {
            I18nUtil.setLocale(previousLocale);
        }
    }

    private JPanel invokeCompressionBadge(ResponseSizeCalculator.SizeInfo sizeInfo) throws Exception {
        Method method = ResponseTooltipBuilder.class.getDeclaredMethod(
                "compressionBadge", ResponseSizeCalculator.SizeInfo.class);
        method.setAccessible(true);
        return (JPanel) method.invoke(null, sizeInfo);
    }

    private List<String> collectLabels(Container container) {
        List<String> labels = new ArrayList<>();
        collectLabels(container, labels);
        return labels;
    }

    private void collectLabels(Container container, List<String> labels) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label) {
                labels.add(label.getText());
            }
            if (child instanceof Container nested) {
                collectLabels(nested, labels);
            }
        }
    }
}
