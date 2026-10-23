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
 * Escritas só por procedures e funções: registrar_reserva +
 * associar_alojamento_reserva (criação), alterar_datas_reserva +
 * alterar_turista_reserva + Actualiza_Status_Reserva (alteração),
 * cancelar_reserva e eliminar_reserva.
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

    /**
     * Cria a reserva com registrar_reserva e associa cada alojamento com
     * associar_alojamento_reserva. Cada procedure faz COMMIT; se uma associação
     * falhar, a reserva é eliminada (eliminar_reserva; o trigger deleteReserva
     * apaga também os alojamentos já associados), para que fique tudo ou nada.
     */
    public String addReserva(Reserva reserva, List<String> idsAlojamento) throws SQLException {
        try (Connection c = connections.get()) {
            String id = PlSqlOutput.callProcedureForNewId(c, IdGenerator.Sequence.RESERVA, "registrar_reserva",
                    reserva.getIdTurista(), reserva.getDataChegada(), reserva.getDataPartida(),
                    reserva.getStatusReserva());
            try {
                for (String idAlojamento : idsAlojamento) {
                    PlSqlOutput.callProcedure(c, "associar_alojamento_reserva", id, idAlojamento);
                }
            } catch (SQLException | RuntimeException e) {
                undo(c, e, "eliminar_reserva", id);
                throw e;
            }
            return id;
        }
    }

    /**
     * Datas com alterar_datas_reserva, turista com alterar_turista_reserva e
     * estado com a função Actualiza_Status_Reserva. Cada uma faz COMMIT; o
     * handler já validou as datas, o turista e o estado antes de chamar.
     */
    public boolean updateReserva(Reserva reserva) throws SQLException {
        try (Connection c = connections.get()) {
            if (!PlSqlOutput.callProcedureIfFound(c, "alterar_datas_reserva", reserva.getIdReserva(),
                    reserva.getDataChegada(), reserva.getDataPartida())) {
                return false;
            }
            PlSqlOutput.callProcedure(c, "alterar_turista_reserva", reserva.getIdReserva(), reserva.getIdTurista());
            requireSuccess(callStringFunction(c, "Actualiza_Status_Reserva", reserva.getIdReserva(),
                    reserva.getStatusReserva()), "Reserva " + reserva.getIdReserva() + " actualizada");
            return true;
        }
    }

    /** Usa a procedure cancelar_reserva. */
    public String cancelarReserva(String id) throws SQLException {
        return call("cancelar_reserva", id);
    }

    /** Usa a procedure eliminar_reserva; o trigger deleteReserva apaga os pagamentos e alojamentos da reserva. */
    public boolean deleteReserva(String id) throws SQLException {
        return callIfFound("eliminar_reserva", id);
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
