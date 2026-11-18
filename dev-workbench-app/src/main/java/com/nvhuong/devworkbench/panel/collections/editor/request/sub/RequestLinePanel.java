package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.PrimaryButton;
import com.nvhuong.devworkbench.common.component.button.SecondaryButton;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bảng điều khiển dòng yêu cầu hàng đầu - Thiết kế hiện đại
 * Bao gồm: lựa chọn phương pháp,URLNhập, gửi/Nút lưu
 * Khái niệm thiết kế: đơn giản, chuyên nghiệp và dễ sử dụng
 */
public class RequestLinePanel extends JPanel {
    // Hằng số kích thước
    private static final int ICON_SIZE = 14;
    private static final int COMPONENT_HEIGHT = 32;
    private static final int METHOD_COMBO_WIDTH = 95;
    private static final int SEND_BUTTON_MIN_WIDTH = 88;
    private static final int SAVE_BUTTON_MIN_WIDTH = 80;
    private static final int PANEL_PADDING = 5;

    // thành phần
    @Getter
    private final JComboBox<String> methodBox;
    @Getter
    private final JTextField urlField;
    @Getter
    private final JButton sendButton;
    @Getter
    private final JButton saveButton;
    private final RequestItemProtocolEnum protocol;

    public RequestLinePanel(ActionListener sendAction, RequestItemProtocolEnum protocol) {
        this.protocol = protocol;

        // Khởi tạo thành phần
        methodBox = createMethodComboBox();
        urlField = createUrlField();
        sendButton = createSendButton(sendAction);
        saveButton = createSaveButton();


        // Đặt kiểu bảng điều khiển
        setupPanelStyle();

        // Xây dựng bố cục
        buildLayout();
    }

