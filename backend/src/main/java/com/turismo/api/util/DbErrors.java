package com.turismo.api.util;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz erros Oracle em {@link ApiException} com mensagens em português.
 * Nunca inclui credenciais nem detalhes da ligação na mensagem.
 */
public final class DbErrors {

    /** Código interno usado quando a palavra-passe não está configurada. */
    public static final int NO_PASSWORD = 99001;

    private static final Pattern ORA_CODE = Pattern.compile("ORA-(\\d{5})");
    private static final Pattern NAMED_OBJECT = Pattern.compile("'([A-Z0-9_$#]+\\.)?([A-Z0-9_$#]+)'");
    private static final Pattern CONSTRAINT = Pattern.compile("\\(([A-Z0-9_$#]+\\.)?([A-Z0-9_$#]+)\\)");
    private static final Pattern APP_ERROR = Pattern.compile("ORA-20\\d{3}:\\s*([^\\n]*)");

    private DbErrors() {
    }

    public static ApiException translate(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        int code = e.getErrorCode();
        if (code == 0) {
            code = oraCode(message);
        }

        if (code == NO_PASSWORD) {
            return new ApiException(503, "BD_SEM_CREDENCIAIS", message, e);
        }
        if (isConnectionProblem(e, code)) {
            return new ApiException(503, "BD_INDISPONIVEL",
                    "Sem ligação à base de dados Oracle. Verifique se a máquina virtual está ligada e acessível.", e);
        }
        if (e instanceof SQLTimeoutException || code == 1013) {
            return new ApiException(504, "BD_TEMPO_ESGOTADO",
                    "A base de dados demorou demasiado a responder.", e);
        }
        if (code >= 20000 && code <= 20999) {
            return new ApiException(422, "REGRA_BD", applicationMessage(message), e);
        }
        switch (code) {
            case 1:
                return new ApiException(409, "CHAVE_DUPLICADA",
                        "Já existe um registo com esta chave.", e);
            case 1017:
                return new ApiException(503, "BD_CREDENCIAIS_INVALIDAS",
                        "Utilizador ou palavra-passe da base de dados inválidos.", e);
            case 1400:
                return new ApiException(422, "CAMPO_OBRIGATORIO",
                        "Falta preencher um campo obrigatório.", e);
            case 1438:
                return new ApiException(422, "NUMERO_DEMASIADO_GRANDE",
                        "Um dos valores numéricos excede o tamanho permitido.", e);
            case 2290:
                return new ApiException(422, "REGRA_VIOLADA",
                        "O valor não cumpre a regra da base de dados: " + ConstraintCatalog.describe(constraintName(message)), e);
            case 2291:
                return new ApiException(422, "REFERENCIA_INEXISTENTE",
                        "O registo relacionado indicado não existe.", e);
            case 2292:
                return new ApiException(409, "TEM_DEPENDENTES",
                        "Não é possível apagar: existem registos que dependem deste.", e);
            case 4091:
                return new ApiException(500, "TABELA_MUTANTE",
                        "Um trigger da base de dados não permitiu a operação (" + objectName(message) + ").", e);
            case 4098:
                return new ApiException(503, "TRIGGER_INVALIDO",
                        "O trigger " + objectName(message) + " está inválido na base de dados e bloqueia esta operação. "
                                + "Aplique o script database/correcoes.sql.", e);
            case 942:
                return new ApiException(503, "OBJETO_INEXISTENTE",
                        "Uma tabela ou view necessária não existe na base de dados. "
                                + "Aplique o script database/correcoes.sql.", e);
            case 6575:
            case 6550:
                return new ApiException(503, "PLSQL_INVALIDO",
                        "Uma função ou procedure da base de dados está inválida. Aplique o script database/correcoes.sql.", e);
            case 8004:
                return new ApiException(500, "SEQUENCIA_ESGOTADA",
                        "A sequência de identificadores atingiu o valor máximo (999).", e);
            case 12899:
                return new ApiException(422, "TEXTO_DEMASIADO_LONGO",
                        "Um dos textos excede o tamanho máximo permitido.", e);
            case 1841:
            case 1843:
            case 1847:
            case 1861:
                return new ApiException(422, "DATA_INVALIDA", "Data inválida.", e);
            default:
                String suffix = code > 0 ? " (ORA-" + String.format("%05d", code) + ")" : "";
                return new ApiException(500, "ERRO_BD", "Erro na base de dados" + suffix + ".", e);
        }
    }

    /** Converte uma mensagem textual (ex.: devolvida por uma função PL/SQL) num erro. */
    public static ApiException fromMessage(String text) {
        int code = oraCode(text);
        if (code > 0) {
            return translate(new SQLException(text, null, code));
        }
        return ApiException.rule(text);
    }

    static boolean isConnectionProblem(SQLException e, int code) {
        if (e instanceof SQLRecoverableException || e instanceof java.sql.SQLTransientConnectionException) {
            return true;
        }
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {
            return true;
        }
        return code == 17002 || code == 17410 || code == 12170 || code == 12541 || code == 12514
                || code == 12505 || code == 12516 || code == 12519 || code == 12520 || code == 12528
                || code == 12537 || code == 3113 || code == 3114;
    }

    static int oraCode(String message) {
        if (message == null) {
            return 0;
        }
        Matcher m = ORA_CODE.matcher(message);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    static String applicationMessage(String message) {
        Matcher m = APP_ERROR.matcher(message);
        if (m.find()) {
            return m.group(1).trim();
        }
        return message.trim();
    }

    static String objectName(String message) {
        Matcher m = NAMED_OBJECT.matcher(message);
        return m.find() ? m.group(2) : "?";
    }

    static String constraintName(String message) {
        Matcher m = CONSTRAINT.matcher(message);
        return m.find() ? m.group(2) : null;
    }
}
