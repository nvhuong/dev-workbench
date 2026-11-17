package com.laker.postman.panel.sidebar;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class SidebarTabTest {

    @Test(description = "Thanh bên mặc định phải được định cấu hình như đã định cấu hình.Mock、Trình tự kiểm tra và dụng cụ")
    public void testDefaultOrder_ShouldPlaceMockServerAfterWorkspaces() {
        assertEquals(List.of(SidebarTab.values()), List.of(
                SidebarTab.COLLECTIONS,
                SidebarTab.ENVIRONMENTS,
                SidebarTab.WORKSPACES,
                SidebarTab.MOCK_SERVER,
                SidebarTab.FUNCTIONAL,
                SidebarTab.PERFORMANCE,
                SidebarTab.TOOLBOX,
                SidebarTab.HISTORY
        ));
    }

    @Test(description = "Cấu hình sắp xếp thanh bên sẽ bỏ qua các mục không xác định và trùng lặp và điền vào các mục còn thiếu")
    public void testResolveOrderedTabs_ShouldNormalizeConfiguredOrder() {
        List<SidebarTab> tabs = SidebarTab.resolveOrderedTabs(List.of(
                "TOOLBOX",
                "COLLECTIONS",
                "unknown",
                "TOOLBOX"
        ));

        assertEquals(tabs.get(0), SidebarTab.TOOLBOX);
        assertEquals(tabs.get(1), SidebarTab.COLLECTIONS);
        assertEquals(tabs.size(), SidebarTab.values().length);
    }

    @Test(description = "Khi cấu hình ẩn ẩn tất cả các menu, nó sẽ quay trở lại danh sách hiển thị mặc định")
    public void testResolveVisibleTabs_ShouldFallbackWhenAllTabsHidden() {
        Set<String> hiddenTabs = Set.of(
                "COLLECTIONS",
                "ENVIRONMENTS",
                "WORKSPACES",
                "FUNCTIONAL",
                "PERFORMANCE",
                "MOCK_SERVER",
                "TOOLBOX",
                "HISTORY"
        );

        List<SidebarTab> tabs = SidebarTab.resolveVisibleTabs(List.of("HISTORY", "TOOLBOX"), hiddenTabs);

        assertEquals(tabs, List.of(SidebarTab.values()));
    }
}
