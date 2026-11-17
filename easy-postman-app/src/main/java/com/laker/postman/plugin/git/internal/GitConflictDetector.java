package com.laker.postman.plugin.git.internal;

import cn.hutool.core.io.FileUtil;
import com.laker.postman.model.ConflictBlock;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeChunk;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GitLớp công cụ phát hiện xung đột
 * phát hiệnGitNhững xung đột và vấn đề tiềm ẩn trước khi vận hành
 */
@Slf4j
public class GitConflictDetector {

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    // Git Thời gian chờ hoạt động (giây)- Và WorkspaceService Hãy nhất quán
    private static final int GIT_OPERATION_TIMEOUT = 10;

    private GitConflictDetector() {
        // Lớp tiện ích, hàm tạo ẩn
    }

    /**
     * nghiên cứuGitTrạng thái kho hàng để xác định xem hoạt động được chỉ định có thể được thực hiện hay không
     */
    public static GitStatusCheck checkGitStatus(String workspacePath, String operationType,
                                                CredentialsProvider credentialsProvider,
                                                SshCredentialsProvider sshCredentialsProvider) {
        GitStatusCheck result = new GitStatusCheck();

        try (Git git = Git.open(new File(workspacePath))) {
            // Nhận thông tin cơ bản
            result.currentBranch = git.getRepository().getBranch();

            // Kiểm tra trạng thái cục bộ
            Status status = git.status().call();
            checkLocalStatus(status, result);

            // Kiểm tra trạng thái từ xa
            checkRemoteStatus(git, workspacePath, result, credentialsProvider, sshCredentialsProvider);

            // Tạo đề xuất dựa trên loại hành động
            generateSuggestions(result, operationType);

        } catch (Exception e) {
            log.error("Failed to check git status", e);
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CHECK_FAILED, e.getMessage()));
            result.hasAuthenticationIssue = true;
        }

        return result;
    }

    private static void checkLocalStatus(Status status, GitStatusCheck result) {
        // Kiểm tra những thay đổi không được cam kết
        result.hasUncommittedChanges = status.hasUncommittedChanges();

        // Detailed change type assignment
        result.added.addAll(status.getAdded());
        result.changed.addAll(status.getChanged());
        result.modified.addAll(status.getModified());
        result.missing.addAll(status.getMissing());
        result.removed.addAll(status.getRemoved());
        result.untracked.addAll(status.getUntracked());
        result.conflicting.addAll(status.getConflicting());

        // Kiểm tra xem có thể gửi được không
        int totalChanges = result.added.size() + result.changed.size() + result.modified.size()
                + result.missing.size() + result.removed.size() + result.untracked.size();
        result.canCommit = totalChanges > 0 && result.conflicting.isEmpty();
    }

    private static void checkRemoteStatus(Git git, String workspacePath, GitStatusCheck result,
                                          CredentialsProvider credentialsProvider,
                                          SshCredentialsProvider sshCredentialsProvider) {
        try {
            String currentBranch = git.getRepository().getBranch();
            String tracking = git.getRepository().getConfig()
                    .getString("branch", currentBranch, "merge");

            // Kiểm tra xem có kho lưu trữ từ xa không
            var remotes = git.remoteList().call();
            result.hasRemoteRepository = !remotes.isEmpty();

            if (!result.hasRemoteRepository) {
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_NO_REMOTE_REPO));
                result.canPull = false;
                result.canPush = false;
                return;
            }

            // Thiết lập một nhà kho từ xaURL
            result.remoteUrl = remotes.get(0).getURIs().get(0).toString();

            result.hasUpstreamBranch = tracking != null;

            if (!result.hasUpstreamBranch) {
                // Có một kho lưu trữ từ xa nhưng không có nhánh theo dõi nào được thiết lập (điển hình init loại tình huống không gian làm việc)
                result.isInitTypeWorkspace = true;
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_NO_UPSTREAM_BRANCH));
                result.canPull = false;

                // Đối với init các loại, cần phải được kiểm tra các xung đột tiềm ẩn
                checkInitTypeConflicts(git, result, currentBranch, credentialsProvider, sshCredentialsProvider);
                return;
            }

            // Lấy tên kho từ xa
            String remoteName = git.getRepository().getConfig()
                    .getString("branch", currentBranch, "remote");
            if (remoteName == null) {
                remoteName = "origin";
            }

            // Lấy tên chi nhánh từ xa
            String remoteBranchName = tracking;
            if (remoteBranchName.startsWith(REFS_HEADS_PREFIX)) {
                remoteBranchName = remoteBranchName.substring(REFS_HEADS_PREFIX.length());
            }
            result.remoteBranch = remoteName + "/" + remoteBranchName;

            // So sánh các chi nhánh địa phương và từ xa
            String localRef = REFS_HEADS_PREFIX + currentBranch;
            String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;

            ObjectId localId = git.getRepository().resolve(localRef);
            ObjectId remoteId = git.getRepository().resolve(remoteRef);

            result.isEmptyLocalRepository = localId == null;

            // Kiểm tra các cam kết cục bộ và từ xa
            checkLocalAndRemoteCommit(git, result, localId, remoteId, currentBranch);

            // Hãy thử fetch Trạng thái từ xa mới nhất (để phát hiện chính xác hơn)
            boolean fetchSuccess = false;
            Boolean remoteHasBranches = null;
            try {
                var fetchCommand = git.fetch().setDryRun(false);
                fetchCommand.setTimeout(GIT_OPERATION_TIMEOUT); // Đặt thời gian chờ
                // Đặt thông tin xác thực - Hỗ trợSSHvà các phương thức xác thực khác
                if (sshCredentialsProvider != null) {
                    fetchCommand.setTransportConfigCallback(sshCredentialsProvider);
                } else if (credentialsProvider != null) {
                    fetchCommand.setCredentialsProvider(credentialsProvider);
                }
                var fetchResult = fetchCommand.call();
                remoteHasBranches = fetchResult.getAdvertisedRefs().stream()
                        .anyMatch(ref -> ref.getName().startsWith(REFS_HEADS_PREFIX));
                log.debug("Fetched latest remote status for conflict detection");
                fetchSuccess = true;
                result.canConnectToRemote = true;

                // fetch Giải quyết lại chi nhánh từ xa sau khi thành côngID
                remoteId = git.getRepository().resolve(remoteRef);
                // Số liệu thống kê nộp trước đó được sử dụng fetch Nhánh theo dõi từ xa trước đây; khi có một cam kết mới trên điều khiển từ xa.
                // Điều này phải dựa vào fetch Các nhánh tiếp theo sẽ được tính toán lại, nếu không sẽ bị đánh giá sai là không cần kéo khi mở lần đầu.
                checkLocalAndRemoteCommit(git, result, localId, remoteId, currentBranch);

                // Kiểm tra các loại thay đổi tập tin của nhánh từ xa và nhánh cục bộ
                if (localId != null && remoteId != null) {
                    try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                        RevCommit localCommit = revWalk.parseCommit(localId);
                        RevCommit remoteCommit = revWalk.parseCommit(remoteId);
                        RevTree localTree = localCommit.getTree();
                        RevTree remoteTree = remoteCommit.getTree();

                        CanonicalTreeParser localTreeIter = new CanonicalTreeParser();
                        localTreeIter.reset(git.getRepository().newObjectReader(), localTree);
                        CanonicalTreeParser remoteTreeIter = new CanonicalTreeParser();
                        remoteTreeIter.reset(git.getRepository().newObjectReader(), remoteTree);

                        // Lấy merge base
                        ObjectId mergeBaseId = findMergeBase(git, localId, remoteId);
                        if (mergeBaseId != null) {
                            RevCommit mergeBaseCommit = revWalk.parseCommit(mergeBaseId);
                            RevTree baseTree = mergeBaseCommit.getTree();
                            CanonicalTreeParser baseTreeIter = new CanonicalTreeParser();
                            baseTreeIter.reset(git.getRepository().newObjectReader(), baseTree);

                            // Sử dụng merge base và remote làm diff，Lấy remote sự bổ sung mới/Ôn lại/xóa bỏ
                            List<DiffEntry> diffEntries = git.diff()
                                    .setOldTree(baseTreeIter)
                                    .setNewTree(remoteTreeIter)
                                    .call();
                            result.remoteAdded.clear();
                            result.remoteModified.clear();
                            result.remoteRemoved.clear();
                            for (DiffEntry entry : diffEntries) {
                                switch (entry.getChangeType()) {
                                    case ADD:
                                        result.remoteAdded.add(entry.getNewPath());
                                        break;
                                    case MODIFY:
                                        result.remoteModified.add(entry.getNewPath());
                                        break;
                                    case DELETE:
                                        result.remoteRemoved.add(entry.getOldPath());
                                        break;
                                    case RENAME:
                                        result.remoteRenamed.add(entry.getOldPath() + " -> " + entry.getNewPath());
                                        break;
                                    case COPY:
                                        result.remoteCopied.add(entry.getNewPath());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else {
                            // Không merge base，Giữ nguyên logic ban đầu (direct diff remote và local）
                            List<DiffEntry> diffEntries = git.diff()
                                    .setOldTree(remoteTreeIter)
                                    .setNewTree(localTreeIter)
                                    .call();
                            for (DiffEntry entry : diffEntries) {
                                switch (entry.getChangeType()) {
                                    case ADD:
                                        result.remoteAdded.add(entry.getNewPath());
                                        break;
                                    case MODIFY:
                                        result.remoteModified.add(entry.getNewPath());
                                        break;
                                    case DELETE:
                                        result.remoteRemoved.add(entry.getOldPath());
                                        break;
                                    case RENAME:
                                        result.remoteRenamed.add(entry.getOldPath() + " -> " + entry.getNewPath());
                                        break;
                                    case COPY:
                                        result.remoteCopied.add(entry.getNewPath());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to diff remote and local branch", e);
                        result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_DETECT_REMOTE_CHANGES, e.getMessage()));
                    }
                }
            } catch (RefNotAdvertisedException e) {
                log.debug("Remote branch does not exist: {}", e.getMessage());
                result.isFirstPush = true;
                result.canConnectToRemote = true;
            } catch (Exception fetchEx) {
                log.debug("Failed to fetch remote status, using cached refs: {}", fetchEx.getMessage());
                result.canConnectToRemote = false;
                // Khi trạng thái từ xa không khả dụng, trạng thái đó phải được đánh dấu rõ ràng là không xác định và việc thiếu bộ nhớ đệm không thể bị đánh giá sai là kho trống từ xa.
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_GET_LATEST_REMOTE, fetchEx.getMessage()));
                if (credentialsProvider != null || sshCredentialsProvider != null) {
                    result.hasAuthenticationIssue = true;
                } else {
                    log.debug("No credentials provided for fetch, skipping remote status update");
                }
            }

            // Thiết lập tính khả thi trong hoạt động
            determineOperationCapabilities(result, localId, remoteId, fetchSuccess, remoteHasBranches);

            // Thực hiện phát hiện xung đột
            performIntelligentConflictDetection(git, workspacePath, result, localId, remoteId);

        } catch (Exception e) {
            log.warn("Failed to check remote status", e);
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_CHECK_REMOTE, e.getMessage()));
            result.hasAuthenticationIssue = true;
            // Khi xảy ra lỗi, hãy thiết lập thận trọng khả năng vận hành
            result.canPull = false;
            result.canPush = false;
        }
    }

    private static void checkLocalAndRemoteCommit(Git git, GitStatusCheck result, ObjectId localId, ObjectId remoteId, String currentBranch) {
        // This method will be in fetch Được gọi một lần trước và sau, kết quả so sánh từ xa cũ trước tiên sẽ bị xóa rồi tính toán lại.
        result.localCommitsAhead = 0;
        result.remoteCommitsBehind = 0;
        result.hasRemoteCommits = false;
        result.needsForcePush = false;
        result.needsForcePull = false;
        result.isFirstPush = false;
        result.isRemoteRepositoryEmpty = false;

        if (localId != null) {
            try {
                // Kiểm tra xem kho có trống không
                Iterable<RevCommit> localCommits = git.log().setMaxCount(1).call();
                result.hasLocalCommits = localCommits.iterator().hasNext();

                int localCommitsAhead = 0;
                if (result.hasLocalCommits && remoteId != null) {
                    // Đếm số lượng cam kết cục bộ trước điều khiển từ xa
                    Iterable<RevCommit> aheadCommits = git.log()
                            .addRange(remoteId, localId)
                            .call();
                    for (RevCommit ignored : aheadCommits) {
                        localCommitsAhead++;
                    }
                    result.localCommitsAhead = localCommitsAhead;
                    log.info("[GitStatusCheck] Chi nhánh hiện tại: {}，Không được đẩy cục bộ commit Số lượng: {}", currentBranch, localCommitsAhead);

                    // Đếm số lần xác nhận từ xa trước cục bộ
                    int remoteCommitsBehind = 0;
                    Iterable<RevCommit> behindCommits = git.log()
                            .addRange(localId, remoteId)
                            .call();
                    for (RevCommit ignored : behindCommits) {
                        remoteCommitsBehind++;
                    }
                    result.remoteCommitsBehind = remoteCommitsBehind;
                    result.hasRemoteCommits = remoteCommitsBehind > 0;

                    // Đặt cờ yêu cầu hành động bắt buộc
                    result.needsForcePush = result.hasRemoteCommits && result.localCommitsAhead > 0;
                    result.needsForcePull = result.hasUncommittedChanges && result.hasRemoteCommits;

                } else if (result.hasLocalCommits) {
                    // Nhánh theo dõi từ xa chưa tồn tại; liệu đây có phải là lần đẩy đầu tiên hay không sẽ phải chờ fetch Đánh giá sau khi hoàn thành.
                    Iterable<RevCommit> allCommits = git.log().call();
                    for (RevCommit ignored : allCommits) {
                        localCommitsAhead++;
                    }
                    result.localCommitsAhead = localCommitsAhead;
                    log.info("[GitStatusCheck] Chi nhánh hiện tại: {}，Nhánh từ xa không tồn tại, mọi thứ đều cục bộ commit Không đẩy, số lượng: {}", currentBranch, localCommitsAhead);
                }
            } catch (org.eclipse.jgit.api.errors.NoHeadException e) {
                // Kho trống, không có cam kết
                log.debug("Repository has no HEAD (empty repository): {}", e.getMessage());
                result.hasLocalCommits = false;
                result.localCommitsAhead = 0;
                result.isEmptyLocalRepository = true;
            } catch (Exception e) {
                log.warn("Failed to count commits", e);
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_COUNT_COMMITS, e.getMessage()));
            }
        }
    }

    /**
     * Xác định khả năng hoạt động dựa trên trạng thái cục bộ và từ xa
     */
    private static void determineOperationCapabilities(GitStatusCheck result, ObjectId localId, ObjectId remoteId,
                                                       boolean fetchSuccess, Boolean remoteHasBranches) {
        // Pull Đánh giá hoạt động:
        // 1. Chi nhánh từ xa phải tồn tại
        // 2. nếu fetch Nếu thất bại, không nên kéo
        // 3. Phải có một nhánh ngược dòng
        // 4. Có những cam kết mới trên điều khiển từ xa
        result.canPull = remoteId != null && fetchSuccess && result.hasUpstreamBranch && result.remoteCommitsBehind > 0;

        // Kiểm tra trạng thái kho lưu trữ từ xa và thêm các đề xuất tương ứng
        if (remoteId == null && result.canConnectToRemote) {
            // Việc không có tham chiếu theo dõi đến chi nhánh hiện tại không có nghĩa là kho trống; kho chỉ được đánh dấu là trống nếu không có chi nhánh nào ở phía xa.
            result.isRemoteRepositoryEmpty = Boolean.FALSE.equals(remoteHasBranches);
            result.isFirstPush = result.hasLocalCommits;
            if (result.isRemoteRepositoryEmpty) {
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_REPO_EMPTY));
            }
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_NO_SAME_BRANCH));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_WAITING_FIRST_PUSH));
        } else if (remoteId == null) {
            // fetch Trạng thái từ xa không xác định tại thời điểm xảy ra lỗi, nó không thể được coi là kho lưu trữ trống và không thể cho phép đẩy.
            result.isRemoteRepositoryEmpty = false;
            result.isFirstPush = false;
            result.canPull = false;
            result.canPush = false;
        } else {
            result.isRemoteRepositoryEmpty = false;
            result.isFirstPush = false;
            if (!result.canConnectToRemote) {
                result.canPull = false;
                result.canPush = false;
            }
        }

        // Push Đánh giá hoạt động:
        // 1. Phải có hồ sơ địa phương
        // 2. Không thể có những thay đổi không được cam kết (trừ khi có init loại lần đẩy đầu tiên)
        if (!result.hasLocalCommits) {
            result.canPush = false;
        } else if (result.hasUncommittedChanges) {
            // Khi có những thay đổi không được cam kết, chúng chỉ có thể được đẩy lên trong những trường hợp đặc biệt
            result.canPush = result.isInitTypeWorkspace; // init Loại lần đẩy đầu tiên có thể được cho phép
        } else if (result.localCommitsAhead > 0) { // Có những cam kết cục bộ chưa được thực hiện
            result.canPush = true;
        }

        // Đặt cờ yêu cầu hành động bắt buộc
        result.needsForcePush = result.hasRemoteCommits && result.localCommitsAhead > 0;
        result.needsForcePull = result.hasUncommittedChanges && result.hasRemoteCommits;

        // Nếu có những cam kết mới trên điều khiển từ xa, việc đẩy có thể không thành công
        if (result.hasRemoteCommits && result.canPush) {
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_HAS_NEW_COMMITS));
        }

        // Nếu kho lưu trữ cục bộ trống, không thể thực hiện thao tác nào
        if (localId == null) {
            result.isEmptyLocalRepository = true;
            result.canPush = false;
            if (remoteId != null) {
                result.canPull = fetchSuccess; // Kho lưu trữ cục bộ trống có thể lấy nội dung từ xa
            }
        }

        // Khi trạng thái từ xa không được xác nhận, nó bị cấm hiển thị có thể đẩy hoặc kéo dựa trên kết quả được lưu trong bộ nhớ cache.
        if (result.hasRemoteRepository && !result.canConnectToRemote) {
            result.canPull = false;
            result.canPush = false;
        }
    }

    /**
     * nghiên cứu init Xung đột tiềm ẩn trong không gian làm việc kiểu
     * Khi kho lưu trữ cục bộ đã có nội dung nhưng cần được liên kết với một nhánh từ xa hiện có
     */
    private static void checkInitTypeConflicts(Git git, GitStatusCheck result, String currentBranch,
                                               CredentialsProvider credentialsProvider,
                                               SshCredentialsProvider sshCredentialsProvider) {
        try {
            // Kiểm tra xem có cam kết cục bộ hay không
            boolean hasLocalCommits = false;
            ObjectId localId = null;
            try {
                localId = git.getRepository().resolve("HEAD");
                hasLocalCommits = localId != null;
            } catch (Exception e) {
                log.debug("Repository has no HEAD (empty repository): {}", e.getMessage());
            }
            result.hasLocalCommits = hasLocalCommits;

            int localCommitsAhead = 0;
            if (hasLocalCommits) {
                // fetch Thông tin chi nhánh từ xa
                ObjectId remoteId = null;
                try {
                    var fetchCommand = git.fetch().setDryRun(true);
                    fetchCommand.setTimeout(GIT_OPERATION_TIMEOUT); // Đặt thời gian chờ
                    if (sshCredentialsProvider != null) {
                        fetchCommand.setTransportConfigCallback(sshCredentialsProvider);
                    } else if (credentialsProvider != null) {
                        fetchCommand.setCredentialsProvider(credentialsProvider);
                    }
                    fetchCommand.call();
                    String remoteRef = "refs/remotes/origin/" + currentBranch;
                    remoteId = git.getRepository().resolve(remoteRef);
                } catch (Exception e) {
                    log.debug("Cannot fetch remote info for conflict check", e);
                }

                if (remoteId != null) {
                    // Về mặt thống kê, cục bộ đi trước điều khiển từ xa commit con số
                    try {
                        Iterable<RevCommit> aheadCommits = git.log().addRange(remoteId, localId).call();
                        for (RevCommit ignored : aheadCommits) {
                            localCommitsAhead++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to count ahead commits", e);
                    }
                    log.info("[GitStatusCheck] Chi nhánh hiện tại: {}，Không được đẩy cục bộ commit Số lượng: {}", currentBranch, localCommitsAhead);
                } else {
                    // Nhánh từ xa không tồn tại, mọi thứ đều cục bộ commit Không ai đã được đẩy
                    try {
                        Iterable<RevCommit> allCommits = git.log().call();
                        for (RevCommit ignored : allCommits) {
                            localCommitsAhead++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to count local commits", e);
                    }
                    log.info("[GitStatusCheck] Chi nhánh hiện tại: {}，Nhánh từ xa không tồn tại, mọi thứ đều cục bộ commit Không đẩy, số lượng: {}", currentBranch, localCommitsAhead);
                }
            }
            result.localCommitsAhead = localCommitsAhead;

            // Hãy thử fetch Thông tin chi nhánh từ xa để kiểm tra xung đột
            try {
                // Cố gắng lấy thông tin chi nhánh từ xa (không gian làm việc sẽ không được sửa đổi)
                var fetchCommand = git.fetch().setDryRun(true);
                fetchCommand.setTimeout(GIT_OPERATION_TIMEOUT); // Đặt thời gian chờ

                // Đặt thông tin xác thực - Hỗ trợSSHvà các phương thức xác thực khác
                if (sshCredentialsProvider != null) {
                    fetchCommand.setTransportConfigCallback(sshCredentialsProvider);
                } else if (credentialsProvider != null) {
                    fetchCommand.setCredentialsProvider(credentialsProvider);
                }

                fetchCommand.call();
                result.canConnectToRemote = true;

                // Kiểm tra xem điều khiển từ xa có nhánh có cùng tên không
                String remoteRef = "refs/remotes/origin/" + currentBranch;
                ObjectId remoteId = git.getRepository().resolve(remoteRef);

                if (remoteId != null) {
                    // Nhánh từ xa đã có cùng tên và có thể xảy ra xung đột
                    result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_REMOTE_BRANCH_EXISTS));
                    result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_BACKUP_SUGGESTION));

                    // Kiểm tra xung đột tập tin cụ thể
                    checkFileConflicts(git, result, remoteId);
                } else {
                    // Remote không có nhánh cùng tên nên tương đối an toàn
                    result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_SAFE_FIRST_PUSH));
                }

            } catch (org.eclipse.jgit.api.errors.RefNotAdvertisedException e) {
                // Chi nhánh từ xa không tồn tại, điều này là bình thường
                log.debug("Remote branch does not exist: {}", e.getMessage());
                result.canConnectToRemote = true;
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_SAFE_FIRST_PUSH));
            } catch (Exception fetchEx) {
                // fetch Lỗi có thể là sự cố mạng hoặc sự cố xác thực
                log.debug("Cannot fetch remote info for conflict check", fetchEx);
                result.canConnectToRemote = false;
                result.hasAuthenticationIssue = true;
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_CANNOT_GET_REMOTE_INFO));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_INIT_CHECK_NETWORK_AUTH));
            }

            // Bạn không thể đẩy khi kho lưu trữ cục bộ trống hoặc không có cam kết cục bộ.
            result.canPush = result.canConnectToRemote && result.hasLocalCommits
                    && result.localCommitsAhead > 0 && !result.hasUncommittedChanges;


        } catch (Exception e) {
            log.warn("Failed to check init type conflicts", e);
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CHECK_INIT_CONFLICTS_FAILED, e.getMessage()));
            result.canPush = false;
        }
    }

    /**
     * Kiểm tra xung đột cấp độ tập tin
     */
    private static void checkFileConflicts(Git git, GitStatusCheck result, ObjectId remoteCommitId) {
        try {
            // Nhận danh sách các tập tin cục bộ
            ObjectId localCommitId = git.getRepository().resolve("HEAD");
            if (localCommitId == null) return;

            // So sánh sự khác biệt của tập tin cục bộ và từ xa
            var diffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), remoteCommitId))
                    .setNewTree(prepareTreeParser(git.getRepository(), localCommitId))
                    .call();

            List<String> conflictFiles = new ArrayList<>();

            for (var diff : diffs) {
                String fileName = diff.getNewPath();
                // Kiểm tra xem chúng có phải là phiên bản khác nhau của cùng một tệp không (có thể xảy ra xung đột)
                if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                    conflictFiles.add(fileName);
                }
            }

            // Đặt trạng thái xung đột
            result.hasFileConflicts = !conflictFiles.isEmpty();
            result.conflictingFiles.addAll(conflictFiles);

            if (result.hasFileConflicts) {
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FILE_CONFLICTS_DETECTED, conflictFiles.size()));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CONFLICT_FILES, String.join(", ", conflictFiles.subList(0, Math.min(5, conflictFiles.size())))));
                if (conflictFiles.size() > 5) {
                    result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_MORE_FILES_CONFLICT, (conflictFiles.size() - 5)));
                }
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_GIT_MERGE_SUGGESTION));
            }

        } catch (Exception e) {
            log.debug("Failed to check file conflicts", e);
            // Lỗi kiểm tra xung đột tệp không ảnh hưởng đến chức năng chính
        }
    }

    /**
     * Phương thức trợ giúp: Chuẩn bị trình phân tích cú pháp cây
     */
    private static AbstractTreeIterator prepareTreeParser(
            Repository repository, ObjectId objectId) throws Exception {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            return treeParser;
        } catch (Exception ex) {
            log.warn("Failed to prepare tree parser", ex);
            throw ex;
        }
    }

    private static void generateSuggestions(GitStatusCheck result, String operationType) {
        // Tạo đề xuất dựa trên loại hành động
        switch (operationType.toLowerCase()) {
            case "commit":
                generateCommitSuggestions(result);
                break;
            case "push":
                generatePushSuggestions(result);
                break;
            case "pull":
                generatePullSuggestions(result);
                break;
            default:
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_UNKNOWN_OPERATION_TYPE, operationType));
        }
    }

    private static void generateCommitSuggestions(GitStatusCheck result) {
        if (result.canCommit) {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CAN_COMMIT_CHANGES));
            if (result.hasUncommittedChanges) {
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_MODIFIED_FILES_WILL_BE_COMMITTED));
            }
        } else {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_NO_CHANGES_TO_COMMIT));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_ALL_FILES_UP_TO_DATE));
        }
    }

    private static void generatePushSuggestions(GitStatusCheck result) {
        if (result.hasRemoteRepository && !result.canConnectToRemote) {
            return;
        }
        if (result.hasUncommittedChanges) {
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_UNCOMMITTED_CHANGES_CANNOT_PUSH));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_COMMIT_FIRST_THEN_PUSH));
            return;
        }

        // Phân tích các kịch bản đẩy
        if (result.isInitTypeWorkspace || result.isFirstPush || result.isRemoteRepositoryEmpty) {
            // Đẩy hoặc khởi tạo một loại không gian làm việc lần đầu tiên
            handleFirstPushSuggestions(result);
        } else if (result.needsForcePush) {
            // Các tình huống cần lực đẩy (có lịch sử phân kỳ)
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_DIVERGED_HISTORY));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_LOCAL_AHEAD_COMMITS, result.localCommitsAhead));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_AHEAD_COMMITS, result.remoteCommitsBehind));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PULL_FIRST_OR_FORCE_PUSH));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FORCE_PUSH_OVERWRITE_COMMITS, result.remoteCommitsBehind));
        } else if (result.hasRemoteCommits) {
            // Có những cam kết mới trên điều khiển từ xa, nhưng việc hợp nhất có thể được chuyển tiếp nhanh
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_HAS_NEW_COMMITS_WARN));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_AHEAD_COMMITS_COUNT, result.remoteCommitsBehind));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PULL_FIRST_THEN_PUSH));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_AVOID_PUSH_CONFLICTS));
        } else {
            // Tình huống đẩy bình thường
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_SAFE_PUSH_LOCAL_COMMITS, result.localCommitsAhead));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_SYNC_AFTER_PUSH));
        }
    }

    /**
     * Xử lý các đề xuất cho lần đẩy đầu tiên
     */
    private static void handleFirstPushSuggestions(GitStatusCheck result) {
        if (result.hasFileConflicts) {
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FIRST_PUSH_OVERWRITE_WARNING));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FILES_MAY_CONFLICT, result.conflictingFiles.size()));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_USE_FORCE_WITH_LEASE));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PULL_REMOTE_FIRST));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CONFIRM_OVERWRITE_FILES));
        } else if (result.isRemoteRepositoryEmpty) {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_EMPTY_SAFE_PUSH));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PUSH_COMMITS_TO_REMOTE, result.localCommitsAhead));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_AUTO_SET_UPSTREAM));
        } else {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FIRST_PUSH_DETECTED));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PUSH_COMMITS_COUNT, result.localCommitsAhead));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_SET_UPSTREAM_TRACKING));
        }
    }

    private static void generatePullSuggestions(GitStatusCheck result) {
        if (result.hasRemoteRepository && !result.canConnectToRemote) {
            return;
        }
        // Sử dụng trực tiếp các thuộc tính boolean thay vì phán đoán văn bản
        if (!result.hasUpstreamBranch) {
            // Kiểm tra xem có init loại và các tình huống xung đột có thể xảy ra
            if (result.hasFileConflicts) {
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_PULL_FILE_CONFLICTS));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_MANUAL_HANDLE_CONFLICTS));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_BACKUP_LOCAL_FILES));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_GIT_FETCH_ORIGIN));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_MANUAL_MERGE_FILES));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CREATE_MERGE_COMMIT));
            } else {
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CANNOT_PULL_NO_UPSTREAM));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CONFIG_REMOTE_FIRST));
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FIRST_PUSH_ESTABLISH_TRACKING));
            }
            return;
        }

        // Kiểm tra trạng thái kho trống trực tiếp bằng cách sử dụng thuộc tính boolean
        if (result.isRemoteRepositoryEmpty) {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_REMOTE_REPO_STATUS_EMPTY));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CAN_TRY_PULL_NO_CONTENT));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_PUSH_LOCAL_CONTENT_FIRST));
        }

        if (result.hasUncommittedChanges) {
            result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_UNCOMMITTED_PULL_CONFLICTS));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_COMMIT_OR_STASH_FIRST));
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_FORCE_PULL_LOSE_CHANGES));
        } else if (!result.hasRemoteCommits && !result.isRemoteRepositoryEmpty) {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_LOCAL_IS_UP_TO_DATE));
        } else if (result.hasRemoteCommits) {
            result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_SAFE_PULL_REMOTE_COMMITS, result.remoteCommitsBehind));
        }
    }

    /**
     * Thực hiện phát hiện xung đột
     * Bằng cách phân tích các thay đổi cục bộ và từ xa, người ta xác định được liệu có xung đột thực tế hay không và liệu chúng có thể được hợp nhất tự động hay không
     */
    private static void performIntelligentConflictDetection(Git git, String workspacePath, GitStatusCheck result, ObjectId localId, ObjectId remoteId) {
        try {
            // Nếu kho lưu trữ cục bộ hoặc từ xa trống, việc phát hiện xung đột thông minh không thể xảy ra
            if (localId == null || remoteId == null) {
                result.hasActualConflicts = false;
                result.canAutoMerge = false;
                return;
            }

            // Tìm các cam kết cơ sở chung (merge base）
            ObjectId mergeBase = findMergeBase(git, localId, remoteId);

            if (mergeBase == null) {
                // Nếu không có điểm chung, đó có thể là một lịch sử hoàn toàn khác
                result.hasActualConflicts = true;
                result.canAutoMerge = false;
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_NO_COMMON_HISTORY));
                return;
            }

            // nếumerge baseTương đương với điều khiển từ xaID，Giải thích rằng điều khiển từ xa là một tập hợp con của điều khiển cục bộ và có thể được đẩy về phía trước nhanh chóng
            if (mergeBase.equals(remoteId)) {
                result.hasActualConflicts = false;
                result.canAutoMerge = true;
                log.info("Nhánh cục bộ là một tập hợp con của điều khiển từ xa và có thể được đẩy nhanh về phía trước");
                return;
            }

            if (mergeBase.equals(localId) && !result.hasUncommittedChanges) {
                result.hasActualConflicts = false;
                result.canAutoMerge = true;
                return;
            }

            // Phân tích xung đột ở cấp độ tệp
            analyzeFileConflicts(git, workspacePath, result, mergeBase, localId, remoteId);

        } catch (Exception e) {
            log.debug("Phát hiện xung đột thông minh không thành công", e);
            result.hasActualConflicts = false;
            result.canAutoMerge = false;
        }
    }

    /**
     * Tìm cơ sở hợp nhất của hai lần xác nhận
     */
    private static ObjectId findMergeBase(Git git, ObjectId commit1, ObjectId commit2) {
        try {
            try (RevWalk walk = new RevWalk(git.getRepository())) {
                RevCommit c1 = walk.parseCommit(commit1);
                RevCommit c2 = walk.parseCommit(commit2);

                walk.setRevFilter(org.eclipse.jgit.revwalk.filter.RevFilter.MERGE_BASE);
                walk.markStart(c1);
                walk.markStart(c2);

                RevCommit mergeBase = walk.next();
                return mergeBase != null ? mergeBase.getId() : null;
            }
        } catch (Exception e) {
            log.debug("Failed to find merge base", e);
            return null;
        }
    }

    /**
     * Phân tích xung đột ở cấp độ tệp
     */
    private static void analyzeFileConflicts(Git git, String workspacePath, GitStatusCheck result, ObjectId mergeBase,
                                             ObjectId localId, ObjectId remoteId) {
        try {
            // Nhận từmerge baseThay đổi về mặt địa phương
            var localDiffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), mergeBase))
                    .setNewTree(prepareTreeParser(git.getRepository(), localId))
                    .call();

            // Nhận từmerge baseThay đổi từ xa
            var remoteDiffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), mergeBase))
                    .setNewTree(prepareTreeParser(git.getRepository(), remoteId))
                    .call();

            // Phân tích các tình huống xung đột
            List<String> localChangedFiles = new ArrayList<>();
            List<String> remoteChangedFiles = new ArrayList<>();
            List<String> conflictFiles = new ArrayList<>();
            List<String> newFiles = new ArrayList<>();

            // Thu thập các tập tin thay đổi cục bộ
            for (DiffEntry diff : localDiffs) {
                String filePath = diff.getNewPath();
                localChangedFiles.add(filePath);
                if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
                    newFiles.add(filePath);
                }
            }

            // Thu thập các tập tin được thay đổi từ xa và kiểm tra xung đột
            for (DiffEntry diff : remoteDiffs) {
                String filePath = diff.getNewPath();
                remoteChangedFiles.add(filePath);
                if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
                    newFiles.add(filePath);
                }
            }

            // Phát hiện xung đột ở cấp độ nội dung
            Set<String> intersectFiles = new HashSet<>(localChangedFiles);
            intersectFiles.retainAll(remoteChangedFiles);
            Repository repo = git.getRepository();
            for (String filePath : intersectFiles) {
                String baseContent = getFileContent(repo, mergeBase, filePath);
                String localContent = getFileContent(repo, localId, filePath);
                String remoteContent = getFileContent(repo, remoteId, filePath);
                RawText baseText = new RawText(baseContent.getBytes(StandardCharsets.UTF_8));
                RawText localText = new RawText(localContent.getBytes(StandardCharsets.UTF_8));
                RawText remoteText = new RawText(remoteContent.getBytes(StandardCharsets.UTF_8));
                MergeAlgorithm mergeAlgorithm = new MergeAlgorithm();
                MergeResult<RawText> mergeResult = mergeAlgorithm.merge(RawTextComparator.DEFAULT, baseText, localText, remoteText);
                boolean hasConflict = false;
                List<ConflictBlock> conflictBlocks = new ArrayList<>();
                // Đi qua tất cảchunk，Nhóm theo khối xung đột
                List<MergeChunk> conflictChunkGroup = new ArrayList<>();
                for (MergeChunk chunk : mergeResult) {
                    if (chunk.getConflictState() != MergeChunk.ConflictState.NO_CONFLICT) {
                        hasConflict = true;
                        conflictChunkGroup.add(chunk);
                        // Các khối xung đột thường liền kề nhau3cá nhânchunk（base/local/remote），Nhưng đôi khi có thể không được, cần phải nhấnsourceIndexNhóm
                        if (conflictChunkGroup.size() == 3) {
                            int begin = conflictChunkGroup.get(0).getBegin();
                            int end = conflictChunkGroup.get(2).getEnd();
                            List<String> baseLines = new ArrayList<>();
                            List<String> localLines = new ArrayList<>();
                            List<String> remoteLines = new ArrayList<>();
                            for (MergeChunk c : conflictChunkGroup) {
                                int srcIdx = c.getSequenceIndex();
                                for (int line = c.getBegin(); line < c.getEnd(); line++) {
                                    if (srcIdx == 0) baseLines.add(baseText.getString(line));
                                    else if (srcIdx == 1) localLines.add(localText.getString(line));
                                    else if (srcIdx == 2) remoteLines.add(remoteText.getString(line));
                                }
                            }
                            conflictBlocks.add(new ConflictBlock(begin, end, baseLines, localLines, remoteLines));
                            conflictChunkGroup.clear();
                        }
                    } else {
                        // Không xung độtchunk，Xóa nhóm
                        conflictChunkGroup.clear();
                    }
                }
                if (hasConflict) {
                    conflictFiles.add(filePath);
                    result.conflictDetails.put(filePath, conflictBlocks);
                }
            }

            // Kiểm tra các thay đổi không được cam kết cục bộ
            var status = git.status().call();
            Set<String> uncommittedFiles = new HashSet<>();
            uncommittedFiles.addAll(status.getModified());
            uncommittedFiles.addAll(status.getUntracked());
            uncommittedFiles.addAll(status.getChanged());
            uncommittedFiles.addAll(status.getAdded());
            uncommittedFiles.addAll(status.getRemoved());
            uncommittedFiles.addAll(status.getMissing());
            for (String filePath : uncommittedFiles) {
                if (remoteChangedFiles.contains(filePath) && !conflictFiles.contains(filePath)) {
                    String baseContent = getFileContent(repo, mergeBase, filePath);
                    String localContent = FileUtil.readString(workspacePath + "/" + filePath, StandardCharsets.UTF_8);
                    String remoteContent = getFileContent(repo, remoteId, filePath);
                    RawText baseText = new RawText(baseContent.getBytes(StandardCharsets.UTF_8));
                    RawText localText = new RawText(localContent.getBytes(StandardCharsets.UTF_8));
                    RawText remoteText = new RawText(remoteContent.getBytes(StandardCharsets.UTF_8));
                    MergeAlgorithm mergeAlgorithm = new MergeAlgorithm();
                    MergeResult<RawText> mergeResult = mergeAlgorithm.merge(RawTextComparator.DEFAULT, baseText, localText, remoteText);
                    boolean hasConflict = false;
                    List<ConflictBlock> conflictBlocks = new ArrayList<>();
                    List<MergeChunk> conflictChunkGroup = new ArrayList<>();
                    for (MergeChunk chunk : mergeResult) {
                        if (chunk.getConflictState() != MergeChunk.ConflictState.NO_CONFLICT) {
                            hasConflict = true;
                            conflictChunkGroup.add(chunk);
                            if (conflictChunkGroup.size() == 3) {
                                int begin = conflictChunkGroup.get(0).getBegin();
                                int end = conflictChunkGroup.get(2).getEnd();
                                List<String> baseLines = new ArrayList<>();
                                List<String> localLines = new ArrayList<>();
                                List<String> remoteLines = new ArrayList<>();
                                for (MergeChunk c : conflictChunkGroup) {
                                    int srcIdx = c.getSequenceIndex();
                                    for (int line = c.getBegin(); line < c.getEnd(); line++) {
                                        if (srcIdx == 0) baseLines.add(baseText.getString(line));
                                        else if (srcIdx == 1) localLines.add(localText.getString(line));
                                        else if (srcIdx == 2) remoteLines.add(remoteText.getString(line));
                                    }
                                }
                                conflictBlocks.add(new ConflictBlock(begin, end, baseLines, localLines, remoteLines));
                                conflictChunkGroup.clear();
                            }
                        } else {
                            conflictChunkGroup.clear();
                        }
                    }
                    if (hasConflict) {
                        conflictFiles.add(filePath);
                        result.conflictDetails.put(filePath, conflictBlocks);
                    }
                }
            }

            // Đặt kết quả phát hiện
            result.hasActualConflicts = !conflictFiles.isEmpty();
            result.conflictingFiles.addAll(conflictFiles);

            // Xác định xem chỉ có các tập tin mới
            result.hasOnlyNewFiles = conflictFiles.isEmpty() && !newFiles.isEmpty();

            // Xác định xem đó có phải là một thay đổi không chồng chéo hay không
            result.hasNonOverlappingChanges = conflictFiles.isEmpty() &&
                    (!localChangedFiles.isEmpty() || !remoteChangedFiles.isEmpty());

            // Xác định xem có thể hợp nhất tự động hay không
            result.canAutoMerge = conflictFiles.isEmpty();

            // Thêm gợi ý chi tiết
            if (result.hasActualConflicts) {
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_ACTUAL_FILE_CONFLICTS, conflictFiles.size()));
                result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_CONFLICT_FILES_LIST,
                        String.join(", ", conflictFiles.subList(0, Math.min(3, conflictFiles.size())))));
                if (conflictFiles.size() > 3) {
                    result.warnings.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_MORE_CONFLICT_FILES, (conflictFiles.size() - 3)));
                }
                result.canAutoMerge = false;
            } else if (result.hasNonOverlappingChanges) {
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_NON_OVERLAPPING_CHANGES));
                result.canAutoMerge = true;
            } else if (result.hasOnlyNewFiles) {
                result.suggestions.add(I18nUtil.getMessage(MessageKeys.GIT_CONFLICT_DETECTOR_ONLY_NEW_FILES_SAFE));
                result.canAutoMerge = true;
            }

        } catch (Exception e) {
            log.debug("Phân tích xung đột tệp không thành công", e);
            result.hasActualConflicts = true;
            result.canAutoMerge = false;
        }
    }

    // Nhận được chỉ địnhcommitTải nội dung tập tin
    private static String getFileContent(Repository repo, ObjectId commitId, String filePath) {
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));
                if (!treeWalk.next()) {
                    return "";
                }
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(objectId);
                return new String(loader.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to get file content for {} at {}", filePath, commitId.name(), e);
            throw new RuntimeException(e);
        }
    }
}
