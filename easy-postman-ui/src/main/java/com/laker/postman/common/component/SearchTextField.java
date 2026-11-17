package com.laker.postman.common.component;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Thành phần đầu vào tìm kiếm chung với biểu tượng tìm kiếm, trình giữ chỗ, nút xóa, phân biệt chữ hoa chữ thường và các tùy chọn khớp toàn bộ từ.
 * sử dụng FlatLaf Phong cách biểu tượng chính thức .
 */
public class SearchTextField extends FlatTextField {
    static final int DEFAULT_WIDTH = 220;
    static final int DEFAULT_HEIGHT = 30;
    static final int MAX_WIDTH = 300;
    static final int MIN_WIDTH = 50;
    static final int OPTION_BUTTON_SIZE = 22;

    private static final String USER_ACTIVATED_FOCUS_INSTALLED =
            SearchTextField.class.getName() + ".userActivatedFocusInstalled";
    private static final String IN_TEXT_FIELD_STYLE_CLASS = "inTextField";

    private final UndoManager undoManager = new UndoManager();
    /**
     * -- GETTER --
     * Nhận trạng thái phân biệt chữ hoa chữ thường
     */
    @Getter
    private boolean caseSensitive = false;
    /**
     * -- GETTER --
     * Nhận trạng thái khớp toàn bộ từ
     */
    @Getter
    private boolean wholeWord = false;

    public SearchTextField() {
        super();
        setLeadingIcon(IconUtil.createThemed("icons/search.svg", 16, 16));
        setPlaceholderText(CommonI18n.get(CommonMessageKeys.BUTTON_SEARCH));
        setShowClearButton(true);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMaximumSize(new Dimension(MAX_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, DEFAULT_HEIGHT));

        // Tạo một thanh công cụ nút tùy chọn
        initOptionsToolbar();

        // Hoàn tác/Làm lại chức năng
        Document doc = getDocument();
        doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        // Hoàn tác phím tắt (Cmd+Z / Ctrl+Z)
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo");
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    try {
                        undoManager.undo();
                    } catch (CannotUndoException ex) {
                        // ignore
                    }
                }
            }
        });
        // Làm lại các phím tắt (Ctrl+Y / Cmd+Shift+Z)
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo");
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    try {
                        undoManager.redo();
                    } catch (CannotRedoException ex) {
                        // ignore
                    }
                }
            }
        });
    }

    /**
     * Khởi tạo thanh công cụ nút tùy chọn (phân biệt chữ hoa chữ thường, khớp toàn bộ từ)
     * sử dụng JToolBar và SVG biểu tượng, với FlatLaf chính thức Demo nhất quán
     */
    private void initOptionsToolbar() {
        JToggleButton caseSensitiveButton = createOptionButton(
                "icons/matchCase.svg",
                "icons/matchCaseHovered.svg",
                "icons/matchCaseSelected.svg",
                UiI18n.get(UiMessageKeys.SEARCH_MATCH_CASE)
        );
        caseSensitiveButton.addActionListener(e -> {
            caseSensitive = caseSensitiveButton.isSelected();
            firePropertyChange("caseSensitive", !caseSensitive, caseSensitive);
        });

        JToggleButton wholeWordButton = createOptionButton(
                "icons/words.svg",
                "icons/wordsHovered.svg",
                "icons/wordsSelected.svg",
                UiI18n.get(UiMessageKeys.SEARCH_MATCH_WHOLE_WORD)
        );
        wholeWordButton.addActionListener(e -> {
            wholeWord = wholeWordButton.isSelected();
            firePropertyChange("wholeWord", !wholeWord, wholeWord);
        });

        // sử dụng JToolBar Là một container, đây là FlatLaf Phương pháp chính thức được đề xuất
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 2, 0, 4));
        toolbar.putClientProperty(FlatClientProperties.STYLE_CLASS, IN_TEXT_FIELD_STYLE_CLASS);
        toolbar.add(caseSensitiveButton);
        toolbar.add(wholeWordButton);

        // Đặt thành  trailing component
        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolbar);
    }

    private JToggleButton createOptionButton(String iconPath, String rolloverIconPath, String selectedIconPath,
                                             String tooltipText) {
        JToggleButton button = new JToggleButton(new FlatSVGIcon(iconPath, 16, 16));
        button.setRolloverIcon(new FlatSVGIcon(rolloverIconPath, 16, 16));
        button.setSelectedIcon(new FlatSVGIcon(selectedIconPath, 16, 16));
        button.setToolTipText(tooltipText);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.putClientProperty(FlatClientProperties.STYLE_CLASS, IN_TEXT_FIELD_STYLE_CLASS);
        return button;
    }

    /**
     * Đặt trạng thái tìm kiếm không có kết quả: ô nhập chuyển sang màu đỏ khi không có kết quả và trở về bình thường khi có kết quả (giống như IDEA Hành vi).
     *
     * @param noResult true=Không có kết quả (viền đỏ).false=Có kết quả hoặc rõ ràng (trở lại bình thường)
     */
    public void setNoResult(boolean noResult) {
        putClientProperty(FlatClientProperties.OUTLINE, noResult ? FlatClientProperties.OUTLINE_ERROR : null);
    }

    /**
     * Prevents the search field from taking initial window focus until the user explicitly clicks it.
     */
    public void installUserActivatedFocus() {
        if (Boolean.TRUE.equals(getClientProperty(USER_ACTIVATED_FOCUS_INSTALLED))) {
            return;
        }
        putClientProperty(USER_ACTIVATED_FOCUS_INSTALLED, true);
        setFocusable(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setFocusable(true);
                SwingUtilities.invokeLater(SearchTextField.this::requestFocusInWindow);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setFocusable(false);
                }
            }
        });
    }

}
