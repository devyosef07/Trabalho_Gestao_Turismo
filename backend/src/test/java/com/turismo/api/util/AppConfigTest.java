package com.turismo.api.util;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppConfigTest {

    private static AppConfig config(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return new AppConfig(p, null);
    }

    @Test
    void urlComServiceName() {
        AppConfig c = config("db.host", "10.0.0.5", "db.port", "1522", "db.service", "XE");
        assertEquals("jdbc:oracle:thin:@//10.0.0.5:1522/XE", c.jdbcUrl());
    }

    @Test
    void urlComSid() {
        AppConfig c = config("db.host", "vm", "db.sid", "XE");
        assertEquals("jdbc:oracle:thin:@vm:1521:XE", c.jdbcUrl());
    }

    @Test
    void semServiceNemSidFalha() {
        assertThrows(IllegalStateException.class, () -> config("db.host", "vm").jdbcUrl());
    }

    @Test
    void descricaoNaoIncluiPalavraPasse() {
        AppConfig c = config("db.host", "vm", "db.service", "XE", "db.user", "yosef", "db.password", "segredo");
        assertFalse(c.describeDatabase().contains("segredo"));
        assertEquals("yosef@vm:1521/XE", c.describeDatabase());
    }

    @Test
    void ficheiroDeConfiguracaoDoProjetoNaoTemPalavraPasse() throws Exception {
        java.nio.file.Path file = java.nio.file.Paths.get(System.getProperty("turismo.config.dir", "../config"),
                "turismo.properties");
        Properties p = new Properties();
        try (var in = java.nio.file.Files.newInputStream(file)) {
            p.load(in);
        }
        assertEquals(null, p.getProperty("db.password"));
    }
}
