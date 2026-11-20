package com.example.personalblog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends NamedEntity {
    protected Tag() {}

    public Tag(String name) {
        rename(name);
    }
}
