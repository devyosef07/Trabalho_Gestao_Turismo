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

    private static final String SELECT =
            "SELECT ot.ID_OPERADOR, ot.ID_TURISTA, ot.AVALIACAO_OPERADOR, o.NOME_OPERADOR, t.NOME_TURISTA "
                    + "FROM OPERADOR_TURISTA ot "
                    + "JOIN OPERADOR o ON o.ID_OPERADOR = ot.ID_OPERADOR "
                    + "JOIN TURISTA t ON t.ID_TURISTA = ot.ID_TURISTA";
    private static final String ORDER = " ORDER BY ot.ID_OPERADOR, ot.ID_TURISTA";

    public OperadorTuristaDAO() {
    }

    public OperadorTuristaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<OperadorTurista> getAllOperadorTuristas() throws SQLException {
        return query(SELECT + ORDER, OperadorTuristaDAO::map);
    }

    public List<OperadorTurista> getOperadorTuristasByOperador(String idOperador) throws SQLException {
        return query(SELECT + " WHERE ot.ID_OPERADOR = ?" + ORDER, OperadorTuristaDAO::map, idOperador);
    }

    public List<OperadorTurista> getOperadorTuristasByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE ot.ID_TURISTA = ?" + ORDER, OperadorTuristaDAO::map, idTurista);
    }

    public OperadorTurista getOperadorTurista(String idOperador, String idTurista) throws SQLException {
        return queryOne(SELECT + " WHERE ot.ID_OPERADOR = ? AND ot.ID_TURISTA = ?", OperadorTuristaDAO::map,
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

    public boolean deleteOperadorTurista(String idOperador, String idTurista) throws SQLException {
        return update("DELETE FROM OPERADOR_TURISTA WHERE ID_OPERADOR = ? AND ID_TURISTA = ?", idOperador, idTurista) > 0;
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
