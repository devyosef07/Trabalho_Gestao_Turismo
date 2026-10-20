package com.turismo.api.handler;

import com.turismo.api.dao.TuristaDestinoDAO;
import com.turismo.api.model.TuristaDestino;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * HTTP handler for TuristaDestino endpoints
 *
 * GET/POST /api/turistaDestinos (?idTurista= | ?idDestino=)
 * GET/DELETE /api/turistaDestinos/{idDestino}/{idTurista}/{dataVisita}
 * GET /api/turistaDestinos/turista/{idTurista} · /destino/{idDestino}
 *
 * Não há PUT: as três colunas fazem parte da chave primária.
 */
public class TuristaDestinoHandler extends BaseHandler {
    private final TuristaDestinoDAO turistaDestinoDAO;

    public TuristaDestinoHandler() {
        this(new TuristaDestinoDAO(), ConnectionProvider.DEFAULT);
    }

    public TuristaDestinoHandler(TuristaDestinoDAO dao, ConnectionProvider connections) {
        super("/api/turistaDestinos", connections);
        this.turistaDestinoDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idTurista = r.param("idTurista");
                String idDestino = r.param("idDestino");
                if (idTurista != null) {
                    r.ok(turistaDestinoDAO.getTuristaDestinosByTurista(idTurista));
                } else if (idDestino != null) {
                    r.ok(turistaDestinoDAO.getTuristaDestinosByDestino(idDestino));
                } else {
                    r.ok(turistaDestinoDAO.getAllTuristaDestinos());
                }
            } else if (r.is("POST")) {
                TuristaDestino td = r.body(TuristaDestino.class);
                td.setIdDestino(clean(td.getIdDestino()));
                td.setIdTurista(clean(td.getIdTurista()));
                new Validator()
                        .required("idDestino", td.getIdDestino())
                        .required("idTurista", td.getIdTurista())
                        .required("dataVisita", td.getDataVisita())
                        .throwIfInvalid();
                String msg = turistaDestinoDAO.addTuristaDestino(td);
                r.created(message(msg + ".", turistaDestinoDAO.getTuristaDestino(
                        td.getIdDestino(), td.getIdTurista(), td.getDataVisita())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "turista".equals(r.segment(0))) {
            r.ok(turistaDestinoDAO.getTuristaDestinosByTurista(r.segment(1)));
            return;
        }
        if (r.size() == 2 && r.is("GET") && "destino".equals(r.segment(0))) {
            r.ok(turistaDestinoDAO.getTuristaDestinosByDestino(r.segment(1)));
            return;
        }
        if (r.size() != 3) {
            throw notFound("Endpoint");
        }
        String idDestino = r.segment(0);
        String idTurista = r.segment(1);
        LocalDate data = parseDate(r.segment(2), "dataVisita");
        if (r.is("GET")) {
            TuristaDestino td = turistaDestinoDAO.getTuristaDestino(idDestino, idTurista, data);
            if (td == null) {
                throw notFound("Visita");
            }
            r.ok(td);
        } else if (r.is("DELETE")) {
            if (!turistaDestinoDAO.deleteTuristaDestino(idDestino, idTurista, data)) {
                throw notFound("Visita");
            }
            r.ok(message("Visita removida."));
        } else {
            throw methodNotAllowed();
        }
    }
}
