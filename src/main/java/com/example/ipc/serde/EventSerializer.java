package com.example.ipc.serde;

import com.example.ipc.model.Event;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializes an {@link Event} into JSON bytes before it is sent to Kafka.
 *
 * <p>Plugging a custom serializer into the producer keeps the application code
 * working with rich domain objects rather than raw byte arrays.</p>
 */
public class EventSerializer implements Serializer<Event> {

    @Override
    public byte[] serialize(String topic, Event data) {
        if (data == null) {
            return null;
        }
        try {
            return JsonMapper.get().writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize Event to JSON", e);
        }
    }
}
