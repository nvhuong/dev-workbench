package com.nvhuong.devworkbench.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Git Gửi thông tin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitCommitInfo {
    /**
     * nộpID（trọnSHA）
     */
    private String commitId;

    /**
     * Trình bày ngắn gọnID（trước đây8chút)
     */
    private String shortCommitId;

    /**
     * Gửi tin nhắn
     */
    private String message;

    /**
     * Tên tác giả
     */
    private String authorName;

    /**
     * Địa chỉ email của tác giả
     */
    private String authorEmail;

    /**
     * Thời gian gửi (dấu thời gian mili giây)
     */
    private long commitTime;

    /**
     * Tên người gửi
     */
    private String committerName;

    /**
     * Địa chỉ email của người gửi
     */
    private String committerEmail;
}
