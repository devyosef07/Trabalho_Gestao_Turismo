package com.turismo.api.dao;

import com.turismo.api.model.Operador;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Operador entity
 */
public class OperadorDAO extends BaseDAO {

    /** Leitura só da view v_lista_operadores (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_OPERADORES";

    public OperadorDAO() {
    }

    public OperadorDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Operador> getAllOperadores() throws SQLException {
        return query(SELECT + " ORDER BY ID_OPERADOR", OperadorDAO::map);
    }

    public Operador getOperadorById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_OPERADOR = ?", OperadorDAO::map, id);
    }

    /** Usa a procedure registrar_operador. */
    public String addOperador(Operador o) throws SQLException {
        return callForNewId(IdGenerator.Sequence.OPERADOR, "registrar_operador",
                o.getNomeOperador(), o.getEspecialidade(), o.getClassOperador());
    }

    /** Usa a procedure atualizar_operador. */
    public boolean updateOperador(Operador o) throws SQLException {
        return callIfFound("atualizar_operador", o.getIdOperador(), o.getNomeOperador(),
                o.getEspecialidade(), o.getClassOperador());
    }

    /** Usa a procedure eliminar_operador; o trigger deleteOperador apaga as associações a turistas, transportes e alojamentos. */
    public boolean deleteOperador(String id) throws SQLException {
        return callIfFound("eliminar_operador", id);
    }

    private static Operador map(ResultSet rs) throws SQLException {
        Operador o = new Operador(
                rs.getString("ID_OPERADOR"),
                rs.getString("NOME_OPERADOR"),
                rs.getString("ESPECIALIDADE"),
                rs.getBigDecimal("CLASS_OPERADOR"));
        o.setTotalTuristas(rs.getInt("TOTAL_TURISTAS"));
        o.setMediaAvaliacao(rs.getBigDecimal("MEDIA_AVALIACAO"));
        return o;
    }
}
