package com.turismo.api.dao;

import com.turismo.api.model.Alojamento;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Alojamento entity
 */
public class AlojamentoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT a.ID_ALOJAMENTO, a.NOME_ALOJAMENTO, a.CLASS_ALOJAMENTO, a.TIPO_ALOJAMENTO, a.ID_DESTINO, "
                    + "d.NOME_DESTINO, t.CAP_ALOJAMENTO, Media_Avaliacao_Alojamento(a.ID_ALOJAMENTO) AS MEDIA_AVALIACOES "
                    + "FROM ALOJAMENTO a "
                    + "JOIN DESTINO d ON d.ID_DESTINO = a.ID_DESTINO "
                    + "JOIN TIPO_ALOJAMENTO t ON t.TIPO_ALOJAMENTO = a.TIPO_ALOJAMENTO";

    public AlojamentoDAO() {
    }

    public AlojamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Alojamento> getAllAlojamentos() throws SQLException {
        return query(SELECT + " ORDER BY a.ID_ALOJAMENTO", AlojamentoDAO::map);
    }

    public Alojamento getAlojamentoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE a.ID_ALOJAMENTO = ?", AlojamentoDAO::map, id);
    }

    public List<Alojamento> getAlojamentosByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE a.ID_DESTINO = ? ORDER BY a.ID_ALOJAMENTO", AlojamentoDAO::map, idDestino);
    }

    public String addAlojamento(Alojamento a) throws SQLException {
        return inTransaction(c -> IdGenerator.insertWithNewId(c, IdGenerator.Sequence.ALOJAMENTO, id ->
                update(c, "INSERT INTO ALOJAMENTO (ID_ALOJAMENTO, NOME_ALOJAMENTO, TIPO_ALOJAMENTO, CLASS_ALOJAMENTO, ID_DESTINO) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        id, a.getNomeAlojamento(), a.getTipoAlojamento(), a.getClassAlojamento(), a.getIdDestino())));
    }

    public boolean updateAlojamento(Alojamento a) throws SQLException {
        return update("UPDATE ALOJAMENTO SET NOME_ALOJAMENTO = ?, TIPO_ALOJAMENTO = ?, CLASS_ALOJAMENTO = ?, ID_DESTINO = ? "
                        + "WHERE ID_ALOJAMENTO = ?",
                a.getNomeAlojamento(), a.getTipoAlojamento(), a.getClassAlojamento(), a.getIdDestino(),
                a.getIdAlojamento()) > 0;
    }

    /** O trigger deleteAlojamento apaga as associações a reservas e operadores. */
    public boolean deleteAlojamento(String id) throws SQLException {
        return update("DELETE FROM ALOJAMENTO WHERE ID_ALOJAMENTO = ?", id) > 0;
    }

    private static Alojamento map(ResultSet rs) throws SQLException {
        Alojamento a = new Alojamento(
                rs.getString("ID_ALOJAMENTO"),
                rs.getString("NOME_ALOJAMENTO"),
                rs.getBigDecimal("CLASS_ALOJAMENTO"),
                rs.getString("TIPO_ALOJAMENTO"),
                rs.getString("ID_DESTINO"));
        a.setNomeDestino(rs.getString("NOME_DESTINO"));
        a.setCapAlojamento(getInteger(rs, "CAP_ALOJAMENTO"));
        a.setMediaAvaliacoes(rs.getBigDecimal("MEDIA_AVALIACOES"));
        return a;
    }
}
