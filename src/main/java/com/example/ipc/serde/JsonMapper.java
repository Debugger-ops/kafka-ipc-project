package com.example.ipc.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson {@link ObjectMapper} configured to handle Java 8 date/time
 * types (used by {@link com.example.ipc.model.Event#getTimestamp()}).
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private JsonMapper() {
    }

    public static ObjectMapper get() {
        return MAPPER;
    }
}
