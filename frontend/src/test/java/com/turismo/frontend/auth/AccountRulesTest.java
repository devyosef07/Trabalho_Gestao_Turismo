package com.turismo.frontend.auth;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do registo no cliente (as mesmas da API) e leitura da sessão devolvida pela API. */
class AccountRulesTest {

    @Test
    void mesmasMensagensQueAApi() {
        LocalDate hoje = LocalDate.of(2026, 10, 22);
        assertNull(AccountRules.checkName("Maria José Cossa"));
        assertEquals("Indique o nome e o apelido.", AccountRules.checkName("Maria"));
        assertEquals("A data de nascimento não pode ser no futuro.",
                AccountRules.checkBirthDate(hoje.plusDays(1), hoje));
        assertEquals("É preciso ter pelo menos 13 anos.", AccountRules.checkBirthDate(LocalDate.of(2015, 1, 1), hoje));
        assertEquals("Use pelo menos uma letra e um número.", AccountRules.checkPassword("abcdefgh"));
        assertEquals("As palavras-passe não coincidem.", AccountRules.checkConfirmation("segura123", "segura124"));
        assertNull(AccountRules.checkConfirmation("segura123", "segura123"));
    }

    @Test
    void forcaDaPalavraPasse() {
        assertEquals(0, AccountRules.strength(""));
        assertTrue(AccountRules.strength("abc") <= 1, "inválida nunca passa de fraca");
        assertEquals(2, AccountRules.strength("segura123"));
        assertEquals(4, AccountRules.strength("Segura-123-Praia"));
    }

    @Test
    void sessaoLidaDaRespostaDaApi() {
        Session s = Session.fromJson(JsonParser.parseString("{\"token\":\"t\",\"iniciadaEm\":\"2026-10-22T10:00:00\","
                + "\"expiraEm\":\"2026-10-29T10:00:00\",\"conta\":{\"idConta\":3,\"nomeUtilizador\":\"ana.silva\","
                + "\"nomeCompleto\":\"Ana Silva\",\"dataNascimento\":\"2000-01-31\",\"dataCriacao\":null}}")
                .getAsJsonObject(), null);
        assertEquals("t", s.token());
        assertEquals("ana.silva", s.account().username());
        assertEquals(LocalDate.of(2000, 1, 31), s.account().birthDate());
        assertNull(s.account().createdAt(), "um valor em falta fica vazio, nunca inventado");
    }
}
