package com.turismo.api.dao;

import com.turismo.api.model.TipoAlojamento;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for TipoAlojamento entity
 */
public class TipoAlojamentoDAO extends BaseDAO {

    /** Leitura só da view v_lista_tipos_alojamento (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_TIPOS_ALOJAMENTO";

    public TipoAlojamentoDAO() {
    }

    public TipoAlojamentoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<TipoAlojamento> getAllTipoAlojamentos() throws SQLException {
        return query(SELECT + " ORDER BY TIPO_ALOJAMENTO", TipoAlojamentoDAO::map);
    }

    public TipoAlojamento getTipoAlojamentoById(String tipo) throws SQLException {
        return queryOne(SELECT + " WHERE TIPO_ALOJAMENTO = ?", TipoAlojamentoDAO::map, tipo);
    }

    /** Usa a procedure definir_tipo_alojamento. */
    public String addTipoAlojamento(TipoAlojamento t) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, "definir_tipo_alojamento", t.getTipoAlojamento(), t.getCapAlojamento());
        }
    }

    /** A chave (nome do tipo) não muda: é referenciada pelos alojamentos. */
    public boolean updateTipoAlojamento(TipoAlojamento t) throws SQLException {
        return update("UPDATE TIPO_ALOJAMENTO SET CAP_ALOJAMENTO = ? WHERE TIPO_ALOJAMENTO = ?",
                t.getCapAlojamento(), t.getTipoAlojamento()) > 0;
    }

    public boolean deleteTipoAlojamento(String tipo) throws SQLException {
        return update("DELETE FROM TIPO_ALOJAMENTO WHERE TIPO_ALOJAMENTO = ?", tipo) > 0;
    }

    private static TipoAlojamento map(ResultSet rs) throws SQLException {
        TipoAlojamento t = new TipoAlojamento(rs.getString("TIPO_ALOJAMENTO"), getInteger(rs, "CAP_ALOJAMENTO"));
        t.setTotalAlojamentos(rs.getInt("TOTAL_ALOJAMENTOS"));
        return t;
    }
}