    /**
     * Đặt phong cách tổng thể của bảng điều khiển
     */
    private void setupPanelStyle() {
        setLayout(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));
    }

    /**
     * Tạo hộp thả xuống lựa chọn phương pháp
     */
    private JComboBox<String> createMethodComboBox() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
        JComboBox<String> combo = new JComboBox<>(methods);

        // Đặt kích thước
        Dimension size = new Dimension(METHOD_COMBO_WIDTH, COMPONENT_HEIGHT);
        combo.setPreferredSize(size);
        combo.setMaximumSize(size);
        combo.setMinimumSize(size);
        combo.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Đặt phông chữ
        combo.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));

        // WebSocket Xử lý đặc biệt các giao thức
        if (protocol.isWebSocketProtocol()) {
            combo.setVisible(false);
            combo.setSelectedItem("GET");
        }

        return combo;
    }

    /**
     * Tạo URL hộp đầu vào
     */
    private JTextField createUrlField() {
        JTextField field = new RequestUrlField(
                null,
                30,
                I18nUtil.getMessage(MessageKeys.REQUEST_URL_PLACEHOLDER)
        );

        // Đặt kích thước
        field.setPreferredSize(new Dimension(500, COMPONENT_HEIGHT));
        field.setMinimumSize(new Dimension(300, COMPONENT_HEIGHT));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));
        field.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Đặt phông chữ
        field.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        return field;
    }

    public void setPathVariablesSupplier(Supplier<List<HttpParam>> pathVariablesSupplier) {
        if (urlField instanceof RequestUrlField requestUrlField) {
            requestUrlField.setPathVariablesSupplier(pathVariablesSupplier);
        }
    }

    /**
     * Tạo Gửi/Nút kết nối
     */
    private JButton createSendButton(ActionListener sendAction) {
        String text = protocol.isWebSocketProtocol() ?
                I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT) :
                I18nUtil.getMessage(MessageKeys.BUTTON_SEND);
        String iconPath = protocol.isWebSocketProtocol() ?
                "icons/connect.svg" : "icons/send.svg";
        String tooltip = protocol.isWebSocketProtocol() ?
                I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT_TOOLTIP) :
                I18nUtil.getMessage(MessageKeys.BUTTON_SEND_TOOLTIP);

        PrimaryButton button = new PrimaryButton(text, iconPath);
        button.setToolTipText(tooltip);
        button.addActionListener(sendAction);
        applyActionButtonSize(button, SEND_BUTTON_MIN_WIDTH);

        return button;
    }

    /**
     * Tạo nút lưu
     */
    private JButton createSaveButton() {
        SecondaryButton button = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_SAVE),
                "icons/save.svg"
        );
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE_TOOLTIP));
        button.addActionListener(e -> {
            if (UiSingletonFactory.getInstance(RequestEditorPanel.class).saveCurrentRequest()) {
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SAVE_SUCCESS));
            }
        });
        applyActionButtonSize(button, SAVE_BUTTON_MIN_WIDTH);

        return button;
    }

    private static void applyActionButtonSize(AbstractButton button, int minimumWidth) {
        button.setPreferredSize(null);
        Dimension preferredSize = button.getPreferredSize();
        Dimension size = new Dimension(Math.max(preferredSize.width, minimumWidth), COMPONENT_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
    }


    /**
     * Xây dựng bố cục bảng điều khiển
     */
    private void buildLayout() {
        // Bên trái: lựa chọn phương pháp + URL
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        if (methodBox.isVisible()) {
            leftPanel.add(methodBox);
            leftPanel.add(Box.createHorizontalStrut(6));
        }
        leftPanel.add(urlField);
        leftPanel.add(Box.createHorizontalStrut(10)); // URLKhoảng cách giữa hộp nhập và nhóm nút

        // Phải: nhóm nút
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        rightPanel.add(sendButton);
        rightPanel.add(Box.createHorizontalStrut(6));
        rightPanel.add(saveButton);

        // Thêm vào bảng điều khiển chính
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }


    /**
     * Tự động cập nhật kiểu nút (màu sắc)
     */
    private void updateButtonStyle(JButton button, Color baseColor, Color hoverColor, Color pressColor) {
        // Đặt màu nút thành  ClientProperty
        button.putClientProperty("baseColor", baseColor);
        button.putClientProperty("hoverColor", hoverColor);
        button.putClientProperty("pressColor", pressColor);

        // Đặt lại cờ bộ đệm để buộc đọc lại màu vào lần vẽ tiếp theo
        button.putClientProperty("colorsInitialized", false);

        // Buộc làm mới
        button.repaint();
    }

    /**
     * Nút chuyển đổi là  Send trạng thái
     */
    public void setSendButtonToSend(ActionListener sendAction) {
        // Loại bỏ trình nghe cũ
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // Đặt văn bản và biểu tượng
        if (protocol.isWebSocketProtocol()) {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT));
            sendButton.setIcon(IconUtil.createOnPrimary("icons/connect.svg", ICON_SIZE, ICON_SIZE));
            sendButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT_TOOLTIP));
        } else {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_SEND));
            sendButton.setIcon(IconUtil.createOnPrimary("icons/send.svg", ICON_SIZE, ICON_SIZE));
            sendButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SEND_TOOLTIP));
        }

        sendButton.setEnabled(true);
        applyActionButtonSize(sendButton, SEND_BUTTON_MIN_WIDTH);

        // Đặt lại về màu xanh mặc định
        updateButtonStyle(sendButton, ModernColors.getPrimary(), ModernColors.getPrimaryLight(),
                ModernColors.getPrimaryDarker());

        // Thêm người nghe mới
        sendButton.addActionListener(sendAction);
    }

    /**
     * Nút chuyển đổi là  Cancel trạng thái
     */
    public void setSendButtonToCancel(ActionListener cancelAction) {
        // Loại bỏ trình nghe cũ
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // Đặt làm kiểu nút hủy
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        sendButton.setIcon(IconUtil.createOnPrimary("icons/cancel.svg", ICON_SIZE, ICON_SIZE));
        sendButton.setEnabled(true);
        applyActionButtonSize(sendButton, SEND_BUTTON_MIN_WIDTH);

        // Nút thay đổi sang màu cảnh báo (màu cam)
        updateButtonStyle(sendButton, ModernColors.getWarning(), ModernColors.getWarningDark(),
                ModernColors.getWarningDarker());

        // Thêm người nghe mới
        sendButton.addActionListener(cancelAction);
    }

    /**
     * Nút chuyển đổi là  Close trạng thái
     */
    public void setSendButtonToClose(ActionListener closeAction) {
        // Loại bỏ trình nghe cũ
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // Đặt làm kiểu nút đóng
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        sendButton.setIcon(IconUtil.createOnPrimary("icons/close.svg", ICON_SIZE, ICON_SIZE));
        sendButton.setEnabled(true);

        // Thay đổi nút thành màu xám trung tính
        updateButtonStyle(sendButton, ModernColors.getNeutral(), ModernColors.getNeutralDark(),
                ModernColors.getNeutralDarker());

        // Thêm người nghe mới
        sendButton.addActionListener(closeAction);
    }

    public void setReadOnlySnapshotMode() {
        methodBox.setEnabled(false);
        urlField.setEditable(false);
        sendButton.setVisible(false);
        sendButton.setEnabled(false);
        saveButton.setVisible(false);
        saveButton.setEnabled(false);
    }
}
