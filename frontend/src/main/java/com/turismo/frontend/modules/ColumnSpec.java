package com.turismo.frontend.modules;

import com.google.gson.JsonObject;
import com.turismo.frontend.ui.components.UI;

import java.util.function.Function;

/** Coluna da tabela de um módulo: campo JSON, cabeçalho e tipo de formatação. */
public final class ColumnSpec {

    public enum Type { TEXT, INTEGER, DECIMAL, MONEY, RATING, DATE, STATUS }

    final String key;
    final String header;
    final Type type;
    final int width;
    /** Para colunas de estado: tom da etiqueta conforme o valor. */
    Function<String, UI.Tone> tone = v -> UI.Tone.NEUTRAL;
    /** Valor calculado a partir da linha (ex.: "Carlos Silva (TR-002)"). */
    Function<JsonObject, String> derived;
    boolean detailOnly;

    ColumnSpec(String key, String header, Type type, int width) {
        this.key = key;
        this.header = header;
        this.type = type;
        this.width = width;
    }

    public String key() {
        return key;
    }

    public String header() {
        return header;
    }

    public Type type() {
        return type;
    }
}
