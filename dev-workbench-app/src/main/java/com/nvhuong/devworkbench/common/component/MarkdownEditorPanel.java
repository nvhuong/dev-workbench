package com.nvhuong.devworkbench.common.component;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.service.render.HttpHtmlRenderer;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FileChooserUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown Thành phần soạn thảo
 * Hỗ trợ xem trước, thanh công cụ và hoàn tác theo thời gian thực/Làm lại, xuất và các chức năng khác
 */
public class MarkdownEditorPanel extends JPanel {
    private RSyntaxTextArea editorArea;
    private SearchableTextArea searchableTextArea;
    private JTextPane previewPane;
    private JSplitPane splitPane;
    private JPanel toolbarPanel;
    private final List<DocumentListener> changeListeners = new ArrayList<>();
    private final UndoManager undoManager = new UndoManager();

    private JPanel editorPanelRef;
    private JPanel previewPanelRef;

    private static final int MODE_SPLIT = 0;
    private static final int MODE_EDIT_ONLY = 1;
    private static final int MODE_PREVIEW_ONLY = 2;
    private int viewMode = MODE_SPLIT;

    private JButton undoButton;
    private JButton redoButton;
    private boolean editable = true;

    public MarkdownEditorPanel() {
        initUI();
        setupKeyBindings();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (toolbarPanel != null && editorPanelRef != null && previewPanelRef != null) {
            removeAll();

            toolbarPanel = createEnhancedToolbar();
            add(toolbarPanel, BorderLayout.NORTH);

            add(splitPane, BorderLayout.CENTER);

            JPanel statusBar = createStatusBar();
            add(statusBar, BorderLayout.SOUTH);

            setEditable(editable);
            updatePreviewPaneStyles();

            revalidate();
            repaint();
        }
    }

    /**
     * Updated preview panel style to accommodate theme changes
     */
    private void updatePreviewPaneStyles() {
        if (previewPane != null) {
            updatePreview();
        }
    }


    /**
     * Sẽ Color Chuyển đổi sang chuỗi thập lục phân
     */
    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String getTableStyle() {
        return "border-collapse:collapse;width:100%;margin:0 0 8px 0;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableCellStyle() {
        return "padding:4px 8px;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableHeaderStyle() {
        String bgColor = toHex(ModernColors.getHoverBackgroundColor());
        return getTableCellStyle() + "font-weight:600;background-color:" + bgColor + ";";
    }

    private String getCodeBlockStyle() {
        return "background-color:" + toHex(ModernColors.getConsoleTextAreaBg()) +
                ";padding:8px;overflow:auto;font-size:10px;line-height:1.5;border-radius:4px;" +
                "margin:0 0 8px 0;font-family:monospace;color:" +
                toHex(ModernColors.getConsoleText()) +
                ";display:block;white-space:pre;word-wrap:normal;";
    }

    private String getInlineCodeStyle() {
        String bgColor = toHex(ModernColors.getHoverBackgroundColor());
        String textColor = toHex(ModernColors.getErrorDark());
        return "background-color:" + bgColor + ";color:" + textColor +
                ";padding:1px 4px;margin:0 1px;font-size:10px;border-radius:3px;font-family:monospace;";
    }

    private String getHeadingStyle(int level) {
        String dividerColor = toHex(ModernColors.getDividerBorderColor());
        return switch (level) {
            case 1 ->
                    "font-size:18px;font-weight:600;margin:4px 0 4px 0;border-bottom:2px solid " + dividerColor + ";padding-bottom:0.2em;";
            case 2 ->
                    "font-size:16px;font-weight:600;margin:4px 0 3px 0;border-bottom:1px solid " + dividerColor + ";padding-bottom:0.2em;";
            case 3 -> "font-size:14px;font-weight:600;margin:4px 0 3px 0;";
            case 4 -> "font-size:12px;font-weight:600;margin:4px 0 3px 0;";
            case 5 -> "font-size:11px;font-weight:600;margin:4px 0 3px 0;";
            case 6 ->
                    "font-size:10px;font-weight:600;margin:4px 0 3px 0;color:" + toHex(ModernColors.getTextHint()) + ";";
            default -> "";
        };
    }

