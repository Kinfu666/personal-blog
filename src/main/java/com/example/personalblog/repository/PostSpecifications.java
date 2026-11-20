package com.example.personalblog.repository;

import java.util.ArrayList;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.example.personalblog.entity.Post;
import com.example.personalblog.enums.PostStatus;

public final class PostSpecifications {
    private PostSpecifications() {}

    public static Specification<Post> matching(
            boolean publicOnly, String query, Long category, Long tag, PostStatus status) {
        return (root, cq, cb) -> {
            var predicates = new ArrayList<Predicate>();
            PostStatus selectedStatus = publicOnly ? PostStatus.PUBLISHED : status;
            if (selectedStatus != null)
                predicates.add(cb.equal(root.get("status"), selectedStatus));
            if (query != null && !query.isBlank()) {
                String pattern = "%" + escapeLike(query.strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                                cb.like(cb.lower(root.get("summary")), pattern, '\\'),
                                cb.like(cb.lower(root.get("content")), pattern, '\\')));
            }
            if (category != null)
                predicates.add(cb.equal(root.get("category").get("id"), category));
            if (tag != null) {
                var sub = cq.subquery(Long.class);
                var post = sub.from(Post.class);
                sub.select(post.get("id")).where(cb.equal(post.join("tags").get("id"), tag));
                predicates.add(root.get("id").in(sub));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
