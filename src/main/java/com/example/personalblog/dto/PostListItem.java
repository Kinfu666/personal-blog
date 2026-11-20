package com.example.personalblog.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.personalblog.enums.PostStatus;

public record PostListItem(
        Long id,
        String title,
        String summary,
        PostStatus status,
        Instant publishedAt,
        Instant updatedAt,
        NamedItem category,
        List<NamedItem> tags) {
    public String date() {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                .withZone(ZoneId.of("Asia/Taipei"))
                .format(publishedAt == null ? updatedAt : publishedAt);
    }
}
