package com.turismo.api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Serialização JSON partilhada. As datas usam o formato ISO (yyyy-MM-dd) e os
 * números são sempre escritos com ponto decimal, independentemente do locale.
 */
public final class Json {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String body, Class<T> type) {
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("O pedido não tem dados.");
        }
        try {
            T value = GSON.fromJson(body, type);
            if (value == null) {
                throw ApiException.badRequest("O pedido não tem dados.");
            }
            return value;
        } catch (JsonParseException | IllegalStateException e) {
            throw ApiException.badRequest("Dados inválidos no pedido: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }

    static final class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String text = in.nextString();
            if (text.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
            } catch (DateTimeParseException e) {
                throw new JsonParseException("data inválida '" + text + "' (use AAAA-MM-DD)");
            }
        }
    }
}
