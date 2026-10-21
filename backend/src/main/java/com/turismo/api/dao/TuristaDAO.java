package com.turismo.api.dao;

import com.turismo.api.model.Turista;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data Access Object for Turista entity
 *
 * Inserir e eliminar usam as funções Insere_Turista e Elimina_Turista da BD,
 * que geram o código pela sequência e impedem apagar turistas com reservas.
 */
public class TuristaDAO extends BaseDAO {

    /** Leitura só da view v_lista_turistas (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_TURISTAS";
    private static final Pattern CODIGO = Pattern.compile("([A-Z]{2}-\\d{3})");

    public TuristaDAO() {
    }

    public TuristaDAO(ConnectionProvider connections) {
        super(connections);
    }

    /**
     * Gets all turistas from the database
     * @return List of Turista objects
     */
    public List<Turista> getAllTuristas() throws SQLException {
        return query(SELECT + " ORDER BY ID_TURISTA", TuristaDAO::map);
    }

    /**
     * Gets a turista by ID
     * @param id the turista ID
     * @return Turista object or null if not found
     */
    public Turista getTuristaById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_TURISTA = ?", TuristaDAO::map, id);
    }

    /**
     * Adds a new turista through the Insere_Turista function
     * @return the generated ID (ex.: TR-006)
     */
    public String addTurista(Turista turista) throws SQLException {
        try (Connection c = connections.get()) {
            String result = callStringFunction(c, "Insere_Turista", turista.getNomeTurista(), turista.getNacionalidade());
            requireSuccess(result, "Turista inserido");
            Matcher m = CODIGO.matcher(result);
            if (!m.find()) {
                throw new ApiException(500, "SEM_CODIGO", "A função Insere_Turista não devolveu o código: " + result);
            }
            return m.group(1);
        }
    }

    public boolean updateTurista(Turista turista) throws SQLException {
        return update("UPDATE TURISTA SET NOME_TURISTA = ?, NACIONALIDADE = ? WHERE ID_TURISTA = ?",
                turista.getNomeTurista(), turista.getNacionalidade(), turista.getIdTurista()) > 0;
    }

    /**
     * Deletes a turista through the Elimina_Turista function (recusa se tiver reservas)
     * @return the message returned by the function
     */
    public String deleteTurista(String id) throws SQLException {
        try (Connection c = connections.get()) {
            return requireSuccess(callStringFunction(c, "Elimina_Turista", id), "Turista eliminado");
        }
    }

    private static Turista map(ResultSet rs) throws SQLException {
        Turista t = new Turista(
                rs.getString("ID_TURISTA"),
                rs.getString("NOME_TURISTA"),
                rs.getString("NACIONALIDADE"));
        t.setTotalReservas(rs.getInt("TOTAL_RESERVAS"));
        return t;
    }
}
