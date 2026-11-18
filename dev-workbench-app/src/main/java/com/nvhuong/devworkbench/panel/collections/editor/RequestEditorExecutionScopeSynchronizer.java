package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.service.collections.CollectionRequestExecutionScopeResolver;

/**
 * Yêu cầu đồng bộ hóa phạm vi biến của trình soạn thảo.
 * <p>
 * Khi mở một yêu cầu trong bộ sưu tập, bạn cần ghi các biến kế thừa của nhóm vào ngữ cảnh thực thi hiện tại. Logic này dựa trên cây bộ sưu tập và không thuộc về bố cục bảng điều khiển.
 */
final class RequestEditorExecutionScopeSynchronizer {

    void syncScopeForRequest(String requestId) {
        CollectionRequestExecutionScopeResolver.syncCurrentScopeOrEmpty(requestId);
    }
}
