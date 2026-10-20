package com.turismo.api.handler;

import com.turismo.api.dao.OperadorDAO;
import com.turismo.api.model.Operador;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Operador endpoints
 *
 * GET/POST /api/operadores · GET/PUT/DELETE /api/operadores/{id}
 */
public class OperadorHandler extends BaseHandler {
    private final OperadorDAO operadorDAO;

    public OperadorHandler() {
        this(new OperadorDAO(), ConnectionProvider.DEFAULT);
    }

    public OperadorHandler(OperadorDAO operadorDAO, ConnectionProvider connections) {
        super("/api/operadores", connections);
        this.operadorDAO = operadorDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                r.ok(operadorDAO.getAllOperadores());
            } else if (r.is("POST")) {
                String id = operadorDAO.addOperador(read(r));
                r.created(message("Operador " + id + " criado.", operadorDAO.getOperadorById(id)));
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
            Operador o = operadorDAO.getOperadorById(id);
            if (o == null) {
                throw notFound("Operador");
            }
            r.ok(o);
        } else if (r.is("PUT")) {
            Operador o = read(r);
            o.setIdOperador(id);
            if (!operadorDAO.updateOperador(o)) {
                throw notFound("Operador");
            }
            r.ok(message("Operador " + id + " atualizado.", operadorDAO.getOperadorById(id)));
        } else if (r.is("DELETE")) {
            if (!operadorDAO.deleteOperador(id)) {
                throw notFound("Operador");
            }
            r.ok(message("Operador " + id + " eliminado."));
        } else {
            throw methodNotAllowed();
        }
    }

    private Operador read(Request r) throws IOException {
        Operador o = r.body(Operador.class);
        o.setNomeOperador(clean(o.getNomeOperador()));
        o.setEspecialidade(clean(o.getEspecialidade()));
        new Validator()
                .text("nomeOperador", o.getNomeOperador(), 25, true)
                .text("especialidade", o.getEspecialidade(), 15, true)
                .rating("classOperador", o.getClassOperador())
                .throwIfInvalid();
        return o;
    }
}
