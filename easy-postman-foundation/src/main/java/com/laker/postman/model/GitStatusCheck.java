package com.laker.postman.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitKết quả kiểm tra trạng thái
 */
public class GitStatusCheck {
    // Trạng thái cơ bản
    public boolean hasUncommittedChanges = false; // Có bất kỳ thay đổi không được cam kết?
    public boolean hasLocalCommits = false; // Có một cam kết địa phương
    public boolean hasRemoteCommits = false; // Cho dù có gửi từ xa

    // Thông tin số lượng nộp tại địa phương
    public int localCommitsAhead = 0;
    // Gửi thông tin số lượng từ xa
    public int remoteCommitsBehind = 0;

    // khả năng hoạt động
    public boolean canCommit = false; // Cho dù nó có thể được gửi
    public boolean canPush = false; // Liệu nó có thể được đẩy
    public boolean canPull = false; // Liệu nó có thể được kéo

    // Tình trạng kho từ xa
    public boolean hasRemoteRepository = false; // Liệu một nhà kho ở xa có tồn tại hay không
    public boolean hasUpstreamBranch = false; // Có chi nhánh thượng nguồn không?
    public boolean isRemoteRepositoryEmpty = false; // Kho từ xa có trống không
    public boolean isFirstPush = false; // Cho dù đó là lần đẩy đầu tiên

    // Xung đột và phát hiện vấn đề
    public boolean hasFileConflicts = false; // Có bất kỳ xung đột tập tin?
    public boolean needsForcePush = false; // Có cần đẩy lực hay không
    public boolean needsForcePull = false; // Có cần phải kéo cưỡng bức hay không

    // Phát hiện xung đột
    public boolean hasActualConflicts = false; // Liệu có xung đột nội dung thực tế hay không
    public boolean canAutoMerge = false; // Liệu nó có thể được hợp nhất tự động hay không
    public boolean hasOnlyNewFiles = false; // Đây chỉ là một tập tin mới (không có xung đột)
    public boolean hasNonOverlappingChanges = false; // Cho dù đó là một thay đổi không chồng chéo

    // Trạng thái mạng và xác thực
    public boolean canConnectToRemote = false; // Liệu nó có thể kết nối với kho từ xa hay không
    public boolean hasAuthenticationIssue = false; // Có bất kỳ vấn đề xác thực?

    // Phán quyết loại hoạt động
    public boolean isInitTypeWorkspace = false; // Cho dù đó là không gian làm việc thuộc loại khởi tạo
    public boolean isEmptyLocalRepository = false; // Liệu kho địa phương có trống không

    // Danh sách tin nhắn (dành riêng cho việc xây dựng)
    public final List<String> warnings = new ArrayList<>();
    public final List<String> suggestions = new ArrayList<>();

    // Thông tin chi nhánh hiện tại
    public String currentBranch;
    public String remoteBranch;
    public String remoteUrl;

    // Danh sách tập tin
    public final List<String> conflictingFiles = new ArrayList<>();

    // Xung độtChi tiết (tên tệp -> danh sách các khối xung đột)
    public Map<String, List<ConflictBlock>> conflictDetails = new HashMap<>();

    // Danh sách các tập tin thay đổi cục bộ
    public final List<String> added = new ArrayList<>();
    public final List<String> changed = new ArrayList<>();
    public final List<String> removed = new ArrayList<>();
    public final List<String> missing = new ArrayList<>();
    public final List<String> modified = new ArrayList<>();
    public final List<String> untracked = new ArrayList<>();
    public final List<String> conflicting = new ArrayList<>();

    // Danh sách tập tin được thay đổi từ xa
    public List<String> remoteAdded = new ArrayList<>();
    public List<String> remoteModified = new ArrayList<>();
    public List<String> remoteRemoved = new ArrayList<>();
    public List<String> remoteRenamed = new ArrayList<>();
    public List<String> remoteCopied = new ArrayList<>();
}