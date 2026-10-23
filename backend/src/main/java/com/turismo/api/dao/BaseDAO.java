package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import com.turismo.api.util.AppConfig;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.DbErrors;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Operações JDBC comuns a todos os DAOs.
 *
 * Os erros SQL são propagados (nunca são convertidos em listas vazias), para que
 * a interface consiga distinguir "sem registos" de "falha na base de dados".
 */
public abstract class BaseDAO {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected final ConnectionProvider connections;

    protected BaseDAO() {
        this(ConnectionProvider.DEFAULT);
    }

    protected BaseDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection c = connections.get()) {
            return query(c, sql, mapper, params);
        }
    }

    protected <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> result = new ArrayList<>();
        try (PreparedStatement ps = prepare(c, sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        }
        return result;
    }

    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * As escritas são todas feitas por procedures (procedures.sql): a
     * aplicação nunca escreve diretamente nas tabelas.
     * Chama a procedure e devolve a mensagem de sucesso.
     */
    protected String call(String procedure, Object... params) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedure(c, procedure, params);
        }
    }

    /** Para as procedures registrar_*: devolve o código gerado (ex.: DT-004). */
    protected String callForNewId(IdGenerator.Sequence seq, String procedure, Object... params) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedureForNewId(c, seq, procedure, params);
        }
    }

    /** Para as procedures de alteração e eliminação: false se o registo não existir. */
    protected boolean callIfFound(String procedure, Object... params) throws SQLException {
        try (Connection c = connections.get()) {
            return PlSqlOutput.callProcedureIfFound(c, procedure, params);
        }
    }

    /**
     * Desfaz um registo já gravado (as procedures fazem COMMIT) quando o passo
     * seguinte da mesma operação falha; um erro aqui fica anexado ao original.
     */
    protected static void undo(Connection c, Exception original, String procedure, Object... params) {
        try {
            PlSqlOutput.callProcedure(c, procedure, params);
        } catch (SQLException | RuntimeException cleanup) {
            original.addSuppressed(cleanup);
        }
    }

    /** Chama uma função PL/SQL que devolve VARCHAR2. */
    protected String callStringFunction(Connection c, String function, Object... params) throws SQLException {
        StringBuilder sql = new StringBuilder("{? = call ").append(function).append("(");
        for (int i = 0; i < params.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")}");
        try (CallableStatement cs = c.prepareCall(sql.toString())) {
            cs.setQueryTimeout(queryTimeout());
            cs.registerOutParameter(1, Types.VARCHAR);
            for (int i = 0; i < params.length; i++) {
                setParam(cs, i + 2, params[i]);
            }
            cs.execute();
            return cs.getString(1);
        }
    }

    /**
     * As funções do projeto (Insere_Turista, Regista_Visita...) devolvem uma mensagem
     * em vez de lançar exceções. Aceita a mensagem de sucesso ou converte-a num erro.
     */
    protected static String requireSuccess(String result, String successPrefix) {
        if (result != null && result.startsWith(successPrefix)) {
            return result;
        }
        throw functionError(result);
    }

    static ApiException functionError(String result) {
        if (result == null) {
            return new ApiException(500, "SEM_RESPOSTA_PLSQL", "A função da base de dados não devolveu resultado.");
        }
        String lower = result.toLowerCase();
        if (lower.contains("ora-")) {
            return DbErrors.fromMessage(result);
        }
        if (lower.contains("inexistente")) {
            return ApiException.notFound(result);
        }
        if (lower.contains("ja existe") || lower.contains("ja tem") || lower.contains("nao e possivel")
                || lower.contains("ja nao esta")) {
            return ApiException.conflict(result);
        }
        return ApiException.rule(result);
    }

    protected PreparedStatement prepare(Connection c, String sql) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setQueryTimeout(queryTimeout());
        return ps;
    }

    static int queryTimeout() {
        return AppConfig.get().getInt("db.queryTimeoutSeconds", 30);
    }

    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            setParam(ps, i + 1, params[i]);
        }
    }

    static void setParam(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else if (value instanceof LocalDate) {
            ps.setDate(index, java.sql.Date.valueOf((LocalDate) value));
        } else if (value instanceof BigDecimal) {
            ps.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else {
            ps.setString(index, value.toString());
        }
    }

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime().toLocalDate();
    }

    protected static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    protected static String getTrimmed(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : value.trim();
    }
}
