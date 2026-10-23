package com.turismo.frontend;

import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ClientConfig;
import com.turismo.frontend.auth.AuthService;
import com.turismo.frontend.auth.AuthShell;
import com.turismo.frontend.auth.LoginView;
import com.turismo.frontend.auth.RegisterView;
import com.turismo.frontend.auth.TokenStore;
import com.turismo.frontend.pages.AccountPage;
import com.turismo.frontend.pages.DashboardPage;
import com.turismo.frontend.ui.theme.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import java.awt.Dimension;
import java.awt.Window;
import java.time.Duration;
import java.time.LocalDate;

import static com.turismo.frontend.SwingTestSupport.edt;
import static com.turismo.frontend.SwingTestSupport.find;
import static com.turismo.frontend.SwingTestSupport.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Fluxo de autenticação na janela real contra uma API falsa: boas-vindas,
 * registo (validação e sucesso), início de sessão (erro e sucesso), sessão
 * guardada, sessão expirada e terminar sessão. Não precisa da base de dados.
 */
class AuthFlowUiTest {

    private StubApi stub;
    private MainWindow window;
    private TokenStore store;

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

    private MainWindow open(StubApi api, String savedToken) {
        ApiClient client = new ApiClient(new ClientConfig(api.baseUrl(), Duration.ofSeconds(10)));
        store = TokenStore.memory();
        if (savedToken != null) {
            store.save(savedToken);
        }
        AuthService auth = new AuthService(client, store);
        window = edt(() -> {
            MainWindow w = new MainWindow(client, auth);
            w.setSize(new Dimension(1280, 820));
            w.setVisible(true);
            return w;
        });
        return window;
    }

    /** O AuthShell é um campo final da janela: pode ser lido fora da EDT. */
    private AuthShell shell() {
        return window.authShell();
    }

    private LoginView goToLogin() {
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        JButton enter = edt(() -> find(window.authShell(), "boas-vindas-entrar", JButton.class));
        edt(() -> enter.doClick());
        assertEquals(AuthShell.LOGIN, edt(() -> shell().current()));
        return edt(() -> shell().login());
    }

    private void signIn(LoginView login) {
        edt(() -> {
            login.usernameField().setText("maria.cossa");
            login.passwordField().setText("segura123");
            login.submitButton().doClick();
        });
        waitFor("área de trabalho", 5_000, () -> window.isAuthenticated());
    }

    @Test
    void semSessaoGuardadaAbreNasBoasVindasEMostraOEstadoDoServidor() throws Exception {
        stub = new StubApi().on("GET /estado", 200, "{\"bd\":\"OK\"}");
        open(stub, null);
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        assertFalse(edt(window::isAuthenticated));
        waitFor("estado do servidor", 5_000,
                () -> window.authShell().welcome().serverText().equals("Servidor e base de dados disponíveis"));
        SwingTestSupport.screenshot(window.getRootPane(), "40-boas-vindas");
        assertEquals(0, stub.count("GET /destinos"), "nenhum dado é pedido antes de iniciar sessão");
    }

    @Test
    void registoValidaOsCamposSemEnviarPedido() throws Exception {
        stub = new StubApi();
        open(stub, null);
        goToLogin();
        JButton create = edt(() -> find(shell(), "ir-criar-conta", JButton.class));
        edt(() -> create.doClick());
        RegisterView reg = edt(() -> shell().register());
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assumeTrue(tomorrow.getYear() == LocalDate.now().getYear(), "a lista de anos só vai até ao ano atual");
        edt(() -> {
            reg.nameInput().setText("Maria");
            reg.nameInput().transferFocus();
            reg.birthInput().setDate(tomorrow);
            reg.passwordInput().setText("abc");
            reg.confirmInput().setText("abd");
            reg.confirmInput().transferFocus();
        });
        waitFor("erros", 3_000, () -> reg.errorText("confirmacao") != null);
        assertEquals("Indique o nome e o apelido.", edt(() -> reg.errorText("nome")));
        assertEquals("A data de nascimento não pode ser no futuro.", edt(() -> reg.errorText("nascimento")));
        assertEquals("As palavras-passe não coincidem.", edt(() -> reg.errorText("confirmacao")));
        assertFalse(edt(() -> reg.submitButton().isEnabled()), "o envio fica desativado com dados inválidos");
        edt(() -> reg.passwordInput().transferFocus());
        SwingTestSupport.screenshot(window.getRootPane(), "42-registo-erros");
        assertEquals(0, stub.count("POST /auth/registo"));
    }

