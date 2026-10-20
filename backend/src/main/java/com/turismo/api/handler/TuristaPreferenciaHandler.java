package com.turismo.api.handler;

import com.turismo.api.dao.TuristaPreferenciaDAO;
import com.turismo.api.model.TuristaPreferencia;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for TuristaPreferencia endpoints
 *
 * GET/POST /api/turistaPreferencias (?idTurista=)
 * GET/PUT/DELETE /api/turistaPreferencias/{idTurista}/{preferencia}
 * GET /api/turistaPreferencias/turista/{idTurista}
 */
public class TuristaPreferenciaHandler extends BaseHandler {
    private final TuristaPreferenciaDAO turistaPreferenciaDAO;

    public TuristaPreferenciaHandler() {
        this(new TuristaPreferenciaDAO(), ConnectionProvider.DEFAULT);
    }

    public TuristaPreferenciaHandler(TuristaPreferenciaDAO dao, ConnectionProvider connections) {
        super("/api/turistaPreferencias", connections);
        this.turistaPreferenciaDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idTurista = r.param("idTurista");
                r.ok(idTurista == null ? turistaPreferenciaDAO.getAllTuristaPreferencias()
                        : turistaPreferenciaDAO.getTuristaPreferenciasByTurista(idTurista));
            } else if (r.is("POST")) {
                TuristaPreferencia tp = r.body(TuristaPreferencia.class);
                tp.setIdTurista(clean(tp.getIdTurista()));
                tp.setPreferencia(clean(tp.getPreferencia()));
                validate(new Validator().required("idTurista", tp.getIdTurista()), "preferencia", tp.getPreferencia());
                String msg = turistaPreferenciaDAO.addTuristaPreferencia(tp);
                r.created(message(msg, turistaPreferenciaDAO.getTuristaPreferencia(tp.getIdTurista(), tp.getPreferencia())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && "turista".equals(r.segment(0)) && r.is("GET")) {
            r.ok(turistaPreferenciaDAO.getTuristaPreferenciasByTurista(r.segment(1)));
            return;
        }
        if (r.size() != 2) {
            throw notFound("Endpoint");
        }
        String idTurista = r.segment(0);
        String preferencia = r.segment(1);
        if (r.is("GET")) {
            TuristaPreferencia tp = turistaPreferenciaDAO.getTuristaPreferencia(idTurista, preferencia);
            if (tp == null) {
                throw notFound("Preferência");
            }
            r.ok(tp);
        } else if (r.is("PUT")) {
            TuristaPreferencia nova = r.body(TuristaPreferencia.class);
            String novaPreferencia = clean(nova.getPreferencia());
            validate(new Validator(), "preferencia", novaPreferencia);
            if (!turistaPreferenciaDAO.updateTuristaPreferencia(idTurista, preferencia, novaPreferencia)) {
                throw notFound("Preferência");
            }
            r.ok(message("Preferência atualizada.", turistaPreferenciaDAO.getTuristaPreferencia(idTurista, novaPreferencia)));
        } else if (r.is("DELETE")) {
            if (!turistaPreferenciaDAO.deleteTuristaPreferencia(idTurista, preferencia)) {
                throw notFound("Preferência");
            }
            r.ok(message("Preferência removida."));
        } else {
            throw methodNotAllowed();
        }
    }

    private void validate(Validator v, String field, String preferencia) throws SQLException {
        v.required(field, preferencia)
                .oneOf(field, preferencia, domain("TURISTA_PREFERENCIA", "PREFERENCIA"))
                .throwIfInvalid();
    }
}
