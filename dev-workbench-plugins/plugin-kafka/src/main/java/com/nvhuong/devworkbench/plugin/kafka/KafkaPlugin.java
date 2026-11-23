package com.nvhuong.devworkbench.plugin.kafka;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;
import com.nvhuong.devworkbench.plugin.api.PluginContext;

import static com.nvhuong.devworkbench.plugin.kafka.KafkaI18n.t;

public class KafkaPlugin implements DevWorkbenchPlugin {

    @Override
    public void onLoad(PluginContext context) {
        // Bản thân lớp mục nhập trình cắm thêm không thực hiện các công việc phức tạp, nhiệm vụ cốt lõi của nó là thực hiện“Kafka Danh sách năng lực”Đăng ký với chủ nhà.
        // Máy chủ sẽ kết nối các khả năng này với các tập lệnh sau.Toolbox、Hoàn thành tổng Snippet Chờ các địa điểm khác nhau.
        context.registerI18nBundle(KafkaI18n.BUNDLE_NAME);
        context.registerScriptApi("kafka", ScriptKafkaApi::new);
        try {
            KafkaMcpTools.register(context);
        } catch (LinkageError olderHost) {
            // Hosts without MCP tool support keep the toolbox and script API.
        }
        PluginContributionSupport.registerToolbox(
                context,
                "kafka",
                t(MessageKeys.TOOLBOX_KAFKA),
                "icons/kafka.svg",
                MessageKeys.TOOLBOX_GROUP_DATABASE,
                t(MessageKeys.TOOLBOX_GROUP_DATABASE),
                () -> new KafkaPanel(context.storage()),
                KafkaPlugin.class
        );
        context.registerScriptCompletionContributor(sink -> {
            PluginContributionSupport.addScriptApiCompletions(
                    sink,
                    "kafka",
                    "Kafka script API",
                    "listTopics",
                    "send",
                    "poll"
            );
            PluginContributionSupport.addSnippetCompletion(
                    sink,
                    "kafka.poll",
                    """
                    const kafka = pm.plugin("kafka");
                    const records = kafka.poll({
                      bootstrapServers: "localhost:9092",
                      topic: "demo-topic",
                      groupId: "dev-workbench-script",
                      autoOffsetReset: "earliest",
                      pollTimeoutMs: 1000
                    });
                    pm.test("Kafka has records", function () {
                      pm.expect(records.length).to.be.above(0);
                    });""",
                    "Kafka poll + assert"
            );
        });
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_ASSERT_DESC),
                "// Kafka Truy vấn + khẳng định\nvar kafka = pm.plugin('kafka');\nvar records = kafka.poll({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: pm.environment.get('kafkaTopic') || 'demo-topic',\n    groupId: 'dev-workbench-script-' + Date.now(),\n    autoOffsetReset: 'earliest',\n    pollTimeoutMs: 1000,\n    maxMessages: 20\n});\n\npm.test('Kafka records should not be empty', function () {\n    pm.expect(records.length).to.be.above(0);\n});\n\nconsole.log('Kafka records count:', records.length);"
        );
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_SEND_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_SEND_ASSERT_DESC),
                "// Kafka gửi + khẳng định\nvar kafka = pm.plugin('kafka');\nvar kafkaTopic = pm.environment.get('kafkaTopic') || 'demo-topic';\nvar kafkaResp = kafka.send({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: kafkaTopic,\n    key: 'order-1001',\n    value: JSON.stringify({ orderId: 1001, status: 'CREATED', source: 'dev-workbench' })\n});\n\npm.test('Kafka send success', function () {\n    pm.expect(kafkaResp.topic).to.equal(kafkaTopic);\n    pm.expect(kafkaResp.offset).to.be.least(0);\n});\n\nconsole.log('Kafka send metadata:', JSON.stringify(kafkaResp));"
        );
    }
}
