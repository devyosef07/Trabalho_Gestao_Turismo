package com.turismo.api.handler;

import com.turismo.api.auth.AccountRules;
import com.turismo.api.auth.PasswordHasher;
import com.turismo.api.auth.SessionService;
import com.turismo.api.dao.ContaDAO;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.Validator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Contas e sessões da aplicação.
 *
 * POST   /api/auth/registo  cria uma conta (nome completo, data de nascimento, palavra-passe)
 * POST   /api/auth/sessao   inicia sessão e devolve o token
 * GET    /api/auth/sessao   conta da sessão atual (Authorization: Bearer)
 * DELETE /api/auth/sessao   termina a sessão atual
 *
 * O nome de utilizador é gerado a partir do nome completo (ex.: "jose.vaz")
 * e devolvido no registo, porque o formulário só pede os quatro campos.
 */
public class AuthHandler extends BaseHandler {

    /** Duração da sessão: 7 dias com "manter sessão iniciada", 12 horas sem. */
    static final int MINUTES_REMEMBER = 7 * 24 * 60;
    static final int MINUTES_SHORT = 12 * 60;
    private static final String BAD_CREDENTIALS = "Nome de utilizador ou palavra-passe incorretos.";

    static final class RegistoBody {
        String nomeCompleto;
        String dataNascimento;
        String palavraPasse;
        String confirmacao;
    }

    static final class SessaoBody {
        String nomeUtilizador;
        String palavraPasse;
        Boolean manterSessao;
    }

    private final ContaDAO contas;
    private final SessionService sessions;
    private final PasswordHasher hasher;

    public AuthHandler() {
        this(new ContaDAO(), SessionService.DEFAULT, new PasswordHasher(), ConnectionProvider.DEFAULT);
    }

    public AuthHandler(ContaDAO contas, SessionService sessions, PasswordHasher hasher, ConnectionProvider connections) {
        super("/api/auth", connections);
        this.contas = contas;
        this.sessions = sessions;
        this.hasher = hasher;
    }

    @Override
    protected void route(Request r) throws IOException, SQLException {
        if (r.size() != 1) {
            throw notFound("Endpoint");
        }
        String what = r.segment(0);
        if (what.equals("registo") && r.is("POST")) {
            register(r);
        } else if (what.equals("sessao") && r.is("POST")) {
            login(r);
        } else if (what.equals("sessao") && r.is("GET")) {
            r.ok(sessionBody(currentSession(r), null));
        } else if (what.equals("sessao") && r.is("DELETE")) {
            logout(r);
        } else if (what.equals("registo") || what.equals("sessao")) {
            throw methodNotAllowed();
        } else {
            throw notFound("Endpoint");
        }
    }

