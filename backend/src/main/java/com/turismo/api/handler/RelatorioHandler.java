package com.turismo.api.handler;

import com.turismo.api.dao.RelatorioDAO;
import com.turismo.api.util.ConnectionProvider;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /api/relatorios → lista das views disponíveis
 * GET /api/relatorios/{view} → colunas e linhas da view
 */
public class RelatorioHandler extends BaseHandler {
    private final RelatorioDAO relatorioDAO;

    public RelatorioHandler() {
        this(new RelatorioDAO(), ConnectionProvider.DEFAULT);
    }

    public RelatorioHandler(RelatorioDAO relatorioDAO, ConnectionProvider connections) {
        super("/api/relatorios", connections);
        this.relatorioDAO = relatorioDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (!r.is("GET")) {
            throw methodNotAllowed();
        }
        if (r.size() == 0) {
            r.ok(RelatorioDAO.VIEWS.keySet());
        } else if (r.size() == 1) {
            r.ok(relatorioDAO.read(r.segment(0)));
        } else {
            throw notFound("Endpoint");
        }
    }
}
