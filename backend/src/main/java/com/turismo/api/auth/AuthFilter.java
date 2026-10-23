package com.turismo.api.auth;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.DbErrors;
import com.turismo.api.util.Json;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exige uma sessão válida ("Authorization: Bearer <token>") antes de entregar
 * o pedido ao handler. Fica em todos os endpoints de dados; /api/estado e
 * /api/auth continuam públicos.
 *
 * Se a BD não responder devolve 503 (e não 401), para a aplicação não
 * terminar a sessão por causa de uma falha de rede.
 */
public final class AuthFilter extends Filter {

    private final SessionService sessions;

    public AuthFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        ApiException error;
        try {
            String token = SessionService.bearer(exchange.getRequestHeaders().getFirst("Authorization"));
            if (sessions.find(token) != null) {
                chain.doFilter(exchange);
                return;
            }
            error = new ApiException(401, "SESSAO_INVALIDA",
                    "A sessão terminou ou é inválida. Inicie sessão novamente.");
        } catch (SQLException e) {
            error = DbErrors.translate(e);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", error.getMessage());
            body.put("code", error.getCode());
            byte[] bytes = Json.toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            if (error.getStatus() == 401) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
            }
            exchange.sendResponseHeaders(error.getStatus(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    @Override
    public String description() {
        return "Exige uma sessão de utilizador válida";
    }
}
