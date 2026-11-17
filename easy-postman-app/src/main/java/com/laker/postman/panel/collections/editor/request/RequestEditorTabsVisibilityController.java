package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import java.awt.Component;

/**
 * Request editing area Tab Bộ điều khiển khả năng hiển thị.
 * <p>
 * Người dùng có thể ẩn nó trong cài đặt Docs/Params/Auth/Headers/Body/Scripts/Settings，Các lựa chọn xây dựng lại và phục hồi đều tập trung ở đây.
 */
@RequiredArgsConstructor
final class RequestEditorTabsVisibilityController {
    private final RequestItemProtocolEnum protocol;
    private final RequestViewComponents view;
    private final Runnable indicatorUpdater;

    void updateVisibility() {
        Component selectedComponent = view.reqTabs.getSelectedComponent();
        RequestViewFactory.rebuildRequestTabs(view, protocol);
        RequestTabSelector.selectFirstVisible(view.reqTabs, selectedComponent, view.paramsTabPanel, view.requestBodyPanel);
        indicatorUpdater.run();
        view.reqTabs.revalidate();
        view.reqTabs.repaint();
    }
}
