package com.turismo.frontend;

import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.api.ClientConfig;
import com.turismo.frontend.modules.CrudPage;
import com.turismo.frontend.pages.DashboardPage;
import com.turismo.frontend.ui.theme.Theme;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.swing.JTabbedPane;
import java.awt.Dimension;
import java.util.List;

import static com.turismo.frontend.SwingTestSupport.edt;
import static com.turismo.frontend.SwingTestSupport.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Teste ponta a ponta com a aplicação real: API do backend (config/turismo.properties)
 * + base de dados Oracle. Abre a janela verdadeira, percorre os 16 ecrãs e grava
 * capturas em target/screenshots. Só leitura: não cria nem altera registos.
 * Fica "skipped" se a API não estiver a correr.
 */
@Tag("integracao")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveApplicationE2ETest {

    private static ApiClient api;
    private static MainWindow window;

    @BeforeAll
    static void open() {
        api = new ApiClient(ClientConfig.load());
        try {
            JsonObject estado = api.getObject("/estado");
            assumeTrue("OK".equals(estado.get("bd").getAsString()), "BD sem ligação: " + estado);
        } catch (ApiException e) {
            assumeTrue(false, "API indisponível em " + api.baseUrl() + ": " + e.getMessage());
        }
        edt(() -> {
            Theme.install();
            window = new MainWindow(api);
            window.setSize(new Dimension(1440, 900));
            window.setVisible(true);
        });
    }

    @AfterAll
    static void close() {
        if (window != null) {
            edt(() -> window.dispose());
        }
    }

    @Test
    @Order(1)
    void painelMostraOsTotaisReaisDaBd() throws ApiException {
        DashboardPage page = (DashboardPage) edt(() -> window.page(MainWindow.DASHBOARD));
        waitFor("painel carregado", 20_000, () -> !page.isLoading() && !page.totalText("destinos").equals("…"));
        JsonObject totais = api.getObject("/dashboard").getAsJsonObject("totais");
        assertEquals(totais.get("destinos").getAsString(), edt(() -> page.totalText("destinos")));
        assertEquals(totais.get("turistas").getAsString(), edt(() -> page.totalText("turistas")));
        assertEquals(totais.get("reservas").getAsString(), edt(() -> page.totalText("reservas")));
        waitFor("barra de estado", 10_000, () -> window.statusBar().getConnectionText().startsWith("Ligado a"));
        SwingTestSupport.screenshot(window.getRootPane(), "00-painel");
    }

    @Test
    @Order(2)
    void todosOsModulosCarregamDadosReaisSemErro() throws ApiException {
        List<String> ids = edt(() -> window.sidebar().itemIds());
        assertEquals(16, ids.size(), "painel + 15 tabelas");
        int i = 1;
        for (String id : ids) {
            if (id.equals(MainWindow.DASHBOARD)) {
                continue;
            }
            edt(() -> window.navigate(id));
            CrudPage page = (CrudPage) edt(() -> window.page(id));
            waitFor("módulo " + id, 20_000, () -> page.hasLoaded() && !page.isLoading()
                    || "error".equals(page.visibleState()));
            String state = edt(page::visibleState);
            assertNotEquals("error", state, "o módulo " + id + " não devia falhar");
            int apiRows = api.getArray(page.spec().endpoint()).size();
            assertEquals(apiRows, (int) edt(() -> page.table().getRowCount()), "linhas de " + id);
            assertEquals(apiRows == 0 ? "empty" : "table", state, id);
            if (apiRows > 0) {
                edt(() -> page.table().setRowSelectionInterval(0, 0));
            }
            SwingTestSupport.screenshot(window.getRootPane(), String.format("%02d-%s", i++, id));
        }
    }

    @Test
    @Order(3)
    void relatoriosMostramOConteudoOuOErroRealDaView() throws ApiException {
        edt(() -> window.navigate("pagamentos"));
        CrudPage page = (CrudPage) edt(() -> window.page("pagamentos"));
        edt(() -> {
            JTabbedPane t = findTabs(page);
            t.setSelectedIndex(1);
        });
        boolean missing = api.getObject("/estado").getAsJsonArray("viewsEmFalta").toString().contains("v_recibos");
        waitFor("relatório", 15_000, () -> SwingTestSupport.containsText(page, missing ? "não existe na base de dados" : "Recibos"));
        SwingTestSupport.screenshot(window.getRootPane(), "20-relatorio-pagamentos");
    }

    @Test
    @Order(4)
    void temaEscuroAplicaSeSemErros() {
        edt(() -> window.navigate("destinos"));
        CrudPage page = (CrudPage) edt(() -> window.page("destinos"));
        waitFor("destinos", 15_000, () -> !page.isLoading());
        edt(() -> Theme.setDark(true));
        SwingTestSupport.screenshot(window.getRootPane(), "21-destinos-escuro");
        edt(() -> window.navigate(MainWindow.DASHBOARD));
        DashboardPage dash = (DashboardPage) edt(() -> window.page(MainWindow.DASHBOARD));
        waitFor("painel", 15_000, () -> !dash.isLoading());
        SwingTestSupport.screenshot(window.getRootPane(), "22-painel-escuro");
        edt(() -> Theme.setDark(false));
    }

    private static JTabbedPane findTabs(java.awt.Container c) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof JTabbedPane) {
                return (JTabbedPane) child;
            }
            if (child instanceof java.awt.Container) {
                JTabbedPane t = findTabs((java.awt.Container) child);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }
}
