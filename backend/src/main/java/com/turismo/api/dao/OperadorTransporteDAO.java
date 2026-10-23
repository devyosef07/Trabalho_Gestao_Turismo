package com.turismo.api.dao;

import com.turismo.api.model.OperadorTransporte;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for OperadorTransporte entity (chave composta operador + transporte)
 */
public class OperadorTransporteDAO extends BaseDAO {

    /** Leitura só da view v_lista_operador_transportes (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_OPERADOR_TRANSPORTES";
    private static final String ORDER = " ORDER BY ID_OPERADOR, ID_TRANSPORTE";

    public OperadorTransporteDAO() {
    }

    public OperadorTransporteDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<OperadorTransporte> getAllOperadorTransportes() throws SQLException {
        return query(SELECT + ORDER, OperadorTransporteDAO::map);
    }

    public List<OperadorTransporte> getOperadorTransportesByOperador(String idOperador) throws SQLException {
        return query(SELECT + " WHERE ID_OPERADOR = ?" + ORDER, OperadorTransporteDAO::map, idOperador);
    }

    public List<OperadorTransporte> getOperadorTransportesByTransporte(String idTransporte) throws SQLException {
        return query(SELECT + " WHERE ID_TRANSPORTE = ?" + ORDER, OperadorTransporteDAO::map, idTransporte);
    }

    public OperadorTransporte getOperadorTransporte(String idOperador, String idTransporte) throws SQLException {
        return queryOne(SELECT + " WHERE ID_OPERADOR = ? AND ID_TRANSPORTE = ?", OperadorTransporteDAO::map,
                idOperador, idTransporte);
    }

    /** Usa a procedure associar_operador_transporte. */
    public String addOperadorTransporte(OperadorTransporte ot) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "associar_operador_transporte", ot.getIdOperador(), ot.getIdTransporte());
        }
    }

    /** Usa a procedure remover_operador_transporte. */
    public boolean deleteOperadorTransporte(String idOperador, String idTransporte) throws SQLException {
        return callIfFound("remover_operador_transporte", idOperador, idTransporte);
    }

    private static OperadorTransporte map(ResultSet rs) throws SQLException {
        OperadorTransporte ot = new OperadorTransporte(rs.getString("ID_OPERADOR"), rs.getString("ID_TRANSPORTE"));
        ot.setNomeOperador(rs.getString("NOME_OPERADOR"));
        ot.setTipoTransporte(rs.getString("TIPO_TRANSPORTE"));
        return ot;
    }
}
