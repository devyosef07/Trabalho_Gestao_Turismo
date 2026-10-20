package com.turismo.frontend;

import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ClientConfig;
import com.turismo.frontend.modules.CrudPage;
import com.turismo.frontend.modules.FormDialog;
import com.turismo.frontend.ui.components.Spinner;
import com.turismo.frontend.ui.theme.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Window;
import java.time.Duration;
import java.util.List;

import static com.turismo.frontend.SwingTestSupport.edt;
import static com.turismo.frontend.SwingTestSupport.find;
import static com.turismo.frontend.SwingTestSupport.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da interface real (janela, navegação, estados, formulários) contra
 * uma API falsa local. Não precisam da base de dados.
 */
class MainWindowUiTest {

    private static final String DESTINOS = "[{\"idDestino\":\"DT-001\",\"nomeDestino\":\"Destino de teste\","
            + "\"regiao\":\"Gaza\",\"capacidadeVisitantes\":10,\"totalAtracoes\":0,\"totalAlojamentos\":0}]";

    private StubApi stub;
    private MainWindow window;

    @BeforeAll
    static void theme() {
        edt(Theme::install);
    }

    @AfterEach
    void cleanup() {
        edt(() -> {
            for (Window w : Window.getWindows()) {
                w.dispose();
            }
        });
        if (stub != null) {
            stub.close();
        }
    }

    private MainWindow open(StubApi api) {
        ApiClient client = new ApiClient(new ClientConfig(api.baseUrl(), Duration.ofSeconds(10)));
        window = edt(() -> {
            MainWindow w = new MainWindow(client);
            w.setSize(new Dimension(1280, 820));
            w.setVisible(true);
            return w;
        });
        return window;
    }

    private CrudPage go(String id) {
        edt(() -> window.navigate(id));
        CrudPage page = (CrudPage) edt(() -> window.page(id));
        waitFor(id, 10_000, () -> !page.isLoading() && (page.hasLoaded() || page.visibleState().equals("error")));
        return page;
    }

    @Test
    void navegacaoAbreOsQuinzeModulosEOPainel() throws Exception {
        stub = new StubApi();
        open(stub);
        List<String> ids = edt(() -> window.sidebar().itemIds());
        assertEquals(16, ids.size());
        for (String id : ids) {
            if (id.equals(MainWindow.DASHBOARD)) {
                continue;
            }
            CrudPage page = go(id);
            assertEquals(id, edt(window::currentPage));
            assertEquals(id, edt(() -> window.sidebar().getActive()));
            assertEquals("empty", edt(page::visibleState), "lista vazia em " + id);
        }
    }

    @Test
    void listaComDadosMostraTabelaEDetalhe() throws Exception {
        stub = new StubApi().on("GET /destinos", 200, DESTINOS);
        open(stub);
        CrudPage page = go("destinos");
        assertEquals("table", edt(page::visibleState));
        assertEquals(1, (int) edt(() -> page.table().getRowCount()));
        edt(() -> page.table().setRowSelectionInterval(0, 0));
        JButton delete = edt(() -> find(page, "eliminar", JButton.class));
        assertNotNull(delete, "o detalhe mostra as ações do registo selecionado");
    }

    @Test
    void erroDaApiNaoEhMostradoComoListaVazia() throws Exception {
        stub = new StubApi().on("GET /destinos", 503,
                "{\"error\":\"Sem ligação à base de dados Oracle.\",\"code\":\"BD_INDISPONIVEL\"}");
        open(stub);
        CrudPage page = go("destinos");
        assertEquals("error", edt(page::visibleState));
        assertTrue(edt(() -> SwingTestSupport.containsText(page, "Sem ligação à base de dados Oracle.")));
        JButton retry = edt(() -> findButton(page, "Tentar novamente"));
        assertNotNull(retry);
        stub.on("GET /destinos", 200, DESTINOS);
        edt(() -> retry.doClick());
        waitFor("recuperação", 10_000, () -> "table".equals(page.visibleState()));
    }

    @Test
    void apiDesligadaMostraEstadoSemLigacao() throws Exception {
        stub = new StubApi();
        String url = stub.baseUrl();
        stub.close();
        ApiClient client = new ApiClient(new ClientConfig(url, Duration.ofSeconds(5)));
        window = edt(() -> {
            MainWindow w = new MainWindow(client);
            w.setVisible(true);
            return w;
        });
        CrudPage page = go("turistas");
        assertEquals("error", edt(page::visibleState));
        assertTrue(edt(() -> SwingTestSupport.containsText(page, "Sem ligação")));
        waitFor("barra de estado", 10_000, () -> window.statusBar().getConnectionText().startsWith("API indisponível"));
        stub = null;
    }

    @Test
    void pesquisaFiltraSemAcentosEMostraSemResultados() throws Exception {
        stub = new StubApi().on("GET /turistas", 200, "[{\"idTurista\":\"TR-001\",\"nomeTurista\":\"João Mondlane\","
                + "\"nacionalidade\":\"Moçambicana\",\"totalReservas\":0},{\"idTurista\":\"TR-002\",\"nomeTurista\":\"Ana\","
                + "\"nacionalidade\":\"Portuguesa\",\"totalReservas\":1}]");
        open(stub);
        CrudPage page = go("turistas");
        JTextField search = edt(() -> find(page, "pesquisa", JTextField.class));
        edt(() -> search.setText("mocambicana"));
        waitFor("filtro", 3_000, () -> page.table().getRowCount() == 1);
        edt(() -> search.setText("xyz"));
        waitFor("sem resultados", 3_000, () -> "nomatch".equals(page.visibleState()));
        edt(() -> search.setText(""));
        waitFor("todos", 3_000, () -> page.table().getRowCount() == 2);
    }

