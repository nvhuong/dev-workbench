package com.nvhuong.devworkbench.plugin.redis;

import org.testng.annotations.Test;
import sun.misc.Unsafe;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class RedisPanelInputTest {

    @Test
    public void hostFieldShouldAcceptRedisUrlsWithCredentialsAndDatabase() throws Exception {
        assertHostPort("redis://:secret@cache.local:6380/2", "cache.local", 6380);
        assertHostPort("redis://user:p%40ss@10.0.0.5:6379", "10.0.0.5", 6379);
        assertHostPort("user@cache.local", "cache.local", 6379);
    }

    @Test
    public void hostFieldShouldAcceptBracketedIpv6() throws Exception {
        assertHostPort("[::1]:6380", "::1", 6380);
        assertHostPort("redis://[2001:db8::7]:6390/0", "2001:db8::7", 6390);
        assertHostPort("[::1]", "::1", 6379);
    }

    @Test
    public void hostFieldShouldKeepPlainHostAndPortBehaviour() throws Exception {
        assertHostPort("localhost", "localhost", 6379);
        assertHostPort("redis.example.com:7000", "redis.example.com", 7000);
        assertHostPort("redis://localhost", "localhost", 6379);
    }

    @Test
    public void keyListShouldNotRenderKeysAsHtml() throws Exception {
        // The renderer reuses one JLabel, so capture its state right after each render.
        Object[] hostileView = new Object[1];
        String[] hostileText = new String[1];
        Object[] ttlView = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            RedisPanel panel = new RedisPanel();
            @SuppressWarnings("unchecked")
            JList<String> keyList = (JList<String>) field(panel, "keyList");
            DefaultListModel<String> model = new DefaultListModel<>();
            String hostileKey = "<html><b>bold</b><img src='http://example.invalid/x.png'>";
            model.addElement(hostileKey);
            keyList.setModel(model);
            // Keys beyond the metadata limit have no TTL entry and took the plain-text branch.
            JLabel first = (JLabel) keyList.getCellRenderer()
                    .getListCellRendererComponent(keyList, hostileKey, 0, false, false);
            hostileView[0] = first.getClientProperty(BasicHTML.propertyKey);
            hostileText[0] = first.getText();
            // Render a normal key afterwards to make sure the shared renderer is not left HTML-disabled
            // for keys that legitimately use the TTL HTML markup.
            @SuppressWarnings("unchecked")
            java.util.Map<String, Long> ttls = (java.util.Map<String, Long>) field(panel, "keyTtlMap");
            ttls.put("session:1", 30L);
            JLabel second = (JLabel) keyList.getCellRenderer()
                    .getListCellRendererComponent(keyList, "session:1", 0, false, false);
            ttlView[0] = second.getClientProperty(BasicHTML.propertyKey);
        });
        assertNull(hostileView[0], "a key starting with <html> must be shown literally, not parsed as HTML");
        assertEquals(hostileText[0], "<html><b>bold</b><img src='http://example.invalid/x.png'>");
        org.testng.Assert.assertNotNull(ttlView[0], "keys with TTL metadata still use the HTML layout");
    }

    private static void assertHostPort(String input, String expectedHost, int expectedPort) throws Exception {
        Method parse = RedisPanel.class.getDeclaredMethod("parseHostAndPort", String.class, int.class);
        parse.setAccessible(true);
        Object parsed = parse.invoke(panelWithoutConstructor(), input, 6379);
        Method host = parsed.getClass().getDeclaredMethod("host");
        Method port = parsed.getClass().getDeclaredMethod("port");
        host.setAccessible(true);
        port.setAccessible(true);
        assertEquals(host.invoke(parsed), expectedHost, "host of " + input);
        assertEquals(port.invoke(parsed), expectedPort, "port of " + input);
    }

    private static RedisPanel panelWithoutConstructor() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (RedisPanel) ((Unsafe) field.get(null)).allocateInstance(RedisPanel.class);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = RedisPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
