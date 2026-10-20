package com.turismo.api.util;

import com.turismo.api.model.Pagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void numerosComPontoMesmoComLocalePortugues() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("pt", "PT"));
            Pagamento p = new Pagamento("PG-001", new BigDecimal("25000.50"), LocalDate.of(2026, 10, 10),
                    "CARTAO", "CONCLUIDO", "RC-001", "a \"citação\"", "RS-001");
            String json = Json.toJson(p);
            assertTrue(json.contains("\"valor\":25000.50"), json);
            assertTrue(json.contains("\"dataPagamento\":\"2026-10-10\""), json);
            assertTrue(json.contains("a \\\"citação\\\""), json);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void leDatasEDecimais() {
        Pagamento p = Json.fromJson("{\"valor\": 12.5, \"dataPagamento\": \"2026-01-31\"}", Pagamento.class);
        assertEquals(new BigDecimal("12.5"), p.getValor());
        assertEquals(LocalDate.of(2026, 1, 31), p.getDataPagamento());
    }

    @Test
    void dataInvalidaDa400() {
        ApiException e = assertThrows(ApiException.class,
                () -> Json.fromJson("{\"dataPagamento\": \"31/01/2026\"}", Pagamento.class));
        assertEquals(400, e.getStatus());
    }

    @Test
    void corpoVazioDa400() {
        assertEquals(400, assertThrows(ApiException.class, () -> Json.fromJson("", Pagamento.class)).getStatus());
    }
}
