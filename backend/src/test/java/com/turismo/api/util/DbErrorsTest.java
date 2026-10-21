package com.turismo.api.util;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbErrorsTest {

    private static ApiException translate(int code, String message) {
        return DbErrors.translate(new SQLException(message, null, code));
    }

    @Test
    void chaveDuplicadaDa409() {
        ApiException e = translate(1, "ORA-00001: unique constraint (YOSEF.SYS_C007640) violated");
        assertEquals(409, e.getStatus());
        assertEquals("CHAVE_DUPLICADA", e.getCode());
    }

    @Test
    void referenciaInexistenteDa422() {
        assertEquals(422, translate(2291, "ORA-02291: integrity constraint violated - parent key not found").getStatus());
    }

    @Test
    void registosDependentesDa409() {
        ApiException e = translate(2292, "ORA-02292: integrity constraint violated - child record found");
        assertEquals(409, e.getStatus());
        assertEquals("TEM_DEPENDENTES", e.getCode());
    }

    @Test
    void triggerInvalidoIndicaONomeEOScriptDeCorrecao() {
        ApiException e = translate(4098, "ORA-04098: trigger 'YOSEF.VALIDARHORABERTURA' is invalid and failed re-validation");
        assertEquals(503, e.getStatus());
        assertEquals("TRIGGER_INVALIDO", e.getCode());
        assertTrue(e.getMessage().contains("VALIDARHORABERTURA"), e.getMessage());
        assertTrue(e.getMessage().contains("Triggers.sql"));
    }

    @Test
    void viewInexistenteDa503() {
        assertEquals("OBJETO_INEXISTENTE", translate(942, "ORA-00942: table or view does not exist").getCode());
    }

    @Test
    void erroDeAplicacaoDosTriggersMostraAMensagemDaBd() {
        ApiException e = translate(20008, "ORA-20008: ERRO: Data de chegada invalida!\nORA-06512: at \"YOSEF.VALIDARDATACHEGADA\", line 3");
        assertEquals(422, e.getStatus());
        assertEquals("ERRO: Data de chegada invalida!", e.getMessage());
    }

    @Test
    void falhaDeRedeEhBdIndisponivel() {
        ApiException e = DbErrors.translate(new SQLRecoverableException("IO Error: Network Adapter could not establish the connection"));
        assertEquals(503, e.getStatus());
        assertEquals("BD_INDISPONIVEL", e.getCode());
    }

    @Test
    void codigoExtraidoDaMensagemQuandoErrorCodeEhZero() {
        ApiException e = DbErrors.fromMessage("Erro desconhecido: -2291 - ORA-02291: integrity constraint (YOSEF.SYS_C1) violated");
        assertEquals("REFERENCIA_INEXISTENTE", e.getCode());
    }

    @Test
    void mensagemSemCodigoOracleEhRegraDeNegocio() {
        ApiException e = DbErrors.fromMessage("Destino lotado nessa data");
        assertEquals(422, e.getStatus());
        assertEquals("Destino lotado nessa data", e.getMessage());
    }

    @Test
    void erroDesconhecidoNaoExpoeDetalhesDaLigacao() {
        ApiException e = translate(12345, "ORA-12345: algo jdbc:oracle:thin:@192.168.64.4 password=segredo");
        assertEquals(500, e.getStatus());
        assertFalse(e.getMessage().contains("segredo"));
        assertFalse(e.getMessage().contains("192.168"));
        assertTrue(e.getMessage().contains("ORA-12345"));
    }

    @Test
    void semPalavraPasseDa503ComMensagemClara() {
        ApiException e = DbErrors.translate(new SQLException("A variável TURISMO_DB_PASSWORD não está definida.", "08001", DbErrors.NO_PASSWORD));
        assertEquals(503, e.getStatus());
        assertEquals("BD_SEM_CREDENCIAIS", e.getCode());
    }
}
