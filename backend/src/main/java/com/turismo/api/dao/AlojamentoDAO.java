package com.turismo.api.dao;

import com.turismo.api.model.Alojamento;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Alojamento entity
 *
 * Na view v_lista_alojamentos a capacidade vem da função Capacidade_Alojamento
 * (lê-a do tipo de alojamento) e a média das avaliações da função
 * Media_Avaliacao_Alojamento.
 */
public class AlojamentoDAO extends BaseDAO {

    /** Leitura só da view v_lista_alojamentos (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_ALOJAMENTOS";

    public AlojamentoDAO() {
    }

    public AlojamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Alojamento> getAllAlojamentos() throws SQLException {
        return query(SELECT + " ORDER BY ID_ALOJAMENTO", AlojamentoDAO::map);
    }

    public Alojamento getAlojamentoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_ALOJAMENTO = ?", AlojamentoDAO::map, id);
    }

    public List<Alojamento> getAlojamentosByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE ID_DESTINO = ? ORDER BY ID_ALOJAMENTO", AlojamentoDAO::map, idDestino);
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
