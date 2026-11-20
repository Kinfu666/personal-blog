package com.example.personalblog.service;

import com.example.personalblog.dto.NameForm;
import com.example.personalblog.dto.PostForm;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.TagRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeedService {
    private final boolean enabled;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TaxonomyService taxonomy;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final PostService posts;
    private final ImageService images;

    public SeedService(
            @Value("${app.seed.enabled}") boolean enabled,
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            TaxonomyService taxonomy,
            CategoryRepository categories,
            TagRepository tags,
            PostService posts,
            ImageService images) {
        this.enabled = enabled;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.taxonomy = taxonomy;
        this.categories = categories;
        this.tags = tags;
        this.posts = posts;
        this.images = images;
    }

    @Transactional(rollbackFor = Exception.class)
    public void initialize() throws IOException {
        if (!enabled
                || !jdbc.queryForList("SELECT value FROM app_metadata WHERE key='seed_version'")
                        .isEmpty()) return;
        List<SeedArticle> articles;
        try (var input = new ClassPathResource("seed/articles.json").getInputStream()) {
            articles = mapper.readValue(input, new TypeReference<List<SeedArticle>>() {});
        }
        for (String name : List.of("Java", "後端開發", "資料庫", "開發工具")) {
            if (!categories.existsByNormalizedName(name.toLowerCase(Locale.ROOT)))
                createName(false, name);
        }
        for (String name :
                List.of(
                        "Java 21",
                        "Spring Boot",
                        "MVC",
                        "JPA",
                        "SQLite",
                        "Security",
                        "測試",
                        "Markdown",
                        "Git",
                        "Maven")) {
            if (!tags.existsByNormalizedName(name.toLowerCase(Locale.ROOT))) createName(true, name);
        }
        Map<String, Long> categoryIds =
                categories.findAll().stream()
                        .collect(Collectors.toMap(c -> c.getNormalizedName(), c -> c.getId()));
        Map<String, Long> tagIds =
                tags.findAll().stream()
                        .collect(Collectors.toMap(t -> t.getNormalizedName(), t -> t.getId()));
        Map<String, String> imageUrls = new LinkedHashMap<>();
        for (String name : List.of("java", "mvc", "database", "security", "tools", "markdown")) {
            try (var input =
                    new ClassPathResource("seed/images/" + name + ".png").getInputStream()) {
                imageUrls.put(
                        name,
                        images.store(
                                        input.readAllBytes(),
                                        "示範圖示-" + name + ".png",
                                        "image/png",
                                        "admin")
                                .url());
            }
        }
        for (SeedArticle article : articles) {
            String content;
            try (var input =
                    new ClassPathResource("seed/articles/" + article.file()).getInputStream()) {
                content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (var image : imageUrls.entrySet())
                content = content.replace("{{image:" + image.getKey() + "}}", image.getValue());
            PostForm form = new PostForm();
            form.setTitle(article.title());
            form.setSummary(article.summary());
            form.setContent(content);
            form.setCategoryId(categoryIds.get(article.category().toLowerCase(Locale.ROOT)));
            form.setTagIds(
                    article.tags().stream()
                            .map(name -> tagIds.get(name.toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
            form.setStatus(article.status());
            posts.save(null, form, "admin");
        }
        // metadata 與所有文章、圖片 metadata 使用同一筆資料庫交易；檔案則由 ImageService 補償。
        jdbc.update("INSERT INTO app_metadata(key,value) VALUES('seed_version','1')");
    }

    private void createName(boolean tag, String name) {
        NameForm form = new NameForm();
        form.setName(name);
        taxonomy.save(tag, null, form);
    }

    public record SeedArticle(
            String file,
            String title,
            String summary,
            String category,
            List<String> tags,
            PostStatus status) {}
}
