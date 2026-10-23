package com.turismo.frontend;

import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.auth.AuthService;
import com.turismo.frontend.auth.Session;
import com.turismo.frontend.auth.TokenStore;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sessões para os testes de interface com a API falsa ({@link StubApi}).
 * São dados só de teste: nunca chegam à base de dados nem à aplicação real.
 */
final class TestSessions {

    static final String TOKEN = "token-de-teste";

    /** Resposta de POST/GET /api/auth/sessao para a conta de teste. */
    static final String SESSION_JSON = "{\"token\":\"" + TOKEN + "\",\"iniciadaEm\":\"2026-10-22T10:00:00\","
            + "\"expiraEm\":\"2026-10-29T10:00:00\",\"conta\":{\"idConta\":7,\"nomeUtilizador\":\"maria.cossa\","
            + "\"nomeCompleto\":\"Maria José Cossa\",\"dataNascimento\":\"1995-03-14\","
            + "\"dataCriacao\":\"2026-10-01T09:30:00\"}}";

    private TestSessions() {
    }

    static Session sample() {
        return new Session(TOKEN,
                new Session.Account(7, "maria.cossa", "Maria José Cossa", LocalDate.of(1995, 3, 14),
                        LocalDateTime.of(2026, 10, 1, 9, 30)),
                LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 29, 10, 0));
    }

    /** Serviço de autenticação com a sessão de teste já ativa e o token só em memória. */
    static AuthService signedIn(ApiClient client) {
        AuthService auth = new AuthService(client, TokenStore.memory());
        auth.resume(sample());
        return auth;
    }
}
