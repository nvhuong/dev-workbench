package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.core.model.NodeType;


import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.panel.performance.tree.PerformanceRequestNodeStateSynchronizer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.function.Function;

final class PerformanceRequestEditorSupport {

    private final JPanel requestEditorHost;
    private final Function<HttpRequestItem, RequestItemProtocolEnum> protocolResolver;
    private RequestEditSubPanel requestEditSubPanel;
    private RequestItemProtocolEnum currentRequestEditorProtocol = RequestItemProtocolEnum.HTTP;

    PerformanceRequestEditorSupport(RequestEditSubPanel requestEditSubPanel,
                                    JPanel requestEditorHost,
                                    Function<HttpRequestItem, RequestItemProtocolEnum> protocolResolver) {
        this.requestEditSubPanel = requestEditSubPanel;
        this.requestEditorHost = requestEditorHost;
        this.protocolResolver = protocolResolver;
    }

    RequestEditSubPanel getRequestEditSubPanel() {
        return requestEditSubPanel;
    }

    void switchRequestEditor(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = protocolResolver.apply(item);
        if (requestEditSubPanel == null || requestEditorHost == null || protocol != currentRequestEditorProtocol) {
            if (requestEditorHost != null && requestEditSubPanel != null) {
                requestEditorHost.remove(requestEditSubPanel);
            }
            requestEditSubPanel = RequestEditSubPanel.performanceSnapshot("", protocol, false);
            currentRequestEditorProtocol = protocol;
            if (requestEditorHost != null) {
                requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
                requestEditorHost.revalidate();
                requestEditorHost.repaint();
            }
        }

        if (item != null) {
            requestEditSubPanel.initPanelData(item);
            // Trang kiểm tra căng thẳng sử dụng trình bao khởi tạo bị trì hoãn để giảm chi phí mở ban đầu, nhưng sau khi người dùng chọn nút yêu cầu
            // Bạn phải nâng cấp lên trình chỉnh sửa thực sự ngay lập tức, nếu không bạn sẽ ở lại màn hình khung xương.
            requestEditSubPanel.ensureEditorInitialized();
        }
    }

    void saveRequestNodeData(DefaultMutableTreeNode node,
                             java.util.function.BiConsumer<DefaultMutableTreeNode, PerformanceTreeNode> syncRequestStructureAction) {
        if (requestEditSubPanel == null || node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (userObj instanceof PerformanceTreeNode nodeData && nodeData.type == NodeType.REQUEST) {
            PerformanceRequestNodeStateSynchronizer.replaceRequestItem(nodeData, requestEditSubPanel.getCurrentRequest());
            syncRequestStructureAction.accept(node, nodeData);
        }
    }
}
