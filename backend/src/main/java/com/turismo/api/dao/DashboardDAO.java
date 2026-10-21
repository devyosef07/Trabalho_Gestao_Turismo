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
 * Tudo o que o painel mostra vem das views de database/views.sql (nunca
 * diretamente das tabelas); os indicadores do topo são calculados pela
 * interface a partir das linhas destas views.
 *
 * Cada secção é lida de forma independente: se uma view não existir, só essa
 * secção devolve erro e as restantes continuam a aparecer.
 */
public class DashboardDAO extends BaseDAO {

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
                    ? "A view " + view + " não existe na base de dados. Execute database/views.sql "
                    + "(precisa do privilégio CREATE VIEW: database/conceder_privilegios.sql)."
                    : error.getMessage());
        }
        return s;
    }
}