    private String getBlockquoteStyle() {
        Color accentColor = ModernColors.getAccent();
        String borderColor = toHex(accentColor);
        String bgColor = String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,0.08)",
                accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue());
        return "padding:6px 10px;color:" + toHex(ModernColors.getTextSecondary()) +
                ";border-left:3px solid " + borderColor + ";background-color:" + bgColor +
                ";margin:0 0 8px 0;border-radius:0 3px 3px 0;";
    }

    private String getHrStyle() {
        return "height:2px;margin:24px 0;background-color:" + toHex(ModernColors.getDividerBorderColor()) + ";border:0;";
    }


    private void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);

        editorPanelRef = createEditorPanel();
        previewPanelRef = createPreviewPanel();

        splitPane = AppToolWindowChrome.createHorizontalInnerSplitPane(editorPanelRef, previewPanelRef, 0);
        splitPane.setResizeWeight(0.5);

        toolbarPanel = createEnhancedToolbar();
        add(toolbarPanel, BorderLayout.NORTH);

        add(splitPane, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    /**
     * Tạo một thanh công cụ
     */
    private JPanel createEnhancedToolbar() {
        JPanel toolbarContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 2));
        ToolWindowSurfaceStyle.applySectionHeader(toolbarContainer, 3, 5, 3, 5);

        undoButton = createFlatButton("↶", I18nUtil.getMessage(MessageKeys.MARKDOWN_UNDO), e -> undo());
        redoButton = createFlatButton("↷", I18nUtil.getMessage(MessageKeys.MARKDOWN_REDO), e -> redo());
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        toolbarContainer.add(undoButton);
        toolbarContainer.add(redoButton);
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("H1", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING1), "# ", ""));
        toolbarContainer.add(createFlatButton("H2", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING2), "## ", ""));
        toolbarContainer.add(createFlatButton("H3", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING3), "### ", ""));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("<html><b>B</b></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_BOLD), "**", "**"));
        toolbarContainer.add(createFlatButton("<html><i>I</i></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_ITALIC), "_", "_"));
        toolbarContainer.add(createFlatButton("<html><s>S</s></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_STRIKETHROUGH), "~~", "~~"));
        toolbarContainer.add(createFlatButton("<html><code>`</code></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_INLINE_CODE), "`", "`"));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("🔗", I18nUtil.getMessage(MessageKeys.MARKDOWN_LINK), "[", "](url)"));
        toolbarContainer.add(createFlatButton("🖼", I18nUtil.getMessage(MessageKeys.MARKDOWN_IMAGE), "![", "](url)"));
        toolbarContainer.add(createFlatActionButton("⊞", I18nUtil.getMessage(MessageKeys.MARKDOWN_TABLE), this::insertTable));
        toolbarContainer.add(createFlatButton("{}", I18nUtil.getMessage(MessageKeys.MARKDOWN_CODE_BLOCK), "```\n", "\n```"));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("•", I18nUtil.getMessage(MessageKeys.MARKDOWN_UNORDERED_LIST), "- ", ""));
        toolbarContainer.add(createFlatButton("☑", I18nUtil.getMessage(MessageKeys.MARKDOWN_TASK_LIST), "- [ ] ", ""));
        toolbarContainer.add(createFlatButton("❝", I18nUtil.getMessage(MessageKeys.MARKDOWN_QUOTE), "> ", ""));
        toolbarContainer.add(createFlatButton("─", I18nUtil.getMessage(MessageKeys.MARKDOWN_HORIZONTAL_LINE), "---\n", ""));
        toolbarContainer.add(createVerticalDivider());

        JButton moreButton = createFlatButton("⋮", I18nUtil.getMessage(MessageKeys.MARKDOWN_MORE), null);
        JPopupMenu moreMenu = createMoreMenu();
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        toolbarContainer.add(moreButton);
        toolbarContainer.add(createVerticalDivider());

        JToggleButton splitViewBtn = new JToggleButton("⚏");
        JToggleButton editViewBtn = new JToggleButton("✎");
        JToggleButton previewViewBtn = new JToggleButton("👁");

        splitViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_SPLIT));
        editViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_EDIT_ONLY));
        previewViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_PREVIEW_ONLY));

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(splitViewBtn);
        viewGroup.add(editViewBtn);
        viewGroup.add(previewViewBtn);

        styleToggleButton(splitViewBtn);
        styleToggleButton(editViewBtn);
        styleToggleButton(previewViewBtn);

        splitViewBtn.setSelected(true);

        splitViewBtn.addActionListener(e -> {
            viewMode = MODE_SPLIT;
            updateViewMode();
        });
        editViewBtn.addActionListener(e -> {
            viewMode = MODE_EDIT_ONLY;
            updateViewMode();
        });
        previewViewBtn.addActionListener(e -> {
            viewMode = MODE_PREVIEW_ONLY;
            updateViewMode();
        });

        toolbarContainer.add(splitViewBtn);
        toolbarContainer.add(editViewBtn);
        toolbarContainer.add(previewViewBtn);

        return toolbarContainer;
    }

    /**
     * Hỗ trợ gói dòng tự động FlowLayout
     */
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();

                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);

                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);

            if (dim.height > 0) {
                dim.height += getVgap();
            }

            dim.height += rowHeight;
        }
    }

    /**
     * Tạo nhiều menu chức năng hơn
     */
    private JPopupMenu createMoreMenu() {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        JMenuItem exportItem = new JMenuItem("💾 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML));
        exportItem.addActionListener(e -> exportToHtml());

        JMenuItem copyItem = new JMenuItem("📋 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_COPY_HTML));
        copyItem.addActionListener(e -> copyHtmlToClipboard());

        menu.add(exportItem);
        menu.add(copyItem);

        return menu;
    }

    private JButton createFlatButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(28, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        if (action != null) {
            button.addActionListener(action);
        }

        return button;
    }

    private JButton createFlatButton(String text, String tooltip, String prefix, String suffix) {
        return createFlatButton(text, tooltip, e -> insertFormat(prefix, suffix));
    }

    private JButton createFlatActionButton(String text, String tooltip, Runnable action) {
        return createFlatButton(text, tooltip, e -> action.run());
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setMargin(new Insets(4, 10, 4, 10));
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(32, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }

    private Component createVerticalDivider() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(ModernColors.getDividerBorderColor());
        return separator;
    }

    /**
     * Tạo một bảng soạn thảo
     */
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Tạo RSyntaxTextArea Được sử dụng cho Markdown Chỉnh sửa
        editorArea = new FallbackAwareRSyntaxTextArea();
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN); // Đặt thành  Markdown Syntax highlighting
        editorArea.setCodeFoldingEnabled(false); // Markdown Không cần gấp mã
        editorArea.setTabSize(4); // cài đặt Tab Chiều rộng là  4 không gian
        // Tải thống nhất các chủ đề, phông chữ soạn thảo và bản vẽ dự phòng cho các ký tự bị thiếu
        EditorThemeUtil.loadTheme(editorArea);

        // sử dụng SearchableTextArea Wrapper (cho phép chức năng tìm kiếm và thay thế)
        searchableTextArea = new SearchableTextArea(editorArea, true);

        // Thêm hoàn tác/Làm lại hỗ trợ
        editorArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            updateUndoRedoButtons();
        });

        // Theo dõi các thay đổi nội dung và cập nhật bản xem trước
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }
        });

        panel.add(searchableTextArea, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tạo bảng xem trước
     */
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        previewPane = new JTextPane();
        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        previewPane.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        ToolWindowSurfaceStyle.applyTextComponentCard(previewPane);
        JScrollPane scrollPane = new JScrollPane(previewPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tạo một thanh trạng thái
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        ToolWindowSurfaceStyle.applySectionHeader(statusBar);

        JLabel statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_READY));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(statusLabel);

        // Số từ
        JLabel wordCountLabel = new JLabel(String.format("%s: 0 | %s: 0",
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS),
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_CHARS)));
        wordCountLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        wordCountLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(wordCountLabel);

        // Số hàng
        JLabel positionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE) + ": 1, " +
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN) + ": 1");
        positionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        positionLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(positionLabel);

        // Cập nhật thanh trạng thái
        editorArea.addCaretListener(e -> {
            try {
                int pos = editorArea.getCaretPosition();
                int line = editorArea.getLineOfOffset(pos);
                int col = pos - editorArea.getLineStartOffset(line);
                positionLabel.setText(String.format("%s: %d, %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE), line + 1,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN), col + 1));

                String text = editorArea.getText();
                // Đếm số từ: văn bản trống hoặc chỉ có ký tự khoảng trắng0
                int wordCount = 0;
                int charCount = 0;
                if (text != null) {
                    charCount = text.length();
                    if (!text.trim().isEmpty()) {
                        wordCount = text.trim().split("\\s+").length;
                    }
                }
                wordCountLabel.setText(String.format("%s: %d | %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS), wordCount,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_CHARS), charCount));
            } catch (Exception ex) {
                // Ignore
            }
        });

        return statusBar;
    }


    /**
     * Đặt phím tắt
     */
    private void setupKeyBindings() {
        InputMap inputMap = editorArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorArea.getActionMap();

        // Ctrl+B - In đậm
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "bold");
        actionMap.put("bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("**", "**");
            }
        });

        // Ctrl+I - in nghiêng
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "italic");
        actionMap.put("italic", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("_", "_");
            }
        });

        // Ctrl+K - Liên kết
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "link");
        actionMap.put("link", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("[", "](url)");
            }
        });

        // Ctrl+Z - Hoàn tác
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Ctrl+Y - làm lại
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });


        // Ctrl+` - Mã nội tuyến
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, InputEvent.CTRL_DOWN_MASK), "inlineCode");
        actionMap.put("inlineCode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("`", "`");
            }
        });

        // Ctrl+Shift+K - Gạch ngang
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "strikethrough");
        actionMap.put("strikethrough", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("~~", "~~");
            }
        });

        // Ctrl+Shift+C - khối mã
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "codeBlock");
        actionMap.put("codeBlock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("```\n", "\n```");
            }
        });

        // Ctrl+Shift+Q - Trích dẫn
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "quote");
        actionMap.put("quote", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("> ");
            }
        });

        // Ctrl+Shift+L - Danh sách không có thứ tự
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "unorderedList");
        actionMap.put("unorderedList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("- ");
            }
        });

        // Ctrl+Shift+O - Danh sách đặt hàng
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "orderedList");
        actionMap.put("orderedList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("1. ");
            }
        });

        // Ctrl+Shift+T - Danh sách nhiệm vụ
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "taskList");
        actionMap.put("taskList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("- [ ] ");
            }
        });

        // Ctrl+1-6 - Cấp độ tiêu đề
        for (int i = 1; i <= 6; i++) {
            final int level = i;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, InputEvent.CTRL_DOWN_MASK), "heading" + i);
            actionMap.put("heading" + i, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String prefix = "#".repeat(level) + " ";
                    insertLinePrefix(prefix);
                }
            });
        }

        // Ctrl+Shift+H - đường ngang
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "horizontalLine");
        actionMap.put("horizontalLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pos = editorArea.getCaretPosition();
                editorArea.insert("\n---\n", pos);
                editorArea.requestFocus();
            }
        });

        // Ctrl+Shift+I - Chèn hình ảnh
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "image");
        actionMap.put("image", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("![", "](url)");
            }
        });

        // Ctrl+Shift+T - Chèn bảng
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "table");
        actionMap.put("table", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTable();
            }
        });

        // Ctrl+S - Kích hoạt sự kiện lưu (thông báo cho người nghe)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Kích hoạt sự kiện thay đổi tài liệu và để trình nghe bên ngoài xử lý việc lưu
                editorArea.getDocument().putProperty("save-requested", true);
            }
        });

        // Ctrl+E - Xuất khẩuHTML
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export");
        actionMap.put("export", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToHtml();
            }
        });

        // Ctrl+Shift+C - Sao chépHTML（Không xung đột với các khối mã, hãy sử dụngAlt）
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "copyHtml");
        actionMap.put("copyHtml", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyHtmlToClipboard();
            }
        });
    }

    /**
     * Chèn tiền tố vào đầu dòng (dành cho danh sách, trích dẫn, tiêu đề, v.v.)
     */
    private void insertLinePrefix(String prefix) {
        try {
            int pos = editorArea.getCaretPosition();
            int lineStart = editorArea.getLineStartOffset(editorArea.getLineOfOffset(pos));
            editorArea.insert(prefix, lineStart);
            editorArea.setCaretPosition(lineStart + prefix.length());
            editorArea.requestFocus();
        } catch (Exception e) {
            // Nếu xảy ra lỗi, hãy chèn vào vị trí con trỏ
            int pos = editorArea.getCaretPosition();
            editorArea.insert(prefix, pos);
            editorArea.requestFocus();
        }
    }

    /**
     * Chèn văn bản được định dạng
     */
    private void insertFormat(String prefix, String suffix) {
        int start = editorArea.getSelectionStart();
        int end = editorArea.getSelectionEnd();
        String selectedText = editorArea.getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            editorArea.replaceSelection(prefix + selectedText + suffix);
            editorArea.setSelectionStart(start + prefix.length());
            editorArea.setSelectionEnd(end + prefix.length());
        } else {
            editorArea.insert(prefix + suffix, start);
            editorArea.setCaretPosition(start + prefix.length());
        }
        editorArea.requestFocus();
    }

    /**
     * Chèn bảng
     */
    private void insertTable() {
        String table = """
                | cột1 | cột2 | cột3 |
                | --- | --- | --- |
                | tế bào | tế bào | tế bào |
                | tế bào | tế bào | tế bào |
                """;
        int pos = editorArea.getCaretPosition();
        editorArea.insert(table, pos);
        editorArea.requestFocus();
    }

    /**
     * Hoàn tác
     */
    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
            updateUndoRedoButtons();
        }
    }

    /**
     * làm lại
     */
    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
            updateUndoRedoButtons();
        }
    }

    /**
     * Cập nhật hoàn tác/Làm lại trạng thái nút
     */
    private void updateUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setEnabled(undoManager.canUndo());
        }
        if (redoButton != null) {
            redoButton.setEnabled(undoManager.canRedo());
        }
    }


    /**
     * Xuất dưới dạng HTML（Sử dụng văn bản quốc tế hóa)
     */
    private void exportToHtml() {
        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "markdown.export.html",
                I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML));
        fileChooser.setFileFilter(FileChooserUtil.extensionFilter("HTML tập tin", "html"));

        if (fileChooser.showSaveDialog(this) == SystemFileChooser.APPROVE_OPTION) {
            try {
                String html = convertMarkdownToHtml(editorArea.getText());
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".html")) {
                    file = new java.io.File(file.getAbsolutePath() + ".html");
                }
                java.nio.file.Files.writeString(file.toPath(), html);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_SUCCESS),
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML),
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_FAILED) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Sao chép HTML vào clipboard (sử dụng văn bản được quốc tế hóa)
     */
    private void copyHtmlToClipboard() {
        String html = convertMarkdownToHtml(editorArea.getText());
        StringSelection selection = new StringSelection(html);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        JOptionPane.showMessageDialog(this,
                I18nUtil.getMessage(MessageKeys.MARKDOWN_HTML_COPIED),
                I18nUtil.getMessage(MessageKeys.MARKDOWN_COPY_HTML),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Cập nhật chế độ xem
     */
    private void updateViewMode() {
        switch (viewMode) {
            case MODE_SPLIT:
                splitPane.setLeftComponent(editorPanelRef);
                splitPane.setRightComponent(previewPanelRef);
                splitPane.setDividerSize(AppToolWindowChrome.DIVIDER_SIZE);
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
                break;
            case MODE_EDIT_ONLY:
                splitPane.setLeftComponent(editorPanelRef);
                splitPane.setRightComponent(null);
                splitPane.setDividerSize(0);
                break;
            case MODE_PREVIEW_ONLY:
                splitPane.setLeftComponent(null);
                splitPane.setRightComponent(previewPanelRef);
                splitPane.setDividerSize(0);
                break;
            default:
                break;
        }
        splitPane.revalidate();
        splitPane.repaint();
        updatePreview();
    }

    /**
     * Cập nhật bản xem trước
     */
    private void updatePreview() {
        String markdown = editorArea.getText();
        String html = convertMarkdownToHtml(markdown);
        previewPane.setText(html);
        previewPane.setCaretPosition(0);
    }


    /**
     * Cấp doanh nghiệp Markdown Đến HTML Chuyển thành
     * Hỗ trợ đầy đủ GitHub Flavored Markdown (GFM) ngữ pháp
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "<html><body style='margin:0;padding:12px;font-size:10px;'></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("</head>");

        // Vào body được sử dụng trên inline style Đặt kiểu cơ bản (không sử dụng  StyleSheet）
        html.append("<body style='");
        html.append("margin:0;padding:8px;"); // Đặt phần đệm để làm cho nó nhỏ gọn hơn
        html.append("font-family:sans-serif;");
        html.append("font-size:10px;");
        html.append("line-height:1.6;");
        html.append("color:").append(toHex(ModernColors.getTextPrimary())).append(";");
        html.append("background:").append(toHex(ModernColors.getCardBackgroundColor())).append(";");
        html.append("'>");

        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inList = false;
        boolean inOrderedList = false;
        boolean inTable = false;
        String codeLanguage = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // khối mã
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</pre>");
                    inCodeBlock = false;
                    codeLanguage = "";
                } else {
                    // Trích xuất định danh ngôn ngữ
                    codeLanguage = line.trim().substring(3).trim();
                    html.append("<pre style='").append(getCodeBlockStyle()).append("'");
                    if (!codeLanguage.isEmpty()) {
                        html.append(" class='language-").append(escapeHtml(codeLanguage)).append("'");
                    }
                    html.append(">");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                // Xóa các ký tự khoảng trắng ở cuối để tránh hiển thị dưới dạng hộp
                String codeLine = line.replaceAll("\\s+$", "");
                html.append(escapeHtml(codeLine)).append("\n");
                continue;
            }

            // Xử lý biểu mẫu
            if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
                if (!inTable) {
                    html.append("<table style='").append(getTableStyle()).append("'>");
                    inTable = true;
                }

                // Kiểm tra xem nó có phải là một đường phân cách không
                if (line.matches("^\\|[\\s\\-:|]+\\|$")) {
                    // Bỏ qua các dòng phân cách
                    continue;
                }

                // Xác định xem đó có phải là tiêu đề bảng hay không (dòng tiếp theo là dòng tách biệt)
                boolean isHeader = false;
                if (i + 1 < lines.length && lines[i + 1].matches("^\\|[\\s\\-:|]+\\|$")) {
                    isHeader = true;
                    html.append("<thead><tr>");
                } else if (inTable && html.toString().contains("<thead>")) {
                    if (!html.toString().contains("<tbody>")) {
                        html.append("<tbody>");
                    }
                    html.append("<tr>");
                } else {
                    html.append("<tr>");
                }

                String[] cells = line.split("\\|", -1); // -1 Giữ chuỗi trống ở cuối
                // Xóa các phần tử trống ở đầu và cuối (| đầu và cuối)
                int start = cells[0].trim().isEmpty() ? 1 : 0;
                int end = cells[cells.length - 1].trim().isEmpty() ? cells.length - 1 : cells.length;

                for (int j = start; j < end; j++) {
                    String cell = cells[j].trim();
                    if (isHeader) {
                        html.append("<th style='").append(getTableHeaderStyle()).append("'>").append(processInlineMarkdown(cell)).append("</th>");
                    } else {
                        html.append("<td style='").append(getTableCellStyle()).append("'>").append(processInlineMarkdown(cell)).append("</td>");
                    }
                }

                html.append("</tr>");
                if (isHeader) {
                    html.append("</thead>");
                }
                continue;
            } else if (inTable) {
                if (html.toString().contains("<tbody>")) {
                    html.append("</tbody>");
                }
                html.append("</table>");
                inTable = false;
            }

            // tiêu đề
            if (line.startsWith("# ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h1 style='").append(getHeadingStyle(1)).append("'>").append(processInlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h2 style='").append(getHeadingStyle(2)).append("'>").append(processInlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h3 style='").append(getHeadingStyle(3)).append("'>").append(processInlineMarkdown(line.substring(4))).append("</h3>");
            } else if (line.startsWith("#### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h4 style='").append(getHeadingStyle(4)).append("'>").append(processInlineMarkdown(line.substring(5))).append("</h4>");
            } else if (line.startsWith("##### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h5 style='").append(getHeadingStyle(5)).append("'>").append(processInlineMarkdown(line.substring(6))).append("</h5>");
            } else if (line.startsWith("###### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h6 style='").append(getHeadingStyle(6)).append("'>").append(processInlineMarkdown(line.substring(7))).append("</h6>");
            }
            // đường ngang
            else if (line.trim().equals("---") || line.trim().equals("***") || line.trim().equals("___")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<hr style='").append(getHrStyle()).append("'>");
            }
            // Danh sách nhiệm vụ - Sử dụng bố cục bảng để đảm bảo căn chỉnh và tối ưu hóa kiểu dáng
            else if (line.trim().matches("^[-*]\\s+\\[[ xX]\\]\\s+.*")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                boolean checked = line.toLowerCase().contains("[x]");
                String content = line.trim().replaceFirst("^[-*]\\s+\\[[ xX]\\]\\s+", "");

                // Ghi đè hoàn toàn các kiểu bảng bằng các kiểu nội tuyến, loại bỏ tất cả các đường viền
                html.append("<table class='task-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 18px; padding-right: 6px; background: transparent; vertical-align: middle;'>");
                html.append("<input type='checkbox' disabled");
                if (checked) html.append(" checked");
                html.append(">");
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: middle;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // Danh sách không có thứ tự - Sử dụng bố cục bảng để căn lề trái hoàn toàn
            else if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                String content = line.substring(line.indexOf(" ") + 1);

                // Sử dụng bố cục bảng, giữ sự căn lề trái nhất quán với danh sách nhiệm vụ
                html.append("<table class='list-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 1%; padding-right: 6px; background: transparent; vertical-align: top; white-space: nowrap;'>");
                html.append("•"); // Unicode bullet point
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: top;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // Danh sách đặt hàng - Sử dụng bố cục bảng để căn lề trái hoàn toàn
            else if (line.trim().matches("^\\d+\\.\\s.*")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                // Trích xuất số sê-ri và nội dung
                String trimmed = line.trim();
                int dotIndex = trimmed.indexOf('.');
                String number = trimmed.substring(0, dotIndex + 1); // Bao gồm khoảng thời gian
                String content = trimmed.substring(dotIndex + 1).trim();

                // Sử dụng bố cục bảng, giữ sự căn lề trái nhất quán với danh sách nhiệm vụ
                html.append("<table class='list-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 1%; padding-right: 6px; background: transparent; vertical-align: top; text-align: left; white-space: nowrap;'>");
                html.append(number);
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: top;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // Trích dẫn
            else if (line.trim().startsWith("> ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                String content = line.substring(line.indexOf(">") + 1).trim();
                html.append("<blockquote style='").append(getBlockquoteStyle()).append("'>").append(processInlineMarkdown(content)).append("</blockquote>");
            }
            // Dòng trống
            else if (line.trim().isEmpty()) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<br>");
            }
            // Đoạn thông thường
            else {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<p style='margin:0 0 6px 0;'>").append(processInlineMarkdown(line)).append("</p>");
            }
        }

        // Đóng các thẻ không được tiết lộ
        closeLists(html, inList, inOrderedList);
        if (inTable) {
            if (html.toString().contains("<tbody>")) {
                html.append("</tbody>");
            }
            html.append("</table>");
        }
        if (inCodeBlock) {
            html.append("</code></pre>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Đóng nhãn danh sách
     */
    private void closeLists(StringBuilder html, boolean inList, boolean inOrderedList) {
        if (inList) {
            html.append("</ul>");
        }
        if (inOrderedList) {
            html.append("</ol>");
        }
    }

    /**
     * Xử lý nội tuyến Markdown Syntax (GFM Phiên bản nâng cao)
     */
    private String processInlineMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Hãy trốn thoát trước tiên HTML，Ngăn chặn đầu vào của người dùng HTML Thẻ được thực thi
        text = escapeHtml(text);

        // Sau đó giải quyết nó Markdown syntax (safe to insert at this point HTML Nhãn)

        // In nghiêng đậm ***text*** (Phải được xử lý trước chữ in đậm và in nghiêng)
        text = text.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        text = text.replaceAll("___(.+?)___", "<strong><em>$1</em></strong>");

        // In đậm **text** hoặc __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // in nghiêng *text* hoặc _text_ (Phải được xử lý sau khi in đậm)
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        text = text.replaceAll("(?<!_)_(.+?)_(?!_)", "<em>$1</em>");

        // Gạch ngang ~~text~~ - sử dụng strike Nhãn,Swing HTMLEditorKit Hỗ trợ bản địa
        text = text.replaceAll("~~(.+?)~~", "<strike>$1</strike>");

        // Làm nổi bật ==text== (Được hỗ trợ bởi một số biên tập viên)
        text = text.replaceAll("==(.+?)==", "<mark>$1</mark>");

        // Mã nội tuyến `code` (Phải tránh các ký tự đặc biệt trong mã sau khi xử lý khác)
        text = text.replaceAll("`(.+?)`", "<code style='" + getInlineCodeStyle() + "'>$1</code>");

        // hình ảnh ![alt](url)
        text = text.replaceAll("!\\[([^\\]]*)\\]\\(([^)]+)\\)", "<img src=\"$2\" alt=\"$1\" style=\"max-width: 100%;\" />");

        // Liên kết [text](url)
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");

        // Liên kết tự động <url>
        text = text.replaceAll("&lt;(https?://[^&]+)&gt;", "<a href=\"$1\">$1</a>");

        return text;
    }

    /**
     * HTML bỏ trốn
     */
    private String escapeHtml(String text) {
        return HttpHtmlRenderer.escapeHtml(text);
    }

    /**
     * Lấy văn bản soạn thảo
     */
    public String getText() {
        return editorArea.getText();
    }


    /**
     * Đặt văn bản soạn thảo
     */
    public void setText(String text) {
        editorArea.setText(text == null ? "" : text);
        editorArea.setCaretPosition(0);
        // setText Chủ yếu được sử dụng cho các yêu cầu chuyển giao/Việc tải mô tả hiện có khi nhóm không phải là một bước không thể hoàn tác đối với người dùng.
        // Cũng dọn dẹp RSTA Tích hợp sẵn undo Và bảng này tự quản lý undo，tránh Ctrl+Z Xóa nội dung đã tải.
        editorArea.discardAllEdits();
        undoManager.discardAllEdits();
        updateUndoRedoButtons();
        updatePreview();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        editorArea.setEditable(editable);
        if (toolbarPanel != null) {
            setComponentTreeEnabled(toolbarPanel, editable);
        }
    }

    private void setComponentTreeEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setComponentTreeEnabled(child, enabled);
            }
        }
    }

    /**
     * Thêm trình nghe thay đổi tài liệu
     */
    public void addDocumentListener(DocumentListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Thông báo cho tất cả người nghe
     */
    private void notifyChangeListeners(DocumentEvent e) {
        for (DocumentListener listener : changeListeners) {
            if (e.getType() == DocumentEvent.EventType.INSERT) {
                listener.insertUpdate(e);
            } else if (e.getType() == DocumentEvent.EventType.REMOVE) {
                listener.removeUpdate(e);
            } else {
                listener.changedUpdate(e);
            }
        }
    }

}
