package com.nvhuong.devworkbench.common;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Objects;

/**
 * Swing Lớp phụ trợ tiết kiệm chống rung tại hiện trường.
 * <p>
 * Tất cả Timer Hoạt động đã hội tụ EDT，tránh UI Lệnh được đặt thường xuyên khi sự kiện được kích hoạt liên tục.
 */
public class DebouncedSaveSupport {
    private final Timer timer;
    private final Runnable saveAction;

    public DebouncedSaveSupport(int delayMillis, Runnable saveAction) {
        this.saveAction = Objects.requireNonNull(saveAction, "saveAction");
        this.timer = new Timer(delayMillis, e -> this.saveAction.run());
        this.timer.setRepeats(false);
    }

    public void requestSave() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestSave);
            return;
        }
        timer.restart();
    }

    public void cancel() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(this::cancel);
            } catch (Exception e) {
                timer.stop();
            }
            return;
        }
        timer.stop();
    }
}
