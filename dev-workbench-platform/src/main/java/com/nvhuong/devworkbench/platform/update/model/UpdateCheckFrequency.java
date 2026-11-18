package com.nvhuong.devworkbench.platform.update.model;

/**
 * Cập nhật bảng liệt kê tần suất kiểm tra
 */
public enum UpdateCheckFrequency {
    STARTUP("startup", 0),   // mỗi khi nó bắt đầu
    DAILY("daily", 1),       // hằng ngày
    WEEKLY("weekly", 7),     // hàng tuần
    MONTHLY("monthly", 30);  // mỗi tháng

    private final String code;
    private final int days;

    UpdateCheckFrequency(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public int getDays() {
        return days;
    }

    /**
     * Có nên kiểm tra mỗi lần khởi động hay không
     */
    public boolean isAlwaysCheck() {
        return this == STARTUP;
    }

    public static UpdateCheckFrequency fromCode(String code) {
        for (UpdateCheckFrequency frequency : values()) {
            if (frequency.code.equals(code)) {
                return frequency;
            }
        }
        return DAILY; // Mặc định là hàng ngày
    }
}