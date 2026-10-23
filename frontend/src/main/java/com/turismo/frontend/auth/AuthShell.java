package com.turismo.frontend.auth;

import com.turismo.frontend.ui.components.Backdrop;
import com.turismo.frontend.ui.components.Spinner;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.motion.MotionPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluxo de autenticação: boas-vindas, início de sessão e registo, sobre o
 * mesmo fundo azul. A troca de ecrã é um fade com deslocamento lateral curto
 * (para a direita ao avançar, para a esquerda ao voltar); o fundo fica parado,
 * por isso a transição é leve e nunca bloqueia a interação.
 */
public class AuthShell extends JPanel {

    public static final String WELCOME = "boas-vindas";
    public static final String LOGIN = "entrar";
    public static final String REGISTER = "registo";
    public static final String RESTORING = "a-retomar";
    private static final List<String> ORDER = List.of(WELCOME, LOGIN, REGISTER);

    private final AuthService auth;
    private final Consumer<Session> onAuthenticated;
    private final Backdrop backdrop = new Backdrop(Backdrop.Style.EXPRESSIVE);
    private final CardLayout cards = new CardLayout();
    private final MotionPanel stage = new MotionPanel(cards);
    private final WelcomeView welcome;
    private final LoginView login;
    private final RegisterView register;
    private String current;

    public AuthShell(AuthService auth, Consumer<Session> onAuthenticated) {
        super(new BorderLayout());
        this.auth = auth;
        this.onAuthenticated = onAuthenticated;
        setName("autenticacao");
        setOpaque(true);
        welcome = new WelcomeView(this);
        login = new LoginView(this);
        register = new RegisterView(this);
        stage.add(welcome, WELCOME);
        stage.add(login, LOGIN);
        stage.add(register, REGISTER);
        stage.add(restoringView(), RESTORING);
        add(stage, BorderLayout.CENTER);
    }

    AuthService auth() {
        return auth;
    }

    void authenticated(Session session) {
        onAuthenticated.accept(session);
    }

    public String current() {
        return current;
    }

    public LoginView login() {
        return login;
    }

    public RegisterView register() {
        return register;
    }

    public WelcomeView welcome() {
        return welcome;
    }

    /** Mostra um ecrã do fluxo com a transição adequada à direção. */
    public void show(String id) {
        if (id.equals(current)) {
            return;
        }
        int from = current == null ? -1 : ORDER.indexOf(current);
        int to = ORDER.indexOf(id);
        float dx = current == null || from < 0 || to < 0 ? 0 : (to > from ? 28 : -28);
        current = id;
        cards.show(stage, id);
        stage.reveal(Motion.NORMAL + 40, 0, dx, dx == 0 ? 12 : 0);
        Component view = visibleView();
        if (view instanceof Shown) {
            ((Shown) view).onShown();
        }
    }

    /** Início de sessão, com o nome já escrito e uma mensagem opcional (ex.: "conta criada"). */
    public void showLogin(String username, UI.Tone tone, String notice) {
        login.prepare(username, tone, notice);
        show(LOGIN);
    }

    private Component visibleView() {
        for (Component c : stage.getComponents()) {
            if (c.isVisible()) {
                return c;
            }
        }
        return null;
    }

    /** Ecrã breve enquanto se confirma no servidor a sessão guardada. */
    private static JComponent restoringView() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Spinner spinner = new Spinner(32);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(spinner);
        col.add(Box.createVerticalStrut(UI.S4));
        JLabel text = AuthLayout.onDark("A retomar a sessão…", com.turismo.frontend.ui.theme.Theme.body()
                .deriveFont(15f), 220);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(text);
        p.add(col);
        return p;
    }

    @Override
    public Color getBackground() {
        return com.turismo.frontend.ui.theme.Theme.p().brandDeep;
    }

    @Override
    protected void paintComponent(Graphics g) {
        backdrop.paint(g, getWidth(), getHeight());
    }

    /** Ecrãs que querem saber quando passam a ser os visíveis (ex.: para pôr o foco). */
    interface Shown {
        void onShown();
    }
}
