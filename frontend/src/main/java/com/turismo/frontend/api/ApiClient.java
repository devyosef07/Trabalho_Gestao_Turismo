package com.turismo.frontend.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP da API REST do backend.
 *
 * Todos os métodos bloqueiam até haver resposta: devem ser chamados fora da
 * Event Dispatch Thread (ver {@code ui.async.Task}).
 */
public class ApiClient {

    private final ClientConfig config;
    private final HttpClient http;

    public ApiClient(ClientConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
    }

    public String baseUrl() {
        return config.baseUrl();
    }

    public JsonArray getArray(String path) throws ApiException {
        JsonElement e = send("GET", path, null);
        if (!e.isJsonArray()) {
            throw invalidResponse(path);
        }
        return e.getAsJsonArray();
    }

    public JsonObject getObject(String path) throws ApiException {
        return asObject(path, send("GET", path, null));
    }

    public JsonObject post(String path, JsonObject body) throws ApiException {
        return asObject(path, send("POST", path, body == null ? new JsonObject() : body));
    }

    public JsonObject put(String path, JsonObject body) throws ApiException {
        return asObject(path, send("PUT", path, body));
    }

    public JsonObject delete(String path) throws ApiException {
        return asObject(path, send("DELETE", path, null));
    }

    /** Codifica um valor para ser usado como segmento do caminho (ex.: chaves compostas). */
    public static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    JsonElement send(String method, String path, JsonObject body) throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                .timeout(config.timeout())
                .header("Accept", "application/json");
        if (body != null) {
            builder.header("Content-Type", "application/json; charset=UTF-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new ApiException(ApiException.UNREACHABLE, "API_INDISPONIVEL",
                    "Não foi possível contactar a API em " + config.baseUrl()
                            + ". Verifique se o backend está em execução.", null, e);
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "API_TEMPO_ESGOTADO", "A API demorou demasiado a responder.", null, e);
        } catch (IOException e) {
            throw new ApiException(ApiException.UNREACHABLE, "API_INDISPONIVEL",
                    "Falha na comunicação com a API: " + e.getMessage(), null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiException.UNREACHABLE, "CANCELADO", "Operação cancelada.", null, e);
        }

        JsonElement parsed = parse(response.body());
        if (response.statusCode() >= 400) {
            throw error(response.statusCode(), parsed);
        }
        if (parsed == null) {
            throw invalidResponse(path);
        }
        return parsed;
    }

    private static JsonElement parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(text);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static ApiException error(int status, JsonElement body) {
        String message = "A API devolveu o erro HTTP " + status + ".";
        String code = null;
        Map<String, String> fields = new LinkedHashMap<>();
        if (body != null && body.isJsonObject()) {
            JsonObject o = body.getAsJsonObject();
            if (o.has("error") && o.get("error").isJsonPrimitive()) {
                message = o.get("error").getAsString();
            }
            if (o.has("code") && o.get("code").isJsonPrimitive()) {
                code = o.get("code").getAsString();
            }
            if (o.has("fields") && o.get("fields").isJsonObject()) {
                for (Map.Entry<String, JsonElement> f : o.getAsJsonObject("fields").entrySet()) {
                    fields.put(f.getKey(), f.getValue().getAsString());
                }
            }
        }
        return new ApiException(status, code, message, fields, null);
    }

    private static JsonObject asObject(String path, JsonElement e) throws ApiException {
        if (!e.isJsonObject()) {
            throw invalidResponse(path);
        }
        return e.getAsJsonObject();
    }

    private static ApiException invalidResponse(String path) {
        return new ApiException(502, "RESPOSTA_INVALIDA", "Resposta inesperada da API em " + path + ".", null, null);
    }
}
