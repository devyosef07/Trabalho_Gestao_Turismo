package com.turismo.frontend;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * API falsa para testes de interface: responde com JSON fixo por caminho.
 * Os dados aqui são só de teste e nunca são usados pela aplicação real.
 */
final class StubApi implements AutoCloseable {

    record Reply(int status, String body, long delayMs) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();
    final List<String> requests = new CopyOnWriteArrayList<>();

    StubApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/api", exchange -> {
            String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath().substring(4);
            requests.add(key);
            Reply r = replies.getOrDefault(key, new Reply(200, "[]", 0));
            if (key.equals("GET /dominios") && !replies.containsKey(key)) {
                r = new Reply(200, "{}", 0);
            }
            try {
                if (r.delayMs() > 0) {
                    Thread.sleep(r.delayMs());
                }
                byte[] bytes = r.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(r.status(), bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    StubApi on(String methodAndPath, int status, String body) {
        replies.put(methodAndPath, new Reply(status, body, 0));
        return this;
    }

    StubApi slow(String methodAndPath, String body, long delayMs) {
        replies.put(methodAndPath, new Reply(200, body, delayMs));
        return this;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    long count(String methodAndPath) {
        return requests.stream().filter(methodAndPath::equals).count();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
