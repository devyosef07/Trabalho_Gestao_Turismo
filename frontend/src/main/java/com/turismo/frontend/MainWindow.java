package com.turismo.frontend;

import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.modules.CrudPage;
import com.turismo.frontend.modules.Lookups;
import com.turismo.frontend.modules.ModuleSpec;
import com.turismo.frontend.modules.Modules;
import com.turismo.frontend.modules.Page;
import com.turismo.frontend.pages.DashboardPage;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Sidebar;
import com.turismo.frontend.ui.components.StatusBar;
import com.turismo.frontend.ui.components.Toast;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Janela principal: navegação lateral, ecrãs (criados só quando são abertos
 * pela primeira vez) e barra de estado com a ligação à base de dados.
 */
public class MainWindow extends JFrame {

    public static final String DASHBOARD = "dashboard";
    private static final int HEALTH_INTERVAL_MS = 60_000;

    private final ApiClient api;
    private final Lookups lookups;
    private final Map<String, ModuleSpec> specs = new LinkedHashMap<>();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final StatusBar status = new StatusBar();
    private final Timer healthTimer;
    private String current;

    public MainWindow(ApiClient api) {
        super("Gestão de Turismo");
        this.api = api;
        this.lookups = new Lookups(api);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(new Dimension(1360, 860));
        setLocationRelativeTo(null);

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
        sidebar.setFooter(buildFooter());

        content.setOpaque(false);
        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);

        Toast.setStatusListener(status::setMessage);
        bindShortcuts();

        healthTimer = new Timer(HEALTH_INTERVAL_MS, e -> checkHealth());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                currentPageObject().focusDefault();
                checkHealth();
                healthTimer.start();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                healthTimer.stop();
            }
        });
        navigate(DASHBOARD);
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
        updateThemeButton(theme);
        theme.addActionListener(e -> {
            Theme.setDark(!Theme.isDark());
            updateThemeButton(theme);
        });
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(theme, BorderLayout.WEST);
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

    /** Abre um ecrã (cria-o na primeira vez) e atualiza os seus dados. */
    public void navigate(String id) {
        JComponent page = pages.get(id);
        if (page == null) {
            page = DASHBOARD.equals(id) ? new DashboardPage(api, this::navigate) : new CrudPage(specs.get(id), lookups);
            pages.put(id, page);
            content.add(page, id);
        }
        current = id;
        sidebar.setActive(id);
        cards.show(content, id);
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

    private Page currentPageObject() {
        return (Page) pages.get(current);
    }

    private void bindShortcuts() {
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JRootPane root = getRootPane();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, menu), "novo", () -> currentPageObject().onNew());
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "pesquisar", () -> currentPageObject().onSearch());
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "atualizar", () -> currentPageObject().onRefresh());
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_R, menu), "atualizar2", () -> currentPageObject().onRefresh());
    }

    private static void bind(JRootPane root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
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
