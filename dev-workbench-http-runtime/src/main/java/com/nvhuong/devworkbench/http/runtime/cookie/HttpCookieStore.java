package com.nvhuong.devworkbench.http.runtime.cookie;

import com.nvhuong.devworkbench.http.runtime.interaction.HttpCallbackDispatcher;
import com.nvhuong.devworkbench.http.runtime.okhttp.OkHttpClientManager;
import com.nvhuong.devworkbench.request.model.CookieInfo;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cookie Các lớp công cụ liên quan đến quản lý, chịu trách nhiệm giám sát, thu thập và thiết lập Cookie
 */
public class HttpCookieStore {
    // Danh sách người nghe
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean notificationPending = new AtomicBoolean(false);
    private static volatile HttpCallbackDispatcher notificationDispatcher = HttpCallbackDispatcher.direct();
    // Bức tranh lớn CookieManager
    private static final CookieManager GLOBAL_COOKIE_MANAGER = OkHttpClientManager.getGlobalCookieManager();

    public static void setNotificationDispatcher(HttpCallbackDispatcher dispatcher) {
        notificationDispatcher = dispatcher == null ? HttpCallbackDispatcher.direct() : dispatcher;
    }

    public static void registerCookieChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public static void unregisterCookieChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static List<CookieInfo> getAllCookieInfos() {
        List<CookieInfo> list = new ArrayList<>();
        CookieStore store = GLOBAL_COOKIE_MANAGER.getCookieStore();
        for (HttpCookie cookie : store.getCookies()) {
            if (cookie.hasExpired()) continue;
            CookieInfo info = new CookieInfo();
            info.name = cookie.getName();
            info.value = cookie.getValue();
            info.domain = cookie.getDomain();
            info.path = cookie.getPath();
            info.expires = cookie.getMaxAge() > 0 ? System.currentTimeMillis() + cookie.getMaxAge() * 1000 : -1;
            info.secure = cookie.getSecure();
            info.httpOnly = cookie.isHttpOnly();
            list.add(info);
        }
        return list;
    }

    public static void removeCookie(String name, String domain, String path) {
        CookieStore store = GLOBAL_COOKIE_MANAGER.getCookieStore();
        List<HttpCookie> toRemove = new ArrayList<>();
        for (HttpCookie cookie : store.getCookies()) {
            if (cookie.getName().equals(name)
                    && (domain == null || domain.equals(cookie.getDomain()))
                    && (path == null || path.equals(cookie.getPath()))) {
                toRemove.add(cookie);
            }
        }
        for (HttpCookie cookie : toRemove) {
            store.remove(null, cookie);
        }
        notifyCookieChanged();
    }

    public static void clearAllCookies() {
        GLOBAL_COOKIE_MANAGER.getCookieStore().removeAll();
        notifyCookieChanged();
    }

    public static void notifyCookieChanged() {
        HttpCallbackDispatcher dispatcher = notificationDispatcher;
        if (dispatcher.isDispatchThread()) {
            notifyListeners();
            return;
        }
        if (!notificationPending.compareAndSet(false, true)) {
            return;
        }
        dispatcher.dispatch(() -> {
            notificationPending.set(false);
            notifyListeners();
        });
    }

    private static void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    public static void addCookie(String name, String value, String domain, String path, boolean secure, boolean httpOnly) {
        try {
            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path == null || path.isEmpty() ? "/" : path);
            cookie.setSecure(secure);
            cookie.setHttpOnly(httpOnly);
            GLOBAL_COOKIE_MANAGER.getCookieStore().add(null, cookie);
            notifyCookieChanged();
        } catch (Exception ignore) {
        }
    }

    public static void refreshCookies() {
        notifyCookieChanged();
    }
}
