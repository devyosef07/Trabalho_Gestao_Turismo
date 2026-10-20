package com.turismo.frontend.api;

import java.util.Collections;
import java.util.Map;

/**
 * Erro devolvido pela API (ou falha ao contactá-la), com a mensagem em português
 * pronta a mostrar e, nos erros de validação, a mensagem de cada campo.
 */
public class ApiException extends Exception {

    /** Estado usado quando a API não respondeu (servidor desligado, rede...). */
    public static final int UNREACHABLE = 0;

    private final int status;
    private final String code;
    private final Map<String, String> fields;

    public ApiException(int status, String code, String message, Map<String, String> fields, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code == null ? "ERRO" : code;
        this.fields = fields == null ? Collections.emptyMap() : Collections.unmodifiableMap(fields);
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

    public boolean isUnreachable() {
        return status == UNREACHABLE;
    }

    /** Problemas de ligação (API ou BD) em que tentar de novo pode resolver. */
    public boolean isRetryable() {
        return status == UNREACHABLE || status == 503 || status == 504;
    }
}
