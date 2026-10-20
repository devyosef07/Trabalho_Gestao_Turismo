package com.turismo.api.handler;

import com.turismo.api.dao.OperadorTuristaDAO;
import com.turismo.api.model.OperadorTurista;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for OperadorTurista endpoints
 *
 * GET/POST /api/operadorTuristas (?idOperador= | ?idTurista=)
 * GET/PUT/DELETE /api/operadorTuristas/{idOperador}/{idTurista} (PUT = avaliação)
 * GET /api/operadorTuristas/operador/{idOperador} · /turista/{idTurista}
 */
public class OperadorTuristaHandler extends BaseHandler {
    private final OperadorTuristaDAO operadorTuristaDAO;

    public OperadorTuristaHandler() {
        this(new OperadorTuristaDAO(), ConnectionProvider.DEFAULT);
    }

    public OperadorTuristaHandler(OperadorTuristaDAO dao, ConnectionProvider connections) {
        super("/api/operadorTuristas", connections);
        this.operadorTuristaDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idOperador = r.param("idOperador");
                String idTurista = r.param("idTurista");
                if (idOperador != null) {
                    r.ok(operadorTuristaDAO.getOperadorTuristasByOperador(idOperador));
                } else if (idTurista != null) {
                    r.ok(operadorTuristaDAO.getOperadorTuristasByTurista(idTurista));
                } else {
                    r.ok(operadorTuristaDAO.getAllOperadorTuristas());
                }
            } else if (r.is("POST")) {
                OperadorTurista ot = r.body(OperadorTurista.class);
                ot.setIdOperador(clean(ot.getIdOperador()));
                ot.setIdTurista(clean(ot.getIdTurista()));
                new Validator()
                        .required("idOperador", ot.getIdOperador())
                        .required("idTurista", ot.getIdTurista())
                        .throwIfInvalid();
                String msg = operadorTuristaDAO.addOperadorTurista(ot);
                r.created(message(msg, operadorTuristaDAO.getOperadorTurista(ot.getIdOperador(), ot.getIdTurista())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "operador".equals(r.segment(0))) {
            r.ok(operadorTuristaDAO.getOperadorTuristasByOperador(r.segment(1)));
            return;
        }
        if (r.size() == 2 && r.is("GET") && "turista".equals(r.segment(0))) {
            r.ok(operadorTuristaDAO.getOperadorTuristasByTurista(r.segment(1)));
            return;
        }
        if (r.size() != 2) {
            throw notFound("Endpoint");
        }
        String idOperador = r.segment(0);
        String idTurista = r.segment(1);
        if (r.is("GET")) {
            OperadorTurista ot = operadorTuristaDAO.getOperadorTurista(idOperador, idTurista);
            if (ot == null) {
                throw notFound("Associação");
            }
            r.ok(ot);
        } else if (r.is("PUT")) {
            if (operadorTuristaDAO.getOperadorTurista(idOperador, idTurista) == null) {
                throw notFound("Associação");
            }
            OperadorTurista ot = r.body(OperadorTurista.class);
            new Validator().rating("avaliacaoOperador", ot.getAvaliacaoOperador()).throwIfInvalid();
            String msg = operadorTuristaDAO.avaliar(idOperador, idTurista, ot.getAvaliacaoOperador());
            r.ok(message(msg, operadorTuristaDAO.getOperadorTurista(idOperador, idTurista)));
        } else if (r.is("DELETE")) {
            if (!operadorTuristaDAO.deleteOperadorTurista(idOperador, idTurista)) {
                throw notFound("Associação");
            }
            r.ok(message("Associação removida."));
        } else {
            throw methodNotAllowed();
        }
    }
}
