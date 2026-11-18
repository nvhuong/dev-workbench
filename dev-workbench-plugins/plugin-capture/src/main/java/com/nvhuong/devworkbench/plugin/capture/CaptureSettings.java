package com.nvhuong.devworkbench.plugin.capture;

record CaptureSettings(String bindHost, int bindPort, boolean syncSystemProxy, String hostFilter, int maxFlows) {
}
