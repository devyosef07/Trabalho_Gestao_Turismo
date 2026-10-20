package com.turismo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.modules.Format;
import com.turismo.frontend.modules.Page;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Badge;
import com.turismo.frontend.ui.components.BarChart;
import com.turismo.frontend.ui.components.Card;
import com.turismo.frontend.ui.components.StateView;
import com.turismo.frontend.ui.components.Surface;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Painel inicial. Tudo o que aparece vem da base de dados:
 * os totais das 15 tabelas, as views de receita, reservas ativas e atrações
 * por destino, e as views de verificação. Cada secção carrega e falha de
 * forma independente, com o motivo indicado.
 */
public class DashboardPage extends Surface implements Page {

    private final ApiClient api;
    private final Consumer<String> navigate;
    private final Map<String, JLabel> totals = new LinkedHashMap<>();
    private final JLabel receitaTotal = UI.text("…", Theme::figure, () -> Theme.p().text);
    private final JLabel receitaNota = UI.muted(" ");
    private final ChartSection receitaMensal = new ChartSection("Receita por mês",
            "Pagamentos concluídos, view v_receita_mensal", BarChart.Orientation.COLUMNS);
    private final ChartSection receitaNacionalidade = new ChartSection("Receita por nacionalidade",
            "Pagamentos concluídos, view v_receita_nacionalidade", BarChart.Orientation.BARS);
    private final ChartSection atracoes = new ChartSection("Atrações por destino",
            "View v_total_atracoes_destino", BarChart.Orientation.BARS);
    private final Section ativas = new Section("Reservas ativas", "Pendentes ou confirmadas, view v_reservas_ativas");
    private final IntegritySection integridade = new IntegritySection();
    private final StateView pageState = new StateView();
    private final CardLayout pageCards = new CardLayout();
    private final JPanel pageHost = new JPanel(pageCards);
    private final JButton refresh = UI.iconButton(MaterialDesignR.REFRESH, "Atualizar (F5)");
    private boolean loading;
    private int pending;

    public DashboardPage(ApiClient api, Consumer<String> navigate) {
        super(new BorderLayout());
        this.api = api;
        this.navigate = navigate;
        setName("page-dashboard");
        setBorder(UI.pad(UI.S5, UI.S6, UI.S5, UI.S6));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UI.title("Painel"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UI.secondary("Situação atual da agência, lida diretamente da base de dados Oracle."));
        header.add(titles, BorderLayout.CENTER);
        refresh.addActionListener(e -> onRefresh());
        refresh.setName("atualizar");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(refresh);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(UI.pad(0, 0, UI.S5, 0));
        add(header, BorderLayout.NORTH);

        JPanel content = new com.turismo.frontend.ui.components.Scroll.WidthTrackingPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, UI.S4, UI.S4);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        content.add(buildTotals(), c);

        c.gridwidth = 1;
        c.gridy = 1;
        c.weightx = 0.62;
        c.weighty = 0.5;
        content.add(receitaMensal, c);
        c.gridx = 1;
        c.weightx = 0.38;
        c.gridheight = 2;
        c.insets = new Insets(0, 0, UI.S4, 0);
        content.add(integridade, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridheight = 1;
        c.weightx = 0.62;
        c.insets = new Insets(0, 0, UI.S4, UI.S4);
        JPanel pair = new JPanel(new GridLayout(1, 2, UI.S4, 0));
        pair.setOpaque(false);
        pair.add(receitaNacionalidade);
        pair.add(atracoes);
        content.add(pair, c);

        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0.3;
        c.insets = new Insets(0, 0, 0, 0);
        content.add(ativas, c);

        JScrollPane scroll = com.turismo.frontend.ui.components.Scroll.vertical(content);
        pageHost.setOpaque(false);
        pageHost.add(scroll, "content");
        pageHost.add(pageState, "state");
        add(pageHost, BorderLayout.CENTER);
    }

    private JComponent buildTotals() {
        Card card = new Card(new GridLayout(1, 7));
        card.setName("totais");
        card.setBorder(UI.pad(UI.S4, 0, UI.S4, 0));
        addTotal(card, "destinos", "Destinos", "destinos");
        addTotal(card, "turistas", "Turistas", "turistas");
        addTotal(card, "reservas", "Reservas", "reservas");
        addTotal(card, "alojamentos", "Alojamentos", "alojamentos");
        addTotal(card, "operadores", "Operadores", "operadores");
        addTotal(card, "pagamentos", "Pagamentos", "pagamentos");
        JPanel money = tile("Receita total", receitaTotal, receitaNota, null, true);
        card.add(money);
        return card;
    }

