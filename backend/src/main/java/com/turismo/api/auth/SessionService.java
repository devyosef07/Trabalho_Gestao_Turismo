package com.turismo.api.auth;

import com.turismo.api.dao.ContaDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens de sessão: 32 bytes aleatórios (SecureRandom) entregues uma única vez
 * à aplicação desktop. A BD guarda só o SHA-256 do token, por isso uma cópia
 * da tabela de sessões não permite entrar em nenhuma conta.
 *
 * As sessões válidas ficam em memória durante 30 s para não ir à BD (que tem
 * só 4 ligações) em cada pedido; terminar a sessão retira-a logo da cache.
 */
public final class SessionService {

    public static final SessionService DEFAULT = new SessionService(new ContaDAO());

    private static final long CACHE_NANOS = 30_000_000_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Cached(ContaDAO.SessaoAtiva sessao, long until) {
    }

    private final ContaDAO dao;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public SessionService(ContaDAO dao) {
        this.dao = dao;
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }

    /** Extrai o token de "Authorization: Bearer ..."; null se não houver. */
    public static String bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() || token.length() > 200 ? null : token;
    }

    /** Sessão em vigor do token, ou null se não existir, tiver expirado ou terminado. */
    public ContaDAO.SessaoAtiva find(String token) throws SQLException {
        if (token == null) {
            return null;
        }
        String key = hash(token);
        long now = System.nanoTime();
        Cached cached = cache.get(key);
        if (cached != null && now < cached.until) {
            return cached.sessao;
        }
        ContaDAO.SessaoAtiva sessao = dao.getSessaoAtiva(key);
        if (sessao == null) {
            cache.remove(key);
        } else {
            if (cache.size() > 1000) {
                cache.clear();
            }
            cache.put(key, new Cached(sessao, now + CACHE_NANOS));
        }
        return sessao;
    }

    public void forget(String token) {
        if (token != null) {
            cache.remove(hash(token));
        }
    }
}
