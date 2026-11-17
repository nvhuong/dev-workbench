package com.laker.postman.panel.workspace.components;

import cn.hutool.core.util.RandomUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.common.exception.WorkspaceCreateException;
import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Tạo hộp thoại không gian làm việc
 * Hỗ trợ tạo không gian làm việc cục bộ vàGitkhu vực làm việc
 */
@Slf4j
public class WorkspaceCreateDialog extends ProgressDialog {

    private static final String DEFAULT_BRANCH = "master";
    private static final int DIALOG_MIN_WIDTH = 580;
    public static final String WORKSPACES = "workspaces";

    @Getter
    private transient Workspace workspace;

    // UIthành phần
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JRadioButton localTypeRadio;
    private JRadioButton gitTypeRadio;
    private JTextField pathField;
    private JButton browseButton;
    private JCheckBox autoGeneratePathCheckBox;
    private JScrollPane descriptionScrollPane;

    // GitCác thành phần liên quan
    private JPanel gitPanel;
    private JRadioButton cloneRadio;
    private JRadioButton initRadio;
    private JTextField gitUrlField;
    private GitAuthPanel gitAuthPanel;

    // Đã thêm các thành phần liên quan đến chi nhánh
    private JTextField branchField;
    private JLabel branchLabel;
    private boolean adjustingWorkspaceType;

