package com.turismo.api.dao;

import com.turismo.api.model.OperadorAlojamento;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for OperadorAlojamento entity (chave composta operador + alojamento)
 */
public class OperadorAlojamentoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT oa.ID_OPERADOR, oa.ID_ALOJAMENTO, o.NOME_OPERADOR, a.NOME_ALOJAMENTO "
                    + "FROM OPERADOR_ALOJAMENTO oa "
                    + "JOIN OPERADOR o ON o.ID_OPERADOR = oa.ID_OPERADOR "
                    + "JOIN ALOJAMENTO a ON a.ID_ALOJAMENTO = oa.ID_ALOJAMENTO";
    private static final String ORDER = " ORDER BY oa.ID_OPERADOR, oa.ID_ALOJAMENTO";

    public OperadorAlojamentoDAO() {
    }

    public OperadorAlojamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<OperadorAlojamento> getAllOperadorAlojamentos() throws SQLException {
        return query(SELECT + ORDER, OperadorAlojamentoDAO::map);
    }

    public List<OperadorAlojamento> getOperadorAlojamentosByOperador(String idOperador) throws SQLException {
        return query(SELECT + " WHERE oa.ID_OPERADOR = ?" + ORDER, OperadorAlojamentoDAO::map, idOperador);
    }

    public List<OperadorAlojamento> getOperadorAlojamentosByAlojamento(String idAlojamento) throws SQLException {
        return query(SELECT + " WHERE oa.ID_ALOJAMENTO = ?" + ORDER, OperadorAlojamentoDAO::map, idAlojamento);
    }

    public OperadorAlojamento getOperadorAlojamento(String idOperador, String idAlojamento) throws SQLException {
        return queryOne(SELECT + " WHERE oa.ID_OPERADOR = ? AND oa.ID_ALOJAMENTO = ?", OperadorAlojamentoDAO::map,
                idOperador, idAlojamento);
    }

    /** Usa a procedure associar_operador_alojamento. */
    public String addOperadorAlojamento(OperadorAlojamento oa) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "associar_operador_alojamento", oa.getIdOperador(), oa.getIdAlojamento());
        }
    }

    public boolean deleteOperadorAlojamento(String idOperador, String idAlojamento) throws SQLException {
        return update("DELETE FROM OPERADOR_ALOJAMENTO WHERE ID_OPERADOR = ? AND ID_ALOJAMENTO = ?",
                idOperador, idAlojamento) > 0;
    }

    private static OperadorAlojamento map(ResultSet rs) throws SQLException {
        OperadorAlojamento oa = new OperadorAlojamento(rs.getString("ID_OPERADOR"), rs.getString("ID_ALOJAMENTO"));
        oa.setNomeOperador(rs.getString("NOME_OPERADOR"));
        oa.setNomeAlojamento(rs.getString("NOME_ALOJAMENTO"));
        return oa;
    }
}
