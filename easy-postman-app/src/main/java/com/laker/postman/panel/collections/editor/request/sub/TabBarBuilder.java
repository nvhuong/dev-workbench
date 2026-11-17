package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

/**
 * TabTrình tạo cột
 * Chịu trách nhiệm tạo dựa trên loại giao thứcTabCác nút và bảng điều khiển
 */
@UtilityClass
public class TabBarBuilder {

    public enum Tab {
        RESPONSE_BODY,
        EVENT_STREAM,
        RESPONSE_HEADERS,
        TESTS,
        NETWORK_LOG,
        TIMING,
        WEBSOCKET_MESSAGES
    }

    /**
     * TabThông tin cấu hình
     */
    public static class TabConfig {
        public final String[] tabNames;
        public final boolean[] initialVisibility;
        private final Tab[] tabs;

        public TabConfig(Tab[] tabs, String[] tabNames, boolean[] initialVisibility) {
            this.tabs = tabs;
            this.tabNames = tabNames;
            this.initialVisibility = initialVisibility;
        }

        public int indexOf(Tab tab) {
            for (int i = 0; i < tabs.length; i++) {
                if (tabs[i] == tab) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * vìWebSocketTạo thỏa thuậnTabcấu hình
     */
    public static TabConfig createWebSocketTabs() {
        Tab[] tabs = new Tab[]{Tab.WEBSOCKET_MESSAGES, Tab.RESPONSE_HEADERS, Tab.NETWORK_LOG};
        String[] names = new String[]{
                I18nUtil.getMessage(MessageKeys.TAB_WEBSOCKET_MESSAGES),
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS),
                I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG)
        };
        return createTabConfig(tabs, names, null);
    }

    /**
     * vìHTTPTạo thỏa thuậnTabcấu hình
     */
    public static TabConfig createHttpTabs() {
        Tab[] tabs = new Tab[]{
                Tab.RESPONSE_BODY, Tab.EVENT_STREAM, Tab.RESPONSE_HEADERS,
                Tab.TESTS, Tab.NETWORK_LOG, Tab.TIMING
        };
        String[] names = new String[]{
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_BODY),
                I18nUtil.getMessage(MessageKeys.TAB_SSE_EVENT_STREAM),
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS),
                I18nUtil.getMessage(MessageKeys.TAB_TESTS),
                I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG),
                I18nUtil.getMessage(MessageKeys.TAB_TIMING)
        };

        // HTTP chế độ này, nhãn luồng sự kiện bị ẩn theo mặc định.
        boolean[] visibility = createAllVisibleArray(names.length);
        visibility[indexOf(tabs, Tab.EVENT_STREAM)] = false;

        return createTabConfig(tabs, names, visibility);
    }

    /**
     * TạoTabcấu hình
     *
     * @param names      tabMảng tên
     * @param visibility Mảng hiển thị, .nullCho biết tất cả đều có thể nhìn thấy được
     * @return TabĐối tượng cấu hình
     */
    private static TabConfig createTabConfig(Tab[] tabs, String[] names, boolean[] visibility) {
        boolean[] vis = visibility != null ? visibility : createAllVisibleArray(names.length);
        return new TabConfig(tabs, names, vis);
    }

    private static int indexOf(Tab[] tabs, Tab tab) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == tab) {
                return i;
            }
        }
        return -1;
    }

    public static JButton[] createModernTabButtons(String[] names) {
        JButton[] buttons = new JButton[names.length];
        for (int i = 0; i < names.length; i++) {
            buttons[i] = new ModernTabButton(names[i], i);
        }
        return buttons;
    }

    /**
     * Tạo một mảng gồm tất cả các boolean có thể nhìn thấy được
     */
    private static boolean[] createAllVisibleArray(int length) {
        boolean[] result = new boolean[length];
        java.util.Arrays.fill(result, true);
        return result;
    }

    /**
     * SẽTabnút được thêm vàotabBarbảng điều khiển
     *
     * @param tabBar     Bảng mục tiêu
     * @param buttons    Mảng nút
     * @param visibility Mảng hiển thị
     */
    public static void addButtonsToTabBar(JPanel tabBar, JButton[] buttons, boolean[] visibility) {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                buttons[i].setVisible(visibility[i]);
                tabBar.add(buttons[i]);
            }
        }
    }

    /**
     * cho tất cảtabSự kiện nhấp chuột ràng buộc nút
     *
     * @param buttons       Mảng nút
     * @param tabNames      tabMảng tên
     * @param cardPanel     Bảng thẻ
     * @param onTabSelected tabGọi lại khi được chọn
     */
    public static void bindTabActions(JButton[] buttons, String[] tabNames,
                                      JPanel cardPanel, IntConsumer onTabSelected) {
        for (int i = 0; i < buttons.length; i++) {
            final int tabIndex = i;
            buttons[i].addActionListener(e -> {
                CardLayout cl = (CardLayout) cardPanel.getLayout();
                cl.show(cardPanel, tabNames[tabIndex]);
                onTabSelected.accept(tabIndex);
            });
        }
    }
}
