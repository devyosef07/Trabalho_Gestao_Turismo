package com.turismo.api.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erro com estado HTTP e mensagem em português pronta a mostrar ao utilizador.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, String> fields;

    public ApiException(int status, String code, String message) {
        this(status, code, message, Collections.emptyMap(), null);
    }

    public ApiException(int status, String code, String message, Throwable cause) {
        this(status, code, message, Collections.emptyMap(), cause);
    }

    public ApiException(int status, String code, String message, Map<String, String> fields, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "NAO_ENCONTRADO", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, "PEDIDO_INVALIDO", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, "CONFLITO", message);
    }

    public static ApiException rule(String message) {
        return new ApiException(422, "REGRA_NEGOCIO", message);
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
