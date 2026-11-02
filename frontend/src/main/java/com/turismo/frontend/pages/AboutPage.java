package com.turismo.frontend.pages;

import com.turismo.frontend.modules.Page;
import com.turismo.frontend.ui.components.Avatar;
import com.turismo.frontend.ui.components.Badge;
import com.turismo.frontend.ui.components.Card;
import com.turismo.frontend.ui.components.Scroll;
import com.turismo.frontend.ui.components.Surface;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.components.WrapText;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * "Sobre o projeto": identificação do trabalho, integrantes do grupo,
 * tecnologias usadas e uma explicação breve de como a aplicação comunica com
 * a base de dados Oracle. Conteúdo estático: não faz pedidos à API.
 */
public class AboutPage extends Surface implements Page {

    /** Integrantes do Grupo 1 (nome, número de estudante). */
    static final String[][] MEMBERS = {
            {"Allanah Malate", "20230338"},
            {"Hervene Martins", "20250814"},
            {"José Vaz", "20250247"},
            {"Letícia Nhangumbe", "20250816"},
    };

    /** Tecnologias (nome, papel no projeto). */
    static final String[][] TECHNOLOGIES = {
            {"Java 21", "Linguagem do backend e da aplicação desktop"},
            {"Maven", "Compilação e dependências (módulos turismo-api e turismo-desktop)"},
            {"Java Swing + FlatLaf 3.6", "Interface gráfica e tema claro/escuro"},
            {"Ikonli (Material Design)", "Ícones da interface"},
            {"API REST (HttpServer do JDK)", "Servidor HTTP próprio que expõe os dados em /api"},
            {"Gson 2.13", "Conversão entre objetos Java e JSON"},
            {"Oracle Database 10g XE", "Base de dados relacional do projeto"},
            {"SQL e PL/SQL", "Tabelas, views, procedures, functions, triggers e sequences"},
            {"JDBC (ojdbc11)", "Driver de ligação do Java ao Oracle"},
            {"HikariCP 6.2", "Pool de ligações à base de dados"},
            {"JUnit 5", "Testes automáticos do backend e da interface"},
    };

    /** Passos da comunicação com a base de dados (título, descrição). */
    static final String[][] STEPS = {
            {"1. Pedido HTTP", "A aplicação desktop nunca liga diretamente à base de dados. Cada ação (listar, "
                    + "criar, editar, eliminar) envia um pedido HTTP com JSON à API REST "
                    + "(http://localhost:8083/api), com o token da sessão no cabeçalho "
                    + "\"Authorization: Bearer\"."},
            {"2. Validação na API", "O filtro de autenticação confirma que a sessão é válida. Depois, o handler "
                    + "da rota valida os dados recebidos e chama o DAO (Data Access Object) da entidade."},
            {"3. JDBC e pool de ligações", "O DAO pede uma ligação ao pool HikariCP (no máximo 4 ligações "
                    + "simultâneas, porque o Oracle 10g XE tem poucos processos) e executa o SQL através do "
                    + "driver JDBC ojdbc11. As consultas usam PreparedStatement com parâmetros."},
            {"4. Leituras por views", "Todas as consultas são SELECT sobre views (ex.: V_LISTA_DESTINOS, "
                    + "definidas em views.sql). A aplicação nunca lê diretamente das tabelas."},
            {"5. Escritas por procedures", "Inserir, alterar e eliminar é feito só por stored procedures PL/SQL "
                    + "(procedures.sql e contas.sql). As sequences geram os códigos (ex.: DT-004) e os triggers "
                    + "aplicam as regras de integridade."},
            {"6. Resposta", "As procedures fazem COMMIT ou ROLLBACK e escrevem o resultado com DBMS_OUTPUT. "
                    + "A API lê essa saída, transforma-a numa resposta JSON (sucesso ou erro) e a aplicação "
                    + "atualiza o ecrã."},
    };

    private final Card hero;
    private final Card members;
    private final Card technologies;
    private final Card database;
    private boolean revealed;

    public AboutPage() {
        super(new BorderLayout());
        setName("page-sobre");
        setBorder(UI.pad(UI.S5, UI.S6, UI.S5, UI.S6));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UI.title("Sobre o projeto"));
        header.add(Box.createVerticalStrut(2));
        header.add(UI.secondary("Quem fez, com que tecnologias e como a aplicação fala com a base de dados."));
        header.setBorder(UI.pad(0, 0, UI.S5, 0));
        add(header, BorderLayout.NORTH);

