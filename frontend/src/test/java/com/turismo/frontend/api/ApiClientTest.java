package com.turismo.frontend.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiClientTest {

    private HttpServer server;
    private ApiClient client;
    private volatile String lastBody;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api", ex -> {
            String path = ex.getRequestURI().getRawPath();
            lastBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = 200;
            String body = "[]";
            if (path.endsWith("/erro")) {
                status = 422;
                body = "{\"error\":\"Existem campos com valores inválidos.\",\"code\":\"VALIDACAO\",\"fields\":{\"nome\":\"Campo obrigatório.\"}}";
            } else if (path.endsWith("/html")) {
                status = 500;
                body = "<html>oops</html>";
            } else if (path.endsWith("/objeto")) {
                body = "{\"message\":\"ok\"}";
            } else if (path.contains("/eco/")) {
                body = "{\"path\":\"" + path + "\"}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        client = new ApiClient(new ClientConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/api/",
                Duration.ofSeconds(5)));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void arrayVazioEhResultadoValido() throws Exception {
        assertEquals(0, client.getArray("/x").size());
    }

    @Test
    void errosTrazemMensagemCodigoECampos() {
        ApiException e = assertThrows(ApiException.class, () -> client.getArray("/erro"));
        assertEquals(422, e.getStatus());
        assertEquals("VALIDACAO", e.getCode());
        assertEquals("Campo obrigatório.", e.getFields().get("nome"));
        assertEquals("Existem campos com valores inválidos.", e.getMessage());
    }

    @Test
    void respostaNaoJsonNaoEhAceiteComoDados() {
        ApiException e = assertThrows(ApiException.class, () -> client.getArray("/html"));
        assertEquals(500, e.getStatus());
    }

    @Test
    void tipoDeRespostaInesperadoEhErro() {
        assertEquals("RESPOSTA_INVALIDA", assertThrows(ApiException.class, () -> client.getArray("/objeto")).getCode());
    }

    @Test
    void corpoJsonEhEnviadoEmUtf8() throws Exception {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("nome", "Ilha de Moçambique");
        client.post("/objeto", o);
        assertTrue(lastBody.contains("Moçambique"));
    }

    @Test
    void segmentosDaChaveSaoCodificados() throws Exception {
        String path = client.getObject("/eco/" + ApiClient.segment("Patrimonio historico") + "/" + ApiClient.segment("a/b"))
                .get("path").getAsString();
        assertEquals("/api/eco/Patrimonio%20historico/a%2Fb", path);
    }

    @Test
    void servidorDesligadoEhIndisponivel() {
        server.stop(0);
        ApiException e = assertThrows(ApiException.class, () -> client.getArray("/x"));
        assertTrue(e.isUnreachable());
        assertTrue(e.isRetryable());
    }
}
