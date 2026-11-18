package com.nvhuong.devworkbench.panel.topmenu;

import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.panel.topmenu.help.ChangelogDialog;
import com.nvhuong.devworkbench.panel.topmenu.help.MemoryTuningDialog;
import com.nvhuong.devworkbench.panel.lifecycle.AppExitCoordinator;
import com.nvhuong.devworkbench.panel.lifecycle.ApplicationRestartSupport;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;

/**
 * Trình đơn trợ giúp hàng đầu.
 */
@Slf4j
@UtilityClass
class TopMenuHelpMenu {

    JMenu create(Component parent) {
        JMenu helpMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_HELP));

        JMenuItem restartMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_RESTART));
        restartMenuItem.addActionListener(e -> restartApplication(parent));
        helpMenu.add(restartMenuItem);

        JMenuItem changelogMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_CHANGELOG));
        changelogMenuItem.addActionListener(e -> showChangelogDialog(parent));
        helpMenu.add(changelogMenuItem);

        JMenuItem memoryTuningMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_MEMORY_TUNING));
        memoryTuningMenuItem.addActionListener(e -> showMemoryTuningDialog(parent));
        helpMenu.add(memoryTuningMenuItem);

        JMenuItem feedbackMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_FEEDBACK));
        feedbackMenuItem.addActionListener(e -> showFeedbackDialog(parent));
        helpMenu.add(feedbackMenuItem);

        return helpMenu;
    }

    private void restartApplication(Component parent) {
        ApplicationRestartSupport.RestartPlan restartPlan = ApplicationRestartSupport.prepare();
        if (restartPlan == null) {
            JOptionPane.showMessageDialog(
                    TopMenuDialogOwner.resolve(parent),
                    I18nUtil.getMessage(MessageKeys.APP_RESTART_UNAVAILABLE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        BeanFactory.getBean(AppExitCoordinator.class).restartApplication(restartPlan);
    }

    private void showChangelogDialog(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof Frame frame) {
            ChangelogDialog.showDialog(frame);
        } else {
            log.warn("Cannot show changelog dialog: parent is not a Frame");
        }
    }

    private void showMemoryTuningDialog(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        MemoryTuningDialog.showDialog(window instanceof Frame frame ? frame : null);
    }

    private void showFeedbackDialog(Component parent) {
        JOptionPane.showMessageDialog(TopMenuDialogOwner.resolve(parent),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_MESSAGE),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_TITLE),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
