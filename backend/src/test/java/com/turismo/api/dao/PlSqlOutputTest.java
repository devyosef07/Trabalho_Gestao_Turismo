package com.turismo.api.dao;

import com.turismo.api.util.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlSqlOutputTest {

    @Test
    void mensagemDeSucessoEhDevolvida() {
        assertEquals("Operador  OP-001 associado ao turista TR-001 com sucesso!",
                PlSqlOutput.interpret("p", List.of("Operador  OP-001 associado ao turista TR-001 com sucesso!")));
    }

    @Test
    void erroComCodigoOracleEhTraduzido() {
        ApiException e = assertThrows(ApiException.class, () -> PlSqlOutput.interpret("p",
                List.of("Erro ao associar operador: ORA-00001: unique constraint (YOSEF.SYS_C007719) violated")));
        assertEquals("CHAVE_DUPLICADA", e.getCode());
    }

    @Test
    void naoEncontradoDa404() {
        ApiException e = assertThrows(ApiException.class, () -> PlSqlOutput.interpret("p",
                List.of("Reserva com o ID RS-999 nao foi encontrada.")));
        assertEquals(404, e.getStatus());
    }

    @Test
    void erroDeFuncaoChamadaPelaProcedureEhClassificado() {
        ApiException e = assertThrows(ApiException.class, () -> PlSqlOutput.interpret("registrar_visita",
                List.of("Erro ao registrar visita: Destino inexistente")));
        assertEquals(404, e.getStatus());
        assertEquals("Destino inexistente", e.getMessage());
        assertEquals(409, assertThrows(ApiException.class, () -> PlSqlOutput.interpret("registrar_visita",
                List.of("Erro ao registrar visita: Esse turista ja tem uma visita registada a este destino nessa data")))
                .getStatus());
    }

    @Test
    void identificadorCriadoEhLidoDaMensagem() {
        assertEquals("AT-012", PlSqlOutput.newId(IdGenerator.Sequence.ATRACAO, "registrar_atracao",
                "Atracao AT-012 registrada com sucesso!"));
        assertEquals("RS-007", PlSqlOutput.newId(IdGenerator.Sequence.RESERVA, "registrar_reserva",
                "Reserva RS-007 gerada com sucesso!"));
        assertEquals(500, assertThrows(ApiException.class, () -> PlSqlOutput.newId(IdGenerator.Sequence.PAGAMENTO,
                "registrar_pagamento", "Pagamento gerado com sucesso!")).getStatus());
    }

    @Test
    void semSaidaNaoEhTratadoComoSucesso() {
        assertEquals(500, assertThrows(ApiException.class, () -> PlSqlOutput.interpret("p", List.of())).getStatus());
    }

    @Test
    void mensagensDasFuncoes() {
        assertEquals("Visita registada", BaseDAO.requireSuccess("Visita registada", "Visita registada"));
        assertEquals(404, BaseDAO.functionError("Pagamento inexistente").getStatus());
        assertEquals(409, BaseDAO.functionError("Nao e possivel eliminar: o turista tem reservas").getStatus());
        assertEquals(409, BaseDAO.functionError("O pagamento ja nao esta pendente (estado: CONCLUIDO)").getStatus());
        assertEquals(422, BaseDAO.functionError("Destino lotado nessa data").getStatus());
        assertEquals("TRIGGER_INVALIDO", BaseDAO.functionError(
                "Erro desconhecido: -4098 - ORA-04098: trigger 'YOSEF.DELETERESERVA' is invalid and failed re-validation").getCode());
    }
}
