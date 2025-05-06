package com.spring.spring.utils;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

    public CustomZonedDateTimeSerializer() {
        this(null);
    }

    public CustomZonedDateTimeSerializer(Class<ZonedDateTime> t) {
        super(t);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.toString()); // Format ISO par défaut
        }
    }
}