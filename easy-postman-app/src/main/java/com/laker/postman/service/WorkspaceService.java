package com.laker.postman.service;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitBranchInfo;
import com.laker.postman.model.GitCommitInfo;
import com.laker.postman.model.GitFileChange;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.plugin.api.service.GitPluginService;
import com.laker.postman.plugin.host.GitServiceAccess;
import com.laker.postman.util.WorkspaceStorageUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lớp dịch vụ không gian làm việc
 * Chịu trách nhiệm tạo và quản lý không gian làm việc.Gitcác chức năng cốt lõi như hoạt động
 */
@Slf4j
public class WorkspaceService {

    private static final String WORKSPACE_NOT_FOUND_MSG = "Workspace not found: ";

    private static WorkspaceService instance;
    private List<Workspace> workspaces = new ArrayList<>();
    @Getter
    private Workspace currentWorkspace;

    private WorkspaceService() {
        loadWorkspaces();
    }

    public static synchronized WorkspaceService getInstance() {
        if (instance == null) {
            instance = new WorkspaceService();
        }
        return instance;
    }

    /**
     * Nhận tất cả các không gian làm việc
     */
    public List<Workspace> getAllWorkspaces() {
        return new ArrayList<>(workspaces);
    }

    /**
     * Tạo một không gian làm việc mới
     */
    public void createWorkspace(Workspace workspace) throws Exception {
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Cannot create the default workspace");
        }
        validateWorkspace(workspace);

        // Tạo sự độc đáoID
        workspace.setId(UUID.randomUUID().toString());
        workspace.setCreatedAt(System.currentTimeMillis());
        workspace.setUpdatedAt(System.currentTimeMillis());

        // Đối với khôngGitChế độ sao chép, tạo một thư mục cục bộ
        if (workspace.getType() != WorkspaceType.GIT || workspace.getGitRepoSource() != GitRepoSource.CLONED) {
            Path workspacePath = Paths.get(workspace.getPath());
            if (!Files.exists(workspacePath)) {
                Files.createDirectories(workspacePath);
            }
        }

        // Thực hiện các hành động thích hợp dựa trên loại không gian làm việc
        if (workspace.getType() == WorkspaceType.GIT) {
            handleGitWorkspace(workspace);
        }

        // Thêm vào danh sách không gian làm việc và lưu
        workspaces.add(workspace);
        saveWorkspaces();

