package com.nvhuong.devworkbench.panel.performance.controller;

import com.nvhuong.devworkbench.performance.core.controller.LoopData;


import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import com.nvhuong.devworkbench.panel.performance.PerformanceStagePropertyLayout;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class LoopPropertyPanel extends JPanel {
    private final DevWorkbenchJSpinner iterationsSpinner;
    private PerformanceTreeNode currentNode;

    public LoopPropertyPanel() {
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);

        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_ITERATIONS));
        iterationsSpinner = DevWorkbenchJSpinner.intSpinner(
                LoopData.MIN_ITERATIONS,
                LoopData.MIN_ITERATIONS,
                LoopData.MAX_ITERATIONS,
                1
        );
        PerformanceStagePropertyLayout.configureFieldWidth(
                iterationsSpinner,
                PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                96
        );
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc, label, iterationsSpinner);

        PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);
    }

    public void setLoopData(PerformanceTreeNode node) {
        currentNode = node;
        LoopData data = resolveLoopData(node);
        iterationsSpinner.setValue(data.iterations);
    }

    public void saveLoopData() {
        if (currentNode == null) {
            return;
        }
        LoopData data = resolveLoopData(currentNode);
        data.iterations = iterationsSpinner.getCommittedIntValue();
        data.normalize();
        currentNode.name = buildLoopTitle(data);
    }

    public void forceCommitAllSpinners() {
        iterationsSpinner.forceCommit();
    }

    private LoopData resolveLoopData(PerformanceTreeNode node) {
        LoopData data = node.loopData;
        if (data == null) {
            data = new LoopData();
            node.loopData = data;
        }
        data.normalize();
        return data;
    }

    private String buildLoopTitle(LoopData data) {
        return I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_NODE)
                + " [" + data.iterations + "x]";
    }
}