    private void addTotal(Card card, String key, String label, String module) {
        JLabel value = UI.text("…", Theme::figure, () -> Theme.p().text);
        value.setName("total-" + key);
        totals.put(key, value);
        card.add(tile(label, value, null, module, totals.size() > 1));
    }

    private JPanel tile(String label, JLabel value, JLabel note, String module, boolean divider) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (divider) {
                    g.setColor(Theme.p().line);
                    g.drawLine(0, 4, 0, getHeight() - 4);
                }
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(UI.pad(0, UI.S4, 0, UI.S2));
        p.add(UI.fieldLabel(label));
        p.add(Box.createVerticalStrut(2));
        p.add(value);
        if (note != null) {
            p.add(note);
        }
        if (module != null) {
            p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.setToolTipText("Abrir " + label);
            p.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigate.accept(module);
                }
            });
        }
        return p;
    }

    // ------------------------------------------------------------- Page

    @Override
    public void onShow() {
        load();
    }

    @Override
    public void onRefresh() {
        load();
    }

    @Override
    public void focusDefault() {
        refresh.requestFocusInWindow();
    }

    public boolean isLoading() {
        return loading;
    }

    public String totalText(String key) {
        return totals.get(key).getText();
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        pending = 2;
        refresh.setEnabled(false);
        Task.run(() -> api.getObject("/dashboard"), this::render, this::renderError, this::finishOne);
        integridade.loading();
        Task.run(() -> api.getObject("/estado"), estado -> renderEstado(estado), error -> {
            integridade.error("Estado da base de dados indisponível", error.getMessage());
        }, this::finishOne);
    }

    private void finishOne() {
        pending--;
        if (pending <= 0) {
            loading = false;
            refresh.setEnabled(true);
        }
    }

    private void renderError(Exception error) {
        if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
            pageState.unreachable(error.getMessage(), this::load);
        } else {
            pageState.error("Não foi possível carregar o painel", error.getMessage(), this::load);
        }
        pageCards.show(pageHost, "state");
    }

    private void render(JsonObject data) {
        pageCards.show(pageHost, "content");
        JsonObject t = data.getAsJsonObject("totais");
        totals.forEach((k, label) -> label.setText(t.has(k) ? t.get(k).getAsString() : Format.EMPTY));

        JsonObject views = data.getAsJsonObject("views");
        JsonObject mensal = views.getAsJsonObject("v_receita_mensal");
        if (mensal.get("ok").getAsBoolean()) {
            BigDecimal sum = BigDecimal.ZERO;
            List<BarChart.Point> points = new ArrayList<>();
            JsonObject d = mensal.getAsJsonObject("data");
            int mes = col(d, "mes");
            int receita = col(d, "receita");
            for (JsonElement r : d.getAsJsonArray("rows")) {
                JsonArray row = r.getAsJsonArray();
                BigDecimal v = row.get(receita).isJsonNull() ? BigDecimal.ZERO : row.get(receita).getAsBigDecimal();
                sum = sum.add(v);
                points.add(new BarChart.Point(row.get(mes).getAsString().replace('-', '/'), v.doubleValue()));
            }
            receitaTotal.setText(Format.compact(sum.doubleValue()));
            receitaNota.setText("MT, pagamentos concluídos");
            receitaTotal.setToolTipText(Format.money(sum));
            receitaMensal.show(points, "Ainda não há pagamentos concluídos.", Format::money);
        } else {
            receitaTotal.setText(Format.EMPTY);
            receitaTotal.setToolTipText(mensal.get("error").getAsString());
            receitaNota.setText("view em falta");
            receitaMensal.error(mensal.get("error").getAsString());
        }
        renderBars(views.getAsJsonObject("v_receita_nacionalidade"), receitaNacionalidade, "nacionalidade", "receita",
                "Ainda não há pagamentos concluídos.", Format::money);
        renderBars(views.getAsJsonObject("v_total_atracoes_destino"), atracoes, "nome_destino", "total_atracoes",
                "Não há destinos registados.", v -> Format.number(v));
        renderAtivas(views.getAsJsonObject("v_reservas_ativas"));
        integridade.setChecks(data.getAsJsonObject("verificacoes"));
    }

    private static int col(JsonObject view, String name) {
        JsonArray cols = view.getAsJsonArray("columns");
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).getAsJsonObject().get("name").getAsString().equals(name)) {
                return i;
            }
        }
        throw new IllegalStateException("A view não tem a coluna " + name);
    }

    private void renderBars(JsonObject section, ChartSection target, String labelCol, String valueCol, String empty,
                            java.util.function.Function<BigDecimal, String> format) {
        if (!section.get("ok").getAsBoolean()) {
            target.error(section.get("error").getAsString());
            return;
        }
        JsonObject d = section.getAsJsonObject("data");
        int l = col(d, labelCol);
        int v = col(d, valueCol);
        List<BarChart.Point> points = new ArrayList<>();
        for (JsonElement r : d.getAsJsonArray("rows")) {
            JsonArray row = r.getAsJsonArray();
            BigDecimal value = row.get(v).isJsonNull() ? BigDecimal.ZERO : row.get(v).getAsBigDecimal();
            points.add(new BarChart.Point(row.get(l).getAsString(), value.doubleValue()));
        }
        target.show(points, empty, format);
    }

    private void renderAtivas(JsonObject section) {
        if (!section.get("ok").getAsBoolean()) {
            ativas.content(ativas.state.unavailable("View em falta", "Aplique database/correcoes.sql para criar as views."));
            return;
        }
        JsonObject d = section.getAsJsonObject("data");
        JsonArray rows = d.getAsJsonArray("rows");
        if (rows.isEmpty()) {
            ativas.empty("Não há reservas pendentes nem confirmadas.");
            return;
        }
        int id = col(d, "id_reserva");
        int nome = col(d, "nome_turista");
        int chegada = col(d, "data_chegada");
        int partida = col(d, "data_partida");
        int estado = col(d, "status_reserva");
        JPanel list = new JPanel(new GridLayout(0, 1));
        list.setOpaque(false);
        int shown = 0;
        for (JsonElement r : rows) {
            if (shown++ == 8) {
                break;
            }
            JsonArray row = r.getAsJsonArray();
            JPanel line = new JPanel(new BorderLayout(UI.S4, 0));
            line.setOpaque(false);
            line.setBorder(UI.pad(UI.S2, 0, UI.S2, 0));
            line.add(UI.body(row.get(id).getAsString() + "   " + row.get(nome).getAsString()), BorderLayout.WEST);
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, UI.S3, 0));
            right.setOpaque(false);
            right.add(UI.secondary(Format.date(Format.parseIsoDate(row.get(chegada).getAsString())) + " a "
                    + Format.date(Format.parseIsoDate(row.get(partida).getAsString()))));
            Badge b = new Badge();
            String s = row.get(estado).getAsString();
            b.set(s, "PENDENTE".equals(s) ? UI.Tone.WARNING : UI.Tone.INFO);
            right.add(b);
            line.add(right, BorderLayout.EAST);
            list.add(line);
        }
        JButton all = UI.secondary("Ver todas as reservas", null);
        all.addActionListener(e -> navigate.accept("reservas"));
        JPanel body = new JPanel(new BorderLayout(0, UI.S3));
        body.setOpaque(false);
        body.add(list, BorderLayout.CENTER);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.add(all);
        body.add(foot, BorderLayout.SOUTH);
        ativas.content(body);
    }

    private void renderEstado(JsonObject estado) {
        integridade.setEstado(estado);
    }

    // --------------------------------------------------------- secções

    /** Cartão com título, fonte dos dados e corpo com estado próprio. */
    private static class Section extends Card {
        final JPanel bodyHost = new JPanel(new BorderLayout());
        final StateView state = new StateView();

        Section(String title, String source) {
            super(new BorderLayout(0, UI.S3));
            setBorder(UI.pad(UI.S4, UI.S5, UI.S4, UI.S5));
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(UI.section(title));
            t.add(UI.muted(source));
            head.add(t, BorderLayout.CENTER);
            add(head, BorderLayout.NORTH);
            bodyHost.setOpaque(false);
            add(bodyHost, BorderLayout.CENTER);
            loading();
        }

        JPanel header() {
            return (JPanel) getComponent(0);
        }

        void loading() {
            content(state.loading("A carregar…"));
        }

        void error(String title, String message) {
            content(state.error(title, message, null));
        }

        void empty(String message) {
            content(state.empty(MaterialDesignC.CHART_BAR, "Sem dados", message, null));
        }

        void content(JComponent c) {
            bodyHost.removeAll();
            bodyHost.add(c, BorderLayout.CENTER);
            bodyHost.revalidate();
            bodyHost.repaint();
        }
    }

    /** Secção com gráfico de barras e alternativa em tabela (acessibilidade). */
    private static final class ChartSection extends Section {
        private final BarChart.Orientation orientation;
        private final JToggleButton asTable = new JToggleButton("Ver tabela");
        private List<BarChart.Point> points = List.of();
        private java.util.function.Function<BigDecimal, String> format = Format::number;

        ChartSection(String title, String source, BarChart.Orientation orientation) {
            super(title, source);
            this.orientation = orientation;
            asTable.setToolTipText("Mostrar os valores numa tabela");
            asTable.setVisible(false);
            asTable.addActionListener(e -> renderBody());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(asTable);
            header().add(right, BorderLayout.EAST);
            setPreferredSize(new Dimension(300, 280));
            setMinimumSize(new Dimension(240, 240));
        }

        void show(List<BarChart.Point> data, String emptyMessage, java.util.function.Function<BigDecimal, String> fmt) {
            this.points = data;
            this.format = fmt;
            if (data.isEmpty()) {
                asTable.setVisible(false);
                empty(emptyMessage);
                return;
            }
            asTable.setVisible(true);
            renderBody();
        }

        void error(String message) {
            asTable.setVisible(false);
            if (message.contains("não existe na base de dados")) {
                content(state.unavailable("View em falta", "Aplique database/correcoes.sql para criar as views."));
            } else {
                error("Sem dados desta view", message);
            }
        }

        private void renderBody() {
            if (asTable.isSelected()) {
                JPanel table = new JPanel(new GridLayout(0, 2, UI.S4, UI.S1));
                table.setOpaque(false);
                for (BarChart.Point p : points) {
                    table.add(UI.body(p.label()));
                    JLabel v = UI.body(format.apply(BigDecimal.valueOf(p.value())));
                    v.setHorizontalAlignment(JLabel.RIGHT);
                    table.add(v);
                }
                JPanel holder = new JPanel(new BorderLayout());
                holder.setOpaque(false);
                holder.add(table, BorderLayout.NORTH);
                content(holder);
                return;
            }
            BarChart chart = new BarChart(orientation, v -> format.apply(BigDecimal.valueOf(v)), Format::compact);
            chart.setData(points);
            content(chart);
        }

    }

    /**
     * Verificações: objetos inválidos e views em falta (de /api/estado) e as
     * quatro views de auditoria, que devem estar vazias quando os dados estão certos.
     */
    private final class IntegritySection extends Section {
        private static final String[][] CHECKS = {
                {"v_aud_pagamento_data_futura", "Pagamentos com data no futuro"},
                {"v_aud_recibo_sem_concluido", "Recibos em pagamentos não concluídos"},
                {"v_reservas_sem_alojamento", "Reservas sem alojamento"},
                {"v_reservas_sem_pagamento", "Reservas sem pagamento"},
        };
        private final JPanel schemaRows = column();
        private final JPanel checkRows = column();
        private JsonObject estado;

        IntegritySection() {
            super("Integridade dos dados", "Estado do esquema e views de verificação");
            setName("integridade");
            setMinimumSize(new Dimension(280, 200));
            setPreferredSize(new Dimension(320, 400));
            JPanel body = column();
            body.add(schemaRows);
            body.add(Box.createVerticalStrut(UI.S4));
            JLabel checks = UI.fieldLabel("Verificações dos dados");
            checks.setAlignmentX(LEFT_ALIGNMENT);
            body.add(checks);
            body.add(Box.createVerticalStrut(UI.S1));
            body.add(checkRows);
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(body, BorderLayout.NORTH);
            content(holder);
        }

        private JPanel column() {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setAlignmentX(LEFT_ALIGNMENT);
            return p;
        }

        @Override
        void loading() {
            // As linhas são atualizadas no lugar; não é preciso esconder o conteúdo.
        }

        @Override
        void error(String title, String message) {
            schemaRows.removeAll();
            schemaRows.add(row(title, message, "Indisponível", UI.Tone.NEUTRAL));
            refresh();
        }

        void setEstado(JsonObject e) {
            estado = e;
            schemaRows.removeAll();
            if (!"OK".equals(e.get("bd").getAsString())) {
                schemaRows.add(row("Base de dados", e.get("bdErro").getAsString(), "Sem ligação", UI.Tone.DANGER));
                refresh();
                return;
            }
            int invalid = e.getAsJsonArray("objetosInvalidos").size();
            int missing = e.getAsJsonArray("viewsEmFalta").size();
            int expected = e.get("viewsEsperadas").getAsInt();
            schemaRows.add(row("Triggers, funções e procedures",
                    invalid == 0 ? "Todos os objetos estão válidos." : "Objetos INVALID bloqueiam escritas nas tabelas afetadas.",
                    invalid == 0 ? "Válidos" : Format.plural(invalid, "inválido", "inválidos"),
                    invalid == 0 ? UI.Tone.SUCCESS : UI.Tone.DANGER));
            schemaRows.add(row("Views", (expected - missing) + " de " + expected + " views existem na base de dados.",
                    missing == 0 ? "Completas" : Format.plural(missing, "em falta", "em falta"),
                    missing == 0 ? UI.Tone.SUCCESS : UI.Tone.WARNING));
            if (invalid + missing > 0) {
                JButton details = UI.secondary("Ver detalhes e correção", null);
                details.setName("detalhes-esquema");
                details.addActionListener(ev -> showSchemaDetails(estado));
                details.setAlignmentX(LEFT_ALIGNMENT);
                schemaRows.add(Box.createVerticalStrut(UI.S2));
                schemaRows.add(details);
            }
            refresh();
        }

        void setChecks(JsonObject verificacoes) {
            checkRows.removeAll();
            for (String[] check : CHECKS) {
                JsonObject s = verificacoes.getAsJsonObject(check[0]);
                if (!s.get("ok").getAsBoolean()) {
                    checkRows.add(row(check[1], "View " + check[0] + " em falta.", "Indisponível", UI.Tone.NEUTRAL));
                    continue;
                }
                int n = s.getAsJsonObject("data").getAsJsonArray("rows").size();
                checkRows.add(row(check[1], "View " + check[0] + ".",
                        n == 0 ? "Sem problemas" : Format.plural(n, "registo", "registos"),
                        n == 0 ? UI.Tone.SUCCESS : UI.Tone.WARNING));
            }
            refresh();
        }

        private JPanel row(String title, String detail, String status, UI.Tone tone) {
            JPanel r = new JPanel(new BorderLayout(UI.S3, 0));
            r.setOpaque(false);
            r.setBorder(UI.pad(UI.S2, 0, UI.S2, 0));
            r.setAlignmentX(LEFT_ALIGNMENT);
            JPanel text = new JPanel(new BorderLayout(0, 2));
            text.setOpaque(false);
            text.add(new com.turismo.frontend.ui.components.WrapText(title, Theme::body, () -> Theme.p().text),
                    BorderLayout.NORTH);
            text.add(new com.turismo.frontend.ui.components.WrapText(detail, Theme::small, () -> Theme.p().textMuted),
                    BorderLayout.CENTER);
            r.add(text, BorderLayout.CENTER);
            Badge b = new Badge();
            b.set(status, tone);
            JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            badge.setOpaque(false);
            badge.add(b);
            r.add(badge, BorderLayout.EAST);
            return r;
        }

        private void refresh() {
            revalidate();
            repaint();
        }
    }

    private void showSchemaDetails(JsonObject estado) {
        StringBuilder sb = new StringBuilder();
        JsonArray invalid = estado.getAsJsonArray("objetosInvalidos");
        sb.append("Objetos INVALID na base de dados (").append(invalid.size()).append("):\n");
        for (JsonElement e : invalid) {
            JsonObject o = e.getAsJsonObject();
            sb.append("  ").append(o.get("tipo").getAsString()).append(" ").append(o.get("nome").getAsString()).append('\n');
        }
        JsonArray missing = estado.getAsJsonArray("viewsEmFalta");
        sb.append("\nViews em falta (").append(missing.size()).append(" de ")
                .append(estado.get("viewsEsperadas").getAsInt()).append("):\n");
        for (JsonElement e : missing) {
            sb.append("  ").append(e.getAsString()).append('\n');
        }
        sb.append("\nUm trigger INVALID impede todas as escritas na tabela a que pertence. ")
                .append("O script database/correcoes.sql recria estes objetos e as views.");
        JTextArea area = new JTextArea(sb.toString(), 18, 52);
        area.setEditable(false);
        area.setFont(Theme.body());
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Estado da base de dados", JOptionPane.WARNING_MESSAGE);
    }
}
