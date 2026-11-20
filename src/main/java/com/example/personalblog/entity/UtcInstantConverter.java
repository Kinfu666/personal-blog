package com.example.personalblog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@Converter
public class UtcInstantConverter implements AttributeConverter<Instant, String> {
    private static final DateTimeFormatter FORMAT =
            new DateTimeFormatterBuilder().appendInstant(9).toFormatter();

    public String convertToDatabaseColumn(Instant value) {
        return value == null ? null : FORMAT.format(value);
    }

    public Instant convertToEntityAttribute(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
