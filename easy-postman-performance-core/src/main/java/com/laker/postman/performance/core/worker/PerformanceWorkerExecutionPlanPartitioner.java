package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class PerformanceWorkerExecutionPlanPartitioner {

    public PerformanceTestPlan apply(PerformanceTestPlan plan, PerformanceWorkerAssignment assignment) {
        if (plan == null || assignment == null) {
            return plan;
        }
        // Không có người dùng ảo nào được chỉ định worker Một kế hoạch trống phải được thực thi và không thể khôi phục về một kế hoạch đầy đủ.
        if (assignment.getThreadGroups().isEmpty()) {
            return new PerformanceTestPlan(List.of());
        }
        List<PerformanceThreadGroupPlan> groups = new ArrayList<>();
        for (PerformanceWorkerThreadGroupAssignment threadGroupAssignment : assignment.getThreadGroups()) {
            int groupIndex = threadGroupAssignment.getThreadGroupIndex();
            if (groupIndex < 0 || groupIndex >= plan.getThreadGroups().size()
                    || threadGroupAssignment.getVirtualUserCount() <= 0) {
                continue;
            }
            PerformanceThreadGroupPlan source = plan.getThreadGroups().get(groupIndex);
            // CSV Dữ liệu không có ở đó master Kết thúc việc cắt bớt vật lý,worker Đi qua nguồn gốc người dùng ảo toàn cầu và tránh từng máy worker Tất cả từ đầu tiên 0 hàng đọc.
            groups.add(new PerformanceThreadGroupPlan(
                    source.getName(),
                    adjustThreadGroupData(source.getThreadGroupData(), threadGroupAssignment.getVirtualUserCount()),
                    source.getCsvDataSetData(),
                    source.getElements(),
                    threadGroupAssignment.getFirstVirtualUserIndex()
            ));
        }
        return new PerformanceTestPlan(groups);
    }

    private ThreadGroupData adjustThreadGroupData(ThreadGroupData source, int virtualUserCount) {
        ThreadGroupData target = source == null
                ? new ThreadGroupData()
                : JsonUtil.deepCopy(source, ThreadGroupData.class);
        target.normalize();
        int count = Math.max(1, virtualUserCount);
        int originalMax = Math.max(1, PerformanceWorkerAssignmentPlanner.maxThreadCount(source));
        switch (target.threadMode) {
            case FIXED -> target.numThreads = count;
            case RAMP_UP -> {
                target.rampUpStartThreads = scaledThreadCount(target.rampUpStartThreads, originalMax, count);
                target.rampUpEndThreads = count;
            }
            case SPIKE -> {
                target.spikeMinThreads = scaledThreadCount(target.spikeMinThreads, originalMax, count);
                target.spikeMaxThreads = count;
            }
            case STAIRS -> {
                target.stairsStartThreads = scaledThreadCount(target.stairsStartThreads, originalMax, count);
                target.stairsEndThreads = count;
                target.stairsStep = Math.max(1, Math.min(target.stairsStep, count));
            }
        }
        target.normalize();
        return target;
    }

    private static int scaledThreadCount(int originalValue, int originalMax, int assignedMax) {
        if (assignedMax <= 0) {
            return 0;
        }
        double ratio = Math.max(1, originalValue) / (double) Math.max(1, originalMax);
        return Math.max(1, Math.min(assignedMax, (int) Math.round(ratio * assignedMax)));
    }
}
