package com.example.ipc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * The unit of data exchanged between processes over Kafka.
 *
 * <p>This is a simple immutable event carrying a type, a payload and some
 * metadata. It is serialized to JSON on the producer side and reconstructed on
 * the consumer side, demonstrating message serialization in an event-driven
 * architecture.</p>
 */
public class Event {

    private final String id;
    private final String type;
    private final String source;
    private final String payload;
    private final Instant timestamp;

    @JsonCreator
    public Event(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("source") String source,
            @JsonProperty("payload") String payload,
            @JsonProperty("timestamp") Instant timestamp) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    /** Factory for creating a fresh event with a generated id and current time. */
    public static Event create(String type, String source, String payload) {
        return new Event(UUID.randomUUID().toString(), type, source, payload, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", payload='" + payload + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
