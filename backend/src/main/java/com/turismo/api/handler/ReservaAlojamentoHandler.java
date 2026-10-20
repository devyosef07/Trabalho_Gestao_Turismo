package com.turismo.api.handler;

import com.turismo.api.dao.ReservaAlojamentoDAO;
import com.turismo.api.model.ReservaAlojamento;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for ReservaAlojamento endpoints
 *
 * GET/POST /api/reservaAlojamentos (?idReserva= | ?idAlojamento=)
 * GET/PUT/DELETE /api/reservaAlojamentos/{idReserva}/{idAlojamento} (PUT = avaliação)
 * GET /api/reservaAlojamentos/reserva/{idReserva} · /alojamento/{idAlojamento}
 */
public class ReservaAlojamentoHandler extends BaseHandler {
    private final ReservaAlojamentoDAO reservaAlojamentoDAO;

    public ReservaAlojamentoHandler() {
        this(new ReservaAlojamentoDAO(), ConnectionProvider.DEFAULT);
    }

    public ReservaAlojamentoHandler(ReservaAlojamentoDAO dao, ConnectionProvider connections) {
        super("/api/reservaAlojamentos", connections);
        this.reservaAlojamentoDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idReserva = r.param("idReserva");
                String idAlojamento = r.param("idAlojamento");
                if (idReserva != null) {
                    r.ok(reservaAlojamentoDAO.getReservaAlojamentosByReserva(idReserva));
                } else if (idAlojamento != null) {
                    r.ok(reservaAlojamentoDAO.getReservaAlojamentosByAlojamento(idAlojamento));
                } else {
                    r.ok(reservaAlojamentoDAO.getAllReservaAlojamentos());
                }
            } else if (r.is("POST")) {
                ReservaAlojamento ra = r.body(ReservaAlojamento.class);
                ra.setIdReserva(clean(ra.getIdReserva()));
                ra.setIdAlojamento(clean(ra.getIdAlojamento()));
                new Validator()
                        .required("idReserva", ra.getIdReserva())
                        .required("idAlojamento", ra.getIdAlojamento())
                        .throwIfInvalid();
                String msg = reservaAlojamentoDAO.addReservaAlojamento(ra);
                r.created(message(msg, reservaAlojamentoDAO.getReservaAlojamento(ra.getIdReserva(), ra.getIdAlojamento())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "reserva".equals(r.segment(0))) {
            r.ok(reservaAlojamentoDAO.getReservaAlojamentosByReserva(r.segment(1)));
            return;
        }
        if (r.size() == 2 && r.is("GET") && "alojamento".equals(r.segment(0))) {
            r.ok(reservaAlojamentoDAO.getReservaAlojamentosByAlojamento(r.segment(1)));
            return;
        }
        if (r.size() != 2) {
            throw notFound("Endpoint");
        }
        String idReserva = r.segment(0);
        String idAlojamento = r.segment(1);
        if (r.is("GET")) {
            ReservaAlojamento ra = reservaAlojamentoDAO.getReservaAlojamento(idReserva, idAlojamento);
            if (ra == null) {
                throw notFound("Associação");
            }
            r.ok(ra);
        } else if (r.is("PUT")) {
            if (reservaAlojamentoDAO.getReservaAlojamento(idReserva, idAlojamento) == null) {
                throw notFound("Associação");
            }
            ReservaAlojamento ra = r.body(ReservaAlojamento.class);
            new Validator().rating("avaliacaoAlojamento", ra.getAvaliacaoAlojamento()).throwIfInvalid();
            String msg = reservaAlojamentoDAO.avaliar(idReserva, idAlojamento, ra.getAvaliacaoAlojamento());
            r.ok(message(msg, reservaAlojamentoDAO.getReservaAlojamento(idReserva, idAlojamento)));
        } else if (r.is("DELETE")) {
            if (!reservaAlojamentoDAO.deleteReservaAlojamento(idReserva, idAlojamento)) {
                throw notFound("Associação");
            }
            r.ok(message("Alojamento removido da reserva."));
        } else {
            throw methodNotAllowed();
        }
    }
}
