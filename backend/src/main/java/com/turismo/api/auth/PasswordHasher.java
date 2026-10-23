package com.turismo.api.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Hash de palavras-passe com PBKDF2-HMAC-SHA256 (incluído no JDK, sem
 * dependências novas), sal aleatório de 16 bytes e 600 000 iterações
 * (recomendação OWASP para PBKDF2-SHA256).
 *
 * Formato guardado: {@code pbkdf2-sha256$<iterações>$<sal base64>$<hash base64>}.
 * As iterações ficam no próprio hash, por isso podem subir no futuro sem
 * invalidar as contas existentes.
 */
public final class PasswordHasher {

    public static final int ITERATIONS = 600_000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Hash de referência para gastar o mesmo tempo quando a conta não existe. */
    private static volatile String dummy;

    private final int iterations;

    public PasswordHasher() {
        this(ITERATIONS);
    }

    /** Só os testes usam menos iterações (o formato regista o valor usado). */
    public PasswordHasher(int iterations) {
        this.iterations = iterations;
    }

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIX + "$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    /** Compara em tempo constante; um hash mal formado nunca é aceite. */
    public boolean verify(char[] password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int rounds = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, rounds);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Faz o mesmo trabalho que {@link #verify} quando o nome de utilizador não
     * existe, para que o tempo de resposta não revele que contas existem.
     */
    public void verifyDummy(char[] password) {
        String ref = dummy;
        if (ref == null) {
            ref = hash("referencia".toCharArray());
            dummy = ref;
        }
        verify(password, ref);
    }

    private static byte[] derive(char[] password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível nesta JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Apaga da memória uma palavra-passe que já não é precisa. */
    public static void wipe(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
