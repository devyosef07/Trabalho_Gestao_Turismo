package com.turismo.api.dao;

import com.turismo.api.model.Conta;
import com.turismo.api.util.ConnectionProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contas de utilizador e sessões (database/contas.sql).
 *
 * Como no resto do projeto, lê só das views (v_conta_credenciais,
 * v_sessao_ativa) e escreve só pelas procedures. Os tokens de sessão nunca
 * chegam aqui: só o seu SHA-256.
 */
public class ContaDAO extends BaseDAO {

    /** Linha de v_conta_credenciais: o necessário para validar um início de sessão. */
    public record Credenciais(long idConta, String nomeUtilizador, String hashPalavraPasse, int minutosBloqueio) {
        public boolean bloqueada() {
            return minutosBloqueio > 0;
        }
    }

    /** Linha de v_sessao_ativa: a conta dona da sessão, quando começou e quando expira. */
    public record SessaoAtiva(Conta conta, String iniciadaEm, String expiraEm) {
    }

    private static final String CREDENCIAIS = "SELECT * FROM V_CONTA_CREDENCIAIS";
    private static final String SESSOES = "SELECT * FROM V_SESSAO_ATIVA";

    public ContaDAO() {
    }

    public ContaDAO(ConnectionProvider connections) {
        super(connections);
    }

    public Credenciais getCredenciais(String nomeUtilizador) throws SQLException {
        return queryOne(CREDENCIAIS + " WHERE NOME_UTILIZADOR = ?", rs -> new Credenciais(
                rs.getLong("ID_CONTA"), rs.getString("NOME_UTILIZADOR"), rs.getString("HASH_PALAVRA_PASSE"),
                rs.getInt("MINUTOS_BLOQUEIO")), nomeUtilizador);
    }

    /** Nomes de utilizador já usados que começam por {@code base} (para escolher um livre). */
    public List<String> nomesComBase(String base) throws SQLException {
        String like = base.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return query("SELECT NOME_UTILIZADOR FROM V_CONTA_CREDENCIAIS WHERE NOME_UTILIZADOR LIKE ? ESCAPE '\\'",
                rs -> rs.getString(1), like);
    }

    public SessaoAtiva getSessaoAtiva(String hashToken) throws SQLException {
        return queryOne(SESSOES + " WHERE HASH_TOKEN = ?", ContaDAO::mapSessao, hashToken);
    }

    /** Usa a procedure registrar_conta (o hash já vem calculado). */
    public String registrarConta(String nomeUtilizador, String nomeCompleto, LocalDate dataNascimento,
                                 String hashPalavraPasse) throws SQLException {
        return call("registrar_conta", nomeUtilizador, nomeCompleto, dataNascimento, hashPalavraPasse);
    }

    /** Usa a procedure registar_acesso_conta: limpa as falhas ou conta mais uma (e bloqueia à 5.ª). */
    public void registarAcesso(long idConta, boolean sucesso) throws SQLException {
        call("registar_acesso_conta", idConta, sucesso ? "S" : "N");
    }

    /** Usa a procedure iniciar_sessao. */
    public void iniciarSessao(String hashToken, long idConta, int minutos) throws SQLException {
        call("iniciar_sessao", hashToken, idConta, minutos);
    }

    /** Usa a procedure terminar_sessao: false se a sessão já não estava aberta. */
    public boolean terminarSessao(String hashToken) throws SQLException {
        return callIfFound("terminar_sessao", hashToken);
    }

    private static SessaoAtiva mapSessao(ResultSet rs) throws SQLException {
        Conta c = new Conta();
        c.setIdConta(rs.getLong("ID_CONTA"));
        c.setNomeUtilizador(rs.getString("NOME_UTILIZADOR"));
        c.setNomeCompleto(rs.getString("NOME_COMPLETO"));
        c.setDataNascimento(getDate(rs, "DATA_NASCIMENTO"));
        c.setDataCriacao(isoDateTime(rs, "DATA_CRIACAO"));
        return new SessaoAtiva(c, isoDateTime(rs, "DATA_INICIO"), isoDateTime(rs, "DATA_EXPIRACAO"));
    }

    private static String isoDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        if (ts == null) {
            return null;
        }
        LocalDateTime t = ts.toLocalDateTime().withNano(0);
        return t.toString();
    }
}
