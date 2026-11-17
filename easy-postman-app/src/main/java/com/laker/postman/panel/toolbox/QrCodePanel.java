package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.FileChooserUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** QR Code generator with live preview and PNG export. */
@Slf4j
public class QrCodePanel extends JPanel {
    private static final int QR_IMAGE_SIZE = 400;
    private static final int PREVIEW_SIZE = 280;

    private final JTextArea contentArea = new JTextArea(3, 40);
    private final JTextField foregroundField = new JTextField("#000000ff");
    private final JTextField backgroundField = new JTextField("#ffffffff");
    private final JComboBox<CorrectionOption> correctionBox;
    private final JLabel previewLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton downloadButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_DOWNLOAD));
    private final Timer refreshTimer;
    private BufferedImage currentImage;

    public QrCodePanel() {
        correctionBox = new JComboBox<>(new CorrectionOption[]{
                new CorrectionOption(ErrorCorrectionLevel.L, MessageKeys.TOOLBOX_QR_LOW),
                new CorrectionOption(ErrorCorrectionLevel.M, MessageKeys.TOOLBOX_QR_MEDIUM),
                new CorrectionOption(ErrorCorrectionLevel.Q, MessageKeys.TOOLBOX_QR_QUARTILE),
                new CorrectionOption(ErrorCorrectionLevel.H, MessageKeys.TOOLBOX_QR_HIGH)
        });
        correctionBox.setSelectedIndex(1);
        refreshTimer = new Timer(180, event -> refreshQrCode());
        refreshTimer.setRepeats(false);
        initUI();
        installListeners();
        refreshQrCode();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 12, 6, 12));
        body.add(createForm(), BorderLayout.NORTH);
        body.add(createPreview(), BorderLayout.CENTER);
        body.add(createActions(), BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        add(ToolboxWorkbench.statusBar(statusLabel), BorderLayout.SOUTH);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 10);
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.weightx = 0;

        JScrollPane textScroll = new JScrollPane(contentArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(textScroll);
        addFormRow(form, constraints, MessageKeys.TOOLBOX_QR_TEXT, textScroll, 0);
        addFormRow(form, constraints, MessageKeys.TOOLBOX_QR_FOREGROUND,
                colorEditor(foregroundField), 1);
        addFormRow(form, constraints, MessageKeys.TOOLBOX_QR_BACKGROUND,
                colorEditor(backgroundField), 2);
        correctionBox.setPreferredSize(new Dimension(260, correctionBox.getPreferredSize().height));
        addFormRow(form, constraints, MessageKeys.TOOLBOX_QR_ERROR_CORRECTION, correctionBox, 3);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setTabSize(2);
        contentArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        contentArea.setBorder(new EmptyBorder(7, 8, 7, 8));
        ToolWindowSurfaceStyle.applyTextComponentInput(contentArea);
        contentArea.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_TEXT));
        colorFieldStyle(foregroundField);
        colorFieldStyle(backgroundField);
        return form;
    }

    private void addFormRow(JPanel form, GridBagConstraints base, String labelKey,
                            JComponent editor, int row) {
        GridBagConstraints labelConstraints = (GridBagConstraints) base.clone();
        labelConstraints.gridy = row;
        labelConstraints.gridx = 0;
        labelConstraints.weightx = 0;
        form.add(new JLabel(I18nUtil.getMessage(labelKey) + ":"), labelConstraints);
        GridBagConstraints editorConstraints = (GridBagConstraints) base.clone();
        editorConstraints.gridy = row;
        editorConstraints.gridx = 1;
        editorConstraints.weightx = 1;
        editorConstraints.insets = new Insets(5, 0, 5, 5);
        form.add(editor, editorConstraints);
    }

    private JPanel colorEditor(JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JButton choose = new JButton("…");
        choose.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_COLOR_PICKER));
        choose.getAccessibleContext().setAccessibleName(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_COLOR_PICKER));
        choose.addActionListener(event -> chooseColor(field));
        row.add(field, BorderLayout.CENTER);
        row.add(choose, BorderLayout.EAST);
        return row;
    }

    private void colorFieldStyle(JTextField field) {
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        field.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_INVALID_COLOR));
        ToolWindowSurfaceStyle.applyTextComponentInput(field);
    }

    private JPanel createPreview() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        JPanel preview = new JPanel(new BorderLayout());
        preview.setOpaque(false);
        preview.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        preview.setPreferredSize(new Dimension(PREVIEW_SIZE + 24, PREVIEW_SIZE + 24));
        previewLabel.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        preview.add(previewLabel, BorderLayout.CENTER);
        wrapper.add(preview);
        return wrapper;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        actions.setOpaque(false);
        downloadButton.addActionListener(event -> downloadQrCode());
        downloadButton.setEnabled(false);
        actions.add(downloadButton);
        return actions;
    }

    private void installListeners() {
        DocumentListener changes = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { scheduleRefresh(); }
            @Override public void removeUpdate(DocumentEvent event) { scheduleRefresh(); }
            @Override public void changedUpdate(DocumentEvent event) { scheduleRefresh(); }
        };
        contentArea.getDocument().addDocumentListener(changes);
        foregroundField.getDocument().addDocumentListener(changes);
        backgroundField.getDocument().addDocumentListener(changes);
        correctionBox.addActionListener(event -> scheduleRefresh());
    }

    private void scheduleRefresh() {
        refreshTimer.restart();
    }

    private void refreshQrCode() {
        String text = contentArea.getText().trim();
        if (text.isEmpty()) {
            currentImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_EMPTY));
            previewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            downloadButton.setEnabled(false);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_EMPTY), false);
            return;
        }
        try {
            Color foreground = QrCodeGenerator.parseColor(foregroundField.getText());
            Color background = QrCodeGenerator.parseColor(backgroundField.getText());
            CorrectionOption correction = (CorrectionOption) correctionBox.getSelectedItem();
            currentImage = QrCodeGenerator.generate(text, foreground, background,
                    correction == null ? ErrorCorrectionLevel.M : correction.level(), QR_IMAGE_SIZE);
            Image preview = currentImage.getScaledInstance(PREVIEW_SIZE, PREVIEW_SIZE, Image.SCALE_SMOOTH);
            previewLabel.setIcon(new ImageIcon(preview));
            previewLabel.setText("");
            downloadButton.setEnabled(true);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_READY), true);
        } catch (IllegalArgumentException exception) {
            currentImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText("");
            downloadButton.setEnabled(false);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_INVALID_COLOR), false);
        } catch (Exception exception) {
            currentImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText("");
            downloadButton.setEnabled(false);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_TOO_LONG), false);
        }
    }

    private void chooseColor(JTextField field) {
        Color initial;
        try {
            initial = QrCodeGenerator.parseColor(field.getText());
        } catch (IllegalArgumentException ignored) {
            initial = Color.BLACK;
        }
        Color selected = JColorChooser.showDialog(this,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_COLOR_PICKER), initial);
        if (selected == null) return;
        field.setText(QrCodeGenerator.toRgbaHex(new Color(selected.getRed(), selected.getGreen(),
                selected.getBlue(), initial.getAlpha())));
    }

    private void downloadQrCode() {
        if (currentImage == null) return;
        SystemFileChooser chooser = FileChooserUtil.createSaveFileChooser(
                "toolbox.qrcode.download", I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_DOWNLOAD));
        chooser.setSelectedFile(new File("qr-code.png"));
        if (chooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) return;
        File selected = chooser.getSelectedFile();
        File destination = selected.getName().toLowerCase(Locale.ROOT).endsWith(".png")
                ? selected : new File(selected.getParentFile(), selected.getName() + ".png");
        try {
            ImageIO.write(currentImage, "png", destination);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_SAVED) + ": " + destination.getName(), true);
        } catch (IOException exception) {
            log.warn("Could not save QR image to {}", destination, exception);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_QR_STATUS_SAVE_FAILED), false);
        }
    }

    private void updateStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setForeground(success ? ModernColors.getSuccess() : ModernColors.getError());
    }

    private record CorrectionOption(ErrorCorrectionLevel level, String messageKey) {
        @Override public String toString() { return I18nUtil.getMessage(messageKey); }
    }
}
