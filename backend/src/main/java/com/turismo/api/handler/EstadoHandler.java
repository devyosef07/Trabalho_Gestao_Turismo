package com.turismo.api.handler;

import com.turismo.api.dao.SchemaDAO;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.DbErrors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/estado: estado da API e da base de dados.
 *
 * Responde sempre 200 quando a API está a funcionar; o campo "bd" indica se a
 * base de dados respondeu, para a interface mostrar o estado da ligação.
 */
public class EstadoHandler extends BaseHandler {
    private final SchemaDAO schemaDAO;

    public EstadoHandler() {
        this(new SchemaDAO(), ConnectionProvider.DEFAULT);
    }

    public EstadoHandler(SchemaDAO schemaDAO, ConnectionProvider connections) {
        super("/api/estado", connections);
        this.schemaDAO = schemaDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (!r.is("GET")) {
            throw methodNotAllowed();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api", "OK");
        try {
            body.putAll(schemaDAO.status());
            body.put("bd", "OK");
        } catch (SQLException e) {
            ApiException error = DbErrors.translate(e);
            body.put("bd", "ERRO");
            body.put("bdErro", error.getMessage());
            body.put("bdCodigo", error.getCode());
        }
        r.ok(body);
    }
}
