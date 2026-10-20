package com.turismo.api.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConstraintCatalogTest {

    @Test
    void interpretaListaInComQuebrasDeLinha() {
        Map.Entry<String, List<String>> e = ConstraintCatalog.parseInList(
                "regiao in (\n\t\t'Maputo', 'Gaza', 'Cabo Delgado')");
        assertEquals("REGIAO", e.getKey());
        assertEquals(List.of("Maputo", "Gaza", "Cabo Delgado"), e.getValue());
    }

    @Test
    void ignoraOutrasCondicoes() {
        assertNull(ConstraintCatalog.parseInList("capacidade_visitantes > 0"));
        assertNull(ConstraintCatalog.parseInList("\"ID_DESTINO\" IS NOT NULL"));
        assertNull(ConstraintCatalog.parseInList("class_alojamento between 0.0 and 5.0"));
        assertNull(ConstraintCatalog.parseInList(null));
    }
}
