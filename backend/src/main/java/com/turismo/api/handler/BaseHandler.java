package com.turismo.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.ConstraintCatalog;
import com.turismo.api.util.DbErrors;
import com.turismo.api.util.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base handler for HTTP endpoints
 *
 * Cada handler implementa {@link #route(Request)}; os erros são convertidos aqui
 * numa resposta JSON {"error", "code", "fields"} com o estado HTTP adequado.
 */
public abstract class BaseHandler implements HttpHandler {

    /** Tamanho máximo do corpo de um pedido (os formulários são pequenos). */
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final String basePath;
    protected final ConnectionProvider connections;

    protected BaseHandler(String basePath) {
        this(basePath, ConnectionProvider.DEFAULT);
    }

    protected BaseHandler(String basePath, ConnectionProvider connections) {
        this.basePath = basePath;
        this.connections = connections;
    }

    protected abstract void route(Request request) throws IOException, SQLException;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Request request = new Request(exchange, segments(exchange.getRequestURI().getPath()));
            route(request);
        } catch (ApiException e) {
            logIfServerError(exchange, e.getStatus(), e);
            sendError(exchange, e);
        } catch (SQLException e) {
            ApiException error = DbErrors.translate(e);
            System.err.println("[" + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + "] "
                    + "SQL " + e.getErrorCode() + ": " + firstLine(e.getMessage()));
            sendError(exchange, error);
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendError(exchange, new ApiException(500, "ERRO_INTERNO", "Erro interno no servidor."));
        } finally {
            exchange.close();
        }
    }

    private List<String> segments(String path) {
        if (!path.startsWith(basePath)) {
            throw ApiException.notFound("Endpoint não encontrado.");
        }
        String rest = path.substring(basePath.length());
        List<String> parts = new ArrayList<>();
        for (String part : rest.split("/")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts;
    }

    /**
     * Sends a JSON response
     * @param exchange the HTTP exchange
     * @param json the JSON string to send
     * @param statusCode the HTTP status code
     * @throws IOException if an I/O error occurs
     */
    protected void sendJsonResponse(HttpExchange exchange, String json, int statusCode) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Sends an error response
     * @param exchange the HTTP exchange
     * @param message the error message
     * @param statusCode the HTTP status code
     * @throws IOException if an I/O error occurs
     */
    protected void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        sendError(exchange, new ApiException(statusCode, "ERRO", message));
    }

    private void sendError(HttpExchange exchange, ApiException e) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        body.put("code", e.getCode());
        if (!e.getFields().isEmpty()) {
            body.put("fields", e.getFields());
        }
        sendJsonResponse(exchange, Json.toJson(body), e.getStatus());
    }

    private static void logIfServerError(HttpExchange exchange, int status, ApiException e) {
        if (status >= 500) {
            String cause = e.getCause() == null ? "" : " (" + firstLine(e.getCause().getMessage()) + ")";
            System.err.println("[" + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + "] "
                    + e.getCode() + ": " + e.getMessage() + cause);
        }
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int nl = text.indexOf('\n');
        return nl < 0 ? text : text.substring(0, nl);
    }

    // ---------------------------------------------------------------
    // Ajudas comuns aos handlers
    // ---------------------------------------------------------------

    protected static ApiException methodNotAllowed() {
        return new ApiException(405, "METODO_NAO_PERMITIDO", "Método não permitido neste endpoint.");
    }

    protected static ApiException notFound(String what) {
        return ApiException.notFound(what + " inexistente.");
    }

    /** Valores permitidos pela constraint CHECK "coluna in (...)" da tabela. */
    protected List<String> domain(String table, String column) throws SQLException {
        return ConstraintCatalog.domain(connections, table, column);
    }

    /** Texto sem espaços nas pontas; texto vazio passa a null. */
    protected static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.badRequest("Data inválida em " + field + " (use AAAA-MM-DD).");
        }
    }

    protected static BigDecimal parseDecimal(String value, String field) {
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException e) {
            throw ApiException.badRequest("Número inválido em " + field + ".");
        }
    }

    protected static Map<String, Object> message(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", text);
        return body;
    }

    protected static Map<String, Object> message(String text, Object data) {
        Map<String, Object> body = message(text);
        body.put("data", data);
        return body;
    }

    /**
     * Pedido HTTP já decomposto: método, segmentos do caminho depois do
     * endpoint base, parâmetros da query string e corpo.
     */
    protected final class Request {
        final HttpExchange exchange;
        final String method;
        final List<String> segments;
        final Map<String, String> query;

        Request(HttpExchange exchange, List<String> segments) {
            this.exchange = exchange;
            this.method = exchange.getRequestMethod().toUpperCase();
            this.segments = Collections.unmodifiableList(segments);
            this.query = parseQuery(exchange.getRequestURI().getRawQuery());
        }

        boolean is(String m) {
            return method.equals(m);
        }

        int size() {
            return segments.size();
        }

        String segment(int index) {
            return segments.get(index);
        }

        String param(String name) {
            return clean(query.get(name));
        }

        String body() throws IOException {
            try (InputStream in = exchange.getRequestBody()) {
                byte[] data = in.readNBytes(MAX_BODY_BYTES + 1);
                if (data.length > MAX_BODY_BYTES) {
                    throw new ApiException(413, "PEDIDO_GRANDE", "O pedido é demasiado grande.");
                }
                return new String(data, StandardCharsets.UTF_8);
            }
        }

        <T> T body(Class<T> type) throws IOException {
            return Json.fromJson(body(), type);
        }

        void send(int status, Object value) throws IOException {
            sendJsonResponse(exchange, Json.toJson(value), status);
        }

        void ok(Object value) throws IOException {
            send(200, value);
        }

        void created(Object value) throws IOException {
            send(201, value);
        }
    }

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }
}