    private void register(Request r) throws IOException, SQLException {
        RegistoBody b = r.body(RegistoBody.class);
        String name = AccountRules.normalizeName(b.nomeCompleto);
        LocalDate birth = null;
        String birthError = null;
        if (b.dataNascimento == null || b.dataNascimento.isBlank()) {
            birthError = "Indique a data de nascimento.";
        } else {
            try {
                birth = LocalDate.parse(b.dataNascimento.trim());
                birthError = AccountRules.checkBirthDate(birth, LocalDate.now());
            } catch (DateTimeParseException e) {
                birthError = "Data de nascimento inválida.";
            }
        }
        String password = b.palavraPasse;
        Validator v = new Validator();
        check(v, "nomeCompleto", AccountRules.checkName(name));
        check(v, "dataNascimento", birthError);
        check(v, "palavraPasse", AccountRules.checkPassword(password));
        v.check(password == null || password.equals(b.confirmacao), "confirmacao", "As palavras-passe não coincidem.");
        v.throwIfInvalid();

        char[] secret = password.toCharArray();
        String hash;
        try {
            hash = hasher.hash(secret);
        } finally {
            PasswordHasher.wipe(secret);
        }

        String base = AccountRules.usernameBase(name);
        ApiException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            String username = freeUsername(base);
            try {
                contas.registrarConta(username, name, birth, hash);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("nomeUtilizador", username);
                data.put("nomeCompleto", name);
                data.put("dataNascimento", birth);
                r.created(message("Conta criada com sucesso.", data));
                return;
            } catch (ApiException e) {
                // Outro registo ficou com o mesmo nome entre a escolha e o INSERT: tenta o seguinte.
                if (!"CHAVE_DUPLICADA".equals(e.getCode())) {
                    throw e;
                }
                last = e;
            }
        }
        throw last;
    }

    /** Primeiro nome livre: base, base2, base3... */
    private String freeUsername(String base) throws SQLException {
        Set<String> taken = new HashSet<>(contas.nomesComBase(base));
        if (!taken.contains(base)) {
            return base;
        }
        for (int i = 2; ; i++) {
            String candidate = base + i;
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
    }

    private void login(Request r) throws IOException, SQLException {
        SessaoBody b = r.body(SessaoBody.class);
        String username = b.nomeUtilizador == null ? "" : b.nomeUtilizador.trim().toLowerCase(Locale.ROOT);
        Validator v = new Validator();
        v.check(!username.isEmpty(), "nomeUtilizador", "Indique o nome de utilizador.");
        v.check(b.palavraPasse != null && !b.palavraPasse.isEmpty(), "palavraPasse", "Indique a palavra-passe.");
        v.throwIfInvalid();

        char[] secret = b.palavraPasse.toCharArray();
        try {
            ContaDAO.Credenciais cred = username.length() > 40 ? null : contas.getCredenciais(username);
            if (cred == null) {
                hasher.verifyDummy(secret);
                throw new ApiException(401, "CREDENCIAIS_INVALIDAS", BAD_CREDENTIALS);
            }
            if (cred.bloqueada()) {
                throw new ApiException(423, "CONTA_BLOQUEADA", "Demasiadas tentativas falhadas. Tente novamente dentro de "
                        + cred.minutosBloqueio() + (cred.minutosBloqueio() == 1 ? " minuto." : " minutos."));
            }
            if (!hasher.verify(secret, cred.hashPalavraPasse())) {
                contas.registarAcesso(cred.idConta(), false);
                throw new ApiException(401, "CREDENCIAIS_INVALIDAS", BAD_CREDENTIALS);
            }
            contas.registarAcesso(cred.idConta(), true);
            String token = SessionService.newToken();
            boolean remember = b.manterSessao == null || b.manterSessao;
            contas.iniciarSessao(SessionService.hash(token), cred.idConta(), remember ? MINUTES_REMEMBER : MINUTES_SHORT);
            ContaDAO.SessaoAtiva sessao = sessions.find(token);
            if (sessao == null) {
                throw new ApiException(500, "SESSAO_NAO_CRIADA", "Não foi possível abrir a sessão.");
            }
            r.created(sessionBody(sessao, token));
        } finally {
            PasswordHasher.wipe(secret);
        }
    }

    private void logout(Request r) throws SQLException, IOException {
        String token = SessionService.bearer(r.exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) {
            throw invalidSession();
        }
        sessions.forget(token);
        contas.terminarSessao(SessionService.hash(token));
        r.ok(message("Sessão terminada."));
    }

    private ContaDAO.SessaoAtiva currentSession(Request r) throws SQLException {
        String token = SessionService.bearer(r.exchange.getRequestHeaders().getFirst("Authorization"));
        ContaDAO.SessaoAtiva sessao = sessions.find(token);
        if (sessao == null) {
            throw invalidSession();
        }
        return sessao;
    }

    private static Map<String, Object> sessionBody(ContaDAO.SessaoAtiva sessao, String token) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (token != null) {
            body.put("token", token);
        }
        body.put("iniciadaEm", sessao.iniciadaEm());
        body.put("expiraEm", sessao.expiraEm());
        body.put("conta", sessao.conta());
        return body;
    }

    private static ApiException invalidSession() {
        return new ApiException(401, "SESSAO_INVALIDA", "A sessão terminou ou é inválida. Inicie sessão novamente.");
    }

    private static void check(Validator v, String field, String error) {
        v.check(error == null, field, error);
    }
}
