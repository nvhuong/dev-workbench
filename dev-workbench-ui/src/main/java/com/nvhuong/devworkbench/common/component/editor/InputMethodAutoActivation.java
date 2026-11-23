package com.nvhuong.devworkbench.common.component.editor;

import lombok.experimental.UtilityClass;
import org.fife.ui.autocomplete.AutoCompletion;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Makes {@link AutoCompletion} auto-activate for input methods. The library only auto-activates when exactly one
 * character is inserted, but Vietnamese input methods (Telex/VNI on macOS, EVKey, OpenKey...) and other IMEs commit a
 * whole word in one insert, so completion never popped up while typing. A short word (2-40 characters, no whitespace)
 * inserted at once while auto-activation is enabled is treated like typing; pastes and programmatic text are not.
 */
@UtilityClass
public class InputMethodAutoActivation {
    private static final int MAX_WORD_LENGTH = 40;

    public static void install(JTextComponent editor, AutoCompletion completion) {
        install(editor, completion, () -> {
            if (editor.isFocusOwner() && completion.isAutoActivationEnabled() && !completion.isPopupVisible()) {
                completion.doCompletion();
            }
        });
    }

    static void install(JTextComponent editor, AutoCompletion completion, Runnable trigger) {
        Timer timer = new Timer(Math.max(0, completion.getAutoActivationDelay()), event -> trigger.run());
        timer.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                // Checked at insert time: callers disable auto-activation only around programmatic setText().
                if (completion.isAutoActivationEnabled() && isCommittedWord(editor, event)) {
                    timer.restart();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
            }
        });
    }

    static boolean isCommittedWord(JTextComponent editor, DocumentEvent event) {
        int length = event.getLength();
        if (length < 2 || length > MAX_WORD_LENGTH) {
            return false;
        }
        try {
            String inserted = editor.getDocument().getText(event.getOffset(), length);
            for (int i = 0; i < inserted.length(); i++) {
                if (Character.isWhitespace(inserted.charAt(i))) {
                    return false;
                }
            }
            char last = inserted.charAt(inserted.length() - 1);
            return Character.isLetterOrDigit(last) || last == '_' || last == '.';
        } catch (BadLocationException ignored) {
            return false;
        }
    }
}
