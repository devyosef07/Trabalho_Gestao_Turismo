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
 * Política de escrita: tudo o que altera dados corre numa ligação em
 * transação que é desfeita no fim (ROLLBACK). As funções/procedures que fazem
 * COMMIT por dentro só são chamadas em caminhos de erro, que não gravam nada.
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

    // ------------------------------------------- escritas com ROLLBACK

    @Test
    void criaAtualizaEApagaDestinoNumaTransacaoDesfeita() throws SQLException {
        requireListViews();
        int before = new DestinoDAO().getAllDestinos().size();
        try (RollbackConnection tx = new RollbackConnection()) {
            DestinoDAO dao = new DestinoDAO(tx.provider());
            String id = dao.addDestino(new Destino(null, "[TESTE] Ilha de Moçambique", "Nampula", 120));
            assertTrue(id.matches("DT-\\d{3}"), id);
            Destino d = dao.getDestinoById(id);
            assertEquals("[TESTE] Ilha de Moçambique", d.getNomeDestino());
            d.setCapacidadeVisitantes(150);
            assertTrue(dao.updateDestino(d));
            assertEquals(150, dao.getDestinoById(id).getCapacidadeVisitantes());
            assertTrue(dao.deleteDestino(id));
            assertNull(dao.getDestinoById(id));
        }
        assertEquals(before, new DestinoDAO().getAllDestinos().size(), "o ROLLBACK desfez tudo");
    }

    @Test
    void constraintCheckDaBdRejeitaRegiaoInvalida() throws SQLException {
        try (RollbackConnection tx = new RollbackConnection()) {
            SQLException e = assertThrows(SQLException.class, () -> new DestinoDAO(tx.provider())
                    .addDestino(new Destino(null, "[TESTE]", "Lisboa", 10)));
            ApiException api = DbErrors.translate(e);
            assertEquals("REGRA_VIOLADA", api.getCode());
            assertTrue(api.getMessage().contains("regiao in"), api.getMessage());
        }
    }

    @Test
    void transporteEOperadorComRollback() throws SQLException {
        requireListViews();
        try (RollbackConnection tx = new RollbackConnection()) {
            TransporteDAO t = new TransporteDAO(tx.provider());
            String id = t.addTransporte(new Transporte(null, "[TESTE] Barco", 12, "Nao"));
            assertEquals("Nao", t.getTransporteById(id).getDisponibilidade());
            assertTrue(t.deleteTransporte(id));

            OperadorDAO o = new OperadorDAO(tx.provider());
            String op = o.addOperador(new com.turismo.api.model.Operador(null, "[TESTE] Operador", "Teste",
                    new java.math.BigDecimal("4.5")));
            assertTrue(op.matches("OP-\\d{3}"), op);
            assertEquals(0, new java.math.BigDecimal("4.5").compareTo(o.getOperadorById(op).getClassOperador()));
            assertTrue(o.deleteOperador(op));
        }
    }

    @Test
    void trocaDePreferenciaEhAtomica() throws SQLException {
        requireListViews();
        List<TuristaPreferencia> prefs = new TuristaPreferenciaDAO().getAllTuristaPreferencias();
        assumeTrue(!prefs.isEmpty(), "sem preferências");
        TuristaPreferencia p = prefs.get(0);
        String nova = ConstraintCatalog.domain(ConnectionProvider.DEFAULT, "TURISTA_PREFERENCIA", "PREFERENCIA").stream()
                .filter(v -> prefs.stream().noneMatch(x -> x.getIdTurista().equals(p.getIdTurista()) && x.getPreferencia().equals(v)))
                .findFirst().orElseThrow();
        try (RollbackConnection tx = new RollbackConnection()) {
            TuristaPreferenciaDAO dao = new TuristaPreferenciaDAO(tx.provider());
            assertTrue(dao.updateTuristaPreferencia(p.getIdTurista(), p.getPreferencia(), nova));
            assertNull(dao.getTuristaPreferencia(p.getIdTurista(), p.getPreferencia()));
            assertNotNull(dao.getTuristaPreferencia(p.getIdTurista(), nova));
        }
        assertNotNull(new TuristaPreferenciaDAO().getTuristaPreferencia(p.getIdTurista(), p.getPreferencia()));
    }

    @Test
    void atualizacoesDeTuristaETipoComRollback() throws SQLException {
        requireListViews();
        List<Turista> turistas = new TuristaDAO().getAllTuristas();
        List<TipoAlojamento> tipos = new TipoAlojamentoDAO().getAllTipoAlojamentos();
        assumeTrue(!turistas.isEmpty() && !tipos.isEmpty());
        try (RollbackConnection tx = new RollbackConnection()) {
            Turista t = turistas.get(0);
            TuristaDAO dao = new TuristaDAO(tx.provider());
            assertTrue(dao.updateTurista(new Turista(t.getIdTurista(), "[TESTE] Nome", t.getNacionalidade())));
            assertEquals("[TESTE] Nome", dao.getTuristaById(t.getIdTurista()).getNomeTurista());

            TipoAlojamentoDAO tdao = new TipoAlojamentoDAO(tx.provider());
            TipoAlojamento tipo = tipos.get(0);
            assertTrue(tdao.updateTipoAlojamento(new TipoAlojamento(tipo.getTipoAlojamento(), 9)));
            assertEquals(9, tdao.getTipoAlojamentoById(tipo.getTipoAlojamento()).getCapAlojamento());
        }
        assertEquals(turistas.get(0).getNomeTurista(),
                new TuristaDAO().getTuristaById(turistas.get(0).getIdTurista()).getNomeTurista());
    }

    @Test
    void escritaEmAtracaoRefleteOEstadoDoTrigger() throws SQLException {
        requireListViews();
        List<Destino> destinos = new DestinoDAO().getAllDestinos();
        assumeTrue(!destinos.isEmpty());
        try (RollbackConnection tx = new RollbackConnection()) {
            AtracaoDAO dao = new AtracaoDAO(tx.provider());
            Atracao a = new Atracao(null, "Praia", "[TESTE]", "08:00", "17:00", null, null, null,
                    destinos.get(0).getIdDestino());
            if (invalid("VALIDARHORABERTURA")) {
                SQLException e = assertThrows(SQLException.class, () -> dao.addAtracao(a));
                assertEquals("TRIGGER_INVALIDO", DbErrors.translate(e).getCode());
            } else {
                String id = dao.addAtracao(a);
                assertEquals("08:00", dao.getAtracaoById(id).getHoraAbertura());
            }
        }
    }

    @Test
    void criacaoDeReservaRefleteOEstadoDosTriggers() throws SQLException {
        requireListViews();
        List<Turista> turistas = new TuristaDAO().getAllTuristas();
        assumeTrue(!turistas.isEmpty());
        try (RollbackConnection tx = new RollbackConnection()) {
            ReservaDAO dao = new ReservaDAO(tx.provider());
            Reserva r = new Reserva(null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(33), "PENDENTE",
                    turistas.get(0).getIdTurista());
            if (invalid("VALIDARDATACHEGADA") || invalid("VALIDARDATAPARTIDA")) {
                SQLException e = assertThrows(SQLException.class, () -> dao.addReserva(r, List.of()));
                assertEquals("TRIGGER_INVALIDO", DbErrors.translate(e).getCode());
            } else {
                String id = dao.addReserva(r, List.of());
                Reserva saved = dao.getReservaById(id);
                assertEquals(3, saved.getNoites());
                assertEquals("Falta alojamento e pagamento", saved.getEstadoCompletude());
            }
        }
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
        assertEquals(39, schema.get("viewsEsperadas"), "24 de relatório + 15 de listagem");
    }
}
