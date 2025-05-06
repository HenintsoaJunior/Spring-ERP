package com.spring.spring.utils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private static final DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CustomZonedDateTimeDeserializer() {
        this(null);
    }

    public CustomZonedDateTimeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String date = p.getText();
        if (date == null || date.isEmpty()) {
            return null;
        }

        try {
            // Essayer le format ISO d'abord (ex. "2025-03-23T17:51:02.000000Z")
            return ZonedDateTime.parse(date, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Essayer le format personnalisé (ex. "2025-03-24 17:51:02")
                return ZonedDateTime.parse(date + "Z", CUSTOM_FORMATTER.withZone(java.time.ZoneOffset.UTC));
            } catch (DateTimeParseException ex) {
                throw new IOException("Unable to parse date: " + date, ex);
            }
        }
    }
}