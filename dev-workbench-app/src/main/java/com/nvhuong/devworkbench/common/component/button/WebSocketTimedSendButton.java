package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * WebSocket Nút gửi theo lịch trình
 */
public class WebSocketTimedSendButton extends JButton {
    public WebSocketTimedSendButton() {
        setIcon(IconUtil.createThemed("icons/time.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setToolTipText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
