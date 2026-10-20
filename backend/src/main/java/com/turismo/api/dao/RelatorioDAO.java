package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitura das 24 views definidas em database/views.sql.
 *
 * Só são aceites os nomes desta lista (nunca texto vindo do pedido), e cada
 * view tem a ordenação que faz sentido para a apresentar.
 */
public class RelatorioDAO extends BaseDAO {

    /** Nome da view → cláusula ORDER BY. */
    public static final Map<String, String> VIEWS;

    static {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("v_atracoes_destino", "nome_destino, ID_atracao");
        v.put("v_total_atracoes_destino", "total_atracoes DESC, nome_destino");
        v.put("v_total_alojamentos_destino", "total_alojamentos DESC, nome_destino");
        v.put("v_alojamentos_detalhe", "ID_alojamento");
        v.put("v_media_avaliacao_alojamento", "media_avaliacoes DESC NULLS LAST, nome_alojamento");
        v.put("v_reservas_detalhe", "data_chegada DESC");
        v.put("v_reservas_ativas", "data_chegada");
        v.put("v_reservas_alojamento", "ID_reserva");
        v.put("v_total_pago_reserva", "ID_reserva");
        v.put("v_recibos", "data_pagamento DESC");
        v.put("v_receita_mensal", "mes_ordem");
        v.put("v_receita_nacionalidade", "receita DESC");
        v.put("v_turista_destinos", "data_visita DESC");
        v.put("v_turista_preferencias", "nome_turista, preferencia");
        v.put("v_avaliacao_operador", "media_avaliacao DESC NULLS LAST, nome_operador");
        v.put("v_operadores_alojamentos", "nome_operador, nome_alojamento");
        v.put("v_operadores_transportes", "nome_operador, tipo_transporte");
        v.put("v_transportes_disponiveis", "ID_transporte");
        v.put("v_aud_pagamento_data_futura", "data_pagamento");
        v.put("v_aud_recibo_sem_concluido", "ID_pagamento");
        v.put("v_reservas_sem_alojamento", "data_chegada");
        v.put("v_reservas_sem_pagamento", "data_chegada");
        v.put("v_alojamentos_acima_media", "class_alojamento DESC");
        v.put("v_turistas_sem_reserva", "ID_turista");
        VIEWS = Collections.unmodifiableMap(v);
    }

    /** Limite de linhas por pedido, para nunca transferir tabelas enormes de uma vez. */
    static final int MAX_ROWS = 1000;

    public RelatorioDAO() {
    }

    public RelatorioDAO(ConnectionProvider connections) {
        super(connections);
    }

    public Map<String, Object> read(String viewName) throws SQLException {
        try (Connection c = connections.get()) {
            return read(c, viewName);
        }
    }

    public Map<String, Object> read(Connection c, String viewName) throws SQLException {
        String key = viewName == null ? null : viewName.toLowerCase();
        String order = key == null ? null : VIEWS.get(key);
        if (order == null) {
            throw ApiException.notFound("Relatório desconhecido: " + viewName);
        }
        String sql = "SELECT * FROM (SELECT * FROM " + key + " ORDER BY " + order + ") WHERE ROWNUM <= " + (MAX_ROWS + 1);
        List<Map<String, Object>> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        try (var ps = prepare(c, sql); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", md.getColumnLabel(i).toLowerCase());
                col.put("type", typeOf(md.getColumnType(i)));
                columns.add(col);
            }
            while (rs.next()) {
                List<Object> row = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    row.add(value(rs, i, md.getColumnType(i)));
                }
                rows.add(row);
            }
        }
        boolean truncated = rows.size() > MAX_ROWS;
        if (truncated) {
            rows = rows.subList(0, MAX_ROWS);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("view", key);
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("truncated", truncated);
        return result;
    }

    static String typeOf(int sqlType) {
        switch (sqlType) {
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.INTEGER:
            case Types.DOUBLE:
            case Types.FLOAT:
                return "number";
            case Types.DATE:
            case Types.TIMESTAMP:
                return "date";
            default:
                return "text";
        }
    }

    static Object value(ResultSet rs, int i, int sqlType) throws SQLException {
        switch (typeOf(sqlType)) {
            case "number":
                BigDecimal n = rs.getBigDecimal(i);
                return n == null ? null : n.stripTrailingZeros();
            case "date":
                Timestamp ts = rs.getTimestamp(i);
                return ts == null ? null : ts.toLocalDateTime().toLocalDate().toString();
            default:
                String s = rs.getString(i);
                return s == null ? null : s.trim();
        }
    }
}
