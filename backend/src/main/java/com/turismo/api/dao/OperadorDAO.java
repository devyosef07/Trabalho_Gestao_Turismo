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

    private static final String SELECT =
            "SELECT o.ID_OPERADOR, o.NOME_OPERADOR, o.ESPECIALIDADE, o.CLASS_OPERADOR, "
                    + "(SELECT COUNT(*) FROM OPERADOR_TURISTA ot WHERE ot.ID_OPERADOR = o.ID_OPERADOR) AS TOTAL_TURISTAS, "
                    + "Media_Avaliacao_Operador(o.ID_OPERADOR) AS MEDIA_AVALIACAO "
                    + "FROM OPERADOR o";

    public OperadorDAO() {
    }

    public OperadorDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Operador> getAllOperadores() throws SQLException {
        return query(SELECT + " ORDER BY o.ID_OPERADOR", OperadorDAO::map);
    }

    public Operador getOperadorById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE o.ID_OPERADOR = ?", OperadorDAO::map, id);
    }

    public String addOperador(Operador o) throws SQLException {
        return inTransaction(c -> IdGenerator.insertWithNewId(c, IdGenerator.Sequence.OPERADOR, id ->
                update(c, "INSERT INTO OPERADOR (ID_OPERADOR, NOME_OPERADOR, ESPECIALIDADE, CLASS_OPERADOR) VALUES (?, ?, ?, ?)",
                        id, o.getNomeOperador(), o.getEspecialidade(), o.getClassOperador())));
    }

    public boolean updateOperador(Operador o) throws SQLException {
        return update("UPDATE OPERADOR SET NOME_OPERADOR = ?, ESPECIALIDADE = ?, CLASS_OPERADOR = ? WHERE ID_OPERADOR = ?",
                o.getNomeOperador(), o.getEspecialidade(), o.getClassOperador(), o.getIdOperador()) > 0;
    }

    /** O trigger deleteOperador apaga as associações a turistas, transportes e alojamentos. */
    public boolean deleteOperador(String id) throws SQLException {
        return update("DELETE FROM OPERADOR WHERE ID_OPERADOR = ?", id) > 0;
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
