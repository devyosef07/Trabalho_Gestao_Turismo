package com.turismo.api.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gera identificadores no formato usado em Inserts.sql: prefixo + número da
 * sequência com 3 dígitos (ex.: 'DT-' || LPAD(ID_destino.NEXTVAL, 3, '0')).
 */
public final class IdGenerator {

    /** Sequências existentes na BD e respetivos prefixos. */
    public enum Sequence {
        DESTINO("ID_destino", "DT-"),
        ATRACAO("ID_atracao", "AT-"),
        TURISTA("ID_turista", "TR-"),
        RESERVA("ID_reserva", "RS-"),
        OPERADOR("ID_operador", "OP-"),
        ALOJAMENTO("ID_alojamento", "AL-"),
        TRANSPORTE("ID_transporte", "TP-"),
        PAGAMENTO("ID_pagamento", "PG-");

        final String sequenceName;
        final String prefix;

        Sequence(String sequenceName, String prefix) {
            this.sequenceName = sequenceName;
            this.prefix = prefix;
        }
    }

    @FunctionalInterface
    public interface Insert {
        void run(String id) throws SQLException;
    }

    /** Tentativas quando o número gerado já está ocupado (ex.: dados inseridos à mão). */
    static final int MAX_ATTEMPTS = 20;

    private IdGenerator() {
    }

    static String sql(Sequence seq) {
        return "SELECT '" + seq.prefix + "' || LPAD(" + seq.sequenceName + ".NEXTVAL, 3, '0') FROM dual";
    }

    public static String next(Connection c, Sequence seq) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql(seq));
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    /**
     * Insere com um novo identificador; se esse identificador já existir
     * (ORA-00001), pede o seguinte à sequência.
     */
    public static String insertWithNewId(Connection c, Sequence seq, Insert insert) throws SQLException {
        SQLException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String id = next(c, seq);
            try {
                insert.run(id);
                return id;
            } catch (SQLException e) {
                if (e.getErrorCode() != 1) {
                    throw e;
                }
                last = e;
            }
        }
        throw last;
    }
}
