package com.nvhuong.devworkbench.plugin.kafka.connection.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.DevWorkbenchComboBox;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.connection.ConnectionToolbarUi;
import com.nvhuong.devworkbench.plugin.kafka.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

import static com.nvhuong.devworkbench.plugin.kafka.KafkaI18n.t;

public class KafkaConnectionPanel extends JPanel {

    private static final String CARD_CONNECT = "connect";
    private static final String CARD_DISCONNECT = "disconnect";
    private static final int GROUP_GAP = 12;
    /** Bốn nhóm“Nhãn + hộp đầu vào”Cột: Không 1 Được rồi cấu hình / Host / Client ID / Khu vực điều hành, trang 2 Được rồi thỏa thuận / SASL / Người dùng / mật khẩu. */
    private static final int PROFILE_GROUP_MIN_WIDTH = 150;
    private static final int PROFILE_GROUP_PREF_WIDTH = 240;
    private static final int HOST_GROUP_MIN_WIDTH = 110;
    private static final int HOST_GROUP_PREF_WIDTH = 200;
    private static final int CLIENT_ID_GROUP_MIN_WIDTH = 90;
    private static final int CLIENT_ID_GROUP_PREF_WIDTH = 170;
    private static final int ACTION_GROUP_MIN_WIDTH = 90;
    private static final int ACTION_GROUP_PREF_WIDTH = 170;

    public final JComboBox<String> profileCombo;
    public final JButton newProfileBtn;
    public final JButton saveProfileBtn;
    public final JButton saveAsProfileBtn;
    public final JButton deleteProfileBtn;
    public final JTextField bootstrapField;
    public final JTextField clientIdField;
    public final JComboBox<String> securityProtocolCombo;
    public final JComboBox<String> saslMechanismCombo;
    public final JTextField usernameField;
    public final JPasswordField passwordField;
    /** Hai hàng chia sẻ cùng một lưới, đảm bảo rằng các cạnh trái và phải của mỗi cột được căn chỉnh chặt chẽ. */
    public final JPanel formGrid;
    public final JButton connectBtn;
    public final JButton disconnectBtn;
    public final JButton testConnectionBtn;
    public final CardLayout btnCardLayout;
    public final JPanel btnCard;
    private final JComponent[] saslComponents;

    public KafkaConnectionPanel(Runnable connectAction, Runnable disconnectAction) {
        this(connectAction, disconnectAction, () -> {
        });
    }

