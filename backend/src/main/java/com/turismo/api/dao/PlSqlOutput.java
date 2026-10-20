package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import com.turismo.api.util.DbErrors;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Chama as procedures do projeto, que não lançam exceções: apanham o erro,
 * fazem ROLLBACK e escrevem-no com DBMS_OUTPUT. Para não perder essas falhas,
 * a saída do DBMS_OUTPUT é lida e interpretada depois de cada chamada.
 */
public final class PlSqlOutput {

    private PlSqlOutput() {
    }

    /** Executa a procedure e devolve a mensagem de sucesso; lança ApiException em caso de erro. */
    public static String callProcedure(Connection c, String procedure, Object... params) throws SQLException {
        List<String> lines = execute(c, procedure, params);
        return interpret(procedure, lines);
    }

    static List<String> execute(Connection c, String procedure, Object... params) throws SQLException {
        try (CallableStatement enable = c.prepareCall("{call DBMS_OUTPUT.ENABLE(1000000)}")) {
            enable.execute();
        }
        StringBuilder sql = new StringBuilder("{call ").append(procedure).append("(");
        for (int i = 0; i < params.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")}");
        try (CallableStatement cs = c.prepareCall(sql.toString())) {
            cs.setQueryTimeout(BaseDAO.queryTimeout());
            for (int i = 0; i < params.length; i++) {
                BaseDAO.setParam(cs, i + 1, params[i]);
            }
            cs.execute();
        }
        List<String> lines = new ArrayList<>();
        try (CallableStatement get = c.prepareCall("{call DBMS_OUTPUT.GET_LINE(?, ?)}")) {
            while (true) {
                get.registerOutParameter(1, Types.VARCHAR);
                get.registerOutParameter(2, Types.INTEGER);
                get.execute();
                if (get.getInt(2) != 0) {
                    break;
                }
                String line = get.getString(1);
                if (line != null && !line.isBlank()) {
                    lines.add(line.trim());
                }
            }
        }
        try (CallableStatement disable = c.prepareCall("{call DBMS_OUTPUT.DISABLE}")) {
            disable.execute();
        }
        return lines;
    }

    /** Decide o resultado a partir das mensagens escritas pela procedure. */
    static String interpret(String procedure, List<String> lines) {
        if (lines.isEmpty()) {
            throw new ApiException(500, "SEM_RESPOSTA_PLSQL",
                    "A procedure " + procedure + " não indicou o resultado da operação.");
        }
        String text = String.join(" ", lines);
        String lower = text.toLowerCase();
        if (lower.startsWith("erro")) {
            throw DbErrors.fromMessage(text);
        }
        if (lower.contains("nao foi encontrad")) {
            throw ApiException.notFound(text);
        }
        return text;
    }
}
