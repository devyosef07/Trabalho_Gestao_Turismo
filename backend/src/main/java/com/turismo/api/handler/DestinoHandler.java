package com.turismo.api.handler;

import com.turismo.api.dao.DestinoDAO;
import com.turismo.api.model.Destino;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Destino endpoints
 *
 * GET/POST /api/destinos · GET/PUT/DELETE /api/destinos/{id}
 * GET /api/destinos/{id}/dependencias · GET /api/destinos/{id}/info
 */
public class DestinoHandler extends BaseHandler {
    private final DestinoDAO destinoDAO;

    public DestinoHandler() {
        this(new DestinoDAO(), ConnectionProvider.DEFAULT);
    }

    public DestinoHandler(DestinoDAO destinoDAO, ConnectionProvider connections) {
        super("/api/destinos", connections);
        this.destinoDAO = destinoDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                r.ok(destinoDAO.getAllDestinos());
            } else if (r.is("POST")) {
                Destino d = read(r);
                String id = destinoDAO.addDestino(d);
                r.created(message("Destino " + id + " criado.", destinoDAO.getDestinoById(id)));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        String id = r.segment(0);
        if (r.size() == 1) {
            if (r.is("GET")) {
                Destino d = destinoDAO.getDestinoById(id);
                if (d == null) {
                    throw notFound("Destino");
                }
                r.ok(d);
            } else if (r.is("PUT")) {
                Destino d = read(r);
                d.setIdDestino(id);
                if (!destinoDAO.updateDestino(d)) {
                    throw notFound("Destino");
                }
                r.ok(message("Destino " + id + " atualizado.", destinoDAO.getDestinoById(id)));
            } else if (r.is("DELETE")) {
                if (!destinoDAO.deleteDestino(id)) {
                    throw notFound("Destino");
                }
                r.ok(message("Destino " + id + " eliminado."));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "dependencias".equals(r.segment(1))) {
            r.ok(destinoDAO.getDependencias(id));
        } else if (r.size() == 2 && r.is("GET") && "info".equals(r.segment(1))) {
            r.ok(destinoDAO.getInfo(id));
        } else {
            throw notFound("Endpoint");
        }
    }

    private Destino read(Request r) throws IOException, SQLException {
        Destino d = r.body(Destino.class);
        d.setNomeDestino(clean(d.getNomeDestino()));
        d.setRegiao(clean(d.getRegiao()));
        new Validator()
                .text("nomeDestino", d.getNomeDestino(), 80, true)
                .required("regiao", d.getRegiao())
                .oneOf("regiao", d.getRegiao(), domain("DESTINO", "REGIAO"))
                .required("capacidadeVisitantes", d.getCapacidadeVisitantes())
                .positive("capacidadeVisitantes", d.getCapacidadeVisitantes(), 10)
                .throwIfInvalid();
        return d;
    }
}
