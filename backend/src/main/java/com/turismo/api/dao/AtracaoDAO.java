package com.turismo.api.dao;

import com.turismo.api.model.Atracao;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Atracao entity
 *
 * As escritas usam SQL direto: a procedure registrar_atracao está INVALID na BD
 * e recebe as horas como DATE, quando as colunas são CHAR(5) "HH:MM".
 */
public class AtracaoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT a.ID_ATRACAO, a.TIPO_ATRACAO, a.DESCRICAO, a.HORA_ABERTURA, a.HORA_FECHO, "
                    + "a.HORA_ABERTURA_ESPECIAL, a.HORA_FECHO_ESPECIAL, a.DETALHES_HORARIO_ESPECIAL, a.ID_DESTINO, "
                    + "d.NOME_DESTINO FROM ATRACAO a JOIN DESTINO d ON d.ID_DESTINO = a.ID_DESTINO";

    public AtracaoDAO() {
    }

    public AtracaoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Atracao> getAllAtracoes() throws SQLException {
        return query(SELECT + " ORDER BY a.ID_ATRACAO", AtracaoDAO::map);
    }

    public Atracao getAtracaoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE a.ID_ATRACAO = ?", AtracaoDAO::map, id);
    }

    public List<Atracao> getAtracoesByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE a.ID_DESTINO = ? ORDER BY a.ID_ATRACAO", AtracaoDAO::map, idDestino);
    }

    public String addAtracao(Atracao a) throws SQLException {
        return inTransaction(c -> IdGenerator.insertWithNewId(c, IdGenerator.Sequence.ATRACAO, id ->
                update(c, "INSERT INTO ATRACAO (ID_ATRACAO, TIPO_ATRACAO, DESCRICAO, HORA_ABERTURA, HORA_FECHO, "
                                + "HORA_ABERTURA_ESPECIAL, HORA_FECHO_ESPECIAL, DETALHES_HORARIO_ESPECIAL, ID_DESTINO) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        id, a.getTipoAtracao(), a.getDescricao(), a.getHoraAbertura(), a.getHoraFecho(),
                        a.getHoraAberturaEspecial(), a.getHoraFechoEspecial(), a.getDetalhesHorarioEspecial(),
                        a.getIdDestino())));
    }

    public boolean updateAtracao(Atracao a) throws SQLException {
        return update("UPDATE ATRACAO SET TIPO_ATRACAO = ?, DESCRICAO = ?, HORA_ABERTURA = ?, HORA_FECHO = ?, "
                        + "HORA_ABERTURA_ESPECIAL = ?, HORA_FECHO_ESPECIAL = ?, DETALHES_HORARIO_ESPECIAL = ?, "
                        + "ID_DESTINO = ? WHERE ID_ATRACAO = ?",
                a.getTipoAtracao(), a.getDescricao(), a.getHoraAbertura(), a.getHoraFecho(),
                a.getHoraAberturaEspecial(), a.getHoraFechoEspecial(), a.getDetalhesHorarioEspecial(),
                a.getIdDestino(), a.getIdAtracao()) > 0;
    }

    public boolean deleteAtracao(String id) throws SQLException {
        return update("DELETE FROM ATRACAO WHERE ID_ATRACAO = ?", id) > 0;
    }

    private static Atracao map(ResultSet rs) throws SQLException {
        Atracao a = new Atracao(
                rs.getString("ID_ATRACAO"),
                rs.getString("TIPO_ATRACAO"),
                rs.getString("DESCRICAO"),
                getTrimmed(rs, "HORA_ABERTURA"),
                getTrimmed(rs, "HORA_FECHO"),
                getTrimmed(rs, "HORA_ABERTURA_ESPECIAL"),
                getTrimmed(rs, "HORA_FECHO_ESPECIAL"),
                rs.getString("DETALHES_HORARIO_ESPECIAL"),
                rs.getString("ID_DESTINO"));
        a.setNomeDestino(rs.getString("NOME_DESTINO"));
        return a;
    }
}
