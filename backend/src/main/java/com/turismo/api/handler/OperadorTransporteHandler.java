package com.turismo.api.handler;

import com.turismo.api.dao.OperadorTransporteDAO;
import com.turismo.api.model.OperadorTransporte;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for OperadorTransporte endpoints
 *
 * GET/POST /api/operadorTransportes (?idOperador= | ?idTransporte=)
 * GET/DELETE /api/operadorTransportes/{idOperador}/{idTransporte}
 * GET /api/operadorTransportes/operador/{idOperador} · /transporte/{idTransporte}
 *
 * Não há PUT: as duas colunas fazem parte da chave primária.
 */
public class OperadorTransporteHandler extends BaseHandler {
    private final OperadorTransporteDAO operadorTransporteDAO;

    public OperadorTransporteHandler() {
        this(new OperadorTransporteDAO(), ConnectionProvider.DEFAULT);
    }

    public OperadorTransporteHandler(OperadorTransporteDAO dao, ConnectionProvider connections) {
        super("/api/operadorTransportes", connections);
        this.operadorTransporteDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idOperador = r.param("idOperador");
                String idTransporte = r.param("idTransporte");
                if (idOperador != null) {
                    r.ok(operadorTransporteDAO.getOperadorTransportesByOperador(idOperador));
                } else if (idTransporte != null) {
                    r.ok(operadorTransporteDAO.getOperadorTransportesByTransporte(idTransporte));
                } else {
                    r.ok(operadorTransporteDAO.getAllOperadorTransportes());
                }
            } else if (r.is("POST")) {
                OperadorTransporte o = r.body(OperadorTransporte.class);
                o.setIdOperador(clean(o.getIdOperador()));
                o.setIdTransporte(clean(o.getIdTransporte()));
                new Validator()
                        .required("idOperador", o.getIdOperador())
                        .required("idTransporte", o.getIdTransporte())
                        .throwIfInvalid();
                String msg = operadorTransporteDAO.addOperadorTransporte(o);
                r.created(message(msg, operadorTransporteDAO.getOperadorTransporte(o.getIdOperador(), o.getIdTransporte())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "operador".equals(r.segment(0))) {
            r.ok(operadorTransporteDAO.getOperadorTransportesByOperador(r.segment(1)));
            return;
        }
        if (r.size() == 2 && r.is("GET") && "transporte".equals(r.segment(0))) {
            r.ok(operadorTransporteDAO.getOperadorTransportesByTransporte(r.segment(1)));
            return;
        }
        if (r.size() != 2) {
            throw notFound("Endpoint");
        }
        String idOperador = r.segment(0);
        String idTransporte = r.segment(1);
        if (r.is("GET")) {
            OperadorTransporte o = operadorTransporteDAO.getOperadorTransporte(idOperador, idTransporte);
            if (o == null) {
                throw notFound("Associação");
            }
            r.ok(o);
        } else if (r.is("DELETE")) {
            if (!operadorTransporteDAO.deleteOperadorTransporte(idOperador, idTransporte)) {
                throw notFound("Associação");
            }
            r.ok(message("Associação removida."));
        } else {
            throw methodNotAllowed();
        }
    }
}
