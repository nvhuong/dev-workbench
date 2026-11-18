package com.nvhuong.devworkbench.panel.performance.timer;

import com.nvhuong.devworkbench.performance.core.timer.TimerData;


import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.panel.performance.PerformanceStagePropertyLayout;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class TimerPropertyPanel extends JPanel {
    private final DevWorkbenchJSpinner delaySpinner;
    private PerformanceTreeNode currentNode;

    public TimerPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 120));
        setPreferredSize(new Dimension(380, 100));
        PerformanceStagePropertyLayout.applyCompactBorder(this);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TIMER_DELAY));
        add(label, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 0, 6, 6); // Khoảng cách bên trái là 0，Khoảng cách phù hợp là6
        delaySpinner = DevWorkbenchJSpinner.intSpinner(1000, 0, 60000, 100);
        delaySpinner.setPreferredSize(new Dimension(100, 28));
        add(delaySpinner, gbc);
        // Hướng dẫn trợ giúp
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel helpLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TIMER_HINT));
        helpLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        helpLabel.setForeground(ModernColors.getTextSecondary());
        add(helpLabel, gbc);
        // Không gian được chiếm hết chiều cao
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(Box.createVerticalGlue(), gbc);
    }

    public void setTimerData(PerformanceTreeNode node) {
        this.currentNode = node;
        TimerData data = node.timerData;
        if (data == null) {
            data = new TimerData();
            node.timerData = data;
        }
        delaySpinner.setValue(data.delayMs);
    }

    public void saveTimerData() {
        if (currentNode == null) return;
        TimerData data = currentNode.timerData;
        if (data == null) {
            data = new TimerData();
            currentNode.timerData = data;
        }
        data.delayMs = delaySpinner.getCommittedIntValue();
    }

    public void forceCommitAllSpinners() {
        delaySpinner.forceCommit();
    }
}
