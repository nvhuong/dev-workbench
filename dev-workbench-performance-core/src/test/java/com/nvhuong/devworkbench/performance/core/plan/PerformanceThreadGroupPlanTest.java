package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.config.CsvDataSetData;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceThreadGroupPlanTest {

    @Test(description = "Khi chạy, chỉ những cái tương ứng mới được tìm nạp theo chỉ mục người dùng ảo CSV sao chép dòng")
    public void shouldReturnCsvRowForVirtualUserWithoutExposingPlanState() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                new CsvDataSetData(
                        "users.csv",
                        List.of("userId"),
                        List.of(Map.of("userId", "u1"), Map.of("userId", "u2"))
                ),
                List.of()
        );

        Map<String, String> firstRow = groupPlan.csvRowForVirtualUser(0);
        Map<String, String> wrappedRow = groupPlan.csvRowForVirtualUser(2);
        firstRow.put("userId", "mutated");

        assertEquals(firstRow.get("userId"), "mutated");
        assertEquals(wrappedRow.get("userId"), "u1");
        assertEquals(groupPlan.csvRowForVirtualUser(0).get("userId"), "u1");
        assertEquals(groupPlan.csvRowForVirtualUser(1).get("userId"), "u2");
    }

    @Test(description = "file-source CSV nên được nhấn worker Bức tranh lớn offset Đọc các tập tin cục bộ với cùng một đường dẫn")
    public void shouldLoadFileSourceCsvRowsForVirtualUserOffset() throws Exception {
        Path csvPath = Files.createTempFile("ep-performance-users", ".csv");
        Files.writeString(csvPath, "userId,token\nu0,t0\nu1,t1\nu2,t2\n");
        CsvDataSetData csvData = CsvDataSetData.file("users.csv", csvPath.toString());

        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                csvData,
                List.of(),
                1
        );

        assertEquals(groupPlan.csvRowForVirtualUser(0), Map.of("userId", "u1", "token", "t1"));
        assertEquals(groupPlan.csvRowForVirtualUser(1), Map.of("userId", "u2", "token", "t2"));
    }
}
