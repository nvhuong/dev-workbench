package com.nvhuong.devworkbench.panel.performance.threadgroup;

import com.nvhuong.devworkbench.common.component.DevWorkbenchComboBox;
import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.SegmentedButtonBar;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreadGroupPropertyPanel extends JPanel {
    private static final int CONFIG_PANEL_WIDTH = 460;
    private static final int PREVIEW_PANEL_HEIGHT = 200;
    private static final int FORM_CONTROL_HEIGHT = 28;
    private static final int SPINNER_WIDTH = 80;
    private static final int LABEL_FIELD_GAP = 8;
    private static final int FIELD_PAIR_GAP = 30;
    private static final int FORM_ROW_GAP = 8;
    private static final int CONFIG_PREVIEW_GAP = 36;

    private final DevWorkbenchComboBox<ThreadGroupData.ThreadMode> modeComboBox;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private PerformanceTreeNode currentNode;
    private boolean updatingPreview;

    // Thành phần bảng điều khiển chế độ cố định
    private final JPanel fixedPanel;
    private final DevWorkbenchJSpinner fixedNumThreadsSpinner;
    private final DevWorkbenchJSpinner fixedLoopsSpinner;
    private final SegmentedButtonBar<Boolean> executionModeBar;
    private final JToggleButton useLoopCountButton;
    private final JToggleButton useTimeCheckBox;
    private final DevWorkbenchJSpinner durationSpinner;
    private final JLabel maxInFlightWaitLabel;
    private final DevWorkbenchJSpinner maxInFlightWaitSpinner;

    // Thành phần bảng chế độ tăng dần
    private final JPanel rampUpPanel;
    private final DevWorkbenchJSpinner rampUpStartThreadsSpinner;
    private final DevWorkbenchJSpinner rampUpEndThreadsSpinner;
    private final DevWorkbenchJSpinner rampUpTimeSpinner;
    private final DevWorkbenchJSpinner rampUpDurationSpinner;

    // Thành phần bảng điều khiển có gai
    private final JPanel spikePanel;
    private final DevWorkbenchJSpinner spikeMinThreadsSpinner;
    private final DevWorkbenchJSpinner spikeMaxThreadsSpinner;
    private final DevWorkbenchJSpinner spikeRampUpTimeSpinner;
    private final DevWorkbenchJSpinner spikeHoldTimeSpinner;
    private final DevWorkbenchJSpinner spikeRampDownTimeSpinner;
    private final DevWorkbenchJSpinner spikeDurationSpinner;

    // Thành phần bảng điều khiển chế độ bậc thang
    private final JPanel stairsPanel;
    private final DevWorkbenchJSpinner stairsStartThreadsSpinner;
    private final DevWorkbenchJSpinner stairsEndThreadsSpinner;
    private final DevWorkbenchJSpinner stairsStepSpinner;
    private final DevWorkbenchJSpinner stairsHoldTimeSpinner;
    private final DevWorkbenchJSpinner stairsDurationSpinner;

    // Tải chế độ xem trước liên quan
    private final ThreadLoadPreviewPanel previewPanel;

    public ThreadGroupPropertyPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        modeComboBox = new DevWorkbenchComboBox<>(
                ThreadGroupData.ThreadMode.values(),
                DevWorkbenchComboBox.WidthMode.FIXED_MAX
        );
        modeComboBox.setRenderer(new ThreadModeRenderer());

        // Phần giữa: bảng cấu hình bên trái, xem trước bên phải
        JPanel mainPanel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[" + CONFIG_PANEL_WIDTH + "!,left]" + CONFIG_PREVIEW_GAP + "[420::,grow,fill]",
                "[]"
        ));
        mainPanel.setOpaque(false);

        // Bố cục thẻ ở giữa được sử dụng để chuyển đổi bảng cấu hình của các chế độ khác nhau
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        // Khởi tạo tất cả các điều khiển và bảng điều khiển
        // 1. Bảng chế độ cố định
        fixedPanel = new JPanel(createFixedModeLayout());
        fixedPanel.setOpaque(false);
        fixedNumThreadsSpinner = standardThreadCountSpinner(1);
        fixedLoopsSpinner = standardIntSpinner(1, 1, null, 1);
        executionModeBar = new SegmentedButtonBar<>(FlowLayout.CENTER, SegmentedButtonBar.Size.COMPACT);
        useLoopCountButton = executionModeBar.addOption(
                Boolean.FALSE,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_COUNT),
                true
        );
        useTimeCheckBox = executionModeBar.addOption(
                Boolean.TRUE,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME),
                false
        );
        durationSpinner = standardIntSpinner(60, 1, null, 10);
        maxInFlightWaitLabel = formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_MAX_IN_FLIGHT_WAIT));
        maxInFlightWaitSpinner = standardIntSpinner(
                ThreadGroupData.DEFAULT_MAX_IN_FLIGHT_WAIT_SECONDS,
                1,
                null,
                1
        );
        String maxInFlightWaitTooltip = I18nUtil.getMessage(MessageKeys.THREADGROUP_MAX_IN_FLIGHT_WAIT_TOOLTIP);
        maxInFlightWaitLabel.setToolTipText(maxInFlightWaitTooltip);
        maxInFlightWaitSpinner.setToolTipText(maxInFlightWaitTooltip);

        // 2. Bảng chế độ tăng dần
        rampUpPanel = new JPanel(createValuePairLayout());
        rampUpPanel.setOpaque(false);
        rampUpStartThreadsSpinner = standardThreadCountSpinner(1);
        rampUpEndThreadsSpinner = standardThreadCountSpinner(10);
        rampUpTimeSpinner = standardIntSpinner(30, 1, null, 5);
        rampUpDurationSpinner = standardIntSpinner(120, 1, null, 10);

        // 3. Bảng điều khiển chế độ tăng đột biến
        spikePanel = new JPanel(createValuePairLayout());
        spikePanel.setOpaque(false);
        spikeMinThreadsSpinner = standardThreadCountSpinner(1);
        spikeMaxThreadsSpinner = standardThreadCountSpinner(20);
        spikeRampUpTimeSpinner = standardIntSpinner(10, 1, null, 1);
        spikeHoldTimeSpinner = standardIntSpinner(5, 0, null, 1);
        spikeRampDownTimeSpinner = standardIntSpinner(10, 1, null, 1);
        spikeDurationSpinner = standardIntSpinner(120, 1, null, 10);

        // 4. Bảng chế độ bậc thang
        stairsPanel = new JPanel(createValuePairLayout());
        stairsPanel.setOpaque(false);
        stairsStartThreadsSpinner = standardThreadCountSpinner(1);
        stairsEndThreadsSpinner = standardThreadCountSpinner(20);
        stairsStepSpinner = standardIntSpinner(5, 1, null, 1);
        stairsHoldTimeSpinner = standardIntSpinner(10, 1, null, 1);
        stairsDurationSpinner = standardIntSpinner(240, 1, null, 10);

        // Đặt bố cục của từng bảng
        setupFixedPanel();
        setupRampUpPanel();
        setupSpikePanel();
        setupStairsPanel();

        // Thêm tất cả các bảng vào bố cục thẻ
        cardPanel.add(fixedPanel, ThreadGroupData.ThreadMode.FIXED.name());
        cardPanel.add(rampUpPanel, ThreadGroupData.ThreadMode.RAMP_UP.name());
        cardPanel.add(spikePanel, ThreadGroupData.ThreadMode.SPIKE.name());
        cardPanel.add(stairsPanel, ThreadGroupData.ThreadMode.STAIRS.name());

        // Bảng chế độ cố định được hiển thị theo mặc định
        showThreadMode(ThreadGroupData.ThreadMode.FIXED);

        // Người nghe chuyển đổi chế độ
        modeComboBox.addActionListener(e -> {
            ThreadGroupData.ThreadMode selectedMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
            if (selectedMode != null) {
                showThreadMode(selectedMode);
                updateMaxInFlightWaitState();
                updatePreview();
            }
        });

        executionModeBar.setSelectionListener(useTime -> {
            updateFixedExecutionModeState();
            updateMaxInFlightWaitState();
        });
        updateFixedExecutionModeState();
        updateMaxInFlightWaitState();

        // Xem trước khu vực biểu đồ
        previewPanel = new ThreadLoadPreviewPanel();
        previewPanel.setPreferredSize(new Dimension(560, PREVIEW_PANEL_HEIGHT));
        previewPanel.setMinimumSize(new Dimension(380, PREVIEW_PANEL_HEIGHT));  // Đặt kích thước tối thiểu để tránh nén quá mức
        previewPanel.setOpaque(false);
        previewPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Khu vực cấu hình bên trái được bao bọc trong một bảng điều khiển để kiểm soát bố cục
        JPanel configPanel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[left]",
                "[]10[]8[]"
        ));
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        JPanel modeRow = new JPanel(new MigLayout(
                "insets 0, novisualpadding, gap 0",
                "[right,pref!]8[pref!,left]",
                "[]"
        ));
        modeRow.setOpaque(false);
        modeRow.add(formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_MODE_LABEL)), "aligny center");
        modeRow.add(modeComboBox, "w pref!, h " + FORM_CONTROL_HEIGHT + "!");
        configPanel.add(modeRow, "left, wrap");
        configPanel.add(cardPanel, "w " + CONFIG_PANEL_WIDTH + "!, h pref!, growx, wrap");
        int fixedRightFieldWidth = Math.max(SPINNER_WIDTH, executionModeBar.getPreferredSize().width);
        JPanel maxInFlightWaitRow = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[grow,fill][right,pref!]" + LABEL_FIELD_GAP + "[" + fixedRightFieldWidth + "!,left]",
                "[]"
        ));
        maxInFlightWaitRow.setOpaque(false);
        maxInFlightWaitRow.add(maxInFlightWaitLabel, "skip 1, aligny center");
        maxInFlightWaitRow.add(maxInFlightWaitSpinner, spinnerConstraints());
        maxInFlightWaitRow.setPreferredSize(new Dimension(
                fixedPanel.getPreferredSize().width,
                FORM_CONTROL_HEIGHT
        ));
        configPanel.add(maxInFlightWaitRow, "left");
        JPanel previewSection = new JPanel(new MigLayout(
                "insets 0, fill, novisualpadding, gap 0",
                "[grow,fill]",
                "[" + PREVIEW_PANEL_HEIGHT + "!,fill]"
        ));
        previewSection.setOpaque(false);
        previewSection.add(previewPanel, "grow");

        // Thêm vào bảng điều khiển chính
        mainPanel.add(configPanel, "top, growx");
        mainPanel.add(previewSection, "top, growx, pushx");

        // bố cục tổng thể
        add(mainPanel, BorderLayout.NORTH);

        // Thêm trình xử lý thay đổi vào tất cả các thành phần đầu vào và làm mới bản xem trước
        addPreviewUpdateListeners();
    }

    private static DevWorkbenchJSpinner threadCountSpinner(int value) {
        return DevWorkbenchJSpinner.intSpinner(value, ThreadGroupData.MIN_THREADS, null, 1);
    }

    private static DevWorkbenchJSpinner standardThreadCountSpinner(int value) {
        return standardSizedSpinner(threadCountSpinner(value));
    }

    private static DevWorkbenchJSpinner standardIntSpinner(int value, int minimum, Integer maximum, int stepSize) {
        return standardSizedSpinner(DevWorkbenchJSpinner.intSpinner(value, minimum, maximum, stepSize));
    }

    private static DevWorkbenchJSpinner standardSizedSpinner(DevWorkbenchJSpinner spinner) {
        spinner.setPreferredSize(new Dimension(SPINNER_WIDTH, FORM_CONTROL_HEIGHT));
        return spinner;
    }

    private static MigLayout createValuePairLayout() {
        return createThreadGroupFormLayout(3, "[" + SPINNER_WIDTH + "!]");
    }

    private static MigLayout createFixedModeLayout() {
        return createThreadGroupFormLayout(2, "[pref!]");
    }

    private static MigLayout createThreadGroupFormLayout(int rowCount, String rightFieldColumn) {
        return new MigLayout(
                formLayoutConstraints(),
                formColumns(rightFieldColumn),
                formRows(rowCount)
        );
    }

    private static String formLayoutConstraints() {
        return "insets 0, novisualpadding, gap 0";
    }

    private static String formColumns(String rightFieldColumn) {
        return "[right,pref!]" + LABEL_FIELD_GAP + "["
                + SPINNER_WIDTH + "!]"
                + FIELD_PAIR_GAP
                + "[right,pref!]" + LABEL_FIELD_GAP + rightFieldColumn;
    }

    private static String formRows(int rowCount) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) {
                rows.append(FORM_ROW_GAP);
            }
            rows.append("[]");
        }
        return rows.toString();
    }

    private static JLabel formLabel(String text) {
        return new JLabel(text, SwingConstants.RIGHT);
    }

    private static String spinnerConstraints() {
        return "w " + SPINNER_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!";
    }

    // Đặt bảng chế độ cố định
    private void setupFixedPanel() {
        fixedPanel.add(formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_USERS)));
        fixedPanel.add(fixedNumThreadsSpinner, spinnerConstraints());
        fixedPanel.add(formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_MODE)));
        fixedPanel.add(createExecutionModePanel(), "w pref!, h pref!, wrap");

        fixedPanel.add(formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_LOOPS)));
        fixedPanel.add(fixedLoopsSpinner, spinnerConstraints());
        fixedPanel.add(formLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_DURATION)));
        fixedPanel.add(durationSpinner, spinnerConstraints());
    }

    // Đặt bảng chế độ tăng dần
    private void setupRampUpPanel() {
        addValuePairRow(
                rampUpPanel,
                MessageKeys.THREADGROUP_RAMPUP_START_USERS,
                rampUpStartThreadsSpinner,
                MessageKeys.THREADGROUP_RAMPUP_END_USERS,
                rampUpEndThreadsSpinner
        );
        addValuePairRow(
                rampUpPanel,
                MessageKeys.THREADGROUP_RAMPUP_RAMP_TIME,
                rampUpTimeSpinner,
                MessageKeys.THREADGROUP_RAMPUP_TEST_DURATION,
                rampUpDurationSpinner
        );
    }

    // Thiết lập bảng chế độ tăng đột biến
    private void setupSpikePanel() {
        addValuePairRow(
                spikePanel,
                MessageKeys.THREADGROUP_SPIKE_MIN_USERS,
                spikeMinThreadsSpinner,
                MessageKeys.THREADGROUP_SPIKE_MAX_USERS,
                spikeMaxThreadsSpinner
        );
        addValuePairRow(
                spikePanel,
                MessageKeys.THREADGROUP_SPIKE_RAMP_UP_TIME,
                spikeRampUpTimeSpinner,
                MessageKeys.THREADGROUP_SPIKE_HOLD_TIME,
                spikeHoldTimeSpinner
        );
        addValuePairRow(
                spikePanel,
                MessageKeys.THREADGROUP_SPIKE_RAMP_DOWN_TIME,
                spikeRampDownTimeSpinner,
                MessageKeys.THREADGROUP_SPIKE_TEST_DURATION,
                spikeDurationSpinner
        );
    }

    // Thiết lập bảng chế độ bậc thang
    private void setupStairsPanel() {
        addValuePairRow(
                stairsPanel,
                MessageKeys.THREADGROUP_STAIRS_START_USERS,
                stairsStartThreadsSpinner,
                MessageKeys.THREADGROUP_STAIRS_END_USERS,
                stairsEndThreadsSpinner
        );
        addValuePairRow(
                stairsPanel,
                MessageKeys.THREADGROUP_STAIRS_STEP_SIZE,
                stairsStepSpinner,
                MessageKeys.THREADGROUP_STAIRS_HOLD_TIME,
                stairsHoldTimeSpinner
        );
        addValuePairRow(
                stairsPanel,
                MessageKeys.THREADGROUP_STAIRS_TEST_DURATION,
                stairsDurationSpinner,
                null,
                null
        );
    }

    private void addValuePairRow(JPanel panel,
                                 String leftLabelKey,
                                 JComponent leftField,
                                 String rightLabelKey,
                                 JComponent rightField) {
        panel.add(formLabel(I18nUtil.getMessage(leftLabelKey)));
        panel.add(leftField, spinnerConstraints());
        if (rightLabelKey != null && rightField != null) {
            panel.add(formLabel(I18nUtil.getMessage(rightLabelKey)));
            panel.add(rightField, spinnerConstraints() + ", wrap");
        } else {
            panel.add(Box.createHorizontalStrut(0), "span 2, wrap");
        }
    }

    private void addPreviewUpdateListeners() {
        // Giám sát thay đổi tham số chế độ cố định
        fixedNumThreadsSpinner.addChangeListener(e -> updatePreview());
        fixedLoopsSpinner.addChangeListener(e -> updatePreview());
        useLoopCountButton.addActionListener(e -> updatePreview());
        useTimeCheckBox.addActionListener(e -> updatePreview());
        durationSpinner.addChangeListener(e -> updatePreview());

        // Giám sát thay đổi tham số chế độ tăng dần
        rampUpStartThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpEndThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpTimeSpinner.addChangeListener(e -> updatePreview());
        rampUpDurationSpinner.addChangeListener(e -> updatePreview());

        // Giám sát thay đổi tham số chế độ tăng đột biến
        spikeMinThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeRampUpTimeSpinner.addChangeListener(e -> updatePreview());
        spikeHoldTimeSpinner.addChangeListener(e -> updatePreview());
        spikeRampDownTimeSpinner.addChangeListener(e -> updatePreview());
        spikeDurationSpinner.addChangeListener(e -> updatePreview());

        // Giám sát thay đổi tham số chế độ thang
        stairsStartThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsEndThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsStepSpinner.addChangeListener(e -> updatePreview());
        stairsHoldTimeSpinner.addChangeListener(e -> updatePreview());
        stairsDurationSpinner.addChangeListener(e -> updatePreview());
    }

    private void updatePreview() {
        if (updatingPreview) {
            return;
        }
        updatingPreview = true;
        try {
            updatePreviewSnapshot();
        } finally {
            updatingPreview = false;
        }
    }

    private void updatePreviewSnapshot() {
        ThreadGroupData.ThreadMode mode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
        if (mode == null) return;

        ThreadLoadPreviewData previewData = new ThreadLoadPreviewData();
        previewData.mode = mode;

        switch (mode) {
            case FIXED:
                previewData.fixedThreads = fixedNumThreadsSpinner.getCommittedIntValue();
                previewData.useTime = useTimeCheckBox.isSelected();
                previewData.duration = durationSpinner.getCommittedIntValue();
                previewData.loops = fixedLoopsSpinner.getCommittedIntValue();
                break;

            case RAMP_UP:
                previewData.rampUpStartThreads = rampUpStartThreadsSpinner.getCommittedIntValue();
                previewData.rampUpEndThreads = rampUpEndThreadsSpinner.getCommittedIntValue();
                previewData.rampUpTime = rampUpTimeSpinner.getCommittedIntValue();
                previewData.rampUpDuration = rampUpDurationSpinner.getCommittedIntValue();
                break;

            case SPIKE:
                previewData.spikeMinThreads = spikeMinThreadsSpinner.getCommittedIntValue();
                previewData.spikeMaxThreads = spikeMaxThreadsSpinner.getCommittedIntValue();
                previewData.spikeRampUpTime = spikeRampUpTimeSpinner.getCommittedIntValue();
                previewData.spikeHoldTime = spikeHoldTimeSpinner.getCommittedIntValue();
                previewData.spikeRampDownTime = spikeRampDownTimeSpinner.getCommittedIntValue();
                previewData.spikeDuration = spikeDurationSpinner.getCommittedIntValue();
                break;

            case STAIRS:
                previewData.stairsStartThreads = stairsStartThreadsSpinner.getCommittedIntValue();
                previewData.stairsEndThreads = stairsEndThreadsSpinner.getCommittedIntValue();
                previewData.stairsStep = stairsStepSpinner.getCommittedIntValue();
                previewData.stairsHoldTime = stairsHoldTimeSpinner.getCommittedIntValue();
                previewData.stairsDuration = stairsDurationSpinner.getCommittedIntValue();
                break;
        }

        previewPanel.setPreviewData(previewData);
        previewPanel.repaint();
    }

    // Chèn lấp dữ liệu
    public void setThreadGroupData(PerformanceTreeNode node) {
        this.currentNode = node;
        ThreadGroupData data = node.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            node.threadGroupData = data;
        }
        data.normalize();

        // Đặt chế độ
        modeComboBox.setSelectedItem(data.threadMode);
        cardLayout.show(cardPanel, data.threadMode.name());

        // Đặt thông số chế độ cố định
        fixedNumThreadsSpinner.setValue(data.numThreads);
        fixedLoopsSpinner.setValue(data.loops);
        useTimeCheckBox.setSelected(data.useTime);
        useLoopCountButton.setSelected(!data.useTime);
        durationSpinner.setValue(data.duration);
        maxInFlightWaitSpinner.setValue(data.maxInFlightWaitSeconds);

        // đổi mớiUItrạng thái
        updateFixedExecutionModeState();
        updateMaxInFlightWaitState();

        // Đặt các tham số chế độ tăng
        rampUpStartThreadsSpinner.setValue(data.rampUpStartThreads);
        rampUpEndThreadsSpinner.setValue(data.rampUpEndThreads);
        rampUpTimeSpinner.setValue(data.rampUpTime);
        rampUpDurationSpinner.setValue(data.rampUpDuration);

        // Đặt tham số chế độ tăng đột biến
        spikeMinThreadsSpinner.setValue(data.spikeMinThreads);
        spikeMaxThreadsSpinner.setValue(data.spikeMaxThreads);
        spikeRampUpTimeSpinner.setValue(data.spikeRampUpTime);
        spikeHoldTimeSpinner.setValue(data.spikeHoldTime);
        spikeRampDownTimeSpinner.setValue(data.spikeRampDownTime);
        spikeDurationSpinner.setValue(data.spikeDuration);

        // Thiết lập các thông số chế độ bậc thang
        stairsStartThreadsSpinner.setValue(data.stairsStartThreads);
        stairsEndThreadsSpinner.setValue(data.stairsEndThreads);
        stairsStepSpinner.setValue(data.stairsStep);
        stairsHoldTimeSpinner.setValue(data.stairsHoldTime);
        stairsDurationSpinner.setValue(data.stairsDuration);

        // Đã cập nhật hình ảnh xem trước
        updatePreview();
    }

    /**
     * Buộc nộp tất cả DevWorkbenchJSpinner giá trị
     * Được sử dụng cho Ctrl/Cmd+S Khi lưu, hãy đảm bảo tất cả thông tin đầu vào đều được gửi
     */
    public void forceCommitAllSpinners() {
        List<DevWorkbenchJSpinner> allSpinners = Arrays.asList(
                fixedNumThreadsSpinner, fixedLoopsSpinner, durationSpinner, maxInFlightWaitSpinner,
                rampUpStartThreadsSpinner, rampUpEndThreadsSpinner,
                rampUpTimeSpinner, rampUpDurationSpinner,
                spikeMinThreadsSpinner, spikeMaxThreadsSpinner,
                spikeRampUpTimeSpinner, spikeHoldTimeSpinner,
                spikeRampDownTimeSpinner, spikeDurationSpinner,
                stairsStartThreadsSpinner, stairsEndThreadsSpinner,
                stairsStepSpinner, stairsHoldTimeSpinner, stairsDurationSpinner
        );

        allSpinners.forEach(DevWorkbenchJSpinner::forceCommit);
    }

    public void saveThreadGroupData() {
        if (currentNode == null) return;
        ThreadGroupData data = currentNode.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            currentNode.threadGroupData = data;
        }

        // Chế độ lưu
        data.threadMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();

        // Lưu các thông số chế độ cố định
        data.numThreads = fixedNumThreadsSpinner.getCommittedIntValue();
        data.loops = fixedLoopsSpinner.getCommittedIntValue();
        data.useTime = useTimeCheckBox.isSelected();
        data.duration = durationSpinner.getCommittedIntValue();
        data.maxInFlightWaitSeconds = maxInFlightWaitSpinner.getCommittedIntValue();

        // Lưu các tham số chế độ tăng
        data.rampUpStartThreads = rampUpStartThreadsSpinner.getCommittedIntValue();
        data.rampUpEndThreads = rampUpEndThreadsSpinner.getCommittedIntValue();
        data.rampUpTime = rampUpTimeSpinner.getCommittedIntValue();
        data.rampUpDuration = rampUpDurationSpinner.getCommittedIntValue();

        // Lưu các thông số chế độ tăng đột biến
        data.spikeMinThreads = spikeMinThreadsSpinner.getCommittedIntValue();
        data.spikeMaxThreads = spikeMaxThreadsSpinner.getCommittedIntValue();
        data.spikeRampUpTime = spikeRampUpTimeSpinner.getCommittedIntValue();
        data.spikeHoldTime = spikeHoldTimeSpinner.getCommittedIntValue();
        data.spikeRampDownTime = spikeRampDownTimeSpinner.getCommittedIntValue();
        data.spikeDuration = spikeDurationSpinner.getCommittedIntValue();

        // Lưu các tham số chế độ bậc thang
        data.stairsStartThreads = stairsStartThreadsSpinner.getCommittedIntValue();
        data.stairsEndThreads = stairsEndThreadsSpinner.getCommittedIntValue();
        data.stairsStep = stairsStepSpinner.getCommittedIntValue();
        data.stairsHoldTime = stairsHoldTimeSpinner.getCommittedIntValue();
        data.stairsDuration = stairsDurationSpinner.getCommittedIntValue();
        data.normalize();
    }

    private static final class ThreadModeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ThreadGroupData.ThreadMode threadMode) {
                setText(I18nUtil.getMessage(threadMode.getMessageKey()));
            }
            return component;
        }
    }

    private JPanel createExecutionModePanel() {
        return executionModeBar;
    }

    private void updateFixedExecutionModeState() {
        boolean useTime = useTimeCheckBox.isSelected();
        fixedLoopsSpinner.setEnabled(!useTime);
        durationSpinner.setEnabled(useTime);
    }

    private void showThreadMode(ThreadGroupData.ThreadMode mode) {
        JPanel visiblePanel = switch (mode) {
            case FIXED -> fixedPanel;
            case RAMP_UP -> rampUpPanel;
            case SPIKE -> spikePanel;
            case STAIRS -> stairsPanel;
        };
        cardLayout.show(cardPanel, mode.name());
        cardPanel.setPreferredSize(new Dimension(
                CONFIG_PANEL_WIDTH,
                visiblePanel.getPreferredSize().height
        ));
        cardPanel.revalidate();
    }

    private void updateMaxInFlightWaitState() {
        ThreadGroupData.ThreadMode mode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
        boolean enabled = mode != null
                && (mode != ThreadGroupData.ThreadMode.FIXED || useTimeCheckBox.isSelected());
        maxInFlightWaitLabel.setEnabled(enabled);
        maxInFlightWaitSpinner.setEnabled(enabled);
    }

    private static String trimFieldLabel(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[:：]\\s*$", "");
    }

    // Xem trước mô hình dữ liệu
    private static class ThreadLoadPreviewData {
        ThreadGroupData.ThreadMode mode;
        // Mẫu cố định
        int fixedThreads;
        boolean useTime;
        int duration;
        int loops;
        // Chế độ tăng dần
        int rampUpStartThreads;
        int rampUpEndThreads;
        int rampUpTime;
        int rampUpDuration;
        // Chế độ đột biến
        int spikeMinThreads;
        int spikeMaxThreads;
        int spikeRampUpTime;
        int spikeHoldTime;
        int spikeRampDownTime;
        int spikeDuration;
        // Chế độ thang
        int stairsStartThreads;
        int stairsEndThreads;
        int stairsStep;
        int stairsHoldTime;
        int stairsDuration;
    }

    // Triển khai bảng xem trước
    private static class ThreadLoadPreviewPanel extends JPanel {
        private ThreadLoadPreviewData previewData;
        private static final int PADDING = 40;
        private static final int AXIS_LABEL_GAP = 6;

        public void setPreviewData(ThreadLoadPreviewData data) {
            this.previewData = data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewData == null) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontsUtil.applyTextRenderingHints(g2d);
            g2d.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));

            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            // Vẽ lưới và trục
            drawGrid(g2d, width, height);

            // Vẽ đường cong theo các chế độ khác nhau
            drawCurve(g2d, width, height);

            g2d.dispose();
        }

        /**
         * Tạo vảy đẹp (nice ticks），Chẳng hạn như1-2-5-10-20Đợi đã
         *
         * @param min      giá trị tối thiểu
         * @param max      giá trị tối đa
         * @param maxTicks Số lượng dấu tích tối đa
         * @return đánh dấu mảng
         */
        private static java.util.List<Integer> getNiceTicks(int min, int max, int maxTicks) {
            java.util.List<Integer> ticks = new java.util.ArrayList<>();
            if (max <= min || maxTicks <= 0) return ticks;
            int range = max - min;
            if (range == 0) {
                ticks.add(min);
                return ticks;
            }
            // Tính toán các khoảng thời gian duyên dáng
            double rawStep = (double) range / maxTicks;
            double mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
            double[] niceSteps = {1, 2, 5, 10};
            double niceStep = niceSteps[0] * mag;
            for (double s : niceSteps) {
                if (rawStep <= s * mag) {
                    niceStep = s * mag;
                    break;
                }
            }
            // ngăn chặnniceStepvì0Hoặc quá nhỏ để tránh vòng lặp vô hạn
            if (niceStep < 1) niceStep = 1;
            int niceStepInt = (int) niceStep; // Convert to int to avoid implicit narrowing in compound assignment
            int niceMin = (int) (Math.floor(min / niceStep) * niceStep);
            int niceMax = (int) (Math.ceil(max / niceStep) * niceStep);
            int maxLoop = 1000; // Số lượng chu kỳ bảo vệ tối đa
            for (int v = niceMin, count = 0; v <= niceMax && count < maxLoop; v += niceStepInt, count++) {
                if (v >= min && v <= max) {
                    ticks.add(v);
                }
            }
            if (!ticks.contains(max)) ticks.add(max);
            return ticks;
        }

        private void drawGrid(Graphics2D g2d, int width, int height) {
            Color gridColor = UIManager.getColor("Performance.chart.gridColor");
            Color textColor = UIManager.getColor("Performance.chart.textColor");
            Color axisColor = UIManager.getColor("Performance.chart.axisColor");

            g2d.setColor(gridColor);
            g2d.setStroke(new BasicStroke(0.5f));

            // YTrục được thu nhỏ đẹp mắt
            int maxThreads = getMaxThreads();
            java.util.List<Integer> yTicks = getNiceTicks(0, maxThreads, 5);
            for (int i = 0; i < yTicks.size(); i++) {
                int threadValue = yTicks.get(i);
                int y = PADDING + height - (int) ((double) threadValue / maxThreads * height);
                g2d.draw(new java.awt.geom.Line2D.Double(PADDING, y, PADDING + width, y));
                if (threadValue > 0) {
                    g2d.setColor(textColor);
                    String label = String.valueOf(threadValue);
                    FontMetrics fm = g2d.getFontMetrics();
                    g2d.drawString(label, PADDING - AXIS_LABEL_GAP - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
                    g2d.setColor(gridColor);
                }
            }

            // XTrục được thu nhỏ đẹp mắt
            int duration = getDuration();
            java.util.List<Integer> xTicks = getNiceTicks(0, duration, 10);
            for (int i = 0; i < xTicks.size(); i++) {
                int timeValue = xTicks.get(i);
                int x = PADDING + (int) ((double) timeValue / duration * width);
                g2d.draw(new java.awt.geom.Line2D.Double(x, PADDING, x, PADDING + height));
                if (timeValue > 0) {
                    g2d.setColor(textColor);
                    String label = timeValue + "s";
                    FontMetrics fm = g2d.getFontMetrics();
                    g2d.drawString(label, x - fm.stringWidth(label) / 2, PADDING + height + AXIS_LABEL_GAP + fm.getAscent());
                    g2d.setColor(gridColor);
                }
            }

            // Trục tọa độ
            g2d.setColor(axisColor);
            g2d.setStroke(new BasicStroke(1.5f));

            // Xtrục
            g2d.draw(new Line2D.Double(PADDING, PADDING + height, PADDING + width, PADDING + height));
            // Ytrục
            g2d.draw(new Line2D.Double(PADDING, PADDING, PADDING, PADDING + height));

            // Nhãn trục
            g2d.setColor(textColor);
            FontMetrics fm = g2d.getFontMetrics();
            String axisTitle = I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_TIME_SECONDS);
            g2d.drawString(axisTitle, PADDING + (width - fm.stringWidth(axisTitle)) / 2,
                    PADDING + height + AXIS_LABEL_GAP + fm.getHeight() + fm.getAscent());

            // Thêm thông tin chế độ ở góc trên bên trái
            g2d.drawString(I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_MODE_PREFIX)
                    + " "
                    + I18nUtil.getMessage(previewData.mode.getMessageKey()), PADDING, PADDING - 10);
        }

        private int getMaxThreads() {

            // Tính toán số lượng luồng tối đa thực tế được sử dụng dựa trên chế độ hiện tại
            int actualMax;
            switch (previewData.mode) {
                case FIXED:
                    actualMax = previewData.fixedThreads;
                    break;
                case RAMP_UP:
                    actualMax = Math.max(previewData.rampUpStartThreads, previewData.rampUpEndThreads);
                    break;
                case SPIKE:
                    actualMax = previewData.spikeMaxThreads;
                    break;
                case STAIRS:
                    actualMax = previewData.stairsEndThreads;
                    break;
                default:
                    actualMax = 20; // Giá trị mặc định
            }
            // tăng lên50% biên độ an toàn
            return (int) (actualMax * 1.5);
        }

        private int getDuration() {
            // Trả về thời lượng dựa trên chế độ hiện tại (đối với Xthang đo trục)
            switch (previewData.mode) {
                case FIXED:
                    return previewData.useTime ? previewData.duration : 60;
                case RAMP_UP:
                    return previewData.rampUpDuration;
                case SPIKE:
                    return previewData.spikeDuration;
                case STAIRS:
                    return previewData.stairsDuration;
                default:
                    return 60;
            }
        }

        private void drawCurve(Graphics2D g2d, int width, int height) {
            Color curveColor = UIManager.getColor("Performance.chart.curveColor");
            g2d.setColor(curveColor);
            g2d.setStroke(new BasicStroke(2.0f));

            List<Point> points = new ArrayList<>();

            switch (previewData.mode) {
                case FIXED:
                    drawFixedCurve(points, width, height);
                    break;
                case RAMP_UP:
                    drawRampUpCurve(points, width, height);
                    break;
                case SPIKE:
                    drawSpikeCurve(points, width, height);
                    break;
                case STAIRS:
                    drawStairsCurve(points, width, height);
                    break;
            }

            // Vẽ các đoạn thẳng
            if (points.size() >= 2) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
                }
            }

            // Vẽ điểm
            Color bgColor = ModernColors.getCardBackgroundColor();
            g2d.setColor(bgColor);
            g2d.setStroke(new BasicStroke(1.0f));
            for (Point p : points) {
                g2d.fill(new Rectangle2D.Double((double) p.x - 3, (double) p.y - 3, 6, 6));
                g2d.setColor(curveColor);
                g2d.draw(new Rectangle2D.Double((double) p.x - 3, (double) p.y - 3, 6, 6));
                g2d.setColor(bgColor);
            }
        }

        private void drawFixedCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();

            int x1 = PADDING;
            int y2 = PADDING + height - height * previewData.fixedThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            x1 = PADDING + width;
            points.add(new Point(x1, y2));
        }

        private void drawRampUpCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x1 = PADDING;
            int y2 = PADDING + height - height * previewData.rampUpStartThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            int rampUpEndX = PADDING + (width * previewData.rampUpTime / duration);
            int y3 = PADDING + height - height * previewData.rampUpEndThreads / maxThreads;
            if (y3 < PADDING) y3 = PADDING + 5;
            points.add(new Point(rampUpEndX, y3));

            points.add(new Point(PADDING + width, y3));
        }

        private void drawSpikeCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x = PADDING;
            int yMin = PADDING + height - height * previewData.spikeMinThreads / maxThreads;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.spikeMaxThreads / maxThreads;
            if (yMax < PADDING) yMax = PADDING + 5;


            // Chủ đề tối thiểu
            points.add(new Point(x, yMin));

            // Tỷ lệ thời gian tính toán
            int availWidth = width;

            // tăng lên
            x = PADDING + (availWidth * previewData.spikeRampUpTime / duration);
            points.add(new Point(x, yMax));

            // Giữ
            x = PADDING + (availWidth * (previewData.spikeRampUpTime + previewData.spikeHoldTime) / duration);
            points.add(new Point(x, yMax));

            // sự suy sụp
            x = PADDING + (availWidth * (previewData.spikeRampUpTime + previewData.spikeHoldTime + previewData.spikeRampDownTime) / duration);
            points.add(new Point(x, yMin));

            points.add(new Point(PADDING + width, yMin));
        }

        private void drawStairsCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = previewData.stairsDuration; // Tổng thời lượng

            int x = PADDING;
            int startThreads = previewData.stairsStartThreads;
            int endThreads = previewData.stairsEndThreads;
            int step = previewData.stairsStep;
            int holdTime = previewData.stairsHoldTime;

            int currentThreads = startThreads;
            int time = 0;

            // Tính toányđiều phối
            int y = PADDING + height - height * currentThreads / maxThreads;
            if (y < PADDING) y = PADDING + 5;
            points.add(new Point(x, y));

            while (time < duration && currentThreads < endThreads) {
                // Tính thời gian cho bước tiếp theo
                int nextTime = time + holdTime;
                if (nextTime > duration) nextTime = duration;

                // Tính toánxđiều phối
                int nextX = PADDING + (width * nextTime / duration);
                // Đường ngang: Số lượng người dùng không thay đổi.xTiến lên
                points.add(new Point(nextX, y));

                // Đường dọc: Số lượng người dùng tăng lên
                currentThreads += step;
                if (currentThreads > endThreads) currentThreads = endThreads;
                int nextY = PADDING + height - height * currentThreads / maxThreads;
                if (nextY < PADDING) nextY = PADDING + 5;
                points.add(new Point(nextX, nextY));

                // Thời gian trước
                time = nextTime;
                y = nextY;
                x = nextX;
            }

            // Nếu điểm cuối cùng chưa đạt đượcduration，Thêm một đường ngang vào cuối
            int endX = PADDING + width;
            if (x < endX) {
                points.add(new Point(endX, y));
            }
        }
    }
}