        log.info("Created workspace: {} ({})", workspace.getName(), workspace.getType());
    }

    /**
     * xử lýGitĐã tạo không gian làm việc
     */
    private void handleGitWorkspace(Workspace workspace) throws Exception {
        requireGitService().prepareGitWorkspace(workspace);
    }

    /**
     * Xác minh các tham số không gian làm việc
     */
    private void validateWorkspace(Workspace workspace) throws IllegalArgumentException {
        if (workspace.getName() == null || workspace.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace name is required");
        }

        if (workspace.getPath() == null || workspace.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace path is required");
        }

        if (workspace.getType() == WorkspaceType.GIT) {
            if (workspace.getGitRepoSource() == GitRepoSource.CLONED) {
                if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().trim().isEmpty()) {
                    throw new IllegalArgumentException("Git repository URL is required for cloning");
                }

                if (workspace.getGitAuthType() == GitAuthType.PASSWORD) {
                    if (workspace.getGitUsername() == null || workspace.getGitPassword() == null) {
                        throw new IllegalArgumentException("Username and password are required");
                    }
                } else if (workspace.getGitAuthType() == GitAuthType.TOKEN) {
                    if (workspace.getGitUsername() == null || workspace.getGitToken() == null) {
                        throw new IllegalArgumentException("Username and access token are required");
                    }
                }
            }
        }

        // Kiểm tra xem đường dẫn đã được sử dụng bởi không gian làm việc khác chưa
        String normalizedPath = Paths.get(workspace.getPath()).toAbsolutePath().normalize().toString();
        boolean pathExists = workspaces.stream()
                .anyMatch(w -> Paths.get(w.getPath()).toAbsolutePath().normalize().toString().equals(normalizedPath));

        if (pathExists) {
            throw new IllegalArgumentException("Path is already used by another workspace");
        }

        // Đối vớiGitỞ chế độ sao chép, hãy kiểm tra xem thư mục đích đã tồn tại chưa và có trống không
        if (workspace.getType() == WorkspaceType.GIT && workspace.getGitRepoSource() == GitRepoSource.CLONED) {
            Path targetPath = Paths.get(workspace.getPath());
            if (Files.exists(targetPath)) {
                try {
                    // Kiểm tra xem thư mục có trống không
                    boolean isEmpty = Files.list(targetPath).findAny().isEmpty();
                    if (!isEmpty) {
                        throw new IllegalArgumentException("Target directory is not empty: " + workspace.getPath());
                    }
                    // Nếu thư mục tồn tại nhưng trống, hãy xóa nóGitSao chép và tạo lại
                    Files.delete(targetPath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot access target directory: " + workspace.getPath());
                }
            }
        }
    }

    /**
     * Chuyển đổi không gian làm việc
     */
    public void switchWorkspace(String workspaceId) {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));

        currentWorkspace = workspace;
        WorkspaceStorageUtil.saveCurrentWorkspace(workspaceId);
        log.info("Switched to workspace: {}", workspace.getName());
    }

    /**
     * Từ EasyPostman Xóa bản ghi không gian làm việc khỏi thư mục, giữ lại thư mục cục bộ và tất cả các tệp trong đó.
     */
    public void removeWorkspace(String workspaceId) {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Default workspace cannot be removed");
        }

        List<Workspace> remainingWorkspaces = new ArrayList<>(workspaces);
        remainingWorkspaces.removeIf(w -> w.getId().equals(workspaceId));

        // Trước tiên hãy kiên trì và giữ trạng thái bộ nhớ nếu quá trình lưu không thành công UI không thay đổi.
        WorkspaceStorageUtil.saveWorkspaces(remainingWorkspaces);
        workspaces = remainingWorkspaces;

        // Nếu không gian làm việc hiện tại bị xóa, hãy chuyển sang không gian làm việc mặc định
        if (currentWorkspace != null && currentWorkspace.getId().equals(workspaceId)) {
            // Chuyển về không gian làm việc mặc định trước, nếu không tìm thấy thì lấy cái đầu tiên trong danh sách, đảm bảo currentWorkspace Không null
            currentWorkspace = getDefaultWorkspace();
            if (currentWorkspace != null) {
                WorkspaceStorageUtil.saveCurrentWorkspace(currentWorkspace.getId());
            } else {
                log.warn("No workspace available after removal, currentWorkspace is null");
            }
        }

        log.info("Removed workspace registration and preserved local files: {} ({})",
                workspace.getName(), workspace.getPath());
    }

    public Workspace getDefaultWorkspace() {
        return workspaces.stream()
                .filter(WorkspaceStorageUtil::isDefaultWorkspace)
                .findFirst()
                .orElse(workspaces.isEmpty() ? null : workspaces.get(0));
    }

    /**
     * Đổi tên không gian làm việc
     */
    public void renameWorkspace(String workspaceId, String newName) {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Default workspace cannot be renamed");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace name cannot be empty");
        }

        workspace.setName(newName.trim());
        workspace.setUpdatedAt(System.currentTimeMillis());
        saveWorkspaces();

        log.info("Renamed workspace to: {}", newName);
    }

    /**
     * GitHành động: Kéo cập nhật
     */
    public GitOperationResult pullUpdates(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().pullUpdates(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * GitHành động: Đẩy các thay đổi
     */
    public GitOperationResult pushChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().pushChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Buộc thay đổi đẩy (ghi đè các thay đổi từ xa)
     */
    public GitOperationResult forcePushChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().forcePushChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Giai đoạn thay đổi cục bộ
     */
    public GitOperationResult stashChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().stashChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Hoàn nguyên các thay đổi theo giai đoạn
     */
    public GitOperationResult popStashChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().popStashChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Buộc cập nhật kéo (loại bỏ các thay đổi cục bộ)
     */
    public GitOperationResult forcePullUpdates(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().forcePullUpdates(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * GitHành động: Cam kết thay đổi
     */
    public GitOperationResult commitChanges(String workspaceId, String message) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().commitChanges(workspace, message);
        saveWorkspaces();
        return result;
    }

    /**
     * Nhận hai commit Danh sách các tập tin đã thay đổi giữa
     */
    public List<String> getChangedFilesBetweenCommits(String workspaceId, String oldCommitId, String newCommitId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getChangedFilesBetweenCommits(workspace, oldCommitId, newCommitId);
    }

    public List<GitFileChange> listWorkingTreeChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().listWorkingTreeChanges(workspace);
    }

    public String getWorkingTreeDiff(String workspaceId, String filePath) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getWorkingTreeDiff(workspace, filePath);
    }

    /**
     * theoIDNhận không gian làm việc
     */
    private Workspace getWorkspaceById(String workspaceId) {
        return workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
    }

    /**
     * Tải cấu hình không gian làm việc
     */
    private void loadWorkspaces() {
        try {
            workspaces = WorkspaceStorageUtil.loadWorkspaces();

            String storedCurrentWorkspaceId = WorkspaceStorageUtil.getCurrentWorkspace();
            if (storedCurrentWorkspaceId != null) {
                currentWorkspace = workspaces.stream()
                        .filter(w -> w.getId().equals(storedCurrentWorkspaceId))
                        .findFirst()
                        .orElse(null);
            }

            // Nếu không có không gian làm việc hiện tại nhưng có danh sách các không gian làm việc, hãy đặt không gian làm việc đầu tiên làm không gian làm việc hiện tại
            boolean storedWorkspaceSelected = currentWorkspace != null;
            if (currentWorkspace == null && !workspaces.isEmpty()) {
                currentWorkspace = workspaces.get(0);
                WorkspaceStorageUtil.saveCurrentWorkspace(currentWorkspace.getId());
            }

            String selectionSource = storedWorkspaceSelected
                    ? "saved_workspace"
                    : storedCurrentWorkspaceId == null ? "first_available_workspace" : "saved_workspace_not_found";
            log.info("Loaded workspaces: count={}, storedCurrentWorkspaceId={}, selectedCurrentWorkspaceId={}, selectionSource={}",
                    workspaces.size(), storedCurrentWorkspaceId,
                    currentWorkspace == null ? null : currentWorkspace.getId(), selectionSource);
        } catch (Exception e) {
            log.error("Failed to load workspaces", e);
            workspaces = new ArrayList<>();
            // Khi tải không thành công, cũng cần đảm bảo rằng có một không gian làm việc mặc định trong bộ nhớ để tránh các con trỏ null logic tiếp theo
            Workspace defaultWs = WorkspaceStorageUtil.getDefaultWorkspace();
            workspaces.add(defaultWs);
            currentWorkspace = defaultWs;
        }
    }

    /**
     * Lưu cấu hình không gian làm việc
     */
    private void saveWorkspaces() {
        try {
            WorkspaceStorageUtil.saveWorkspaces(workspaces);
        } catch (Exception e) {
            log.error("Failed to save workspaces", e);
        }
    }

    /**
     * vì Git Thêm kho từ xa mới vào không gian làm việc hoặc cập nhật kho hiện có origin。
     */
    public void addRemoteRepository(String workspaceId, String remoteUrl, String remoteBranch,
                                    GitAuthType authType, String username, String password, String token) throws Exception {
        addRemoteRepository(workspaceId, remoteUrl, remoteBranch, authType, username, password, token, null, null);
    }

    public void addRemoteRepository(String workspaceId, String remoteUrl, String remoteBranch,
                                    GitAuthType authType, String username, String password, String token,
                                    String sshPrivateKeyPath, String sshPassphrase) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        requireGitService().addRemoteRepository(workspace, remoteUrl, remoteBranch, authType, username, password, token,
                sshPrivateKeyPath, sshPassphrase);
        saveWorkspaces();
    }

    public List<String> testRemoteConnection(String remoteUrl, GitAuthType authType,
                                             String username, String password, String token,
                                             String sshPrivateKeyPath, String sshPassphrase) throws Exception {
        return requireGitService().testRemoteConnection(remoteUrl, authType, username, password, token,
                sshPrivateKeyPath, sshPassphrase);
    }

    /**
     * Nhận thông tin trạng thái kho từ xa
     */
    public RemoteStatus getRemoteStatus(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getRemoteStatus(workspace);
    }

    /**
     * Chuyển đổi không gian làm việc cục bộ sang Gitkhu vực làm việc
     * Tái sử dụng những cái hiện cóGitLogic khởi tạo
     */
    public void convertLocalToGit(Workspace workspace, String localBranch) throws Exception {
        requireGitService().convertLocalToGit(workspace, localBranch);
        saveWorkspaces();
    }

    /**
     * Lưu thứ tự không gian làm việc (được gọi sau khi sắp xếp kéo)
     */
    public void saveWorkspaceOrder(List<String> idOrder) {
        try {
            // theoidOrderSắp xếp lạiworkspacesdanh sách
            List<Workspace> sortedWorkspaces = new ArrayList<>();
            for (String id : idOrder) {
                for (Workspace ws : workspaces) {
                    if (ws.getId().equals(id)) {
                        sortedWorkspaces.add(ws);
                        break;
                    }
                }
            }

            // Thêm không gian làm việc có thể bị thiếu (ngăn ngừa mất dữ liệu)
            for (Workspace ws : workspaces) {
                if (!sortedWorkspaces.contains(ws)) {
                    sortedWorkspaces.add(ws);
                }
            }

            // Cập nhật danh sách không gian làm việc
            workspaces = sortedWorkspaces;

            // Kiên trì nộp hồ sơ
            saveWorkspaces();

            log.debug("Workspace order saved successfully");
        } catch (Exception e) {
            log.error("Failed to save workspace order", e);
        }
    }

    /**
     * Nhận không gian làm việc Git Cam kết lịch sử
     *
     * @param workspaceId khu vực làm việcID
     * @param maxCount    Số lượng hoàn trả tối đa,0Cho biết trả lại tất cả
     * @return Git Gửi danh sách thông tin
     */
    public List<GitCommitInfo> getGitHistory(String workspaceId, int maxCount) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getGitHistory(workspace, maxCount);
    }

    /**
     * Khôi phục không gian làm việc về không gian làm việc đã chỉ định Git Gửi phiên bản
     * sử dụng reset + commit cách để giữ một lịch sử đầy đủ
     *
     * @param workspaceId  khu vực làm việcID
     * @param commitId     nộpID
     * @param createBackup Có tạo các cam kết dự phòng hay không (lưu các thay đổi chưa được cam kết) trước khi khôi phục
     * @return Kết quả hoạt động
     */
    public GitOperationResult restoreToCommit(String workspaceId, String commitId, boolean createBackup) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().restoreToCommit(workspace, commitId, createBackup);
        saveWorkspaces();
        return result;
    }

    /**
     * Xem chi tiết của một cam kết được chỉ định
     *
     * @param workspaceId khu vực làm việcID
     * @param commitId    nộpID
     * @return Gửi chi tiết
     */
    public String getCommitDetails(String workspaceId, String commitId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getCommitDetails(workspace, commitId);
    }

    public GitStatusCheck checkGitStatus(Workspace workspace, GitOperation operation) {
        return requireGitService().checkGitStatus(workspace, operation);
    }

    public void clearSshCache(String privateKeyPath) {
        requireGitService().clearSshCache(privateKeyPath);
    }

    public boolean isGitServiceAvailable() {
        return GitServiceAccess.isServiceAvailable();
    }

    public List<GitBranchInfo> listGitBranches(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().listBranches(workspace);
    }

    public GitOperationResult switchGitBranch(String workspaceId, String branchName) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().switchBranch(workspace, branchName);
        saveWorkspaces();
        return result;
    }

    public GitOperationResult fetchGitBranches(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().fetchBranches(workspace);
    }

    public GitOperationResult createGitBranch(String workspaceId, String branchName) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().createBranch(workspace, branchName);
        saveWorkspaces();
        return result;
    }

    public GitOperationResult deleteGitBranch(String workspaceId, String branchName) throws Exception {
        return deleteGitBranch(workspaceId, branchName, false);
    }

    public GitOperationResult deleteGitBranch(String workspaceId, String branchName, boolean force) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().deleteBranch(workspace, branchName, force);
        saveWorkspaces();
        return result;
    }

    public GitOperationResult publishGitBranch(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().publishBranch(workspace);
        saveWorkspaces();
        return result;
    }

    private GitPluginService requireGitService() {
        return GitServiceAccess.requireService();
    }
}
