package com.turismo.frontend;

import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.auth.AuthService;
import com.turismo.frontend.auth.Session;
import com.turismo.frontend.modules.CrudPage;
import com.turismo.frontend.modules.Lookups;
import com.turismo.frontend.modules.ModuleSpec;
import com.turismo.frontend.modules.Modules;
import com.turismo.frontend.modules.Page;
import com.turismo.frontend.pages.AccountPage;
import com.turismo.frontend.pages.DashboardPage;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.AccountChip;
import com.turismo.frontend.ui.components.Sidebar;
import com.turismo.frontend.ui.components.StatusBar;
import com.turismo.frontend.ui.components.Toast;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.motion.MotionPanel;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Área de trabalho de uma sessão iniciada: navegação lateral (com a conta no
 * rodapé), ecrãs (criados só quando são abertos pela primeira vez, com uma
 * entrada em fade) e barra de estado com a ligação à base de dados.
 *
 * Cada sessão tem a sua área de trabalho; ao terminar a sessão é descartada
 * por inteiro ({@link #dispose()}), por isso nenhum dado fica no ecrã nem em
 * memória para a pessoa seguinte.
 */
public class Workspace extends JPanel {

    public static final String DASHBOARD = "dashboard";
    public static final String ACCOUNT = "conta";
    private static final int HEALTH_INTERVAL_MS = 60_000;

    private final ApiClient api;
    private final AuthService auth;
    private final Lookups lookups;
    private final Map<String, ModuleSpec> specs = new LinkedHashMap<>();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final MotionPanel content = new MotionPanel(cards);
    private final Sidebar sidebar;
    private final AccountChip account;
    private final StatusBar status = new StatusBar();
    private final Timer healthTimer;
    private final Runnable onLogout;
    private Runnable themeListener;
    private String current;

    public Workspace(ApiClient api, AuthService auth, Runnable onLogout) {
        super(new BorderLayout());
        this.api = api;
        this.auth = auth;
        this.onLogout = onLogout;
        this.lookups = new Lookups(api);
        setName("area-de-trabalho");

        for (ModuleSpec s : Modules.all()) {
            specs.put(s.id(), s);
        }

        sidebar = new Sidebar("Gestão de Turismo", "Moçambique", this::navigate);
        sidebar.setName("navegacao");
        sidebar.addGroup("Visão geral");
        sidebar.addItem(DASHBOARD, "Painel", MaterialDesignV.VIEW_DASHBOARD_OUTLINE);
        group("Turismo", "destinos", "atracoes", "alojamentos", "tiposAlojamento", "transportes");
        group("Pessoas", "turistas", "preferencias", "operadores");
        group("Reservas e pagamentos", "reservas", "pagamentos");
        group("Relações e atividade", "visitas", "reservaAlojamentos", "operadorTuristas", "operadorTransportes",
                "operadorAlojamentos");
        account = new AccountChip(() -> navigate(ACCOUNT), onLogout);
        account.setName("conta-menu");
        sidebar.setFooter(buildFooter());

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        Toast.setStatusListener(status::setMessage);
        bindShortcuts();
        healthTimer = new Timer(HEALTH_INTERVAL_MS, e -> checkHealth());
        showSession(auth.current());
        navigate(DASHBOARD, false);
    }

    /** Arranca o que depende de a área estar visível (estado da BD e foco inicial). */
    public void start() {
        Page page = currentPageObject();
        if (page != null) {
            page.focusDefault();
        }
        checkHealth();
        healthTimer.start();
    }

    /** Para os temporizadores e retira os ecrãs (e os seus dados) da memória. */
    public void dispose() {
        healthTimer.stop();
        Theme.removeListener(themeListener);
        Toast.setStatusListener(null);
        content.removeAll();
        pages.clear();
    }

    private void group(String title, String... ids) {
        sidebar.addGroup(title);
        for (String id : ids) {
            ModuleSpec s = specs.get(id);
            sidebar.addItem(id, s.title(), s.icon());
        }
    }

    private JComponent buildFooter() {
        JButton theme = new JButton();
        theme.setName("tema");
        theme.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        theme.setHorizontalAlignment(JButton.LEFT);
        updateThemeButton(theme);
        theme.addActionListener(e -> Theme.setDark(!Theme.isDark()));
        themeListener = () -> updateThemeButton(theme);
        Theme.addListener(themeListener);
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        theme.setAlignmentX(Component.LEFT_ALIGNMENT);
        account.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(theme);
        p.add(javax.swing.Box.createVerticalStrut(6));
        p.add(account);
        return p;
    }

    private static void updateThemeButton(JButton b) {
        boolean dark = Theme.isDark();
        b.setText(dark ? "Tema claro" : "Tema escuro");
        b.setIcon(Icons.of(dark ? MaterialDesignW.WHITE_BALANCE_SUNNY : MaterialDesignW.WEATHER_NIGHT, 16,
                () -> Theme.p().sidebarText));
        b.setForeground(Theme.p().sidebarText);
        b.setToolTipText("Mudar para o " + (dark ? "tema claro" : "tema escuro"));
    }

    /** Atualiza o nome e as iniciais da conta no rodapé e no painel. */
    public void showSession(Session session) {
        account.setPerson(session == null ? null : session.account().fullName(),
                session == null ? null : session.account().username());
        JComponent dashboard = pages.get(DASHBOARD);
        if (dashboard instanceof DashboardPage && session != null) {
            ((DashboardPage) dashboard).setGreeting(session.account().fullName());
        }
    }

    /** Abre um ecrã (cria-o na primeira vez), com a entrada animada, e atualiza os seus dados. */
    public void navigate(String id) {
        navigate(id, true);
    }

    private void navigate(String id, boolean animate) {
        JComponent page = pages.get(id);
        if (page == null) {
            if (DASHBOARD.equals(id)) {
                DashboardPage dashboard = new DashboardPage(api, this::navigate);
                if (auth.current() != null) {
                    dashboard.setGreeting(auth.current().account().fullName());
                }
                page = dashboard;
            } else if (ACCOUNT.equals(id)) {
                page = new AccountPage(auth, onLogout);
            } else {
                page = new CrudPage(specs.get(id), lookups);
            }
            pages.put(id, page);
            content.add(page, id);
        }
        boolean changed = !id.equals(current);
        current = id;
        sidebar.setActive(id);
        account.setActive(ACCOUNT.equals(id));
        cards.show(content, id);
        if (animate && changed) {
            content.reveal(Motion.NORMAL, 0, 0, 8);
        }
        ((Page) page).onShow();
    }

    public String currentPage() {
        return current;
    }

    public JComponent page(String id) {
        return pages.get(id);
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public StatusBar statusBar() {
        return status;
    }

    public AccountChip accountChip() {
        return account;
    }

    private Page currentPageObject() {
        return (Page) pages.get(current);
    }

    private void bindShortcuts() {
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_N, menu), "novo", () -> currentPageObject().onNew());
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "pesquisar", () -> currentPageObject().onSearch());
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "atualizar", () -> currentPageObject().onRefresh());
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, menu), "atualizar2", () -> currentPageObject().onRefresh());
    }

    private void bind(KeyStroke key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** Consulta /api/estado em segundo plano e atualiza a barra de estado. */
    void checkHealth() {
        Task.run(() -> api.getObject("/estado"), this::showHealth, error -> {
            if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
                status.setConnection("API indisponível em " + api.baseUrl(), MaterialDesignC.CLOUD_OFF_OUTLINE,
                        () -> Theme.p().danger);
            } else {
                status.setConnection("Erro ao consultar o estado: " + error.getMessage(),
                        MaterialDesignD.DATABASE_ALERT_OUTLINE, () -> Theme.p().danger);
            }
            status.setSchemaWarning(null, null, null);
        });
    }

    private void showHealth(JsonObject estado) {
        if (!"OK".equals(estado.get("bd").getAsString())) {
            status.setConnection("Base de dados sem ligação: " + estado.get("bdErro").getAsString(),
                    MaterialDesignD.DATABASE_ALERT_OUTLINE, () -> Theme.p().danger);
            status.setSchemaWarning(null, null, null);
            return;
        }
        String db = estado.get("database").getAsString().replace("Oracle Database ", "Oracle ");
        status.setConnection("Ligado a " + db.replace(" - Production", ""), MaterialDesignD.DATABASE_CHECK_OUTLINE,
                () -> Theme.p().success);
        int invalid = estado.getAsJsonArray("objetosInvalidos").size();
        int missing = estado.getAsJsonArray("viewsEmFalta").size();
        if (invalid + missing == 0) {
            status.setSchemaWarning(null, null, null);
        } else {
            String text = (invalid > 0 ? invalid + " objetos inválidos" : "")
                    + (invalid > 0 && missing > 0 ? ", " : "")
                    + (missing > 0 ? missing + " views em falta" : "");
            status.setSchemaWarning(text, MaterialDesignD.DATABASE_ALERT_OUTLINE, () -> navigate(DASHBOARD));
        }
    }
}
