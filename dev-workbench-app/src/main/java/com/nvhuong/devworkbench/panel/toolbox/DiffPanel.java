package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Text and host file/folder comparison tool. */
@Slf4j
public class DiffPanel extends JPanel {
    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    private static final int MAX_FOLDER_FILES = 20_000;
    private static final String CARD_TEXT = "text";
    private static final String CARD_PATHS = "paths";

    private JTextArea originalArea;
    private JTextArea modifiedArea;
    private JTextPane resultPane;
    private JComboBox<String> modeSelector;
    private JTextField sourceAPath;
    private JTextField sourceBPath;
    private JPanel inputCards;
    private CardLayout inputCardLayout;

    public DiffPanel() {
        initUI();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);

        JButton compareBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_COMPARE), true, "icons/check.svg", 14);
        JButton copyBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_COPY), false, "icons/copy.svg", 14);
        JButton clearBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR), false, "icons/delete.svg", 14);
        JButton swapBtn = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_SWAP), false);
        modeSelector = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODE_TEXT),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODE_FILE),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODE_FOLDER)
        });
        modeSelector.setPreferredSize(new Dimension(132, 30));
        JPanel modeControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        modeControl.setOpaque(false);
        JLabel modeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODE));
        modeLabel.setForeground(ModernColors.getTextSecondary());
        modeControl.add(modeLabel);
        modeControl.add(modeSelector);

        JPanel actions = ToolboxWorkbench.leftToolbar(compareBtn, copyBtn, swapBtn, clearBtn);
        add(ToolboxWorkbench.toolbar(actions, modeControl), BorderLayout.NORTH);

        inputCards = new JPanel();
        inputCardLayout = new CardLayout();
        inputCards.setLayout(inputCardLayout);
        inputCards.setOpaque(false);
        inputCards.add(createTextInputPanel(), CARD_TEXT);
        inputCards.add(createPathInputPanel(), CARD_PATHS);

        resultPane = new JTextPane();
        resultPane.setEditable(false);
        resultPane.setFont(FontsUtil.getMonospacedFontWithOffset(Font.PLAIN, 0));
        resultPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ToolWindowSurfaceStyle.applyTextComponentCard(resultPane);
        JScrollPane resultScrollPane = new JScrollPane(resultPane);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(resultScrollPane);
        JPanel resultPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_RESULT), resultScrollPane);

        JSplitPane mainSplitPane = ToolboxWorkbench.editorSplit(inputCards, resultPanel, 250);
        mainSplitPane.setDividerSize(7);
        mainSplitPane.setResizeWeight(0.45);
        add(mainSplitPane, BorderLayout.CENTER);

        modeSelector.addActionListener(e -> {
            updateInputMode();
            SwingUtilities.invokeLater(() -> mainSplitPane.setDividerLocation(
                    modeSelector.getSelectedIndex() == 0 ? 250 : 150));
        });
        compareBtn.addActionListener(e -> compareDiff());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> clearInputs());
        swapBtn.addActionListener(e -> swapInputs());
        updateInputMode();
    }

    private JComponent createTextInputPanel() {
        originalArea = createTextArea();
        modifiedArea = createTextArea();
        JScrollPane originalScrollPane = new JScrollPane(originalArea);
        JScrollPane modifiedScrollPane = new JScrollPane(modifiedArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(originalScrollPane);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(modifiedScrollPane);
        JPanel originalPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_ORIGINAL), originalScrollPane);
        JPanel modifiedPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODIFIED), modifiedScrollPane);
        JSplitPane split = ToolboxWorkbench.horizontalSplit(originalPanel, modifiedPanel, 360);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.5));
        return split;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(FontsUtil.getMonospacedFontWithOffset(Font.PLAIN, 0));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ToolWindowSurfaceStyle.applyTextComponentInput(area);
        return area;
    }

    private JComponent createPathInputPanel() {
        JPanel sourceA = createPathSource(true);
        JPanel sourceB = createPathSource(false);
        JPanel sources = new JPanel(new GridLayout(1, 2, 12, 0));
        sources.setOpaque(false);
        sources.add(sourceA);
        sources.add(sourceB);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(sources, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createPathSource(boolean isSourceA) {
        String title = I18nUtil.getMessage(isSourceA
                ? MessageKeys.TOOLBOX_DIFF_SOURCE_A : MessageKeys.TOOLBOX_DIFF_SOURCE_B);
        JTextField pathField = new JTextField();
        pathField.setEditable(false);
        pathField.setToolTipText(title);
        pathField.putClientProperty("JTextField.placeholderText",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_PATH_PLACEHOLDER));
        pathField.setPreferredSize(new Dimension(160, 34));
        pathField.setCaretPosition(0);
        ToolWindowSurfaceStyle.applyTextComponentInput(pathField);
        JButton browseButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_BROWSE), false);
        browseButton.setPreferredSize(new Dimension(104, 34));
        browseButton.addActionListener(e -> choosePath(isSourceA));
        JLabel badge = new JLabel(isSourceA ? "A" : "B", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(ModernColors.withAlpha(ModernColors.getPrimary(), ModernColors.isDarkTheme() ? 72 : 32));
        badge.setForeground(ModernColors.getPrimary());
        badge.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        badge.setPreferredSize(new Dimension(28, 28));
        badge.setBorder(BorderFactory.createLineBorder(
                ModernColors.withAlpha(ModernColors.getPrimary(), 96), 1, true));
        JLabel sourceTitle = ToolboxWorkbench.sectionTitle(title);
        sourceTitle.setForeground(ModernColors.getTextPrimary());
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        heading.setOpaque(false);
        heading.add(badge);
        heading.add(sourceTitle);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(pathField, BorderLayout.CENTER);
        row.add(browseButton, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        ToolWindowSurfaceStyle.applySectionCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(row, BorderLayout.CENTER);
        if (isSourceA) {
            sourceAPath = pathField;
        } else {
            sourceBPath = pathField;
        }
        return panel;
    }

    private void choosePath(boolean isSourceA) {
        boolean folderMode = modeSelector.getSelectedIndex() == 2;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18nUtil.getMessage(isSourceA
                ? MessageKeys.TOOLBOX_DIFF_SOURCE_A : MessageKeys.TOOLBOX_DIFF_SOURCE_B));
        chooser.setFileSelectionMode(folderMode
                ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        String current = (isSourceA ? sourceAPath : sourceBPath).getText();
        if (!current.isBlank()) {
            Path currentPath = Path.of(current);
            Path initial = Files.isDirectory(currentPath) ? currentPath : currentPath.getParent();
            if (initial != null && Files.exists(initial)) {
                chooser.setCurrentDirectory(initial.toFile());
            }
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            (isSourceA ? sourceAPath : sourceBPath).setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private void updateInputMode() {
        inputCardLayout.show(inputCards, modeSelector.getSelectedIndex() == 0 ? CARD_TEXT : CARD_PATHS);
    }

    private void compareDiff() {
        if (modeSelector.getSelectedIndex() == 0) {
            compareText(originalArea.getText(), modifiedArea.getText());
        } else if (modeSelector.getSelectedIndex() == 1) {
            compareFiles();
        } else {
            compareFolders();
        }
    }

    private void compareText(String original, String modified) {
        if (original.isEmpty() && modified.isEmpty()) {
            resultPane.setText("⚠️ Please enter text in both areas");
            return;
        }
        try {
            StyledDocument doc = resetResultDocument();
            DiffStyles styles = createStyles();
            appendLineDiff(doc, styles, original, modified, null, true, false);
            resultPane.setCaretPosition(0);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void compareFiles() {
        Path left = selectedPath(sourceAPath);
        Path right = selectedPath(sourceBPath);
        if (left == null || right == null) {
            showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_SELECT_BOTH));
            return;
        }
        if (!Files.isRegularFile(left) || !Files.isRegularFile(right)) {
            showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_INVALID_SELECTION));
            return;
        }
        try {
            byte[] leftBytes = readFile(left);
            byte[] rightBytes = readFile(right);
            if (Arrays.equals(leftBytes, rightBytes)) {
                showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FILE_UNCHANGED) + ": " + left + " ↔ " + right);
                return;
            }
            String leftText = decodeText(leftBytes);
            String rightText = decodeText(rightBytes);
            if (leftText == null || rightText == null) {
                showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_BINARY) + "\n" + left + "\n" + right);
                return;
            }
            StyledDocument doc = resetResultDocument();
            DiffStyles styles = createStyles();
            appendStyled(doc, styles.header(), left + "  ↔  " + right + "\n\n");
            appendLineDiff(doc, styles, leftText, rightText, null, true, true);
            resultPane.setCaretPosition(0);
        } catch (IOException | BadLocationException ex) {
            showError(ex);
        }
    }

    private void compareFolders() {
        Path left = selectedPath(sourceAPath);
        Path right = selectedPath(sourceBPath);
        if (left == null || right == null) {
            showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_SELECT_BOTH));
            return;
        }
        if (!Files.isDirectory(left) || !Files.isDirectory(right)) {
            showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_INVALID_SELECTION));
            return;
        }
        try {
            Map<String, Path> leftFiles = listFiles(left);
            Map<String, Path> rightFiles = listFiles(right);
            TreeSet<String> paths = new TreeSet<>();
            paths.addAll(leftFiles.keySet());
            paths.addAll(rightFiles.keySet());

            int added = 0;
            int removed = 0;
            int modified = 0;
            int unchanged = 0;
            StyledDocument doc = resetResultDocument();
            DiffStyles styles = createStyles();
            List<FolderChange> changes = new ArrayList<>();
            for (String relativePath : paths) {
                Path leftFile = leftFiles.get(relativePath);
                Path rightFile = rightFiles.get(relativePath);
                if (leftFile == null) {
                    added++;
                    changes.add(new FolderChange(relativePath, null, rightFile, ChangeType.ADDED));
                } else if (rightFile == null) {
                    removed++;
                    changes.add(new FolderChange(relativePath, leftFile, null, ChangeType.REMOVED));
                } else if (Files.mismatch(leftFile, rightFile) == -1) {
                    unchanged++;
                } else {
                    modified++;
                    changes.add(new FolderChange(relativePath, leftFile, rightFile, ChangeType.MODIFIED));
                }
            }

            appendStyled(doc, styles.header(), I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FOLDER_SUMMARY,
                    added, removed, modified, unchanged) + "\n");
            for (FolderChange change : changes) {
                appendStyled(doc, styles.header(), "\n" + statusLabel(change.type()) + "  " + change.relativePath() + "\n");
                byte[] leftBytes = change.left() == null ? new byte[0] : readFile(change.left());
                byte[] rightBytes = change.right() == null ? new byte[0] : readFile(change.right());
                String leftText = decodeText(leftBytes);
                String rightText = decodeText(rightBytes);
                if (leftText == null || rightText == null) {
                    appendStyled(doc, styles.removed(), I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_BINARY) + "\n");
                } else {
                    appendLineDiff(doc, styles, leftText, rightText, null, false, true);
                }
            }
            if (changes.isEmpty()) {
                appendStyled(doc, styles.normal(), "\n" + I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FILE_UNCHANGED) + "\n");
            }
            resultPane.setCaretPosition(0);
        } catch (IOException | BadLocationException ex) {
            showError(ex);
        }
    }

    private Map<String, Path> listFiles(Path root) throws IOException {
        Map<String, Path> files = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream.filter(path -> !path.equals(root))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(Files::isRegularFile)
                    .limit(MAX_FOLDER_FILES + 1L)
                    .toList();
            if (paths.size() > MAX_FOLDER_FILES) {
                throw new IOException(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_TOO_MANY_FILES, MAX_FOLDER_FILES));
            }
            for (Path path : paths) {
                files.put(root.relativize(path).toString().replace('\\', '/'), path);
            }
        }
        return files;
    }

    private byte[] readFile(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_BYTES) {
            throw new IOException(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_TOO_LARGE,
                    MAX_FILE_BYTES / (1024 * 1024)) + " " + path);
        }
        return Files.readAllBytes(path);
    }

    private String decodeText(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return null;
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private void appendLineDiff(StyledDocument doc, DiffStyles styles, String original, String modified,
                                String title, boolean includeStatistics, boolean emptyIsNoLines)
            throws BadLocationException {
        if (title != null) {
            appendStyled(doc, styles.header(), title + "\n");
        } else if (includeStatistics) {
            appendStyled(doc, styles.header(), "📊 Diff Result\n");
            appendStyled(doc, styles.normal(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        }
        String[] originalLines = emptyIsNoLines && original.isEmpty()
                ? new String[0] : original.split("\\n", -1);
        String[] modifiedLines = emptyIsNoLines && modified.isEmpty()
                ? new String[0] : modified.split("\\n", -1);
        int additions = 0;
        int deletions = 0;
        int modifications = 0;
        int maxLen = Math.max(originalLines.length, modifiedLines.length);
        for (int i = 0; i < maxLen; i++) {
            String originalLine = i < originalLines.length ? originalLines[i] : null;
            String modifiedLine = i < modifiedLines.length ? modifiedLines[i] : null;
            if (originalLine == null && modifiedLine != null) {
                additions++;
                appendStyled(doc, styles.added(), "+ " + modifiedLine + "\n");
            } else if (originalLine != null && modifiedLine == null) {
                deletions++;
                appendStyled(doc, styles.removed(), "- " + originalLine + "\n");
            } else if (originalLine != null && modifiedLine != null && !originalLine.equals(modifiedLine)) {
                modifications++;
                appendStyled(doc, styles.removed(), "- " + originalLine + "\n");
                appendStyled(doc, styles.added(), "+ " + modifiedLine + "\n");
            } else if (originalLine != null) {
                appendStyled(doc, styles.normal(), "  " + originalLine + "\n");
            }
        }
        if (includeStatistics) {
            appendStyled(doc, styles.normal(), "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            appendStyled(doc, styles.header(), "📈 Statistics:\n");
            appendStyled(doc, styles.added(), "  Additions: " + additions + " line(s)\n");
            appendStyled(doc, styles.removed(), "  Deletions: " + deletions + " line(s)\n");
            appendStyled(doc, styles.normal(), "  Modifications: " + modifications + " line(s)\n");
            appendStyled(doc, styles.header(), "  Total changes: " + (additions + deletions + modifications) + "\n");
        }
    }

    private StyledDocument resetResultDocument() throws BadLocationException {
        StyledDocument doc = resultPane.getStyledDocument();
        doc.remove(0, doc.getLength());
        return doc;
    }

    private DiffStyles createStyles() {
        Style normal = resultPane.addStyle("diff-normal", null);
        StyleConstants.setForeground(normal, ModernColors.getTextPrimary());
        Style added = resultPane.addStyle("diff-added", null);
        StyleConstants.setForeground(added, ModernColors.getSuccess());
        StyleConstants.setBackground(added, ModernColors.withAlpha(ModernColors.getSuccess(), ModernColors.isDarkTheme() ? 64 : 36));
        Style removed = resultPane.addStyle("diff-removed", null);
        StyleConstants.setForeground(removed, ModernColors.getError());
        StyleConstants.setBackground(removed, ModernColors.withAlpha(ModernColors.getError(), ModernColors.isDarkTheme() ? 64 : 36));
        Style header = resultPane.addStyle("diff-header", null);
        StyleConstants.setForeground(header, ModernColors.getPrimary());
        StyleConstants.setBold(header, true);
        return new DiffStyles(normal, added, removed, header);
    }

    private void appendStyled(StyledDocument doc, Style style, String text) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }

    private String statusLabel(ChangeType type) {
        return switch (type) {
            case ADDED -> I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FILE_ADDED);
            case REMOVED -> I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FILE_REMOVED);
            case MODIFIED -> I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_FILE_MODIFIED);
        };
    }

    private Path selectedPath(JTextField field) {
        String value = field.getText().trim();
        return value.isEmpty() ? null : Path.of(value);
    }

    private void showError(Exception ex) {
        log.error("Diff comparison error", ex);
        showMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_READ_FAILED, ex.getMessage()));
    }

    private void showMessage(String message) {
        resultPane.setText(message);
    }

    private void clearInputs() {
        originalArea.setText("");
        modifiedArea.setText("");
        sourceAPath.setText("");
        sourceBPath.setText("");
        resultPane.setText("");
    }

    private void swapInputs() {
        if (modeSelector.getSelectedIndex() == 0) {
            String temp = originalArea.getText();
            originalArea.setText(modifiedArea.getText());
            modifiedArea.setText(temp);
        } else {
            String temp = sourceAPath.getText();
            sourceAPath.setText(sourceBPath.getText());
            sourceBPath.setText(temp);
        }
    }

    private void copyToClipboard() {
        String text = resultPane.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }

    private record DiffStyles(Style normal, Style added, Style removed, Style header) { }
    private record FolderChange(String relativePath, Path left, Path right, ChangeType type) { }
    private enum ChangeType { ADDED, REMOVED, MODIFIED }
}
