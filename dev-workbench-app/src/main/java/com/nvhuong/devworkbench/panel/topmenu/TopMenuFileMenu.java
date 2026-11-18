package com.nvhuong.devworkbench.panel.topmenu;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.constants.ConfigPathConstants;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.panel.env.EnvironmentPanel;
import com.nvhuong.devworkbench.panel.lifecycle.AppExitCoordinator;
import com.nvhuong.devworkbench.service.setting.ShortcutManager;
import com.nvhuong.devworkbench.util.FileChooserUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Trình đơn tập tin hàng đầu.
 */
@Slf4j
@UtilityClass
class TopMenuFileMenu {

    JMenu create(Component parent) {
        JMenu fileMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_FILE));

        JMenuItem exportCollectionsMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXPORT_COLLECTIONS));
        exportCollectionsMenuItem.setIcon(IconUtil.createThemed("icons/export.svg", 16, 16));
        exportCollectionsMenuItem.addActionListener(e -> exportAllCollections(parent));
        fileMenu.add(exportCollectionsMenuItem);

        JMenuItem exportEnvironmentsMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXPORT_ENVIRONMENTS));
        exportEnvironmentsMenuItem.setIcon(IconUtil.createThemed("icons/export.svg", 16, 16));
        exportEnvironmentsMenuItem.addActionListener(e -> UiSingletonFactory.getInstance(EnvironmentPanel.class).exportEnvironments());
        fileMenu.add(exportEnvironmentsMenuItem);

        fileMenu.addSeparator();

        JMenuItem logMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG));
        logMenuItem.addActionListener(e -> openLogDirectory(parent));
        fileMenu.add(logMenuItem);

        JMenuItem exitMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXIT));
        KeyStroke exitKey = ShortcutManager.getKeyStroke(ShortcutManager.EXIT_APP);
        if (exitKey != null) {
            exitMenuItem.setAccelerator(exitKey);
        }
        exitMenuItem.addActionListener(e -> BeanFactory.getBean(AppExitCoordinator.class).exitApplication());
        fileMenu.add(exitMenuItem);

        return fileMenu;
    }

    private void exportAllCollections(Component parent) {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "collections.export",
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File(CollectionTreePanel.EXPORT_FILE_NAME));
        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                leftPanel.getCollectionTreePersistence().exportCurrentTree(fileToSave);
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS));
            } catch (Exception ex) {
                log.error("Export error", ex);
                JOptionPane.showMessageDialog(parent,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openLogDirectory(Component parent) {
        try {
            Desktop.getDesktop().open(new File(ConfigPathConstants.LOG_DIR));
        } catch (IOException ex) {
            log.error("Failed to open log directory", ex);
            JOptionPane.showMessageDialog(parent,
                    I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LOG_MESSAGE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
