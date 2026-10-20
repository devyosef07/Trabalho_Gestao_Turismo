package com.turismo.api.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validação dos dados recebidos, de acordo com os tipos e regras de tabelas.sql.
 * Junta todos os erros de uma vez para a interface os mostrar campo a campo.
 */
public final class Validator {

    private static final Pattern HORA = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public Validator text(String field, String value, int maxLength, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                errors.putIfAbsent(field, "Campo obrigatório.");
            }
        } else if (value.trim().length() > maxLength) {
            errors.putIfAbsent(field, "Máximo de " + maxLength + " caracteres.");
        }
        return this;
    }

    public Validator required(String field, Object value) {
        if (value == null || (value instanceof String && ((String) value).isBlank())) {
            errors.putIfAbsent(field, "Campo obrigatório.");
        }
        return this;
    }

    public Validator oneOf(String field, String value, Collection<String> allowed) {
        if (value != null && !value.isBlank() && !allowed.isEmpty() && !allowed.contains(value)) {
            errors.putIfAbsent(field, "Valor não permitido. Use: " + String.join(", ", allowed) + ".");
        }
        return this;
    }

    public Validator positive(String field, Integer value, int maxDigits) {
        if (value != null) {
            if (value <= 0) {
                errors.putIfAbsent(field, "Tem de ser maior que zero.");
            } else if (String.valueOf(value).length() > maxDigits) {
                errors.putIfAbsent(field, "Máximo de " + maxDigits + " dígitos.");
            }
        }
        return this;
    }

    /** NUMBER(2,1) entre 0.0 e 5.0, usado em classificações e avaliações. */
    public Validator rating(String field, BigDecimal value) {
        if (value != null) {
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("5.0")) > 0) {
                errors.putIfAbsent(field, "Tem de estar entre 0,0 e 5,0.");
            } else if (value.stripTrailingZeros().scale() > 1) {
                errors.putIfAbsent(field, "Use no máximo uma casa decimal.");
            }
        }
        return this;
    }

    /** NUMBER(10,2) maior que zero. */
    public Validator money(String field, BigDecimal value) {
        if (value != null) {
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.putIfAbsent(field, "Tem de ser maior que zero.");
            } else if (value.stripTrailingZeros().scale() > 2) {
                errors.putIfAbsent(field, "Use no máximo duas casas decimais.");
            } else if (value.compareTo(new BigDecimal("99999999.99")) > 0) {
                errors.putIfAbsent(field, "Valor demasiado elevado.");
            }
        }
        return this;
    }

    public Validator hour(String field, String value) {
        if (value != null && !value.isBlank() && !HORA.matcher(value.trim()).matches()) {
            errors.putIfAbsent(field, "Use o formato HH:MM (00:00 a 23:59).");
        }
        return this;
    }

    public Validator after(String field, String laterValue, String earlierValue, String message) {
        if (laterValue != null && earlierValue != null && laterValue.compareTo(earlierValue) <= 0) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public Validator after(String field, LocalDate later, LocalDate earlier, String message) {
        if (later != null && earlier != null && !later.isAfter(earlier)) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public Validator check(boolean condition, String field, String message) {
        if (!condition) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public Map<String, String> errors() {
        return errors;
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw new ApiException(422, "VALIDACAO", "Existem campos com valores inválidos.", errors, null);
        }
    }
}
