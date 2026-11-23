package com.nvhuong.devworkbench.common.component.editor;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.testng.annotations.Test;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class InputMethodAutoActivationTest {

    /**
     * Vietnamese input methods commit a whole word in one insert; AutoCompletion only auto-activates on single
     * characters, so script/mock editors never showed suggestions for those users.
     */
    @Test
    public void wordsCommittedAtOnceTriggerCompletionLikeTyping() throws Exception {
        AtomicInteger triggers = new AtomicInteger();
        JTextArea[] area = new JTextArea[1];
        AutoCompletion[] completion = new AutoCompletion[1];
        SwingUtilities.invokeAndWait(() -> {
            area[0] = new JTextArea();
            completion[0] = new AutoCompletion(new DefaultCompletionProvider());
            completion[0].setAutoActivationEnabled(true);
            completion[0].setAutoActivationDelay(100);
            completion[0].install(area[0]);
            InputMethodAutoActivation.install(area[0], completion[0], triggers::incrementAndGet);
        });

        SwingUtilities.invokeAndWait(() -> area[0].append("pm"));
        waitFor(() -> triggers.get() == 1);
        assertEquals(triggers.get(), 1, "word committed at once must trigger completion");

        SwingUtilities.invokeAndWait(() -> {
            area[0].append(" ");
            area[0].append("pm.environment.set('a', 1);\n"); // pasted code
            area[0].append("x");                            // single key: library handles it
            completion[0].setAutoActivationEnabled(false);   // programmatic setText
            area[0].append("loaded");
            completion[0].setAutoActivationEnabled(true);
        });
        Thread.sleep(400);
        assertEquals(triggers.get(), 1, "pastes, single keys and programmatic text must not trigger");
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) Thread.sleep(20);
    }
}
