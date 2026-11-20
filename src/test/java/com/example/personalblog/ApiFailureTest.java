package com.example.personalblog;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.personalblog.controller.ApiExceptionHandler;
import com.example.personalblog.controller.ImageApiController;
import com.example.personalblog.service.ImageService;
import com.example.personalblog.service.MarkdownService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiFailureTest {
    @Test
    void unexpectedUploadFailureStaysJsonAndHidesDiagnostics() throws Exception {
        var images = mock(ImageService.class);
        when(images.upload(any(), eq("admin")))
                .thenThrow(
                        new org.springframework.dao.DataAccessResourceFailureException(
                                "sensitive database detail"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new ImageApiController(images, new MarkdownService()))
                        .setControllerAdvice(new ApiExceptionHandler())
                        .build();
        mvc.perform(
                        multipart("/admin/images")
                                .file(
                                        new MockMultipartFile(
                                                "file", "x.png", "image/png", new byte[] {1}))
                                .principal(() -> "admin"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(
                        jsonPath("$.message").value(org.hamcrest.Matchers.containsString("錯誤編號")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "sensitive database detail"))));
    }
}
