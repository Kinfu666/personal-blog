package com.example.personalblog.repository;

import com.example.personalblog.entity.Image;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
    java.util.Optional<Image> findByStoredName(String storedName);

    java.util.List<Image> findAllByOrderByIdDesc();
}
