package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;

import jakarta.servlet.RequestDispatcher;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;

import com.example.personalblog.controller.WebExceptionHandler;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class ErrorHandlingTest {
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) throws Exception {
        r.add("app.data-dir", Files.createTempDirectory("blog-errors-")::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired WebExceptionHandler errors;

    @Test
    void friendly404AndBadInputAndMethod() throws Exception {
        mvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(Matchers.containsString("這一頁暫時找不到")));
        mvc.perform(get("/").param("category", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("請確認網址")));
        mvc.perform(get("/admin/posts/1/delete").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void errorViewsDoNotExposeExceptionOrSession() throws Exception {
        for (int code : new int[] {403, 500}) {
            mvc.perform(
                            get("/error")
                                    .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, code)
                                    .requestAttr(
                                            RequestDispatcher.ERROR_EXCEPTION,
                                            new IllegalStateException("sensitive-diagnostic")))
                    .andExpect(status().is(code))
                    .andExpect(
                            content()
                                    .string(
                                            Matchers.not(
                                                    Matchers.containsString("sensitive-diagnostic"))))
                    .andExpect(
                            content()
                                    .string(
                                            Matchers.not(
                                                    Matchers.containsString(
                                                            "java.lang.IllegalStateException"))));
        }
    }

    @Test
    void unexpectedErrorHasTraceReference() {
        var model = new ExtendedModelMap();
        assertThat(
                        errors.unexpected(
                                new IllegalStateException("intentional test"),
                                new MockHttpServletRequest("GET", "/"),
                                model))
                .isEqualTo("error/500");
        assertThat(model.getAttribute("reference")).asString().matches("[a-f0-9-]{36}");
    }
}
