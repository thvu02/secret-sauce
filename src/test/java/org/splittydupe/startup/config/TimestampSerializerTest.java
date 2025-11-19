package org.splittydupe.startup.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.cloud.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimestampSerializer Tests")
class TimestampSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializerProvider serializerProvider;

    private TimestampSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new TimestampSerializer();
    }

    @Test
    @DisplayName("Should serialize timestamp to epoch milliseconds")
    void shouldSerializeTimestampToEpochMilliseconds() throws IOException {
        Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(1640000000, 0); // 2021-12-20

        serializer.serialize(timestamp, jsonGenerator, serializerProvider);

        verify(jsonGenerator, times(1)).writeNumber(timestamp.toDate().getTime());
    }

    @Test
    @DisplayName("Should serialize null timestamp as null")
    void shouldSerializeNullTimestampAsNull() throws IOException {
        serializer.serialize(null, jsonGenerator, serializerProvider);

        verify(jsonGenerator, times(1)).writeNull();
        verify(jsonGenerator, never()).writeNumber(anyLong());
    }

    @Test
    @DisplayName("Should serialize current timestamp")
    void shouldSerializeCurrentTimestamp() throws IOException {
        Timestamp now = Timestamp.now();

        serializer.serialize(now, jsonGenerator, serializerProvider);

        verify(jsonGenerator, times(1)).writeNumber(now.toDate().getTime());
    }

    @Test
    @DisplayName("Should handle timestamp with nanoseconds")
    void shouldHandleTimestampWithNanoseconds() throws IOException {
        Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(1640000000, 500000000);

        serializer.serialize(timestamp, jsonGenerator, serializerProvider);

        verify(jsonGenerator, times(1)).writeNumber(anyLong());
    }
}
