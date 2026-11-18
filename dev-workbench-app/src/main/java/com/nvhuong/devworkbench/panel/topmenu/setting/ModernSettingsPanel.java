package com.nvhuong.devworkbench.panel.topmenu.setting;

import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.setting.SettingsCheckBoxRow;
import com.nvhuong.devworkbench.common.component.setting.SettingsFieldRow;
import com.nvhuong.devworkbench.common.component.setting.SettingsInputStyle;
import com.nvhuong.devworkbench.common.component.setting.SettingsSectionPanel;
import com.nvhuong.devworkbench.common.component.setting.SettingsTextFieldValidator;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Lớp cơ sở của bảng cài đặt hiện đại
 * Cung cấp hiện đại hóa thống nhấtUIphong cách và trải nghiệm tương tác
 */
public abstract class ModernSettingsPanel extends JPanel {
    protected static final int COMPACT_FIELD_LABEL_MIN_WIDTH = 120;
    protected static final int FIELD_LABEL_TEXT_PADDING = 12;
    protected static final int ACTION_BUTTON_HEIGHT = 34;
    private static final int ACTION_BUTTON_MIN_WIDTH = 96;
    private static final int ACTION_BUTTON_HORIZONTAL_PADDING = 36;
    private static final int ACTION_BUTTON_MIN_HORIZONTAL_PADDING = 24;

    protected JButton saveBtn;
    @Getter
    protected JButton cancelBtn;
    protected JButton applyBtn;
    protected final Map<JTextField, SettingsTextFieldValidator> validators = new HashMap<>();
    protected final Map<JComponent, Object> originalValues = new HashMap<>();
    private JScrollPane contentScrollPane;

    // Quản lý trạng thái
    protected boolean hasUnsavedChanges = false;
    private JLabel unsavedChangesLabel;
    private boolean initialized;

    /**
     * Lấy màu nền của hộp thoại cài đặt phù hợp với chủ đề
     */
    protected Color getBackgroundColor() {
        return ModernColors.getDialogChromeBackgroundColor();
    }

    /**
     * Lấy màu nền của hộp nhập liệu phù hợp với chủ đề
     */
    protected Color getInputBackgroundColor() {
        return ModernColors.getInputBackgroundColor();
    }

    /**
     * Lấy màu văn bản chính phù hợp với chủ đề
     */
    protected Color getTextPrimaryColor() {
        return ModernColors.getTextPrimary();
    }

    /**
     * Lấy màu văn bản phụ thích ứng với chủ đề
     */
    protected Color getTextSecondaryColor() {
        return ModernColors.getTextSecondary();
    }

    /**
     * Nhận màu của rãnh thanh cuộn được điều chỉnh theo chủ đề
     */
    protected Color getScrollbarTrackColor() {
        return ModernColors.getScrollbarTrackColor();
    }

    /**
     * Nhận màu thanh trượt thanh cuộn thích ứng với chủ đề
     */
    protected Color getScrollbarThumbColor() {
        return ModernColors.getScrollbarThumbColor();
    }

    /**
     * Nhận màu di chuột của thanh cuộn được điều chỉnh theo chủ đề
     */
    protected Color getScrollbarThumbHoverColor() {
        return ModernColors.getScrollbarThumbHoverColor();
    }

    protected ModernSettingsPanel() {
    }

    private void initializePanel() {
        if (initialized) {
            return;
        }
        initUI();
        registerListeners();
        initialized = true;
    }

    @Override
    public void addNotify() {
        initializePanel();
        super.addNotify();
        resetScrollPositionToTop();
    }

