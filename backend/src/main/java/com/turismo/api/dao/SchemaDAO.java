package com.turismo.api.dao;

import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado da base de dados: ligação, objetos inválidos e views em falta.
 * Permite à interface explicar porque é que uma operação não está disponível.
 */
public class SchemaDAO extends BaseDAO {

    public SchemaDAO() {
    }

    public SchemaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public Map<String, Object> status() throws SQLException {
        try (Connection c = connections.get()) {
            Map<String, Object> result = new LinkedHashMap<>();
            String version = c.getMetaData().getDatabaseProductVersion();
            result.put("database", version == null ? null : version.split("\n")[0]);

            List<Map<String, String>> invalid = query(c,
                    "SELECT object_type, object_name FROM user_objects WHERE status <> 'VALID' ORDER BY object_type, object_name",
                    rs -> {
                        Map<String, String> o = new LinkedHashMap<>();
                        o.put("tipo", rs.getString(1));
                        o.put("nome", rs.getString(2));
                        return o;
                    });
            result.put("objetosInvalidos", invalid);

            List<String> present = query(c, "SELECT LOWER(view_name) FROM user_views", rs -> rs.getString(1));
            List<String> missing = new ArrayList<>();
            for (String view : RelatorioDAO.VIEWS.keySet()) {
                if (!present.contains(view)) {
                    missing.add(view);
                }
            }
            result.put("viewsEsperadas", RelatorioDAO.VIEWS.size());
            result.put("viewsEmFalta", missing);
            return result;
        }
    }
}
