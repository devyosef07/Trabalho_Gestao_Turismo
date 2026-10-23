package com.turismo.api.integration;

import com.turismo.api.dao.AlojamentoDAO;
import com.turismo.api.dao.AtracaoDAO;
import com.turismo.api.dao.DashboardDAO;
import com.turismo.api.dao.DestinoDAO;
import com.turismo.api.dao.OperadorAlojamentoDAO;
import com.turismo.api.dao.OperadorDAO;
import com.turismo.api.dao.OperadorTransporteDAO;
import com.turismo.api.dao.OperadorTuristaDAO;
import com.turismo.api.dao.PagamentoDAO;
import com.turismo.api.dao.RelatorioDAO;
import com.turismo.api.dao.ReservaAlojamentoDAO;
import com.turismo.api.dao.ReservaDAO;
import com.turismo.api.dao.SchemaDAO;
import com.turismo.api.dao.TipoAlojamentoDAO;
import com.turismo.api.dao.TransporteDAO;
import com.turismo.api.dao.TuristaDAO;
import com.turismo.api.dao.TuristaDestinoDAO;
import com.turismo.api.dao.TuristaPreferenciaDAO;
import com.turismo.api.model.Atracao;
import com.turismo.api.model.Destino;
import com.turismo.api.model.Reserva;
import com.turismo.api.model.TipoAlojamento;
import com.turismo.api.model.Transporte;
import com.turismo.api.model.Turista;
import com.turismo.api.model.TuristaDestino;
import com.turismo.api.model.TuristaPreferencia;
import com.turismo.api.util.ApiException;
import com.turismo.api.util.ConnectionProvider;
import com.turismo.api.util.ConstraintCatalog;
import com.turismo.api.util.DatabaseConnection;
import com.turismo.api.util.DbErrors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testes contra a base de dados Oracle 10g real (config/turismo.properties +
 * TURISMO_DB_PASSWORD). Ficam "skipped" se a BD não estiver acessível.
 *
 * Política de escrita: todas as escritas são procedures/funções que fazem
 * COMMIT por dentro, por isso só são chamadas em caminhos de erro (registo
 * inexistente, regra violada), que não gravam nada.
 * Nota: um INSERT que chega a pedir NEXTVAL consome esse número da sequência
 * (as sequências do Oracle não voltam atrás com ROLLBACK).
 */
@Tag("integracao")
class OracleIntegrationTest {

    private static Map<String, Object> schema;

