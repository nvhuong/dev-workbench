package com.nvhuong.devworkbench.common.component.setting;

import org.testng.annotations.Test;

import java.awt.Dimension;

import static org.testng.Assert.assertTrue;

public class SettingsHintLabelTest {

    private static final int HINT_WIDTH = 320;

    @Test
    public void hintShouldWrapOnWordBoundaries() {
        SettingsHintLabel label = new SettingsHintLabel(
                "Disabled by default. Enable it only when outbound access is restricted or requests need a corporate proxy.",
                320
        );

        assertTrue(label.getLineWrap(),
                "Hint text should wrap within the configured width");
        assertTrue(label.getWrapStyleWord(),
                "English hint text should wrap on word boundaries instead of splitting words");
    }

    @Test
    public void longHintShouldWrapWithinConfiguredWidth() {
        SettingsHintLabel label = new SettingsHintLabel("", HINT_WIDTH);
        String hint = textWiderThanTwoLines(label, sidebarHint());
        label.setPlainText(hint);

        Dimension preferredSize = label.getPreferredSize();

        assertTrue(label.getText().equals(hint),
                "Hint text should remain plain text instead of relying on Swing HTML rendering");
        assertTrue(preferredSize.width <= HINT_WIDTH + 4,
                "Wrapped hint preferred width should stay within configured width");
        assertTrue(preferredSize.height > label.getFontMetrics(label.getFont()).getHeight(),
                "Long hint should wrap to multiple lines instead of clipping horizontally");
    }

    @Test
    public void mixedChineseEnglishHintShouldWrapInsteadOfUsingHtmlLabelRenderer() {
        SettingsHintLabel label = new SettingsHintLabel(
                "Khả năng chứng chỉ ứng dụng khách đã được chia thành các plugin tùy chọn. Cài đặt chính thức Client Certificate Sau khi plug-in, bạn có thể tiếp tục cấu hình mTLS certificate and automatically applied to the request.",
                360
        );

        Dimension preferredSize = label.getPreferredSize();

        assertTrue(preferredSize.width <= 364,
                "Mixed Chinese/English hint preferred width should stay within configured width");
        assertTrue(preferredSize.height > label.getFontMetrics(label.getFont()).getHeight(),
                "Mixed Chinese/English hint should wrap to multiple lines");
    }

    private static String sidebarHint() {
        return "Kéo các mục trong danh sách để điều chỉnh thứ tự; nhấp vào hộp kiểm ở bên trái hoặc nhấn thanh dấu cách để ẩn hoặc hiển thị menu. Giữ ít nhất một menu.";
    }

    private static String textWiderThanTwoLines(SettingsHintLabel label, String sample) {
        StringBuilder text = new StringBuilder(sample);
        int minimumWidth = HINT_WIDTH * 2;
        while (label.getFontMetrics(label.getFont()).stringWidth(text.toString()) <= minimumWidth) {
            text.append(sample);
        }
        return text.toString();
    }
}
