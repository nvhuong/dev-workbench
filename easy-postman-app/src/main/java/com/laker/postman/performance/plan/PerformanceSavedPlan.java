package com.laker.postman.performance.plan;

import lombok.Builder;
import lombok.Value;
import com.laker.postman.performance.core.run.PerformanceServiceResource;

import java.util.UUID;
import java.util.List;

@Value
public class PerformanceSavedPlan {
    private static final String DEFAULT_NAME = "Test Plan";

    String id;
    String name;
    PerformancePlanDocument planDocument;
    boolean efficientMode;
    boolean trendEnabled;
    boolean reportRealtimeEnabled;
    PerformanceRemoteWorkerSettings remoteWorkerSettings;
    List<PerformanceServiceResource> serviceResources;

    @Builder
    public PerformanceSavedPlan(String id,
                                String name,
                                PerformancePlanDocument planDocument,
                                Boolean efficientMode,
                                Boolean trendEnabled,
                                Boolean reportRealtimeEnabled,
                                PerformanceRemoteWorkerSettings remoteWorkerSettings,
                                List<PerformanceServiceResource> serviceResources) {
        this.id = hasText(id) ? id.trim() : UUID.randomUUID().toString();
        this.name = hasText(name) ? name.trim() : DEFAULT_NAME;
        this.planDocument = planDocument;
        this.efficientMode = efficientMode == null || efficientMode;
        this.trendEnabled = trendEnabled == null || trendEnabled;
        this.reportRealtimeEnabled = reportRealtimeEnabled != null && reportRealtimeEnabled;
        this.remoteWorkerSettings = remoteWorkerSettings == null
                ? PerformanceRemoteWorkerSettings.disabled()
                : remoteWorkerSettings;
        this.serviceResources = PerformanceServiceResource.sanitize(serviceResources);
    }

    public static PerformanceSavedPlan fromConfiguration(String id,
                                                         String name,
                                                         PerformancePlanConfiguration configuration) {
        PerformancePlanConfiguration safeConfiguration = configuration == null
                ? PerformancePlanConfiguration.builder().build()
                : configuration;
        return PerformanceSavedPlan.builder()
                .id(id)
                .name(name)
                .planDocument(safeConfiguration.getPlanDocument())
                .efficientMode(safeConfiguration.isEfficientMode())
                .trendEnabled(safeConfiguration.isTrendEnabled())
                .reportRealtimeEnabled(safeConfiguration.isReportRealtimeEnabled())
                .remoteWorkerSettings(safeConfiguration.getRemoteWorkerSettings())
                .serviceResources(safeConfiguration.getServiceResources())
                .build();
    }

    public PerformancePlanConfiguration toConfiguration() {
        return PerformancePlanConfiguration.builder()
                .planDocument(planDocument)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .remoteWorkerSettings(remoteWorkerSettings)
                .serviceResources(serviceResources)
                .build();
    }

    public PerformanceSavedPlan withConfiguration(PerformancePlanConfiguration configuration) {
        return fromConfiguration(id, name, configuration);
    }

    public PerformanceSavedPlan withName(String newName) {
        return PerformanceSavedPlan.builder()
                .id(id)
                .name(newName)
                .planDocument(planDocument)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .remoteWorkerSettings(remoteWorkerSettings)
                .serviceResources(serviceResources)
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
