package com.laker.postman.common;

import javax.swing.*;

/**
 * Bảng điều khiển lớp trừu tượng cơ bản, thống nhấtUIKhởi tạo và giám sát cấu trúc đăng ký
 * cung cấpinitUIvàregisterListenersHai phương pháp trừu tượng,
 * Các lớp con cần triển khai hai phương thức này để hoàn thành các nhiệm vụ cụ thểUIKhởi tạo thành phần và đăng ký trình xử lý sự kiện.
 * Tất cả các lớp con phải vượt quaUiSingletonFactoryLấy ví dụ, trực tiếp bị cấmnewTạo một ví dụ.
 */
public abstract class UiSingletonPanel extends JPanel {
    // Sử dụng các biến cục bộ luồng tĩnh để kiểm soát trực tiếp trạng thái tạo
    private static final ThreadLocal<Boolean> CREATING_ALLOWED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Hàm tạo được đặt thành protected，Hạn chế trực tiếp từ bên ngoàinew
    protected UiSingletonPanel() {
        // Xác minh rằng việc tạo phiên bản được cho phép
        if (!CREATING_ALLOWED.get()) {
            throw new IllegalStateException(
                    "UiSingletonPanelCác lớp con phải vượt quaUiSingletonFactory.getInstance()Lấy ví dụ, trực tiếp bị cấmnewTạo: " +
                            this.getClass().getName());
        }
    }

    public void initializeSingletonUi() {
        initUI();
        registerListeners();
    }

    /**
     * khởi tạoUIthành phần
     */
    protected abstract void initUI();

    /**
     * Đăng ký người nghe sự kiện
     */
    protected abstract void registerListeners();

    /**
     * Đặt xem có cho phép tạo phiên bản hay khôngUiSingletonFactorysử dụng
     */
    public static void setFactoryCreationAllowed(boolean allowed) {
        CREATING_ALLOWED.set(allowed);
    }
}
