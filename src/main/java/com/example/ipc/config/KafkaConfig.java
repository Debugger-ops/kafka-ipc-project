package com.example.ipc.config;

/**
 * Central configuration for the Kafka-based IPC system.
 *
 * <p>Keeping bootstrap servers, topic names and group ids in one place makes it
 * easy for every process (producer, consumer, admin) to agree on how they talk
 * to each other.</p>
 */
public final class KafkaConfig {

    private KafkaConfig() {
        // utility class - no instances
    }

    /** Address of the Kafka broker exposed by docker-compose to the host. */
    public static final String BOOTSTRAP_SERVERS =
            System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

    /** The topic used to stream events between processes. */
    public static final String TOPIC = "ipc-events";

    /** Number of partitions - enables parallel, scalable consumption. */
    public static final int PARTITIONS = 3;

    /** Replication factor. 1 is fine for a single-broker dev cluster. */
    public static final short REPLICATION_FACTOR = 1;

    /** Consumer group id. Consumers in the same group share the partitions. */
    public static final String CONSUMER_GROUP = "ipc-consumer-group";
}
