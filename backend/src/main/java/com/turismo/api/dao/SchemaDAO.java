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

    /** Views de listagem (views 25 a 39 de database/views.sql), uma por tabela. */
    public static final List<String> LIST_VIEWS = List.of(
            "v_lista_destinos", "v_lista_atracoes", "v_lista_alojamentos", "v_lista_tipos_alojamento",
            "v_lista_transportes", "v_lista_turistas", "v_lista_preferencias", "v_lista_operadores",
            "v_lista_reservas", "v_lista_pagamentos", "v_lista_visitas", "v_lista_reserva_alojamentos",
            "v_lista_operador_turistas", "v_lista_operador_transportes", "v_lista_operador_alojamentos");

    /** As 24 views de relatório seguidas das 15 de listagem. */
    public static List<String> expectedViews() {
        List<String> all = new ArrayList<>(RelatorioDAO.VIEWS.keySet());
        all.addAll(LIST_VIEWS);
        return all;
    }

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
            List<String> expected = expectedViews();
            for (String view : expected) {
                if (!present.contains(view)) {
                    missing.add(view);
                }
            }
            result.put("viewsEsperadas", expected.size());
            result.put("viewsEmFalta", missing);
            return result;
        }
    }
}
