package com.nvhuong.devworkbench.panel.workspace.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.button.PrimaryButton;
import com.nvhuong.devworkbench.model.GitOperation;
import org.testng.annotations.Test;

import javax.swing.JButton;
import java.awt.Color;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class GitOperationDialogTest {

    @Test
    public void executeButtonShouldUseOperationColoredPrimaryHoverStates() {
        JButton button = GitOperationDialog.createExecuteButton(GitOperation.COMMIT);
        Color baseColor = GitOperationPresentation.getColor(GitOperation.COMMIT);

        assertTrue(button instanceof PrimaryButton);
        assertEquals(button.getClientProperty("baseColor"), baseColor);
        assertNotEquals(button.getClientProperty("hoverColor"), baseColor);
        assertNotEquals(button.getClientProperty("pressColor"), baseColor);
        assertTrue(button.isContentAreaFilled());
        assertTrue(button.isFocusPainted());

        String style = Objects.toString(button.getClientProperty(FlatClientProperties.STYLE), "");
        assertTrue(style.contains("background: " + toStyleColor(baseColor)));
    }

    private static String toStyleColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
