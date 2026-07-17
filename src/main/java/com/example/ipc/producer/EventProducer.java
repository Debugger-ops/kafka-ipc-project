package com.example.ipc.producer;

import com.example.ipc.config.KafkaConfig;
import com.example.ipc.model.Event;
import com.example.ipc.serde.EventSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A producer process that publishes {@link Event} messages to Kafka.
 *
 * <p>Represents one side of the interprocess communication: it turns local
 * work into events streamed to any interested consumer process. Configured for
 * reliable delivery (acks=all, idempotence, retries).</p>
 */
public class EventProducer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final Producer<String, Event> producer;

    public EventProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSerializer.class.getName());

        // Reliability: wait for all in-sync replicas and avoid duplicates.
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        this.producer = new KafkaProducer<>(props);
    }

    /**
     * Sends an event asynchronously. The event id is used as the record key so
     * that all events with the same id land on the same partition (ordering).
     */
    public void send(Event event) {
        ProducerRecord<String, Event> record =
                new ProducerRecord<>(KafkaConfig.TOPIC, event.getId(), event);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send event {}", event.getId(), exception);
            } else {
                log.info("Sent event {} -> partition {} offset {}",
                        event.getType(), metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }

    /**
     * Demo driver: streams a series of sensor-reading events, then exits.
     */
    public static void main(String[] args) throws InterruptedException {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 20;

        try (EventProducer eventProducer = new EventProducer()) {
            log.info("Producing {} events to topic '{}'...", count, KafkaConfig.TOPIC);
            for (int i = 1; i <= count; i++) {
                double reading = ThreadLocalRandom.current().nextDouble(15.0, 35.0);
                Event event = Event.create(
                        "SENSOR_READING",
                        "producer-app",
                        String.format("{\"sensor\":\"temp-%d\",\"celsius\":%.2f}", (i % 5), reading));
                eventProducer.send(event);
                Thread.sleep(500); // simulate a real-time stream
            }
            log.info("Done producing events.");
        }
    }
}
