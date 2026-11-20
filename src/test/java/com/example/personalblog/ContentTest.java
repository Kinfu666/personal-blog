package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.personalblog.dto.NameForm;
import com.example.personalblog.dto.NamedItem;
import com.example.personalblog.dto.PostForm;
import com.example.personalblog.entity.Category;
import com.example.personalblog.entity.Tag;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.TagRepository;
import com.example.personalblog.service.PostService;
import com.example.personalblog.service.TaxonomyService;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class ContentTest {
    @Test
    void formLengthBoundariesAreEnforcedInService() {
        PostForm f = form();
        f.setTitle("文".repeat(150));
        f.setSummary("文".repeat(300));
        f.setContent("文".repeat(50000));
        assertThat(posts.save(null, f, "admin")).isNotNull();
        f.setTitle("文".repeat(151));
        assertThatThrownBy(() -> posts.save(null, f, "admin"))
                .isInstanceOf(ConstraintViolationException.class);
        f.setTitle("標題");
        f.setSummary("文".repeat(301));
        assertThatThrownBy(() -> posts.save(null, f, "admin"))
                .isInstanceOf(ConstraintViolationException.class);
        f.setSummary("摘要");
        f.setContent("文".repeat(50001));
        assertThatThrownBy(() -> posts.save(null, f, "admin"))
                .isInstanceOf(ConstraintViolationException.class);
        NameForm name = new NameForm();
        name.setName("字".repeat(50));
        taxonomy.save(false, null, name);
        name.setName("字".repeat(51));
        assertThatThrownBy(() -> taxonomy.save(false, null, name))
                .isInstanceOf(RuleViolation.class);
    }

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) throws Exception {
        r.add("app.data-dir", Files.createTempDirectory("blog-content-")::toString);
    }

    @Autowired PostService posts;
    @Autowired TaxonomyService taxonomy;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;
    @Autowired PostRepository repository;
    @Autowired MockMvc mvc;
    private Long category;
    private Long tag;

    @BeforeEach
    void prepare() {
        repository.deleteAll();
        tags.deleteAll();
        categories.deleteAll();
        category = categories.save(new Category("Java")).getId();
        tag = tags.save(new Tag("Testing")).getId();
    }

    private PostForm form() {
        PostForm f = new PostForm();
        f.setTitle("  技術文章  ");
        f.setSummary("摘要");
        f.setContent("# 內容\n實作驗證");
        f.setCategoryId(category);
        f.setTagIds(Set.of(tag));
        return f;
    }

    @Test
    void lifecycleRelationsAndDeletion() {
        PostForm f = form();
        Long id = posts.save(null, f, "admin");
        assertThat(posts.edit(id).getStatus()).isEqualTo(PostStatus.DRAFT);
        f.setStatus(PostStatus.PUBLISHED);
        posts.save(id, f, "admin");
        var first = repository.findById(id).orElseThrow().getPublishedAt();
        assertThat(first).isNotNull();
        posts.save(id, f, "admin");
        assertThat(repository.findById(id).orElseThrow().getPublishedAt()).isEqualTo(first);
        f.setStatus(PostStatus.DRAFT);
        posts.save(id, f, "admin");
        assertThat(repository.findById(id).orElseThrow().getPublishedAt()).isNull();
        f.setStatus(PostStatus.PUBLISHED);
        posts.save(id, f, "admin");
        assertThat(repository.findById(id).orElseThrow().getPublishedAt()).isAfter(first);
        assertThatThrownBy(() -> taxonomy.delete(false, category))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> taxonomy.delete(true, tag)).isInstanceOf(RuleViolation.class);
        posts.delete(id);
        taxonomy.delete(false, category);
        taxonomy.delete(true, tag);
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void duplicateNamesAreNormalizedAndValidated() {
        NameForm f = new NameForm();
        f.setName("  JAVA  ");
        assertThatThrownBy(() -> taxonomy.save(false, null, f)).isInstanceOf(RuleViolation.class);
        f.setName(" Spring ");
        taxonomy.save(true, null, f);
        assertThat(taxonomy.list(true)).extracting(NamedItem::name).contains("Spring");
        f.setName(" ");
        assertThatThrownBy(() -> taxonomy.save(true, null, f)).isInstanceOf(RuleViolation.class);
    }

    @Test
    void invalidReferencesAndValidationAreRejected() {
        PostForm f = form();
        f.setCategoryId(9999L);
        assertThatThrownBy(() -> posts.save(null, f, "admin")).isInstanceOf(RuleViolation.class);
        f.setCategoryId(category);
        f.setTagIds(Set.of(9999L));
        assertThatThrownBy(() -> posts.save(null, f, "admin")).isInstanceOf(RuleViolation.class);
        f.setTagIds(Set.of());
        f.setTitle(" ");
        assertThatThrownBy(() -> posts.save(null, f, "admin"))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void adminFormsHaveCsrfAndPreserveInvalidValues() throws Exception {
        mvc.perform(get("/admin/posts/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("_csrf")));
        mvc.perform(
                        post("/admin/posts")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("title", "保留標題")
                                .param("content", "內容")
                                .param("categoryId", category.toString())
                                .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("postForm", "summary"))
                .andExpect(content().string(Matchers.containsString("保留標題")));
        mvc.perform(
                        post("/admin/posts")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("title", "文章")
                                .param("summary", "摘要")
                                .param("content", "內容")
                                .param("categoryId", category.toString())
                                .param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/admin/posts"));
        mvc.perform(get("/admin/posts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/categories").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/posts/9999/edit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
