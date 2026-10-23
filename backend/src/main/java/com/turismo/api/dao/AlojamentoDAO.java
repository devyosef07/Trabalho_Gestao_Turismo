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

    /** Usa a procedure registrar_alojamento. */
    public String addAlojamento(Alojamento a) throws SQLException {
        return callForNewId(IdGenerator.Sequence.ALOJAMENTO, "registrar_alojamento",
                a.getNomeAlojamento(), a.getTipoAlojamento(), a.getClassAlojamento(), a.getIdDestino());
    }

    /** Usa a procedure atualizar_alojamento. */
    public boolean updateAlojamento(Alojamento a) throws SQLException {
        return callIfFound("atualizar_alojamento", a.getIdAlojamento(), a.getNomeAlojamento(),
                a.getTipoAlojamento(), a.getClassAlojamento(), a.getIdDestino());
    }

    /** Usa a procedure eliminar_alojamento; o trigger deleteAlojamento apaga as associações a reservas e operadores. */
    public boolean deleteAlojamento(String id) throws SQLException {
        return callIfFound("eliminar_alojamento", id);
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
