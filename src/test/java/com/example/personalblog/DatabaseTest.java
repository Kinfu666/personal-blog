package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.personalblog.entity.Category;
import com.example.personalblog.entity.Image;
import com.example.personalblog.entity.Post;
import com.example.personalblog.entity.Tag;
import com.example.personalblog.entity.User;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.ImageRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.TagRepository;
import com.example.personalblog.repository.UserRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.Set;

@SpringBootTest(properties = {"app.seed.enabled=false"})
class DatabaseTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        registry.add("app.data-dir", Files.createTempDirectory("blog-db-test-")::toString);
    }

    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;
    @Autowired PostRepository posts;
    @Autowired ImageRepository images;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;

    @Test
    @Transactional
    void sqliteCrudAndRelations() {
        User author = users.save(new User("db-test", "hash"));
        Category category = categories.save(new Category("Java"));
        Tag tag = tags.save(new Tag("JPA"));
        Post post = new Post(author);
        post.revise("標題", "摘要", "# 內容", category, Set.of(tag), PostStatus.PUBLISHED);
        posts.saveAndFlush(post);
        Long id = post.getId();
        em.clear();
        Post loaded = posts.findOneById(id).orElseThrow();
        assertThat(loaded.getTags()).extracting(Tag::getName).containsExactly("JPA");
        assertThat(loaded.getPublishedAt()).isEqualTo(post.getPublishedAt());
        loaded.revise("更新", "摘要", "內容", category, Set.of(tag), PostStatus.DRAFT);
        posts.flush();
        assertThat(loaded.getPublishedAt()).isNull();
        Image image =
                images.saveAndFlush(new Image("image.png", "unique.png", "image/png", 10, author));
        assertThat(images.findByStoredName("unique.png")).isPresent();
        images.delete(image);
        posts.delete(loaded);
        posts.flush();
        assertThat(jdbc.queryForObject("select count(*) from post_tags", Integer.class)).isZero();
        assertThat(tags.existsById(tag.getId())).isTrue();
    }

    @Test
    void foreignKeysAndVersionAreReal() throws Exception {
        dataSource
                .unwrap(com.zaxxer.hikari.HikariDataSource.class)
                .getHikariPoolMXBean()
                .softEvictConnections();
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO post_tags(post_id,tag_id)"
                                                + " VALUES(99999,99999)"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("SQLITE_CONSTRAINT_FOREIGNKEY");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT value FROM app_metadata WHERE key='schema_version'",
                                String.class))
                .isEqualTo("1");
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
    }
}
