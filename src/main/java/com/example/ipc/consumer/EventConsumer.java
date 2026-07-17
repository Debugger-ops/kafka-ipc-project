package com.example.ipc.consumer;

import com.example.ipc.config.KafkaConfig;
import com.example.ipc.model.Event;
import com.example.ipc.serde.EventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * A consumer process that subscribes to the IPC topic and reacts to events.
 *
 * <p>This is the other side of the interprocess communication: it receives
 * events streamed by producers and handles them (event-driven architecture).
 * Belonging to a consumer group makes consumption scalable and fault-tolerant —
 * partitions are rebalanced automatically if an instance dies.</p>
 */
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final KafkaConsumer<String, Event> consumer;
    private volatile boolean running = true;

    public EventConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConfig.CONSUMER_GROUP);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDeserializer.class.getName());

        // Start from the beginning if this group has no committed offset yet.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Commit offsets only after we have processed a batch (at-least-once).
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        this.consumer = new KafkaConsumer<>(props);
    }

    public void run() {
        consumer.subscribe(List.of(KafkaConfig.TOPIC));
        log.info("Consumer subscribed to '{}' as group '{}'. Waiting for events...",
                KafkaConfig.TOPIC, KafkaConfig.CONSUMER_GROUP);

        // Graceful shutdown on Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            consumer.wakeup();
        }));

        try {
            while (running) {
                ConsumerRecords<String, Event> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, Event> record : records) {
                    handle(record);
                }
                if (!records.isEmpty()) {
                    consumer.commitSync(); // acknowledge the batch
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            // expected on shutdown - ignore
        } finally {
            consumer.close();
            log.info("Consumer closed.");
        }
    }

    /** Event handler — this is where downstream business logic would live. */
    private void handle(ConsumerRecord<String, Event> record) {
        Event event = record.value();
        log.info("Received [{}] from partition {} offset {}: {}",
                event.getType(), record.partition(), record.offset(), event.getPayload());
    }

    public static void main(String[] args) {
        new EventConsumer().run();
    }
}
