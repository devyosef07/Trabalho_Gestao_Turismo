package com.turismo.frontend.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Fonte das opções dos formulários. As listas de valores permitidos vêm das
 * constraints CHECK da BD (/api/dominios) e ficam em memória; as opções de
 * chaves estrangeiras são sempre pedidas de novo (os dados mudam).
 */
public class Lookups {

    private final ApiClient api;
    private volatile JsonObject domains;

    public Lookups(ApiClient api) {
        this.api = api;
    }

    public ApiClient api() {
        return api;
    }

    public List<String> domain(String key) throws ApiException {
        JsonObject d = domains;
        if (d == null) {
            d = api.getObject("/dominios");
            domains = d;
        }
        List<String> values = new ArrayList<>();
        if (d.has(key) && d.get(key).isJsonArray()) {
            for (JsonElement e : d.getAsJsonArray(key)) {
                values.add(e.getAsString());
            }
        }
        if (values.isEmpty()) {
            throw new ApiException(500, "DOMINIO_EM_FALTA",
                    "A base de dados não tem a lista de valores de " + key + ".", null, null);
        }
        return values;
    }

    public List<JsonObject> rows(String endpoint) throws ApiException {
        JsonArray array = api.getArray(endpoint);
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement e : array) {
            result.add(e.getAsJsonObject());
        }
        return result;
    }
}
