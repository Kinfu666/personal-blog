package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.personalblog.dto.ImageResponse;
import com.example.personalblog.dto.PostForm;
import com.example.personalblog.entity.Category;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.exception.NotFoundException;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.service.ImageService;
import com.example.personalblog.service.PostService;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class ImageTest {
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) throws Exception {
        r.add("app.data-dir", Files.createTempDirectory("blog-images-")::toString);
    }

    @Autowired ImageService images;
    @Autowired PostService posts;
    @Autowired CategoryRepository categories;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired Environment environment;

    static byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void fileCompensationAndStartupRecovery() throws Exception {
        var tx = new TransactionTemplate(transactions);
        var image = images.store(png(), "recovery.png", "image/png", "admin");
        var root = Path.of(environment.getRequiredProperty("app.data-dir")).resolve("uploads");
        var stored = Path.of(image.url()).getFileName().toString();
        var file = root.resolve(stored);
        var trash = root.resolve(stored + ".deleted");
        tx.executeWithoutResult(
                status -> {
                    images.delete(image.id());
                    status.setRollbackOnly();
                });
        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.exists(trash)).isFalse();
        Files.move(file, trash);
        images.reconcile();
        assertThat(Files.exists(file)).isTrue();
        var orphan = root.resolve(UUID.randomUUID() + ".png");
        Files.write(orphan, png());
        images.reconcile();
        assertThat(Files.exists(orphan)).isFalse();
        long before;
        try (var files = Files.list(root)) {
            before = files.count();
        }
        tx.executeWithoutResult(
                status -> {
                    try {
                        images.store(png(), "rollback.png", "image/png", "admin");
                    } catch (Exception error) {
                        throw new RuntimeException(error);
                    }
                    status.setRollbackOnly();
                });
        try (var files = Files.list(root)) {
            assertThat(files.count()).isEqualTo(before);
        }
        images.delete(image.id());
        Files.write(trash, png());
        images.reconcile();
        assertThat(Files.exists(trash)).isFalse();
    }

    @Test
    void uploadReadReferencesAndDelete() throws Exception {
        ImageResponse image =
                images.upload(
                        new MockMultipartFile("file", "../../evil.png", "image/png", png()),
                        "admin");
        assertThat(image.url()).matches("/uploads/[a-f0-9-]{36}\\.png");
        mvc.perform(get(image.url())).andExpect(status().isNotFound());
        mvc.perform(get(image.url()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        PostForm form = new PostForm();
        form.setTitle("圖片測試");
        form.setSummary("摘要");
        form.setContent(image.markdown());
        form.setCategoryId(categories.save(new Category("圖片")).getId());
        Long id = posts.save(null, form, "admin");
        mvc.perform(get(image.url())).andExpect(status().isNotFound());
        assertThatThrownBy(() -> images.delete(image.id())).isInstanceOf(RuleViolation.class);
        form.setStatus(PostStatus.PUBLISHED);
        posts.save(id, form, "admin");
        mvc.perform(get(image.url())).andExpect(status().isOk());
        form.setStatus(PostStatus.DRAFT);
        posts.save(id, form, "admin");
        mvc.perform(get(image.url())).andExpect(status().isNotFound());
        form.setContent("`" + image.markdown() + "`\n" + image.url());
        posts.save(id, form, "admin");
        images.delete(image.id());
        mvc.perform(get(image.url()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsFakeMimeLargeAndTraversal() throws Exception {
        assertThatThrownBy(
                        () ->
                                images.upload(
                                        new MockMultipartFile(
                                                "file",
                                                "x.png",
                                                "image/png",
                                                "<script>evil</script>".getBytes()),
                                        "admin"))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(
                        () ->
                                images.upload(
                                        new MockMultipartFile("file", "x.jpg", "image/jpeg", png()),
                                        "admin"))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(
                        () ->
                                images.upload(
                                        new MockMultipartFile(
                                                "file",
                                                "x.png",
                                                "image/png",
                                                new byte[(int) ImageService.MAX_BYTES + 1]),
                                        "admin"))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> images.read("../../blog.db", true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void acceptsDecodedWebpAndJpegAndRejectsHugeDimensions() throws Exception {
        byte[] webp = getClass().getResourceAsStream("/images/original.webp").readAllBytes();
        ImageResponse image =
                images.upload(
                        new MockMultipartFile("file", "original.webp", "image/webp", webp),
                        "admin");
        assertThat(image.url()).endsWith(".webp");
        var jpeg = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "jpeg", jpeg);
        assertThat(
                        images.upload(
                                        new MockMultipartFile(
                                                "file",
                                                "original.jpg",
                                                "image/jpeg",
                                                jpeg.toByteArray()),
                                        "admin")
                                .url())
                .endsWith(".jpg");
        byte[] huge = png();
        ByteBuffer.wrap(huge, 16, 4).putInt(20000);
        assertThatThrownBy(
                        () ->
                                images.upload(
                                        new MockMultipartFile(
                                                "file", "huge.png", "image/png", huge),
                                        "admin"))
                .isInstanceOf(RuleViolation.class);
    }

    @Test
    void uploadAndPreviewRequireAdminAndCsrf() throws Exception {
        var file = new MockMultipartFile("file", "x.png", "image/png", png());
        mvc.perform(multipart("/admin/images").file(file)).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/images").file(file).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(
                        multipart("/admin/images")
                                .file(file)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists());
        mvc.perform(
                        post("/admin/markdown/preview")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"content\":\"# 預覽\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value("<h1>預覽</h1>"));
        mvc.perform(
                        multipart("/admin/images")
                                .file(
                                        new MockMultipartFile(
                                                "file", "bad.png", "image/png", new byte[] {1}))
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
