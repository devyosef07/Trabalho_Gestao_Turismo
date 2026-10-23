package com.turismo.api.dao;

import com.turismo.api.model.Atracao;
import com.turismo.api.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for Atracao entity
 *
 * Escritas só por procedures: registrar_atracao + alterar_horario_atracao
 * (criação), atualizar_atracao e eliminar_atracao (procedures.sql).
 */
public class AtracaoDAO extends BaseDAO {

    /** Leitura só da view v_lista_atracoes (database/views.sql), nunca da tabela. */
    private static final String SELECT = "SELECT * FROM V_LISTA_ATRACOES";

    public AtracaoDAO() {
    }

    public AtracaoDAO(ConnectionProvider connections) {
        super(connections);
    }

    public List<Atracao> getAllAtracoes() throws SQLException {
        return query(SELECT + " ORDER BY ID_ATRACAO", AtracaoDAO::map);
    }

    public Atracao getAtracaoById(String id) throws SQLException {
        return queryOne(SELECT + " WHERE ID_ATRACAO = ?", AtracaoDAO::map, id);
    }

    public List<Atracao> getAtracoesByDestino(String idDestino) throws SQLException {
        return query(SELECT + " WHERE ID_DESTINO = ? ORDER BY ID_ATRACAO", AtracaoDAO::map, idDestino);
    }

    /**
     * Usa registrar_atracao e, se houver horário especial, alterar_horario_atracao.
     * Cada procedure faz COMMIT; se a segunda falhar, a atração acabada de criar
     * é eliminada (eliminar_atracao) para não ficar gravada sem o horário pedido.
     */
    public String addAtracao(Atracao a) throws SQLException {
        try (Connection c = connections.get()) {
            String id = PlSqlOutput.callProcedureForNewId(c, IdGenerator.Sequence.ATRACAO, "registrar_atracao",
                    a.getIdDestino(), a.getTipoAtracao(), a.getDescricao(), a.getHoraAbertura(), a.getHoraFecho());
            if (a.getHoraAberturaEspecial() != null) {
                try {
                    PlSqlOutput.callProcedure(c, "alterar_horario_atracao", id,
                            a.getHoraAberturaEspecial(), a.getHoraFechoEspecial(), a.getDetalhesHorarioEspecial());
                } catch (SQLException | RuntimeException e) {
                    undo(c, e, "eliminar_atracao", id);
                    throw e;
                }
            }
            return id;
        }
    }

    /** Usa a procedure atualizar_atracao, que grava os dados base e o horário especial num só UPDATE. */
    public boolean updateAtracao(Atracao a) throws SQLException {
        return callIfFound("atualizar_atracao", a.getIdAtracao(), a.getTipoAtracao(), a.getDescricao(),
                a.getHoraAbertura(), a.getHoraFecho(), a.getHoraAberturaEspecial(), a.getHoraFechoEspecial(),
                a.getDetalhesHorarioEspecial(), a.getIdDestino());
    }


    /** Usa a procedure eliminar_atracao. */
    public boolean deleteAtracao(String id) throws SQLException {
        return callIfFound("eliminar_atracao", id);
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
