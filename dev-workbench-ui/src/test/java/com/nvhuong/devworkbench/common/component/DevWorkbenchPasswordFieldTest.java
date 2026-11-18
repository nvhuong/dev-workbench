package com.nvhuong.devworkbench.common.component;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class DevWorkbenchPasswordFieldTest {

    @Test
    public void shouldExposePasswordTextAndRevealButtonFlag() {
        DevWorkbenchPasswordField field = new DevWorkbenchPasswordField(12, "Password");

        field.setPasswordText("secret");
        field.setShowRevealButton(false);

        assertEquals(field.getPasswordText(), "secret");
        assertFalse(field.isShowRevealButton());
    }
}
