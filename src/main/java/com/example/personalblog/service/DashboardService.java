package com.example.personalblog.service;

import com.example.personalblog.dto.DashboardStats;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.repository.CategoryRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.TagRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final TagRepository tags;

    public DashboardService(
            PostRepository posts, CategoryRepository categories, TagRepository tags) {
        this.posts = posts;
        this.categories = categories;
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        return new DashboardStats(
                posts.count(),
                posts.countByStatus(PostStatus.DRAFT),
                categories.count(),
                tags.count());
    }
}
