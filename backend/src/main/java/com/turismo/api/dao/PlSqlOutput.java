package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import com.turismo.api.util.DbErrors;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Para alterar ou eliminar: devolve false quando a procedure indica "nao foi encontrado(a)". */
    public static boolean callProcedureIfFound(Connection c, String procedure, Object... params) throws SQLException {
        try {
            callProcedure(c, procedure, params);
            return true;
        } catch (ApiException e) {
            if (e.getStatus() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Para as procedures registrar_* que geram o identificador com a sequência:
     * devolve o identificador indicado na mensagem de sucesso. Se o número já
     * estiver ocupado (ORA-00001, ex.: dados inseridos à mão), volta a chamar a
     * procedure, que pede o seguinte à sequência.
     */
    public static String callProcedureForNewId(Connection c, IdGenerator.Sequence seq, String procedure,
                                               Object... params) throws SQLException {
        ApiException last = null;
        for (int attempt = 0; attempt < IdGenerator.MAX_ATTEMPTS; attempt++) {
            try {
                return newId(seq, procedure, callProcedure(c, procedure, params));
            } catch (ApiException e) {
                if (!"CHAVE_DUPLICADA".equals(e.getCode())) {
                    throw e;
                }
                last = e;
            }
        }
        throw last;
    }

    static String newId(IdGenerator.Sequence seq, String procedure, String message) {
        Matcher m = Pattern.compile(Pattern.quote(seq.prefix) + "\\d{3}").matcher(message);
        if (!m.find()) {
            throw new ApiException(500, "SEM_RESPOSTA_PLSQL",
                    "A procedure " + procedure + " não indicou o identificador criado.");
        }
        return m.group();
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
            if (lower.contains("ora-")) {
                throw DbErrors.fromMessage(text);
            }
            // Mensagem de uma função chamada pela procedure (ex.: registrar_visita
            // -> Regista_Visita): classifica-se o texto que vem depois de "Erro ...: ".
            int sep = text.indexOf(": ");
            throw BaseDAO.functionError(sep < 0 ? text : text.substring(sep + 2));
        }
        if (lower.contains("nao foi encontrad")) {
            throw ApiException.notFound(text);
        }
        return text;
    }
}
