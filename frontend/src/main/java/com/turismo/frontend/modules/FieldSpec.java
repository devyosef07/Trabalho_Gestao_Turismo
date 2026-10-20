package com.turismo.frontend.modules;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Campo de formulário: tipo de controlo, regras de validação e origem das opções. */
public final class FieldSpec {

    public enum Type { TEXT, TEXTAREA, INTEGER, MONEY, RATING, DATE, TIME, DOMAIN, FK, MULTI_FK }

    final String key;
    final String label;
    final Type type;
    boolean required;
    int maxLength = 0;
    Integer maxDigits;
    String help;
    /** DOMAIN: chave "TABELA.COLUNA" de /api/dominios. */
    String domainKey;
    final List<String> excluded = new ArrayList<>();
    /** FK / MULTI_FK: endpoint e como mostrar cada opção. */
    String fkEndpoint;
    String fkIdKey;
    Function<JsonObject, String> fkLabel;
    boolean onCreate = true;
    boolean onEdit = true;
    String defaultValue;

    private FieldSpec(String key, String label, Type type) {
        this.key = key;
        this.label = label;
        this.type = type;
    }

    public static FieldSpec text(String key, String label, int maxLength) {
        FieldSpec f = new FieldSpec(key, label, Type.TEXT);
        f.maxLength = maxLength;
        return f;
    }

    public static FieldSpec textArea(String key, String label, int maxLength) {
        FieldSpec f = new FieldSpec(key, label, Type.TEXTAREA);
        f.maxLength = maxLength;
        return f;
    }

    public static FieldSpec integer(String key, String label, int maxDigits) {
        FieldSpec f = new FieldSpec(key, label, Type.INTEGER);
        f.maxDigits = maxDigits;
        return f;
    }

    public static FieldSpec money(String key, String label) {
        return new FieldSpec(key, label, Type.MONEY);
    }

    public static FieldSpec rating(String key, String label) {
        FieldSpec f = new FieldSpec(key, label, Type.RATING);
        f.help = "De 0,0 a 5,0, com uma casa decimal.";
        return f;
    }

    public static FieldSpec date(String key, String label) {
        return new FieldSpec(key, label, Type.DATE);
    }

    public static FieldSpec time(String key, String label) {
        FieldSpec f = new FieldSpec(key, label, Type.TIME);
        f.help = "Formato HH:MM, de 00:00 a 23:59.";
        return f;
    }

    public static FieldSpec domain(String key, String label, String domainKey) {
        FieldSpec f = new FieldSpec(key, label, Type.DOMAIN);
        f.domainKey = domainKey;
        return f;
    }

    public static FieldSpec fk(String key, String label, String endpoint, String idKey, Function<JsonObject, String> display) {
        FieldSpec f = new FieldSpec(key, label, Type.FK);
        f.fkEndpoint = endpoint;
        f.fkIdKey = idKey;
        f.fkLabel = display;
        return f;
    }

    public static FieldSpec multiFk(String key, String label, String endpoint, String idKey, Function<JsonObject, String> display) {
        FieldSpec f = fk(key, label, endpoint, idKey, display);
        return new FieldSpec(key, label, Type.MULTI_FK).copyFk(f);
    }

    private FieldSpec copyFk(FieldSpec other) {
        fkEndpoint = other.fkEndpoint;
        fkIdKey = other.fkIdKey;
        fkLabel = other.fkLabel;
        return this;
    }

    public FieldSpec required() {
        required = true;
        return this;
    }

    public FieldSpec help(String text) {
        help = text;
        return this;
    }

    public FieldSpec exclude(String... values) {
        excluded.addAll(List.of(values));
        return this;
    }

    public FieldSpec createOnly() {
        onEdit = false;
        return this;
    }

    public FieldSpec editOnly() {
        onCreate = false;
        return this;
    }

    public FieldSpec defaultValue(String value) {
        defaultValue = value;
        return this;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public Type type() {
        return type;
    }
}
