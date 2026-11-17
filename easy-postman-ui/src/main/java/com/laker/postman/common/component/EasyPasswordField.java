package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatPasswordField;
import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * EasyPostman Thành phần hộp nhập mật khẩu
 * <p>
 * Đặc trưng:
 * 1. tích hợp FlatLaf trình diễn/Nút ẩn mật khẩu (biểu tượng con mắt)
 * 2. Hỗ trợ rút tiền/Làm lại thao tác
 * - Windows: Ctrl+Z Rút tiền,Ctrl+Y làm lại
 * - macOS: Cmd+Z Rút tiền,Cmd+Shift+Z làm lại
 * 3. Văn bản giữ chỗ được hỗ trợ
 * 4. Kiểu phông chữ thống nhất
 * 5. Hỗ trợ các kiểu tùy chỉnh (thông qua  FlatLaf của STYLE thuộc tính)
 *
 */
@Slf4j
public class EasyPasswordField extends FlatPasswordField {

    private static final String SHOW_REVEAL_BUTTON_STYLE = "showRevealButton: true";

    private final UndoManager undoManager = new UndoManager();
    private boolean showRevealButton = true;

    /**
     * Tạo hộp nhập mật khẩu mặc định
     */
    public EasyPasswordField() {
        this(20);
    }

    /**
     * Tạo hộp nhập mật khẩu với số cột được chỉ định
     *
     * @param columns Hiển thị số lượng cột
     */
    public EasyPasswordField(int columns) {
        super();
        setColumns(columns);
        init();
    }

    /**
     * Tạo hộp nhập mật khẩu với văn bản và số cột ban đầu
     *
     * @param text    Văn bản ban đầu
     * @param columns Hiển thị số lượng cột
     */
    public EasyPasswordField(String text, int columns) {
        super();
        setText(text);
        setColumns(columns);
        init();
    }

    /**
     * Tạo hộp nhập mật khẩu có phần giữ chỗ
     *
     * @param columns         Hiển thị số lượng cột
     * @param placeholderText Văn bản giữ chỗ
     */
    public EasyPasswordField(int columns, String placeholderText) {
        super();
        setColumns(columns);
        setPlaceholderText(placeholderText);
        init();
    }

    /**
     * Tạo hộp nhập mật khẩu với văn bản ban đầu, số cột và phần giữ chỗ
     *
     * @param text            Văn bản ban đầu
     * @param columns         Hiển thị số lượng cột
     * @param placeholderText Văn bản giữ chỗ
     */
    public EasyPasswordField(String text, int columns, String placeholderText) {
        super();
        setText(text);
        setColumns(columns);
        setPlaceholderText(placeholderText);
        init();
    }

    /**
     * Khởi tạo thành phần
     */
    private void init() {
        // Đặt phông chữ mặc định
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // Kích hoạt hiển thị mật khẩu/Nút ẩn
        updateRevealButtonStyle();

        // Đã rút lại quá trình khởi tạo/Làm lại chức năng
        initUndoRedo();
    }

    /**
     * Cập nhật hiển thị/Ẩn kiểu nút
     */
    private void updateRevealButtonStyle() {
        if (showRevealButton) {
            putClientProperty(FlatClientProperties.STYLE, SHOW_REVEAL_BUTTON_STYLE);
        } else {
            putClientProperty(FlatClientProperties.STYLE, null);
        }
    }

    /**
     * Đặt xem có hiển thị hiển thị mật khẩu hay không/Nút ẩn
     *
     * @param show true Nút hiển thị,false Nút ẩn
     */
    public void setShowRevealButton(boolean show) {
        if (this.showRevealButton != show) {
            this.showRevealButton = show;
            updateRevealButtonStyle();
        }
    }

    /**
     * Nhận xem có hiển thị hiển thị mật khẩu hay không/Nút ẩn
     *
     * @return true Nút hiển thị,false Nút ẩn
     */
    public boolean isShowRevealButton() {
        return showRevealButton;
    }

    /**
     * Đã rút lại quá trình khởi tạo/Làm lại chức năng
     */
    private void initUndoRedo() {
        getDocument().addUndoableEditListener(undoManager);

        // Undo - Rút tiền
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException ex) {
                    log.debug("Cannot undo", ex);
                }
            }
        });

        // Redo - làm lại
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotRedoException ex) {
                    log.debug("Cannot redo", ex);
                }
            }
        });
    }

    /**
     * Lấy văn bản mật khẩu (phương pháp thuận tiện)
     *
     * @return Chuỗi mật khẩu
     */
    public String getPasswordText() {
        return new String(getPassword());
    }

    /**
     * Đặt văn bản mật khẩu (phương pháp thuận tiện)
     *
     * @param password Chuỗi mật khẩu
     */
    public void setPasswordText(String password) {
        setText(password);
    }

    /**
     * Xóa mật khẩu
     */
    public void clearPassword() {
        setText("");
    }

    /**
     * Thêm trình nghe tài liệu (phương pháp tiện lợi)
     *
     * @param listener Người nghe tài liệu
     */
    public void addDocumentListener(DocumentListener listener) {
        getDocument().addDocumentListener(listener);
    }

    /**
     * Xóa trình nghe tài liệu (phương pháp tiện lợi)
     *
     * @param listener Người nghe tài liệu
     */
    public void removeDocumentListener(DocumentListener listener) {
        getDocument().removeDocumentListener(listener);
    }

    /**
     * Đặt lại trình quản lý thu hồi
     * Phương thức này được gọi khi lịch sử thu hồi cần được xóa
     */
    public void resetUndoManager() {
        undoManager.discardAllEdits();
    }

    /**
     * Đặt kiểu tùy chỉnh
     * LƯU Ý: Nếu bạn muốn giữ màn hình/Để ẩn nút, hãy đưa nó vào chuỗi kiểu "showRevealButton: true"
     *
     * @param style FlatLaf Chuỗi phong cách
     */
    public void setCustomStyle(String style) {
        if (showRevealButton && (style == null || !style.contains("showRevealButton"))) {
            // Nếu nút hiển thị được bật nhưng không đúng kiểu, nó sẽ được thêm vào
            String newStyle = style == null ? SHOW_REVEAL_BUTTON_STYLE :
                    style + "; " + SHOW_REVEAL_BUTTON_STYLE;
            putClientProperty(FlatClientProperties.STYLE, newStyle);
        } else {
            putClientProperty(FlatClientProperties.STYLE, style);
        }
    }
}

