package com.laker.postman.model;

/**
 * Đóng gói thông tin trạng thái từ xa
 */
public class RemoteStatus {
    public boolean hasRemote = false;
    public boolean hasUpstream = false;
    public String remoteUrl;
    public String currentBranch;
    public String upstreamBranch;
}