package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.DbErrors;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dados do painel inicial, obtidos com uma só ligação.
 *
 * Cada secção é lida de forma independente: se uma view não existir, só essa
 * secção devolve erro e as restantes continuam a aparecer.
 */
public class DashboardDAO extends BaseDAO {

    /** Contagens por tabela: não existe nenhuma view com estes totais. */
    private static final String COUNTS =
            "SELECT 'destinos', COUNT(*) FROM DESTINO UNION ALL "
                    + "SELECT 'atracoes', COUNT(*) FROM ATRACAO UNION ALL "
                    + "SELECT 'alojamentos', COUNT(*) FROM ALOJAMENTO UNION ALL "
                    + "SELECT 'tiposAlojamento', COUNT(*) FROM TIPO_ALOJAMENTO UNION ALL "
                    + "SELECT 'transportes', COUNT(*) FROM TRANSPORTE UNION ALL "
                    + "SELECT 'turistas', COUNT(*) FROM TURISTA UNION ALL "
                    + "SELECT 'preferencias', COUNT(*) FROM TURISTA_PREFERENCIA UNION ALL "
                    + "SELECT 'operadores', COUNT(*) FROM OPERADOR UNION ALL "
                    + "SELECT 'reservas', COUNT(*) FROM RESERVA UNION ALL "
                    + "SELECT 'pagamentos', COUNT(*) FROM PAGAMENTO UNION ALL "
                    + "SELECT 'visitas', COUNT(*) FROM TURISTA_DESTINO UNION ALL "
                    + "SELECT 'reservaAlojamentos', COUNT(*) FROM RESERVA_ALOJAMENTO UNION ALL "
                    + "SELECT 'operadorTuristas', COUNT(*) FROM OPERADOR_TURISTA UNION ALL "
                    + "SELECT 'operadorTransportes', COUNT(*) FROM OPERADOR_TRANSPORTE UNION ALL "
                    + "SELECT 'operadorAlojamentos', COUNT(*) FROM OPERADOR_ALOJAMENTO";

    /** Views usadas no painel. */
    static final List<String> DASHBOARD_VIEWS = List.of(
            "v_receita_mensal",
            "v_receita_nacionalidade",
            "v_reservas_ativas",
            "v_total_atracoes_destino",
            "v_total_alojamentos_destino",
            "v_avaliacao_operador");

    /** Views de verificação: devem estar vazias quando os dados estão corretos. */
    static final List<String> AUDIT_VIEWS = List.of(
            "v_aud_pagamento_data_futura",
            "v_aud_recibo_sem_concluido",
            "v_reservas_sem_alojamento",
            "v_reservas_sem_pagamento");

    private final RelatorioDAO relatorios;

    public DashboardDAO() {
        this(ConnectionProvider.DEFAULT);
    }

    public DashboardDAO(ConnectionProvider connections) {
        super(connections);
        this.relatorios = new RelatorioDAO(connections);
    }

    public Map<String, Object> load() throws SQLException {
        try (Connection c = connections.get()) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Object[] row : query(c, COUNTS, rs -> new Object[]{rs.getString(1), rs.getInt(2)})) {
                counts.put((String) row[0], (Integer) row[1]);
            }
            result.put("totais", counts);

            Map<String, Object> views = new LinkedHashMap<>();
            for (String view : DASHBOARD_VIEWS) {
                views.put(view, section(c, view));
            }
            result.put("views", views);

            Map<String, Object> audit = new LinkedHashMap<>();
            for (String view : AUDIT_VIEWS) {
                audit.put(view, section(c, view));
            }
            result.put("verificacoes", audit);
            return result;
        }
    }

    private Map<String, Object> section(Connection c, String view) {
        Map<String, Object> s = new LinkedHashMap<>();
        try {
            s.put("ok", true);
            s.put("data", relatorios.read(c, view));
        } catch (SQLException e) {
            ApiException error = DbErrors.translate(e);
            s.put("ok", false);
            s.put("code", error.getCode());
            s.put("error", e.getErrorCode() == 942
                    ? "A view " + view + " não existe na base de dados. Aplique database/correcoes.sql."
                    : error.getMessage());
        }
        return s;
    }
}
