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

    /** Leitura só da view v_lista_destinos (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_DESTINOS";

    public DestinoDAO() {
    }

    public DestinoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Destino> getAllDestinos() throws SQLException {
        return query(SELECT + " ORDER BY ID_DESTINO", DestinoDAO::map);
    }

    public Destino getDestinoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_DESTINO = ?", DestinoDAO::map, id);
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

    /**
     * Registos que o trigger deleteDestino apaga em cascata (para avisar antes de apagar),
     * lidos da view v_lista_destinos. Um destino inexistente não tem dependentes.
     */
    public Map<String, Integer> getDependencias(String id) throws SQLException {
        Map<String, Integer> result = queryOne(
                "SELECT TOTAL_ATRACOES, TOTAL_ALOJAMENTOS, TOTAL_VISITAS FROM V_LISTA_DESTINOS WHERE ID_DESTINO = ?", rs -> {
                    Map<String, Integer> r = new LinkedHashMap<>();
                    r.put("atracoes", rs.getInt("TOTAL_ATRACOES"));
                    r.put("alojamentos", rs.getInt("TOTAL_ALOJAMENTOS"));
                    r.put("visitas", rs.getInt("TOTAL_VISITAS"));
                    return r;
                }, id);
        if (result == null) {
            result = new LinkedHashMap<>();
            result.put("atracoes", 0);
            result.put("alojamentos", 0);
            result.put("visitas", 0);
        }
        return result;
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
