package com.example.personalblog.repository;

import com.example.personalblog.entity.Tag;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByNormalizedNameAndIdNot(String name, Long id);

    boolean existsByNormalizedName(String name);

    java.util.List<Tag> findAllByOrderByNameAsc();
}
