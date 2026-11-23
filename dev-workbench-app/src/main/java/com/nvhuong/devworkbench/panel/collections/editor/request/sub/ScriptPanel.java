package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.common.component.FallbackAwareRSyntaxTextArea;
import com.nvhuong.devworkbench.common.component.SearchableTextArea;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.HelpButton;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.button.SnippetButton;
import com.nvhuong.devworkbench.common.component.dialog.SnippetDialog;
import com.nvhuong.devworkbench.common.component.editor.DevWorkbenchJavaScriptTokenMaker;
import com.nvhuong.devworkbench.common.component.editor.ScriptSnippetManager;
import com.nvhuong.devworkbench.common.component.tab.IndicatorTabComponent;
import com.nvhuong.devworkbench.snippet.Snippet;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;


@Slf4j
public class ScriptPanel extends JPanel {
    private static final String DEV_WORKBENCH_JS_SYNTAX = "text/devworkbench-javascript";

    private final RSyntaxTextArea prescriptArea;
    private final RSyntaxTextArea postscriptArea;
    private final JTabbedPane tabbedPane;
    private final SnippetButton snippetBtn;
    private final IndicatorTabComponent preScriptTab;
    private final IndicatorTabComponent postScriptTab;
    private final transient AutoCompletion prescriptAc;
    private final transient AutoCompletion postscriptAc;

    static {
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(DEV_WORKBENCH_JS_SYNTAX, DevWorkbenchJavaScriptTokenMaker.class.getName());
        // Đăng ký cú pháp tùy chỉnh FoldParser，Kích hoạt tính năng gấp mã
        FoldParserManager.get().addFoldParserMapping(DEV_WORKBENCH_JS_SYNTAX, new CurlyFoldParser());
    }

    public ScriptPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(new EmptyBorder(5, 0, 5, 5));

        // Khởi tạo và cấu hình PreScript Biên tập viên
        prescriptArea = new FallbackAwareRSyntaxTextArea(6, 40);
        prescriptAc = configureEditor(prescriptArea);
        SearchableTextArea prescriptSearchableArea = new SearchableTextArea(prescriptArea);

        // Khởi tạo và cấu hình PostScript Biên tập viên
        postscriptArea = new FallbackAwareRSyntaxTextArea(6, 40);
        postscriptAc = configureEditor(postscriptArea);
        SearchableTextArea postscriptSearchableArea = new SearchableTextArea(postscriptArea);

