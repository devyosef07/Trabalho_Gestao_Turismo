package com.turismo.api.handler;

import com.turismo.api.util.ConstraintCatalog;
import com.turismo.api.util.ConnectionProvider;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /api/dominios: valores permitidos pelas constraints CHECK "coluna in (...)"
 * (regiões, tipos de atração, estados...), com chave "TABELA.COLUNA".
 */
public class DominioHandler extends BaseHandler {

    public DominioHandler() {
        this(ConnectionProvider.DEFAULT);
    }

    public DominioHandler(ConnectionProvider connections) {
        super("/api/dominios", connections);
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (!r.is("GET")) {
            throw methodNotAllowed();
        }
        if (r.size() != 0) {
            throw notFound("Endpoint");
        }
        r.ok(ConstraintCatalog.domains(connections));
    }
}