    @Override
    public Dimension getPreferredSize() {
        initializePanel();
        return super.getPreferredSize();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(this);

        // Tạo vùng chứa chính
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(mainContainer);

        // Khu vực nội dung chính
        JPanel contentPanel = new ViewportWidthTrackingPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(new EmptyBorder(20, 24, 16, 24));

        // Các lớp con thực hiện nội dung cụ thể
        buildContent(contentPanel);

        // Bảng điều khiển cuộn
        contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        ToolWindowSurfaceStyle.applyDialogScrollPane(contentScrollPane);
        customizeScrollBar(contentScrollPane);

        // Lắp ráp thùng chứa chính
        mainContainer.add(contentScrollPane, BorderLayout.CENTER);

        // Thanh nút dưới cùng
        JPanel buttonBar = createModernButtonBar();

        add(mainContainer, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);

        // Add ESC key handling to trigger cancel button
        registerKeyboardAction(
                e -> {
                    if (cancelBtn != null) {
                        cancelBtn.doClick();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        resetScrollPositionToTop();
    }

    void resetScrollPositionToTop() {
        if (contentScrollPane == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            contentScrollPane.getViewport().setViewPosition(new Point(0, 0));
            contentScrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    /**
     * Các lớp con triển khai phương pháp này để xây dựng các cài đặt cụ thể
     */
    protected abstract void buildContent(JPanel contentPanel);

    /**
     * Các lớp con thực hiện phương thức này để đăng ký người nghe
     */
    protected abstract void registerListeners();

    /**
     * Tạo bảng vùng hiện đại
     */
    protected JPanel createModernSection(String title, String description) {
        return new SettingsSectionPanel(title, description);
    }

    /**
     * Tạo các hàng trường hiện đại (nhãn + hộp nhập liệu)
     */
    protected SettingsFieldRow createFieldRow(String labelText, String tooltip, JComponent inputComponent) {
        styleInputComponent(inputComponent);
        inputComponent.setPreferredSize(new Dimension(SettingsFieldRow.DEFAULT_FIELD_WIDTH, 34));
        inputComponent.setMaximumSize(new Dimension(SettingsFieldRow.DEFAULT_FIELD_WIDTH, 34));
        return new SettingsFieldRow(labelText, tooltip, inputComponent);
    }

    /**
     * Tạo các hàng trường có chiều rộng cột tùy chỉnh cho các biểu mẫu một phần để thắt chặt các cột đầu vào mà không cần cắt bớt nhãn ngôn ngữ hiện tại.
     */
    protected SettingsFieldRow createFieldRow(String labelText,
                                              String tooltip,
                                              JComponent inputComponent,
                                              int labelWidth,
                                              int fieldWidth) {
        styleInputComponent(inputComponent);
        return new SettingsFieldRow(labelText, tooltip, inputComponent, labelWidth, fieldWidth);
    }

    protected int calculateFieldLabelWidth(Collection<String> labelTexts) {
        return calculateFieldLabelWidth(
                labelTexts,
                COMPACT_FIELD_LABEL_MIN_WIDTH,
                SettingsFieldRow.DEFAULT_LABEL_WIDTH
        );
    }

    protected int calculateFieldLabelWidth(Collection<String> labelTexts, int minWidth, int maxWidth) {
        FontMetrics metrics = getFontMetrics(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        int maxTextWidth = 0;
        for (String labelText : labelTexts) {
            if (labelText != null) {
                maxTextWidth = Math.max(maxTextWidth, metrics.stringWidth(labelText));
            }
        }
        return Math.min(maxWidth, Math.max(minWidth, maxTextWidth + FIELD_LABEL_TEXT_PADDING));
    }

    /**
     * Tạo một hàng hộp kiểm hiện đại
     */
    protected JPanel createCheckBoxRow(JCheckBox checkBox, String tooltip) {
        return new SettingsCheckBoxRow(checkBox, tooltip);
    }


    /**
     * Các thành phần đầu vào được tạo kiểu
     */
    private void styleInputComponent(JComponent component) {
        SettingsInputStyle.apply(component);
    }

    /**
     * Tạo một thanh nút hiện đại
     */
    private JPanel createModernButtonBar() {
        JPanel buttonBar = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonBar);

        unsavedChangesLabel = new JLabel(
                I18nUtil.getMessage(MessageKeys.SETTINGS_UNSAVED_CHANGES_WARNING),
                IconUtil.createColored("icons/warning.svg", 14, 14, ModernColors.getWarning()),
                SwingConstants.LEADING
        );
        unsavedChangesLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        unsavedChangesLabel.setForeground(ModernColors.getTextSecondary());
        unsavedChangesLabel.setVisible(false);
        buttonBar.add(unsavedChangesLabel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(actionPanel);

        cancelBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL),
                false
        );

        applyBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_APPLY),
                false
        );
        applyBtn.setEnabled(false); // Ban đầu bị vô hiệu hóa

        saveBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE),
                true
        );

        actionPanel.add(cancelBtn);
        actionPanel.add(applyBtn);
        actionPanel.add(saveBtn);
        buttonBar.add(actionPanel, BorderLayout.EAST);

        return buttonBar;
    }

    /**
     * Tạo các nút hiện đại
     */
    protected JButton createModernButton(String text, boolean isPrimary) {
        return ModernButtonFactory.createButton(text, isPrimary);
    }

    /**
     * Các nút thao tác trên biểu mẫu: Độ rộng được tính dựa trên bản sao ngôn ngữ hiện tại để tránh văn bản bị cắt bớt sau khi chuyển đổi ngôn ngữ.
     */
    protected JButton createActionButton(String text, String tooltip, boolean primary) {
        JButton button = createModernButton(text, primary);
        button.setToolTipText(tooltip);
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        Dimension size = new Dimension(
                Math.max(ACTION_BUTTON_MIN_WIDTH, textWidth + ACTION_BUTTON_HORIZONTAL_PADDING),
                ACTION_BUTTON_HEIGHT
        );
        button.setPreferredSize(size);
        button.setMinimumSize(new Dimension(
                Math.min(size.width, textWidth + ACTION_BUTTON_MIN_HORIZONTAL_PADDING),
                ACTION_BUTTON_HEIGHT
        ));
        button.setMaximumSize(size);
        return button;
    }

    /**
     * Thêm khoảng cách
     */
    protected Component createVerticalSpace(int height) {
        return Box.createVerticalStrut(height);
    }

