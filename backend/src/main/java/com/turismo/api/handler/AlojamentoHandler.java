package com.turismo.api.handler;

import com.turismo.api.dao.AlojamentoDAO;
import com.turismo.api.model.Alojamento;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Alojamento endpoints
 *
 * GET/POST /api/alojamentos · GET/PUT/DELETE /api/alojamentos/{id}
 */
public class AlojamentoHandler extends BaseHandler {
    private final AlojamentoDAO alojamentoDAO;

    public AlojamentoHandler() {
        this(new AlojamentoDAO(), ConnectionProvider.DEFAULT);
    }

    public AlojamentoHandler(AlojamentoDAO alojamentoDAO, ConnectionProvider connections) {
        super("/api/alojamentos", connections);
        this.alojamentoDAO = alojamentoDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idDestino = r.param("idDestino");
                r.ok(idDestino == null ? alojamentoDAO.getAllAlojamentos() : alojamentoDAO.getAlojamentosByDestino(idDestino));
            } else if (r.is("POST")) {
                String id = alojamentoDAO.addAlojamento(read(r));
                r.created(message("Alojamento " + id + " criado.", alojamentoDAO.getAlojamentoById(id)));
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
            Alojamento a = alojamentoDAO.getAlojamentoById(id);
            if (a == null) {
                throw notFound("Alojamento");
            }
            r.ok(a);
        } else if (r.is("PUT")) {
            Alojamento a = read(r);
            a.setIdAlojamento(id);
            if (!alojamentoDAO.updateAlojamento(a)) {
                throw notFound("Alojamento");
            }
            r.ok(message("Alojamento " + id + " atualizado.", alojamentoDAO.getAlojamentoById(id)));
        } else if (r.is("DELETE")) {
            if (!alojamentoDAO.deleteAlojamento(id)) {
                throw notFound("Alojamento");
            }
            r.ok(message("Alojamento " + id + " eliminado."));
        } else {
            throw methodNotAllowed();
        }
    }

    private Alojamento read(Request r) throws IOException {
        Alojamento a = r.body(Alojamento.class);
        a.setNomeAlojamento(clean(a.getNomeAlojamento()));
        a.setTipoAlojamento(clean(a.getTipoAlojamento()));
        a.setIdDestino(clean(a.getIdDestino()));
        new Validator()
                .text("nomeAlojamento", a.getNomeAlojamento(), 50, true)
                .required("tipoAlojamento", a.getTipoAlojamento())
                .rating("classAlojamento", a.getClassAlojamento())
                .required("idDestino", a.getIdDestino())
                .throwIfInvalid();
        return a;
    }
}
