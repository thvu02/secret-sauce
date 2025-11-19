package org.splittydupe.startup.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.cloud.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimestampDeserializer Tests")
class TimestampDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    @Mock
    private JsonToken jsonToken;

    private TimestampDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new TimestampDeserializer();
    }

    @Test
    @DisplayName("Should deserialize epoch milliseconds to timestamp")
    void shouldDeserializeEpochMillisecondsToTimestamp() throws IOException {
        long epochMillis = 1640000000000L; // 2021-12-20
        when(jsonParser.getCurrentToken()).thenReturn(jsonToken);
        when(jsonToken.isNumeric()).thenReturn(true);
        when(jsonParser.getLongValue()).thenReturn(epochMillis);

        Timestamp result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNotNull(result);
        assertEquals(epochMillis, result.toDate().getTime());
    }

    @Test
    @DisplayName("Should return null for non-numeric token")
    void shouldReturnNullForNonNumericToken() throws IOException {
        when(jsonParser.getCurrentToken()).thenReturn(jsonToken);
        when(jsonToken.isNumeric()).thenReturn(false);

        Timestamp result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNull(result);
        verify(jsonParser, never()).getLongValue();
    }

    @Test
    @DisplayName("Should deserialize zero epoch")
    void shouldDeserializeZeroEpoch() throws IOException {
        long epochMillis = 0L;
        when(jsonParser.getCurrentToken()).thenReturn(jsonToken);
        when(jsonToken.isNumeric()).thenReturn(true);
        when(jsonParser.getLongValue()).thenReturn(epochMillis);

        Timestamp result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNotNull(result);
        assertEquals(epochMillis, result.toDate().getTime());
    }

    @Test
    @DisplayName("Should deserialize large epoch value")
    void shouldDeserializeLargeEpochValue() throws IOException {
        long epochMillis = System.currentTimeMillis();
        when(jsonParser.getCurrentToken()).thenReturn(jsonToken);
        when(jsonToken.isNumeric()).thenReturn(true);
        when(jsonParser.getLongValue()).thenReturn(epochMillis);

        Timestamp result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNotNull(result);
        assertEquals(epochMillis, result.toDate().getTime());
    }
}