    protected void registerSaveShortcut(Runnable saveAction) {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("control S"), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAction.run();
            }
        });
    }

    /**
     * Thiết lập trình xác nhận
     */
    protected void setupValidator(JTextField field, Predicate<String> validator, String errorMessage) {
        validators.put(field, SettingsTextFieldValidator.install(field, validator, errorMessage));
    }

    /**
     * Xác thực tất cả các trường
     */
    protected boolean validateAllFields() {
        for (Map.Entry<JTextField, SettingsTextFieldValidator> entry : validators.entrySet()) {
            JTextField field = entry.getKey();
            if (!entry.getValue().validateNow()) {
                field.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * Phong cách thanh cuộn tùy chỉnh
     */
    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = getScrollbarThumbColor();
                this.thumbDarkShadowColor = getScrollbarThumbColor();
                this.thumbHighlightColor = getScrollbarThumbColor();
                this.thumbLightShadowColor = getScrollbarThumbColor();
                this.trackColor = getBackgroundColor();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createInvisibleButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createInvisibleButton();
            }

            private JButton createInvisibleButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? getScrollbarThumbHoverColor() : getScrollbarThumbColor());
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackgroundColor());
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });
    }

    // Phương pháp công cụ
    protected boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected boolean isPositiveInteger(String s) {
        return isInteger(s) && Integer.parseInt(s) >= 0;
    }

    // ==================== Các phương pháp quản lý nhà nước ====================

    /**
     * Ghi lại giá trị ban đầu của thành phần
     */
    protected void trackComponentValue(JComponent component) {
        if (component instanceof JTextField) {
            originalValues.put(component, ((JTextField) component).getText());
            ((JTextField) component).getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    checkForChanges();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    checkForChanges();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    checkForChanges();
                }
            });
        } else if (component instanceof JCheckBox) {
            originalValues.put(component, ((JCheckBox) component).isSelected());
            ((JCheckBox) component).addItemListener(e -> checkForChanges());
        } else if (component instanceof JComboBox) {
            originalValues.put(component, ((JComboBox<?>) component).getSelectedItem());
            ((JComboBox<?>) component).addItemListener(e -> checkForChanges());
        }
    }

    /**
     * Kiểm tra các thay đổi chưa được lưu
     */
    protected void checkForChanges() {
        boolean hasChanges = false;

        for (Map.Entry<JComponent, Object> entry : originalValues.entrySet()) {
            JComponent component = entry.getKey();
            Object originalValue = entry.getValue();

            if (component instanceof JTextField) {
                String currentValue = ((JTextField) component).getText();
                if (!currentValue.equals(originalValue)) {
                    hasChanges = true;
                    break;
                }
            } else if (component instanceof JCheckBox) {
                boolean currentValue = ((JCheckBox) component).isSelected();
                if (currentValue != (Boolean) originalValue) {
                    hasChanges = true;
                    break;
                }
            } else if (component instanceof JComboBox) {
                Object currentValue = ((JComboBox<?>) component).getSelectedItem();
                if (currentValue != null && !currentValue.equals(originalValue)) {
                    hasChanges = true;
                    break;
                }
            }
        }

        setHasUnsavedChanges(hasChanges);
    }

    /**
     * Đặt trạng thái thay đổi chưa được lưu
     */
    protected void setHasUnsavedChanges(boolean hasChanges) {
        this.hasUnsavedChanges = hasChanges;
        if (unsavedChangesLabel != null) {
            unsavedChangesLabel.setVisible(hasChanges);
            unsavedChangesLabel.revalidate();
            unsavedChangesLabel.repaint();
        }
        if (applyBtn != null) {
            applyBtn.setEnabled(hasChanges);
        }

        // Cập nhật tiêu đề cửa sổ chính (nếu đó là hộp thoại)
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog && hasChanges) {
            JDialog dialog = (JDialog) window;
            String title = dialog.getTitle();
            if (!title.startsWith("* ")) {
                dialog.setTitle("* " + title);
            }
        } else if (window instanceof JDialog && !hasChanges) {
            JDialog dialog = (JDialog) window;
            String title = dialog.getTitle();
            if (title.startsWith("* ")) {
                dialog.setTitle(title.substring(2));
            }
        }
    }

    /**
     * Hủy bỏ các thay đổi
     */
    protected void discardChanges() {
        for (Map.Entry<JComponent, Object> entry : originalValues.entrySet()) {
            JComponent component = entry.getKey();
            Object originalValue = entry.getValue();

            if (component instanceof JTextField) {
                ((JTextField) component).setText((String) originalValue);
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).setSelected((Boolean) originalValue);
            } else if (component instanceof JComboBox) {
                ((JComboBox) component).setSelectedItem(originalValue);
            }
        }
        setHasUnsavedChanges(false);
    }

    /**
     * Xác nhận để loại bỏ thay đổi
     */
    protected boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.SETTINGS_CONFIRM_DISCARD_MESSAGE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_CONFIRM_DISCARD_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    private static final class ViewportWidthTrackingPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            if (getParent() instanceof JViewport viewport) {
                return getPreferredSize().height < viewport.getHeight();
            }
            return false;
        }
    }
}
