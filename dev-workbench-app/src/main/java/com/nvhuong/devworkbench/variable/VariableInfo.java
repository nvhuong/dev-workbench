package com.nvhuong.devworkbench.variable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Thông tin biến đổi
 * <p>
 * Được sử dụng trongUIHiển thị thông tin đầy đủ về biến (tên, giá trị, loại)
 */
@Data
@AllArgsConstructor
public class VariableInfo {
    /**
     * tên biến
     */
    private String name;

    /**
     * giá trị thay đổi
     */
    private String value;

    /**
     * Loại biến
     */
    private VariableType type;
}
