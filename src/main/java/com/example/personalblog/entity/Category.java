package com.example.personalblog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends NamedEntity {
    protected Category() {}

    public Category(String name) {
        rename(name);
    }
}
