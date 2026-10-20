package com.turismo.api.handler;

import com.turismo.api.dao.TransporteDAO;
import com.turismo.api.model.Transporte;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Transporte endpoints
 *
 * GET/POST /api/transportes · GET/PUT/DELETE /api/transportes/{id}
 */
public class TransporteHandler extends BaseHandler {
    private final TransporteDAO transporteDAO;

    public TransporteHandler() {
        this(new TransporteDAO(), ConnectionProvider.DEFAULT);
    }

    public TransporteHandler(TransporteDAO transporteDAO, ConnectionProvider connections) {
        super("/api/transportes", connections);
        this.transporteDAO = transporteDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                r.ok(transporteDAO.getAllTransportes());
            } else if (r.is("POST")) {
                String id = transporteDAO.addTransporte(read(r));
                r.created(message("Transporte " + id + " criado.", transporteDAO.getTransporteById(id)));
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
            Transporte t = transporteDAO.getTransporteById(id);
            if (t == null) {
                throw notFound("Transporte");
            }
            r.ok(t);
        } else if (r.is("PUT")) {
            Transporte t = read(r);
            t.setIdTransporte(id);
            if (!transporteDAO.updateTransporte(t)) {
                throw notFound("Transporte");
            }
            r.ok(message("Transporte " + id + " atualizado.", transporteDAO.getTransporteById(id)));
        } else if (r.is("DELETE")) {
            if (!transporteDAO.deleteTransporte(id)) {
                throw notFound("Transporte");
            }
            r.ok(message("Transporte " + id + " eliminado."));
        } else {
            throw methodNotAllowed();
        }
    }

    private Transporte read(Request r) throws IOException, SQLException {
        Transporte t = r.body(Transporte.class);
        t.setTipoTransporte(clean(t.getTipoTransporte()));
        t.setDisponibilidade(clean(t.getDisponibilidade()));
        new Validator()
                .text("tipoTransporte", t.getTipoTransporte(), 15, true)
                .required("capTransporte", t.getCapTransporte())
                .positive("capTransporte", t.getCapTransporte(), 9)
                .required("disponibilidade", t.getDisponibilidade())
                .oneOf("disponibilidade", t.getDisponibilidade(), domain("TRANSPORTE", "DISPONIBILIDADE"))
                .throwIfInvalid();
        return t;
    }
}
