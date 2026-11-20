package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.personalblog.dto.PostForm;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.ImageRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.TagRepository;
import com.example.personalblog.repository.UserRepository;
import com.example.personalblog.service.PostService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

class SeedLifecycleTest {
    @TempDir Path directory;

    private ConfigurableApplicationContext start(boolean seed) {
        return new SpringApplication(PersonalBlogApplication.class)
                .run(
                        "--server.port=0",
                        "--app.data-dir=" + directory,
                        "--app.seed.enabled=" + seed,
                        "--spring.profiles.active=demo",
                        "--debug=false",
                        "--logging.level.root=WARN");
    }

    @Test
    void firstStartAndRestartPreserveEditsAndDeletedContent() throws Exception {
        Long edited;
        Long deleted;
        String hash;
        String stored;
        try (var context = start(true)) {
            var posts = context.getBean(PostRepository.class);
            assertThat(posts.count()).isEqualTo(15);
            assertThat(posts.countByStatus(PostStatus.PUBLISHED)).isEqualTo(12);
            assertThat(context.getBean(CategoryRepository.class).count()).isEqualTo(4);
            assertThat(context.getBean(TagRepository.class).count()).isEqualTo(10);
            assertThat(context.getBean(ImageRepository.class).count()).isEqualTo(6);
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT min(length(content)) FROM posts", Integer.class))
                    .isGreaterThan(600);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM posts WHERE content LIKE '%{{image:%'",
                                    Integer.class))
                    .isZero();
            hash =
                    context.getBean(UserRepository.class)
                            .findByUsername("admin")
                            .orElseThrow()
                            .getPasswordHash();
            stored = context.getBean(ImageRepository.class).findAll().getFirst().getStoredName();
            assertThat(Files.isRegularFile(directory.resolve("uploads").resolve(stored))).isTrue();
            var service = context.getBean(PostService.class);
            edited = posts.findAll().getFirst().getId();
            deleted = posts.findAll().getLast().getId();
            PostForm form = service.edit(edited);
            form.setTitle("重啟後保留的標題");
            service.save(edited, form, "admin");
            service.delete(deleted);
        }
        try (var context = start(true)) {
            assertThat(context.getBean(PostRepository.class).count()).isEqualTo(14);
            assertThat(context.getBean(PostService.class).edit(edited).getTitle())
                    .isEqualTo("重啟後保留的標題");
            assertThat(context.getBean(PostRepository.class).existsById(deleted)).isFalse();
            assertThat(context.getBean(ImageRepository.class).count()).isEqualTo(6);
            assertThat(Files.isRegularFile(directory.resolve("uploads").resolve(stored))).isTrue();
            assertThat(
                            context.getBean(UserRepository.class)
                                    .findByUsername("admin")
                                    .orElseThrow()
                                    .getPasswordHash())
                    .isEqualTo(hash);
            assertThat(
                            context.getBean(JdbcTemplate.class)
                                    .queryForObject(
                                            "SELECT value FROM app_metadata WHERE"
                                                    + " key='seed_version'",
                                            String.class))
                    .isEqualTo("1");
        }
    }

    @Test
    void failedBatchRollsBackRowsFilesAndVersionThenCanRetry() throws Exception {
        try (var context = start(false)) {
            assertThat(context.getBean(PostRepository.class).count()).isZero();
            context.getBean(JdbcTemplate.class)
                    .execute(
                            "CREATE TRIGGER fail_seed BEFORE INSERT ON posts WHEN NEW.title LIKE"
                                + " 'JPA%' BEGIN SELECT RAISE(ABORT,'intentional seed failure');"
                                + " END");
        }
        assertThatThrownBy(
                        () -> {
                            try (var ignored = start(true)) {}
                        })
                .isInstanceOf(RuntimeException.class);
        try (var connection =
                        DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("blog.db"));
                var statement = connection.createStatement()) {
            for (String table : new String[] {"posts", "categories", "tags", "images"}) {
                try (var result = statement.executeQuery("SELECT count(*) FROM " + table)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isZero();
                }
            }
            try (var result =
                    statement.executeQuery(
                            "SELECT count(*) FROM app_metadata WHERE key='seed_version'")) {
                result.next();
                assertThat(result.getInt(1)).isZero();
            }
            statement.execute("DROP TRIGGER fail_seed");
        }
        try (var files = Files.list(directory.resolve("uploads"))) {
            assertThat(files.count()).isZero();
        }
        try (var context = start(true)) {
            assertThat(context.getBean(PostRepository.class).count()).isEqualTo(15);
        }
    }
}
