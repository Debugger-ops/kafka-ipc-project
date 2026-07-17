package com.example.ipc.admin;

import com.example.ipc.config.KafkaConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Uses the Kafka Admin API to create the IPC topic if it does not exist.
 *
 * <p>Run this once before starting producers/consumers. It demonstrates
 * programmatic topic management (partitions + replication) for building
 * fault-tolerant, scalable pipelines.</p>
 */
public class TopicManager {

    private static final Logger log = LoggerFactory.getLogger(TopicManager.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);

        try (Admin admin = Admin.create(props)) {
            Set<String> existing = admin.listTopics().names().get();

            if (existing.contains(KafkaConfig.TOPIC)) {
                log.info("Topic '{}' already exists — nothing to do.", KafkaConfig.TOPIC);
                return;
            }

            NewTopic topic = new NewTopic(
                    KafkaConfig.TOPIC,
                    KafkaConfig.PARTITIONS,
                    KafkaConfig.REPLICATION_FACTOR);

            admin.createTopics(List.of(topic)).all().get();
            log.info("Created topic '{}' with {} partitions (replication factor {}).",
                    KafkaConfig.TOPIC, KafkaConfig.PARTITIONS, KafkaConfig.REPLICATION_FACTOR);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while managing topics", e);
        } catch (ExecutionException e) {
            log.error("Failed to create topic '{}'", KafkaConfig.TOPIC, e);
        }
    }
}
