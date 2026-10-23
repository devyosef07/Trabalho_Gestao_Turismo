package com.turismo.api.dao;

import com.turismo.api.model.Pagamento;
import com.turismo.api.util.ConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Pagamento entity
 *
 * O recibo é sempre gerado pela função Concluir_Pagamento (sequência ID_recibo),
 * por isso a aplicação nunca escreve ID_RECIBO diretamente.
 */
public class PagamentoDAO extends BaseDAO {

    /** Leitura só da view v_lista_pagamentos (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_PAGAMENTOS";

    public PagamentoDAO() {
    }

    public PagamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Pagamento> getAllPagamentos() throws SQLException {
        return query(SELECT + " ORDER BY ID_PAGAMENTO", PagamentoDAO::map);
    }

    public Pagamento getPagamentoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_PAGAMENTO = ?", PagamentoDAO::map, id);
    }

    public List<Pagamento> getPagamentosByReserva(String idReserva) throws SQLException {
        return query(SELECT + " WHERE ID_RESERVA = ? ORDER BY ID_PAGAMENTO", PagamentoDAO::map, idReserva);
    }

    /** Usa a procedure registrar_pagamento; o recibo fica vazio (é gerado por Concluir_Pagamento). */
    public String addPagamento(Pagamento p) throws SQLException {
        return callForNewId(IdGenerator.Sequence.PAGAMENTO, "registrar_pagamento",
                p.getIdReserva(), p.getValor(), p.getDataPagamento(), p.getMetodoPagamento(),
                p.getStatusPagamento(), null, p.getDetalhesTransacao());
    }

    /**
     * Usa a procedure atualizar_pagamento, que só altera pagamentos ainda não
     * concluídos (os concluídos já têm recibo), mesmo em concorrência.
     */
    public boolean updatePagamento(Pagamento p) throws SQLException {
        return callIfFound("atualizar_pagamento", p.getIdPagamento(), p.getValor(), p.getDataPagamento(),
                p.getMetodoPagamento(), p.getStatusPagamento(), p.getDetalhesTransacao(), p.getIdReserva());
    }

    /** Usa a função Concluir_Pagamento, que gera o recibo. */
    public String concluirPagamento(String id) throws SQLException {
        try (Connection c = connections.get()) {
            return requireSuccess(callStringFunction(c, "Concluir_Pagamento", id), "Pagamento concluido");
        }
    }

    /** Usa a função Valor_Em_Dolar com a cotação indicada (meticais por dólar). */
    public BigDecimal valorEmDolar(String id, BigDecimal cotacao) throws SQLException {
        return queryOne("SELECT Valor_Em_Dolar(VALOR, ?) AS VALOR_USD FROM V_LISTA_PAGAMENTOS WHERE ID_PAGAMENTO = ?",
                rs -> rs.getBigDecimal("VALOR_USD"), cotacao, id);
    }

    /** Usa a procedure eliminar_pagamento. */
    public boolean deletePagamento(String id) throws SQLException {
        return callIfFound("eliminar_pagamento", id);
    }

    private static Pagamento map(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento(
                rs.getString("ID_PAGAMENTO"),
                rs.getBigDecimal("VALOR"),
                getDate(rs, "DATA_PAGAMENTO"),
                rs.getString("METODO_PAGAMENTO"),
                rs.getString("STATUS_PAGAMENTO"),
                rs.getString("ID_RECIBO"),
                rs.getString("DETALHES_TRANSACAO"),
                rs.getString("ID_RESERVA"));
        p.setNomeTurista(rs.getString("NOME_TURISTA"));
        return p;
    }
}
