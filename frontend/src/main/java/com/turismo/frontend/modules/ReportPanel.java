package com.turismo.frontend.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Card;
import com.turismo.frontend.ui.components.StateView;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Separador "Relatórios": mostra o conteúdo das views da base de dados
 * relacionadas com o módulo. Os dados vêm diretamente de /api/relatorios/{view}.
 * Se a view não existir na BD, o erro é mostrado tal como é (não há dados de
 * substituição).
 */
public class ReportPanel extends JPanel {

    private final ApiClient api;
    private final List<ReportSpec> reports;
    private final JList<ReportSpec> list;
    private final ViewTableModel model = new ViewTableModel();
    private final JTable table = new JTable(model);
    private final JLabel title = UI.section(" ");
    private final JLabel description = UI.secondary(" ");
    private final JLabel source = UI.muted(" ");
    private final CardLayout states = new CardLayout();
    private final JPanel stateHost = new JPanel(states);
    private final StateView stateView = new StateView();
    private Task<?> current;
    private boolean shownOnce;

    public ReportPanel(ApiClient api, List<ReportSpec> reports) {
        super(new BorderLayout(UI.S4, 0));
        this.api = api;
        this.reports = reports;
        setOpaque(false);
        setBorder(UI.pad(UI.S3, 0, 0, 0));

        list = new JList<>(reports.toArray(new ReportSpec[0]));
        list.setName("lista-relatorios");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, value, index, selected, focus) -> {
            JPanel cell = new JPanel(new BorderLayout(0, 2));
            cell.setOpaque(true);
            cell.setBackground(selected ? Theme.p().neutralSurface : Theme.p().card);
            cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, selected ? Theme.p().primary : Theme.p().card),
                    UI.pad(UI.S2, UI.S3, UI.S2, UI.S3)));
            cell.add(UI.text(value.title, selected ? Theme::strong : Theme::body, () -> Theme.p().text), BorderLayout.NORTH);
            return cell;
        });
        list.getAccessibleContext().setAccessibleName("Relatórios disponíveis");
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                reload();
            }
        });
        Card listCard = new Card(new BorderLayout());
        listCard.setBorder(UI.pad(UI.S2, 1, UI.S2, 1));
        JScrollPane listScroll = com.turismo.frontend.ui.components.Scroll.vertical(list);
        listCard.add(listScroll, BorderLayout.CENTER);
        listCard.setPreferredSize(new Dimension(250, 200));
        add(listCard, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, UI.S3));
        right.setOpaque(false);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(title);
        head.add(Box.createVerticalStrut(2));
        head.add(description);
        head.add(Box.createVerticalStrut(2));
        head.add(source);
        right.add(head, BorderLayout.NORTH);

        table.setName("tabela-relatorio");
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.label());
        table.setDefaultRenderer(Object.class, new ViewRenderer());
        table.setDefaultRenderer(BigDecimal.class, new ViewRenderer());
        table.setDefaultRenderer(LocalDate.class, new ViewRenderer());
        JScrollPane tableScroll = new com.turismo.frontend.ui.components.Scroll(table);
        Card tableCard = new Card(new BorderLayout());
        tableCard.setBorder(UI.pad(1));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        stateHost.setOpaque(false);
        stateHost.add(stateView, "state");
        stateHost.add(tableCard, "table");
        right.add(stateHost, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);
    }

    public void onShow() {
        if (!shownOnce) {
            shownOnce = true;
            list.setSelectedIndex(0);
        } else {
            reload();
        }
    }

    public void reload() {
        ReportSpec spec = list.getSelectedValue();
        if (spec == null) {
            return;
        }
        if (current != null) {
            current.cancel(true);
        }
        title.setText(spec.title);
        description.setText(spec.description);
        source.setText("Fonte: view " + spec.view);
        states.show(stateHost, "state");
        stateView.loading("A ler a view " + spec.view + "…");
        current = Task.run(() -> api.getObject("/relatorios/" + spec.view), result -> {
            model.load(result);
            if (model.getRowCount() == 0) {
                stateView.empty(MaterialDesignI.INBOX_OUTLINE, "Sem linhas",
                        "A view " + spec.view + " não devolveu nenhuma linha.", null);
            } else {
                states.show(stateHost, "table");
            }
        }, error -> {
            if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
                stateView.unreachable(error.getMessage(), this::reload);
            } else if (error instanceof ApiException && "OBJETO_INEXISTENTE".equals(((ApiException) error).getCode())) {
                stateView.unavailable("A view " + spec.view + " não existe na base de dados",
                        "O SYSTEM tem de correr database/conceder_privilegios.sql (CREATE VIEW) e depois "
                                + "executa-se database/views.sql.");
            } else {
                stateView.error("Não foi possível ler a view " + spec.view, error.getMessage(), this::reload);
            }
        });
    }

    public JTable table() {
        return table;
    }

    public JList<ReportSpec> reportList() {
        return list;
    }

    /** Modelo genérico: as colunas vêm da própria view (nome e tipo). */
    static final class ViewTableModel extends AbstractTableModel {
        private final List<String> names = new ArrayList<>();
        private final List<String> types = new ArrayList<>();
        private final List<Object[]> rows = new ArrayList<>();

        void load(JsonObject result) {
            names.clear();
            types.clear();
            rows.clear();
            for (JsonElement c : result.getAsJsonArray("columns")) {
                names.add(c.getAsJsonObject().get("name").getAsString());
                types.add(c.getAsJsonObject().get("type").getAsString());
            }
            for (JsonElement r : result.getAsJsonArray("rows")) {
                JsonArray a = r.getAsJsonArray();
                Object[] row = new Object[names.size()];
                for (int i = 0; i < row.length; i++) {
                    JsonElement e = a.get(i);
                    if (e == null || e.isJsonNull()) {
                        row[i] = null;
                    } else if ("number".equals(types.get(i))) {
                        row[i] = e.getAsBigDecimal();
                    } else if ("date".equals(types.get(i))) {
                        row[i] = Format.parseIsoDate(e.getAsString());
                    } else {
                        row[i] = e.getAsString();
                    }
                }
                rows.add(row);
            }
            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return names.size();
        }

        @Override
        public String getColumnName(int column) {
            return Format.humanize(names.get(column));
        }

        String rawName(int column) {
            return names.get(column);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (types.get(column)) {
                case "number": return BigDecimal.class;
                case "date": return LocalDate.class;
                default: return String.class;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row)[column];
        }
    }

    /** Valores monetários (receita, valor, total pago) aparecem em meticais. */
    static final class ViewRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            ViewTableModel m = (ViewTableModel) table.getModel();
            String name = m.rawName(table.convertColumnIndexToModel(column));
            String text;
            if (value == null) {
                text = Format.EMPTY;
            } else if (value instanceof BigDecimal) {
                boolean money = name.contains("valor") || name.contains("receita") || name.contains("pago");
                boolean rating = name.contains("media") || name.contains("avaliacao") || name.startsWith("class");
                text = money ? Format.money((BigDecimal) value)
                        : rating ? Format.rating((BigDecimal) value) : Format.number((BigDecimal) value);
            } else if (value instanceof LocalDate) {
                text = Format.date((LocalDate) value);
            } else {
                text = value.toString();
            }
            super.getTableCellRendererComponent(table, text, isSelected, false, row, column);
            setBorder(UI.pad(0, UI.S3, 0, UI.S3));
            setHorizontalAlignment(value instanceof BigDecimal ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (!isSelected) {
                setForeground(value == null ? Theme.p().textMuted : Theme.p().text);
            }
            return this;
        }
    }
}
