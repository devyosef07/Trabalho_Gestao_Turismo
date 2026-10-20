package com.turismo.api.dao;

import com.turismo.api.model.Reserva;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Reserva entity
 *
 * A criação usa SQL direto numa transação (reserva + alojamentos escolhidos):
 * a procedure registrar_reserva está INVALID e associar_alojamento_reserva faz
 * COMMIT sozinha, o que impediria criar tudo de forma atómica.
 */
public class ReservaDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT r.ID_RESERVA, r.DATA_CHEGADA, r.DATA_PARTIDA, r.STATUS_RESERVA, r.ID_TURISTA, t.NOME_TURISTA, "
                    + "(r.DATA_PARTIDA - r.DATA_CHEGADA) AS NOITES, "
                    + "Total_Pago_Reserva(r.ID_RESERVA) AS TOTAL_PAGO, "
                    + "Reserva_Completa(r.ID_RESERVA) AS ESTADO_COMPLETUDE "
                    + "FROM RESERVA r JOIN TURISTA t ON t.ID_TURISTA = r.ID_TURISTA";

    public ReservaDAO() {
    }

    public ReservaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Reserva> getAllReservas() throws SQLException {
        return query(SELECT + " ORDER BY r.ID_RESERVA", ReservaDAO::map);
    }

    public Reserva getReservaById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE r.ID_RESERVA = ?", ReservaDAO::map, id);
    }

    public List<Reserva> getReservasByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE r.ID_TURISTA = ? ORDER BY r.ID_RESERVA", ReservaDAO::map, idTurista);
    }

    /** Cria a reserva e associa os alojamentos indicados, tudo ou nada. */
    public String addReserva(Reserva reserva, List<String> idsAlojamento) throws SQLException {
        return inTransaction(c -> {
            String id = IdGenerator.insertWithNewId(c, IdGenerator.Sequence.RESERVA, newId ->
                    update(c, "INSERT INTO RESERVA (ID_RESERVA, ID_TURISTA, DATA_CHEGADA, DATA_PARTIDA, STATUS_RESERVA) "
                                    + "VALUES (?, ?, ?, ?, ?)",
                            newId, reserva.getIdTurista(), reserva.getDataChegada(), reserva.getDataPartida(),
                            reserva.getStatusReserva()));
            for (String idAlojamento : idsAlojamento) {
                update(c, "INSERT INTO RESERVA_ALOJAMENTO (ID_RESERVA, ID_ALOJAMENTO, AVALIACAO_ALOJAMENTO) VALUES (?, ?, NULL)",
                        id, idAlojamento);
            }
            return id;
        });
    }

    /** Uma só instrução, para as datas e o estado mudarem juntos. */
    public boolean updateReserva(Reserva reserva) throws SQLException {
        return update("UPDATE RESERVA SET ID_TURISTA = ?, DATA_CHEGADA = ?, DATA_PARTIDA = ?, STATUS_RESERVA = ? "
                        + "WHERE ID_RESERVA = ?",
                reserva.getIdTurista(), reserva.getDataChegada(), reserva.getDataPartida(), reserva.getStatusReserva(),
                reserva.getIdReserva()) > 0;
    }

    /** Usa a procedure cancelar_reserva. */
    public String cancelarReserva(String id) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "cancelar_reserva", id);
        }
    }

    /** O trigger deleteReserva apaga os pagamentos e alojamentos da reserva. */
    public boolean deleteReserva(String id) throws SQLException {
        return update("DELETE FROM RESERVA WHERE ID_RESERVA = ?", id) > 0;
    }

    private static Reserva map(ResultSet rs) throws SQLException {
        Reserva r = new Reserva(
                rs.getString("ID_RESERVA"),
                getDate(rs, "DATA_CHEGADA"),
                getDate(rs, "DATA_PARTIDA"),
                rs.getString("STATUS_RESERVA"),
                rs.getString("ID_TURISTA"));
        r.setNomeTurista(rs.getString("NOME_TURISTA"));
        r.setNoites(getInteger(rs, "NOITES"));
        r.setTotalPago(rs.getBigDecimal("TOTAL_PAGO"));
        r.setEstadoCompletude(rs.getString("ESTADO_COMPLETUDE"));
        return r;
    }
}
