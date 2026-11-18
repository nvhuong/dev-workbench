package com.nvhuong.devworkbench.panel.topmenu.setting;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.panel.topmenu.TopMenuBar;
import com.nvhuong.devworkbench.service.setting.ShortcutManager;
import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng cài đặt phím tắt
 */
public class ShortcutSettingsPanel extends JPanel {
    private JTable shortcutTable;
    private ShortcutTableModel tableModel;

    public ShortcutSettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        ToolWindowSurfaceStyle.applyDialogSurface(this);

        // Bảng tiêu đề
        JPanel titlePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(titlePanel);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_SETTINGS_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 3));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        // Đã thêm lời nhắc chỉnh sửa nhấp đúp
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_DOUBLE_CLICK_HINT));
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        hintLabel.setForeground(ModernColors.getTextSecondary());
        titlePanel.add(hintLabel, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH);

        // Tạo một bảng
        tableModel = new ShortcutTableModel();
        shortcutTable = new JTable(tableModel);
        ToolWindowSurfaceStyle.applyTableCard(shortcutTable);
        shortcutTable.setRowHeight(32);
        shortcutTable.setFocusable(false);
        shortcutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Đặt chiều rộng cột (chỉ2cột)
        shortcutTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        shortcutTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        // Thiết lập trình kết xuất
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        shortcutTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // Đã thêm chức năng chỉnh sửa nhấp đúp
        shortcutTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editShortcut();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(shortcutTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, shortcutTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bảng nút dưới cùng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(buttonPanel);

        JButton editButton = new JButton(I18nUtil.getMessage(MessageKeys.SHORTCUT_EDIT));
        editButton.addActionListener(e -> editShortcut());

        JButton resetButton = new JButton(I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET));
        resetButton.addActionListener(e -> resetToDefaults());

        buttonPanel.add(editButton);
        buttonPanel.add(resetButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Chỉnh sửa phím tắt
     */
    private void editShortcut() {
        int selectedRow = shortcutTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_SELECT_FIRST),
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ShortcutItem item = tableModel.getShortcutItem(selectedRow);
        ShortcutEditDialog dialog = new ShortcutEditDialog(
                SwingUtilities.getWindowAncestor(this), item);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            // Áp dụng thay đổi ngay lập tức
            applySettings();
        }
    }

    /**
     * Đặt lại về giá trị mặc định
     */
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this,
                I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET_CONFIRM),
                I18nUtil.getMessage(MessageKeys.TIP),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            ShortcutManager.resetToDefaults();
            tableModel.refresh();
            // Áp dụng thay đổi ngay lập tức
            applySettings();
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET_SUCCESS),
                    I18nUtil.getMessage(MessageKeys.SUCCESS),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Áp dụng cài đặt
     */
    public void applySettings() {
        ShortcutManager.save();
        // Thông báo các phím tắt cần phải đăng ký lại
        notifyShortcutChanged();
    }

    /**
     * Phím tắt thông báo đã được thay đổi và cần phải đăng ký lại
     */
    private void notifyShortcutChanged() {
        // Đăng ký lại RequestEditorPanel phím tắt
        UiSingletonFactory.getInstance(
                RequestEditorPanel.class).reloadShortcuts();

        // Tải lại thanh menu (bao gồm phím tắt và  Git tất cả các thành phần bao gồm cả thanh công cụ)
        UiSingletonFactory.getInstance(
                TopMenuBar.class).refresh();
    }

    /**
     * Mô hình bảng phím tắt
     */
    private static class ShortcutTableModel extends AbstractTableModel {
        private final List<ShortcutItem> shortcuts;
        private final String[] columnNames;

        public ShortcutTableModel() {
            this.shortcuts = new ArrayList<>();
            this.columnNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_ACTION),
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_KEY)
            };
            loadShortcuts();
        }

        private void loadShortcuts() {
            shortcuts.clear();
            shortcuts.add(new ShortcutItem(ShortcutManager.SEND_REQUEST,
                    I18nUtil.getMessage(MessageKeys.BUTTON_SEND)));
            shortcuts.add(new ShortcutItem(ShortcutManager.NEW_REQUEST,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST)));
            shortcuts.add(new ShortcutItem(ShortcutManager.SAVE_REQUEST,
                    I18nUtil.getMessage(MessageKeys.SAVE_REQUEST)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_CURRENT_TAB,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_CURRENT)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_OTHER_TABS,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_OTHERS)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_ALL_TABS,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_ALL)));
            shortcuts.add(new ShortcutItem(ShortcutManager.EXIT_APP,
                    I18nUtil.getMessage(MessageKeys.EXIT_APP)));
        }

        public void refresh() {
            loadShortcuts();
            fireTableDataChanged();
        }

        public ShortcutItem getShortcutItem(int row) {
            return shortcuts.get(row);
        }

        @Override
        public int getRowCount() {
            return shortcuts.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ShortcutItem item = shortcuts.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.getActionName();
                case 1 -> shortcutDisplayText(item.getId());
                default -> "";
            };
        }
    }

    private static String shortcutDisplayText(String id) {
        return ShortcutManager.isAssigned(id)
                ? ShortcutManager.getShortcutText(id)
                : I18nUtil.getMessage(MessageKeys.SHORTCUT_UNASSIGNED);
    }

    /**
     * Các mục phím tắt
     */
    static class ShortcutItem {
        private final String id;
        private final String actionName;

        public ShortcutItem(String id, String actionName) {
            this.id = id;
            this.actionName = actionName;
        }

        public String getId() {
            return id;
        }

        public String getActionName() {
            return actionName;
        }
    }

    /**
     * Hộp thoại chỉnh sửa phím tắt
     */
    private static class ShortcutEditDialog extends JDialog {
        private final ShortcutItem item;
        @Getter
        private boolean confirmed = false;
        private int newKeyCode = -1;
        private int newModifiers = 0;
        private final JLabel currentKeyLabel;
        private final JTextField keyInputField;

        public ShortcutEditDialog(Window owner, ShortcutItem item) {
            super(owner, I18nUtil.getMessage(MessageKeys.SHORTCUT_EDIT_TITLE), ModalityType.APPLICATION_MODAL);
            this.item = item;

            ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));
            ToolWindowSurfaceStyle.applyDialogSurface((JPanel) getContentPane());

            // Bảng điều khiển trung tâm
            JPanel centerPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            ToolWindowSurfaceStyle.applyDialogSurface(centerPanel);

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_ACTION) + ":"));
            centerPanel.add(new JLabel(item.getActionName()));

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_CURRENT) + ":"));
            currentKeyLabel = new JLabel(shortcutDisplayText(item.getId()));
            centerPanel.add(currentKeyLabel);

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_NEW) + ":"));
            keyInputField = new JTextField();
            keyInputField.setEditable(false);
            keyInputField.setFocusable(true);
            keyInputField.setText(I18nUtil.getMessage(MessageKeys.SHORTCUT_PRESS_KEY));
            keyInputField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // Không được xử lý
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    captureKey(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    // Không được xử lý
                }
            });
            centerPanel.add(keyInputField);

            add(centerPanel, BorderLayout.CENTER);

            // Bảng nút
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);

            JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE));
            okButton.addActionListener(e -> confirm());

            JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL));
            cancelButton.addActionListener(e -> dispose());

            JButton clearButton = new JButton(CommonI18n.get(CommonMessageKeys.BUTTON_CLEAR));
            clearButton.setToolTipText(I18nUtil.getMessage(MessageKeys.SHORTCUT_CLEAR_TOOLTIP));
            clearButton.setEnabled(ShortcutManager.isAssigned(item.getId()));
            clearButton.addActionListener(e -> clearShortcut());

            buttonPanel.add(clearButton);
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            add(buttonPanel, BorderLayout.SOUTH);

            setSize(450, 200);
            setLocationRelativeTo(owner);
        }

        private void captureKey(KeyEvent e) {
            int keyCode = e.getKeyCode();
            int modifiers = e.getModifiersEx();

            // Các phím bổ trợ thuần túy bị bỏ qua
            if (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL ||
                    keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META) {
                return;
            }

            // Kiểm tra xung đột
            String conflict = ShortcutManager.checkConflict(item.getId(), keyCode, modifiers);
            if (conflict != null) {
                keyInputField.setText(I18nUtil.getMessage(MessageKeys.SHORTCUT_CONFLICT) + ": " + conflict);
                return;
            }

            this.newKeyCode = keyCode;
            this.newModifiers = modifiers;

            // Hiển thị các phím tắt mới
            ShortcutManager.ShortcutConfig tempConfig = new ShortcutManager.ShortcutConfig(
                    "", "", keyCode, modifiers, "", "");
            keyInputField.setText(tempConfig.getDisplayText());
        }

        private void confirm() {
            if (newKeyCode == -1) {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.SHORTCUT_NOT_SET),
                        I18nUtil.getMessage(MessageKeys.TIP),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ShortcutManager.setShortcut(item.getId(), newKeyCode, newModifiers);
            confirmed = true;
            dispose();
        }

        private void clearShortcut() {
            ShortcutManager.clearShortcut(item.getId());
            confirmed = true;
            dispose();
        }

    }
}
