package com.example.personalblog.entity;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class NamedEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void rename(String name) {
        this.name = name.strip();
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
    }
}
