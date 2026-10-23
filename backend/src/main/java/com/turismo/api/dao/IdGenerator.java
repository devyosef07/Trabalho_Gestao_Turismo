package com.turismo.api.dao;


/**
 * Prefixos dos identificadores gerados pelas procedures registrar_* com a
 * sequência, no formato de Inserts.sql (ex.: 'DT-' || LPAD(ID_destino.NEXTVAL, 3, '0')).
 * O código criado é lido da mensagem da procedure ({@link PlSqlOutput#callProcedureForNewId}).
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

    /** Tentativas quando o número gerado já está ocupado (ex.: dados inseridos à mão). */
    static final int MAX_ATTEMPTS = 20;

    private IdGenerator() {
    }
}
