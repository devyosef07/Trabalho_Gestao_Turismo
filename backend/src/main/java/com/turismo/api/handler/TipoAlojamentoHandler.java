package com.turismo.api.handler;

import com.turismo.api.dao.TipoAlojamentoDAO;
import com.turismo.api.model.TipoAlojamento;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for TipoAlojamento endpoints
 *
 * GET/POST /api/tipoAlojamentos · GET/PUT/DELETE /api/tipoAlojamentos/{tipo}
 */
public class TipoAlojamentoHandler extends BaseHandler {
    private final TipoAlojamentoDAO tipoAlojamentoDAO;

    public TipoAlojamentoHandler() {
        this(new TipoAlojamentoDAO(), ConnectionProvider.DEFAULT);
    }

    public TipoAlojamentoHandler(TipoAlojamentoDAO tipoAlojamentoDAO, ConnectionProvider connections) {
        super("/api/tipoAlojamentos", connections);
        this.tipoAlojamentoDAO = tipoAlojamentoDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                r.ok(tipoAlojamentoDAO.getAllTipoAlojamentos());
            } else if (r.is("POST")) {
                TipoAlojamento t = read(r, true);
                String msg = tipoAlojamentoDAO.addTipoAlojamento(t);
                r.created(message(msg, tipoAlojamentoDAO.getTipoAlojamentoById(t.getTipoAlojamento())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        String tipo = r.segment(0);
        if (r.is("GET")) {
            TipoAlojamento t = tipoAlojamentoDAO.getTipoAlojamentoById(tipo);
            if (t == null) {
                throw notFound("Tipo de alojamento");
            }
            r.ok(t);
        } else if (r.is("PUT")) {
            TipoAlojamento t = read(r, false);
            t.setTipoAlojamento(tipo);
            if (!tipoAlojamentoDAO.updateTipoAlojamento(t)) {
                throw notFound("Tipo de alojamento");
            }
            r.ok(message("Tipo de alojamento " + tipo + " atualizado.", tipoAlojamentoDAO.getTipoAlojamentoById(tipo)));
        } else if (r.is("DELETE")) {
            if (!tipoAlojamentoDAO.deleteTipoAlojamento(tipo)) {
                throw notFound("Tipo de alojamento");
            }
            r.ok(message("Tipo de alojamento " + tipo + " eliminado."));
        } else {
            throw methodNotAllowed();
        }
    }

    private TipoAlojamento read(Request r, boolean creating) throws IOException {
        TipoAlojamento t = r.body(TipoAlojamento.class);
        t.setTipoAlojamento(clean(t.getTipoAlojamento()));
        Validator v = new Validator();
        if (creating) {
            v.text("tipoAlojamento", t.getTipoAlojamento(), 11, true);
        }
        v.required("capAlojamento", t.getCapAlojamento())
                .positive("capAlojamento", t.getCapAlojamento(), 2)
                .throwIfInvalid();
        return t;
    }
}
