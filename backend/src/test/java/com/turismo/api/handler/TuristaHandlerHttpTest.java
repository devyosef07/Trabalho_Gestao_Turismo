package com.turismo.api.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import com.turismo.api.dao.TuristaDAO;
import com.turismo.api.model.Turista;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o encaminhamento HTTP, os códigos de estado e o JSON de erro de um
 * handler, com um DAO falso (sem base de dados).
 */
class TuristaHandlerHttpTest {

    private HttpServer server;
    private final HttpClient http = HttpClient.newHttpClient();
    private final FakeDAO dao = new FakeDAO();
    private String base;

    static final class FakeDAO extends TuristaDAO {
        final List<Turista> rows = new ArrayList<>();
        boolean failConnection;
        Turista lastUpdate;

        FakeDAO() {
            super(() -> {
                throw new SQLException("não usado");
            });
        }

        @Override
        public List<Turista> getAllTuristas() throws SQLException {
            if (failConnection) {
                throw new SQLRecoverableException("IO Error: Network Adapter could not establish the connection");
            }
            return rows;
        }

        @Override
        public Turista getTuristaById(String id) {
            return rows.stream().filter(t -> t.getIdTurista().equals(id)).findFirst().orElse(null);
        }

        @Override
        public boolean updateTurista(Turista turista) {
            lastUpdate = turista;
            return getTuristaById(turista.getIdTurista()) != null;
        }
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/turistas", new TuristaHandler(dao, () -> {
            throw new SQLException("não usado");
        }));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/turistas";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void listaVaziaEhUmArrayVazioCom200() throws Exception {
        HttpResponse<String> r = send("GET", "", null);
        assertEquals(200, r.statusCode());
        assertEquals("[]", r.body());
    }

    @Test
    void falhaDaBdNaoSeTornaUmaListaVazia() throws Exception {
        dao.failConnection = true;
        HttpResponse<String> r = send("GET", "", null);
        assertEquals(503, r.statusCode());
        JsonObject body = JsonParser.parseString(r.body()).getAsJsonObject();
        assertEquals("BD_INDISPONIVEL", body.get("code").getAsString());
    }

    @Test
    void devolveOsDadosComCaracteresEspeciais() throws Exception {
        dao.rows.add(new Turista("TR-001", "João \"Jó\" Mondlane", "Moçambicana"));
        HttpResponse<String> r = send("GET", "", null);
        JsonArray a = JsonParser.parseString(r.body()).getAsJsonArray();
        assertEquals("João \"Jó\" Mondlane", a.get(0).getAsJsonObject().get("nomeTurista").getAsString());
    }

    @Test
    void registoInexistenteDa404() throws Exception {
        assertEquals(404, send("GET", "/TR-999", null).statusCode());
    }

    @Test
    void validacaoDevolveOsErrosPorCampo() throws Exception {
        dao.rows.add(new Turista("TR-001", "Ana", "Portuguesa"));
        HttpResponse<String> r = send("PUT", "/TR-001", "{\"nomeTurista\": \"\", \"nacionalidade\": \"" + "x".repeat(51) + "\"}");
        assertEquals(422, r.statusCode());
        JsonObject fields = JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonObject("fields");
        assertEquals("Campo obrigatório.", fields.get("nomeTurista").getAsString());
        assertTrue(fields.get("nacionalidade").getAsString().contains("50"));
    }

    @Test
    void atualizacaoUsaOIdDoCaminho() throws Exception {
        dao.rows.add(new Turista("TR-001", "Ana", "Portuguesa"));
        HttpResponse<String> r = send("PUT", "/TR-001", "{\"idTurista\": \"TR-777\", \"nomeTurista\": \" Ana Maria \", \"nacionalidade\": \"Portuguesa\"}");
        assertEquals(200, r.statusCode());
        assertEquals("TR-001", dao.lastUpdate.getIdTurista());
        assertEquals("Ana Maria", dao.lastUpdate.getNomeTurista());
    }

    @Test
    void jsonInvalidoDa400() throws Exception {
        assertEquals(400, send("POST", "", "{isto não é json").statusCode());
    }

    @Test
    void metodoNaoSuportadoDa405() throws Exception {
        assertEquals(405, send("PATCH", "", null).statusCode());
    }
}
