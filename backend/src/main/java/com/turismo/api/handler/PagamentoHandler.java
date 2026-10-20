package com.turismo.api.handler;

import com.turismo.api.dao.PagamentoDAO;
import com.turismo.api.model.Pagamento;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP handler for Pagamento endpoints
 *
 * GET/POST /api/pagamentos · GET/PUT/DELETE /api/pagamentos/{id}
 * POST /api/pagamentos/{id}/concluir · GET /api/pagamentos/{id}/dolar?cotacao=64
 * GET /api/pagamentos/reserva/{idReserva}
 */
public class PagamentoHandler extends BaseHandler {
    private final PagamentoDAO pagamentoDAO;

    /** Estados que se podem escolher; CONCLUIDO só através de Concluir_Pagamento (gera o recibo). */
    static final List<String> ESTADOS_EDITAVEIS = List.of("PENDENTE", "FALHADO");

    public PagamentoHandler() {
        this(new PagamentoDAO(), ConnectionProvider.DEFAULT);
    }

    public PagamentoHandler(PagamentoDAO pagamentoDAO, ConnectionProvider connections) {
        super("/api/pagamentos", connections);
        this.pagamentoDAO = pagamentoDAO;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() == 0) {
            if (r.is("GET")) {
                String idReserva = r.param("idReserva");
                r.ok(idReserva == null ? pagamentoDAO.getAllPagamentos() : pagamentoDAO.getPagamentosByReserva(idReserva));
            } else if (r.is("POST")) {
                String id = pagamentoDAO.addPagamento(read(r));
                r.created(message("Pagamento " + id + " registado.", pagamentoDAO.getPagamentoById(id)));
            } else {
                throw methodNotAllowed();
            }
            return;
        }
        if (r.size() == 2 && "reserva".equals(r.segment(0)) && r.is("GET")) {
            r.ok(pagamentoDAO.getPagamentosByReserva(r.segment(1)));
            return;
        }
        String id = r.segment(0);
        if (r.size() == 2 && "concluir".equals(r.segment(1))) {
            if (!r.is("POST")) {
                throw methodNotAllowed();
            }
            String msg = pagamentoDAO.concluirPagamento(id);
            r.ok(message(msg + ".", pagamentoDAO.getPagamentoById(id)));
            return;
        }
        if (r.size() == 2 && "dolar".equals(r.segment(1))) {
            if (!r.is("GET")) {
                throw methodNotAllowed();
            }
            BigDecimal cotacao = parseDecimal(r.param("cotacao"), "cotacao");
            if (cotacao.signum() <= 0) {
                throw ApiException.badRequest("A cotação tem de ser maior que zero.");
            }
            BigDecimal usd = pagamentoDAO.valorEmDolar(id, cotacao);
            if (usd == null) {
                throw notFound("Pagamento");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("idPagamento", id);
            body.put("cotacao", cotacao);
            body.put("valorUsd", usd);
            r.ok(body);
            return;
        }
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        if (r.is("GET")) {
            Pagamento p = pagamentoDAO.getPagamentoById(id);
            if (p == null) {
                throw notFound("Pagamento");
            }
            r.ok(p);
        } else if (r.is("PUT")) {
            Pagamento atual = pagamentoDAO.getPagamentoById(id);
            if (atual == null) {
                throw notFound("Pagamento");
            }
            if ("CONCLUIDO".equals(atual.getStatusPagamento())) {
                throw ApiException.conflict("Um pagamento concluído (com recibo) não pode ser alterado.");
            }
            Pagamento p = read(r);
            p.setIdPagamento(id);
            if (!pagamentoDAO.updatePagamento(p)) {
                throw ApiException.conflict("O pagamento foi concluído entretanto e já não pode ser alterado.");
            }
            r.ok(message("Pagamento " + id + " atualizado.", pagamentoDAO.getPagamentoById(id)));
        } else if (r.is("DELETE")) {
            if (!pagamentoDAO.deletePagamento(id)) {
                throw notFound("Pagamento");
            }
            r.ok(message("Pagamento " + id + " eliminado."));
        } else {
            throw methodNotAllowed();
        }
    }

    /**
     * Regras de tabelas.sql e do trigger validarpagamento: a data não pode ser
     * no futuro nem ter mais de 12 meses.
     */
    private Pagamento read(Request r) throws IOException, SQLException {
        Pagamento p = r.body(Pagamento.class);
        p.setMetodoPagamento(clean(p.getMetodoPagamento()));
        p.setStatusPagamento(clean(p.getStatusPagamento()));
        p.setDetalhesTransacao(clean(p.getDetalhesTransacao()));
        p.setIdReserva(clean(p.getIdReserva()));
        if (p.getStatusPagamento() == null) {
            p.setStatusPagamento("PENDENTE");
        }
        LocalDate hoje = LocalDate.now();
        new Validator()
                .required("valor", p.getValor())
                .money("valor", p.getValor())
                .required("dataPagamento", p.getDataPagamento())
                .check(p.getDataPagamento() == null || !p.getDataPagamento().isAfter(hoje), "dataPagamento",
                        "Não pode ser no futuro.")
                .check(p.getDataPagamento() == null || !p.getDataPagamento().isBefore(hoje.minusMonths(12)),
                        "dataPagamento", "Não pode ter mais de 12 meses.")
                .required("metodoPagamento", p.getMetodoPagamento())
                .oneOf("metodoPagamento", p.getMetodoPagamento(), domain("PAGAMENTO", "METODO_PAGAMENTO"))
                .oneOf("statusPagamento", p.getStatusPagamento(), ESTADOS_EDITAVEIS)
                .text("detalhesTransacao", p.getDetalhesTransacao(), 200, false)
                .required("idReserva", p.getIdReserva())
                .throwIfInvalid();
        return p;
    }
}
