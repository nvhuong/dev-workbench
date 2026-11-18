package com.nvhuong.devworkbench.panel.topmenu;

import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.util.SystemInfo;
import com.nvhuong.devworkbench.common.IRefreshable;
import com.nvhuong.devworkbench.common.UiSingletonMenuBar;
import com.nvhuong.devworkbench.common.component.combobox.EnvironmentComboBox;
import com.nvhuong.devworkbench.common.component.combobox.WorkspaceComboBox;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.panel.lifecycle.AppExitCoordinator;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class TopMenuBar extends UiSingletonMenuBar implements IRefreshable {
    private TopMenuWorkspaceControls workspaceControls;

    @Override
    protected void initUI() {
        setOpaque(true);
        setBorder(createPanelBorder());
        initComponents();
    }

    static Border createPanelBorder() {
        return BorderFactory.createEmptyBorder(2, 4, 1, 8);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Tạo lại đường viền khi chuyển đổi chủ đề để đảm bảo không gian trống trên thanh menu vẫn ổn định.
        setBorder(createPanelBorder());
    }

    @Override
    protected void registerListeners() {
        FlatDesktop.setAboutHandler(this::showAboutDialog);
        FlatDesktop.setQuitHandler(e -> {
            // Ứng dụng xử lý việc lưu, hủy, dọn dẹp không đồng bộ và chặn một cách thống nhất macOS Kết thúc quá trình trực tiếp.
            e.cancelQuit();
            BeanFactory.getBean(AppExitCoordinator.class).exitApplication();
        });
        if (SystemInfo.isMacFullWindowContentSupported) {
            addMouseListener(new MacTitleBarDoubleClickHandler());
        }
    }


    /**
     * Làm mới thanh menu (đã triển khai IRefreshable giao diện)
     * <p>
     * Tải lại thanh menu (bao gồm các mục menu, phím tắt,Git tất cả các thành phần bao gồm cả thanh công cụ)
     * Được gọi trong các trường hợp sau:
     * 1. Sau khi chuyển đổi ngôn ngữ (tất cả văn bản menu cần được cập nhật)
     * 2. Sau khi cài đặt phím tắt được sửa đổi
     * 3. Sau khi chuyển đổi không gian làm việc (cần được cập nhật Git trạng thái hiển thị thanh công cụ)
     */
    @Override
    public void refresh() {
        removeAll();
        // Tạo lại tất cả các thành phần thanh menu
        initComponents();
        // Làm mới giao diện
        revalidate();
        repaint();
    }

    private void initComponents() {
        if (SystemInfo.isMacFullWindowContentSupported) {
            // macOS Full Window Content chế độ này, chừa nhiều không gian hơn ở phía bên trái cho các nút màu đỏ, vàng và xanh lục
            // macOS Chiều rộng của các nút màu đỏ, vàng và xanh lục là khoảng 70-76px
            add(Box.createHorizontalStrut(70));
        }

        add(TopMenuFileMenu.create(this));
        add(TopMenuLanguageMenu.create());
        add(TopMenuThemeMenu.create());
        add(TopMenuSettingsMenu.create(this));
        add(TopMenuPluginMenu.create(this));
        add(TopMenuHelpMenu.create(this));
        addAboutMenu();

        add(Box.createGlue()); // Thêm không gian linh hoạt để đẩy các thành phần tiếp theo sang bên phải

        workspaceControls().addTo(this);
    }

    public EnvironmentComboBox getEnvironmentComboBox() {
        return workspaceControls().getEnvironmentComboBox();
    }

    public WorkspaceComboBox getWorkspaceComboBox() {
        return workspaceControls().getWorkspaceComboBox();
    }

    /**
     * Cập nhật nội dung hộp thả xuống của không gian làm việc (không cần tải lại toàn bộ thanh menu)
     * Được sử dụng cho các tình huống trong đó danh sách không gian làm việc thay đổi nhưng loại không gian làm việc hiện tại không thay đổi (chẳng hạn như tạo, đổi tên)
     */
    public void updateWorkspaceComboBox() {
        workspaceControls().updateWorkspaceComboBox();
    }

    /**
     * Hiển thị không gian làm việc được cập nhật (bao gồm hộp thả xuống không gian làm việc và các tệp  Git thanh công cụ)
     * Được gọi sau khi chuyển đổi không gian làm việc bên ngoài (ví dụ: from WorkspacePanel chuyển đổi)
     * Toàn bộ thanh menu sẽ được tải lại để cập nhật Git Thanh công cụ hiển thị trạng thái
     */
    public void updateWorkspaceDisplay() {
        workspaceControls().updateWorkspaceDisplay();
    }

    private TopMenuWorkspaceControls workspaceControls() {
        if (workspaceControls == null) {
            workspaceControls = new TopMenuWorkspaceControls(this, this::refresh);
        }
        return workspaceControls;
    }

    private void addAboutMenu() {
        JMenu aboutMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_ABOUT));
        JMenuItem aboutMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_ABOUT_DEV_WORKBENCH));
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        aboutMenu.add(aboutMenuItem);
        add(aboutMenu);
    }

    private void showAboutDialog() {
        TopMenuAboutDialog.show(this);
    }

}
