package com.turismo.api.handler;

import com.turismo.api.dao.TuristaDAO;
import com.turismo.api.model.Turista;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Turista endpoints
 *
 * GET/POST /api/turistas · GET/PUT/DELETE /api/turistas/{id}
 */
public class TuristaHandler extends BaseHandler {
    private final TuristaDAO turistaDAO;

    public TuristaHandler() {
        this(new TuristaDAO(), ConnectionProvider.DEFAULT);
    }

    public TuristaHandler(TuristaDAO turistaDAO, ConnectionProvider connections) {
        super("/api/turistas", connections);
        this.turistaDAO = turistaDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                r.ok(turistaDAO.getAllTuristas());
            } else if (r.is("POST")) {
                String id = turistaDAO.addTurista(read(r));
                r.created(message("Turista " + id + " criado.", turistaDAO.getTuristaById(id)));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        String id = r.segment(0);
        if (r.is("GET")) {
            Turista turista = turistaDAO.getTuristaById(id);
            if (turista == null) {
                throw notFound("Turista");
            }
            r.ok(turista);
        } else if (r.is("PUT")) {
            Turista turista = read(r);
            turista.setIdTurista(id);
            if (!turistaDAO.updateTurista(turista)) {
                throw notFound("Turista");
            }
            r.ok(message("Turista " + id + " atualizado.", turistaDAO.getTuristaById(id)));
        } else if (r.is("DELETE")) {
            r.ok(message(turistaDAO.deleteTurista(id) + "."));
        } else {
            throw methodNotAllowed();
        }
    }

    private Turista read(Request r) throws IOException {
        Turista t = r.body(Turista.class);
        t.setNomeTurista(clean(t.getNomeTurista()));
        t.setNacionalidade(clean(t.getNacionalidade()));
        new Validator()
                .text("nomeTurista", t.getNomeTurista(), 80, true)
                .text("nacionalidade", t.getNacionalidade(), 50, true)
                .throwIfInvalid();
        return t;
    }
}
