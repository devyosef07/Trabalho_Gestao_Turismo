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

    /** Usa a procedure registrar_transporte. */
    public String addTransporte(Transporte t) throws SQLException {
        return callForNewId(IdGenerator.Sequence.TRANSPORTE, "registrar_transporte",
                t.getTipoTransporte(), t.getCapTransporte(), t.getDisponibilidade());
    }

    /** Usa a procedure atualizar_transporte. */
    public boolean updateTransporte(Transporte t) throws SQLException {
        return callIfFound("atualizar_transporte", t.getIdTransporte(), t.getTipoTransporte(),
                t.getCapTransporte(), t.getDisponibilidade());
    }

    /** Usa a procedure eliminar_transporte; o trigger deleteTransporte apaga as associações a operadores. */
    public boolean deleteTransporte(String id) throws SQLException {
        return callIfFound("eliminar_transporte", id);
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
