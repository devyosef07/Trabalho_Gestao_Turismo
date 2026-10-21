package com.turismo.api.dao;

import com.turismo.api.model.Transporte;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Transporte entity
 */
public class TransporteDAO extends BaseDAO {

    /** Leitura só da view v_lista_transportes (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_TRANSPORTES";

    public TransporteDAO() {
    }

    public TransporteDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Transporte> getAllTransportes() throws SQLException {
        return query(SELECT + " ORDER BY ID_TRANSPORTE", TransporteDAO::map);
    }

    public Transporte getTransporteById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_TRANSPORTE = ?", TransporteDAO::map, id);
    }

    public String addTransporte(Transporte t) throws SQLException {
        return inTransaction(c -> IdGenerator.insertWithNewId(c, IdGenerator.Sequence.TRANSPORTE, id ->
                update(c, "INSERT INTO TRANSPORTE (ID_TRANSPORTE, TIPO_TRANSPORTE, CAP_TRANSPORTE, DISPONIBILIDADE) VALUES (?, ?, ?, ?)",
                        id, t.getTipoTransporte(), t.getCapTransporte(), t.getDisponibilidade())));
    }

    public boolean updateTransporte(Transporte t) throws SQLException {
        return update("UPDATE TRANSPORTE SET TIPO_TRANSPORTE = ?, CAP_TRANSPORTE = ?, DISPONIBILIDADE = ? WHERE ID_TRANSPORTE = ?",
                t.getTipoTransporte(), t.getCapTransporte(), t.getDisponibilidade(), t.getIdTransporte()) > 0;
    }

    /** O trigger deleteTransporte apaga as associações a operadores. */
    public boolean deleteTransporte(String id) throws SQLException {
        return update("DELETE FROM TRANSPORTE WHERE ID_TRANSPORTE = ?", id) > 0;
    }

    private static Transporte map(ResultSet rs) throws SQLException {
        Transporte t = new Transporte(
                rs.getString("ID_TRANSPORTE"),
                rs.getString("TIPO_TRANSPORTE"),
                getInteger(rs, "CAP_TRANSPORTE"),
                getTrimmed(rs, "DISPONIBILIDADE"));
        t.setTotalOperadores(rs.getInt("TOTAL_OPERADORES"));
        return t;
    }
}
