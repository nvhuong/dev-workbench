package com.nvhuong.devworkbench.common.component.dialog;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.FallbackAwareRSyntaxTextArea;
import com.nvhuong.devworkbench.common.component.SearchTextField;
import com.nvhuong.devworkbench.common.component.SyntaxEditorScrollPane;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.setting.SettingsInputStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.plugin.host.PluginAccess;
import com.nvhuong.devworkbench.snippet.Snippet;
import com.nvhuong.devworkbench.snippet.SnippetType;
import com.nvhuong.devworkbench.util.EditorThemeUtil;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cửa sổ bật lên đoạn mã, dựa trên ListModel
 */
public class SnippetDialog extends JDialog {
    private final JList<Snippet> snippetList;
    private final DefaultListModel<Snippet> listModel;
    private final SearchTextField searchField;
    private final RSyntaxTextArea previewArea;
    private final JLabel descriptionLabel;
    private final JComboBox<String> categoryCombo;
    private final Map<String, List<Snippet>> snippetCategories = new LinkedHashMap<>();
    @Getter
    private Snippet selectedSnippet;
    List<Snippet> snippets;

    private static List<Snippet> getI18nSnippets() {
        return Stream.concat(
                        Arrays.stream(SnippetType.values()).map(Snippet::new),
                        PluginAccess.getSnippetDefinitions().stream().map(Snippet::new)
                )
                .toList();
    }