    @Test
    void registoComSucessoMostraONomeDeUtilizadorELevaAoLogin() throws Exception {
        stub = new StubApi().on("POST /auth/registo", 201, "{\"message\":\"Conta criada com sucesso.\",\"data\":"
                + "{\"nomeUtilizador\":\"maria.cossa\",\"nomeCompleto\":\"Maria José Cossa\",\"dataNascimento\":\"1995-03-14\"}}");
        open(stub, null);
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        JButton create = edt(() -> find(shell(), "boas-vindas-criar", JButton.class));
        edt(() -> create.doClick());
        RegisterView reg = edt(() -> shell().register());
        edt(() -> {
            reg.nameInput().setText("Maria José Cossa");
            reg.birthInput().setDate(LocalDate.of(1995, 3, 14));
            reg.passwordInput().setText("segura123");
            reg.confirmInput().setText("segura123");
        });
        SwingTestSupport.screenshot(window.getRootPane(), "41-registo");
        assertTrue(edt(() -> reg.submitButton().isEnabled()));
        edt(() -> reg.submitButton().doClick());
        waitFor("sucesso", 5_000, reg::isShowingSuccess);
        assertEquals("maria.cossa", edt(reg::createdUsername));
        Thread.sleep(700);
        SwingTestSupport.screenshot(window.getRootPane(), "43-registo-sucesso");
        JButton toLogin = edt(() -> find(reg, "sucesso-entrar", JButton.class));
        edt(() -> toLogin.doClick());
        LoginView login = edt(() -> shell().login());
        assertEquals(AuthShell.LOGIN, edt(() -> shell().current()));
        assertEquals("maria.cossa", edt(() -> login.usernameField().getText()));
        assertNotNull(edt(login::alertText));
        assertTrue(stub.requests.contains("POST /auth/registo"));
    }

    @Test
    void erroDeCampoDaApiApareceNoCampoEMantemOsDados() throws Exception {
        stub = new StubApi().on("POST /auth/registo", 422, "{\"error\":\"Existem campos com valores inválidos.\","
                + "\"code\":\"VALIDACAO\",\"fields\":{\"dataNascimento\":\"É preciso ter pelo menos 13 anos.\"}}");
        open(stub, null);
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        edt(() -> shell().show(AuthShell.REGISTER));
        RegisterView reg = edt(() -> shell().register());
        edt(() -> {
            reg.nameInput().setText("Maria José Cossa");
            reg.birthInput().setDate(LocalDate.of(1995, 3, 14));
            reg.passwordInput().setText("segura123");
            reg.confirmInput().setText("segura123");
            reg.submitButton().doClick();
        });
        waitFor("erro da API", 5_000, () -> "É preciso ter pelo menos 13 anos.".equals(reg.errorText("nascimento")));
        assertEquals("Maria José Cossa", edt(() -> reg.nameInput().getText()), "o nome escrito mantém-se");
        assertEquals("segura123", edt(() -> new String(reg.passwordInput().getPassword())));
        assertFalse(edt(reg::isShowingSuccess));
    }

    @Test
    void loginErradoMostraAMensagemDaApiEMantemONome() throws Exception {
        stub = new StubApi().on("POST /auth/sessao", 401,
                "{\"error\":\"Nome de utilizador ou palavra-passe incorretos.\",\"code\":\"CREDENCIAIS_INVALIDAS\"}");
        open(stub, null);
        LoginView login = goToLogin();
        assertFalse(edt(() -> login.submitButton().isEnabled()), "sem dados o botão está desativado");
        edt(() -> {
            login.usernameField().setText("maria.cossa");
            login.passwordField().setText("errada123");
            login.submitButton().doClick();
        });
        waitFor("mensagem de erro", 5_000,
                () -> "Nome de utilizador ou palavra-passe incorretos.".equals(login.alertText()));
        assertFalse(edt(window::isAuthenticated));
        assertEquals("maria.cossa", edt(() -> login.usernameField().getText()));
        assertNull(store.load());
        SwingTestSupport.screenshot(window.getRootPane(), "44-login-erro");
    }

    @Test
    void loginCertoAbreAAreaDeTrabalhoDaContaEEnviaOToken() throws Exception {
        stub = new StubApi().on("POST /auth/sessao", 201, TestSessions.SESSION_JSON)
                .on("GET /auth/sessao", 200, TestSessions.SESSION_JSON);
        open(stub, null);
        signIn(goToLogin());
        Workspace ws = edt(window::workspace);
        assertEquals("Maria José Cossa", edt(() -> ws.accountChip().displayedName()));
        assertEquals("MC", edt(() -> ws.accountChip().initials()));
        assertEquals(TestSessions.TOKEN, store.load(), "\"manter sessão\" guarda o token");
        assertTrue(edt(window::getTitle).contains("Maria José Cossa"));
        DashboardPage dash = (DashboardPage) edt(() -> window.page(MainWindow.DASHBOARD));
        assertTrue(edt(dash::greetingText).endsWith(", Maria"));
        waitFor("pedido do painel", 5_000, () -> stub.requests.contains("GET /dashboard"));
        int i = stub.requests.indexOf("GET /dashboard");
        assertEquals("Bearer " + TestSessions.TOKEN, stub.authorizations.get(i));

        edt(() -> window.navigate(Workspace.ACCOUNT));
        AccountPage account = (AccountPage) edt(() -> window.page(Workspace.ACCOUNT));
        waitFor("dados da conta", 5_000, () -> account.valueText("nomeUtilizador").equals("maria.cossa"));
        assertTrue(edt(() -> account.valueText("dataNascimento")).startsWith("14 de março de 1995"));
        Thread.sleep(500);
        SwingTestSupport.screenshot(window.getRootPane(), "45-conta");
    }

