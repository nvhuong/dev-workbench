package com.nvhuong.devworkbench.panel.collections;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorTabInserter;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.panel.collections.tree.adapter.SwingCollectionTreeQueries;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.collections.OpenedRequestTabsStore;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.util.List;

@UtilityClass
public class OpenedRequestTabSessionRestorer {

    public static void restoreOpenedRequests(List<HttpRequestItem> requestItems, Runnable onComplete) {
        Runnable restoreTask = () -> {
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            List<HttpRequestItem> restorableRequests = SwingCollectionTreeQueries.resolveRestorableOpenedRequests(
                    requestItems,
                    leftPanel.getRootTreeNode()
            );
            for (int i = 0; i < restorableRequests.size(); i++) {
                HttpRequestItem item = restorableRequests.get(i);
                boolean selectTab = i == restorableRequests.size() - 1;
                RequestEditSubPanel panel = RequestEditorTabInserter.insertRequestTab(item, selectTab, true);
                RequestEditorTabInserter.setTabNewRequestMarker(panel, item.isNewRequest());
            }
            OpenedRequestTabsStore.clear();
            if (onComplete != null) {
                onComplete.run();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            restoreTask.run();
        } else {
            SwingUtilities.invokeLater(restoreTask);
        }
    }
}
