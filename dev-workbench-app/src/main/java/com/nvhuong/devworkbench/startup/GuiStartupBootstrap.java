package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.common.constants.AppConstants;
import com.nvhuong.devworkbench.ioc.BeanFactory;
import com.nvhuong.devworkbench.plugin.api.service.RequestCollectionImportService;
import com.nvhuong.devworkbench.plugin.host.AppSwingRequestCollectionImportService;
import com.nvhuong.devworkbench.plugin.runtime.PluginRuntime;
import com.nvhuong.devworkbench.service.EnvironmentService;
import com.nvhuong.devworkbench.service.WorkspaceService;
import lombok.experimental.UtilityClass;

/**
 * GUI Bắt đầu khởi tạo thời gian chạy máy chủ của nhánh.
 */
@UtilityClass
class GuiStartupBootstrap {

    void initBeanFactory() {
        BeanFactory.init(AppConstants.BASE_PACKAGE);
    }

    void initWorkspaceState() {
        WorkspaceService.getInstance();
        EnvironmentService.getAllEnvironments();
    }

    void initPluginRuntime() {
        // Dịch vụ cầu nối máy chủ có thể được truy vấn trong quá trình tải trình cắm, vì vậy các khả năng của máy chủ phải được đăng ký trước khi khởi chạy thời gian chạy trình cắm.
        PluginRuntime.getRegistry().registerService(
                RequestCollectionImportService.class,
                new AppSwingRequestCollectionImportService()
        );
        PluginRuntime.initialize();
    }
}
