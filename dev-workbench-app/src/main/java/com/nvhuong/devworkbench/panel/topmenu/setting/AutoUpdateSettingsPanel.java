package com.nvhuong.devworkbench.panel.topmenu.setting;

import com.nvhuong.devworkbench.platform.update.UpdateCenter;
import com.nvhuong.devworkbench.platform.update.model.UpdatePolicy;
import com.nvhuong.devworkbench.platform.update.model.UpdateTarget;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.service.update.AppUpdateCenter;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tự động cập nhật bảng cài đặt
 */
public class AutoUpdateSettingsPanel extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JCheckBox appUpdateCheckBox;
    private JComboBox<String> appUpdateFrequencyComboBox;
    private JCheckBox pluginUpdateCheckBox;
    private JComboBox<String> pluginUpdateFrequencyComboBox;
    private final UpdateCenter updateCenter = AppUpdateCenter.get();

    @Override
    protected void buildContent(JPanel contentPanel) {
        // Tự động cập nhật khu vực cài đặt
        JPanel autoUpdateSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_DESCRIPTION)
        );

        // Công tắc cập nhật chương trình chính
        UpdatePolicy appPolicy = updateCenter.policy(UpdateTarget.APP);
        appUpdateCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_CHECKBOX),
                appPolicy.enabled()
        );
        JPanel appUpdateRow = createCheckBoxRow(
                appUpdateCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_TOOLTIP)
        );
        autoUpdateSection.add(appUpdateRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // Kiểm tra tần số
        String[] frequencyOptions = createFrequencyOptions();
        appUpdateFrequencyComboBox = new JComboBox<>(frequencyOptions);

        // Chọn tùy chọn tương ứng dựa trên cài đặt hiện tại
        selectFrequency(appUpdateFrequencyComboBox, appPolicy.frequency().getCode());

        JPanel frequencyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_TOOLTIP),
                appUpdateFrequencyComboBox
        );
        autoUpdateSection.add(frequencyRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // Lần kiểm tra cuối cùng
        JLabel lastCheckTimeLabel = new JLabel(formatCheckTime(
                updateCenter.state(UpdateTarget.APP).lastCheckTimeMillis()
        ));
        JPanel lastCheckRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME_TOOLTIP),
                lastCheckTimeLabel
        );
        autoUpdateSection.add(lastCheckRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // Công tắc cập nhật plug-in
        UpdatePolicy pluginPolicy = updateCenter.policy(UpdateTarget.PLUGIN);
        pluginUpdateCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_ENABLED_CHECKBOX),
                pluginPolicy.enabled()
        );
        JPanel pluginUpdateRow = createCheckBoxRow(
                pluginUpdateCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_ENABLED_TOOLTIP)
        );
        autoUpdateSection.add(pluginUpdateRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        pluginUpdateFrequencyComboBox = new JComboBox<>(frequencyOptions);
        selectFrequency(pluginUpdateFrequencyComboBox, pluginPolicy.frequency().getCode());
        JPanel pluginFrequencyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_FREQUENCY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_FREQUENCY_TOOLTIP),
                pluginUpdateFrequencyComboBox
        );
        autoUpdateSection.add(pluginFrequencyRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        JLabel pluginLastCheckTimeLabel = new JLabel(formatCheckTime(
                updateCenter.state(UpdateTarget.PLUGIN).lastCheckTimeMillis()
        ));
        JPanel pluginLastCheckRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_LAST_CHECK_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_LAST_CHECK_TIME_TOOLTIP),
                pluginLastCheckTimeLabel
        );
        autoUpdateSection.add(pluginLastCheckRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));


        contentPanel.add(autoUpdateSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        // Theo dõi các giá trị ban đầu cho tất cả các thành phần
        trackComponentValue(appUpdateCheckBox);
        trackComponentValue(appUpdateFrequencyComboBox);
        trackComponentValue(pluginUpdateCheckBox);
        trackComponentValue(pluginUpdateFrequencyComboBox);
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        });
        registerSaveShortcut(() -> saveSettings(false));
    }

    private void saveSettings(boolean closeAfterSave) {
        try {
            // Lưu cài đặt cập nhật tự động
            SettingManager.setAutoUpdateCheckEnabled(appUpdateCheckBox.isSelected());

            // Lưu tần suất kiểm tra
            SettingManager.setAutoUpdateCheckFrequency(selectedFrequency(appUpdateFrequencyComboBox));
            SettingManager.setPluginUpdateCheckEnabled(pluginUpdateCheckBox.isSelected());
            SettingManager.setPluginUpdateCheckFrequency(selectedFrequency(pluginUpdateFrequencyComboBox));


            // Truy xuất giá trị hiện tại
            originalValues.clear();
            trackComponentValue(appUpdateCheckBox);
            trackComponentValue(appUpdateFrequencyComboBox);
            trackComponentValue(pluginUpdateCheckBox);
            trackComponentValue(pluginUpdateFrequencyComboBox);
            setHasUnsavedChanges(false);

            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            // Xác định có nên đóng hộp thoại hay không dựa trên các tham số
            if (closeAfterSave) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }

    private String[] createFrequencyOptions() {
        return new String[]{
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_STARTUP),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_DAILY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_WEEKLY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_MONTHLY)
        };
    }

    private void selectFrequency(JComboBox<String> comboBox, String frequency) {
        switch (frequency) {
            case "startup" -> comboBox.setSelectedIndex(0);
            case "weekly" -> comboBox.setSelectedIndex(2);
            case "monthly" -> comboBox.setSelectedIndex(3);
            default -> comboBox.setSelectedIndex(1);
        }
    }

    private String selectedFrequency(JComboBox<String> comboBox) {
        return switch (comboBox.getSelectedIndex()) {
            case 0 -> "startup";
            case 2 -> "weekly";
            case 3 -> "monthly";
            default -> "daily";
        };
    }

    private String formatCheckTime(long timestamp) {
        return timestamp > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp))
                : I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_NEVER_CHECKED);
    }
}
