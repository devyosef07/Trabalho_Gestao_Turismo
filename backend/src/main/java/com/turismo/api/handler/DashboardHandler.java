package com.turismo.api.handler;

import com.turismo.api.dao.DashboardDAO;
import com.turismo.api.util.ConnectionProvider;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /api/dashboard: views do painel e views de verificação.
 */
public class DashboardHandler extends BaseHandler {
    private final DashboardDAO dashboardDAO;

    public DashboardHandler() {
        this(new DashboardDAO(), ConnectionProvider.DEFAULT);
    }

    public DashboardHandler(DashboardDAO dashboardDAO, ConnectionProvider connections) {
        super("/api/dashboard", connections);
        this.dashboardDAO = dashboardDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() != 0) {
            throw notFound("Endpoint");
        }
        if (!r.is("GET")) {
            throw methodNotAllowed();
        }
        r.ok(dashboardDAO.load());
    }
}
