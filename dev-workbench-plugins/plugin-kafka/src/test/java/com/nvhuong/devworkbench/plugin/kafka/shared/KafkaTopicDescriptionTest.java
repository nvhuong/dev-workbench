package com.nvhuong.devworkbench.plugin.kafka.shared;

import com.nvhuong.devworkbench.plugin.kafka.ui.KafkaTopicItem;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class KafkaTopicDescriptionTest {

    /**
     * On ACL-protected clusters a user may list every topic but only describe some. Waiting on allTopicNames() made one
     * TopicAuthorizationException fail the whole connection; other topics must still be listed.
     */
    @Test
    public void listsTopicsEvenWhenSomeCannotBeDescribed() throws Exception {
        Node node = new Node(1, "broker", 9092);
        TopicPartitionInfo partition0 = new TopicPartitionInfo(0, node, List.of(node), List.of(node));
        TopicPartitionInfo partition1 = new TopicPartitionInfo(1, node, List.of(node), List.of(node));
        Map<String, KafkaFuture<TopicDescription>> futures = new LinkedHashMap<>();
        futures.put("orders", KafkaFuture.completedFuture(
                new TopicDescription("orders", false, List.of(partition0, partition1))));
        KafkaFutureImpl<TopicDescription> denied = new KafkaFutureImpl<>();
        denied.completeExceptionally(new TopicAuthorizationException("Not authorized to describe payments"));
        futures.put("payments", denied);

        List<KafkaTopicItem> items = KafkaPanelSupport.toTopicItems(List.of("orders", "payments"), futures, 1_000);

        assertEquals(items, List.of(new KafkaTopicItem("orders", 2),
                new KafkaTopicItem("payments", KafkaPanelSupport.UNKNOWN_PARTITION_COUNT)));
    }
}
