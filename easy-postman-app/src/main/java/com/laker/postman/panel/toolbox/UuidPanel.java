package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.FileChooserUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UUIDTạo bảng công cụ - Phiên bản nâng cao
 */
@Slf4j
public class UuidPanel extends JPanel {

    private JTextArea uuidArea;
    private JSpinner countSpinner;
    private JCheckBox uppercaseCheckBox;
    private JCheckBox withHyphensCheckBox;
    private JLabel statusLabel;
    private JComboBox<String> versionComboBox;
    private JComboBox<String> separatorComboBox;
    private JTextArea parseArea;
    private JTextField namespaceField;
    private JTextField nameField;

    // UUID Biểu thức chính quy
    private static final Pattern UUID_NO_HYPHEN_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern UUID_FLEXIBLE_PATTERN = Pattern.compile("(?i)[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}");
    private static final long UUID_EPOCH_OFFSET_100NS = 0x01b21dd213814000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS z")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter UTC_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'")
            .withZone(ZoneId.of("UTC"));

    // Không gian tên được xác định trước
    private static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

    public UuidPanel() {
        initUI();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);

        // Bảng chia chính
        JSplitPane splitPane = ToolboxWorkbench.horizontalSplit(
                createGeneratorPanel(),
                createParsePanel(),
                0
        );
        splitPane.setResizeWeight(0.64);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.64));

        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Tạo một bảng điều khiển máy phát điện
     */
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Bảng cấu hình hàng đầu
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        // Tạo bảng cấu hình
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setOpaque(false);
        configPanel.add(createSectionTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_BATCH_GENERATE)));

        // Dòng đầu tiên: phiên bản và số lượng
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.setOpaque(false);
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION) + ":"));
        versionComboBox = new JComboBox<>(new String[]{
            "UUID v4 (Random)",
            "UUID v1 (Time-based)",
            "UUID v3 (Name-based MD5)",
            "UUID v5 (Name-based SHA-1)"
        });
        versionComboBox.setPreferredSize(new Dimension(200, 28));
        row1.add(versionComboBox);

        row1.add(Box.createHorizontalStrut(10));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT) + ":"));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 10000, 1);
        countSpinner = new JSpinner(spinnerModel);
        countSpinner.setPreferredSize(new Dimension(80, 28));
        countSpinner.setToolTipText("1 - 10000");
        if (countSpinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
        }
        row1.add(countSpinner);
        configPanel.add(row1);

        // Dòng thứ hai: cấu hình định dạng
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.setOpaque(false);
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_FORMAT) + ":"));

        uppercaseCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_UPPERCASE));
        uppercaseCheckBox.setSelected(false);
        row2.add(uppercaseCheckBox);

        withHyphensCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_WITH_HYPHENS));
        withHyphensCheckBox.setSelected(true);
        row2.add(withHyphensCheckBox);

        row2.add(Box.createHorizontalStrut(10));
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR) + ":"));
        separatorComboBox = new JComboBox<>(new String[]{
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_NEWLINE),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_COMMA),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_SPACE),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_SEMICOLON)
        });
        separatorComboBox.setPreferredSize(new Dimension(100, 28));
        row2.add(separatorComboBox);
        configPanel.add(row2);

        // Dòng thứ ba: không gian tên và tên (đối với  v3 và v5）
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.setOpaque(false);
        row3.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE) + ":"));
        JComboBox<String> namespaceCombo = new JComboBox<>(new String[]{
            "DNS", "URL", "OID", "X.500", I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE_CUSTOM)
        });
        namespaceCombo.setPreferredSize(new Dimension(100, 28));
        row3.add(namespaceCombo);

        namespaceField = new JTextField();
        namespaceField.setPreferredSize(new Dimension(150, 28));
        namespaceField.setEnabled(false);
        namespaceField.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE_HINT));
        ToolWindowSurfaceStyle.applyTextComponentInput(namespaceField);
        row3.add(namespaceField);

        row3.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME) + ":"));
        nameField = new JTextField("example.com");
        nameField.setPreferredSize(new Dimension(150, 28));
        ToolWindowSurfaceStyle.applyTextComponentInput(nameField);
        row3.add(nameField);
        row3.setVisible(false); // Ẩn theo mặc định, chỉ khi được chọn v3/v5 được hiển thị
        configPanel.add(row3);

        topPanel.add(configPanel, BorderLayout.CENTER);

        // Bảng nút thao tác
        JButton generateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_GENERATE));
        generateBtn.setPreferredSize(new Dimension(120, 32));
        generateBtn.setFocusPainted(false);

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.setPreferredSize(new Dimension(100, 32));
        copyBtn.setFocusPainted(false);

        JButton copyOneBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COPY_ONE));
        copyOneBtn.setPreferredSize(new Dimension(120, 32));
        copyOneBtn.setFocusPainted(false);

        JButton exportBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT));
        exportBtn.setPreferredSize(new Dimension(100, 32));
        exportBtn.setFocusPainted(false);

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setPreferredSize(new Dimension(100, 32));
        clearBtn.setFocusPainted(false);

        JPanel buttonPanel = ToolboxWorkbench.leftToolbar(
                generateBtn,
                copyBtn,
                copyOneBtn,
                exportBtn,
                clearBtn
        );

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // giữaUUIDKhu vực trưng bày
        uuidArea = new JTextArea();
        uuidArea.setEditable(false);
        uuidArea.setLineWrap(true);
        uuidArea.setWrapStyleWord(false);
        uuidArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        uuidArea.setMargin(new Insets(10, 10, 10, 10));
        ToolWindowSurfaceStyle.applyTextComponentCard(uuidArea);

        JScrollPane scrollPane = new JScrollPane(uuidArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scrollPane);
        JPanel centerPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_GENERATED),
                scrollPane
        );

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bảng thông tin phía dưới
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        bottomPanel.setOpaque(false);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        infoPanel.setOpaque(false);
        JLabel infoLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION_INFO));
        infoLabel.setFont(FontsUtil.getDefaultFont(Font.ITALIC));
        infoLabel.setForeground(ModernColors.getTextSecondary());
        infoPanel.add(infoLabel);

        statusLabel = new JLabel("");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getSuccess());
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel);

        bottomPanel.add(infoPanel, BorderLayout.WEST);
        bottomPanel.add(statusPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Sự kiện nút
        generateBtn.addActionListener(e -> {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        });

        copyBtn.addActionListener(e -> copyToClipboard());

        copyOneBtn.addActionListener(e -> copyFirstUuid());

        exportBtn.addActionListener(e -> exportToFile());

        clearBtn.addActionListener(e -> {
            uuidArea.setText("");
            statusLabel.setText("");
        });

        // Tự động tạo lại khi hộp kiểm thay đổi
        uppercaseCheckBox.addActionListener(e -> refreshIfNotEmpty());
        withHyphensCheckBox.addActionListener(e -> refreshIfNotEmpty());
        separatorComboBox.addActionListener(e -> refreshIfNotEmpty());

        // Sự kiện thay đổi lựa chọn phiên bản
        versionComboBox.addActionListener(e -> {
            int selectedIndex = versionComboBox.getSelectedIndex();
            // v3 và v5 Không gian tên và hộp nhập tên cần được hiển thị
            row3.setVisible(selectedIndex == 2 || selectedIndex == 3);
            refreshIfNotEmpty();
            configPanel.revalidate();
            configPanel.repaint();
        });

        // Sự kiện thay đổi lựa chọn không gian tên
        namespaceCombo.addActionListener(e -> {
            int selected = namespaceCombo.getSelectedIndex();
            if (selected == 4) { // Custom
                namespaceField.setEnabled(true);
                namespaceField.setText("");
            } else {
                namespaceField.setEnabled(false);
                UUID namespace = switch (selected) {
                    case 0 -> NAMESPACE_DNS;
                    case 1 -> NAMESPACE_URL;
                    case 2 -> NAMESPACE_OID;
                    case 3 -> NAMESPACE_X500;
                    default -> NAMESPACE_DNS;
                };
                namespaceField.setText(namespace.toString());
            }
            refreshIfNameBasedAndNotEmpty();
        });

        countSpinner.addChangeListener(e -> refreshIfNotEmpty());
        bindAutoGenerateListener(namespaceField, true);
        bindAutoGenerateListener(nameField, true);

        // Khởi tạo các trường không gian tên
        namespaceField.setText(NAMESPACE_DNS.toString());

        // Thế hệ ban đầu UUID（số lượng mặc định)
        generateUuid((Integer) countSpinner.getValue());

        return panel;
    }

    /**
     * Tạo một bảng phân tích cú pháp
     */
    private JPanel createParsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JPanel topPanel = new JPanel(new BorderLayout(5, 8));
        topPanel.setOpaque(false);
        topPanel.add(createSectionTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE)), BorderLayout.NORTH);

        // Khu vực đầu vào hàng đầu
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_INPUT) + ":"), BorderLayout.NORTH);

        JTextField inputField = new JTextField();
        inputField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        inputField.setPreferredSize(new Dimension(0, 30));
        ToolWindowSurfaceStyle.applyTextComponentInput(inputField);
        inputPanel.add(inputField, BorderLayout.CENTER);

        JButton parseBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE));
        parseBtn.setPreferredSize(new Dimension(100, 30));
        parseBtn.setFocusPainted(false);
        inputPanel.add(parseBtn, BorderLayout.EAST);

        topPanel.add(inputPanel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        // Khu vực hiển thị kết quả phân tích
        parseArea = new JTextArea();
        parseArea.setEditable(false);
        parseArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        parseArea.setMargin(new Insets(10, 10, 10, 10));
        parseArea.setLineWrap(true);
        parseArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentCard(parseArea);

        JScrollPane scrollPane = new JScrollPane(parseArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scrollPane);
        panel.add(ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_RESULT),
                scrollPane
        ), BorderLayout.CENTER);

        // Sự kiện nút phân tích cú pháp
        parseBtn.addActionListener(e -> parseUuid(inputField.getText().trim()));

        // Nhập sự kiện của hộp đầu vào
        inputField.addActionListener(e -> parseUuid(inputField.getText().trim()));

        return panel;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = ToolboxWorkbench.sectionTitle(text);
        label.setForeground(ModernColors.getTextPrimary());
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * phát raUUID
     */
    private void generateUuid(int count) {
        StringBuilder sb = new StringBuilder();
        boolean uppercase = uppercaseCheckBox.isSelected();
        boolean withHyphens = withHyphensCheckBox.isSelected();
        int versionIndex = versionComboBox.getSelectedIndex();
        String separator = getSeparator();

        for (int i = 0; i < count; i++) {
            String uuid = switch (versionIndex) {
                case 0 -> UUID.randomUUID().toString(); // v4
                case 1 -> generateUuidV1(); // v1
                case 2 -> generateUuidV3(); // v3
                case 3 -> generateUuidV5(); // v5
                default -> UUID.randomUUID().toString();
            };

            if (!withHyphens) {
                uuid = uuid.replace("-", "");
            }

            if (uppercase) {
                uuid = uuid.toUpperCase();
            }

            sb.append(uuid);
            if (i < count - 1) {
                sb.append(separator);
            }
        }

        uuidArea.setText(sb.toString());
        uuidArea.setCaretPosition(0);

        // Cập nhật thông tin trạng thái
        String statusText = String.format("%s: %d",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), count);
        statusLabel.setText(statusText);
    }

    /**
     * phát ra UUID v1 (Dựa trên thời gian)
     */
    private String generateUuidV1() {
        long timestamp = System.currentTimeMillis();
        long time = timestamp * 10000 + UUID_EPOCH_OFFSET_100NS;

        long timeLow = time & 0xFFFFFFFFL;
        long timeMid = (time >> 32) & 0xFFFFL;
        long timeHi = ((time >> 48) & 0x0FFFL) | 0x1000; // Version 1

        long clockSeq = SECURE_RANDOM.nextInt(0x3FFF) | 0x8000; // Variant
        long node = getNodeId();

        return String.format("%08x-%04x-%04x-%04x-%012x",
                timeLow, timeMid, timeHi, clockSeq, node);
    }

    /**
     * phát ra UUID v3 (Dựa trên tên MD5)
     */
    private String generateUuidV3() {
        return generateNameBasedUuid("MD5", 3);
    }

    /**
     * phát ra UUID v5 (Dựa trên tên SHA-1)
     */
    private String generateUuidV5() {
        return generateNameBasedUuid("SHA-1", 5);
    }

    /**
     * Tạo dựa trên tên UUID（v3 hoặc v5）
     */
    private String generateNameBasedUuid(String algorithm, int version) {
        try {
            String namespaceStr = namespaceField.getText().trim();
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                name = "example.com"; // Tên mặc định
            }

            // Giải quyết các không gian tên UUID
            UUID namespace;
            try {
                namespaceStr = namespaceStr.replace("-", "");
                if (namespaceStr.length() == 32) {
                    namespaceStr = String.format("%s-%s-%s-%s-%s",
                        namespaceStr.substring(0, 8),
                        namespaceStr.substring(8, 12),
                        namespaceStr.substring(12, 16),
                        namespaceStr.substring(16, 20),
                        namespaceStr.substring(20, 32));
                }
                namespace = UUID.fromString(namespaceStr);
            } catch (Exception e) {
                namespace = NAMESPACE_DNS; // Được sử dụng theo mặc định DNS không gian tên
            }

            // không gian tên UUID và sự kết hợp tên
            byte[] namespaceBytes = toBytes(namespace);
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[namespaceBytes.length + nameBytes.length];
            System.arraycopy(namespaceBytes, 0, combined, 0, namespaceBytes.length);
            System.arraycopy(nameBytes, 0, combined, namespaceBytes.length, nameBytes.length);

            // Tính hàm băm
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(combined);

            // kết cấu UUID
            long msb = 0;
            long lsb = 0;

            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }

            // Đặt phiên bản và biến thể
            msb &= ~0x0000f000L; // Xóa bit phiên bản
            msb |= ((long) version) << 12; // Đặt phiên bản
            lsb &= ~0xc000000000000000L; // Xóa các bit biến thể
            lsb |= 0x8000000000000000L; // Đặt biến thể (RFC 4122）

            return formatUuid(msb, lsb);
        } catch (Exception e) {
            log.error("Failed to generate name-based UUID", e);
            return UUID.randomUUID().toString(); // Trả về ngẫu nhiên khi thất bại UUID
        }
    }

    /**
     * Sẽ UUID Chuyển đổi sang mảng byte
     */
    private byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> (8 * (7 - i)));
        }

        return buffer;
    }

    /**
     * Định dạng UUID
     */
    private String formatUuid(long msb, long lsb) {
        return String.format("%08x-%04x-%04x-%04x-%012x",
                (msb >> 32) & 0xFFFFFFFFL,
                (msb >> 16) & 0xFFFFL,
                msb & 0xFFFFL,
                (lsb >> 48) & 0xFFFFL,
                lsb & 0xFFFFFFFFFFFFL);
    }

    /**
     * Nhận nútID (MACđịa chỉ hoặc số ngẫu nhiên)
     */
    private long getNodeId() {
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();
            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                long node = 0;
                for (int i = 0; i < Math.min(mac.length, 6); i++) {
                    node = (node << 8) | (mac[i] & 0xff);
                }
                return node;
            }
        } catch (Exception e) {
            // Ignore
        }
        // Sử dụng số ngẫu nhiên làm nútID
        return new SecureRandom().nextLong() & 0xFFFFFFFFFFFFL;
    }

    /**
     * Lấy dấu phân cách
     */
    private String getSeparator() {
        int index = separatorComboBox.getSelectedIndex();
        return switch (index) {
            case 1 -> ", ";
            case 2 -> " ";
            case 3 -> "; ";
            default -> "\n";
        };
    }

    /**
     * phân tíchUUID
     */
    private void parseUuid(String input) {
        if (input.isEmpty()) {
            parseArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_EMPTY));
            return;
        }

        // Cho phép dán ở nhiều định dạng (có /Không có dấu gạch nối, kể cả hậu tố và hậu tố, ngay cả trong một khối văn bản UUID）
        String uuid = extractFirstUuidHex(input);

        if (uuid == null || !UUID_NO_HYPHEN_PATTERN.matcher(uuid).matches()) {
            parseArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_INVALID));
            return;
        }

        // Thêm dấu gạch nối
        String formattedUuid = String.format("%s-%s-%s-%s-%s",
                uuid.substring(0, 8),
                uuid.substring(8, 12),
                uuid.substring(12, 16),
                uuid.substring(16, 20),
                uuid.substring(20, 32));

        StringBuilder result = new StringBuilder();
        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_RESULT)).append("\n");
        result.append("\n");

        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_STANDARD_FORMAT)).append(":\n");
        result.append("   ").append(formattedUuid).append("\n\n");

        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_UPPERCASE)).append(":\n");
        result.append("   ").append(formattedUuid.toUpperCase()).append("\n\n");

        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_WITHOUT_HYPHENS)).append(":\n");
        result.append("   ").append(uuid).append("\n\n");

        // Phân tích các phiên bản và biến thể
        int version = Integer.parseInt(uuid.substring(12, 13), 16);
        int variantNibble = Integer.parseInt(uuid.substring(16, 17), 16);

        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION)).append(": ");
        result.append(version).append("\n");
        result.append("   ");
        switch (version) {
            case 1: result.append("(Time-based)"); break;
            case 2: result.append("(DCE Security)"); break;
            case 3: result.append("(Name-based MD5)"); break;
            case 4: result.append("(Random)"); break;
            case 5: result.append("(Name-based SHA-1)"); break;
            case 6: result.append("(Reordered Time-based)"); break;
            case 7: result.append("(Unix Time-based)"); break;
            case 8: result.append("(Custom)"); break;
            default: result.append("(Unknown)"); break;
        }
        result.append("\n\n");

        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VARIANT)).append(": ");
        result.append(resolveVariantName(variantNibble)).append("\n");
        result.append("\n");

        appendTimeInfoIfAvailable(result, uuid, version);

        // trong trường hợp v3 hoặc v5，Mẹo Đây là tên dựa UUID
        if (version == 3 || version == 5) {
            result.append("\n");
            result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME_BASED)).append("\n");
            result.append("   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME_BASED_DESC)).append("\n");
        }

        parseArea.setText(result.toString());
        parseArea.setCaretPosition(0);
    }

    private String extractFirstUuidHex(String input) {
        String normalized = input.trim();
        if (normalized.isEmpty()) return null;

        Matcher matcher = UUID_FLEXIBLE_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group().replace("-", "").toLowerCase(Locale.ROOT);
        }

        // Tóm lại: hỗ trợ urn:uuid:{...} Loại cảnh này
        normalized = normalized.replace("urn:uuid:", "")
                .replace("URN:UUID:", "")
                .replace("{", "")
                .replace("}", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
        if (UUID_NO_HYPHEN_PATTERN.matcher(normalized).matches()) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String resolveVariantName(int variantNibble) {
        if ((variantNibble & 0x8) == 0x0) return "NCS (Reserved)";
        if ((variantNibble & 0xC) == 0x8) return "RFC 4122";
        if ((variantNibble & 0xE) == 0xC) return "Microsoft (Reserved)";
        return "Future (Reserved)";
    }

    private void appendTimeInfoIfAvailable(StringBuilder result, String uuidHex, int version) {
        try {
            switch (version) {
                case 1 -> {
                    long timeLow = Long.parseLong(uuidHex.substring(0, 8), 16);
                    long timeMid = Long.parseLong(uuidHex.substring(8, 12), 16);
                    long timeHi = Long.parseLong(uuidHex.substring(13, 16), 16);
                    long timestamp100ns = (timeHi << 48) | (timeMid << 32) | timeLow;
                    appendDecodedTime(result, timestamp100nsToUnixMs(timestamp100ns), "v1");
                }
                case 6 -> {
                    long timeHigh = Long.parseLong(uuidHex.substring(0, 8), 16);
                    long timeMid = Long.parseLong(uuidHex.substring(8, 12), 16);
                    long timeLow = Long.parseLong(uuidHex.substring(13, 16), 16);
                    long timestamp100ns = (timeHigh << 28) | (timeMid << 12) | timeLow;
                    appendDecodedTime(result, timestamp100nsToUnixMs(timestamp100ns), "v6");
                }
                case 7 -> {
                    long unixTimeMs = Long.parseLong(uuidHex.substring(0, 12), 16);
                    appendDecodedTime(result, unixTimeMs, "v7");
                }
                default -> {
                    // Vô thời gian UUID Không có thông tin thời gian
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse UUID timestamp: {}", e.getMessage());
        }
    }

    private void appendDecodedTime(StringBuilder result, long unixTimeMs, String source) {
        if (unixTimeMs < 0) return;
        Instant instant = Instant.ofEpochMilli(unixTimeMs);
        result.append("⏰ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_TIMESTAMP)).append(" (").append(source).append("):\n");
        result.append("   Local: ").append(LOCAL_DATE_TIME_FORMATTER.format(instant)).append("\n");
        result.append("   UTC  : ").append(UTC_DATE_TIME_FORMATTER.format(instant)).append("\n");
        result.append("   Epoch: ").append(unixTimeMs).append(" ms\n\n");
    }

    private long timestamp100nsToUnixMs(long timestamp100ns) {
        return (timestamp100ns - UUID_EPOCH_OFFSET_100NS) / 10000;
    }

    /**
     * Sao chép vào clipboard
     */
    private void copyToClipboard() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);

            statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.BUTTON_COPY) + " " +
                    I18nUtil.getMessage(MessageKeys.SUCCESS));

            Timer timer = new Timer(3000, e -> updateStatusWithCount());
            timer.setRepeats(false);
            timer.start();
        } else {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
        }
    }

    /**
     * Sao chép cái đầu tiênUUID
     */
    private void copyFirstUuid() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            String[] uuids = text.split("[,;\\s\\n]+");
            if (uuids.length > 0 && !uuids[0].isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(uuids[0]), null);

                statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COPY_ONE) + " " +
                        I18nUtil.getMessage(MessageKeys.SUCCESS));

                Timer timer = new Timer(3000, e -> updateStatusWithCount());
                timer.setRepeats(false);
                timer.start();
            }
        } else {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
        }
    }

    /**
     * Xuất sang tập tin
     */
    private void exportToFile() {
        String text = uuidArea.getText().trim();
        if (text.isEmpty()) {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
            return;
        }

        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "toolbox.uuid.export",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT));
        fileChooser.setSelectedFile(new File("uuids_" + System.currentTimeMillis() + ".txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == SystemFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(text);
                statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT_SUCCESS));

                Timer timer = new Timer(3000, e -> updateStatusWithCount());
                timer.setRepeats(false);
                timer.start();
            } catch (IOException ex) {
                log.error("Failed to export UUIDs", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT_FAILED) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Làm mới nếu có nội dung
     */
    private void refreshIfNotEmpty() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        }
    }

    private void refreshIfNameBasedAndNotEmpty() {
        int selectedIndex = versionComboBox.getSelectedIndex();
        if (selectedIndex == 2 || selectedIndex == 3) {
            refreshIfNotEmpty();
        }
    }

    private void bindAutoGenerateListener(JTextField textField, boolean onlyForNameBased) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                trigger();
            }

            private void trigger() {
                if (onlyForNameBased) {
                    refreshIfNameBasedAndNotEmpty();
                } else {
                    refreshIfNotEmpty();
                }
            }
        });
    }

    /**
     * Cập nhật số lượng hiển thị trạng thái
     */
    private void updateStatusWithCount() {
        String currentText = uuidArea.getText().trim();
        if (!currentText.isEmpty()) {
            String separator = getSeparator().trim();
            String[] parts;
            if (separator.isEmpty()) {
                parts = currentText.split("\\s+");
            } else {
                parts = currentText.split("[,;\\s]+|\\n+");
            }
            List<String> validUuids = new ArrayList<>();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    validUuids.add(part);
                }
            }
            statusLabel.setText(String.format("%s: %d",
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), validUuids.size()));
        }
    }

    /**
     * Hiển thị thông báo thông tin
     */
    private void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                I18nUtil.getMessage(MessageKeys.TIP),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
