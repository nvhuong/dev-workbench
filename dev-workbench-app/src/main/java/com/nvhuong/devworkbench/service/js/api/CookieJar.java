package com.nvhuong.devworkbench.service.js.api;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

/**
 * Cookie Jar API
 * <p>
 * Cung cấp tương tự DevWorkbench của Cookie Jar Chức năng, hỗ trợ hoạt động gọi lại không đồng bộ Cookie。
 * </p>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Lấy Cookie Jar
 * var jar = pm.cookies.jar();
 *
 * // cài đặt Cookie
 * jar.set(pm.request.url, {
 *     name: "session_id",
 *     value: "abc123"
 * }, function(error) {
 *     if (error) {
 *         console.error('Failed to set cookie:', error);
 *     } else {
 *         console.log('Cookie set successfully');
 *     }
 * });
 *
 * // Lấy Cookie
 * jar.get(pm.request.url, 'session_id', function(error, cookie) {
 *     if (error) {
 *         console.error('Failed to get cookie:', error);
 *     } else {
 *         console.log('Cookie value:', cookie);
 *     }
 * });
 *
 * // xóa bỏ Cookie
 * jar.unset(pm.request.url, 'session_id', function(error) {
 *     if (error) {
 *         console.error('Failed to delete cookie:', error);
 *     } else {
 *         console.log('Cookie deleted successfully');
 *     }
 * });
 *
 * // Nhận tất cả chúng Cookie
 * jar.getAll(pm.request.url, function(error, cookies) {
 *     if (error) {
 *         console.error('Failed to get cookies:', error);
 *     } else {
 *         console.log('All cookies:', cookies);
 *     }
 * });
 *
 * // Xóa mọi thứ Cookie
 * jar.clear(pm.request.url, function(error) {
 *     if (error) {
 *         console.error('Failed to clear cookies:', error);
 *     } else {
 *         console.log('All cookies cleared');
 *     }
 * });
 * }</pre>
 */
@Slf4j
public class CookieJar {
    private final CookieApi cookieApi;

    public CookieJar(CookieApi cookieApi) {
        this.cookieApi = cookieApi;
    }

    /**
     * cài đặt Cookie（cuộc gọi lại được hỗ trợ)
     *
     * @param url      Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param cookie   Cookie Đối tượng hoặc tên
     * @param callback chức năng gọi lại
     */
    public void set(Object url, Value cookie, Value callback) {
        try {
            if (cookie.isString()) {
                // Nếu đối số thứ hai là một chuỗi thì nó được coi là như vậy cookie Tên, yêu cầu tham số thứ ba là giá trị
                throw new IllegalArgumentException("set() requires a cookie object, not a string");
            } else if (cookie.hasMembers()) {
                cookieApi.set(cookie);
            }
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * cài đặt Cookie（3 Phiên bản tham số: .url, name, value）
     *
     * @param url   Mục tiêu URL（Hỗ trợ String hoặc UrlWrapper）
     * @param name  Cookie tên
     * @param value Cookie giá trị
     */
    public void set(Object url, String name, String value) {
        cookieApi.set(name, value);
    }

    /**
     * cài đặt Cookie（4 Phiên bản tham số: .url, name, value, callback）
     *
     * @param url      Mục tiêu URL（Hỗ trợ String hoặc UrlWrapper）
     * @param name     Cookie tên
     * @param value    Cookie giá trị
     * @param callback chức năng gọi lại
     */
    public void set(Object url, String name, String value, Value callback) {
        try {
            cookieApi.set(name, value);
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * Lấy Cookie（cuộc gọi lại được hỗ trợ)
     *
     * @param url      Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param name     Cookie tên
     * @param callback Chức năng gọi lại, nhận (error, cookie) các thông số
     */
    public void get(Object url, String name, Value callback) {
        try {
            Cookie cookie = cookieApi.get(name);
            invokeCallback(callback, null, cookie);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage(), null);
        }
    }

    /**
     * xóa bỏ Cookie（2 Phiên bản tham số: .url, name，không gọi lại)
     *
     * @param url  Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param name Cookie tên
     */
    public void unset(Object url, String name) {
        try {
            cookieApi.delete(name);
        } catch (Exception e) {
            // Khi không có lệnh gọi lại, lỗi sẽ được xử lý âm thầm hoặc được ghi lại
            log.error("Failed to delete cookie: " + e.getMessage(), e);
        }
    }

    /**
     * xóa bỏ Cookie（3 Phiên bản tham số: .url, name, callback）
     *
     * @param url      Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param name     Cookie tên
     * @param callback chức năng gọi lại
     */
    public void unset(Object url, String name, Value callback) {
        try {
            cookieApi.delete(name);
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * Nhận tất cả chúng Cookie（cuộc gọi lại được hỗ trợ)
     *
     * @param url      Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param callback Chức năng gọi lại, nhận (error, cookies) các thông số
     */
    public void getAll(Object url, Value callback) {
        try {
            Cookie[] cookies = cookieApi.toArray();
            invokeCallback(callback, null, cookies);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage(), null);
        }
    }

    /**
     * Xóa mọi thứ Cookie（cuộc gọi lại được hỗ trợ)
     *
     * @param url      Mục tiêu URL（Việc triển khai hiện tại bỏ qua và hỗ trợ điều này String hoặc UrlWrapper）
     * @param callback chức năng gọi lại
     */
    public void clear(Object url, Value callback) {
        try {
            cookieApi.clear();
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * Gọi hàm gọi lại (tham số đơn: .error）
     *
     * @param callback chức năng gọi lại
     * @param error    thông báo lỗi,null Cho biết sự thành công
     */
    private void invokeCallback(Value callback, String error) {
        if (callback != null && callback.canExecute()) {
            callback.executeVoid(error);
        }
    }

    /**
     * Gọi hàm gọi lại (hai tham số: .error, result）
     *
     * @param callback chức năng gọi lại
     * @param error    thông báo lỗi,null Cho biết sự thành công
     * @param result   Dữ liệu kết quả
     */
    private void invokeCallback(Value callback, String error, Object result) {
        if (callback != null && callback.canExecute()) {
            callback.executeVoid(error, result);
        }
    }
}

