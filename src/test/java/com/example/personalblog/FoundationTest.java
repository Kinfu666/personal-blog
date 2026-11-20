package com.example.personalblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"app.seed.enabled=false"})
@AutoConfigureMockMvc
class FoundationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        registry.add("app.data-dir", Files.createTempDirectory("blog-home-test-")::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;

    @Test
    void homeAndLocalStylesLoad() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("在實作中")));
        mvc.perform(get("/vendor/bootstrap.min.css")).andExpect(status().isOk());
        Assertions.assertThat(
                        http.getForEntity("http://127.0.0.1:8080/", String.class)
                                .getStatusCode()
                                .value())
                .isEqualTo(200);
    }
}
