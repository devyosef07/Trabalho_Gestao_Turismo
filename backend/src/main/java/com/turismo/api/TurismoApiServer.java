package com.turismo.api;

import com.sun.net.httpserver.HttpServer;
import com.turismo.api.dao.SchemaDAO;
import com.turismo.api.handler.AlojamentoHandler;
import com.turismo.api.handler.AtracaoHandler;
import com.turismo.api.handler.DashboardHandler;
import com.turismo.api.handler.DestinoHandler;
import com.turismo.api.handler.DominioHandler;
import com.turismo.api.handler.EstadoHandler;
import com.turismo.api.handler.OperadorAlojamentoHandler;
import com.turismo.api.handler.OperadorHandler;
import com.turismo.api.handler.OperadorTransporteHandler;
import com.turismo.api.handler.OperadorTuristaHandler;
import com.turismo.api.handler.PagamentoHandler;
import com.turismo.api.handler.RelatorioHandler;
import com.turismo.api.handler.ReservaAlojamentoHandler;
import com.turismo.api.handler.ReservaHandler;
import com.turismo.api.handler.TipoAlojamentoHandler;
import com.turismo.api.handler.TransporteHandler;
import com.turismo.api.handler.TuristaDestinoHandler;
import com.turismo.api.handler.TuristaHandler;
import com.turismo.api.handler.TuristaPreferenciaHandler;
import com.turismo.api.util.AppConfig;
import com.turismo.api.util.DbErrors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main server class for the Turismo API
 *
 * A porta (api.port) e o endereço (api.bindAddress) vêm de config/turismo.properties.
 * Por omissão a API só aceita ligações locais, porque não tem autenticação.
 */
public class TurismoApiServer {
    private static final String[] ENDPOINTS = {
            "/api/estado", "/api/dominios", "/api/dashboard", "/api/relatorios",
            "/api/destinos", "/api/atracoes", "/api/alojamentos", "/api/tipoAlojamentos", "/api/transportes",
            "/api/turistas", "/api/turistaPreferencias", "/api/operadores",
            "/api/reservas", "/api/pagamentos",
            "/api/turistaDestinos", "/api/reservaAlojamentos", "/api/operadorTuristas",
            "/api/operadorTransportes", "/api/operadorAlojamentos"
    };

    private final HttpServer server;
    private final ExecutorService executor;
    private final int port;

    public TurismoApiServer() {
        this(AppConfig.get().getInt("api.port", 8083), AppConfig.get().get("api.bindAddress", "127.0.0.1"));
    }

    public TurismoApiServer(int port, String bindAddress) {
        try {
            // Create HTTP server on specified port
            this.server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            this.port = server.getAddress().getPort();

            // Create context paths and assign handlers
            server.createContext("/api/estado", new EstadoHandler());
            server.createContext("/api/dominios", new DominioHandler());
            server.createContext("/api/dashboard", new DashboardHandler());
            server.createContext("/api/relatorios", new RelatorioHandler());
            server.createContext("/api/turistas", new TuristaHandler());
            server.createContext("/api/destinos", new DestinoHandler());
            server.createContext("/api/reservas", new ReservaHandler());
            server.createContext("/api/alojamentos", new AlojamentoHandler());
            server.createContext("/api/atracoes", new AtracaoHandler());
            server.createContext("/api/pagamentos", new PagamentoHandler());
            server.createContext("/api/transportes", new TransporteHandler());
            server.createContext("/api/tipoAlojamentos", new TipoAlojamentoHandler());
            server.createContext("/api/operadores", new OperadorHandler());
            server.createContext("/api/operadorAlojamentos", new OperadorAlojamentoHandler());
            server.createContext("/api/operadorTransportes", new OperadorTransporteHandler());
            server.createContext("/api/operadorTuristas", new OperadorTuristaHandler());
            server.createContext("/api/reservaAlojamentos", new ReservaAlojamentoHandler());
            server.createContext("/api/turistaDestinos", new TuristaDestinoHandler());
            server.createContext("/api/turistaPreferencias", new TuristaPreferenciaHandler());

            // Vários pedidos em paralelo (o painel e as listas carregam ao mesmo tempo)
            this.executor = Executors.newFixedThreadPool(8, r -> {
                Thread t = new Thread(r, "api-worker");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);

            System.out.println("Turismo API Server initialized on " + bindAddress + ":" + this.port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server on port " + port + ": " + e.getMessage(), e);
        }
    }

    /**
     * Starts the server
     */
    public void start() {
        server.start();
        System.out.println("Turismo API Server started on port " + port);
        System.out.println("Endpoints available:");
        for (String endpoint : ENDPOINTS) {
            System.out.println("  " + endpoint);
        }
    }

    /**
     * Stops the server
     */
    public void stop() {
        server.stop(0);
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Turismo API Server stopped");
    }

    public int getPort() {
        return port;
    }

    /** Mostra o estado da BD no arranque (sem nunca escrever a palavra-passe). */
    @SuppressWarnings("unchecked")
    static void printDatabaseStatus() {
        AppConfig config = AppConfig.get();
        System.out.println("Configuração: " + (config.configDir() == null ? "(pasta config não encontrada)" : config.configDir()));
        System.out.println("Base de dados: " + config.describeDatabase());
        try {
            Map<String, Object> status = new SchemaDAO().status();
            System.out.println("Ligação à base de dados OK (" + status.get("database") + ")");
            List<Map<String, String>> invalid = (List<Map<String, String>>) status.get("objetosInvalidos");
            List<String> missing = (List<String>) status.get("viewsEmFalta");
            if (!invalid.isEmpty()) {
                System.out.println("AVISO: " + invalid.size() + " objetos INVALID na BD (aplique database/correcoes.sql):");
                for (Map<String, String> o : invalid) {
                    System.out.println("  - " + o.get("tipo") + " " + o.get("nome"));
                }
            }
            if (!missing.isEmpty()) {
                System.out.println("AVISO: faltam " + missing.size() + " das " + status.get("viewsEsperadas")
                        + " views (aplique database/correcoes.sql).");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao ligar à base de dados: " + DbErrors.translate(e).getMessage());
        }
    }

    /**
     * Main method to run the server
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Testa a ligação à base de dados antes de aceitar pedidos
        printDatabaseStatus();

        TurismoApiServer apiServer = new TurismoApiServer();
        apiServer.start();

        // Add shutdown hook to stop server gracefully when JVM shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Turismo API Server...");
            apiServer.stop();
            com.turismo.api.util.DatabaseConnection.shutdown();
        }));
    }
}
