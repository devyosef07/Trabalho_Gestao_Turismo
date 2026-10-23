package com.turismo.frontend.auth;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * As mesmas regras que a API aplica no registo (backend: auth.AccountRules),
 * repetidas aqui só para dar feedback imediato junto de cada campo. A API
 * volta sempre a validar.
 */
public final class AccountRules {

    public static final int NAME_MAX = 100;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 128;
    public static final int MIN_AGE = 13;
    public static final LocalDate OLDEST = LocalDate.of(1900, 1, 1);

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L}'’ -]*[\\p{L}]$");

    private AccountRules() {
    }

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
        if (!hasLetter(password) || !hasDigit(password)) {
            return "Use pelo menos uma letra e um número.";
        }
        return null;
    }

    public static String checkConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return "Repita a palavra-passe.";
        }
        return confirmation.equals(password) ? null : "As palavras-passe não coincidem.";
    }

    public static boolean hasLetter(String s) {
        return s != null && s.chars().anyMatch(Character::isLetter);
    }

    public static boolean hasDigit(String s) {
        return s != null && s.chars().anyMatch(Character::isDigit);
    }

    /**
     * Força da palavra-passe de 0 a 4: comprimento mínimo, letras e números,
     * maiúsculas e minúsculas, símbolos ou 12+ caracteres.
     */
    public static int strength(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int score = 0;
        if (s.length() >= PASSWORD_MIN) {
            score++;
        }
        if (hasLetter(s) && hasDigit(s)) {
            score++;
        }
        if (s.chars().anyMatch(Character::isUpperCase) && s.chars().anyMatch(Character::isLowerCase)) {
            score++;
        }
        if (s.length() >= 12 || s.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            score++;
        }
        return checkPassword(s) == null ? Math.max(score, 2) : Math.min(score, 1);
    }
}
