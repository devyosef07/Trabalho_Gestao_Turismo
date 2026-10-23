package com.turismo.frontend.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sessão iniciada: o token (só em memória e, com "manter sessão", nas
 * preferências do utilizador do sistema) e os dados públicos da conta,
 * tal como a API os devolve.
 */
public record Session(String token, Account account, LocalDateTime startedAt, LocalDateTime expiresAt) {

    /** Dados públicos da conta (nunca inclui a palavra-passe nem o hash). */
    public record Account(long id, String username, String fullName, LocalDate birthDate, LocalDateTime createdAt) {
    }

    /** Lê a resposta de POST/GET /api/auth/sessao; {@code token} substitui o do JSON quando este não o traz. */
    static Session fromJson(JsonObject o, String token) {
        JsonObject c = o.getAsJsonObject("conta");
        Account account = new Account(
                c.get("idConta").getAsLong(),
                text(c, "nomeUtilizador"),
                text(c, "nomeCompleto"),
                date(text(c, "dataNascimento")),
                dateTime(text(c, "dataCriacao")));
        String t = o.has("token") ? text(o, "token") : token;
        return new Session(t, account, dateTime(text(o, "iniciadaEm")), dateTime(text(o, "expiraEm")));
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static LocalDate date(String s) {
        return s == null ? null : LocalDate.parse(s);
    }

    private static LocalDateTime dateTime(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}
