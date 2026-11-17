package com.laker.postman.panel.collections.editor.request;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class RequestPreparationFeedbackPresenterTest extends AbstractSwingUiTest {

    @Test(description = "Dọn dẹp URL Chỉ nên xóa khi xác minh phản hồi outline，Nó không nên được thay đổi tooltip")
    public void testClearUrlValidationFeedback_ShouldOnlyClearOutline() {
        RequestPreparationFeedbackPresenter presenter = new RequestPreparationFeedbackPresenter();
        JTextField urlField = new JTextField("{{test}}");
        urlField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        urlField.setToolTipText("keep me");

        presenter.clearUrlValidationFeedback(urlField);

        assertNull(urlField.getClientProperty(FlatClientProperties.OUTLINE));
        assertEquals(urlField.getToolTipText(), "keep me");
    }
}
