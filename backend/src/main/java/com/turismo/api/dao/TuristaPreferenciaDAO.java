package com.turismo.api.dao;

import com.turismo.api.model.TuristaPreferencia;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for TuristaPreferencia entity (chave composta ID_turista + preferencia)
 */
public class TuristaPreferenciaDAO extends BaseDAO {

    /** Leitura só da view v_lista_preferencias (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_PREFERENCIAS";

    public TuristaPreferenciaDAO() {
    }

    public TuristaPreferenciaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<TuristaPreferencia> getAllTuristaPreferencias() throws SQLException {
        return query(SELECT + " ORDER BY ID_TURISTA, PREFERENCIA", TuristaPreferenciaDAO::map);
    }

    public List<TuristaPreferencia> getTuristaPreferenciasByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE ID_TURISTA = ? ORDER BY PREFERENCIA", TuristaPreferenciaDAO::map, idTurista);
    }

    public TuristaPreferencia getTuristaPreferencia(String idTurista, String preferencia) throws SQLException {
        return queryOne(SELECT + " WHERE ID_TURISTA = ? AND PREFERENCIA = ?", TuristaPreferenciaDAO::map,
                idTurista, preferencia);
    }

    /** Usa a procedure inserir_preferencia. */
    public String addTuristaPreferencia(TuristaPreferencia tp) throws SQLException {
        return call("inserir_preferencia", tp.getIdTurista(), tp.getPreferencia());
    }

    /** Usa a procedure atualizar_pref_turista, que troca só a preferência indicada. */
    public boolean updateTuristaPreferencia(String idTurista, String preferenciaAtual, String novaPreferencia)
            throws SQLException {
        return callIfFound("atualizar_pref_turista", idTurista, preferenciaAtual, novaPreferencia);
    }

    /** Usa a procedure remover_preferencia. */
    public boolean deleteTuristaPreferencia(String idTurista, String preferencia) throws SQLException {
        return callIfFound("remover_preferencia", idTurista, preferencia);
    }

    private static TuristaPreferencia map(ResultSet rs) throws SQLException {
        TuristaPreferencia tp = new TuristaPreferencia(rs.getString("ID_TURISTA"), rs.getString("PREFERENCIA"));
        tp.setNomeTurista(rs.getString("NOME_TURISTA"));
        return tp;
    }
}
