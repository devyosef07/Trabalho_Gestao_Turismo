package com.turismo.api.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import com.turismo.api.auth.AuthFilter;
import com.turismo.api.auth.PasswordHasher;
import com.turismo.api.auth.SessionService;
import com.turismo.api.dao.ContaDAO;
import com.turismo.api.model.Conta;
import com.turismo.api.util.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registo, início e fim de sessão e o filtro que protege os endpoints, com
 * um DAO em memória (sem base de dados).
 */
class AuthHandlerHttpTest {

    /** Contas e sessões em memória, com as mesmas regras das procedures de contas.sql. */
    static final class MemoryDAO extends ContaDAO {
        final Map<String, String[]> accounts = new HashMap<>();   // utilizador -> {id, nome, nascimento, hash}
        final Map<String, Integer> failures = new HashMap<>();
        final Map<String, Long> sessions = new HashMap<>();       // hash do token -> id da conta
        final List<String> calls = new ArrayList<>();
        boolean down;

        MemoryDAO() {
            super(() -> {
                throw new SQLException("não usado");
            });
        }

        private void check() throws SQLException {
            if (down) {
                throw new SQLRecoverableException("IO Error: Network Adapter could not establish the connection");
            }
        }

        @Override
        public Credenciais getCredenciais(String user) throws SQLException {
            check();
            String[] a = accounts.get(user);
            if (a == null) {
                return null;
            }
            int f = failures.getOrDefault(user, 0);
            return new Credenciais(Long.parseLong(a[0]), user, a[3], f >= 5 ? 5 : 0);
        }

        @Override
        public List<String> nomesComBase(String base) {
            List<String> r = new ArrayList<>();
            for (String k : accounts.keySet()) {
                if (k.startsWith(base)) {
                    r.add(k);
                }
            }
            return r;
        }

        @Override
        public SessaoAtiva getSessaoAtiva(String hash) throws SQLException {
            check();
            Long id = sessions.get(hash);
            if (id == null) {
                return null;
            }
            for (Map.Entry<String, String[]> e : accounts.entrySet()) {
                if (Long.parseLong(e.getValue()[0]) == id) {
                    Conta c = new Conta();
                    c.setIdConta(id);
                    c.setNomeUtilizador(e.getKey());
                    c.setNomeCompleto(e.getValue()[1]);
                    c.setDataNascimento(LocalDate.parse(e.getValue()[2]));
                    c.setDataCriacao("2026-10-22T10:00:00");
                    return new SessaoAtiva(c, "2026-10-22T10:00:00", "2026-10-29T10:00:00");
                }
            }
            return null;
        }

        @Override
        public String registrarConta(String user, String name, LocalDate birth, String hash) {
            calls.add("registrar_conta");
            if (accounts.containsKey(user)) {
                throw new ApiException(409, "CHAVE_DUPLICADA", "Já existe um registo com esta chave.");
            }
            accounts.put(user, new String[]{String.valueOf(accounts.size() + 1), name, birth.toString(), hash});
            return "Conta registada com sucesso!";
        }

        @Override
        public void registarAcesso(long id, boolean ok) {
            calls.add("registar_acesso_conta:" + (ok ? "S" : "N"));
            for (Map.Entry<String, String[]> e : accounts.entrySet()) {
                if (Long.parseLong(e.getValue()[0]) == id) {
                    failures.put(e.getKey(), ok ? 0 : failures.getOrDefault(e.getKey(), 0) + 1);
                }
            }
        }

        @Override
        public void iniciarSessao(String hash, long id, int minutes) {
            calls.add("iniciar_sessao:" + minutes);
            sessions.put(hash, id);
        }

        @Override
        public boolean terminarSessao(String hash) {
            calls.add("terminar_sessao");
            return sessions.remove(hash) != null;
        }
    }

    private HttpServer server;
    private final HttpClient http = HttpClient.newHttpClient();
    private final MemoryDAO dao = new MemoryDAO();
    private String base;

