package com.turismo.api.dao;

import com.turismo.api.model.TuristaDestino;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for TuristaDestino entity (chave composta destino + turista + data)
 *
 * O registo usa a função Regista_Visita, que recusa a visita se o destino já
 * tiver atingido a capacidade de visitantes nessa data.
 */
public class TuristaDestinoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT td.ID_DESTINO, td.ID_TURISTA, td.DATA_VISITA, d.NOME_DESTINO, t.NOME_TURISTA "
                    + "FROM TURISTA_DESTINO td "
                    + "JOIN DESTINO d ON d.ID_DESTINO = td.ID_DESTINO "
                    + "JOIN TURISTA t ON t.ID_TURISTA = td.ID_TURISTA";
    private static final String ORDER = " ORDER BY td.DATA_VISITA DESC, td.ID_DESTINO, td.ID_TURISTA";

    public TuristaDestinoDAO() {
    }

    public TuristaDestinoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<TuristaDestino> getAllTuristaDestinos() throws SQLException {
        return query(SELECT + ORDER, TuristaDestinoDAO::map);
    }

    public List<TuristaDestino> getTuristaDestinosByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE td.ID_TURISTA = ?" + ORDER, TuristaDestinoDAO::map, idTurista);
    }

    public List<TuristaDestino> getTuristaDestinosByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE td.ID_DESTINO = ?" + ORDER, TuristaDestinoDAO::map, idDestino);
    }

    public TuristaDestino getTuristaDestino(String idDestino, String idTurista, LocalDate dataVisita) throws SQLException {
        return queryOne(SELECT + " WHERE td.ID_DESTINO = ? AND td.ID_TURISTA = ? AND td.DATA_VISITA = ?",
                TuristaDestinoDAO::map, idDestino, idTurista, dataVisita);
    }

    public String addTuristaDestino(TuristaDestino td) throws SQLException {
        try (Connection c = connections.get()) {
            return requireSuccess(callStringFunction(c, "Regista_Visita",
                    td.getIdDestino(), td.getIdTurista(), td.getDataVisita()), "Visita registada");
        }
    }

    public boolean deleteTuristaDestino(String idDestino, String idTurista, LocalDate dataVisita) throws SQLException {
        return update("DELETE FROM TURISTA_DESTINO WHERE ID_DESTINO = ? AND ID_TURISTA = ? AND DATA_VISITA = ?",
                idDestino, idTurista, dataVisita) > 0;
    }

    private static TuristaDestino map(ResultSet rs) throws SQLException {
        TuristaDestino td = new TuristaDestino(
                rs.getString("ID_DESTINO"),
                rs.getString("ID_TURISTA"),
                getDate(rs, "DATA_VISITA"));
        td.setNomeDestino(rs.getString("NOME_DESTINO"));
        td.setNomeTurista(rs.getString("NOME_TURISTA"));
        return td;
    }
}
