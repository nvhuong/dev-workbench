package com.nvhuong.devworkbench.plugin.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Stop + immediate restart while the old consumer's poll is still returning: the old session must neither publish its
 * records into the new session's table nor clear the reference to the new consumer (which then could not be stopped
 * by wakeup()).
 */
public class KafkaPanelConsumeRestartTest {

    @Test(timeOut = 30_000)
    public void staleConsumerDoesNotLeakIntoRestartedSession() throws Exception {
        SlowFirstPollConsumer first = new SlowFirstPollConsumer();
        QuietConsumer second = new QuietConsumer();
        Deque<Consumer<String, String>> consumers = new ArrayDeque<>(List.of(first, second));
        KafkaPanel[] panel = new KafkaPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new KafkaPanel();
            setField(panel[0], "consumerFactory", (Function<Properties, Consumer<String, String>>) props -> consumers.pop());
            Object connection = getField(panel[0], "connectionPanel");
            ((javax.swing.JTextField) getField(connection, "bootstrapField")).setText("localhost:9092");
            Object consumerPanel = getField(panel[0], "consumerPanel");
            ((javax.swing.JTextField) getField(consumerPanel, "topicField")).setText("orders");
            invoke(panel[0], "startConsuming");
        });
        assertTrue(first.inPoll.await(10, TimeUnit.SECONDS), "first consumer never polled");

        SwingUtilities.invokeAndWait(() -> invoke(panel[0], "stopConsuming"));
        SwingUtilities.invokeAndWait(() -> { }); // let the cancelled worker's done() run
        SwingUtilities.invokeAndWait(() -> invoke(panel[0], "startConsuming"));
        assertTrue(second.inPoll.await(10, TimeUnit.SECONDS), "restarted consumer never polled");

        first.release.countDown();
        long deadline = System.currentTimeMillis() + 10_000;
        while (!first.closed() && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertTrue(first.closed(), "old consumer was not closed");
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> { });

        @SuppressWarnings("unchecked")
        List<Object> messages = (List<Object>) getField(panel[0], "consumedMessages");
        assertTrue(messages.isEmpty(), "records of the stopped session leaked into the new one: " + messages);
        @SuppressWarnings("unchecked")
        AtomicReference<Object> running = (AtomicReference<Object>) getField(panel[0], "runningConsumerRef");
        assertSame(running.get(), second, "the running consumer reference was cleared by the old session");

        SwingUtilities.invokeAndWait(() -> invoke(panel[0], "stopConsuming"));
    }

    /** First poll blocks (ignoring interrupts, like a poll that already fetched data) and then returns one record. */
    private static final class SlowFirstPollConsumer extends MockConsumer<String, String> {
        final CountDownLatch inPoll = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private boolean polled;

        SlowFirstPollConsumer() {
            super(OffsetResetStrategy.EARLIEST);
        }

        @Override
        public ConsumerRecords<String, String> poll(Duration timeout) {
            if (polled) {
                return ConsumerRecords.empty();
            }
            polled = true;
            inPoll.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
            TopicPartition partition = new TopicPartition("orders", 0);
            return new ConsumerRecords<>(Map.of(partition, List.of(new ConsumerRecord<>("orders", 0, 0L, "k", "old"))));
        }
    }

    /** Polls nothing until woken up. */
    private static final class QuietConsumer extends MockConsumer<String, String> {
        final CountDownLatch inPoll = new CountDownLatch(1);

        QuietConsumer() {
            super(OffsetResetStrategy.EARLIEST);
        }

        @Override
        public ConsumerRecords<String, String> poll(Duration timeout) {
            inPoll.countDown();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.poll(Duration.ZERO);
        }
    }

    private static Object getField(Object target, String name) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep looking
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void invoke(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
