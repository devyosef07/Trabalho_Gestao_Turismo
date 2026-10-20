package com.turismo.api.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void obrigatorioETamanhoMaximo() {
        Validator v = new Validator().text("nome", " ", 10, true).text("outro", "12345678901", 10, true);
        assertEquals("Campo obrigatório.", v.errors().get("nome"));
        assertEquals("Máximo de 10 caracteres.", v.errors().get("outro"));
    }

    @Test
    void textoOpcionalVazioEhValido() {
        assertTrue(new Validator().text("descricao", null, 200, false).isValid());
    }

    @Test
    void valoresDaListaPermitida() {
        Validator v = new Validator().oneOf("regiao", "Lisboa", List.of("Maputo", "Gaza"));
        assertTrue(v.errors().get("regiao").contains("Maputo, Gaza"));
        assertTrue(new Validator().oneOf("regiao", "Gaza", List.of("Maputo", "Gaza")).isValid());
    }

    @Test
    void inteirosPositivosComLimiteDeDigitos() {
        assertEquals("Tem de ser maior que zero.", new Validator().positive("cap", 0, 2).errors().get("cap"));
        assertEquals("Máximo de 2 dígitos.", new Validator().positive("cap", 100, 2).errors().get("cap"));
        assertTrue(new Validator().positive("cap", 99, 2).isValid());
    }

    @Test
    void classificacaoEntreZeroECincoComUmaCasa() {
        assertTrue(new Validator().rating("c", new BigDecimal("4.5")).isValid());
        assertTrue(new Validator().rating("c", new BigDecimal("5.0")).isValid());
        assertTrue(new Validator().rating("c", null).isValid());
        assertEquals("Tem de estar entre 0,0 e 5,0.", new Validator().rating("c", new BigDecimal("5.1")).errors().get("c"));
        assertEquals("Use no máximo uma casa decimal.", new Validator().rating("c", new BigDecimal("4.55")).errors().get("c"));
    }

    @Test
    void valorMonetario() {
        assertTrue(new Validator().money("v", new BigDecimal("25000.00")).isValid());
        assertEquals("Tem de ser maior que zero.", new Validator().money("v", BigDecimal.ZERO).errors().get("v"));
        assertEquals("Use no máximo duas casas decimais.", new Validator().money("v", new BigDecimal("1.234")).errors().get("v"));
    }

    @Test
    void formatoDeHora() {
        assertTrue(new Validator().hour("h", "23:59").isValid());
        assertTrue(new Validator().hour("h", "00:00").isValid());
        assertEquals(1, new Validator().hour("h", "24:00").errors().size());
        assertEquals(1, new Validator().hour("h", "8:00").errors().size());
        assertEquals(1, new Validator().hour("h", "08:60").errors().size());
    }

    @Test
    void ordemDeHorasEDatas() {
        assertEquals(1, new Validator().after("f", "08:00", "08:00", "x").errors().size());
        assertTrue(new Validator().after("f", "17:00", "08:00", "x").isValid());
        LocalDate d = LocalDate.of(2026, 10, 15);
        assertEquals(1, new Validator().after("p", d, d, "x").errors().size());
        assertTrue(new Validator().after("p", d.plusDays(1), d, "x").isValid());
    }

    @Test
    void throwIfInvalidLevaOsCampos() {
        ApiException e = assertThrows(ApiException.class,
                () -> new Validator().required("a", null).required("b", "").throwIfInvalid());
        assertEquals(422, e.getStatus());
        assertEquals(2, e.getFields().size());
    }
}
