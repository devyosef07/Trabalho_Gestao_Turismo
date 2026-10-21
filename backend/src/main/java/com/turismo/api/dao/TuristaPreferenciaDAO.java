package com.turismo.api.dao;

import com.turismo.api.model.TuristaPreferencia;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
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
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "inserir_preferencia", tp.getIdTurista(), tp.getPreferencia());
        }
    }

    /**
     * Troca uma preferência por outra numa só transação.
     * Não usa atualizar_pref_turista porque essa procedure altera TODAS as
     * preferências do turista (WHERE só por ID_turista), o que viola a chave
     * composta quando o turista tem mais do que uma.
     */
    public boolean updateTuristaPreferencia(String idTurista, String preferenciaAtual, String novaPreferencia)
            throws SQLException {
        return inTransaction(c -> {
            int removed = update(c, "DELETE FROM TURISTA_PREFERENCIA WHERE ID_TURISTA = ? AND PREFERENCIA = ?",
                    idTurista, preferenciaAtual);
            if (removed == 0) {
                return false;
            }
            update(c, "INSERT INTO TURISTA_PREFERENCIA (ID_TURISTA, PREFERENCIA) VALUES (?, ?)", idTurista, novaPreferencia);
            return true;
        });
    }

    public boolean deleteTuristaPreferencia(String idTurista, String preferencia) throws SQLException {
        return update("DELETE FROM TURISTA_PREFERENCIA WHERE ID_TURISTA = ? AND PREFERENCIA = ?", idTurista, preferencia) > 0;
    }

    private static TuristaPreferencia map(ResultSet rs) throws SQLException {
        TuristaPreferencia tp = new TuristaPreferencia(rs.getString("ID_TURISTA"), rs.getString("PREFERENCIA"));
        tp.setNomeTurista(rs.getString("NOME_TURISTA"));
        return tp;
    }
}
