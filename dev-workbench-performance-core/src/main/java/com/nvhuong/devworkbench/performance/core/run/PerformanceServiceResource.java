package com.nvhuong.devworkbench.performance.core.run;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class PerformanceServiceResource {
    String serviceName;
    int instances;
    double cpuCores;
    double memoryGb;

    public PerformanceServiceResource(String serviceName, int instances, double cpuCores, double memoryGb) {
        this.serviceName = serviceName == null ? "" : serviceName.trim();
        this.instances = Math.max(1, instances);
        this.cpuCores = Double.isFinite(cpuCores) ? Math.max(0, cpuCores) : 0;
        this.memoryGb = Double.isFinite(memoryGb) ? Math.max(0, memoryGb) : 0;
    }

    public static List<PerformanceServiceResource> sanitize(List<PerformanceServiceResource> resources) {
        if (resources == null) return List.of();
        List<PerformanceServiceResource> result = new ArrayList<>();
        for (PerformanceServiceResource resource : resources) {
            if (resource != null && !resource.getServiceName().isBlank()) result.add(resource);
        }
        return List.copyOf(result);
    }
}
