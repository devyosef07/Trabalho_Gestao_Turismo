package com.turismo.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuração da aplicação, lida de config/turismo.properties.
 *
 * Ordem de prioridade (a última ganha):
 *   1. config/turismo.properties
 *   2. config/turismo.local.properties (não versionado)
 *   3. propriedades de sistema (-Dchave=valor)
 *
 * A pasta config é procurada em -Dturismo.config.dir, na variável de ambiente
 * TURISMO_CONFIG_DIR, em ./config e em ../config.
 */
public final class AppConfig {

    private static final String PASSWORD_ENV = "TURISMO_DB_PASSWORD";
    private static volatile AppConfig instance;

    private final Properties props;
    private final Path configDir;

    AppConfig(Properties props, Path configDir) {
        this.props = props;
        this.configDir = configDir;
    }

    public static AppConfig get() {
        AppConfig result = instance;
        if (result == null) {
            synchronized (AppConfig.class) {
                result = instance;
                if (result == null) {
                    result = load();
                    instance = result;
                }
            }
        }
        return result;
    }

    static AppConfig load() {
        Path dir = findConfigDir();
        Properties props = new Properties();
        if (dir != null) {
            readInto(props, dir.resolve("turismo.properties"));
            readInto(props, dir.resolve("turismo.local.properties"));
        }
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith("db.") || name.startsWith("api.")) {
                props.setProperty(name, System.getProperty(name));
            }
        }
        return new AppConfig(props, dir);
    }

    private static Path findConfigDir() {
        String explicit = System.getProperty("turismo.config.dir");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("TURISMO_CONFIG_DIR");
        }
        if (explicit != null && !explicit.isBlank()) {
            Path p = Paths.get(explicit);
            return Files.isDirectory(p) ? p : null;
        }
        for (String candidate : new String[]{"config", "../config"}) {
            Path p = Paths.get(candidate);
            if (Files.isRegularFile(p.resolve("turismo.properties"))) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static void readInto(Properties props, Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + file + ": " + e.getMessage(), e);
        }
    }

    public String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valor inválido para " + key + ": " + value);
        }
    }

    /** Palavra-passe da BD: variável de ambiente primeiro, depois o ficheiro local. */
    public String dbPassword() {
        String env = System.getenv(PASSWORD_ENV);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return get("db.password", null);
    }

    public String dbHost() {
        return get("db.host", "localhost");
    }

    public int dbPort() {
        return getInt("db.port", 1521);
    }

    public String dbUser() {
        return get("db.user", null);
    }

    /** URL JDBC: usa o service name quando existe, senão o SID. */
    public String jdbcUrl() {
        String service = get("db.service", null);
        if (service != null) {
            return "jdbc:oracle:thin:@//" + dbHost() + ":" + dbPort() + "/" + service;
        }
        String sid = get("db.sid", null);
        if (sid != null) {
            return "jdbc:oracle:thin:@" + dbHost() + ":" + dbPort() + ":" + sid;
        }
        throw new IllegalStateException("Configure db.service ou db.sid em config/turismo.properties");
    }

    /** Descrição da ligação sem a palavra-passe (para logs e mensagens). */
    public String describeDatabase() {
        String target = get("db.service", get("db.sid", "?"));
        return dbUser() + "@" + dbHost() + ":" + dbPort() + "/" + target;
    }

    public Path configDir() {
        return configDir;
    }
}
