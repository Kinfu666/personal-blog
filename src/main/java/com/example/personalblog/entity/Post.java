package com.example.personalblog.entity;

import com.example.personalblog.enums.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status = PostStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @Convert(converter = UtcInstantConverter.class)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Convert(converter = UtcInstantConverter.class)
    @Column(name = "published_at")
    private Instant publishedAt;

    protected Post() {}

    public Post(User author) {
        this.author = author;
    }

    public void revise(
            String title,
            String summary,
            String content,
            Category category,
            Collection<Tag> tags,
            PostStatus next) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.category = category;
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = Instant.now();
        if (next == PostStatus.DRAFT) publishedAt = null;
        else if (status != PostStatus.PUBLISHED) publishedAt = Instant.now();
        status = next;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public User getAuthor() {
        return author;
    }

    public Category getCategory() {
        return category;
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