    @BeforeEach
    void start() throws IOException {
        SessionService sessions = new SessionService(dao);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth", new AuthHandler(dao, sessions, new PasswordHasher(1_000), () -> {
            throw new SQLException("não usado");
        }));
        server.createContext("/api/destinos", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }).getFilters().add(new AuthFilter(sessions));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonObject json(HttpResponse<String> r) {
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private static final String MARIA = "{\"nomeCompleto\":\"Maria José Cossa\",\"dataNascimento\":\"1995-03-14\","
            + "\"palavraPasse\":\"segura123\",\"confirmacao\":\"segura123\"}";

    private String login(String user, String password) throws Exception {
        HttpResponse<String> r = send("POST", "/auth/sessao",
                "{\"nomeUtilizador\":\"" + user + "\",\"palavraPasse\":\"" + password + "\"}", null);
        assertEquals(201, r.statusCode(), r.body());
        return json(r).get("token").getAsString();
    }

    @Test
    void registoGeraNomeDeUtilizadorEGuardaSoOHash() throws Exception {
        HttpResponse<String> r = send("POST", "/auth/registo", MARIA, null);
        assertEquals(201, r.statusCode(), r.body());
        assertEquals("maria.cossa", json(r).getAsJsonObject("data").get("nomeUtilizador").getAsString());
        String hash = dao.accounts.get("maria.cossa")[3];
        assertTrue(hash.startsWith("pbkdf2-sha256$"));
        assertFalse(hash.contains("segura123"));
        assertFalse(r.body().contains("pbkdf2"), "a resposta nunca inclui o hash");

        HttpResponse<String> again = send("POST", "/auth/registo", MARIA, null);
        assertEquals("maria.cossa2", json(again).getAsJsonObject("data").get("nomeUtilizador").getAsString());
    }

    @Test
    void registoInvalidoDevolveErrosPorCampoSemCriarConta() throws Exception {
        HttpResponse<String> r = send("POST", "/auth/registo", "{\"nomeCompleto\":\"Maria\",\"dataNascimento\":"
                + "\"2999-01-01\",\"palavraPasse\":\"abc\",\"confirmacao\":\"abd\"}", null);
        assertEquals(422, r.statusCode());
        JsonObject fields = json(r).getAsJsonObject("fields");
        assertEquals("Indique o nome e o apelido.", fields.get("nomeCompleto").getAsString());
        assertEquals("A data de nascimento não pode ser no futuro.", fields.get("dataNascimento").getAsString());
        assertTrue(fields.has("palavraPasse"));
        assertEquals("As palavras-passe não coincidem.", fields.get("confirmacao").getAsString());
        assertFalse(dao.calls.contains("registrar_conta"));

        HttpResponse<String> bad = send("POST", "/auth/registo", MARIA.replace("1995-03-14", "1995-02-30"), null);
        assertEquals("Data de nascimento inválida.", json(bad).getAsJsonObject("fields").get("dataNascimento").getAsString());
    }

    @Test
    void loginCertoAbreSessaoELoginErradoNaoRevelaSeAContaExiste() throws Exception {
        send("POST", "/auth/registo", MARIA, null);
        HttpResponse<String> wrong = send("POST", "/auth/sessao",
                "{\"nomeUtilizador\":\"maria.cossa\",\"palavraPasse\":\"errada123\"}", null);
        HttpResponse<String> none = send("POST", "/auth/sessao",
                "{\"nomeUtilizador\":\"ninguem\",\"palavraPasse\":\"errada123\"}", null);
        assertEquals(401, wrong.statusCode());
        assertEquals(401, none.statusCode());
        assertEquals(json(wrong).get("error").getAsString(), json(none).get("error").getAsString());
        assertTrue(dao.calls.contains("registar_acesso_conta:N"));

        String token = login("Maria.Cossa", "segura123");
        assertEquals(43, token.length());
        assertTrue(dao.calls.contains("iniciar_sessao:" + AuthHandler.MINUTES_REMEMBER));
        assertFalse(dao.sessions.containsKey(token), "a BD guarda o hash do token, não o token");

        HttpResponse<String> me = send("GET", "/auth/sessao", null, token);
        assertEquals(200, me.statusCode());
        assertEquals("Maria José Cossa", json(me).getAsJsonObject("conta").get("nomeCompleto").getAsString());
        assertFalse(me.body().contains("token"));
    }

    @Test
    void cincoFalhasBloqueiamAConta() throws Exception {
        send("POST", "/auth/registo", MARIA, null);
        for (int i = 0; i < 5; i++) {
            send("POST", "/auth/sessao", "{\"nomeUtilizador\":\"maria.cossa\",\"palavraPasse\":\"errada123\"}", null);
        }
        HttpResponse<String> r = send("POST", "/auth/sessao",
                "{\"nomeUtilizador\":\"maria.cossa\",\"palavraPasse\":\"segura123\"}", null);
        assertEquals(423, r.statusCode());
        assertEquals("CONTA_BLOQUEADA", json(r).get("code").getAsString());
    }

    @Test
    void filtroExigeSessaoETerminarSessaoRevogaOToken() throws Exception {
        send("POST", "/auth/registo", MARIA, null);
        assertEquals(401, send("GET", "/destinos", null, null).statusCode());
        assertEquals(401, send("GET", "/destinos", null, "inventado").statusCode());

        String token = login("maria.cossa", "segura123");
        assertEquals(200, send("GET", "/destinos", null, token).statusCode());

        assertEquals(200, send("DELETE", "/auth/sessao", null, token).statusCode());
        assertEquals(401, send("GET", "/destinos", null, token).statusCode(), "a cache também é limpa");
        assertEquals(401, send("GET", "/auth/sessao", null, token).statusCode());
    }

    @Test
    void semBaseDeDadosOFiltroResponde503ENao401() throws Exception {
        send("POST", "/auth/registo", MARIA, null);
        String token = login("maria.cossa", "segura123");
        dao.down = true;
        HttpResponse<String> r = send("GET", "/destinos", null, token + "x");
        assertEquals(503, r.statusCode());
        assertNotEquals("SESSAO_INVALIDA", json(r).get("code").getAsString());
    }

    @Test
    void sessaoCurtaSemManterSessao() throws Exception {
        send("POST", "/auth/registo", MARIA, null);
        HttpResponse<String> r = send("POST", "/auth/sessao",
                "{\"nomeUtilizador\":\"maria.cossa\",\"palavraPasse\":\"segura123\",\"manterSessao\":false}", null);
        assertEquals(201, r.statusCode());
        assertTrue(dao.calls.contains("iniciar_sessao:" + AuthHandler.MINUTES_SHORT));
    }
}
