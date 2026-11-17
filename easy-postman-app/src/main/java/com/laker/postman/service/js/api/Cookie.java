package com.laker.postman.service.js.api;

/**
 * Cookie đối tượng dữ liệu
 * <p>
 * đại diện cho một HTTP Cookie，Tất cả các tiêu chuẩn bao gồm Cookie tài sản.
 * </p>
 */
public class Cookie {
    /**
     * Cookie tên
     */
    public String name;

    /**
     * Cookie giá trị
     */
    public String value;

    /**
     * Cookie Tên miền mà nó thuộc về
     */
    public String domain;

    /**
     * Cookie con đường
     */
    public String path;

    /**
     * Cookie Thời gian hết hạn
     */
    public String expires;

    /**
     * Cookie Thời gian tồn tại tối đa (giây)
     */
    public Integer maxAge;

    /**
     * Có phải nó chỉ trong HTTPS Đã gửi trong khi kết nối
     */
    public Boolean secure;

    /**
     * Nó có bị cấm không? JavaScript thăm
     */
    public Boolean httpOnly;

    /**
     * SameSite tài sản (Strict/Lax/None)
     */
    public String sameSite;

    @Override
    public String toString() {
        return String.format("Cookie{name='%s', value='%s', domain='%s', path='%s'}",
                name, value, domain, path);
    }
}