package com.turismo.frontend;

import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.auth.AuthService;
import com.turismo.frontend.auth.AuthShell;
import com.turismo.frontend.auth.Session;
import com.turismo.frontend.auth.TokenStore;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Sidebar;
import com.turismo.frontend.ui.components.StatusBar;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.motion.MotionPanel;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Janela da aplicação. Mostra o fluxo de autenticação ({@link AuthShell}) até
 * haver uma sessão válida e só então cria a área de trabalho ({@link Workspace}).
 *
 * <ul>
 *   <li>Ao abrir, retoma a sessão guardada se o servidor ainda a aceitar.</li>
 *   <li>Terminar sessão descarta a área de trabalho (ecrãs, dados e janelas
 *   abertas) antes de voltar ao início de sessão, e revoga o token na API.</li>
 *   <li>Se a API responder que a sessão expirou, faz o mesmo e explica porquê.</li>
 * </ul>
 */
public class MainWindow extends JFrame {

    public static final String DASHBOARD = Workspace.DASHBOARD;
    private static final String AUTH = "auth";
    private static final String APP = "app";
    private static final Dimension AUTH_MIN = new Dimension(720, 600);
    private static final Dimension APP_MIN = new Dimension(1100, 700);

    private final ApiClient api;
    private final AuthService auth;
    private final CardLayout cards = new CardLayout();
    private final MotionPanel stage = new MotionPanel(cards);
    private final AuthShell authShell;
    private Workspace workspace;

    /** Janela normal: começa pela autenticação (ou retoma a sessão guardada). */
    public MainWindow(ApiClient api) {
        this(api, new AuthService(api, TokenStore.preferences()), true);
    }

    /** Com um serviço de autenticação próprio (ex.: testes com o token só em memória). */
    public MainWindow(ApiClient api, AuthService auth) {
        this(api, auth, true);
    }

    private MainWindow(ApiClient api, AuthService auth, boolean restore) {
        super("Gestão de Turismo");
        this.api = api;
        this.auth = auth;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(AUTH_MIN);
        setSize(new Dimension(1360, 860));
        setLocationRelativeTo(null);

        authShell = new AuthShell(auth, this::enter);
        stage.add(authShell, AUTH);
        setContentPane(stage);
        api.setSessionExpiredListener(() -> SwingUtilities.invokeLater(this::sessionExpired));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (workspace != null) {
                    workspace.start();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (workspace != null) {
                    workspace.dispose();
                }
            }
        });
        if (restore) {
            restoreSession();
        }
    }

    /**
     * Abre diretamente a área de trabalho de uma sessão já iniciada (usado nos
     * testes de interface, que não passam pelo ecrã de início de sessão).
     */
    public static MainWindow withSession(ApiClient api, AuthService auth) {
        MainWindow w = new MainWindow(api, auth, false);
        w.enter(auth.current());
        return w;
    }

    private void restoreSession() {
        authShell.show(AuthShell.RESTORING);
        Task.run(auth::restore, session -> {
            if (session != null) {
                enter(session);
            } else {
                authShell.show(AuthShell.WELCOME);
            }
        }, error -> {
            authShell.show(AuthShell.WELCOME);
            if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
                // A sessão guardada fica para quando a API voltar; o ecrã mostra o estado do servidor.
                return;
            }
            authShell.showLogin(null, UI.Tone.WARNING, "Não foi possível retomar a sessão: " + error.getMessage());
        });
    }

    /** Sessão aceite: cria a área de trabalho desta conta e mostra-a. */
    private void enter(Session session) {
        disposeWorkspace();
        workspace = new Workspace(api, auth, this::logout);
        stage.add(workspace, APP);
        setTitle("Gestão de Turismo — " + session.account().fullName());
        growTo(APP_MIN);
        cards.show(stage, APP);
        stage.reveal(Motion.SLOW, 0, 0, 14);
        if (isShowing()) {
            workspace.start();
        }
    }

    /** Termina a sessão: a interface sai logo e a revogação no servidor corre em segundo plano. */
    public void logout() {
        if (workspace == null) {
            return;
        }
        String username = auth.current() == null ? null : auth.current().account().username();
        leaveWorkspace();
        authShell.showLogin(username, UI.Tone.INFO, "Sessão terminada. Até breve!");
        Task.run(auth::logout, confirmed -> {
            if (!confirmed) {
                authShell.showLogin(username, UI.Tone.WARNING, "Sessão terminada neste computador. O servidor não "
                        + "respondeu: o acesso deixa de ser válido quando a sessão expirar.");
            }
        }, error -> { });
    }

    /** A API recusou o token (sessão expirada ou terminada noutro sítio). */
    private void sessionExpired() {
        if (workspace == null) {
            return;
        }
        String username = auth.current() == null ? null : auth.current().account().username();
        auth.forget();
        leaveWorkspace();
        authShell.showLogin(username, UI.Tone.WARNING, "A sua sessão expirou. Inicie sessão novamente para continuar.");
    }

    private void leaveWorkspace() {
        // Fecha formulários e diálogos abertos: nenhum dado da sessão fica visível.
        for (Window w : getOwnedWindows()) {
            w.dispose();
        }
        disposeWorkspace();
        setTitle("Gestão de Turismo");
        setMinimumSize(AUTH_MIN);
        cards.show(stage, AUTH);
        stage.reveal(Motion.NORMAL + 60, 0, 0, 10);
    }

    private void disposeWorkspace() {
        if (workspace != null) {
            workspace.dispose();
            stage.remove(workspace);
            workspace = null;
            stage.revalidate();
            stage.repaint();
        }
    }

    private void growTo(Dimension min) {
        setMinimumSize(min);
        if (getWidth() < min.width || getHeight() < min.height) {
            setSize(Math.max(getWidth(), min.width), Math.max(getHeight(), min.height));
        }
    }

    // --------------------------------------------------- acesso (testes e atalhos)

    public AuthShell authShell() {
        return authShell;
    }

    public Workspace workspace() {
        return workspace;
    }

    public boolean isAuthenticated() {
        return workspace != null;
    }

    public void navigate(String id) {
        workspace.navigate(id);
    }

    public String currentPage() {
        return workspace == null ? null : workspace.currentPage();
    }

    public JComponent page(String id) {
        return workspace == null ? null : workspace.page(id);
    }

    public Sidebar sidebar() {
        return workspace.sidebar();
    }

    public StatusBar statusBar() {
        return workspace.statusBar();
    }
}
