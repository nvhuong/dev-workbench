package com.nvhuong.devworkbench.panel.toolbox;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ConnectionProfilePersistenceBehaviorTest {

    @Test(description = "Kết nối thành công với công cụ cơ sở dữ liệu sẽ không tự động lưu cấu hình kết nối mà phải được kích hoạt khi người dùng nhấp vào Lưu")
    public void successfulConnectShouldNotPersistConnectionProfiles() throws Exception {
        String redisPanel = source("dev-workbench-plugins/plugin-redis/src/main/java/com/nvhuong/devworkbench/plugin/redis/RedisPanel.java");
        String kafkaPanel = source("dev-workbench-plugins/plugin-kafka/src/main/java/com/nvhuong/devworkbench/plugin/kafka/KafkaPanel.java");
        String elasticsearchPanel = source("dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/ElasticsearchPanel.java");
        String influxDbPanel = source("dev-workbench-app/src/main/java/com/nvhuong/devworkbench/panel/toolbox/InfluxDbPanel.java");

        assertFalse(redisPanel.contains("saveConnectionProfile(host, port, db, user, pass, false);"));
        assertFalse(kafkaPanel.contains("connectionProfileStore.upsertProfile(profileToSave);"));
        assertFalse(elasticsearchPanel.contains("saveConnectionProfile(finalUrl, user, pass, false);"));
        assertFalse(influxDbPanel.contains("saveConnectionProfile(finalBaseUrl, false);"));
    }

    @Test(description = "Sử dụng bản sao ngắn cho nút ngắt kết nối sau khi kết nối để tránh các nút trên thanh công cụ bị cắt bớt")
    public void disconnectButtonLabelsShouldBeShortInVietnamese() throws Exception {
        Properties appMessages = properties("dev-workbench-app/src/main/resources/messages_vi.properties");
        Properties redisMessages = properties("dev-workbench-plugins/plugin-redis/src/main/resources/redis-messages_vi.properties");
        Properties kafkaMessages = properties("dev-workbench-plugins/plugin-kafka/src/main/resources/kafka-messages_vi.properties");

        assertEquals(appMessages.getProperty("toolbox.es.disconnect"), "Ngắt kết nối");
        assertEquals(appMessages.getProperty("toolbox.influx.disconnect"), "Ngắt kết nối");
        assertEquals(redisMessages.getProperty("toolbox.redis.disconnect"), "Ngắt kết nối");
        assertEquals(kafkaMessages.getProperty("toolbox.kafka.disconnect"), "Ngắt kết nối");
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(projectRoot().resolve(relativePath));
    }

    private static Properties properties(String relativePath) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(projectRoot().resolve(relativePath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("dev-workbench-app/pom.xml"))
                    && Files.exists(current.resolve("dev-workbench-plugins"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find dev-workbench project root");
    }
}