    @BeforeAll
    static void requireDatabase() {
        try (Connection c = DatabaseConnection.getConnection()) {
            schema = new SchemaDAO().status();
        } catch (SQLException e) {
            assumeTrue(false, "Base de dados indisponível: " + DbErrors.translate(e).getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean invalid(String objectName) {
        return ((List<Map<String, String>>) schema.get("objetosInvalidos")).stream()
                .anyMatch(o -> o.get("nome").equals(objectName));
    }

    @SuppressWarnings("unchecked")
    private static List<String> missingViews() {
        return (List<String>) schema.get("viewsEmFalta");
    }

    /** As listagens leem das views v_lista_*; sem elas (falta o GRANT CREATE VIEW) o teste é saltado. */
    private static void requireListViews() {
        assumeTrue(SchemaDAO.LIST_VIEWS.stream().noneMatch(missingViews()::contains),
                "views de listagem em falta (database/conceder_privilegios.sql e views.sql)");
    }

    // --------------------------------------------------------- leitura

    @Test
    void listagensFalhamComoViewEmFaltaSemAsViews() {
        assumeTrue(missingViews().contains("v_lista_destinos"), "a view v_lista_destinos existe");
        SQLException e = assertThrows(SQLException.class, () -> new DestinoDAO().getAllDestinos());
        assertEquals(942, e.getErrorCode());
        assertEquals("OBJETO_INEXISTENTE", DbErrors.translate(e).getCode());
    }

    @Test
    void leAsQuinzeTabelasSemErros() throws SQLException {
        requireListViews();
        assertNotNull(new DestinoDAO().getAllDestinos());
        assertNotNull(new AtracaoDAO().getAllAtracoes());
        assertNotNull(new AlojamentoDAO().getAllAlojamentos());
        assertNotNull(new TipoAlojamentoDAO().getAllTipoAlojamentos());
        assertNotNull(new TransporteDAO().getAllTransportes());
        assertNotNull(new TuristaDAO().getAllTuristas());
        assertNotNull(new TuristaPreferenciaDAO().getAllTuristaPreferencias());
        assertNotNull(new OperadorDAO().getAllOperadores());
        assertNotNull(new ReservaDAO().getAllReservas());
        assertNotNull(new PagamentoDAO().getAllPagamentos());
        assertNotNull(new TuristaDestinoDAO().getAllTuristaDestinos());
        assertNotNull(new ReservaAlojamentoDAO().getAllReservaAlojamentos());
        assertNotNull(new OperadorTuristaDAO().getAllOperadorTuristas());
        assertNotNull(new OperadorTransporteDAO().getAllOperadorTransportes());
        assertNotNull(new OperadorAlojamentoDAO().getAllOperadorAlojamentos());
    }

    @Test
    void cadaRegistoDaListaEhEncontradoPelaChave() throws SQLException {
        requireListViews();
        for (Destino d : new DestinoDAO().getAllDestinos()) {
            assertEquals(d.getNomeDestino(), new DestinoDAO().getDestinoById(d.getIdDestino()).getNomeDestino());
        }
        for (Turista t : new TuristaDAO().getAllTuristas()) {
            assertEquals(t, new TuristaDAO().getTuristaById(t.getIdTurista()));
        }
        TuristaDestinoDAO tdDao = new TuristaDestinoDAO();
        for (TuristaDestino td : tdDao.getAllTuristaDestinos()) {
            assertNotNull(tdDao.getTuristaDestino(td.getIdDestino(), td.getIdTurista(), td.getDataVisita()),
                    "chave composta com data: " + td);
        }
        TuristaPreferenciaDAO tpDao = new TuristaPreferenciaDAO();
        for (TuristaPreferencia tp : tpDao.getAllTuristaPreferencias()) {
            assertNotNull(tpDao.getTuristaPreferencia(tp.getIdTurista(), tp.getPreferencia()));
        }
        assertNull(new DestinoDAO().getDestinoById("ZZ-999"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void painelSoUsaViews() throws SQLException {
        requireListViews();
        Map<String, Object> d = new DashboardDAO().load();
        assertEquals(List.of("views", "verificacoes"), List.copyOf(d.keySet()), "o painel não lê tabelas diretamente");
        Map<String, Object> views = (Map<String, Object>) d.get("views");
        Map<String, Object> destinos = (Map<String, Object>) views.get("v_total_atracoes_destino");
        if (Boolean.TRUE.equals(destinos.get("ok"))) {
            List<?> rows = (List<?>) ((Map<String, Object>) destinos.get("data")).get("rows");
            assertEquals(new DestinoDAO().getAllDestinos().size(), rows.size(), "uma linha por destino");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void seccoesDoPainelRefletemAsViewsExistentes() throws SQLException {
        Map<String, Object> views = (Map<String, Object>) new DashboardDAO().load().get("views");
        for (Map.Entry<String, Object> e : views.entrySet()) {
            Map<String, Object> section = (Map<String, Object>) e.getValue();
            boolean exists = !missingViews().contains(e.getKey());
            assertEquals(exists, section.get("ok"), "secção " + e.getKey());
            if (!exists) {
                assertEquals("OBJETO_INEXISTENTE", section.get("code"));
            }
        }
    }

    @Test
    void asVinteEQuatroViewsOuSaoLidasOuFalhamComoEmFalta() {
        RelatorioDAO dao = new RelatorioDAO();
        assertEquals(24, RelatorioDAO.VIEWS.size());
        for (String view : RelatorioDAO.VIEWS.keySet()) {
            if (missingViews().contains(view)) {
                SQLException e = assertThrows(SQLException.class, () -> dao.read(view), view);
                assertEquals(942, e.getErrorCode());
            } else {
                assertDoesNotThrowSql(() -> dao.read(view));
            }
        }
        assertEquals(404, assertThrows(ApiException.class, () -> dao.read("user_users")).getStatus());
    }

    @Test
    void dominiosVemDasConstraintsCheck() throws SQLException {
        Map<String, List<String>> d = ConstraintCatalog.domains(ConnectionProvider.DEFAULT);
        assertTrue(d.get("DESTINO.REGIAO").contains("Cabo Delgado"));
        assertEquals(List.of("PENDENTE", "CONFIRMADA", "CANCELADA", "CONCLUIDA"), d.get("RESERVA.STATUS_RESERVA"));
        assertEquals(List.of("DINHEIRO", "CARTAO", "TRANSFERENCIA"), d.get("PAGAMENTO.METODO_PAGAMENTO"));
        assertEquals(List.of("Sim", "Nao"), d.get("TRANSPORTE.DISPONIBILIDADE"));
        assertTrue(d.get("TURISTA_PREFERENCIA.PREFERENCIA").contains("Gastronomia"));
        assertTrue(d.get("ATRACAO.TIPO_ATRACAO").contains("Patrimonio historico"));
    }

    @Test
    void funcoesDeLeituraDaBd() throws SQLException {
        requireListViews();
        List<Destino> destinos = new DestinoDAO().getAllDestinos();
        assumeTrue(!destinos.isEmpty(), "sem destinos");
        Map<String, String> info = new DestinoDAO().getInfo(destinos.get(0).getIdDestino());
        assertTrue(info.get("info").startsWith("Destino: " + destinos.get(0).getNomeDestino()), info.get("info"));
        assertEquals("Nenhum destino tem o identificador indicado", new DestinoDAO().getInfo("ZZ-999").get("info"));
    }

    // ------------------- escritas (procedures com COMMIT): só caminhos que não gravam

    @Test
    void constraintCheckDaBdRejeitaRegiaoInvalida() {
        ApiException e = assertThrows(ApiException.class, () -> new DestinoDAO()
                .addDestino(new Destino(null, "[TESTE]", "Lisboa", 10)));
        assertEquals("REGRA_VIOLADA", e.getCode());
        assertTrue(e.getMessage().contains("regiao in"), e.getMessage());
    }

    @Test
    void proceduresDeAlteracaoEEliminacaoIndicamRegistoInexistente() throws SQLException {
        String x = "ZZ-999";
        assertFalse(new DestinoDAO().updateDestino(new Destino(x, "[TESTE]", "Nampula", 10)));
        assertFalse(new DestinoDAO().deleteDestino(x));
        assertFalse(new AlojamentoDAO().updateAlojamento(
                new com.turismo.api.model.Alojamento(x, "[TESTE]", null, "Casal", "DT-001")));
        assertFalse(new AlojamentoDAO().deleteAlojamento(x));
        assertFalse(new TransporteDAO().updateTransporte(new Transporte(x, "[TESTE]", 10, "Sim")));
        assertFalse(new TransporteDAO().deleteTransporte(x));
        assertFalse(new OperadorDAO().updateOperador(new com.turismo.api.model.Operador(x, "[TESTE]", "Teste", null)));
        assertFalse(new OperadorDAO().deleteOperador(x));
        assertFalse(new AtracaoDAO().deleteAtracao(x));
        assertFalse(new ReservaDAO().deleteReserva(x));
        assertFalse(new PagamentoDAO().updatePagamento(new com.turismo.api.model.Pagamento(x,
                new java.math.BigDecimal("100"), LocalDate.now().minusDays(1), "DINHEIRO", "PENDENTE", null, null, "RS-001")));
        assertFalse(new PagamentoDAO().deletePagamento(x));
        assertFalse(new TipoAlojamentoDAO().updateTipoAlojamento(new TipoAlojamento("[TESTE]", 2)));
        assertFalse(new TipoAlojamentoDAO().deleteTipoAlojamento("[TESTE]"));
        assertFalse(new TuristaDAO().updateTurista(new Turista(x, "[TESTE]", "Teste")));
        assertFalse(new TuristaPreferenciaDAO().deleteTuristaPreferencia(x, "Praias"));
        assertFalse(new TuristaDestinoDAO().deleteTuristaDestino(x, x, LocalDate.now()));
        assertFalse(new ReservaAlojamentoDAO().deleteReservaAlojamento(x, x));
        assertFalse(new OperadorTuristaDAO().deleteOperadorTurista(x, x));
        assertFalse(new OperadorTransporteDAO().deleteOperadorTransporte(x, x));
        assertFalse(new OperadorAlojamentoDAO().deleteOperadorAlojamento(x, x));
    }

    // ------------------- funções/procedures: só caminhos que não gravam

    @Test
    void funcoesDevolvemErrosTratados() {
        ApiException e1 = assertThrows(ApiException.class, () -> new PagamentoDAO().concluirPagamento("ZZ-999"));
        assertEquals(404, e1.getStatus());
        assertEquals("Pagamento inexistente", e1.getMessage());

        ApiException e2 = assertThrows(ApiException.class, () -> new TuristaDestinoDAO()
                .addTuristaDestino(new TuristaDestino("ZZ-999", "TR-001", LocalDate.now())));
        assertEquals("Destino inexistente", e2.getMessage());

        ApiException e3 = assertThrows(ApiException.class, () -> new TuristaDAO().deleteTurista("ZZ-999"));
        assertEquals(404, e3.getStatus());
    }

    @Test
    void proceduresDeEscritaDevolvemErrosTratados() throws SQLException {
        ApiException e1 = assertThrows(ApiException.class, () -> new AtracaoDAO().addAtracao(
                new Atracao(null, "Praia", "[TESTE]", "08:00", "17:00", null, null, null, "ZZ-999")));
        assertEquals("REFERENCIA_INEXISTENTE", e1.getCode());

        ApiException e2 = assertThrows(ApiException.class, () -> new ReservaDAO().addReserva(
                new Reserva(null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(33), "PENDENTE", "ZZ-999"),
                List.of()));
        assertEquals("REFERENCIA_INEXISTENTE", e2.getCode());

        ApiException e3 = assertThrows(ApiException.class, () -> new PagamentoDAO().addPagamento(
                new com.turismo.api.model.Pagamento(null, new java.math.BigDecimal("100"), LocalDate.now().minusDays(1),
                        "DINHEIRO", "PENDENTE", null, "[TESTE]", "ZZ-999")));
        assertEquals("REFERENCIA_INEXISTENTE", e3.getCode());

        // atualizar_pref_turista e alterar_horario_atracao / alterar_datas_reserva: registo inexistente
        assertFalse(new TuristaPreferenciaDAO().updateTuristaPreferencia("ZZ-999", "Praias", "Cultura"));
        assertFalse(new AtracaoDAO().updateAtracao(
                new Atracao("ZZ-999", "Praia", "[TESTE]", "08:00", "17:00", null, null, null, "DT-001")));
        assertFalse(new ReservaDAO().updateReserva(
                new Reserva("ZZ-999", LocalDate.now().plusDays(30), LocalDate.now().plusDays(33), "PENDENTE", "TR-001")));
    }

    @Test
    void procedureComErroEngolidoEhDetetadaPeloDbmsOutput() {
        ApiException e = assertThrows(ApiException.class, () -> new TuristaPreferenciaDAO()
                .addTuristaPreferencia(new TuristaPreferencia("ZZ-999", "Praias")));
        assertEquals("REFERENCIA_INEXISTENTE", e.getCode());
    }

    @Test
    void cancelarReservaInexistente() {
        ApiException e = assertThrows(ApiException.class, () -> new ReservaDAO().cancelarReserva("ZZ-999"));
        if (invalid("VALIDARDATACHEGADA")) {
            assertEquals("TRIGGER_INVALIDO", e.getCode());
        } else {
            assertEquals(404, e.getStatus());
        }
    }

    private static void assertDoesNotThrowSql(Executable e) {
        try {
            e.execute();
        } catch (Throwable t) {
            throw new AssertionError("não devia falhar: " + t.getMessage(), t);
        }
    }

    @Test
    void estadoDoEsquemaEhCoerente() {
        assertFalse(((String) schema.get("database")).isEmpty());
        assertEquals(41, schema.get("viewsEsperadas"), "24 de relatório + 15 de listagem + 2 das contas");
    }
}
