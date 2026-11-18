package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.common.component.FallbackAwareRSyntaxTextArea;
import com.nvhuong.devworkbench.common.component.SearchableTextArea;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

/** Shared editor and actions for YAML/XML toolbox tools. */
@Slf4j
public abstract class StructuredTextToolPanel extends JPanel {
    private final String formatName;
    private final String inputKey;
    private final String validKey;
    private final String invalidKey;
    private final String syntaxStyle;
    private RSyntaxTextArea inputArea;
    private RSyntaxTextArea outputArea;
    private JLabel statusLabel;

    protected StructuredTextToolPanel(String formatName, String inputKey, String validKey,
                                      String invalidKey, String syntaxStyle) {
        this.formatName = formatName;
        this.inputKey = inputKey;
        this.validKey = validKey;
        this.invalidKey = invalidKey;
        this.syntaxStyle = syntaxStyle;
        initUI();
    }

    protected abstract String formatText(String input, boolean compact) throws Exception;
    protected abstract void validateText(String input) throws Exception;

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);
        JButton format = button(MessageKeys.TOOLBOX_SQL_FORMAT);
        JButton compress = button(MessageKeys.TOOLBOX_SQL_COMPRESS);
        JButton validate = button(MessageKeys.TOOLBOX_SQL_VALIDATE);
        JPanel left = ToolboxWorkbench.leftToolbar(format, compress, validate);
        JButton copy = button(MessageKeys.BUTTON_COPY);
        JButton paste = button(MessageKeys.TOOLBOX_SQL_PASTE);
        JButton clear = button(MessageKeys.BUTTON_CLEAR);
        JButton swap = button(MessageKeys.TOOLBOX_SQL_SWAP);
        JPanel right = ToolboxWorkbench.rightToolbar(copy, paste, clear, swap);
        add(ToolboxWorkbench.toolbar(left, right), BorderLayout.NORTH);

        inputArea = createTextArea(true);
        outputArea = createTextArea(false);
        SearchableTextArea inputSearch = new SearchableTextArea(inputArea);
        inputSearch.setLineNumbersEnabled(true);
        SearchableTextArea outputSearch = new SearchableTextArea(outputArea, false);
        outputSearch.setLineNumbersEnabled(true);
        JPanel input = ToolboxWorkbench.editorSection(I18nUtil.getMessage(inputKey), inputSearch);
        JPanel output = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_OUTPUT), outputSearch);
        add(ToolboxWorkbench.editorSplit(input, output, 300), BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        add(ToolboxWorkbench.statusBar(statusLabel), BorderLayout.SOUTH);

        format.addActionListener(e -> transform(false));
        compress.addActionListener(e -> transform(true));
        validate.addActionListener(e -> validateInput());
        copy.addActionListener(e -> copyOutput());
        paste.addActionListener(e -> pasteInput());
        clear.addActionListener(e -> clearAll());
        swap.addActionListener(e -> swapContent());
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "format", e -> transform(false));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "compress", e -> transform(true));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "validate", e -> validateInput());
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "clear", e -> clearAll());
    }

    private JButton button(String key) { return new JButton(I18nUtil.getMessage(key)); }

    private RSyntaxTextArea createTextArea(boolean editable) {
        RSyntaxTextArea area = new FallbackAwareRSyntaxTextArea(10, 40);
        area.setSyntaxEditingStyle(syntaxStyle);
        area.setEditable(editable);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setAutoIndentEnabled(true);
        area.setTabSize(2);
        area.setTabsEmulated(true);
        area.setMarkOccurrences(true);
        EditorThemeUtil.loadTheme(area);
        EditorThemeUtil.installViewportClippedTokenPainter(area);
        return area;
    }

    private void bind(KeyStroke stroke, String name, java.awt.event.ActionListener listener) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { listener.actionPerformed(event); }
        });
    }

    private void transform(boolean compact) {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_EMPTY), false);
            return;
        }
        try {
            String output = formatText(input, compact);
            outputArea.setText(output);
            if (compact) {
                int saved = input.length() - output.length();
                String percent = String.format(Locale.ROOT, "%.1f", saved * 100.0 / input.length());
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_COMPRESSED,
                        Integer.toString(saved), percent), true);
            } else {
                int lines = output.split("\\R", -1).length;
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_FORMATTED,
                        Integer.toString(lines), Integer.toString(output.length())), true);
            }
        } catch (Exception ex) {
            log.debug("Could not process {} input", formatName, ex);
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_ERROR);
            outputArea.setText("❌ " + message + ":\n\n" + safeMessage(ex));
            updateStatus("❌ " + I18nUtil.getMessage(invalidKey), false);
        }
    }

    private void validateInput() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_EMPTY));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_EMPTY), false);
            return;
        }
        try {
            validateText(input);
            outputArea.setText(I18nUtil.getMessage(validKey));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_STATUS_VALID, formatName), true);
        } catch (Exception ex) {
            log.debug("Invalid {} input", formatName, ex);
            outputArea.setText("❌ " + I18nUtil.getMessage(invalidKey) + ":\n\n" + safeMessage(ex));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_STATUS_INVALID, formatName), false);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private void copyOutput() {
        if (outputArea.getText().isEmpty()) {
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_OUTPUT_EMPTY), false);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(outputArea.getText()), null);
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_COPIED), true);
    }

    private void pasteInput() {
        try {
            Object value = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (value instanceof String text) {
                inputArea.setText(text);
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_PASTED), true);
            }
        } catch (Exception ex) {
            log.debug("Could not paste structured text", ex);
            updateStatus(safeMessage(ex), false);
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_CLEARED), true);
    }

    private void swapContent() {
        String input = inputArea.getText();
        inputArea.setText(outputArea.getText());
        outputArea.setText(input);
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_STRUCTURED_SWAPPED), true);
    }

    private void updateStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setForeground(success ? ModernColors.getSuccess() : ModernColors.getError());
        Timer timer = new Timer(3000, e -> statusLabel.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }
}
