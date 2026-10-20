package com.turismo.api.handler;

import com.turismo.api.dao.OperadorAlojamentoDAO;
import com.turismo.api.model.OperadorAlojamento;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for OperadorAlojamento endpoints
 *
 * GET/POST /api/operadorAlojamentos (?idOperador= | ?idAlojamento=)
 * GET/DELETE /api/operadorAlojamentos/{idOperador}/{idAlojamento}
 * GET /api/operadorAlojamentos/operador/{idOperador} · /alojamento/{idAlojamento}
 *
 * Não há PUT: as duas colunas fazem parte da chave primária.
 */
public class OperadorAlojamentoHandler extends BaseHandler {
    private final OperadorAlojamentoDAO operadorAlojamentoDAO;

    public OperadorAlojamentoHandler() {
        this(new OperadorAlojamentoDAO(), ConnectionProvider.DEFAULT);
    }

    public OperadorAlojamentoHandler(OperadorAlojamentoDAO dao, ConnectionProvider connections) {
        super("/api/operadorAlojamentos", connections);
        this.operadorAlojamentoDAO = dao;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idOperador = r.param("idOperador");
                String idAlojamento = r.param("idAlojamento");
                if (idOperador != null) {
                    r.ok(operadorAlojamentoDAO.getOperadorAlojamentosByOperador(idOperador));
                } else if (idAlojamento != null) {
                    r.ok(operadorAlojamentoDAO.getOperadorAlojamentosByAlojamento(idAlojamento));
                } else {
                    r.ok(operadorAlojamentoDAO.getAllOperadorAlojamentos());
                }
            } else if (r.is("POST")) {
                OperadorAlojamento o = r.body(OperadorAlojamento.class);
                o.setIdOperador(clean(o.getIdOperador()));
                o.setIdAlojamento(clean(o.getIdAlojamento()));
                new Validator()
                        .required("idOperador", o.getIdOperador())
                        .required("idAlojamento", o.getIdAlojamento())
                        .throwIfInvalid();
                String msg = operadorAlojamentoDAO.addOperadorAlojamento(o);
                r.created(message(msg, operadorAlojamentoDAO.getOperadorAlojamento(o.getIdOperador(), o.getIdAlojamento())));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && r.is("GET") && "operador".equals(r.segment(0))) {
            r.ok(operadorAlojamentoDAO.getOperadorAlojamentosByOperador(r.segment(1)));
            return;
        }
        if (r.size() == 2 && r.is("GET") && "alojamento".equals(r.segment(0))) {
            r.ok(operadorAlojamentoDAO.getOperadorAlojamentosByAlojamento(r.segment(1)));
            return;
        }
        if (r.size() != 2) {
            throw notFound("Endpoint");
        }
        String idOperador = r.segment(0);
        String idAlojamento = r.segment(1);
        if (r.is("GET")) {
            OperadorAlojamento o = operadorAlojamentoDAO.getOperadorAlojamento(idOperador, idAlojamento);
            if (o == null) {
                throw notFound("Associação");
            }
            r.ok(o);
        } else if (r.is("DELETE")) {
            if (!operadorAlojamentoDAO.deleteOperadorAlojamento(idOperador, idAlojamento)) {
                throw notFound("Associação");
            }
            r.ok(message("Associação removida."));
        } else {
            throw methodNotAllowed();
        }
    }
}
