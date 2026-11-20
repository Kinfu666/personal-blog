package com.example.personalblog.enums;

public enum PostStatus {
    DRAFT("草稿"),
    PUBLISHED("已發布");
    private final String label;

    PostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
