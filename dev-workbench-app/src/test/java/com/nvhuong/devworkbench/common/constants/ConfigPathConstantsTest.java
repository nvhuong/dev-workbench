package com.nvhuong.devworkbench.common.constants;

import com.nvhuong.devworkbench.model.Workspace;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;

public class ConfigPathConstantsTest {

    @Test(description = "Đường dẫn cấu hình kiểm tra hiệu năng phải nằm trong thư mục không gian làm việc được chỉ định")
    public void shouldResolvePerformanceConfigPathForWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setPath("/tmp/dev-workbench-workspace" + File.separator);

        assertEquals(
                ConfigPathConstants.getPerformanceConfigPath(workspace),
                "/tmp/dev-workbench-workspace" + File.separator + "performance_config.json"
        );
    }

    @Test(description = "Đường dẫn cấu hình kiểm tra hiệu năng sẽ quay trở lại đường dẫn cấp ứng dụng cũ khi không gian làm việc trống")
    public void shouldFallbackToDefaultWorkspacePerformanceConfigPathWhenWorkspaceMissing() {
        assertEquals(
                ConfigPathConstants.getPerformanceConfigPath(null),
                ConfigPathConstants.DEFAULT_WORKSPACE_DIR + "performance_config.json"
        );

        Workspace workspace = new Workspace();
        workspace.setPath(" ");

        assertEquals(
                ConfigPathConstants.getPerformanceConfigPath(workspace),
                ConfigPathConstants.DEFAULT_WORKSPACE_DIR + "performance_config.json"
        );
    }

    @Test(description = "Đường dẫn cấu hình kiểm tra chức năng phải nằm trong thư mục không gian làm việc được chỉ định")
    public void shouldResolveFunctionalConfigPathForWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setPath("/tmp/dev-workbench-workspace" + File.separator);

        assertEquals(
                ConfigPathConstants.getFunctionalConfigPath(workspace),
                "/tmp/dev-workbench-workspace" + File.separator + "functional_config.json"
        );
    }

    @Test(description = "Đường dẫn cấu hình kiểm tra chức năng sẽ quay trở lại đường dẫn cấp ứng dụng cũ khi không gian làm việc trống")
    public void shouldFallbackToDefaultWorkspaceFunctionalConfigPathWhenWorkspaceMissing() {
        assertEquals(
                ConfigPathConstants.getFunctionalConfigPath(null),
                ConfigPathConstants.DEFAULT_WORKSPACE_DIR + "functional_config.json"
        );

        Workspace workspace = new Workspace();
        workspace.setPath(" ");

        assertEquals(
                ConfigPathConstants.getFunctionalConfigPath(workspace),
                ConfigPathConstants.DEFAULT_WORKSPACE_DIR + "functional_config.json"
        );
    }

    @Test(description = "Mock Server Đường dẫn cấu hình phải nằm trong thư mục không gian làm việc được chỉ định")
    public void shouldResolveMockServersPathForWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setPath("/tmp/dev-workbench-workspace" + File.separator);

        assertEquals(
                ConfigPathConstants.getMockServersPath(workspace),
                "/tmp/dev-workbench-workspace" + File.separator + "mock_servers.json"
        );
        assertEquals(ConfigPathConstants.getMockServersPath(null), ConfigPathConstants.MOCK_SERVERS);
    }
}
