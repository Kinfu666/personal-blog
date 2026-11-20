package com.example.personalblog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreviewForm(@NotNull @Size(max = 50000) String content) {}
