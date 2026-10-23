package com.turismo.api;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.turismo.api.auth.AuthFilter;
import com.turismo.api.auth.SessionService;
import com.turismo.api.dao.SchemaDAO;
import com.turismo.api.handler.AlojamentoHandler;
import com.turismo.api.handler.AtracaoHandler;
import com.turismo.api.handler.AuthHandler;
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
 * Por omissão a API só aceita ligações locais. Os endpoints de dados exigem uma
 * sessão iniciada em /api/auth (api.requireAuth=false desliga essa exigência,
 * por exemplo para testes manuais com curl).
 */
public class TurismoApiServer {
    private static final String[] ENDPOINTS = {
            "/api/estado", "/api/auth", "/api/dominios", "/api/dashboard", "/api/relatorios",
            "/api/destinos", "/api/atracoes", "/api/alojamentos", "/api/tipoAlojamentos", "/api/transportes",
            "/api/turistas", "/api/turistaPreferencias", "/api/operadores",
            "/api/reservas", "/api/pagamentos",
            "/api/turistaDestinos", "/api/reservaAlojamentos", "/api/operadorTuristas",
            "/api/operadorTransportes", "/api/operadorAlojamentos"
    };

    private final HttpServer server;
    private final ExecutorService executor;
    private final int port;
    private final AuthFilter authFilter;

    public TurismoApiServer() {
        this(AppConfig.get().getInt("api.port", 8083), AppConfig.get().get("api.bindAddress", "127.0.0.1"),
                !"false".equalsIgnoreCase(AppConfig.get().get("api.requireAuth", "true")));
    }

    public TurismoApiServer(int port, String bindAddress) {
        this(port, bindAddress, true);
    }

    public TurismoApiServer(int port, String bindAddress, boolean requireAuth) {
        this.authFilter = requireAuth ? new AuthFilter(SessionService.DEFAULT) : null;
        try {
            // Create HTTP server on specified port
            this.server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            this.port = server.getAddress().getPort();

            // Públicos: estado da ligação e contas/sessões
            server.createContext("/api/estado", new EstadoHandler());
            server.createContext("/api/auth", new AuthHandler());

            // Dados: só com sessão iniciada
            protect("/api/dominios", new DominioHandler());
            protect("/api/dashboard", new DashboardHandler());
            protect("/api/relatorios", new RelatorioHandler());
            protect("/api/turistas", new TuristaHandler());
            protect("/api/destinos", new DestinoHandler());
            protect("/api/reservas", new ReservaHandler());
            protect("/api/alojamentos", new AlojamentoHandler());
            protect("/api/atracoes", new AtracaoHandler());
            protect("/api/pagamentos", new PagamentoHandler());
            protect("/api/transportes", new TransporteHandler());
            protect("/api/tipoAlojamentos", new TipoAlojamentoHandler());
            protect("/api/operadores", new OperadorHandler());
            protect("/api/operadorAlojamentos", new OperadorAlojamentoHandler());
            protect("/api/operadorTransportes", new OperadorTransporteHandler());
            protect("/api/operadorTuristas", new OperadorTuristaHandler());
            protect("/api/reservaAlojamentos", new ReservaAlojamentoHandler());
            protect("/api/turistaDestinos", new TuristaDestinoHandler());
            protect("/api/turistaPreferencias", new TuristaPreferenciaHandler());

            // Vários pedidos em paralelo (o painel e as listas carregam ao mesmo tempo)
            this.executor = Executors.newFixedThreadPool(8, r -> {
                Thread t = new Thread(r, "api-worker");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);

            System.out.println("Turismo API Server initialized on " + bindAddress + ":" + this.port
                    + (requireAuth ? "" : " (SEM autenticação: api.requireAuth=false)"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server on port " + port + ": " + e.getMessage(), e);
        }
    }

    private void protect(String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        if (authFilter != null) {
            context.getFilters().add(authFilter);
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
                System.out.println("AVISO: " + invalid.size() + " objetos INVALID na BD (volte a executar database/Triggers.sql e database/procedures.sql):");
                for (Map<String, String> o : invalid) {
                    System.out.println("  - " + o.get("tipo") + " " + o.get("nome"));
                }
            }
            if (!missing.isEmpty()) {
                System.out.println("AVISO: faltam " + missing.size() + " das " + status.get("viewsEsperadas")
                        + " views (o SYSTEM tem de correr database/conceder_privilegios.sql;"
                        + " depois execute database/views.sql e database/contas.sql).");
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
