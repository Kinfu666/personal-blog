package com.example.personalblog.dto;

public record ImageResponse(
        Long id, String originalName, String url, String markdown, long sizeBytes) {}
