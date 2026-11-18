package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.service.curl.CurlImportUtil;
import com.nvhuong.devworkbench.util.AsyncClipboardUtil;
import com.nvhuong.devworkbench.util.ClipboardUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * “+ Tab” Bộ điều khiển nhấp chuột.
 * <p>
 * Yêu cầu mới, clipboard cURL Recognition and import both belong to user action choreography and are not included RequestEditorPanel trong mã bố cục.
 */
@RequiredArgsConstructor
final class RequestEditorPlusTabController {
    private final JTabbedPane tabbedPane;
    private final Component dialogParent;
    private final String plusTabTitle;
    private final String defaultRequestTitle;
    private final BiFunction<String, RequestItemProtocolEnum, RequestEditSubPanel> requestTabCreator;

    void install() {
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
    }

    private void handleMousePressed(MouseEvent e) {
        int clickedTabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
        if (!isPlusTab(clickedTabIndex)) {
            return;
        }

        createRequestFromClipboardCurlOrDefault();
    }

    private boolean isPlusTab(int tabIndex) {
        return tabIndex >= 0
                && tabIndex == tabbedPane.getTabCount() - 1
                && plusTabTitle.equals(tabbedPane.getTitleAt(tabIndex));
    }

    private void createRequestFromClipboardCurlOrDefault() {
        readClipboardCurlTextAsync().thenAccept(curlText -> SwingUtilities.invokeLater(() -> {
            if (tryCreateRequestFromClipboardCurl(curlText)) {
                return;
            }
            requestTabCreator.apply(defaultRequestTitle, RequestItemProtocolEnum.HTTP);
        }));
    }

    private CompletableFuture<String> readClipboardCurlTextAsync() {
        return ClipboardUtil.getClipboardCurlTextAsync();
    }

    private boolean tryCreateRequestFromClipboardCurl(String curlText) {
        if (curlText == null) {
            return false;
        }
        int result = JOptionPane.showConfirmDialog(dialogParent,
                I18nUtil.getMessage(MessageKeys.CLIPBOARD_CURL_DETECTED),
                I18nUtil.getMessage(MessageKeys.IMPORT_CURL),
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return false;
        }

        try {
            HttpRequestItem item = CurlImportUtil.fromCurl(curlText);
            if (item == null) {
                return false;
            }
            RequestEditSubPanel tab = requestTabCreator.apply(null, item.getProtocol());
            item.setId(tab.getId());
            tab.initPanelData(item);
            clearClipboardAsync();
            return true;
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.PARSE_CURL_ERROR, ex.getMessage()));
            return true;
        }
    }

    private void clearClipboardAsync() {
        AsyncClipboardUtil.clearStringAsync();
    }
}
