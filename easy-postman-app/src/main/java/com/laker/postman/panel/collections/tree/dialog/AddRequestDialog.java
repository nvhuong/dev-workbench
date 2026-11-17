package com.laker.postman.panel.collections.tree.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.http.request.HttpRequestFactory;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Thêm hộp thoại yêu cầu
 * Chứa lựa chọn giao thức và nhập tên
 */
public class AddRequestDialog {
    static final int DIALOG_WIDTH = 400;
    static final int DIALOG_HEIGHT = 280;
    private static final int PROTOCOL_CARD_WIDTH = 96;
    private static final int PROTOCOL_CARD_HEIGHT = 54;

    private final DefaultMutableTreeNode groupNode;
    private final CollectionTreePanel leftPanel;
    private JDialog dialog;
    private JTextField nameField;
    private JToggleButton httpBtn;
    private JToggleButton wsBtn;

    public AddRequestDialog(DefaultMutableTreeNode groupNode, CollectionTreePanel leftPanel) {
        this.groupNode = groupNode;
        this.leftPanel = leftPanel;
        initDialog();
    }

    /**
     * Khởi tạo hộp thoại
     */
    private void initDialog() {
        dialog = new JDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_TITLE),
                true
        );
        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        ToolWindowSurfaceStyle.applyDialogSurface((JComponent) dialog.getContentPane());

        JPanel mainPanel = createMainPanel();
        JPanel buttonPanel = createButtonPanel();

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton((JButton) buttonPanel.getComponent(0));
    }

    /**
     * Tạo bảng điều khiển chính
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));

        JPanel namePanel = createNamePanel();
        JPanel protocolPanel = createProtocolPanel();

        mainPanel.add(namePanel);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(protocolPanel);

        return mainPanel;
    }

    /**
     * Tạo bảng nhập tên
     */
    private JPanel createNamePanel() {
        JPanel namePanel = new JPanel(new BorderLayout(10, 5));
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_NAME));
        nameLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(0, 30));
        SettingsInputStyle.apply(nameField);

        namePanel.add(nameLabel, BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);

        return namePanel;
    }

    /**
     * Tạo bảng lựa chọn giao thức
     */
    private JPanel createProtocolPanel() {
        ButtonGroup protocolGroup;
        JPanel protocolPanel = new JPanel(new BorderLayout(0, 8));
        protocolPanel.setOpaque(false);

        JLabel protocolLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_PROTOCOL));
        protocolLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

        JPanel buttonsPanel = new JPanel(createProtocolButtonsLayout());
        buttonsPanel.setOpaque(false);

        protocolGroup = new ButtonGroup();
        httpBtn = createProtocolButton("HTTP", "icons/http.svg", true);
        wsBtn = createProtocolButton("WebSocket", "icons/websocket.svg", false);

        protocolGroup.add(httpBtn);
        protocolGroup.add(wsBtn);

        buttonsPanel.add(httpBtn);
        buttonsPanel.add(wsBtn);

        protocolPanel.add(protocolLabel, BorderLayout.NORTH);
        protocolPanel.add(buttonsPanel, BorderLayout.CENTER);

        return protocolPanel;
    }

    static FlowLayout createProtocolButtonsLayout() {
        return new FlowLayout(FlowLayout.CENTER, 10, 0);
    }

    /**
     * Tạo nút Thỏa thuận
     */
    private JToggleButton createProtocolButton(String text, String iconPath, boolean selected) {
        JToggleButton btn = new ProtocolOptionButton(text, new FlatSVGIcon(iconPath, 22, 22));
        btn.setSelected(selected);
        return btn;
    }

    static final class ProtocolOptionButton extends JToggleButton {
        private static final int ARC = 8;

        ProtocolOptionButton(String text, Icon icon) {
            super(text, icon);
            setUI(new BasicToggleButtonUI());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            setForeground(ModernColors.getTextPrimary());
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setIconTextGap(4);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 6, 8));
            setPreferredSize(new Dimension(PROTOCOL_CARD_WIDTH, PROTOCOL_CARD_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 1;
            int height = getHeight() - 1;
            ButtonModel model = getModel();
            Color background = resolveBackground(model);
            Color border = isSelected() || isFocusOwner()
                    ? ModernColors.getPrimary()
                    : ModernColors.getBorderMediumColor();

            g2.setColor(background);
            g2.fillRoundRect(0, 0, width, height, ARC, ARC);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, width, height, ARC, ARC);

            g2.dispose();
            setForeground(isEnabled() ? ModernColors.getTextPrimary() : ModernColors.getTextHint());
            super.paintComponent(g);
        }

        private Color resolveBackground(ButtonModel model) {
            if (isSelected()) {
                return ModernColors.primaryWithAlpha(ModernColors.isDarkTheme() ? 52 : 30);
            }
            if (model.isRollover() || model.isPressed()) {
                return ModernColors.getHoverBackgroundColor();
            }
            return ModernColors.getInputBackgroundColor();
        }
    }

    /**
     * Tạo một bảng nút
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);

        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);

        okButton.addActionListener(e -> handleOk());
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    /**
     * Xử lý nút OK
     */
    private void handleOk() {
        String requestName = nameField.getText().trim();
        if (requestName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    dialog,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_NAME_EMPTY),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                    JOptionPane.WARNING_MESSAGE
            );
            nameField.requestFocus();
            return;
        }

        RequestItemProtocolEnum protocol = getSelectedProtocol();
        createAndAddRequest(requestName, protocol);
        dialog.dispose();
    }

    /**
     * Nhận giao thức đã chọn
     */
    private RequestItemProtocolEnum getSelectedProtocol() {
        if (httpBtn.isSelected()) {
            return RequestItemProtocolEnum.HTTP;
        }
        return wsBtn.isSelected() ? RequestItemProtocolEnum.WEBSOCKET : RequestItemProtocolEnum.HTTP;
    }

    /**
     * Tạo và thêm yêu cầu vào cây
     */
    private void createAndAddRequest(String requestName, RequestItemProtocolEnum protocol) {
        if (groupNode == null) return;

        HttpRequestItem defaultRequest = HttpRequestFactory.createBlankRequest(protocol);
        defaultRequest.setName(requestName);

        // Thêm vào cây
        DefaultMutableTreeNode reqNode = CollectionTreeNodes.requestNode(defaultRequest);
        groupNode.add(reqNode);
        leftPanel.getTreeModel().reload(groupNode);
        JTree tree = leftPanel.getRequestTree();
        tree.expandPath(new TreePath(groupNode.getPath()));
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        // Xác định vị trí nút yêu cầu mới được tạo
        TreePath newPath = new TreePath(reqNode.getPath());
        tree.setSelectionPath(newPath);
        tree.scrollPathToVisible(newPath);

        // Tự động mở các yêu cầu mới tạo
        UiSingletonFactory.getInstance(RequestEditorPanel.class).showOrCreateTab(defaultRequest);
    }

    /**
     * Hiển thị hộp thoại
     */
    public void show() {
        SwingUtilities.invokeLater(nameField::requestFocus);
        dialog.setVisible(true);
    }
}
