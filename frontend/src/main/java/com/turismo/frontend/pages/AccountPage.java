package com.turismo.frontend.pages;

import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.auth.AuthService;
import com.turismo.frontend.auth.Session;
import com.turismo.frontend.modules.Format;
import com.turismo.frontend.modules.Page;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Avatar;
import com.turismo.frontend.ui.components.Badge;
import com.turismo.frontend.ui.components.Card;
import com.turismo.frontend.ui.components.Scroll;
import com.turismo.frontend.ui.components.Surface;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "A minha conta": os dados reais da conta com sessão iniciada (lidos de
 * GET /api/auth/sessao sempre que o ecrã é aberto) e a opção de terminar a
 * sessão. Não mostra nada inventado: um valor que a API não devolve aparece
 * como "—".
 */
public class AccountPage extends Surface implements Page {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Format.PT);
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm",
            Format.PT);

    private final AuthService auth;
    private final Avatar avatar = new Avatar(76, () -> Theme.p().card);
    private final JLabel name = UI.text(" ", () -> Theme.title().deriveFont(26f), () -> Theme.p().text);
    private final JLabel handle = UI.secondary(" ");
    private final Map<String, JLabel> values = new LinkedHashMap<>();
    private final JLabel refreshNote = UI.muted(" ");
    private final Card hero;
    private final Card details;
    private final Card security;
    private boolean revealed;

    public AccountPage(AuthService auth, Runnable onLogout) {
        super(new BorderLayout());
        this.auth = auth;
        setName("page-conta");
        setBorder(UI.pad(UI.S5, UI.S6, UI.S5, UI.S6));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UI.title("A minha conta"));
        header.add(Box.createVerticalStrut(2));
        header.add(UI.secondary("Dados da conta com sessão iniciada neste computador."));
        header.setBorder(UI.pad(0, 0, UI.S5, 0));
        add(header, BorderLayout.NORTH);

        JPanel content = new Scroll.WidthTrackingPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, UI.S4, 0);

        hero = buildHero();
        details = buildDetails();
        security = buildSecurity(onLogout);
        c.gridy = 0;
        content.add(hero, c);
        c.gridy = 1;
        content.add(details, c);
        c.gridy = 2;
        content.add(security, c);
        c.gridy = 3;
        c.weighty = 1;
        content.add(Box.createGlue(), c);
        add(Scroll.vertical(content), BorderLayout.CENTER);
    }

    private Card buildHero() {
        Card card = new Card(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Faixa em gradiente no topo do cartão
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(2, 1, getWidth() - 5, getHeight() - 6, 14, 14));
                g2.setPaint(new GradientPaint(0, 0, Theme.p().brandMid, getWidth(), 0, Theme.p().brandBright));
                g2.fillRect(0, 0, getWidth(), 64);
                g2.setColor(new Color(255, 255, 255, 30));
                for (int i = 0; i < 3; i++) {
                    int r = 90 + i * 46;
                    g2.drawOval(getWidth() - r + 30, -r / 2, r, r);
                }
                g2.dispose();
            }
        };
        card.setName("conta-cartao");
        card.setBorder(UI.pad(36, UI.S5, UI.S5, UI.S5));
        JPanel row = new JPanel(new BorderLayout(UI.S5, 0));
        row.setOpaque(false);
        JPanel avatarHolder = new JPanel(new BorderLayout());
        avatarHolder.setOpaque(false);
        avatarHolder.add(avatar, BorderLayout.NORTH);
        row.add(avatarHolder, BorderLayout.WEST);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.setBorder(UI.pad(34, 0, 0, 0));
        name.setName("conta-nome");
        texts.add(name);
        texts.add(Box.createVerticalStrut(2));
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        line.setOpaque(false);
        line.add(handle);
        line.add(Box.createHorizontalStrut(UI.S3));
        Badge active = new Badge();
        active.set("Sessão ativa", UI.Tone.SUCCESS);
        line.add(active);
        texts.add(line);
        for (Component comp : texts.getComponents()) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        row.add(texts, BorderLayout.CENTER);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private Card buildDetails() {
        Card card = new Card(new BorderLayout(0, UI.S4));
        card.setBorder(UI.pad(UI.S5));
        card.add(UI.section("Dados da conta"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 2, UI.S5, UI.S4));
        grid.setOpaque(false);
        grid.add(value("nomeCompleto", "Nome completo"));
        grid.add(value("nomeUtilizador", "Nome de utilizador"));
        grid.add(value("dataNascimento", "Data de nascimento"));
        grid.add(value("criadaEm", "Conta criada em"));
        grid.add(value("iniciadaEm", "Sessão iniciada em"));
        grid.add(value("expiraEm", "Sessão válida até"));
        card.add(grid, BorderLayout.CENTER);
        card.add(refreshNote, BorderLayout.SOUTH);
        return card;
    }

    private JComponent value(String key, String label) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(UI.stretch(UI.fieldLabel(label)));
        p.add(Box.createVerticalStrut(3));
        JLabel v = UI.stretch(UI.text(Format.EMPTY, Theme::strong, () -> Theme.p().text));
        v.setName("conta-" + key);
        values.put(key, v);
        p.add(v);
        return p;
    }

    private Card buildSecurity(Runnable onLogout) {
        Card card = new Card(new BorderLayout(UI.S4, 0));
        card.setBorder(UI.pad(UI.S5));
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(UI.section("Sessão"));
        texts.add(Box.createVerticalStrut(UI.S1));
        texts.add(UI.secondary("<html>Terminar a sessão fecha a área de trabalho e apaga o acesso guardado "
                + "neste computador. A conta e os seus dados mantêm-se.</html>"));
        card.add(texts, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UI.S2, 0));
        actions.setOpaque(false);
        JButton refresh = UI.secondary("Atualizar", MaterialDesignR.REFRESH);
        refresh.addActionListener(e -> onRefresh());
        JButton logout = UI.danger("Terminar sessão", MaterialDesignL.LOGOUT);
        logout.setName("conta-terminar-sessao");
        logout.addActionListener(e -> onLogout.run());
        actions.add(refresh);
        actions.add(logout);
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(actions);
        card.add(holder, BorderLayout.EAST);
        return card;
    }

    /** Mostra a sessão já conhecida e pede à API os dados atualizados. */
    @Override
    public void onShow() {
        Session s = auth.current();
        if (s != null) {
            render(s);
        }
        if (!revealed) {
            revealed = true;
            hero.reveal(0);
            details.reveal(70);
            security.reveal(140);
        }
        onRefresh();
    }

    @Override
    public void onRefresh() {
        refreshNote.setText("A atualizar…");
        Task.run(auth::refresh, s -> {
            render(s);
            refreshNote.setText(" ");
        }, error -> refreshNote.setText(error instanceof ApiException && ((ApiException) error).isUnreachable()
                ? "Sem ligação ao servidor: a mostrar os dados do início de sessão."
                : "Não foi possível atualizar: " + error.getMessage()));
    }

    void render(Session s) {
        Session.Account a = s.account();
        avatar.setPerson(a.fullName(), true);
        name.setText(a.fullName());
        handle.setText("@" + a.username());
        values.get("nomeCompleto").setText(a.fullName());
        values.get("nomeUtilizador").setText(a.username());
        values.get("dataNascimento").setText(birth(a.birthDate()));
        values.get("criadaEm").setText(moment(a.createdAt()));
        values.get("iniciadaEm").setText(moment(s.startedAt()));
        values.get("expiraEm").setText(moment(s.expiresAt()));
    }

    private static String birth(LocalDate d) {
        if (d == null) {
            return Format.EMPTY;
        }
        int age = Period.between(d, LocalDate.now()).getYears();
        return DAY.format(d) + " (" + age + " anos)";
    }

    private static String moment(LocalDateTime t) {
        return t == null ? Format.EMPTY : MOMENT.format(t);
    }

    public String valueText(String key) {
        return values.get(key).getText();
    }
}
