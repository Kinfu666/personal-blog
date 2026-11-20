package com.example.personalblog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.personalblog.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByStoredName(String storedName);

    List<Image> findAllByOrderByIdDesc();
}
