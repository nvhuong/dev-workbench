package com.nvhuong.devworkbench.service.js.api;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Cookie API (pm.cookies)
 * <p>
 * cung cấp Cookie Các chức năng đọc, đặt và xóa được mô phỏng DevWorkbench của Cookie Giao diện quản lý.
 * </p>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Lấy Cookie
 * var token = pm.cookies.get("session_token");
 *
 * // cài đặt Cookie
 * pm.cookies.set({
 *     name: "user_id",
 *     value: "12345",
 *     domain: "example.com",
 *     path: "/",
 *     secure: true,
 *     httpOnly: true
 * });
 *
 * // xóa bỏ Cookie
 * pm.cookies.delete("session_token");
 *
 * // sử dụng Cookie Jar
 * var jar = pm.cookies.jar();
 * jar.unset(pm.request.url, 'session_id', function(error) {
 *     if (error) {
 *         console.error('Failed to delete cookie');
 *     }
 * });
 * }</pre>
 */
public class CookieApi {
    /**
     * Cookie Danh sách lưu trữ
     */
    private final List<Cookie> cookiesList = new ArrayList<>();

    /**
     * Cookie Jar Ví dụ (khởi tạo lười biếng)
     */
    private CookieJar cookieJar;

    /**
     * Lấy tên được chỉ định Cookie
     *
     * @param name Cookie tên
     * @return Cookie Đối tượng, được trả về nếu nó không tồn tại null
     */
    public Cookie get(String name) {
        if (name == null) {
            return null;
        }
        return cookiesList.stream()
                .filter(c -> name.equals(c.name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Nhận tất cả chúng Cookie
     *
     * @return Cookie danh sách
     */
    public List<Cookie> getAll() {
        return new ArrayList<>(cookiesList);
    }

    /**
     * cài đặt Cookie（theo tên và giá trị)
     *
     * @param name  Cookie tên
     * @param value Cookie giá trị
     */
    public void set(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        Cookie cookie = new Cookie();
        cookie.name = name;
        cookie.value = value;
        set(cookie);
    }

    /**
     * cài đặt Cookie（vượt qua Cookie đối tượng)
     *
     * @param cookie Cookie đối tượng
     */
    public void set(Cookie cookie) {
        if (cookie == null || cookie.name == null) {
            return;
        }
        // Xóa cái cũ có cùng tên Cookie
        delete(cookie.name);
        // Thêm mới Cookie
        cookiesList.add(cookie);
    }

    /**
     * cài đặt Cookie（vượt qua GraalVM Value đối tượng, hỗ trợ JavaScript đối tượng)
     *
     * @param cookieValue JavaScript Cookie đối tượng
     */
    public void set(Value cookieValue) {
        if (cookieValue == null || !cookieValue.hasMembers()) {
            return;
        }

        Cookie cookie = new Cookie();
        if (cookieValue.hasMember("name")) {
            cookie.name = cookieValue.getMember("name").asString();
        }
        if (cookieValue.hasMember("value")) {
            cookie.value = cookieValue.getMember("value").asString();
        }
        if (cookieValue.hasMember("domain")) {
            cookie.domain = cookieValue.getMember("domain").asString();
        }
        if (cookieValue.hasMember("path")) {
            cookie.path = cookieValue.getMember("path").asString();
        }
        if (cookieValue.hasMember("expires")) {
            cookie.expires = cookieValue.getMember("expires").asString();
        }
        if (cookieValue.hasMember("maxAge")) {
            cookie.maxAge = cookieValue.getMember("maxAge").asInt();
        }
        if (cookieValue.hasMember("secure")) {
            cookie.secure = cookieValue.getMember("secure").asBoolean();
        }
        if (cookieValue.hasMember("httpOnly")) {
            cookie.httpOnly = cookieValue.getMember("httpOnly").asBoolean();
        }
        if (cookieValue.hasMember("sameSite")) {
            cookie.sameSite = cookieValue.getMember("sameSite").asString();
        }

        set(cookie);
    }

    /**
     * Xóa tên được chỉ định Cookie
     *
     * @param name Cookie tên
     */
    public void delete(String name) {
        if (name == null) {
            return;
        }
        cookiesList.removeIf(c -> name.equals(c.name));
    }

    /**
     * Kiểm tra xem tên được chỉ định có tồn tại không Cookie
     *
     * @param name Cookie tên
     * @return tồn tại
     */
    public boolean has(String name) {
        if (name == null) {
            return false;
        }
        return cookiesList.stream().anyMatch(c -> name.equals(c.name));
    }

    /**
     * Xóa mọi thứ Cookie
     */
    public void clear() {
        cookiesList.clear();
    }

    /**
     * Chuyển đổi sang  Cookie Mảng đối tượng (for JavaScript thăm)
     *
     * @return Cookie mảng
     */
    public Cookie[] toArray() {
        return cookiesList.toArray(new Cookie[0]);
    }

    /**
     * Lấy Cookie Jar Ví dụ
     * <p>
     * Cookie Jar Cung cấp không đồng bộ Cookie Giao diện hoạt động.
     * </p>
     *
     * @return CookieJar Ví dụ
     */
    public CookieJar jar() {
        if (cookieJar == null) {
            cookieJar = new CookieJar(this);
        }
        return cookieJar;
    }
}
