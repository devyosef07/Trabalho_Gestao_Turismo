package com.turismo.api.auth;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regras das contas de utilizador, validadas no servidor (a aplicação desktop
 * repete-as só para dar feedback imediato). Cada método devolve a mensagem de
 * erro ou null quando o valor é válido.
 */
public final class AccountRules {

    public static final int NAME_MAX = 100;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 128;
    public static final int MIN_AGE = 13;
    public static final LocalDate OLDEST = LocalDate.of(1900, 1, 1);

    /** Letras (com acentos), espaços, hífen e apóstrofo. */
    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L}'’ -]*[\\p{L}]$");

    private AccountRules() {
    }

    /** Espaços nas pontas removidos e espaços repetidos reduzidos a um. */
    public static String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    public static String checkName(String name) {
        String n = normalizeName(name);
        if (n == null || n.isEmpty()) {
            return "Indique o nome completo.";
        }
        if (n.length() > NAME_MAX) {
            return "O nome pode ter no máximo " + NAME_MAX + " caracteres.";
        }
        if (!NAME.matcher(n).matches()) {
            return "Use apenas letras, espaços, hífen ou apóstrofo.";
        }
        if (n.split(" ").length < 2) {
            return "Indique o nome e o apelido.";
        }
        return null;
    }

    public static String checkBirthDate(LocalDate date, LocalDate today) {
        if (date == null) {
            return "Indique a data de nascimento.";
        }
        if (date.isAfter(today)) {
            return "A data de nascimento não pode ser no futuro.";
        }
        if (date.isBefore(OLDEST)) {
            return "A data de nascimento tem de ser posterior a 1900.";
        }
        if (Period.between(date, today).getYears() < MIN_AGE) {
            return "É preciso ter pelo menos " + MIN_AGE + " anos.";
        }
        return null;
    }

    public static String checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Indique uma palavra-passe.";
        }
        if (password.length() < PASSWORD_MIN) {
            return "A palavra-passe precisa de pelo menos " + PASSWORD_MIN + " caracteres.";
        }
        if (password.length() > PASSWORD_MAX) {
            return "A palavra-passe pode ter no máximo " + PASSWORD_MAX + " caracteres.";
        }
        if (!password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit)) {
            return "Use pelo menos uma letra e um número.";
        }
        return null;
    }

    /**
     * Base do nome de utilizador gerado a partir do nome completo:
     * primeiro e último nome, sem acentos, em minúsculas ("José da Silva" → "jose.silva").
     */
    public static String usernameBase(String fullName) {
        String ascii = Normalizer.normalize(normalizeName(fullName), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", "");
        String[] parts = ascii.trim().split(" +");
        String base = parts.length == 1 ? parts[0] : parts[0] + "." + parts[parts.length - 1];
        if (base.length() < 3) {
            base = "utilizador";
        }
        return base.length() > 34 ? base.substring(0, 34) : base;
    }
}
