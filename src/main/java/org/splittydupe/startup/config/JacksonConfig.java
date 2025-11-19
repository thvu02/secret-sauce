package org.splittydupe.startup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.Timestamp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        SimpleModule timestampModule = new SimpleModule("TimestampModule");
        timestampModule.addSerializer(Timestamp.class, new TimestampSerializer());
        timestampModule.addDeserializer(Timestamp.class, new TimestampDeserializer());

        objectMapper.registerModule(timestampModule);

        return objectMapper;
    }
}
