package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.core.assertion.AssertionData;
import com.nvhuong.devworkbench.performance.core.config.CsvDataSetData;
import com.nvhuong.devworkbench.performance.core.controller.ConditionData;
import com.nvhuong.devworkbench.performance.core.controller.LoopData;
import com.nvhuong.devworkbench.performance.core.controller.WhileData;
import com.nvhuong.devworkbench.performance.core.extractor.ExtractorData;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestSnapshot;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import com.nvhuong.devworkbench.performance.core.timer.TimerData;


import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;

public class PerformanceTreeNode {
    public String name;
    public NodeType type;
    public HttpRequestItem httpRequestItem; // ChỉREQUESTĐối với các nút
    public PerformanceRequestSnapshot requestSnapshot; // REQUESTNút là Không cóGUIYêu cầu ảnh chụp nhanh
    public ThreadGroupData threadGroupData; // Dữ liệu nhóm chủ đề
    public CsvDataSetData csvDataSetData;   // CSV Data Set cấu hình
    public LoopData loopData;             // Loop Dữ liệu điều khiển
    public ConditionData conditionData;   // Dữ liệu bộ điều khiển có điều kiện
    public WhileData whileData;   // While Dữ liệu điều khiển
    public AssertionData assertionData;   // Khẳng định dữ liệu
    public ExtractorData extractorData;   // Dữ liệu trích xuất
    public TimerData timerData;           // dữ liệu hẹn giờ
    public SsePerformanceData ssePerformanceData; // SSE Chỉ cấu hình kiểm tra căng thẳng SSE Các nút giai đoạn được sử dụng
    public boolean ssePerformanceEnabled; // REQUEST Các nút được chọn rõ ràng SSE Kịch bản kiểm tra căng thẳng
    public WebSocketPerformanceData webSocketPerformanceData; // WebSocket Cấu hình kiểm tra căng thẳng,REQUEST/WS Các nút được chia sẻ
    public boolean enabled = true;        // Có bật hay không thì bật theo mặc định
    public RequestExecutionScope requestExecutionScope; // REQUEST Ảnh chụp nhanh các biến nhóm cần thiết để thực thi nút

    public PerformanceTreeNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    public PerformanceTreeNode(String name, NodeType type, Object data) {
        this.name = name;
        this.type = type;
        switch (type) {
            case THREAD_GROUP -> this.threadGroupData = (ThreadGroupData) data;
            case CSV_DATA_SET -> this.csvDataSetData = (CsvDataSetData) data;
            case LOOP -> this.loopData = (LoopData) data;
            case CONDITION -> this.conditionData = (ConditionData) data;
            case WHILE -> this.whileData = (WhileData) data;
            case REQUEST -> this.httpRequestItem = (HttpRequestItem) data;
            case ASSERTION -> this.assertionData = (AssertionData) data;
            case EXTRACTOR -> this.extractorData = (ExtractorData) data;
            case TIMER -> this.timerData = (TimerData) data;
            case SIMPLE, ONCE_ONLY, SSE_CONNECT, SSE_READ, WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE, ROOT -> {
            }
        }
    }

    public Object getNodeData() {
        return switch (type) {
            case THREAD_GROUP -> threadGroupData;
            case CSV_DATA_SET -> csvDataSetData;
            case LOOP -> loopData;
            case CONDITION -> conditionData;
            case WHILE -> whileData;
            case REQUEST -> httpRequestItem;
            case ASSERTION -> assertionData;
            case EXTRACTOR -> extractorData;
            case TIMER -> timerData;
            case SIMPLE, ONCE_ONLY, SSE_CONNECT, SSE_READ, WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE, ROOT -> null;
            default -> null;
        };
    }

    public void setNodeData(Object data) {
        switch (type) {
            case THREAD_GROUP -> this.threadGroupData = (ThreadGroupData) data;
            case CSV_DATA_SET -> this.csvDataSetData = (CsvDataSetData) data;
            case LOOP -> this.loopData = (LoopData) data;
            case CONDITION -> this.conditionData = (ConditionData) data;
            case WHILE -> this.whileData = (WhileData) data;
            case REQUEST -> this.httpRequestItem = (com.nvhuong.devworkbench.request.model.HttpRequestItem) data;
            case ASSERTION -> this.assertionData = (AssertionData) data;
            case EXTRACTOR -> this.extractorData = (ExtractorData) data;
            case TIMER -> this.timerData = (TimerData) data;
            case SIMPLE, ONCE_ONLY, SSE_CONNECT, SSE_READ, WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE, ROOT -> {
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