    @Test
    void terminarSessaoVoltaAoLoginRevogaOTokenENaoDeixaDados() throws Exception {
        stub = new StubApi().on("POST /auth/sessao", 201, TestSessions.SESSION_JSON)
                .on("DELETE /auth/sessao", 200, "{\"message\":\"Sessão terminada.\"}");
        open(stub, null);
        signIn(goToLogin());
        Workspace ws = edt(window::workspace);
        JMenuItem logout = edt(() -> menuItem(ws, "menu-terminar-sessao"));
        edt(() -> logout.doClick());
        assertFalse(edt(window::isAuthenticated));
        assertEquals(AuthShell.LOGIN, edt(() -> shell().current()));
        assertNull(edt(() -> window.page(MainWindow.DASHBOARD)), "os ecrãs da sessão foram descartados");
        assertNull(store.load(), "o token guardado foi apagado");
        assertEquals("Gestão de Turismo", edt(window::getTitle));
        waitFor("revogação no servidor", 5_000, () -> stub.count("DELETE /auth/sessao") == 1);
        int i = stub.requests.indexOf("DELETE /auth/sessao");
        assertEquals("Bearer " + TestSessions.TOKEN, stub.authorizations.get(i));
        assertEquals("maria.cossa", edt(() -> shell().login().usernameField().getText()));
        assertEquals("", edt(() -> new String(shell().login().passwordField().getPassword())));
    }

    @Test
    void sessaoGuardadaValidaEntraDiretamente() throws Exception {
        stub = new StubApi().on("GET /auth/sessao", 200, TestSessions.SESSION_JSON);
        open(stub, TestSessions.TOKEN);
        waitFor("sessão retomada", 5_000, () -> window.isAuthenticated());
        assertEquals("Bearer " + TestSessions.TOKEN, stub.authorizations.get(stub.requests.indexOf("GET /auth/sessao")));
    }

    @Test
    void sessaoGuardadaRecusadaVaiParaAsBoasVindasEApagaOToken() throws Exception {
        stub = new StubApi().on("GET /auth/sessao", 401,
                "{\"error\":\"A sessão terminou ou é inválida.\",\"code\":\"SESSAO_INVALIDA\"}");
        open(stub, "token-antigo");
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        assertFalse(edt(window::isAuthenticated));
        assertNull(store.load());
    }

    @Test
    void sessaoQueExpiraDuranteOUsoVoltaAoLoginComAviso() throws Exception {
        stub = new StubApi().on("POST /auth/sessao", 201, TestSessions.SESSION_JSON)
                .on("GET /destinos", 401, "{\"error\":\"A sessão terminou ou é inválida.\",\"code\":\"SESSAO_INVALIDA\"}");
        open(stub, null);
        signIn(goToLogin());
        edt(() -> window.navigate("destinos"));
        waitFor("volta ao login", 5_000, () -> !window.isAuthenticated());
        assertEquals(AuthShell.LOGIN, edt(() -> shell().current()));
        assertTrue(edt(() -> shell().login().alertText()).contains("expirou"));
        assertNull(store.load());
    }

    @Test
    void autenticacaoAdaptaSeAJanelasEstreitas() throws Exception {
        stub = new StubApi();
        open(stub, null);
        LoginView login = goToLogin();
        edt(() -> window.validate());
        assertTrue(edt(login::isBrandVisible), "janela larga: marca + formulário");
        SwingTestSupport.screenshot(window.getRootPane(), "46-login");
        edt(() -> {
            window.setSize(new Dimension(760, 640));
            window.validate();
        });
        waitFor("layout estreito", 3_000, () -> !login.isBrandVisible());
        java.awt.Rectangle form = edt(login::formBounds);
        assertTrue(form.x >= 0 && form.x + form.width <= 760, "o formulário cabe na largura: " + form);
        SwingTestSupport.screenshot(window.getRootPane(), "47-login-estreito");
    }

    @Test
    void areaDeTrabalhoNaoAbreSemSessao() {
        stub = assertNoThrow(StubApi::new);
        open(stub, null);
        waitFor("boas-vindas", 5_000, () -> AuthShell.WELCOME.equals(window.authShell().current()));
        assertNull(edt(window::workspace));
        assertNull(edt(() -> window.page("destinos")));
        assertEquals(0, stub.requests.stream().filter(r -> !r.equals("GET /estado")).count(),
                "só o estado público do servidor é consultado");
    }

    private static JMenuItem menuItem(Workspace ws, String name) {
        for (java.awt.Component c : ws.accountChip().menu().getComponents()) {
            if (c instanceof JMenuItem && name.equals(c.getName())) {
                return (JMenuItem) c;
            }
        }
        throw new AssertionError("item " + name + " não encontrado");
    }

    private static <T> T assertNoThrow(java.util.concurrent.Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
