package com.turismo.api.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê as constraints CHECK do dicionário de dados do Oracle.
 *
 * Os valores permitidos das listas (regiões, estados, métodos de pagamento...)
 * vêm das constraints "coluna in (...)" da própria BD, para a aplicação não
 * manter uma cópia própria dessas listas.
 */
public final class ConstraintCatalog {

    private static final Pattern IN_LIST = Pattern.compile(
            "^\\s*([A-Za-z0-9_]+)\\s+in\\s*\\((.*)\\)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");

    private static final Map<String, String> DESCRIPTIONS = new ConcurrentHashMap<>();
    private static volatile Map<String, List<String>> domains;

    private ConstraintCatalog() {
    }

    /**
     * Valores permitidos por coluna, com chave "TABELA.COLUNA" em maiúsculas.
     */
    public static Map<String, List<String>> domains(ConnectionProvider provider) throws SQLException {
        Map<String, List<String>> result = domains;
        if (result != null) {
            return result;
        }
        Map<String, List<String>> loaded = new LinkedHashMap<>();
        String sql = "SELECT table_name, search_condition FROM user_constraints "
                + "WHERE constraint_type = 'C' ORDER BY table_name";
        try (Connection c = provider.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1);
                String condition = rs.getString(2);
                Map.Entry<String, List<String>> entry = parseInList(condition);
                if (entry != null) {
                    loaded.put(table + "." + entry.getKey(), entry.getValue());
                }
            }
        }
        domains = Collections.unmodifiableMap(loaded);
        return domains;
    }

    public static List<String> domain(ConnectionProvider provider, String table, String column) throws SQLException {
        List<String> values = domains(provider).get(table.toUpperCase() + "." + column.toUpperCase());
        return values == null ? Collections.emptyList() : values;
    }

    /** Interpreta "coluna in ('A', 'B')"; devolve null se a condição tiver outra forma. */
    static Map.Entry<String, List<String>> parseInList(String condition) {
        if (condition == null) {
            return null;
        }
        Matcher m = IN_LIST.matcher(condition);
        if (!m.matches()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        Matcher q = QUOTED.matcher(m.group(2));
        while (q.find()) {
            values.add(q.group(1));
        }
        if (values.isEmpty()) {
            return null;
        }
        return Map.entry(m.group(1).toUpperCase(), Collections.unmodifiableList(values));
    }

    /** Texto da regra de uma constraint (ex.: "capacidade_visitantes > 0"). */
    static String describe(String constraintName) {
        if (constraintName == null) {
            return "regra desconhecida";
        }
        String cached = DESCRIPTIONS.get(constraintName);
        if (cached != null) {
            return cached;
        }
        String sql = "SELECT search_condition FROM user_constraints WHERE constraint_name = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, constraintName);
            try (ResultSet rs = ps.executeQuery()) {
                // search_condition é LONG: o valor só pode ser lido uma vez.
                String raw = rs.next() ? rs.getString(1) : null;
                if (raw != null) {
                    String text = raw.replaceAll("\\s+", " ").trim();
                    DESCRIPTIONS.put(constraintName, text);
                    return text;
                }
            }
        } catch (SQLException e) {
            // A descrição é só informativa; o erro original continua a ser devolvido.
            System.err.println("Não foi possível ler a constraint " + constraintName + ": " + e.getMessage());
        }
        return constraintName;
    }
}
