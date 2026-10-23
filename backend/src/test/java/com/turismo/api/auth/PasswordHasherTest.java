package com.turismo.api.auth;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hash das palavras-passe e regras das contas (sem base de dados). */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void hashNaoContemAPalavraPasseETemSalDiferente() {
        String a = hasher.hash("segura123".toCharArray());
        String b = hasher.hash("segura123".toCharArray());
        assertTrue(a.startsWith("pbkdf2-sha256$1000$"));
        assertFalse(a.contains("segura123"));
        assertNotEquals(a, b, "cada hash tem o seu sal");
    }

    @Test
    void verificaACertaERecusaAErradaEHashesMalFormados() {
        String h = hasher.hash("segura123".toCharArray());
        assertTrue(hasher.verify("segura123".toCharArray(), h));
        assertFalse(hasher.verify("segura124".toCharArray(), h));
        assertFalse(hasher.verify("segura123".toCharArray(), "texto-simples"));
        assertFalse(hasher.verify("segura123".toCharArray(), null));
        assertFalse(hasher.verify("x".toCharArray(), "pbkdf2-sha256$abc$$"));
    }

    @Test
    void asIteracoesFicamNoHash() {
        String antigo = new PasswordHasher(500).hash("segura123".toCharArray());
        assertTrue(hasher.verify("segura123".toCharArray(), antigo), "hashes antigos continuam válidos");
        assertEquals(600_000, PasswordHasher.ITERATIONS);
    }

    @Test
    void regrasDoNome() {
        assertNull(AccountRules.checkName("  Maria   José Cossa "));
        assertNull(AccountRules.checkName("Ana-Rita D'Almeida"));
        assertEquals("Indique o nome e o apelido.", AccountRules.checkName("Maria"));
        assertEquals("Use apenas letras, espaços, hífen ou apóstrofo.", AccountRules.checkName("Maria 2"));
        assertEquals("Indique o nome completo.", AccountRules.checkName("   "));
        assertEquals("Maria José Cossa", AccountRules.normalizeName("  Maria   José Cossa "));
    }

    @Test
    void regrasDaDataDeNascimento() {
        LocalDate hoje = LocalDate.of(2026, 10, 22);
        assertNull(AccountRules.checkBirthDate(LocalDate.of(1995, 3, 14), hoje));
        assertEquals("A data de nascimento não pode ser no futuro.",
                AccountRules.checkBirthDate(LocalDate.of(2026, 10, 23), hoje));
        assertEquals("A data de nascimento tem de ser posterior a 1900.",
                AccountRules.checkBirthDate(LocalDate.of(1899, 12, 31), hoje));
        assertEquals("É preciso ter pelo menos 13 anos.", AccountRules.checkBirthDate(LocalDate.of(2020, 1, 1), hoje));
        assertEquals("Indique a data de nascimento.", AccountRules.checkBirthDate(null, hoje));
    }

    @Test
    void regrasDaPalavraPasse() {
        assertNull(AccountRules.checkPassword("segura123"));
        assertEquals("A palavra-passe precisa de pelo menos 8 caracteres.", AccountRules.checkPassword("ab1"));
        assertEquals("Use pelo menos uma letra e um número.", AccountRules.checkPassword("abcdefgh"));
        assertEquals("Use pelo menos uma letra e um número.", AccountRules.checkPassword("12345678"));
        assertEquals("Indique uma palavra-passe.", AccountRules.checkPassword(""));
    }

    @Test
    void nomeDeUtilizadorGeradoSemAcentos() {
        assertEquals("maria.cossa", AccountRules.usernameBase("Maria José Cossa"));
        assertEquals("joao.goncalves", AccountRules.usernameBase("João Gonçalves"));
        assertEquals("anarita.dalmeida", AccountRules.usernameBase("Ana-Rita D'Almeida"));
        assertEquals("utilizador", AccountRules.usernameBase("李 王"));
    }
}
