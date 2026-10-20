package com.turismo.api.dao;

import com.turismo.api.model.Destino;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Destino entity
 */
public class DestinoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT d.ID_DESTINO, d.NOME_DESTINO, d.REGIAO, d.CAPACIDADE_VISITANTES, "
                    + "(SELECT COUNT(*) FROM ATRACAO a WHERE a.ID_DESTINO = d.ID_DESTINO) AS TOTAL_ATRACOES, "
                    + "(SELECT COUNT(*) FROM ALOJAMENTO a WHERE a.ID_DESTINO = d.ID_DESTINO) AS TOTAL_ALOJAMENTOS "
                    + "FROM DESTINO d";

    public DestinoDAO() {
    }

    public DestinoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Destino> getAllDestinos() throws SQLException {
        return query(SELECT + " ORDER BY d.ID_DESTINO", DestinoDAO::map);
    }

    public Destino getDestinoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE d.ID_DESTINO = ?", DestinoDAO::map, id);
    }

    /** Insere o destino e devolve o identificador gerado pela sequência ID_destino. */
    public String addDestino(Destino destino) throws SQLException {
        return inTransaction(c -> IdGenerator.insertWithNewId(c, IdGenerator.Sequence.DESTINO, id ->
                update(c, "INSERT INTO DESTINO (ID_DESTINO, NOME_DESTINO, REGIAO, CAPACIDADE_VISITANTES) VALUES (?, ?, ?, ?)",
                        id, destino.getNomeDestino(), destino.getRegiao(), destino.getCapacidadeVisitantes())));
    }

    public boolean updateDestino(Destino destino) throws SQLException {
        return update("UPDATE DESTINO SET NOME_DESTINO = ?, REGIAO = ?, CAPACIDADE_VISITANTES = ? WHERE ID_DESTINO = ?",
                destino.getNomeDestino(), destino.getRegiao(), destino.getCapacidadeVisitantes(), destino.getIdDestino()) > 0;
    }

    /** O trigger deleteDestino apaga também as atrações, alojamentos e visitas do destino. */
    public boolean deleteDestino(String id) throws SQLException {
        return update("DELETE FROM DESTINO WHERE ID_DESTINO = ?", id) > 0;
    }

    /** Registos que o trigger deleteDestino apaga em cascata (para avisar antes de apagar). */
    public Map<String, Integer> getDependencias(String id) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM ATRACAO WHERE ID_DESTINO = ?) AS ATRACOES, "
                + "(SELECT COUNT(*) FROM ALOJAMENTO WHERE ID_DESTINO = ?) AS ALOJAMENTOS, "
                + "(SELECT COUNT(*) FROM TURISTA_DESTINO WHERE ID_DESTINO = ?) AS VISITAS FROM dual";
        return queryOne(sql, rs -> {
            Map<String, Integer> result = new LinkedHashMap<>();
            result.put("atracoes", rs.getInt("ATRACOES"));
            result.put("alojamentos", rs.getInt("ALOJAMENTOS"));
            result.put("visitas", rs.getInt("VISITAS"));
            return result;
        }, id, id, id);
    }

    /** Texto das funções Retorna_Info_Destino e Lista_Atracoes_Destino. */
    public Map<String, String> getInfo(String id) throws SQLException {
        try (Connection c = connections.get()) {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("info", callStringFunction(c, "Retorna_Info_Destino", id));
            result.put("atracoes", callStringFunction(c, "Lista_Atracoes_Destino", id));
            return result;
        }
    }

    private static Destino map(ResultSet rs) throws SQLException {
        Destino d = new Destino(
                rs.getString("ID_DESTINO"),
                rs.getString("NOME_DESTINO"),
                rs.getString("REGIAO"),
                getInteger(rs, "CAPACIDADE_VISITANTES"));
        d.setTotalAtracoes(rs.getInt("TOTAL_ATRACOES"));
        d.setTotalAlojamentos(rs.getInt("TOTAL_ALOJAMENTOS"));
        return d;
    }
}
