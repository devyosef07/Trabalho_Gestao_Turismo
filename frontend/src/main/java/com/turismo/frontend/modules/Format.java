package com.turismo.frontend.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Formatação em português (Portugal/Moçambique): datas dd/MM/aaaa, vírgula
 * decimal e valores em meticais (MT).
 */
public final class Format {

    public static final Locale PT = Locale.of("pt", "PT");
    public static final String EMPTY = "—";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(PT);

    private Format() {
    }

    public static String date(LocalDate d) {
        return d == null ? EMPTY : DATE.format(d);
    }

    public static LocalDate parseIsoDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso.length() > 10 ? iso.substring(0, 10) : iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String money(BigDecimal v) {
        if (v == null) {
            return EMPTY;
        }
        DecimalFormat f = new DecimalFormat("#,##0.00", SYMBOLS);
        return f.format(v) + " MT";
    }

    public static String number(BigDecimal v) {
        if (v == null) {
            return EMPTY;
        }
        DecimalFormat f = new DecimalFormat("#,##0.##", SYMBOLS);
        return f.format(v);
    }

    public static String rating(BigDecimal v) {
        if (v == null) {
            return EMPTY;
        }
        DecimalFormat f = new DecimalFormat("0.0", SYMBOLS);
        return f.format(v);
    }

    /** Eixos dos gráficos: 0 / 500 / 1 mil / 2,5 mil / 1 M. */
    public static String compact(double v) {
        DecimalFormat f = new DecimalFormat("#,##0.#", SYMBOLS);
        if (Math.abs(v) >= 1_000_000) {
            return f.format(v / 1_000_000) + " M";
        }
        if (Math.abs(v) >= 1_000) {
            return f.format(v / 1_000) + " mil";
        }
        return f.format(v);
    }

    public static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    // ------------------------------------------------------------ JSON

    public static String str(JsonObject o, String key) {
        if (o == null || !o.has(key)) {
            return null;
        }
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    public static BigDecimal dec(JsonObject o, String key) {
        String s = str(o, key);
        try {
            return s == null ? null : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer integer(JsonObject o, String key) {
        BigDecimal d = dec(o, key);
        return d == null ? null : d.intValue();
    }

    /** Texto sem acentos e em minúsculas, para a pesquisa ignorar acentos ("atracao" encontra "Atração"). */
    public static String fold(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(PT);
    }

    /** "nome_destino" → "Nome destino" (cabeçalhos das views). */
    public static String humanize(String column) {
        String s = column.replace('_', ' ').trim();
        if (s.toLowerCase().startsWith("id ")) {
            s = "Código " + s.substring(3);
        }
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(PT);
    }
}