        // Tạo một bảng tab hướng dọc
        tabbedPane = new JTabbedPane(SwingConstants.LEFT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(tabbedPane);
        String prescriptTabTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_PRESCRIPT_TAB_TITLE);
        String postscriptTabTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_TAB_TITLE);

        // Tạo bằng chỉ báo Tab thành phần
        preScriptTab = new IndicatorTabComponent(prescriptTabTitle);
        postScriptTab = new IndicatorTabComponent(postscriptTabTitle);

        // Pre-script Nhãn với chỉ số
        tabbedPane.addTab(prescriptTabTitle, prescriptSearchableArea);
        tabbedPane.setTabComponentAt(0, preScriptTab);

        // Post-script Nhãn với chỉ số
        tabbedPane.addTab(postscriptTabTitle, postscriptSearchableArea);
        tabbedPane.setTabComponentAt(1, postScriptTab);

        // Thêm trình nghe tài liệu để cập nhật chỉ báo
        addIndicatorListeners();

        add(tabbedPane, BorderLayout.CENTER);

        // Thêm nút trợ giúp ở góc dưới bên phải và Snippets nút
        snippetBtn = new SnippetButton();

        HelpButton helpBtn = new HelpButton();
        helpBtn.addActionListener(e -> showHelpDialog());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        ToolWindowSurfaceStyle.applyCard(btnPanel);
        btnPanel.add(helpBtn);
        btnPanel.add(snippetBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Trình nghe chuyển đổi tab - Chỉ hiển thị trên tab Tập lệnh Snippets nút
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            snippetBtn.setVisible(selectedIndex == 0 || selectedIndex == 1);
        });

        // Snippets Sự kiện nhấn nút
        snippetBtn.addActionListener(e -> openSnippetDialog());
    }

    /**
     * Hiển thị hộp thoại trợ giúp
     */
    private void showHelpDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_TITLE), true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel helpPanel = createHelpPanel();
        dialog.add(helpPanel);

        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Tạo bảng trợ giúp
     */
    private JPanel createHelpPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Bảng nội dung chính
        JPanel contentPanel = new JPanel();
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // tiêu đề
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Giới thiệu
        JTextArea introArea = createHelpTextArea(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_INTRO));
        introArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(introArea);
        contentPanel.add(Box.createVerticalStrut(15));

        // Các chức năng chính
        JTextArea featuresArea = createHelpTextArea(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_FEATURES));
        featuresArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresArea);
        contentPanel.add(Box.createVerticalStrut(15));

        // phím tắt
        JTextArea shortcutsArea = createHelpTextArea(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_SHORTCUTS));
        shortcutsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(shortcutsArea);
        contentPanel.add(Box.createVerticalStrut(15));

        // Ví dụ phổ biến
        JTextArea examplesArea = createHelpTextArea(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_EXAMPLES));
        examplesArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        examplesArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(examplesArea);

        // Đặt bảng nội dung vào bảng cuộn
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tạo một vùng văn bản trợ giúp
     */
    private JTextArea createHelpTextArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(area);
        area.setBorder(null);
        return area;
    }


    /**
     * Định cấu hình cài đặt chung của trình chỉnh sửa và quay lại cài đặt đã cài đặt AutoCompletion Ví dụ
     */
    private AutoCompletion configureEditor(RSyntaxTextArea area) {
        // Đặt đánh dấu cú pháp
        area.setSyntaxEditingStyle(DEV_WORKBENCH_JS_SYNTAX);

        // Chức năng chỉnh sửa mã
        area.setCodeFoldingEnabled(true);           // Gấp mã
        area.setAutoIndentEnabled(true);            // Tự động thụt lề
        area.setBracketMatchingEnabled(true);       // Khớp khung
        area.setMarkOccurrences(false);             // Trình chỉnh sửa tập lệnh không đánh dấu các mã định danh giống hệt nhau

        // Cài đặt hiển thị
        area.setAntiAliasingEnabled(true);          // Khử răng cưa giúp văn bản rõ ràng hơn
        area.setPaintTabLines(true);                // Hiển thị hướng dẫn thụt lề
        area.setTabSize(4);                         // Tab = 4 không gian

        // Tải thống nhất các chủ đề, phông chữ soạn thảo và bản vẽ dự phòng cho các ký tự bị thiếu
        EditorThemeUtil.loadTheme(area);

        return addAutoCompletion(area);
    }

    /**
     * Mở hộp thoại đoạn mã
     */
    private void openSnippetDialog() {
        int tab = tabbedPane.getSelectedIndex();
        if (tab != 0 && tab != 1) {
            return;  // Không có trong tab tập lệnh, không được thực thi
        }

        SnippetDialog dialog = new SnippetDialog();
        dialog.setVisible(true);
        Snippet selected = dialog.getSelectedSnippet();

        if (selected != null) {
            RSyntaxTextArea targetArea = (tab == 0) ? prescriptArea : postscriptArea;

            // Smart insertion: If there is selected text, replace it; nếu không thì chèn vào vị trí con trỏ
            String selectedText = targetArea.getSelectedText();
            String codeToInsert = selected.code;

            // Nếu con trỏ không ở đầu dòng và có nội dung trước nó, hãy thêm dòng mới
            int caretPosition = targetArea.getCaretPosition();
            try {
                int lineStart = targetArea.getLineStartOffsetOfCurrentLine();
                if (caretPosition > lineStart && selectedText == null) {
                    String lineText = targetArea.getText(lineStart, caretPosition - lineStart);
                    if (!lineText.trim().isEmpty()) {
                        codeToInsert = "\n" + codeToInsert;
                    }
                }
            } catch (Exception ex) {
                log.error("Error calculating insert position", ex);
            }

            targetArea.replaceSelection(codeToInsert);
            targetArea.requestFocusInWindow();
        }
    }

    /**
     * Nội dung soạn thảo được thiết lập theo chương trình, tạm thời bị vô hiệu hóa AutoCompletion Tự động kích hoạt.
     * tránh setText() Cửa sổ bật lên hoàn thành kích hoạt (AutoCompletion Không thể phân biệt giữa đầu vào của người dùng và phân công chương trình).
     */
    private void setEditorText(RSyntaxTextArea area, AutoCompletion ac, String text) {
        if (ac != null) ac.setAutoActivationEnabled(false);
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
        // Khi chuyển đổi yêu cầu setText Đó là tải dữ liệu chứ không phải chỉnh sửa của người dùng; nếu không thì đây là lần đầu tiên Ctrl/Cmd+Z Sẽ hoàn tác hành động tải và xóa tập lệnh.
        area.discardAllEdits();
        if (ac != null) ac.setAutoActivationEnabled(true);
    }

    public void setPrescript(String text) {
        setEditorText(prescriptArea, prescriptAc, text);
        updateIndicator(preScriptTab, text);
        if (text != null && !text.trim().isEmpty()) {
            tabbedPane.setSelectedIndex(0);
        }
    }

    public void setPostscript(String text) {
        setEditorText(postscriptArea, postscriptAc, text);
        updateIndicator(postScriptTab, text);
        if (text != null && !text.trim().isEmpty() &&
                (prescriptArea.getText() == null || prescriptArea.getText().trim().isEmpty())) {
            tabbedPane.setSelectedIndex(1);
        }
    }

    public String getPrescript() {
        return prescriptArea.getText();
    }

    public String getPostscript() {
        return postscriptArea.getText();
    }

    public void setEditable(boolean editable) {
        prescriptArea.setEditable(editable);
        postscriptArea.setEditable(editable);
        snippetBtn.setVisible(editable);
        snippetBtn.setEnabled(editable);
    }

    /**
     * Thêm giám sát và gọi lại bẩn khi nội dung thay đổi
     */
    public void addDirtyListeners(Runnable dirtyCallback) {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void removeUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void changedUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }
        };
        prescriptArea.getDocument().addDocumentListener(listener);
        postscriptArea.getDocument().addDocumentListener(listener);
    }


    /**
     * Thêm trình nghe tài liệu để cập nhật trạng thái chỉ báo
     */
    private void addIndicatorListeners() {
        // Pre-script người nghe
        prescriptArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateIndicator(preScriptTab, prescriptArea.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateIndicator(preScriptTab, prescriptArea.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateIndicator(preScriptTab, prescriptArea.getText());
            }
        });

        // Post-script người nghe
        postscriptArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateIndicator(postScriptTab, postscriptArea.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateIndicator(postScriptTab, postscriptArea.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateIndicator(postScriptTab, postscriptArea.getText());
            }
        });
    }

    /**
     * Cập nhật trạng thái chỉ báo dựa trên nội dung văn bản
     *
     * @param tabComponent Tabthành phần
     * @param text         Nội dung văn bản
     */
    private void updateIndicator(IndicatorTabComponent tabComponent, String text) {
        boolean hasContent = text != null && !text.trim().isEmpty();
        tabComponent.setShowIndicator(hasContent);
    }

    /**
     * vì RSyntaxTextArea Cài đặt tự động hoàn tất và quay lại AutoCompletion Ví dụ
     */
    private AutoCompletion addAutoCompletion(RSyntaxTextArea area) {
        var provider = ScriptSnippetManager.createCompletionProvider();

        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(200);
        ac.setAutoCompleteSingleChoices(false);
        ac.setShowDescWindow(true);
        ac.setParameterAssistanceEnabled(false);
        ac.install(area);
        // Vietnamese/IME input commits words at once; AutoCompletion alone only reacts to single characters.
        com.nvhuong.devworkbench.common.component.editor.InputMethodAutoActivation.install(area, ac);
        return ac;
    }
}
