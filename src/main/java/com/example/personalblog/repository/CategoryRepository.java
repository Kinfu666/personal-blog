package com.example.personalblog.repository;

import com.example.personalblog.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNormalizedNameAndIdNot(String name, Long id);

    boolean existsByNormalizedName(String name);

    java.util.List<Category> findAllByOrderByNameAsc();
}