        JPanel content = new Scroll.WidthTrackingPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, UI.S4, 0);

        hero = buildHero();
        members = buildMembers();
        technologies = buildTechnologies();
        database = buildDatabase();
        c.gridy = 0;
        content.add(hero, c);
        c.gridy = 1;
        content.add(members, c);
        c.gridy = 2;
        content.add(technologies, c);
        c.gridy = 3;
        content.add(database, c);
        c.gridy = 4;
        c.weighty = 1;
        content.add(Box.createGlue(), c);
        add(Scroll.vertical(content), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------ cartões

    private Card buildHero() {
        Card card = new Card(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(2, 1, getWidth() - 5, getHeight() - 6, 14, 14));
                g2.setPaint(new GradientPaint(0, 0, Theme.p().brandMid, getWidth(), 0, Theme.p().brandBright));
                g2.fillRect(0, 0, getWidth(), 8);
                g2.dispose();
            }
        };
        card.setName("sobre-projeto");
        card.setBorder(UI.pad(UI.S5 + 6, UI.S5, UI.S5, UI.S5));
        JPanel texts = column();
        JLabel title = UI.text("Gestão de Turismo em Moçambique", () -> Theme.title().deriveFont(24f),
                () -> Theme.p().text);
        texts.add(title);
        texts.add(Box.createVerticalStrut(UI.S1));
        texts.add(UI.secondary("Trabalho Prático de Base de Dados I · Engenharia Informática · ISCTEM, 2026"));
        texts.add(Box.createVerticalStrut(UI.S3));
        Badge group = new Badge();
        group.set("Grupo 1", UI.Tone.INFO);
        texts.add(group);
        texts.add(Box.createVerticalStrut(UI.S3));
        texts.add(UI.muted("Docentes: Mestre Otília Fernandes Graça e Eng. Abel Ernesto"));
        alignLeft(texts);
        card.add(texts, BorderLayout.CENTER);
        return card;
    }

    private Card buildMembers() {
        Card card = new Card(new BorderLayout(0, UI.S4));
        card.setName("sobre-integrantes");
        card.setBorder(UI.pad(UI.S5));
        card.add(UI.section("Integrantes do grupo"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 2, UI.S5, UI.S4));
        grid.setOpaque(false);
        for (String[] m : MEMBERS) {
            grid.add(member(m[0], m[1]));
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private static JComponent member(String fullName, String number) {
        JPanel row = new JPanel(new BorderLayout(UI.S3, 0));
        row.setOpaque(false);
        Avatar avatar = new Avatar(44, () -> Theme.p().card);
        avatar.setPerson(fullName, false);
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(avatar);
        row.add(holder, BorderLayout.WEST);
        JPanel texts = column();
        texts.add(UI.text(fullName, Theme::strong, () -> Theme.p().text));
        texts.add(Box.createVerticalStrut(2));
        texts.add(UI.muted("Nº de estudante " + number));
        alignLeft(texts);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        center.add(texts, c);
        row.add(center, BorderLayout.CENTER);
        return row;
    }

    private Card buildTechnologies() {
        Card card = new Card(new BorderLayout(0, UI.S4));
        card.setName("sobre-tecnologias");
        card.setBorder(UI.pad(UI.S5));
        card.add(UI.section("Tecnologias usadas"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 2, UI.S5, UI.S4));
        grid.setOpaque(false);
        for (String[] t : TECHNOLOGIES) {
            JPanel item = column();
            item.add(UI.text(t[0], Theme::strong, () -> Theme.p().text));
            item.add(Box.createVerticalStrut(2));
            item.add(UI.muted(t[1]));
            alignLeft(item);
            grid.add(item);
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private Card buildDatabase() {
        Card card = new Card(new BorderLayout(0, UI.S4));
        card.setName("sobre-base-de-dados");
        card.setBorder(UI.pad(UI.S5));
        card.add(UI.section("Como funciona a comunicação com a base de dados"), BorderLayout.NORTH);

        JPanel body = column();
        body.add(flow());
        body.add(Box.createVerticalStrut(UI.S5));
        for (String[] s : STEPS) {
            body.add(UI.text(s[0], Theme::strong, () -> Theme.p().text));
            body.add(Box.createVerticalStrut(UI.S1));
            body.add(new WrapText(s[1], Theme::body, () -> Theme.p().textSecondary).widthHint(600));
            body.add(Box.createVerticalStrut(UI.S4));
        }
        alignLeft(body);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Diagrama em linha: Aplicação desktop → API REST → DAO / JDBC → Oracle. */
    private static JComponent flow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        Object[][] boxes = {
                {MaterialDesignM.MONITOR, "Aplicação desktop", "Java Swing"},
                {MaterialDesignA.API, "API REST", "HTTP + JSON, porta 8083"},
                {MaterialDesignL.LANGUAGE_JAVA, "DAO + JDBC", "HikariCP, ojdbc11"},
                {MaterialDesignD.DATABASE, "Oracle 10g XE", "Views e procedures"},
        };
        for (int i = 0; i < boxes.length; i++) {
            if (i > 0) {
                c.gridx = i * 2 - 1;
                c.weightx = 0;
                JLabel arrow = new JLabel(Icons.of(MaterialDesignA.ARROW_LEFT_RIGHT, 20, () -> Theme.p().textMuted));
                arrow.setBorder(UI.pad(0, UI.S2, 0, UI.S2));
                row.add(arrow, c);
            }
            c.gridx = i * 2;
            c.weightx = 1;
            row.add(step((Ikon) boxes[i][0], (String) boxes[i][1], (String) boxes[i][2]), c);
        }
        return row;
    }

    private static JComponent step(Ikon icon, String title, String subtitle) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.p().primarySoft);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.p().border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(UI.pad(UI.S3, UI.S3, UI.S3, UI.S3));
        JLabel ic = new JLabel(Icons.of(icon, 24, () -> Theme.p().primary));
        box.add(ic);
        box.add(Box.createVerticalStrut(UI.S2));
        box.add(UI.text(title, Theme::strong, () -> Theme.p().text));
        box.add(Box.createVerticalStrut(2));
        box.add(UI.muted(subtitle));
        for (Component comp : box.getComponents()) {
            ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        return box;
    }

    // ------------------------------------------------------------ auxiliares

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static void alignLeft(JPanel p) {
        for (Component comp : p.getComponents()) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    @Override
    public void onShow() {
        if (!revealed) {
            revealed = true;
            hero.reveal(0);
            members.reveal(70);
            technologies.reveal(140);
            database.reveal(210);
        }
    }
}
