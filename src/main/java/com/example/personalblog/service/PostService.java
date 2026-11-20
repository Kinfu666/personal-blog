package com.example.personalblog.service;

import com.example.personalblog.dto.NamedItem;
import com.example.personalblog.dto.PostDetail;
import com.example.personalblog.dto.PostForm;
import com.example.personalblog.dto.PostListItem;
import com.example.personalblog.entity.Category;
import com.example.personalblog.entity.Post;
import com.example.personalblog.entity.Tag;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.exception.NotFoundException;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.PostSpecifications;
import com.example.personalblog.repository.TagRepository;
import com.example.personalblog.repository.UserRepository;

import jakarta.validation.Validator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final UserRepository users;
    private final Validator validator;
    private final MarkdownService markdown;

    public PostService(
            PostRepository posts,
            CategoryRepository categories,
            TagRepository tags,
            UserRepository users,
            Validator validator,
            MarkdownService markdown) {
        this.posts = posts;
        this.categories = categories;
        this.tags = tags;
        this.users = users;
        this.validator = validator;
        this.markdown = markdown;
    }

    @Transactional(readOnly = true)
    public PostDetail published(Long id) {
        Post value =
                posts.findOneById(id)
                        .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                        .orElseThrow(NotFoundException::new);
        return new PostDetail(item(value), markdown.render(value.getContent()));
    }

    @Transactional
    public Long save(Long id, PostForm form, String username) {
        var errors = validator.validate(form);
        if (!errors.isEmpty()) throw new jakarta.validation.ConstraintViolationException(errors);
        Category category =
                categories
                        .findById(form.getCategoryId())
                        .orElseThrow(() -> new RuleViolation("categoryId", "分類不存在，請重新選擇"));
        List<Tag> selected = tags.findAllById(form.getTagIds());
        if (selected.size() != form.getTagIds().size())
            throw new RuleViolation("tagIds", "部分標籤已不存在，請重新選擇");
        Post value =
                id == null
                        ? new Post(
                                users.findByUsername(username).orElseThrow(NotFoundException::new))
                        : posts.findOneById(id).orElseThrow(NotFoundException::new);
        value.revise(
                form.getTitle(),
                form.getSummary(),
                form.getContent(),
                category,
                selected,
                form.getStatus());
        return posts.saveAndFlush(value).getId();
    }

    @Transactional(readOnly = true)
    public PostForm edit(Long id) {
        Post value = posts.findOneById(id).orElseThrow(NotFoundException::new);
        PostForm form = new PostForm();
        form.setTitle(value.getTitle());
        form.setSummary(value.getSummary());
        form.setContent(value.getContent());
        form.setStatus(value.getStatus());
        form.setCategoryId(value.getCategory().getId());
        form.setTagIds(
                value.getTags().stream()
                        .map(Tag::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        return form;
    }

    @Transactional
    public void delete(Long id) {
        Post value = posts.findById(id).orElseThrow(NotFoundException::new);
        posts.delete(value);
        posts.flush();
    }

    @Transactional(readOnly = true)
    public Page<PostListItem> list(
            boolean publicOnly,
            String query,
            Long category,
            Long tag,
            PostStatus status,
            int page) {
        Specification<Post> filter =
                PostSpecifications.matching(publicOnly, query, category, tag, status);
        Sort sort =
                publicOnly
                        ? Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
                        : Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        Page<Post> slice =
                posts.findAll(
                        filter,
                        PageRequest.of(
                                Math.min(
                                        Math.max(0, page),
                                        Integer.MAX_VALUE / (publicOnly ? 6 : 10)),
                                publicOnly ? 6 : 10,
                                sort));
        if (slice.isEmpty())
            return new PageImpl<>(List.of(), slice.getPageable(), slice.getTotalElements());
        // 先分頁單值資料，再一次載入該頁標籤，避免 collection fetch join 的記憶體分頁。
        Map<Long, Post> loaded =
                posts.findWithRelationsByIds(slice.stream().map(Post::getId).toList()).stream()
                        .collect(Collectors.toMap(Post::getId, p -> p));
        return slice.map(p -> item(loaded.get(p.getId())));
    }

    public static PostListItem item(Post value) {
        return new PostListItem(
                value.getId(),
                value.getTitle(),
                value.getSummary(),
                value.getStatus(),
                value.getPublishedAt(),
                value.getUpdatedAt(),
                new NamedItem(value.getCategory().getId(), value.getCategory().getName()),
                value.getTags().stream()
                        .sorted(Comparator.comparing(Tag::getName))
                        .map(t -> new NamedItem(t.getId(), t.getName()))
                        .toList());
    }
}
