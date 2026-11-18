package com.nvhuong.devworkbench.common.component;

import com.nvhuong.devworkbench.util.EditorThemeUtil;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Shared scroll pane for RSyntax/RTextArea based editors.
 */
public class SyntaxEditorScrollPane extends RTextScrollPane {

    public SyntaxEditorScrollPane(RTextArea textArea) {
        super(textArea);
        refreshEditorChrome();
    }

    public void refreshEditorChrome() {
        EditorThemeUtil.applyScrollPaneChrome(this);
    }
}
