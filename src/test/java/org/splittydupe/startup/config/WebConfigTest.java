package org.splittydupe.startup.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldAllowCors() throws Exception {
        mockMvc.perform(options("/api/test")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().is4xxClientError()) // 404 since frontend server isn't up
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}

