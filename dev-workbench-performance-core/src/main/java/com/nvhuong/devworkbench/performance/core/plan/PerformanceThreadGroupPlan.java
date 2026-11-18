package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.config.CsvDataSetData;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PerformanceThreadGroupPlan {
    private final String name;
    private final ThreadGroupData threadGroupData;
    private final CsvDataSetData csvDataSetData;
    private final List<PerformancePlanElement> elements;
    private final int virtualUserIndexOffset;

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      List<PerformancePlanElement> elements) {
        this(name, threadGroupData, null, elements);
    }

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      CsvDataSetData csvDataSetData,
                                      List<PerformancePlanElement> elements) {
        this(name, threadGroupData, csvDataSetData, elements, 0);
    }

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      CsvDataSetData csvDataSetData,
                                      List<PerformancePlanElement> elements,
                                      int virtualUserIndexOffset) {
        this.name = name;
        this.threadGroupData = PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
        if (this.threadGroupData != null) {
            this.threadGroupData.normalize();
        }
        this.csvDataSetData = PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
        this.virtualUserIndexOffset = Math.max(0, virtualUserIndexOffset);
    }

    public String getName() {
        return name;
    }

    public ThreadGroupData getThreadGroupData() {
        return PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
    }

    public CsvDataSetData getCsvDataSetData() {
        return PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
    }

    public Map<String, String> csvRowForVirtualUser(int virtualUserIndex) {
        // Trong quá trình thực thi phân tán virtualUserIndexOffset bày tỏ worker Điểm bắt đầu của người dùng toàn cầu được chỉ định,CSV Các hàng cũng bị ràng buộc bởi con số toàn cầu này.
        return csvDataSetData == null ? null : csvDataSetData.rowForVirtualUser(virtualUserIndexOffset + virtualUserIndex);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }

    public int getVirtualUserIndexOffset() {
        return virtualUserIndexOffset;
    }
}
