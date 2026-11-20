package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.personalblog.dto.PostForm;
import com.example.personalblog.dto.PostListItem;
import com.example.personalblog.entity.Category;
import com.example.personalblog.entity.Tag;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.TagRepository;
import com.example.personalblog.service.PostService;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class PublicQueryTest {
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) throws Exception {
        r.add("app.data-dir", Files.createTempDirectory("blog-query-")::toString);
    }

    @Autowired PostService posts;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicQueriesCombineConditionsAndKeepPagination() throws Exception {
        Long category = categories.save(new Category("Java")).getId();
        Long other = categories.save(new Category("資料庫")).getId();
        Long tag = tags.save(new Tag("Spring")).getId();
        List<Long> published = new ArrayList<>();
        Long draft = null;
        for (int i = 0; i < 11; i++) {
            PostForm f = new PostForm();
            f.setTitle("札記 " + i);
            f.setSummary("摘要搜尋");
            f.setContent("# Keyword 技術內容");
            f.setCategoryId(i == 10 ? other : category);
            f.setTagIds(i == 10 ? Set.of() : Set.of(tag));
            f.setStatus(i == 9 ? PostStatus.DRAFT : PostStatus.PUBLISHED);
            Long id = posts.save(null, f, "admin");
            if (i == 9) draft = id;
            else published.add(id);
        }
        jdbc.update(
                "UPDATE posts SET published_at='2026-01-02T03:04:05.000000000Z' WHERE"
                        + " status='PUBLISHED'");
        var first = posts.list(true, null, null, null, null, 0);
        assertThat(first.getTotalElements()).isEqualTo(10);
        assertThat(first.getContent()).hasSize(6);
        var expected = published.stream().sorted(Comparator.reverseOrder()).limit(6).toList();
        assertThat(first.map(PostListItem::id).getContent()).isEqualTo(expected);
        var combined = posts.list(true, "keyword", category, tag, null, 0);
        assertThat(combined.getTotalElements()).isEqualTo(9);
        assertThat(combined.hasNext()).isTrue();
        assertThat(posts.list(true, "keyword", other, tag, null, 0)).isEmpty();
        assertThat(posts.list(true, "摘要搜尋", category, tag, null, 1).getContent()).hasSize(3);
        assertThat(posts.list(true, "%", null, null, null, 0)).isEmpty();
        mvc.perform(
                        get("/").param("q", "keyword")
                                .param("category", category.toString())
                                .param("tag", tag.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("page=1")))
                .andExpect(content().string(Matchers.containsString("q=keyword")));
        mvc.perform(get("/posts/" + draft)).andExpect(status().isNotFound());
        mvc.perform(get("/posts/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/posts/" + published.getFirst()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<h1>Keyword")));
        mvc.perform(get("/").param("q", "絕無結果"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("沒有符合條件")));
        mvc.perform(get("/about")).andExpect(status().isOk());
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("管理總覽")));
    }
}
