package com.turismo.api.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Utility class for database connections.
 *
 * Os dados da ligação vêm de {@link AppConfig}; a palavra-passe nunca é escrita
 * em logs. As ligações são reutilizadas através de um pool pequeno (HikariCP):
 * o Oracle 10g XE tem um limite baixo de processos e abrir uma ligação nova
 * por pedido esgotava o listener (ORA-12516) quando vários pedidos chegavam
 * juntos (por exemplo, o painel e uma lista ao mesmo tempo).
 */
public class DatabaseConnection {

    private static volatile HikariDataSource pool;

    static {
        // O Oracle não reconhece o locale pt_MZ do sistema (ORA-12705),
        // por isso forçamos um locale suportado antes de qualquer ligação.
        Locale.setDefault(Locale.of("pt", "PT"));
        System.setProperty("oracle.jdbc.timezoneAsRegion", "false");
        System.setProperty("oracle.jdbc.REDWOOD_CODE", "false");
        System.setProperty("oracle.jdbc.defaultNChar", "true");
        System.setProperty("user.timezone", "UTC");
        System.setProperty("oracle.jdbc.disableDefineColumnType", "true");
    }

    /** Devolve uma ligação do pool; fechar a ligação devolve-a ao pool. */
    public static Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    private static HikariDataSource pool() throws SQLException {
        HikariDataSource result = pool;
        if (result == null) {
            synchronized (DatabaseConnection.class) {
                result = pool;
                if (result == null) {
                    result = createPool(AppConfig.get());
                    pool = result;
                }
            }
        }
        return result;
    }

    private static HikariDataSource createPool(AppConfig config) throws SQLException {
        String password = config.dbPassword();
        if (password == null || password.isEmpty()) {
            throw new SQLException(
                    "A variável TURISMO_DB_PASSWORD não está definida.", "08001", DbErrors.NO_PASSWORD
            );
        }
        if (config.dbUser() == null) {
            throw new SQLException("Configure db.user em config/turismo.properties.", "08001", DbErrors.NO_PASSWORD);
        }

        int connectTimeoutMs = config.getInt("db.connectTimeoutSeconds", 8) * 1000;
        int readTimeoutMs = config.getInt("db.queryTimeoutSeconds", 30) * 1000 + 5000;

        HikariConfig hc = new HikariConfig();
        hc.setPoolName("turismo-oracle");
        hc.setDriverClassName("oracle.jdbc.OracleDriver");
        hc.setJdbcUrl(config.jdbcUrl());
        hc.setUsername(config.dbUser());
        hc.setPassword(password);
        // Sem isto o Oracle 10g responde ORA-01882 (timezone region not found)
        hc.addDataSourceProperty("oracle.jdbc.timezoneAsRegion", "false");
        hc.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(connectTimeoutMs));
        hc.addDataSourceProperty("oracle.jdbc.ReadTimeout", String.valueOf(readTimeoutMs));
        hc.setMaximumPoolSize(config.getInt("db.poolSize", 4));
        hc.setMinimumIdle(0);
        hc.setConnectionTimeout(connectTimeoutMs + 2000L);
        hc.setValidationTimeout(3000);
        hc.setIdleTimeout(120_000);
        hc.setMaxLifetime(600_000);
        // Não falha o arranque se a VM estiver desligada: tenta ligar no primeiro pedido.
        hc.setInitializationFailTimeout(-1);
        hc.setAutoCommit(true);
        return new HikariDataSource(hc);
    }

    /** Fecha o pool (chamado quando o servidor termina). */
    public static void shutdown() {
        HikariDataSource p = pool;
        if (p != null) {
            p.close();
            pool = null;
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar a ligação: " + e.getMessage());
            }
        }
    }
}
