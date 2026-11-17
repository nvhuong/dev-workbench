package com.laker.postman.panel.topmenu;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.util.FontManager;
import com.laker.postman.util.UIRefreshManager;
import lombok.experimental.UtilityClass;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Menu ngôn ngữ hàng đầu.
 */
@UtilityClass
class TopMenuLanguageMenu {

    JMenu create() {
        JMenu languageMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_LANGUAGE));
        ButtonGroup languageGroup = new ButtonGroup();

        JRadioButtonMenuItem englishItem = new JRadioButtonMenuItem("English");
        JRadioButtonMenuItem vietnameseItem = new JRadioButtonMenuItem("Tiếng Việt");

        languageGroup.add(englishItem);
        languageGroup.add(vietnameseItem);

        if ("vi".equals(I18nUtil.currentLocale().getLanguage())) {
            vietnameseItem.setSelected(true);
        } else {
            englishItem.setSelected(true);
        }

        englishItem.addActionListener(e -> switchLanguage("en"));
        vietnameseItem.addActionListener(e -> switchLanguage("vi"));

        languageMenu.add(englishItem);
        languageMenu.add(vietnameseItem);
        return languageMenu;
    }

    private void switchLanguage(String languageCode) {
        I18nUtil.setLocale(languageCode);
        FontManager.applyFontSettings();
        UIRefreshManager.refreshLanguage();
        NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.LANGUAGE_CHANGED));
    }
}
