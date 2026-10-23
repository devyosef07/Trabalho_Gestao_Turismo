package com.turismo.api.dao;

import com.turismo.api.model.OperadorTurista;
import com.turismo.api.util.ConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for OperadorTurista entity (chave composta operador + turista)
 */
public class OperadorTuristaDAO extends BaseDAO {

    /** Leitura só da view v_lista_operador_turistas (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_OPERADOR_TURISTAS";
    private static final String ORDER = " ORDER BY ID_OPERADOR, ID_TURISTA";

    public OperadorTuristaDAO() {
    }

    public OperadorTuristaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<OperadorTurista> getAllOperadorTuristas() throws SQLException {
        return query(SELECT + ORDER, OperadorTuristaDAO::map);
    }

    public List<OperadorTurista> getOperadorTuristasByOperador(String idOperador) throws SQLException {
        return query(SELECT + " WHERE ID_OPERADOR = ?" + ORDER, OperadorTuristaDAO::map, idOperador);
    }

    public List<OperadorTurista> getOperadorTuristasByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE ID_TURISTA = ?" + ORDER, OperadorTuristaDAO::map, idTurista);
    }

    public OperadorTurista getOperadorTurista(String idOperador, String idTurista) throws SQLException {
        return queryOne(SELECT + " WHERE ID_OPERADOR = ? AND ID_TURISTA = ?", OperadorTuristaDAO::map,
                idOperador, idTurista);
    }

    /** Usa a procedure associar_operador_turista (a avaliação fica vazia). */
    public String addOperadorTurista(OperadorTurista ot) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "associar_operador_turista", ot.getIdOperador(), ot.getIdTurista());
        }
    }

    /** Usa a procedure avaliar_operador. */
    public String avaliar(String idOperador, String idTurista, BigDecimal avaliacao) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "avaliar_operador", idOperador, idTurista, avaliacao);
        }
    }

    /** Usa a procedure remover_operador_turista. */
    public boolean deleteOperadorTurista(String idOperador, String idTurista) throws SQLException {
        return callIfFound("remover_operador_turista", idOperador, idTurista);
    }

    private static OperadorTurista map(ResultSet rs) throws SQLException {
        OperadorTurista ot = new OperadorTurista(
                rs.getString("ID_OPERADOR"),
                rs.getString("ID_TURISTA"),
                rs.getBigDecimal("AVALIACAO_OPERADOR"));
        ot.setNomeOperador(rs.getString("NOME_OPERADOR"));
        ot.setNomeTurista(rs.getString("NOME_TURISTA"));
        return ot;
    }
}
