package com.example.personalblog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.personalblog.entity.Post;
import com.example.personalblog.enums.PostStatus;

public interface PostRepository
        extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    @Query("select p.content from Post p where (:status is null or p.status = :status)")
    List<String> findContents(@Param("status") PostStatus status);

    long countByStatus(PostStatus status);

    boolean existsByCategoryId(Long id);

    boolean existsByTagsId(Long id);

    @EntityGraph(attributePaths = {"category", "tags", "author"})
    Optional<Post> findOneById(Long id);

    @Query(
            "select distinct p from Post p left join fetch p.tags join fetch p.category where p.id"
                    + " in :ids")
    List<Post> findWithRelationsByIds(@Param("ids") Collection<Long> ids);
}
