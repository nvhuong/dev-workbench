package com.nvhuong.devworkbench.panel.collections;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;

@UtilityClass
public class RequestSelectionDialogSupport {

    /**
     * Hộp thoại yêu cầu nhiều lựa chọn bật lên và lệnh gọi lại sẽ trả về hộp thoại đã chọnHttpRequestItemdanh sách
     */
    public static void showMultiSelectRequestDialog(Consumer<List<HttpRequestItem>> onSelected) {
        CollectionTreePanel requestCollectionsLeftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        JDialog dialog = new JDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_TITLE), true);
        dialog.setSize(400, 500);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        ToolWindowSurfaceStyle.applyDialogSurface((JPanel) dialog.getContentPane());

        JTree tree = requestCollectionsLeftPanel.createRequestSelectionTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root != null && root.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
            tree.expandPath(new TreePath(firstChild.getPath()));
        }

        JScrollPane treeScroll = new JScrollPane(tree);
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(treeScroll, tree);
        dialog.add(treeScroll, BorderLayout.CENTER);

        JButton okBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        okBtn.addActionListener(e -> {
            List<HttpRequestItem> selected = requestCollectionsLeftPanel.getSelectedRequestsFromTree(tree);
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_EMPTY),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            onSelected.accept(selected);
            dialog.dispose();
        });
        JButton cancelBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);
        cancelBtn.addActionListener(e -> dialog.dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(btns);
        btns.add(okBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
