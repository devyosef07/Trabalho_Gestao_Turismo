package com.turismo.frontend.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * Configuração da aplicação desktop (endereço da API e tempo máximo de espera),
 * lida de config/turismo.properties. Pode ser sobreposta com -Dapi.baseUrl=...
 */
public final class ClientConfig {

    private final String baseUrl;
    private final Duration timeout;

    public ClientConfig(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
    }

    public static ClientConfig load() {
        Properties props = new Properties();
        Path dir = findConfigDir();
        if (dir != null) {
            read(props, dir.resolve("turismo.properties"));
            read(props, dir.resolve("turismo.local.properties"));
        }
        String baseUrl = System.getProperty("api.baseUrl", props.getProperty("api.baseUrl", "http://localhost:8083/api"));
        String timeout = System.getProperty("api.timeoutSeconds", props.getProperty("api.timeoutSeconds", "40"));
        return new ClientConfig(baseUrl.trim(), Duration.ofSeconds(Long.parseLong(timeout.trim())));
    }

    private static Path findConfigDir() {
        String explicit = System.getProperty("turismo.config.dir", System.getenv("TURISMO_CONFIG_DIR"));
        if (explicit != null && !explicit.isBlank()) {
            return Paths.get(explicit);
        }
        for (String candidate : new String[]{"config", "../config"}) {
            Path p = Paths.get(candidate);
            if (Files.isRegularFile(p.resolve("turismo.properties"))) {
                return p;
            }
        }
        return null;
    }

    private static void read(Properties props, Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            System.err.println("Não foi possível ler " + file + ": " + e.getMessage());
        }
    }

    public String baseUrl() {
        return baseUrl;
    }

    public Duration timeout() {
        return timeout;
    }
}