    @Test
    void formularioValidaNoClienteSemEnviarPedido() throws Exception {
        stub = new StubApi().on("GET /dominios", 200, "{\"DESTINO.REGIAO\":[\"Maputo\",\"Gaza\"]}");
        open(stub);
        go("destinos");
        new Thread(() -> edt(() -> ((CrudPage) window.page("destinos")).onNew())).start();
        waitFor("formulário", 5_000, () -> openDialog() != null && openDialog().isReady());
        FormDialog dialog = edt(this::openDialog);
        JTextField capacidade = (JTextField) edt(() -> dialog.inputFor("capacidadeVisitantes"));
        edt(() -> capacidade.setText("-5"));
        SwingTestSupport.screenshot(dialog.getRootPane(), "30-formulario-destino");
        edt(() -> dialog.saveButton().doClick());
        assertEquals("Campo obrigatório.", edt(() -> dialog.errorFor("nomeDestino")));
        assertEquals("Campo obrigatório.", edt(() -> dialog.errorFor("regiao")));
        assertEquals("Tem de ser maior que zero.", edt(() -> dialog.errorFor("capacidadeVisitantes")));
        SwingTestSupport.screenshot(dialog.getRootPane(), "31-formulario-erros");
        assertEquals(0, stub.count("POST /destinos"), "dados inválidos não são enviados");
    }

    @Test
    void errosDeCampoDevolvidosPelaApiAparecemNoFormulario() throws Exception {
        stub = new StubApi()
                .on("GET /dominios", 200, "{\"DESTINO.REGIAO\":[\"Maputo\",\"Gaza\"]}")
                .on("POST /destinos", 422, "{\"error\":\"Existem campos com valores inválidos.\",\"code\":\"VALIDACAO\","
                        + "\"fields\":{\"nomeDestino\":\"Máximo de 80 caracteres.\"}}");
        open(stub);
        go("destinos");
        new Thread(() -> edt(() -> ((CrudPage) window.page("destinos")).onNew())).start();
        waitFor("formulário", 5_000, () -> openDialog() != null && openDialog().isReady());
        FormDialog dialog = edt(this::openDialog);
        edt(() -> {
            ((JTextField) dialog.inputFor("nomeDestino")).setText("Nome");
            ((javax.swing.JComboBox<?>) dialog.inputFor("regiao")).setSelectedIndex(1);
            ((JTextField) dialog.inputFor("capacidadeVisitantes")).setText("50");
            dialog.saveButton().doClick();
        });
        waitFor("erro da API", 5_000, () -> "Máximo de 80 caracteres.".equals(dialog.errorFor("nomeDestino")));
        assertEquals(1, stub.count("POST /destinos"));
        assertTrue(edt(dialog::isVisible), "o formulário fica aberto para corrigir");
    }

    @Test
    void interfaceContinuaResponsivaDuranteUmPedidoLento() throws Exception {
        stub = new StubApi().slow("GET /operadores", "[]", 2_500);
        open(stub);
        edt(() -> window.navigate("operadores"));
        CrudPage page = (CrudPage) edt(() -> window.page("operadores"));
        waitFor("a carregar", 2_000, page::isLoading);
        long worst = 0;
        for (int i = 0; i < 20; i++) {
            long t0 = System.nanoTime();
            edt(() -> {
            });
            worst = Math.max(worst, (System.nanoTime() - t0) / 1_000_000);
            Thread.sleep(50);
        }
        assertTrue(worst < 250, "a EDT respondeu em no máximo " + worst + " ms enquanto a API demorava 2,5 s");
        assertTrue(edt(page::isLoading));
        Spinner spinner = edt(() -> findSpinner(page));
        assertNotNull(spinner);
        assertTrue(edt(spinner::isRunning), "indicador de carregamento animado");
        waitFor("fim do pedido", 6_000, () -> !page.isLoading());
        assertFalse(edt(spinner::isRunning), "o temporizador do indicador para quando deixa de estar visível");
    }

    @Test
    void temaEscuroETemaClaroAlternam() {
        stub = assertDoesNotFail(StubApi::new);
        open(stub);
        JButton theme = edt(() -> find(window.getContentPane(), "tema", JButton.class));
        edt(() -> theme.doClick());
        assertTrue(edt(Theme::isDark));
        assertEquals("Tema claro", edt(theme::getText));
        edt(() -> theme.doClick());
        assertFalse(edt(Theme::isDark));
    }

    private FormDialog openDialog() {
        for (Window w : Window.getWindows()) {
            if (w instanceof FormDialog && w.isShowing()) {
                return (FormDialog) w;
            }
        }
        return null;
    }

    private static JButton findButton(java.awt.Container c, String text) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText()) && child.isShowing()) {
                return (JButton) child;
            }
            if (child instanceof java.awt.Container) {
                JButton b = findButton((java.awt.Container) child, text);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static Spinner findSpinner(java.awt.Container c) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof Spinner && child.isShowing()) {
                return (Spinner) child;
            }
            if (child instanceof java.awt.Container) {
                Spinner s = findSpinner((java.awt.Container) child);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private static <T> T assertDoesNotFail(java.util.concurrent.Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
