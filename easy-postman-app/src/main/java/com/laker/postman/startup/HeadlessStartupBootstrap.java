package com.laker.postman.startup;

import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.plugin.host.HeadlessRequestCollectionImportService;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HeadlessStartupBootstrap {

    public void initRuntime() {
        if (PluginRuntime.isInitialized()) {
            return;
        }
        synchronized (HeadlessStartupBootstrap.class) {
            if (PluginRuntime.isInitialized()) {
                return;
            }
            // Headless Lệnh chỉ yêu cầu phần mở rộng tập lệnh plugin, không yêu cầu khởi động app IOC，Tránh rời khỏi khu vực làm việc/GUI bean Quét và mang vào CLI/worker。
            PluginRuntime.getRegistry().registerService(
                    RequestCollectionImportService.class,
                    new HeadlessRequestCollectionImportService()
            );
            PluginRuntime.initialize();
        }
    }
}
