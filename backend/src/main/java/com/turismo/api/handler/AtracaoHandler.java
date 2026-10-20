package com.turismo.api.handler;

import com.turismo.api.dao.AtracaoDAO;
import com.turismo.api.model.Atracao;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;

/**
 * HTTP handler for Atracao endpoints
 *
 * GET/POST /api/atracoes · GET/PUT/DELETE /api/atracoes/{id}
 * GET /api/atracoes/destino/{idDestino}
 */
public class AtracaoHandler extends BaseHandler {
    private final AtracaoDAO atracaoDAO;

    public AtracaoHandler() {
        this(new AtracaoDAO(), ConnectionProvider.DEFAULT);
    }

    public AtracaoHandler(AtracaoDAO atracaoDAO, ConnectionProvider connections) {
        super("/api/atracoes", connections);
        this.atracaoDAO = atracaoDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idDestino = r.param("idDestino");
                r.ok(idDestino == null ? atracaoDAO.getAllAtracoes() : atracaoDAO.getAtracoesByDestino(idDestino));
            } else if (r.is("POST")) {
                String id = atracaoDAO.addAtracao(read(r));
                r.created(message("Atração " + id + " criada.", atracaoDAO.getAtracaoById(id)));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && "destino".equals(r.segment(0)) && r.is("GET")) {
            r.ok(atracaoDAO.getAtracoesByDestino(r.segment(1)));
            return;
        }
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        String id = r.segment(0);
        if (r.is("GET")) {
            Atracao a = atracaoDAO.getAtracaoById(id);
            if (a == null) {
                throw notFound("Atração");
            }
            r.ok(a);
        } else if (r.is("PUT")) {
            Atracao a = read(r);
            a.setIdAtracao(id);
            if (!atracaoDAO.updateAtracao(a)) {
                throw notFound("Atração");
            }
            r.ok(message("Atração " + id + " atualizada.", atracaoDAO.getAtracaoById(id)));
        } else if (r.is("DELETE")) {
            if (!atracaoDAO.deleteAtracao(id)) {
                throw notFound("Atração");
            }
            r.ok(message("Atração " + id + " eliminada."));
        } else {
            throw methodNotAllowed();
        }
    }

    private Atracao read(Request r) throws IOException, SQLException {
        Atracao a = r.body(Atracao.class);
        a.setTipoAtracao(clean(a.getTipoAtracao()));
        a.setDescricao(clean(a.getDescricao()));
        a.setHoraAbertura(clean(a.getHoraAbertura()));
        a.setHoraFecho(clean(a.getHoraFecho()));
        a.setHoraAberturaEspecial(clean(a.getHoraAberturaEspecial()));
        a.setHoraFechoEspecial(clean(a.getHoraFechoEspecial()));
        a.setDetalhesHorarioEspecial(clean(a.getDetalhesHorarioEspecial()));
        a.setIdDestino(clean(a.getIdDestino()));

        boolean anyEspecial = a.getHoraAberturaEspecial() != null || a.getHoraFechoEspecial() != null
                || a.getDetalhesHorarioEspecial() != null;
        Validator v = new Validator()
                .required("tipoAtracao", a.getTipoAtracao())
                .oneOf("tipoAtracao", a.getTipoAtracao(), domain("ATRACAO", "TIPO_ATRACAO"))
                .text("descricao", a.getDescricao(), 200, false)
                .required("horaAbertura", a.getHoraAbertura())
                .hour("horaAbertura", a.getHoraAbertura())
                .required("horaFecho", a.getHoraFecho())
                .hour("horaFecho", a.getHoraFecho())
                .after("horaFecho", a.getHoraFecho(), a.getHoraAbertura(), "Tem de ser depois da hora de abertura.")
                .required("idDestino", a.getIdDestino());
        if (anyEspecial) {
            // A constraint CHECK exige os três campos do horário especial juntos.
            v.required("horaAberturaEspecial", a.getHoraAberturaEspecial())
                    .hour("horaAberturaEspecial", a.getHoraAberturaEspecial())
                    .required("horaFechoEspecial", a.getHoraFechoEspecial())
                    .hour("horaFechoEspecial", a.getHoraFechoEspecial())
                    .after("horaFechoEspecial", a.getHoraFechoEspecial(), a.getHoraAberturaEspecial(),
                            "Tem de ser depois da hora de abertura especial.")
                    .required("detalhesHorarioEspecial", a.getDetalhesHorarioEspecial())
                    .text("detalhesHorarioEspecial", a.getDetalhesHorarioEspecial(), 200, true);
        }
        v.throwIfInvalid();
        return a;
    }
}
