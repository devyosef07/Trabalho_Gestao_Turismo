package com.turismo.api.integration;

import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.DatabaseConnection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ligação única para testes: os DAOs podem chamar commit() e close(), mas essas
 * chamadas são ignoradas e no fim tudo é desfeito com ROLLBACK.
 *
 * Atenção: só serve para SQL direto. Funções e procedures PL/SQL que fazem
 * COMMIT por dentro gravam de facto, por isso não são usadas com esta ligação.
 */
final class RollbackConnection implements AutoCloseable {

    private final Connection real;
    private final Connection proxy;

    RollbackConnection() throws SQLException {
        real = DatabaseConnection.getConnection();
        real.setAutoCommit(false);
        proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (p, method, args) -> {
                    switch (method.getName()) {
                        case "close":
                        case "commit":
                            return null;
                        case "setAutoCommit":
                            return null;
                        default:
                            try {
                                return method.invoke(real, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                    }
                });
    }

    ConnectionProvider provider() {
        return () -> proxy;
    }

    Connection connection() {
        return proxy;
    }

    @Override
    public void close() throws SQLException {
        try {
            real.rollback();
        } finally {
            real.close();
        }
    }
}
