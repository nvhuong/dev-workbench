package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * WebSocket Nút gửi
 */
public class WebSocketSendButton extends JButton {
    public WebSocketSendButton() {
        setIcon(IconUtil.createThemed("icons/send.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText("Send");
        setVisible(true);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
