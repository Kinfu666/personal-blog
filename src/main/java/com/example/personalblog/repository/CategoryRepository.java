package com.example.personalblog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.personalblog.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNormalizedNameAndIdNot(String name, Long id);

    boolean existsByNormalizedName(String name);

    List<Category> findAllByOrderByNameAsc();
}