    public SnippetDialog() {
        super(UiSingletonFactory.getInstance(MainFrame.class), I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_TITLE), true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        Frame owner = UiSingletonFactory.getInstance(MainFrame.class);
        JPanel contentPanel = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(new EmptyBorder(12, 12, 8, 12));

        // Khởi tạo dữ liệu phân đoạn
        snippets = getI18nSnippets();
        // Khởi tạo phân loại
        initCategories();

        // Tạo bảng điều khiển phía bắc: hộp tìm kiếm và bộ chọn danh mục
        JPanel northPanel = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(northPanel);
        northPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        // Hộp tìm kiếm có biểu tượng và mẹo
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchField = new SearchTextField();
        searchField.installUserActivatedFocus();

        // Đã thêm biểu tượng tìm kiếm
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Bộ chọn danh mục thả xuống
        String[] categories = snippetCategories.keySet().toArray(new String[0]);
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setPreferredSize(new Dimension(160, 32));
        categoryCombo.setFocusable(false);
        SettingsInputStyle.apply(categoryCombo);

        northPanel.add(searchPanel, BorderLayout.CENTER);
        northPanel.add(categoryCombo, BorderLayout.EAST);

        // Tạo bảng ở giữa: danh sách đoạn trích
        listModel = new DefaultListModel<>();
        loadSnippets(snippets);

        snippetList = new JList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setVisibleRowCount(8);

        // Tùy chỉnh trình kết xuất để làm cho các mục trong danh sách đẹp hơn
        snippetList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Snippet snippet) {
                    label.setText(snippet.title);
                    label.setBorder(new EmptyBorder(5, 10, 5, 10));
                    // Đặt biểu tượng dựa trên loại
                    // Lưu ý:http.svg và cookie.svg Giữ nó đầy màu sắc và sử dụng nó create()
                    // Các biểu tượng khác được sử dụng createThemed() Để hỗ trợ việc thích ứng chủ đề
                    switch (snippet.category) {
                        // Phân loại cơ bản
                        case PRE_SCRIPT ->
                                label.setIcon(IconUtil.createThemed("icons/arrow-up.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case ASSERT ->
                                label.setIcon(IconUtil.createThemed("icons/check.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case EXTRACT ->
                                label.setIcon(IconUtil.createThemed("icons/arrow-down.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case LOCAL_VAR ->
                                label.setIcon(IconUtil.createThemed("icons/code.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case ENV_VAR ->
                                label.setIcon(IconUtil.createThemed("icons/environments.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // Mã hóa và mã hóa
                        case ENCODE ->
                                label.setIcon(IconUtil.createThemed("icons/format.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case ENCRYPT ->
                                label.setIcon(IconUtil.createThemed("icons/security.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // Kiểu dữ liệu
                        case ARRAY ->
                                label.setIcon(IconUtil.createThemed("icons/functional.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case JSON ->
                                label.setIcon(IconUtil.create("icons/http.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)); // Biểu tượng màu sắc
                        case STRING ->
                                label.setIcon(IconUtil.createThemed("icons/code.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case DATE ->
                                label.setIcon(IconUtil.createThemed("icons/time.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case REGEX ->
                                label.setIcon(IconUtil.createThemed("icons/search.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // Những điều cơ bản khác
                        case LOG ->
                                label.setIcon(IconUtil.createThemed("icons/console.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case CONTROL ->
                                label.setIcon(IconUtil.createThemed("icons/functional.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case TOKEN ->
                                label.setIcon(IconUtil.createThemed("icons/security.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case COOKIES ->
                                label.setIcon(IconUtil.createThemed("icons/cookie.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // Phân loại thư viện tích hợp
                        case CRYPTOJS ->
                                label.setIcon(IconUtil.createThemed("icons/security.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case LODASH ->
                                label.setIcon(IconUtil.createThemed("icons/functional.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case MOMENT ->
                                label.setIcon(IconUtil.createThemed("icons/time.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // Phân loại cảnh nâng cao
                        case AUTHENTICATION ->
                                label.setIcon(IconUtil.createThemed("icons/security.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case PERFORMANCE ->
                                label.setIcon(IconUtil.createThemed("icons/performance.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case VALIDATION ->
                                label.setIcon(IconUtil.createThemed("icons/check.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case DATA_PROCESSING ->
                                label.setIcon(IconUtil.createThemed("icons/functional.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case REQUEST_MODIFICATION ->
                                label.setIcon(IconUtil.createThemed("icons/edit.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        case EXAMPLES ->
                                label.setIcon(IconUtil.createThemed("icons/collections.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));

                        // khác
                        case OTHER ->
                                label.setIcon(IconUtil.createThemed("icons/code.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                        default ->
                                label.setIcon(IconUtil.createThemed("icons/code.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                    }
                }
                return label;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(snippetList);
        ToolWindowSurfaceStyle.applyDialogListScrollPane(listScrollPane, snippetList);

        // khu vực xem trước
        JPanel previewPanel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogSurface(previewPanel);
        JLabel previewTitleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_PREVIEW_TITLE));
        previewTitleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        previewTitleLabel.setForeground(ModernColors.getTextPrimary());
        previewTitleLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
        previewPanel.add(previewTitleLabel, BorderLayout.NORTH);

        previewArea = createPreviewEditor();
        SyntaxEditorScrollPane previewScrollPane = new SyntaxEditorScrollPane(previewArea);
        previewScrollPane.setLineNumbersEnabled(true);
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);

        // Bảng nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton closeBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CLOSE), false);
        JButton insertBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_INSERT), true);

        buttonPanel.add(closeBtn);
        buttonPanel.add(insertBtn);

        // Thẻ mô tả
        descriptionLabel = new JLabel(" ");
        descriptionLabel.setForeground(ModernColors.getTextSecondary());

        JPanel footerPanel = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(footerPanel);
        footerPanel.add(descriptionLabel, BorderLayout.CENTER);
        footerPanel.add(buttonPanel, BorderLayout.EAST);

        // Cửa sổ bật lên đoạn mã chỉ giữ lại một thẻ tròn bên ngoài và thẻ bên trong được sử dụng giữa danh sách và bản xem trước split sự phân đoạn.
        JSplitPane splitPane = AppToolWindowChrome.createHorizontalInnerSplitPane(listScrollPane, previewPanel, 260);
        splitPane.setResizeWeight(0.28); // Đặt tỷ lệ trái và phải

        contentPanel.add(northPanel, BorderLayout.NORTH);
        contentPanel.add(AppToolWindowChrome.wrapToolWindow(splitPane, new Insets(0, 0, 0, 0)), BorderLayout.CENTER);
        contentPanel.add(footerPanel, BorderLayout.SOUTH);
        setContentPane(contentPanel);
        getRootPane().setDefaultButton(insertBtn);

        // Liên kết người nghe sự kiện

        // Sự kiện hộp tìm kiếm
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void removeUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void changedUpdate(DocumentEvent e) {
                filterSnippets();
            }
        });

        // Sự kiện chọn danh mục
        categoryCombo.addActionListener(e -> {
            String category = (String) categoryCombo.getSelectedItem();
            if (category != null) {
                if (category.equals(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL))) {
                    loadSnippets(snippets);
                } else {
                    loadSnippets(snippetCategories.get(category));
                }

                // Nếu có từ khóa tìm kiếm, việc lọc cũng được yêu cầu
                if (!searchField.getText().trim().isEmpty()) {
                    filterSnippets();
                } else {
                    // Nếu danh sách không trống, hãy tự động chọn danh sách đầu tiên và hiển thị bản xem trước
                    if (listModel.getSize() > 0) {
                        snippetList.setSelectedIndex(0);
                        selectedSnippet = snippetList.getSelectedValue();
                        if (selectedSnippet != null) {
                            previewArea.setText(selectedSnippet.code);
                            previewArea.setCaretPosition(0);
                            descriptionLabel.setText(selectedSnippet.desc);
                        }
                    } else {
                        // Nếu danh sách trống, hãy xóa vùng xem trước
                        previewArea.setText("");
                        descriptionLabel.setText("");
                    }
                }
            }
        });

        // Liệt kê các sự kiện chọn lọc
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedSnippet = snippetList.getSelectedValue();
                if (selectedSnippet != null) {
                    previewArea.setText(selectedSnippet.code);
                    previewArea.setCaretPosition(0);
                    descriptionLabel.setText(selectedSnippet.desc);
                }
            }
        });

        // Liệt kê sự kiện nhấp đúp chuột
        snippetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Sự kiện nhấp đúp
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                }
            }
        });

        // Sự kiện bàn phím
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                    // Trong khi nhấn các phím mũi tên, hãy chuyển tiêu điểm sang danh sách và chọn mục đầu tiên
                    snippetList.requestFocusInWindow();
                    if (listModel.getSize() > 0 && snippetList.getSelectedIndex() == -1) {
                        snippetList.setSelectedIndex(0);
                    }
                    e.consume();  // Tiêu thụ các sự kiện để ngăn các thành phần khác xử lý chúng
                }
            }
        });

        snippetList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Khi nhấn phím Enter, nếu một mục được chọn, hộp thoại sẽ được chọn và đóng lại
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // ESC để hủy và đóng hộp thoại
                    selectedSnippet = null;
                    dispose();
                }
            }
        });

        // Sự kiện nút
        insertBtn.addActionListener(e -> {
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_SELECT_SNIPPET_FIRST), I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_TIP), JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> {
            selectedSnippet = null;
            dispose();
        });

        // Trạng thái khởi tạo
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
            // Hiển thị bản xem trước của mục đầu tiên
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                previewArea.setText(selectedSnippet.code);
                previewArea.setCaretPosition(0);
                descriptionLabel.setText(selectedSnippet.desc);
            }
        }

        // Thêm trình nghe đóng cửa sổ để xử lý các lượt nhấp chuột X Vỏ nút
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                selectedSnippet = null;
            }
        });

        // Đặt thuộc tính hộp thoại
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(600, 400));
    }

    private RSyntaxTextArea createPreviewEditor() {
        RSyntaxTextArea editor = new FallbackAwareRSyntaxTextArea(8, 40);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        editor.setEditable(false);
        editor.setLineWrap(false);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setPaintTabLines(true);
        editor.setHighlightCurrentLine(false);
        editor.setTabSize(4);
        EditorThemeUtil.loadTheme(editor);
        return editor;
    }

    // Khởi tạo phân loại đoạn mã
    private void initCategories() {
        snippetCategories.put(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL), this.snippets);
        Map<String, List<Snippet>> categorized = this.snippets.stream()
                .collect(Collectors.groupingBy(snippet -> switch (snippet.category) {
                    // Phân loại cơ bản
                    case PRE_SCRIPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT);
                    case ASSERT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT);
                    case EXTRACT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT);
                    case LOCAL_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR);
                    case ENV_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR);

                    // Phân loại cảnh nâng cao
                    case AUTHENTICATION -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_AUTHENTICATION);
                    case PERFORMANCE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PERFORMANCE);
                    case VALIDATION -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_VALIDATION);
                    case DATA_PROCESSING -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATA_PROCESSING);
                    case REQUEST_MODIFICATION ->
                            I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REQUEST_MODIFICATION);

                    // Cookie hoạt động
                    case COOKIES -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_COOKIES);

                    // Mã hóa và mã hóa
                    case ENCRYPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT);
                    case ENCODE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE);

                    // Kiểu dữ liệu
                    case STRING -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING);
                    case ARRAY -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY);
                    case JSON -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON);
                    case DATE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE);
                    case REGEX -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REGEX);

                    // Phân loại thư viện tích hợp
                    case CRYPTOJS -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CRYPTOJS);
                    case LODASH -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LODASH);
                    case MOMENT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_MOMENT);

                    // Ví dụ đầy đủ
                    case EXAMPLES -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXAMPLES);

                    // khác
                    case LOG -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOG);
                    case CONTROL -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CONTROL);
                    case TOKEN -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_TOKEN);
                    case OTHER -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER);
                    default -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER);
                }));

        // Danh sách phân loại được sắp xếp theo tần suất sử dụng và tầm quan trọng
        String[] orderedCategories = {
                // Cơ bản thường dùng
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT),

                // Kịch bản nâng cao (mới, được đặt ở phía trước)
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_AUTHENTICATION),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PERFORMANCE),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_VALIDATION),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATA_PROCESSING),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REQUEST_MODIFICATION),

                // các biến vàCookie
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_COOKIES),

                // Kiểu dữ liệu
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE),

                // Thư viện tích hợp
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CRYPTOJS),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LODASH),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_MOMENT),

                // Mã hóa mã hóa
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE),

                // Ví dụ đầy đủ
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXAMPLES),

                // Hỗ trợ khác
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REGEX),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CONTROL),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOG),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_TOKEN),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER)
        };

        for (String category : orderedCategories) {
            List<Snippet> categorySnippets = categorized.get(category);
            if (categorySnippets != null && !categorySnippets.isEmpty()) {
                snippetCategories.put(category, categorySnippets);
            }
        }
    }

    // Tải các đoạn vào một danh sách
    private void loadSnippets(List<Snippet> snippetsToLoad) {
        listModel.clear();
        for (Snippet s : snippetsToLoad) {
            listModel.addElement(s);
        }
    }

    // Lọc đoạn trích dựa trên cụm từ tìm kiếm
    private void filterSnippets() {
        String query = searchField.getText().trim().toLowerCase();
        String currentCategory = (String) categoryCombo.getSelectedItem();
        List<Snippet> searchSource;
        if (currentCategory != null && !currentCategory.equals(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL))) {
            searchSource = snippetCategories.get(currentCategory);
        } else {
            searchSource = this.snippets;
        }

        // Khi hộp tìm kiếm trống, tất cả các đoạn của danh mục hiện tại sẽ được hiển thị
        if (query.isEmpty()) {
            loadSnippets(searchSource);
            return;
        }

        // Tìm kiếm trong danh mục hiện được chọn
        DefaultListModel<Snippet> filteredModel = new DefaultListModel<>();
        for (Snippet s : searchSource) {
            if (s.title.toLowerCase().contains(query) ||
                    (s.desc != null && s.desc.toLowerCase().contains(query)) ||
                    (s.code != null && s.code.toLowerCase().contains(query))) {
                filteredModel.addElement(s);
            }
        }

        // Cập nhật mô hình danh sách
        listModel.clear();
        for (int i = 0; i < filteredModel.getSize(); i++) {
            listModel.addElement(filteredModel.getElementAt(i));
        }

        // Nếu có kết quả thì chọn kết quả đầu tiên
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
            // Hiển thị bản xem trước
            selectedSnippet = snippetList.getSelectedValue();
            previewArea.setText(selectedSnippet.code);
            previewArea.setCaretPosition(0);
            descriptionLabel.setText(selectedSnippet.desc);
        } else {
            // Xóa bản xem trước khi không có kết quả
            previewArea.setText("");
            descriptionLabel.setText(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_NOT_FOUND));
        }
    }
}
