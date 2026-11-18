package com.nvhuong.devworkbench.performance.plan;

import lombok.Builder;
import lombok.Value;
import com.nvhuong.devworkbench.performance.core.run.PerformanceServiceResource;

import java.util.List;

@Value
public class PerformancePlanConfiguration {
    PerformancePlanDocument planDocument;
    boolean efficientMode;
    boolean trendEnabled;
    boolean reportRealtimeEnabled;
    PerformanceRemoteWorkerSettings remoteWorkerSettings;
    List<PerformanceServiceResource> serviceResources;

    @Builder
    public PerformancePlanConfiguration(PerformancePlanDocument planDocument,
                                        Boolean efficientMode,
                                        Boolean trendEnabled,
                                        Boolean reportRealtimeEnabled,
                                        PerformanceRemoteWorkerSettings remoteWorkerSettings,
                                        List<PerformanceServiceResource> serviceResources) {
        this.planDocument = planDocument;
        this.efficientMode = efficientMode == null || efficientMode;
        this.trendEnabled = trendEnabled == null || trendEnabled;
        this.reportRealtimeEnabled = reportRealtimeEnabled != null && reportRealtimeEnabled;
        this.remoteWorkerSettings = remoteWorkerSettings == null
                ? PerformanceRemoteWorkerSettings.disabled()
                : remoteWorkerSettings;
        this.serviceResources = PerformanceServiceResource.sanitize(serviceResources);
    }
}
