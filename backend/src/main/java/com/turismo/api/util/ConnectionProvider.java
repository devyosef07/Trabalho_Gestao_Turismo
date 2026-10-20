package com.turismo.api.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fonte de ligações à BD. Em produção é {@link DatabaseConnection#getConnection()};
 * os testes de integração usam uma ligação única em transação com ROLLBACK.
 */
@FunctionalInterface
public interface ConnectionProvider {

    ConnectionProvider DEFAULT = DatabaseConnection::getConnection;

    Connection get() throws SQLException;
}
