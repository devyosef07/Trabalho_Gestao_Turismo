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
 * as procedures registrar_reserva e associar_alojamento_reserva fazem COMMIT
 * sozinhas, o que impediria criar tudo de forma atómica.
 */
public class ReservaDAO extends BaseDAO {

    /** Leitura só da view v_lista_reservas (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_RESERVAS";

    public ReservaDAO() {
    }

    public ReservaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Reserva> getAllReservas() throws SQLException {
        return query(SELECT + " ORDER BY ID_RESERVA", ReservaDAO::map);
    }

    public Reserva getReservaById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_RESERVA = ?", ReservaDAO::map, id);
    }

    public List<Reserva> getReservasByTurista(String idTurista) throws SQLException {
        return query(SELECT + " WHERE ID_TURISTA = ? ORDER BY ID_RESERVA", ReservaDAO::map, idTurista);
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

    /**
     * Turista e datas com UPDATE; o estado com a função Actualiza_Status_Reserva,
     * na mesma transação. A função faz COMMIT das duas alterações quando corre
     * bem e ROLLBACK de ambas quando falha, por isso mudam sempre juntas.
     */
    public boolean updateReserva(Reserva reserva) throws SQLException {
        return inTransaction(c -> {
            if (update(c, "UPDATE RESERVA SET ID_TURISTA = ?, DATA_CHEGADA = ?, DATA_PARTIDA = ? WHERE ID_RESERVA = ?",
                    reserva.getIdTurista(), reserva.getDataChegada(), reserva.getDataPartida(),
                    reserva.getIdReserva()) == 0) {
                return false;
            }
            requireSuccess(callStringFunction(c, "Actualiza_Status_Reserva", reserva.getIdReserva(),
                    reserva.getStatusReserva()), "Reserva " + reserva.getIdReserva() + " actualizada");
            return true;
        });
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
