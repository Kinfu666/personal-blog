package com.example.personalblog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.personalblog.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByNormalizedNameAndIdNot(String name, Long id);

    boolean existsByNormalizedName(String name);

    List<Tag> findAllByOrderByNameAsc();
}