    public KafkaConnectionPanel(Runnable connectAction, Runnable disconnectAction, Runnable testConnectionAction) {
        super(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionHeader(this, 3, 6, 3, 6);

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        ConnectionToolbarUi.compactControl(profileCombo);
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(value -> value == null ? "" : value));

        newProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_NEW), "icons/plus.svg");
        saveProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE), "icons/save.svg");
        saveProfileBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE) + " (Ctrl+S)");
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE_AS), "icons/duplicate.svg");
        deleteProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_DELETE), "icons/delete.svg");

        bootstrapField = new JTextField("localhost:9092");
        bootstrapField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_HOST_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(bootstrapField);
        bootstrapField.addActionListener(e -> connectAction.run());

        clientIdField = new JTextField("dev-workbench-toolbox");
        clientIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(clientIdField);

        securityProtocolCombo = new DevWorkbenchComboBox<>(
                new String[]{"PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL"},
                DevWorkbenchComboBox.WidthMode.FIXED_MAX);
        ConnectionToolbarUi.compactControl(securityProtocolCombo);

        saslMechanismCombo = new DevWorkbenchComboBox<>(
                new String[]{"PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512"},
                DevWorkbenchComboBox.WidthMode.FIXED_MAX);
        ConnectionToolbarUi.compactControl(saslMechanismCombo);

        usernameField = new JTextField("");
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_USER_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(usernameField);
        usernameField.addActionListener(e -> connectAction.run());

        passwordField = new JPasswordField("");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_PASS_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(passwordField);
        passwordField.addActionListener(e -> connectAction.run());

        connectBtn = ModernButtonFactory.createCompactButton(t(MessageKeys.TOOLBOX_KAFKA_CONNECT), true, "icons/connect.svg");
        connectBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CONNECT_TOOLTIP));
        connectBtn.addActionListener(e -> connectAction.run());

        disconnectBtn = ModernButtonFactory.createCompactButton(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT), false, "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> disconnectAction.run());

        testConnectionBtn = ModernButtonFactory.createCompactButton(t(MessageKeys.TOOLBOX_KAFKA_TEST_CONNECTION), false, "icons/check.svg");
        testConnectionBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_TEST_CONNECTION_TOOLTIP));
        testConnectionBtn.addActionListener(e -> testConnectionAction.run());

        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CARD_CONNECT);
        btnCard.add(disconnectBtn, CARD_DISCONNECT);
        btnCardLayout.show(btnCard, CARD_CONNECT);

        JPanel profileGroup = new JPanel(new MigLayout(
                "insets 0, novisualpadding, gapx 0",
                "[grow,fill]2[" + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]0["
                        + ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE + "!]",
                "[]"
        ));
        profileGroup.setOpaque(false);
        profileGroup.add(profileCombo, "wmin 0, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!");
        profileGroup.add(newProfileBtn);
        profileGroup.add(saveProfileBtn);
        profileGroup.add(saveAsProfileBtn);
        profileGroup.add(deleteProfileBtn);

        JPanel actions = new JPanel(new MigLayout("insets 0, novisualpadding, gapx 6", "[][]", "[]"));
        actions.setOpaque(false);
        String buttonHeight = "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!";
        actions.add(testConnectionBtn, buttonHeight);
        actions.add(btnCard, buttonHeight);

        formGrid = new JPanel(new MigLayout(
                "insets 0, novisualpadding, gapy 4",
                growFieldColumns(PROFILE_GROUP_MIN_WIDTH, PROFILE_GROUP_PREF_WIDTH) + GROUP_GAP
                        + growFieldColumns(HOST_GROUP_MIN_WIDTH, HOST_GROUP_PREF_WIDTH) + GROUP_GAP
                        + growFieldColumns(CLIENT_ID_GROUP_MIN_WIDTH, CLIENT_ID_GROUP_PREF_WIDTH) + GROUP_GAP
                        + growFieldColumns(ACTION_GROUP_MIN_WIDTH, ACTION_GROUP_PREF_WIDTH),
                "[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!,center]4[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!,center]"
        ));
        formGrid.setOpaque(false);
        String controlHeight = "wmin 0, h " + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!";

        // KHÔNG.  1 Hàng: Kết nối cơ bản
        formGrid.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_PROFILE)));
        formGrid.add(profileGroup);
        formGrid.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_HOST)));
        formGrid.add(bootstrapField, controlHeight);
        formGrid.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID)));
        formGrid.add(clientIdField, controlHeight);
        formGrid.add(actions, "span 2, right, wrap");

        // KHÔNG.  2 Hàng: Chứng nhận bảo mật
        JLabel saslLabel = compactLabel(
                MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM_SHORT,
                MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM);
        JLabel userLabel = ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_USER));
        JLabel passLabel = ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_PASS));
        formGrid.add(compactLabel(
                MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL_SHORT,
                MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL));
        formGrid.add(securityProtocolCombo, controlHeight);
        formGrid.add(saslLabel);
        formGrid.add(saslMechanismCombo, controlHeight);
        formGrid.add(userLabel);
        formGrid.add(usernameField, controlHeight);
        formGrid.add(passLabel);
        formGrid.add(passwordField, controlHeight);
        saslComponents = new JComponent[]{saslLabel, saslMechanismCombo, userLabel, usernameField, passLabel, passwordField};

        JPanel wrapper = new JPanel(new MigLayout("insets 0, novisualpadding, fillx", "[grow,fill]", "[]"));
        wrapper.setOpaque(false);
        wrapper.add(formGrid, "growx, wmin 0");
        add(wrapper, BorderLayout.CENTER);
        ConnectionToolbarUi.lockConnectionPanelHeight(this, true);
    }

    /**
     * Cột nhãn thích ứng theo nhãn dài nhất trong cột; cửa sổ hẹp của cột trường có thể được nén thành min，Cửa sổ rộng phân chia đều không gian thừa để tránh để lại khoảng trống ở phía bên phải và có thể kéo dài tối đa 2 lần.
     */
    private static String growFieldColumns(int minWidth, int prefWidth) {
        int maxWidth = prefWidth * 2;
        return "[pref!,right]4[" + minWidth + ":" + prefWidth + ":" + maxWidth + ",fill,grow,shrink]";
    }

    private static JLabel compactLabel(String shortKey, String fullKey) {
        JLabel label = ConnectionToolbarUi.label(t(shortKey));
        label.setToolTipText(t(fullKey));
        return label;
    }

    /**
     * Không SASL Bị vô hiệu hóa (không bị ẩn) theo giao thứcSASL Cơ chế và mật khẩu tài khoản: Bố cục vẫn ổn định và người dùng cũng có thể xem những mục nào có thể điền sau khi chuyển đổi giao thức.
     */
    public void setSaslFieldsEnabled(boolean enabled) {
        for (JComponent component : saslComponents) {
            component.setEnabled(enabled);
        }
    }

    public void showConnected(boolean connected) {
        btnCardLayout.show(btnCard, connected ? CARD_DISCONNECT : CARD_CONNECT);
    }
}