    public WorkspaceCreateDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE));
        initComponents();
        initDialog();
    }

    private void initComponents() {
        // Thông tin cơ bản
        nameField = new JTextField(10);
        SettingsInputStyle.apply(nameField);
        descriptionArea = new JTextArea(2, 10);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentInput(descriptionArea);
        descriptionScrollPane = new JScrollPane(descriptionArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(descriptionScrollPane);

        // Loại không gian làm việc
        localTypeRadio = new JRadioButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL), true);
        gitTypeRadio = new JRadioButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(localTypeRadio);
        typeGroup.add(gitTypeRadio);

        // Lựa chọn đường đi - Đặt thư mục không gian làm việc mặc định
        pathField = new JTextField(15);
        SettingsInputStyle.apply(pathField);
        setDefaultWorkspacePath(); // Đặt đường dẫn mặc định

        browseButton = new JButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_PATH));
        browseButton.setIcon(IconUtil.createThemed("icons/file.svg", 16, 16));

        // Tự động tạo các tùy chọn đường dẫn
        autoGeneratePathCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.WORKSPACE_AUTO_GENERATE_PATH), true);
        autoGeneratePathCheckBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));

        // GitCác thành phần liên quan
        cloneRadio = new JRadioButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_CLONE_FROM_REMOTE), true);
        initRadio = new JRadioButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_INIT_LOCAL));
        ButtonGroup gitModeGroup = new ButtonGroup();
        gitModeGroup.add(cloneRadio);
        gitModeGroup.add(initRadio);

        gitUrlField = new JTextField(15);
        SettingsInputStyle.apply(gitUrlField);
        gitAuthPanel = new GitAuthPanel();

        // Khởi tạo các trường nhánh
        branchField = new JTextField(15);
        branchField.setText(DEFAULT_BRANCH);
        SettingsInputStyle.apply(branchField);

        // Các thành phần liên quan đến tiến độ
        progressPanel = new ProgressPanel("Progress");
        progressPanel.setVisible(false);

        // Đặt phông chữ mặc định
        Font defaultFont = FontsUtil.getDefaultFont(Font.PLAIN);
        nameField.setFont(defaultFont);
        descriptionArea.setFont(defaultFont);
        pathField.setFont(defaultFont);
        gitUrlField.setFont(defaultFont);
        branchField.setFont(defaultFont);
    }

    /**
     * Đặt đường dẫn không gian làm việc mặc định
     */
    private void setDefaultWorkspacePath() {
        String defaultWorkspaceDir = SystemUtil.getEasyPostmanPath() + WORKSPACES;
        pathField.setText(defaultWorkspaceDir);
    }

    /**
     * Tạo tên thư mục phù hợp với quy tắc đường dẫn dựa trên tên không gian làm việc
     */
    private String generatePathFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // Dọn dẹp tên và tạo tên thư mục hợp pháp
        String cleanName = name.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_") // Thay thế các ký tự không hợp lệ
                .replaceAll("\\s+", "_") // Dấu cách được thay thế bằng dấu gạch dưới
                .replaceAll("_{2,}", "_") // Nhiều dấu gạch dưới được kết hợp thành một
                .replaceAll("^_|_$", ""); // Loại bỏ dấu gạch dưới ở đầu và cuối

        if (cleanName.isEmpty()) {
            cleanName = RandomUtil.randomString(10); // Nếu trống sau khi làm sạch, một chuỗi ngẫu nhiên sẽ được sử dụng
        }

        return ConfigPathConstants.WORKSPACES_DIR + cleanName;
    }

    @Override
    protected void setupLayout() {
        setLayout(new BorderLayout());

        // Tạo bảng điều khiển vùng chứa chính
        JPanel containerPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(containerPanel);
        containerPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));

        // Tạo bảng nội dung - Sử dụng bảng cuộn để thích ứng với các kích thước màn hình khác nhau
        JPanel contentPanel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        GridBagConstraints contentGbc = new GridBagConstraints();
        contentGbc.gridx = 0;
        contentGbc.weightx = 1.0;
        contentGbc.fill = GridBagConstraints.HORIZONTAL;
        contentGbc.anchor = GridBagConstraints.NORTHWEST;

        // Bảng thông tin cơ bản
        JPanel basicPanel = createBasicInfoPanel();
        contentGbc.gridy = 0;
        contentGbc.weighty = 0;
        contentPanel.add(basicPanel, contentGbc);

        // GitBảng cấu hình
        gitPanel = createGitPanel();
        gitPanel.setVisible(false);
        contentGbc.gridy = 1;
        contentGbc.insets = new Insets(10, 0, 0, 0);
        contentPanel.add(gitPanel, contentGbc);

        contentGbc.gridy = 2;
        contentGbc.weighty = 1.0;
        contentGbc.fill = GridBagConstraints.BOTH;
        contentGbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(Box.createVerticalGlue(), contentGbc);

        // Thêm hỗ trợ cuộn cho bảng nội dung
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        containerPanel.add(scrollPane, BorderLayout.CENTER);

        // Bảng dưới cùng: thanh tiến trình + nút
        JPanel bottomPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(bottomPanel);

        // Bảng tiến độ
        bottomPanel.add(progressPanel, BorderLayout.NORTH);

        // Bảng nút
        JPanel buttonPanel = createStandardButtonPanel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE));
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        containerPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(containerPanel, BorderLayout.CENTER);

        // Duy trì chiều rộng và chiều cao tối thiểu ổn định được xác định bởi nội dung hiện có
        setPreferredSize(new Dimension(DIALOG_MIN_WIDTH, getPreferredSize().height));
        setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, 0));
    }

    @Override
    protected void setupEventHandlers() {
        // Chuyển đổi loại không gian làm việc
        ActionListener typeChangeListener = e -> handleWorkspaceTypeChanged();
        localTypeRadio.addActionListener(typeChangeListener);
        gitTypeRadio.addActionListener(typeChangeListener);

        // GitChuyển đổi chế độ
        ActionListener gitModeListener = e -> updateGitUrlFieldState();
        cloneRadio.addActionListener(gitModeListener);
        initRadio.addActionListener(gitModeListener);

        // Duyệt đường dẫn
        browseButton.addActionListener(this::browseForPath);

        // Người nghe thay đổi trường tên - Tự động tạo đường dẫn
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (autoGeneratePathCheckBox.isSelected()) {
                    updatePathFromName();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (autoGeneratePathCheckBox.isSelected()) {
                    updatePathFromName();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (autoGeneratePathCheckBox.isSelected()) {
                    updatePathFromName();
                }
            }
        });

        // Hộp kiểm tự động tạo đường dẫn
        autoGeneratePathCheckBox.addActionListener(e -> {
            boolean autoGen = autoGeneratePathCheckBox.isSelected();
            pathField.setEditable(!autoGen);
            browseButton.setEnabled(!autoGen);
            if (autoGen) {
                updatePathFromName();
            }
        });

        // trạng thái ban đầu
        updateGitPanelVisibility();
        updateGitUrlFieldState();

        // Đặt trạng thái ban đầu
        pathField.setEditable(false);
        browseButton.setEnabled(false);
    }

    /**
     * Cập nhật đường dẫn dựa trên tên
     */
    private void updatePathFromName() {
        String name = nameField.getText();
        String generatedPath = generatePathFromName(name);
        pathField.setText(generatedPath);
    }

    private void handleWorkspaceTypeChanged() {
        if (adjustingWorkspaceType) {
            updateGitPanelVisibility();
            return;
        }
        updateGitPanelVisibility();
    }

    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSection(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_INFO));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(titleLabel, gbc);

        // Tên không gian làm việc
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ":"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        // Mô tả
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_DESCRIPTION) + ":"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panel.add(descriptionScrollPane, gbc);

        // Loại không gian làm việc
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE) + ":"), gbc);
        gbc.gridx = 1;
        panel.add(localTypeRadio, gbc);
        gbc.gridx = 2;
        panel.add(gitTypeRadio, gbc);

        // Tự động tạo các tùy chọn đường dẫn
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(autoGeneratePathCheckBox, gbc);

        // đường đi cục bộ
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(pathField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(browseButton, gbc);

        return panel;
    }

    private JPanel createGitPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSection(panel);

        GridBagConstraints rootGbc = new GridBagConstraints();
        rootGbc.gridx = 0;
        rootGbc.weightx = 1.0;
        rootGbc.fill = GridBagConstraints.HORIZONTAL;
        rootGbc.anchor = GridBagConstraints.NORTHWEST;

        JLabel titleLabel = new JLabel("Git " + I18nUtil.getMessage(MessageKeys.WORKSPACE_INFO));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        rootGbc.gridy = 0;
        rootGbc.insets = new Insets(6, 8, 2, 8);
        panel.add(titleLabel, rootGbc);

        // GitLựa chọn chế độ
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modePanel.setOpaque(false);
        modePanel.add(cloneRadio);
        modePanel.add(Box.createHorizontalStrut(18));
        modePanel.add(initRadio);
        rootGbc.gridy = 1;
        rootGbc.insets = new Insets(6, 8, 2, 8);
        panel.add(modePanel, rootGbc);

        // Git URL、Cấu hình chi nhánh
        JPanel gitConfigPanel = new JPanel(new GridBagLayout());
        gitConfigPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Git URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gitConfigPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_URL) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gitConfigPanel.add(gitUrlField, gbc);

        // Cấu hình chi nhánh
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        branchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE_DIALOG_BRANCH_LABEL));
        gitConfigPanel.add(branchLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gitConfigPanel.add(branchField, gbc);

        rootGbc.gridy = 2;
        rootGbc.insets = new Insets(0, 8, 2, 8);
        panel.add(gitConfigPanel, rootGbc);

        // GitBảng xác thực
        rootGbc.gridy = 3;
        rootGbc.insets = new Insets(0, 8, 6, 8);
        panel.add(gitAuthPanel, rootGbc);

        // Đặt các hàng trống để đảm bảo nội dung luôn hiển thị ở trên cùng thay vì kéo giãn vùng giữa biểu mẫu
        rootGbc.gridy = 4;
        rootGbc.weighty = 1.0;
        rootGbc.fill = GridBagConstraints.BOTH;
        rootGbc.insets = new Insets(0, 0, 0, 0);
        panel.add(Box.createVerticalGlue(), rootGbc);

        return panel;
    }

    private void updateGitPanelVisibility() {
        boolean isGit = gitTypeRadio.isSelected();
        gitPanel.setVisible(isGit);

        setPreferredSize(null);
        pack();
        if (getWidth() < DIALOG_MIN_WIDTH) {
            setSize(new Dimension(DIALOG_MIN_WIDTH, getHeight()));
        }
        setLocationRelativeTo(getParent());
    }

    private void updateGitUrlFieldState() {
        boolean enableUrl = cloneRadio.isSelected();
        gitUrlField.setEnabled(enableUrl);
        gitAuthPanel.setComponentsEnabled(enableUrl);

        // Kiểm soát nhãn và trạng thái của các trường nhánh dựa trên chế độ
        if (cloneRadio.isSelected()) {
            branchLabel.setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_BRANCH) + ":");
            branchField.setEnabled(true);
            if (branchField.getText().trim().isEmpty()) {
                branchField.setText(DEFAULT_BRANCH);
            }
        } else {
            branchLabel.setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_LOCAL_BRANCH) + ":");
            branchField.setEnabled(true);
            if (branchField.getText().trim().isEmpty()) {
                branchField.setText(DEFAULT_BRANCH);
            }
        }
    }

    private void browseForPath(ActionEvent e) {
        SystemFileChooser chooser = FileChooserUtil.createDirectoryChooser(
                "workspace.create.path",
                I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_PATH));

        String currentPath = pathField.getText().trim();
        if (currentPath.isEmpty()) {
            currentPath = ConfigPathConstants.WORKSPACES_DIR;
        }

        // Hãy chắc chắn rằng thư mục mẹ tồn tại
        File parentDir = new File(currentPath).getParentFile();
        if (parentDir != null && parentDir.exists()) {
            chooser.setCurrentDirectory(parentDir);
        } else {
            chooser.setCurrentDirectory(new File(SystemUtil.getEasyPostmanPath()));
        }

        int result = chooser.showOpenDialog(this);
        if (result == SystemFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            pathField.setText(selectedDir.getAbsolutePath());
            // Ngăn chặn việc tạo tự động vì người dùng đã chọn đường dẫn theo cách thủ công
            autoGeneratePathCheckBox.setSelected(false);
            pathField.setEditable(true);
            browseButton.setEnabled(true);
        }
    }

    @Override
    protected void validateInput() throws IllegalArgumentException {
        // Xác minh tên không gian làm việc
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_NAME_REQUIRED));
        }

        // Xác minh đường dẫn
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_PATH_REQUIRED));
        }

        // GitXác thực đặc biệt của không gian làm việc
        // Cần thiết trong chế độ nhân bảnGit URL và thông tin chứng nhận và thông tin chi nhánh
        if (gitTypeRadio.isSelected() && cloneRadio.isSelected()) {
            String gitUrl = gitUrlField.getText().trim();
            if (gitUrl.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_URL_REQUIRED));
            }
            String branch = branchField.getText().trim();
            if (branch.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
            }

            // Xác minh thông tin xác thực
            gitAuthPanel.validateAuth();
        }
        // Trong chế độ khởi tạo, chỉ cần xác minh thông tin nhánh
        if (gitTypeRadio.isSelected() && initRadio.isSelected()) {
            String branch = branchField.getText().trim();
            if (branch.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
            }
        }

        // Xây dựngworkspaceĐối tượng để sử dụng tiếp theo
        workspace = buildWorkspace();
    }

    @Override
    protected SwingWorker<Void, String> createWorkerTask() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE_DIALOG_CREATING));
                setProgress(10);
                try {
                    // sử dụng WorkspaceService Tạo một không gian làm việc và nó sẽ đảm nhiệm tất cả Git hoạt động
                    WorkspaceService.getInstance().createWorkspace(workspace);
                    publish(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE_DIALOG_CREATION_COMPLETED));
                    setProgress(100);
                } catch (Exception e) {
                    // Hãy thử lại ngoại lệ và để cho SwingWorker cơ chế xử lý lỗi
                    log.error(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE_DIALOG_CREATION_FAILED), e);
                    throw new WorkspaceCreateException(I18nUtil.getMessage(MessageKeys.WORKSPACE_CREATE_DIALOG_CREATION_FAILED_WITH_MESSAGE, e.getMessage()), e);
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    @Override
    protected void onOperationSuccess() {
        workspace = buildWorkspace();
        super.onOperationSuccess();
    }

    @Override
    protected void setInputComponentsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
        localTypeRadio.setEnabled(enabled);
        gitTypeRadio.setEnabled(enabled);
        autoGeneratePathCheckBox.setEnabled(enabled);

        // Trạng thái kích hoạt của các điều khiển liên quan đến đường dẫn phụ thuộc vào việc chúng có được tạo tự động hay không
        if (enabled) {
            boolean autoGen = autoGeneratePathCheckBox.isSelected();
            pathField.setEditable(!autoGen);
            browseButton.setEnabled(!autoGen);
        } else {
            pathField.setEditable(false);
            browseButton.setEnabled(false);
        }

        if (gitPanel.isVisible()) {
            cloneRadio.setEnabled(enabled);
            initRadio.setEnabled(enabled);
            gitUrlField.setEnabled(enabled);
            gitAuthPanel.setComponentsEnabled(enabled);
            branchField.setEnabled(enabled);
        }
    }

    private Workspace buildWorkspace() {
        Workspace ws = new Workspace();
        ws.setName(nameField.getText().trim());
        ws.setDescription(descriptionArea.getText().trim());
        ws.setPath(pathField.getText().trim());

        if (localTypeRadio.isSelected()) {
            ws.setType(WorkspaceType.LOCAL);
        } else {
            ws.setType(WorkspaceType.GIT);

            // Đặt thông tin chi nhánh địa phương
            String localBranch = branchField.getText().trim();
            if (!localBranch.isEmpty()) {
                ws.setCurrentBranch(localBranch);
            }

            if (cloneRadio.isSelected()) {
                ws.setGitRepoSource(GitRepoSource.CLONED);
                ws.setGitRemoteUrl(gitUrlField.getText().trim());

                GitAuthType authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();
                ws.setGitAuthType(authType);

                if (authType == GitAuthType.PASSWORD) {
                    ws.setGitUsername(gitAuthPanel.getUsername());
                    ws.setGitPassword(gitAuthPanel.getPassword());
                } else if (authType == GitAuthType.TOKEN) {
                    ws.setGitUsername(gitAuthPanel.getUsername());
                    ws.setGitToken(gitAuthPanel.getToken());
                } else if (authType == GitAuthType.SSH_KEY) {
                    ws.setSshPrivateKeyPath(gitAuthPanel.getSshKeyPath());
                    ws.setSshPassphrase(gitAuthPanel.getSshPassphrase());
                }
            } else {
                ws.setGitRepoSource(GitRepoSource.INITIALIZED);
            }
        }

        return ws;
    }
}
