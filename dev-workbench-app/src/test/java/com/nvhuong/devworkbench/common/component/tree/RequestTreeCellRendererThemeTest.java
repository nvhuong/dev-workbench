package com.nvhuong.devworkbench.common.component.tree;

import com.nvhuong.devworkbench.common.constants.ThemeColors;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Map;

import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.remember;
import static com.nvhuong.devworkbench.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertTrue;

public class RequestTreeCellRendererThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void groupNodeTextShouldUseSemanticTextPrimaryColor() {
        UIManager.put(ThemeColors.TEXT_PRIMARY, new Color(1, 2, 3));
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Workspace"));

        RequestTreeCellRenderer renderer = new RequestTreeCellRenderer();
        renderer.getTreeCellRendererComponent(new JTree(groupNode), groupNode, false, false, true, 0, false);

        assertTrue(renderer.getText().contains("#010203"));
    }
}
