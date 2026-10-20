package com.turismo.api.handler;

import com.turismo.api.dao.ReservaDAO;
import com.turismo.api.model.Reserva;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * HTTP handler for Reserva endpoints
 *
 * GET/POST /api/reservas · GET/PUT/DELETE /api/reservas/{id}
 * POST /api/reservas/{id}/cancelar · GET /api/reservas/turista/{idTurista}
 */
public class ReservaHandler extends BaseHandler {
    private final ReservaDAO reservaDAO;

    /** Corpo do POST: a reserva e, opcionalmente, os alojamentos a associar. */
    static class NovaReserva extends Reserva {
        List<String> alojamentos;
    }

    public ReservaHandler() {
        this(new ReservaDAO(), ConnectionProvider.DEFAULT);
    }

    public ReservaHandler(ReservaDAO reservaDAO, ConnectionProvider connections) {
        super("/api/reservas", connections);
        this.reservaDAO = reservaDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idTurista = r.param("idTurista");
                r.ok(idTurista == null ? reservaDAO.getAllReservas() : reservaDAO.getReservasByTurista(idTurista));
            } else if (r.is("POST")) {
                NovaReserva nova = r.body(NovaReserva.class);
                if (nova.getStatusReserva() == null || nova.getStatusReserva().isBlank()) {
                    nova.setStatusReserva("PENDENTE");
                }
                validate(nova, null);
                List<String> alojamentos = new ArrayList<>(new LinkedHashSet<>(
                        nova.alojamentos == null ? List.of() : nova.alojamentos));
                alojamentos.removeIf(a -> a == null || a.isBlank());
                String id = reservaDAO.addReserva(nova, alojamentos);
                r.created(message("Reserva " + id + " criada.", reservaDAO.getReservaById(id)));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && "turista".equals(r.segment(0)) && r.is("GET")) {
            r.ok(reservaDAO.getReservasByTurista(r.segment(1)));
            return;
        }
        String id = r.segment(0);
        if (r.size() == 2 && "cancelar".equals(r.segment(1))) {
            if (!r.is("POST")) {
                throw methodNotAllowed();
            }
            String msg = reservaDAO.cancelarReserva(id);
            r.ok(message(msg, reservaDAO.getReservaById(id)));
            return;
        }
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        if (r.is("GET")) {
            Reserva reserva = reservaDAO.getReservaById(id);
            if (reserva == null) {
                throw notFound("Reserva");
            }
            r.ok(reserva);
        } else if (r.is("PUT")) {
            Reserva atual = reservaDAO.getReservaById(id);
            if (atual == null) {
                throw notFound("Reserva");
            }
            Reserva reserva = r.body(Reserva.class);
            reserva.setIdReserva(id);
            validate(reserva, atual);
            if (!reservaDAO.updateReserva(reserva)) {
                throw notFound("Reserva");
            }
            r.ok(message("Reserva " + id + " atualizada.", reservaDAO.getReservaById(id)));
        } else if (r.is("DELETE")) {
            if (!reservaDAO.deleteReserva(id)) {
                throw notFound("Reserva");
            }
            r.ok(message("Reserva " + id + " eliminada."));
        } else {
            throw methodNotAllowed();
        }
    }

    /**
     * Regras de tabelas.sql e dos triggers validardatachegada / validardatapartida:
     * a chegada não pode ser no passado (ao criar ou ao mudar a data) e a
     * partida tem de ser depois da chegada.
     */
    private void validate(Reserva reserva, Reserva atual) throws SQLException {
        reserva.setIdTurista(clean(reserva.getIdTurista()));
        reserva.setStatusReserva(clean(reserva.getStatusReserva()));
        boolean chegadaMudou = atual == null || reserva.getDataChegada() == null
                || !reserva.getDataChegada().equals(atual.getDataChegada());
        Validator v = new Validator()
                .required("idTurista", reserva.getIdTurista())
                .required("dataChegada", reserva.getDataChegada())
                .required("dataPartida", reserva.getDataPartida())
                .after("dataPartida", reserva.getDataPartida(), reserva.getDataChegada(),
                        "Tem de ser depois da data de chegada.")
                .required("statusReserva", reserva.getStatusReserva())
                .oneOf("statusReserva", reserva.getStatusReserva(), domain("RESERVA", "STATUS_RESERVA"));
        if (chegadaMudou && reserva.getDataChegada() != null) {
            v.check(!reserva.getDataChegada().isBefore(LocalDate.now()), "dataChegada",
                    "Não pode ser anterior a hoje.");
        }
        v.throwIfInvalid();
    }
}
