package com.turismo.api.dao;

import com.turismo.api.model.TuristaDestino;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for TuristaDestino entity (chave composta destino + turista + data)
 *
 * O registo usa a procedure registrar_visita, que chama a função Regista_Visita:
 * esta recusa a visita se o destino já tiver atingido a capacidade nessa data.
 */
public class TuristaDestinoDAO extends BaseDAO {

    /** Leitura só da view v_lista_visitas (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_VISITAS";
    private static final String ORDER = " ORDER BY DATA_VISITA DESC, ID_DESTINO, ID_TURISTA";

    public TuristaDestinoDAO() {
    }

    public TuristaDestinoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<TuristaDestino> getAllTuristaDestinos() throws SQLException {
        return query(SELECT + ORDER, TuristaDestinoDAO::map);
    }

    public List<TuristaDestino> getTuristaDestinosByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE ID_TURISTA = ?" + ORDER, TuristaDestinoDAO::map, idTurista);
    }

    public List<TuristaDestino> getTuristaDestinosByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE ID_DESTINO = ?" + ORDER, TuristaDestinoDAO::map, idDestino);
    }

    public TuristaDestino getTuristaDestino(String idDestino, String idTurista, LocalDate dataVisita) throws SQLException {
        return queryOne(SELECT + " WHERE ID_DESTINO = ? AND ID_TURISTA = ? AND DATA_VISITA = ?",
                TuristaDestinoDAO::map, idDestino, idTurista, dataVisita);
    }

    public String addTuristaDestino(TuristaDestino td) throws SQLException {
        return call("registrar_visita", td.getIdDestino(), td.getIdTurista(), td.getDataVisita());
    }

    /** Usa a procedure remover_visita. */
    public boolean deleteTuristaDestino(String idDestino, String idTurista, LocalDate dataVisita) throws SQLException {
        return callIfFound("remover_visita", idDestino, idTurista, dataVisita);
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
