package com.example.personalblog.service;

import java.util.List;
import java.util.Locale;

import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personalblog.dto.NameForm;
import com.example.personalblog.dto.NamedItem;
import com.example.personalblog.entity.Category;
import com.example.personalblog.entity.NamedEntity;
import com.example.personalblog.entity.Tag;
import com.example.personalblog.exception.NotFoundException;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.TagRepository;

@Service
public class TaxonomyService {
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final PostRepository posts;
    private final Validator validator;

    public TaxonomyService(
            CategoryRepository categories,
            TagRepository tags,
            PostRepository posts,
            Validator validator) {
        this.categories = categories;
        this.tags = tags;
        this.posts = posts;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<NamedItem> list(boolean tag) {
        List<? extends NamedEntity> values =
                tag ? tags.findAllByOrderByNameAsc() : categories.findAllByOrderByNameAsc();
        return values.stream().map(v -> new NamedItem(v.getId(), v.getName())).toList();
    }

    @Transactional
    public void save(boolean tag, Long id, NameForm form) {
        if (!validator.validate(form).isEmpty()) throw new RuleViolation("name", "名稱須為 1 至 50 字");
        String normalized = form.getName().toLowerCase(Locale.ROOT);
        boolean duplicate =
                tag
                        ? (id == null
                                ? tags.existsByNormalizedName(normalized)
                                : tags.existsByNormalizedNameAndIdNot(normalized, id))
                        : (id == null
                                ? categories.existsByNormalizedName(normalized)
                                : categories.existsByNormalizedNameAndIdNot(normalized, id));
        if (duplicate) throw new RuleViolation("name", "名稱已存在（英文字母不分大小寫）");
        if (tag) {
            Tag value =
                    id == null
                            ? new Tag(form.getName())
                            : tags.findById(id).orElseThrow(NotFoundException::new);
            value.rename(form.getName());
            tags.saveAndFlush(value);
        } else {
            Category value =
                    id == null
                            ? new Category(form.getName())
                            : categories.findById(id).orElseThrow(NotFoundException::new);
            value.rename(form.getName());
            categories.saveAndFlush(value);
        }
    }

    @Transactional
    public void delete(boolean tag, Long id) {
        if (tag) {
            Tag value = tags.findById(id).orElseThrow(NotFoundException::new);
            if (posts.existsByTagsId(id)) throw new RuleViolation("name", "仍有文章使用此標籤，無法刪除");
            tags.delete(value);
        } else {
            Category value = categories.findById(id).orElseThrow(NotFoundException::new);
            if (posts.existsByCategoryId(id)) throw new RuleViolation("name", "仍有文章使用此分類，無法刪除");
            categories.delete(value);
        }
    }
}
