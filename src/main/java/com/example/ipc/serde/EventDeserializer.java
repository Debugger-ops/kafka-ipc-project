package com.example.ipc.serde;

import com.example.ipc.model.Event;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Reconstructs an {@link Event} from the JSON bytes stored in Kafka.
 *
 * <p>The counterpart to {@link EventSerializer}; used by the consumer so it
 * receives fully-formed {@link Event} objects.</p>
 */
public class EventDeserializer implements Deserializer<Event> {

    @Override
    public Event deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return JsonMapper.get().readValue(data, Event.class);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize JSON to Event", e);
        }
    }
}
