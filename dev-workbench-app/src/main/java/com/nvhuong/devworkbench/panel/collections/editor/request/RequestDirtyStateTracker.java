package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.request.compare.HttpRequestDirtyComparator;
import com.nvhuong.devworkbench.request.defaults.GeneratedRequestHeaderPolicy;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.util.JsonUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestDirtyStateTracker {
    private final Supplier<HttpRequestItem> currentRequestFromModelSupplier;
    private final Consumer<Boolean> dirtyIndicatorUpdater;
    private final GeneratedRequestHeaderPolicy generatedHeaderPolicy;
    private HttpRequestItem originalRequestItem;

    HttpRequestItem getOriginalRequestItem() {
        return originalRequestItem;
    }

    void setOriginalRequestItem(HttpRequestItem item) {
        if (item != null && !item.isNewRequest()) {
            originalRequestItem = JsonUtil.deepCopy(item, HttpRequestItem.class);
        } else {
            originalRequestItem = null;
        }
    }

    boolean isModified() {
        if (originalRequestItem == null) {
            return false;
        }

        // Kiểm tra bẩn luôn dựa trên “Ảnh chụp gốc vs Ảnh chụp nhanh của biểu mẫu hiện tại”，Vì thế UI Không cần biết chi tiết so sánh.
        HttpRequestItem current = currentRequestFromModelSupplier.get();
        return HttpRequestDirtyComparator.isDirty(
                originalRequestItem,
                current,
                generatedHeaderPolicy.generatedHeaders()
        );
    }

    void updateTabDirty() {
        SwingUtilities.invokeLater(() -> {
            if (originalRequestItem == null) {
                return;
            }
            dirtyIndicatorUpdater.accept(isModified());
        });
    }

}
