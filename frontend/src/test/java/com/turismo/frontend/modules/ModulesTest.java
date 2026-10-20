package com.turismo.frontend.modules;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulesTest {

    @Test
    void existemQuinzeModulosUmPorTabela() {
        List<ModuleSpec> all = Modules.all();
        assertEquals(15, all.size());
        Set<String> endpoints = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (ModuleSpec m : all) {
            assertTrue(endpoints.add(m.endpoint()), "endpoint repetido " + m.endpoint());
            assertTrue(ids.add(m.id()));
            assertFalse(m.keyFields().isEmpty(), m.id() + " sem chave");
            assertFalse(m.fields().isEmpty(), m.id() + " sem formulário");
            assertFalse(m.columns().isEmpty(), m.id() + " sem colunas");
        }
    }

    @Test
    void asVinteEQuatroViewsEstaoLigadasAModulos() {
        Set<String> views = new HashSet<>();
        for (ModuleSpec m : Modules.all()) {
            for (ReportSpec r : m.reports()) {
                views.add(r.view());
            }
        }
        assertEquals(24, views.size(), views.toString());
    }

    @Test
    void camposDaChaveComposta() {
        ModuleSpec visitas = Modules.visitas();
        assertEquals(List.of("idDestino", "idTurista", "dataVisita"), visitas.keyFields());
        assertFalse(visitas.hasEditFields(), "todas as colunas fazem parte da chave: não há edição");
        JsonObject row = new JsonObject();
        row.addProperty("idDestino", "DT-001");
        row.addProperty("idTurista", "TR-001");
        row.addProperty("dataVisita", "2026-10-15");
        assertEquals("/turistaDestinos/DT-001/TR-001/2026-10-15", visitas.pathOf(row));
        assertEquals("DT-001 / TR-001 / 15/10/2026", visitas.keyText(row));

        JsonObject pref = new JsonObject();
        pref.addProperty("idTurista", "TR-001");
        pref.addProperty("preferencia", "Praias");
        assertEquals("/turistaPreferencias/TR-001/Praias", Modules.preferencias().pathOf(pref));
    }

    @Test
    void pagamentoConcluidoNaoEhEditavel() {
        JsonObject row = new JsonObject();
        row.addProperty("statusPagamento", "CONCLUIDO");
        assertFalse(Modules.pagamentos().editable.test(row));
        row.addProperty("statusPagamento", "PENDENTE");
        assertTrue(Modules.pagamentos().editable.test(row));
    }

    @Test
    void validacaoCruzadaDaAtracao() {
        Map<String, Object> v = new HashMap<>();
        v.put("horaAbertura", "10:00");
        v.put("horaFecho", "09:00");
        v.put("horaAberturaEspecial", "08:00");
        Map<String, String> e = Modules.validateAtracao(v);
        assertEquals("Tem de ser depois da hora de abertura.", e.get("horaFecho"));
        assertTrue(e.containsKey("horaFechoEspecial"));
        assertTrue(e.containsKey("detalhesHorarioEspecial"));
    }

    @Test
    void validacaoCruzadaDaReserva() {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("dataChegada", LocalDate.of(2026, 11, 1));
        v.put("dataPartida", LocalDate.of(2026, 11, 1));
        assertEquals("Tem de ser depois da data de chegada.",
                Modules.reservas().crossValidation.apply(v).get("dataPartida"));
    }

    @Test
    void validacaoDosCamposDoFormulario() {
        Map<String, String> p = new HashMap<>();
        FormDialog.validate(FieldSpec.text("n", "Nome", 5).required(), null, p);
        FormDialog.validate(FieldSpec.text("t", "Texto", 5), "123456", p);
        FormDialog.validate(FieldSpec.rating("r", "Avaliação"), new BigDecimal("5.5"), p);
        FormDialog.validate(FieldSpec.money("m", "Valor"), new BigDecimal("10.123"), p);
        FormDialog.validate(FieldSpec.time("h", "Hora"), "24:00", p);
        FormDialog.validate(FieldSpec.integer("i", "Inteiro", 2), 100, p);
        assertEquals(6, p.size(), p.toString());
        Map<String, String> ok = new HashMap<>();
        FormDialog.validate(FieldSpec.rating("r", "Avaliação"), null, ok);
        FormDialog.validate(FieldSpec.money("m", "Valor"), new BigDecimal("1500.50"), ok);
        FormDialog.validate(FieldSpec.time("h", "Hora"), "23:59", ok);
        assertTrue(ok.isEmpty());
    }

    @Test
    void formatacaoPortuguesa() {
        assertEquals("15/10/2026", Format.date(LocalDate.of(2026, 10, 15)));
        assertTrue(Format.money(new BigDecimal("25000")).matches("25.000,00 MT"), Format.money(new BigDecimal("25000")));
        assertEquals("4,5", Format.rating(new BigDecimal("4.5")));
        assertEquals("1 registo", Format.plural(1, "registo", "registos"));
        assertEquals("atracao", Format.fold("Atração"));
        assertEquals("Código destino", Format.humanize("ID_destino"));
        assertEquals("2,5 mil", Format.compact(2500));
    }
}
