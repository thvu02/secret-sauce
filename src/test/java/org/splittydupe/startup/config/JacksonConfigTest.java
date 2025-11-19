package org.splittydupe.startup.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JacksonConfig Tests")
class JacksonConfigTest {

    private JacksonConfig jacksonConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jacksonConfig = new JacksonConfig();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        objectMapper = jacksonConfig.objectMapper(builder);
    }

    @Test
    @DisplayName("Should create ObjectMapper with Timestamp module")
    void shouldCreateObjectMapperWithTimestampModule() {
        assertNotNull(objectMapper);
    }

    @Test
    @DisplayName("Should serialize Timestamp correctly")
    void shouldSerializeTimestampCorrectly() throws JsonProcessingException {
        Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(1640000000, 0);
        TestObject obj = new TestObject();
        obj.timestamp = timestamp;

        String json = objectMapper.writeValueAsString(obj);

        assertNotNull(json);
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains(String.valueOf(timestamp.toDate().getTime())));
    }

    @Test
    @DisplayName("Should deserialize Timestamp correctly")
    void shouldDeserializeTimestampCorrectly() throws JsonProcessingException {
        long epochMillis = 1640000000000L;
        String json = "{\"timestamp\":" + epochMillis + "}";

        TestObject obj = objectMapper.readValue(json, TestObject.class);

        assertNotNull(obj);
        assertNotNull(obj.timestamp);
        assertEquals(epochMillis, obj.timestamp.toDate().getTime());
    }

    @Test
    @DisplayName("Should handle null Timestamp")
    void shouldHandleNullTimestamp() throws JsonProcessingException {
        TestObject obj = new TestObject();
        obj.timestamp = null;

        String json = objectMapper.writeValueAsString(obj);

        assertNotNull(json);
        assertTrue(json.contains("\"timestamp\":null"));
    }

    @Test
    @DisplayName("Should serialize and deserialize roundtrip")
    void shouldSerializeAndDeserializeRoundtrip() throws JsonProcessingException {
        Timestamp originalTimestamp = Timestamp.now();
        TestObject originalObj = new TestObject();
        originalObj.timestamp = originalTimestamp;

        String json = objectMapper.writeValueAsString(originalObj);
        TestObject deserializedObj = objectMapper.readValue(json, TestObject.class);

        assertNotNull(deserializedObj);
        assertNotNull(deserializedObj.timestamp);
        assertEquals(
                originalTimestamp.toDate().getTime(),
                deserializedObj.timestamp.toDate().getTime()
        );
    }

    static class TestObject {
        public Timestamp timestamp;
    }
}
