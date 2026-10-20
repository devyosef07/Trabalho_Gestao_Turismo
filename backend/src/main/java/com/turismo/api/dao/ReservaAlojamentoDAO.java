package com.turismo.api.dao;

import com.turismo.api.model.ReservaAlojamento;
import com.turismo.api.util.ConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for ReservaAlojamento entity (chave composta reserva + alojamento)
 */
public class ReservaAlojamentoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT ra.ID_RESERVA, ra.ID_ALOJAMENTO, ra.AVALIACAO_ALOJAMENTO, a.NOME_ALOJAMENTO, t.NOME_TURISTA "
                    + "FROM RESERVA_ALOJAMENTO ra "
                    + "JOIN ALOJAMENTO a ON a.ID_ALOJAMENTO = ra.ID_ALOJAMENTO "
                    + "JOIN RESERVA r ON r.ID_RESERVA = ra.ID_RESERVA "
                    + "JOIN TURISTA t ON t.ID_TURISTA = r.ID_TURISTA";
    private static final String ORDER = " ORDER BY ra.ID_RESERVA, ra.ID_ALOJAMENTO";

    public ReservaAlojamentoDAO() {
    }

    public ReservaAlojamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<ReservaAlojamento> getAllReservaAlojamentos() throws SQLException {
        return query(SELECT + ORDER, ReservaAlojamentoDAO::map);
    }

    public List<ReservaAlojamento> getReservaAlojamentosByReserva(String idReserva) throws SQLException {
        return query(SELECT + " WHERE ra.ID_RESERVA = ?" + ORDER, ReservaAlojamentoDAO::map, idReserva);
    }

    public List<ReservaAlojamento> getReservaAlojamentosByAlojamento(String idAlojamento) throws SQLException {
        return query(SELECT + " WHERE ra.ID_ALOJAMENTO = ?" + ORDER, ReservaAlojamentoDAO::map, idAlojamento);
    }

    public ReservaAlojamento getReservaAlojamento(String idReserva, String idAlojamento) throws SQLException {
        return queryOne(SELECT + " WHERE ra.ID_RESERVA = ? AND ra.ID_ALOJAMENTO = ?", ReservaAlojamentoDAO::map,
                idReserva, idAlojamento);
    }

    /** Usa a procedure associar_alojamento_reserva (a avaliação fica vazia). */
    public String addReservaAlojamento(ReservaAlojamento ra) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "associar_alojamento_reserva", ra.getIdReserva(), ra.getIdAlojamento());
        }
    }

    /** Usa a procedure avaliar_alojamento. */
    public String avaliar(String idReserva, String idAlojamento, BigDecimal avaliacao) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "avaliar_alojamento", idReserva, idAlojamento, avaliacao);
        }
    }

    public boolean deleteReservaAlojamento(String idReserva, String idAlojamento) throws SQLException {
        return update("DELETE FROM RESERVA_ALOJAMENTO WHERE ID_RESERVA = ? AND ID_ALOJAMENTO = ?", idReserva, idAlojamento) > 0;
    }

    private static ReservaAlojamento map(ResultSet rs) throws SQLException {
        ReservaAlojamento ra = new ReservaAlojamento(
                rs.getString("ID_RESERVA"),
                rs.getString("ID_ALOJAMENTO"),
                rs.getBigDecimal("AVALIACAO_ALOJAMENTO"));
        ra.setNomeAlojamento(rs.getString("NOME_ALOJAMENTO"));
        ra.setNomeTurista(rs.getString("NOME_TURISTA"));
        return ra;
    }
}
