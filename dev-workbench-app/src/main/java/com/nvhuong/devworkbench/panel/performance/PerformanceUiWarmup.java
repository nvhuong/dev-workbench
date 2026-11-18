package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@UtilityClass
public class PerformanceUiWarmup {
    private static final int WARMUP_DELAY_MS = 250;
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean warmed = new AtomicBoolean(false);

    public void schedule() {
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(WARMUP_DELAY_MS, e -> warmUp());
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void warmUp() {
        if (!warmed.compareAndSet(false, true)) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(PerformanceUiWarmup::warmUp);
            return;
        }
        try {
            DevWorkbenchJSpinner spinner = DevWorkbenchJSpinner.intSpinner(1, 1, 1, 1);
            spinner.getPreferredSize();
        } catch (RuntimeException e) {
            log.debug("Skipped performance UI warmup after optional spinner initialization failed", e);
        }
    }
}
