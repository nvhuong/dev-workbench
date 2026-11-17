package com.laker.postman.panel.sidebar.cookie;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.panel.collections.editor.request.sub.CookieTablePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Cookie Hộp thoại quản lý
 * Bức tranh lớn Cookie Giao diện quản lý hiển thị và quản lý toàn bộ tên miền Cookie
 */
public class CookieManagerDialog extends JDialog {

    public CookieManagerDialog(Window owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.COOKIES_MANAGER_TITLE), ModalityType.MODELESS);
        initUI();
    }

    private void initUI() {
        ToolWindowSurfaceStyle.skipDialogWindowChrome(this);
        CookieTablePanel cookiePanel = new CookieTablePanel();
        ToolWindowSurfaceStyle.applyDialogSurface(cookiePanel);
        setContentPane(cookiePanel);

        // Đặt thuộc tính hộp thoại
        setSize(900, 500);
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
