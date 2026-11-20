package com.example.personalblog.repository;

import com.example.personalblog.entity.Post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository
        extends JpaRepository<Post, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Post> {
    @org.springframework.data.jpa.repository.Query(
            "select p.content from Post p where (:status is null or p.status = :status)")
    java.util.List<String> findContents(
            @org.springframework.data.repository.query.Param("status")
                    com.example.personalblog.enums.PostStatus status);

    long countByStatus(com.example.personalblog.enums.PostStatus status);

    boolean existsByCategoryId(Long id);

    boolean existsByTagsId(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(
            attributePaths = {"category", "tags", "author"})
    java.util.Optional<Post> findOneById(Long id);

    @org.springframework.data.jpa.repository.Query(
            "select distinct p from Post p left join fetch p.tags join fetch p.category where p.id"
                    + " in :ids")
    java.util.List<Post> findWithRelationsByIds(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}
